package com.laputa.server.application.handlers.main.auth;

import com.laputa.server.core.dao.TokenManager;
import com.laputa.server.core.dao.UserDao;
import com.laputa.server.core.model.AppName;
import com.laputa.server.core.protocol.model.messages.appllication.RegisterMessage;
import com.laputa.server.workers.timer.TimerWorker;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

/**
 * The Laputa Project.
 * Created by Sommer
 * Created on 10.08.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class RegisterHandlerTest {

    @Mock
    private ChannelHandlerContext ctx;

    @Mock
    private UserDao userDao;

    @Mock
    private TokenManager tokenManager;

    @Mock
    private TimerWorker timerWorker;

    @Mock
    private ByteBufAllocator allocator;

    @Mock
    private ByteBuf byteBuf;


    @Test
    public void testRegisterOk() throws Exception {
        RegisterHandler registerHandler = new RegisterHandler(userDao, tokenManager, timerWorker, null);

        String userName = "test@gmail.com";

        when(ctx.alloc()).thenReturn(allocator);
        when(allocator.ioBuffer(anyInt())).thenReturn(byteBuf);
        when(byteBuf.writeByte(anyInt())).thenReturn(byteBuf);
        when(byteBuf.writeShort(anyShort())).thenReturn(byteBuf);
        when(byteBuf.writeShort(anyShort())).thenReturn(byteBuf);

        when(userDao.isUserExists(userName, AppName.BLYNK)).thenReturn(false);
        registerHandler.channelRead0(ctx, new RegisterMessage(1, userName + "\0" + "1"));

        verify(userDao).add(eq(userName), eq("1"), eq(AppName.BLYNK));
    }

    @Test
    public void testRegisterOk2() throws Exception {
        RegisterHandler registerHandler = new RegisterHandler(userDao, tokenManager, timerWorker, null);

        String userName = "test@gmail.com";

        when(ctx.alloc()).thenReturn(allocator);
        when(allocator.ioBuffer(anyInt())).thenReturn(byteBuf);
        when(byteBuf.writeByte(anyInt())).thenReturn(byteBuf);
        when(byteBuf.writeShort(anyShort())).thenReturn(byteBuf);
        when(byteBuf.writeShort(anyShort())).thenReturn(byteBuf);

        when(userDao.isUserExists(userName, AppName.BLYNK)).thenReturn(false);
        registerHandler.channelRead0(ctx, new RegisterMessage(1, userName + "\0" + "1"));

        verify(userDao).add(eq(userName), eq("1"), eq(AppName.BLYNK));
    }

    @Test
    public void testAllowedUsersSingleUserWork() throws Exception {
        RegisterHandler registerHandler = new RegisterHandler(userDao, tokenManager, timerWorker, new String[] {"test@gmail.com"});

        String userName = "test@gmail.com";

        when(ctx.alloc()).thenReturn(allocator);
        when(allocator.ioBuffer(anyInt())).thenReturn(byteBuf);
        when(byteBuf.writeByte(anyInt())).thenReturn(byteBuf);
        when(byteBuf.writeShort(anyShort())).thenReturn(byteBuf);
        when(byteBuf.writeShort(anyShort())).thenReturn(byteBuf);

        when(userDao.isUserExists(userName, AppName.BLYNK)).thenReturn(false);
        registerHandler.channelRead0(ctx, new RegisterMessage(1, userName + "\0" + "1"));

        verify(userDao).add(eq(userName), eq("1"), eq(AppName.BLYNK));
    }

    @Test
    public void testAllowedUsersSingleUserNotWork() throws Exception {
        RegisterHandler registerHandler = new RegisterHandler(userDao, tokenManager, timerWorker, new String[] {"test@gmail.com"});

        String email = "test2@gmail.com";

        when(ctx.alloc()).thenReturn(allocator);
        when(allocator.ioBuffer(anyInt())).thenReturn(byteBuf);
        when(byteBuf.writeByte(anyInt())).thenReturn(byteBuf);
        when(byteBuf.writeShort(anyShort())).thenReturn(byteBuf);
        when(byteBuf.writeShort(anyShort())).thenReturn(byteBuf);

        when(userDao.isUserExists(email, AppName.BLYNK)).thenReturn(false);
        registerHandler.channelRead0(ctx, new RegisterMessage(1, email + "\0" + "1"));

        verify(userDao, times(0)).add(eq(email), eq("1"), eq(AppName.BLYNK));
        //verify(ctx).writeAndFlush(eq(new ResponseMessage(1, NOT_ALLOWED)), any());
    }

    @Test
    public void testAllowedUsersSingleUserWork2() throws Exception {
        RegisterHandler registerHandler = new RegisterHandler(userDao, tokenManager, timerWorker, new String[] {"test@gmail.com", "test2@gmail.com"});

        String userName = "test2@gmail.com";

        when(ctx.alloc()).thenReturn(allocator);
        when(allocator.ioBuffer(anyInt())).thenReturn(byteBuf);
        when(byteBuf.writeByte(anyInt())).thenReturn(byteBuf);
        when(byteBuf.writeShort(anyShort())).thenReturn(byteBuf);
        when(byteBuf.writeShort(anyShort())).thenReturn(byteBuf);

        when(userDao.isUserExists(userName, AppName.BLYNK)).thenReturn(false);
        registerHandler.channelRead0(ctx, new RegisterMessage(1, userName + "\0" + "1"));

        verify(userDao).add(eq(userName), eq("1"), eq(AppName.BLYNK));
    }

}
