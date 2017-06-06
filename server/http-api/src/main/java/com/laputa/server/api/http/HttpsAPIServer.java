package com.laputa.server.api.http;

import com.laputa.server.Holder;
import com.laputa.server.api.http.handlers.HttpAndWebSocketUnificatorHandler;
import com.laputa.server.api.http.handlers.LetsEncryptHandler;
import com.laputa.server.core.BaseServer;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerKeepAliveHandler;

/**
 * The Laputa Project.
 * Created by Sommer
 * Created on 1/12/2015.
 */
public class HttpsAPIServer extends BaseServer {

    private final ChannelInitializer<SocketChannel> channelInitializer;

    public HttpsAPIServer(Holder holder, boolean isUnpacked) {
        super(holder.props.getProperty("listen.address"), holder.props.getIntProperty("https.port"), holder.transportTypeHolder);

        String adminRootPath = holder.props.getProperty("admin.rootPath", "/admin");

        final HttpAndWebSocketUnificatorHandler httpAndWebSocketUnificatorHandler =
                new HttpAndWebSocketUnificatorHandler(holder, port, adminRootPath, isUnpacked);
        final LetsEncryptHandler letsEncryptHandler = new LetsEncryptHandler(holder.sslContextHolder.contentHolder);

        channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline()
                .addLast("HttpsSslContext", holder.sslContextHolder.sslCtx.newHandler(ch.alloc()))
                .addLast("HttpsServerCodec", new HttpServerCodec())
                .addLast("HttpsServerKeepAlive", new HttpServerKeepAliveHandler())
                .addLast("HttpsObjectAggregator", new HttpObjectAggregator(65536, true))
                .addLast(letsEncryptHandler)
                .addLast("HttpsWebSocketUnificator", httpAndWebSocketUnificatorHandler);
            }
        };
    }

    @Override
    public ChannelInitializer<SocketChannel> getChannelInitializer() {
        return channelInitializer;
    }

    @Override
    protected String getServerName() {
        return "HTTPS API, WebSockets and Admin page";
    }

    @Override
    public void close() {
        System.out.println("Shutting down HTTPS API, WebSockets and Admin server...");
        super.close();
    }

}
