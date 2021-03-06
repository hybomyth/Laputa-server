package com.laputa.client;

import com.laputa.client.core.AppClient;
import com.laputa.client.core.HardwareClient;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.BufferedReader;

import static com.laputa.client.ClientLauncher.*;
import static org.mockito.Mockito.when;

/**
 * The Laputa Project.
 * Created by Sommer
 * Created on 11.03.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class ClientTest {

    @Mock
    public BufferedReader bufferedReader;

    @Test
    @Ignore
    public void testQuitApp() throws Exception {
        AppClient testAppClient = new AppClient(DEFAULT_HOST, DEFAULT_APPLICATION_PORT);
        when(bufferedReader.readLine()).thenReturn("quit");
        testAppClient.start(bufferedReader);
        //verify(testAppClient.responseMock, never()).channelRead(any(), any());
    }

    @Test
    @Ignore
    public void testQuitHard() throws Exception {
        HardwareClient testHardClient = new HardwareClient(DEFAULT_HOST, DEFAULT_HARDWARE_PORT);
        when(bufferedReader.readLine()).thenReturn("quit");
        testHardClient.start(bufferedReader);
        //verify(testHardClient.responseMock, never()).channelRead(any(), any());
    }

}
