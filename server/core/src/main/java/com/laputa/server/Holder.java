package com.laputa.server;

import com.laputa.server.core.BlockingIOProcessor;
import com.laputa.server.core.dao.*;
import com.laputa.server.core.processors.EventorProcessor;
import com.laputa.server.core.stats.GlobalStats;
import com.laputa.server.db.DBManager;
import com.laputa.server.notifications.mail.MailWrapper;
import com.laputa.server.notifications.push.GCMWrapper;
import com.laputa.server.notifications.sms.SMSWrapper;
import com.laputa.server.notifications.twitter.TwitterWrapper;
import com.laputa.server.redis.RedisClient;
import com.laputa.server.transport.TransportTypeHolder;
import com.laputa.server.workers.ReadingWidgetsWorker;
import com.laputa.server.workers.timer.TimerWorker;
import com.laputa.utils.FileUtils;
import com.laputa.utils.IPUtils;
import com.laputa.utils.ServerProperties;
import io.netty.util.internal.SystemPropertyUtil;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;

import java.io.Closeable;

import static com.laputa.utils.ReportingUtil.getReportingFolder;

/**
 * Just a holder for all necessary objects for server instance creation.
 *
 * The Laputa Project.
 * Created by Sommer
 * Created on 28.09.15.
 */
public class Holder implements Closeable {

    public final FileManager fileManager;

    public final SessionDao sessionDao;

    public final UserDao userDao;

    public final TokenManager tokenManager;

    public final ReportingDao reportingDao;

    public final RedisClient redisClient;

    public final DBManager dbManager;

    public final GlobalStats stats;

    public final ServerProperties props;

    public final BlockingIOProcessor blockingIOProcessor;
    public final TransportTypeHolder transportTypeHolder;
    public final TwitterWrapper twitterWrapper;
    public final MailWrapper mailWrapper;
    public final GCMWrapper gcmWrapper;
    public final SMSWrapper smsWrapper;
    public final String region;
    public final TimerWorker timerWorker;
    public final ReadingWidgetsWorker readingWidgetsWorker;

    public final EventorProcessor eventorProcessor;
    public final DefaultAsyncHttpClient asyncHttpClient;

    public final Limits limits;

    public final String csvDownloadUrl;

    public final String host;

    public final SslContextHolder sslContextHolder;

