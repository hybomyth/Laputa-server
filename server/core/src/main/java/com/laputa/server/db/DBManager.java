package com.laputa.server.db;

import com.laputa.server.core.BlockingIOProcessor;
import com.laputa.server.core.dao.UserKey;
import com.laputa.server.core.model.auth.User;
import com.laputa.server.core.model.enums.GraphType;
import com.laputa.server.core.reporting.average.AggregationKey;
import com.laputa.server.core.reporting.average.AggregationValue;
import com.laputa.server.core.stats.model.Stat;
import com.laputa.server.db.dao.*;
import com.laputa.server.db.model.FlashedToken;
import com.laputa.server.db.model.Purchase;
import com.laputa.server.db.model.Redeem;
import com.laputa.utils.ServerProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The Laputa Project.
 * Created by Sommer
 * Created on 19.02.16.
 */
public class DBManager implements Closeable {

    public static final String DB_PROPERTIES_FILENAME = "db.properties";
    private static final Logger log = LogManager.getLogger(DBManager.class);
    private final HikariDataSource ds;

    private final BlockingIOProcessor blockingIOProcessor;
    private final boolean cleanOldReporting;
    public UserDBDao userDBDao;
    protected ReportingDBDao reportingDBDao;
    protected RedeemDBDao redeemDBDao;
    protected PurchaseDBDao purchaseDBDao;
    protected FlashedTokensDBDao flashedTokensDBDao;

    public DBManager(BlockingIOProcessor blockingIOProcessor, boolean isEnabled) {
        this(DB_PROPERTIES_FILENAME, blockingIOProcessor, isEnabled);
    }

    public DBManager(String propsFilename, BlockingIOProcessor blockingIOProcessor, boolean isEnabled) {
        this.blockingIOProcessor = blockingIOProcessor;

        if (!isEnabled) {
            log.info("Separate DB storage disabled.");
            this.ds = null;
            this.cleanOldReporting = false;
            return;
        }

        ServerProperties serverProperties;
        try {
            serverProperties = new ServerProperties(propsFilename);
            if (serverProperties.size() == 0) {
                throw new RuntimeException();
            }
        } catch (RuntimeException e) {
            log.warn("No {} file found. Separate DB storage disabled.", propsFilename);
            this.ds = null;
            this.cleanOldReporting = false;
            return;
        }

        HikariConfig config = initConfig(serverProperties);

        log.info("DB url : {}", config.getJdbcUrl());
        log.info("DB user : {}", config.getUsername());
        log.info("Connecting to DB...");

        HikariDataSource hikariDataSource;
        try {
            hikariDataSource = new HikariDataSource(config);
        } catch (Exception e) {
            log.error("Not able connect to DB. Skipping. Reason : {}", e.getMessage());
            this.ds = null;
            this.cleanOldReporting = false;
            return;
        }

        this.ds = hikariDataSource;
        this.reportingDBDao = new ReportingDBDao(hikariDataSource);
        this.userDBDao = new UserDBDao(hikariDataSource);
        this.redeemDBDao = new RedeemDBDao(hikariDataSource);
        this.purchaseDBDao = new PurchaseDBDao(hikariDataSource);
        this.flashedTokensDBDao = new FlashedTokensDBDao(hikariDataSource);
        this.cleanOldReporting = serverProperties.getBoolProperty("clean.reporting");

        checkDBVersion();

        log.info("Connected to database successfully.");
    }

    private void checkDBVersion() {
        try {
            String dbVersion = userDBDao.getDBVersion();
            if (dbVersion.contains("5.1")) {
                log.error("Current mysql version is lower than minimum required 5.1.7 version. PLEASE UPDATE YOUR DB.");
            }
        } catch (Exception e) {
            log.error("Error getting DB version.", e.getMessage());
        }
    }

    private HikariConfig initConfig(ServerProperties serverProperties) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(serverProperties.getProperty("jdbc.url"));
        config.setUsername(serverProperties.getProperty("user"));
        config.setPassword(serverProperties.getProperty("password"));

        config.setAutoCommit(false);
        config.setConnectionTimeout(serverProperties.getLongProperty("connection.timeout.millis"));
        config.setMaximumPoolSize(3);
        config.setMaxLifetime(0);
        config.setConnectionTestQuery("SELECT 1");
        return config;
    }

    public void deleteUser(UserKey userKey) {
        if (isDBEnabled() && userKey != null) {
            blockingIOProcessor.executeDB(() -> userDBDao.deleteUser(userKey));
        }
    }

    public void saveUsers(ArrayList<User> users) {
        if (isDBEnabled() && users.size() > 0) {
            blockingIOProcessor.executeDB(() -> userDBDao.save(users));
        }
    }

    public void insertStat(String region, Stat stat) {
        if (isDBEnabled()) {
            reportingDBDao.insertStat(region, stat);
        }
    }

    public void insertReporting(Map<AggregationKey, AggregationValue> map, GraphType graphType) {
        if (isDBEnabled() && map.size() > 0) {
            blockingIOProcessor.executeDB(() -> reportingDBDao.insert(map, graphType));
        }
    }

    public void insertReportingRaw(Map<AggregationKey, Object> rawData) {
        if (isDBEnabled() && rawData.size() > 0) {
            blockingIOProcessor.executeDB(() -> reportingDBDao.insertRawData(rawData));
        }
    }

    public void cleanOldReportingRecords(Instant now) {
        if (isDBEnabled() && cleanOldReporting) {
            blockingIOProcessor.executeDB(() -> reportingDBDao.cleanOldReportingRecords(now));
        }
    }

    public Redeem selectRedeemByToken(String token) throws Exception {
        if (isDBEnabled()) {
            return redeemDBDao.selectRedeemByToken(token);
        }
        return null;
    }

    public boolean updateRedeem(String email, String token) throws Exception {
        return redeemDBDao.updateRedeem(email, token);
    }

    public void insertRedeems(List<Redeem> redeemList) {
        if (isDBEnabled() && redeemList.size() > 0) {
            redeemDBDao.insertRedeems(redeemList);
        }
    }

    public FlashedToken selectFlashedToken(String token) {
        if (isDBEnabled()) {
            return flashedTokensDBDao.selectFlashedToken(token);
        }
        return null;
    }

    public boolean activateFlashedToken(String token) {
        return flashedTokensDBDao.activateFlashedToken(token);
    }

    public boolean insertFlashedTokens(FlashedToken[] flashedTokenList) throws Exception {
        if (isDBEnabled() && flashedTokenList.length > 0) {
            flashedTokensDBDao.insertFlashedTokens(flashedTokenList);
            return true;
        }
        return false;
    }


    public void insertPurchase(Purchase purchase) {
        if (isDBEnabled()) {
            purchaseDBDao.insertPurchase(purchase);
        }
    }

    public boolean isDBEnabled() {
        return ds != null;
    }

    public void executeSQL(String sql) throws Exception {
        try (Connection connection = ds.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
            connection.commit();
        }
    }

    public Connection getConnection() throws Exception {
        return ds.getConnection();
    }

    @Override
    public void close() {
        if (isDBEnabled()) {
            System.out.println("Closing DB...");
            ds.close();
        }
    }

}
