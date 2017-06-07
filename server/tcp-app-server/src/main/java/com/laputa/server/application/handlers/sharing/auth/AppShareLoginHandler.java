package com.laputa.server.application.handlers.sharing.auth;

import com.laputa.server.Holder;
import com.laputa.server.application.handlers.main.auth.AppLoginHandler;
import com.laputa.server.application.handlers.main.auth.GetServerHandler;
import com.laputa.server.application.handlers.main.auth.OsType;
import com.laputa.server.application.handlers.main.auth.RegisterHandler;
import com.laputa.server.application.handlers.sharing.AppShareHandler;
import com.laputa.server.core.dao.SharedTokenValue;
import com.laputa.server.core.model.DashBoard;
import com.laputa.server.core.model.auth.Session;
import com.laputa.server.core.model.auth.User;
import com.laputa.server.core.protocol.model.messages.appllication.sharing.ShareLoginMessage;
import com.laputa.server.handlers.DefaultReregisterHandler;
import com.laputa.server.handlers.common.UserNotLoggedHandler;
import io.netty.channel.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.laputa.utils.LaputaByteBufUtil.*;

/**
 * Handler responsible for managing apps sharing login messages.
 *
 * The Laputa Project.
 * Created by Sommer
 * Created on 2/1/2015.
 *
 */
@ChannelHandler.Sharable
public class AppShareLoginHandler extends SimpleChannelInboundHandler<ShareLoginMessage> implements DefaultReregisterHandler {

    private static final Logger log = LogManager.getLogger(AppShareLoginHandler.class);

    private final Holder holder;

    public AppShareLoginHandler(Holder holder) {
        this.holder = holder;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ShareLoginMessage message) throws Exception {
        //warn: split may be optimized
        String[] messageParts = message.body.split("\0");

        if (messageParts.length < 2) {
            log.error("Wrong income message format.");
            ctx.writeAndFlush(illegalCommand(message.id), ctx.voidPromise());
        } else {
            OsType osType = null;
            String version = null;
            String uid = null;
            if (messageParts.length > 3) {
                osType = OsType.parse(messageParts[2]);
                version = messageParts[3];
            }
            if (messageParts.length == 5) {
              uid = messageParts[4];
            }
            appLogin(ctx, message.id, messageParts[0], messageParts[1], osType, version, uid);
        }
    }

    private void appLogin(ChannelHandlerContext ctx, int messageId, String email, String token, OsType osType, String version, String uid) {
        String userName = email.toLowerCase();

        SharedTokenValue tokenValue = holder.tokenManager.getUserBySharedToken(token);

        if (tokenValue == null || !tokenValue.user.email.equals(userName)) {
            log.debug("Share token is invalid. User : {}, token {}, {}", userName, token, ctx.channel().remoteAddress());
            ctx.writeAndFlush(notAllowed(messageId), ctx.voidPromise());
            return;
        }

        final User user = tokenValue.user;
        final int dashId = tokenValue.dashId;

        DashBoard dash = user.profile.getDashById(dashId);
        if (!dash.isShared) {
            log.debug("Dashboard is not shared. User : {}, token {}, {}", userName, token, ctx.channel().remoteAddress());
            ctx.writeAndFlush(notAllowed(messageId), ctx.voidPromise());
            return;
        }

        cleanPipeline(ctx.pipeline());
        AppShareStateHolder appShareStateHolder = new AppShareStateHolder(user, osType, version, token, dashId);
        ctx.pipeline().addLast("AAppSHareHandler", new AppShareHandler(holder, appShareStateHolder));

        Session session = holder.sessionDao.getOrCreateSessionByUser(appShareStateHolder.userKey, ctx.channel().eventLoop());

        if (session.initialEventLoop != ctx.channel().eventLoop()) {
            log.debug("Re registering app channel. {}", ctx.channel());
            reRegisterChannel(ctx, session, channelFuture -> completeLogin(channelFuture.channel(), session, user.email, messageId));
        } else {
            completeLogin(ctx.channel(), session, user.email, messageId);
        }
    }

    private void completeLogin(Channel channel, Session session, String userName, int msgId) {
        session.addAppChannel(channel);
        channel.writeAndFlush(ok(msgId), channel.voidPromise());
        log.info("Shared {} app joined.", userName);
    }

    private void cleanPipeline(ChannelPipeline pipeline) {
        pipeline.remove(this);
        pipeline.remove(UserNotLoggedHandler.class);
        pipeline.remove(RegisterHandler.class);
        pipeline.remove(AppLoginHandler.class);
        pipeline.remove(GetServerHandler.class);
    }

}
