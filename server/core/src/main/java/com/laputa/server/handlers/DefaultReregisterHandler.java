package com.laputa.server.handlers;

import com.laputa.server.core.model.auth.Session;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;

/**
 * The Laputa Project.
 * Created by Sommer
 * Created on 17.10.15.
 */
public interface DefaultReregisterHandler {

    default void reRegisterChannel(ChannelHandlerContext ctx, Session session, ChannelFutureListener completeHandler) {
        ChannelFuture cf = ctx.deregister();
        cf.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                session.initialEventLoop.register(channelFuture.channel()).addListener(completeHandler);
            }
        });
    }

}
