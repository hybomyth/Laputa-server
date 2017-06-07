package com.laputa.server.application.handlers.main.auth;

import com.laputa.server.Holder;
import com.laputa.server.application.handlers.main.AppHandler;
import com.laputa.server.application.handlers.sharing.auth.AppShareLoginHandler;
import com.laputa.server.core.model.AppName;
import com.laputa.server.core.model.DashBoard;
import com.laputa.server.core.model.auth.FacebookTokenResponse;
import com.laputa.server.core.model.auth.Session;
import com.laputa.server.core.model.auth.User;
import com.laputa.server.core.protocol.enums.Command;
import com.laputa.server.core.protocol.handlers.DefaultExceptionHandler;
import com.laputa.server.core.protocol.model.messages.appllication.LoginMessage;
import com.laputa.server.handlers.DefaultReregisterHandler;
import com.laputa.server.handlers.common.UserNotLoggedHandler;
import com.laputa.utils.IPUtils;
import com.laputa.utils.JsonParser;
import io.netty.channel.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;
import org.asynchttpclient.netty.handler.WebSocketHandler;

import java.util.NoSuchElementException;

import static com.laputa.server.core.protocol.enums.Response.*;
import static com.laputa.utils.LaputaByteBufUtil.*;
import static com.laputa.utils.StringUtils.BODY_SEPARATOR_STRING;


/**
 * Handler responsible for managing apps login messages.
 * Initializes netty channel with a state tied with user.
 *
 * The Laputa Project.
 * Created by Sommer
 * Created on 2/1/2015.
 *
 */
@ChannelHandler.Sharable
public class AppLoginHandler extends SimpleChannelInboundHandler<LoginMessage> implements DefaultReregisterHandler, DefaultExceptionHandler {

    private static final String URL = "https://graph.facebook.com/me?fields=email&access_token=";
    private static final Logger log = LogManager.getLogger(AppLoginHandler.class);

    private final Holder holder;
    private final DefaultAsyncHttpClient asyncHttpClient;

    public AppLoginHandler(Holder holder) {
        this.holder = holder;
        this.asyncHttpClient = holder.asyncHttpClient;
    }