    public Holder(ServerProperties serverProperties, ServerProperties mailProperties,
                  ServerProperties smsProperties, ServerProperties gcmProperties, boolean restore) {
        disableNettyLeakDetector();
        this.props = serverProperties;

        this.region = serverProperties.getProperty("region", "local");
        String netInterface = serverProperties.getProperty("net.interface", "eth");
        this.host = serverProperties.getProperty("server.host", IPUtils.resolveHostIP(netInterface));

        this.redisClient = new RedisClient(new ServerProperties(RedisClient.REDIS_PROPERTIES), region);

        String dataFolder = serverProperties.getProperty("data.folder");
        this.fileManager = new FileManager(dataFolder);
        this.sessionDao = new SessionDao();
        this.blockingIOProcessor = new BlockingIOProcessor(
                serverProperties.getIntProperty("blocking.processor.thread.pool.limit", 6),
                serverProperties.getIntProperty("notifications.queue.limit", 5000)
        );
        this.dbManager = new DBManager(blockingIOProcessor, serverProperties.getBoolProperty("enable.db"));

        if (restore) {
            try {
                this.userDao = new UserDao(dbManager.userDBDao.getAllUsers(this.region), this.region);
            } catch (Exception e) {
                System.out.println("Error restoring data from DB!");
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        } else {
            this.userDao = new UserDao(fileManager.deserializeUsers(), this.region);
        }

        this.tokenManager = new TokenManager(this.userDao.users, blockingIOProcessor, redisClient, host);
        this.stats = new GlobalStats();
        final String reportingFolder = getReportingFolder(dataFolder);
        this.reportingDao = new ReportingDao(reportingFolder, serverProperties);

        this.transportTypeHolder = new TransportTypeHolder(serverProperties);

        this.asyncHttpClient = new DefaultAsyncHttpClient(new DefaultAsyncHttpClientConfig.Builder()
                .setUserAgent(null)
                .setEventLoopGroup(transportTypeHolder.workerGroup)
                .setKeepAlive(true)
                .build()
        );

        this.twitterWrapper = new TwitterWrapper();
        this.mailWrapper = new MailWrapper(mailProperties);
        this.gcmWrapper = new GCMWrapper(gcmProperties, asyncHttpClient);
        this.smsWrapper = new SMSWrapper(smsProperties, asyncHttpClient);

        this.eventorProcessor = new EventorProcessor(gcmWrapper, twitterWrapper, blockingIOProcessor, stats);
        this.timerWorker = new TimerWorker(userDao, sessionDao, gcmWrapper);
        this.readingWidgetsWorker = new ReadingWidgetsWorker(sessionDao, userDao);
        this.limits = new Limits(props);

        this.csvDownloadUrl = FileUtils.csvDownloadUrl(host, props.getProperty("http.port"));

        String contactEmail = serverProperties.getProperty("contact.email", mailProperties.getProperty("mail.smtp.username"));
        this.sslContextHolder = new SslContextHolder(props, contactEmail);
    }

    //for tests only
    public Holder(ServerProperties serverProperties, TwitterWrapper twitterWrapper, MailWrapper mailWrapper, GCMWrapper gcmWrapper, SMSWrapper smsWrapper, String dbFileName) {
        disableNettyLeakDetector();
        this.props = serverProperties;

        this.region = "local";
        String netInterface = serverProperties.getProperty("net.interface", "eth");
        this.host = serverProperties.getProperty("server.host", IPUtils.resolveHostIP(netInterface));
        this.redisClient = new RedisClient(new ServerProperties(RedisClient.REDIS_PROPERTIES), "real");

        String dataFolder = serverProperties.getProperty("data.folder");
        this.fileManager = new FileManager(dataFolder);
        this.sessionDao = new SessionDao();
        this.userDao = new UserDao(fileManager.deserializeUsers(), this.region);
        this.blockingIOProcessor = new BlockingIOProcessor(
                serverProperties.getIntProperty("blocking.processor.thread.pool.limit", 5),
                serverProperties.getIntProperty("notifications.queue.limit", 10000)
        );
        this.tokenManager = new TokenManager(this.userDao.users, blockingIOProcessor, redisClient, host);
        this.stats = new GlobalStats();
        final String reportingFolder = getReportingFolder(dataFolder);
        this.reportingDao = new ReportingDao(reportingFolder, serverProperties);

        this.transportTypeHolder = new TransportTypeHolder(serverProperties);

        this.twitterWrapper = twitterWrapper;
        this.mailWrapper = mailWrapper;
        this.gcmWrapper = gcmWrapper;
        this.smsWrapper = smsWrapper;

        this.eventorProcessor = new EventorProcessor(gcmWrapper, twitterWrapper, blockingIOProcessor, stats);
        this.asyncHttpClient = new DefaultAsyncHttpClient(new DefaultAsyncHttpClientConfig.Builder()
                .setUserAgent(null)
                .setEventLoopGroup(transportTypeHolder.workerGroup)
                .setKeepAlive(false)
                .build()
        );

        this.dbManager = new DBManager(dbFileName, blockingIOProcessor, serverProperties.getBoolProperty("enable.db"));
        this.timerWorker = new TimerWorker(userDao, sessionDao, gcmWrapper);
        this.readingWidgetsWorker = new ReadingWidgetsWorker(sessionDao, userDao);
        this.limits = new Limits(props);

        this.csvDownloadUrl = FileUtils.csvDownloadUrl(host, props.getProperty("http.port"));

        this.sslContextHolder = new SslContextHolder(props, "test@laputa.cc");
    }

    private static void disableNettyLeakDetector() {
        String leakProperty = SystemPropertyUtil.get("io.netty.leakDetection.level");
        //we do not pass any with JVM option
        if (leakProperty == null) {
            System.setProperty("io.netty.leakDetection.level", "disabled");
        }
    }

    @Override
    public void close() {
        this.reportingDao.close();

        System.out.println("Stopping BlockingIOProcessor...");
        this.blockingIOProcessor.close();

        System.out.println("Stopping DBManager...");
        this.dbManager.close();

        System.out.println("Stopping Transport Holder...");
        transportTypeHolder.close();

        redisClient.close();
    }
}