    private static void cleanPipeline(ChannelPipeline pipeline) {
        try {
            //common handlers for websockets and app pipeline
            pipeline.remove(AppLoginHandler.class);
            pipeline.remove(UserNotLoggedHandler.class);
            pipeline.remove(GetServerHandler.class);

            //app pipeline sepcific handlers
            if (pipeline.get(WebSocketHandler.class) != null) {
                pipeline.remove(RegisterHandler.class);
                pipeline.remove(AppShareLoginHandler.class);
            }
        } catch (NoSuchElementException e) {
            //this case possible when few login commands come at same time to different threads
            //just do nothing and ignore.

        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LoginMessage message) throws Exception {
        //warn: split may be optimized
        String[] messageParts = message.body.split(BODY_SEPARATOR_STRING);

        if (messageParts.length < 2) {
            log.error("Wrong income message format.");
            ctx.writeAndFlush(illegalCommand(message.id), ctx.voidPromise());
            return;
        }

        final String email = messageParts[0].toLowerCase();
        final OsType osType = messageParts.length > 3 ? OsType.parse(messageParts[2]) : OsType.OTHER;
        final String version = messageParts.length > 3 ? messageParts[3] : null;

        if (messageParts.length == 5) {
            if (AppName.FACEBOOK.equals(messageParts[4])) {
                facebookLogin(ctx, message.id, email, messageParts[1], osType, version);
            } else {
                final String appName = messageParts[4];
                laputaLogin(ctx, message.id, email, messageParts[1], osType, version, appName);
            }
        } else {
            //todo this is for back compatibility
            laputaLogin(ctx, message.id, email, messageParts[1], osType, version, AppName.LAPUTA);
        }
    }

    private void facebookLogin(ChannelHandlerContext ctx, int messageId, String email, String token, OsType osType, String version) {
        asyncHttpClient.prepareGet(URL + token)
                .execute(new AsyncCompletionHandler<Response>() {
                    @Override
                    public Response onCompleted(Response response) throws Exception {
                        if (response.getStatusCode() != 200) {
                            String errMessage = response.getResponseBody();
                            if (errMessage != null && errMessage.contains("expired")) {
                                log.warn("Facebook token expired for user {}.", email);
                            } else {
                                log.warn("Error getting facebook token for user {}. Reason : {}", email, errMessage);
                            }
                            ctx.writeAndFlush(notAllowed(messageId), ctx.voidPromise());
                            return response;
                        }

                        try {
                            String responseBody = response.getResponseBody();
                            FacebookTokenResponse facebookTokenResponse = JsonParser.parseFacebookTokenResponse(responseBody);
                            if (email.equalsIgnoreCase(facebookTokenResponse.email)) {
                                User user = holder.userDao.getByName(email, AppName.LAPUTA);
                                if (user == null) {
                                    user = holder.userDao.addFacebookUser(email, AppName.LAPUTA);
                                }

                                login(ctx, messageId, user, osType, version);
                            }
                        } catch (Exception e) {
                            log.error("Error during facebook response parsing for user {}. Reason : {}", email, response.getResponseBody());
                            ctx.writeAndFlush(notAllowed(messageId), ctx.voidPromise());
                        }

                        return response;
                    }

                    @Override
                    public void onThrowable(Throwable t) {
                        log.error("Error performing facebook request. Token {} for user {}. Reason : {}", token, email, t.getMessage());
                        ctx.writeAndFlush(notAllowed(messageId), ctx.voidPromise());
                    }
                });
    }

    private void laputaLogin(ChannelHandlerContext ctx, int msgId, String email, String pass, OsType osType, String version, String appName) {
        User user = holder.userDao.getByName(email, appName);

        if (user == null) {
            log.warn("User '{}' not registered. {}", email, ctx.channel().remoteAddress());
            ctx.writeAndFlush(makeResponse(msgId, USER_NOT_REGISTERED), ctx.voidPromise());
            return;
        }

        if (user.pass == null) {
            log.warn("Facebook user '{}' tries to login with pass. {}", email, ctx.channel().remoteAddress());
            ctx.writeAndFlush(makeResponse(msgId, FACEBOOK_USER_LOGIN_WITH_PASS), ctx.voidPromise());
            return;
        }

        if (!user.pass.equals(pass)) {
            log.warn("User '{}' credentials are wrong. {}", email, ctx.channel().remoteAddress());
            ctx.writeAndFlush(makeResponse(msgId, USER_NOT_AUTHENTICATED), ctx.voidPromise());
            return;
        }

        login(ctx, msgId, user, osType, version);
    }

    private void login(ChannelHandlerContext ctx, int messageId, User user, OsType osType, String version) {
        final ChannelPipeline pipeline = ctx.pipeline();
        cleanPipeline(pipeline);

        AppStateHolder appStateHolder = new AppStateHolder(user, osType, version);
        pipeline.addLast("AAppHandler", new AppHandler(holder, appStateHolder));

        final Channel channel = ctx.channel();

        user.lastLoggedIP = IPUtils.getIp(channel);
        user.lastLoggedAt = System.currentTimeMillis();

        //todo back compatibility code. remove in future.
        if (user.region == null || user.region.isEmpty()) {
            user.region = holder.region;
        }

        Session session = holder.sessionDao.getOrCreateSessionByUser(appStateHolder.userKey, channel.eventLoop());
        if (session.initialEventLoop != channel.eventLoop()) {
            log.debug("Re registering app channel. {}", ctx.channel());
            reRegisterChannel(ctx, session, channelFuture -> completeLogin(channelFuture.channel(), session, user, messageId));
        } else {
            completeLogin(channel, session, user, messageId);
        }
    }

    private void completeLogin(Channel channel, Session session, User user, int msgId) {
        session.addAppChannel(channel);
        channel.writeAndFlush(ok(msgId), channel.voidPromise());
        for (DashBoard dashBoard : user.profile.dashBoards) {
            if (dashBoard.isAppConnectedOn && dashBoard.isActive) {
                log.trace("{}-{}. Sending App Connected event to hardware.", user.email, user.appName);
                session.sendMessageToHardware(dashBoard.id, Command.LAPUTA_INTERNAL, 7777, "acon");
            }
        }
        log.info("{} {}-app joined.", user.email, user.appName);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        handleGeneralException(ctx, cause);
    }

}
