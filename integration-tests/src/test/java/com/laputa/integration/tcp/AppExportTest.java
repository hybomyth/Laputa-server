package com.laputa.integration.tcp;

import com.laputa.integration.IntegrationBase;
import com.laputa.integration.model.tcp.ClientPair;
import com.laputa.integration.model.tcp.TestAppClient;
import com.laputa.integration.model.tcp.TestHardClient;
import com.laputa.server.api.http.HttpAPIServer;
import com.laputa.server.application.AppServer;
import com.laputa.server.core.BaseServer;
import com.laputa.server.core.model.DashBoard;
import com.laputa.server.core.model.auth.App;
import com.laputa.server.core.model.device.Device;
import com.laputa.server.core.protocol.model.messages.ResponseMessage;
import com.laputa.server.core.protocol.model.messages.common.HardwareConnectedMessage;
import com.laputa.server.core.protocol.model.messages.common.HardwareMessage;
import com.laputa.server.hardware.HardwareServer;
import com.laputa.utils.JsonParser;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static com.laputa.server.core.protocol.enums.Response.DEVICE_NOT_IN_NETWORK;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * The Laputa Project.
 * Created by Sommer
 * Created on 5/09/2016.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class AppExportTest extends IntegrationBase {

    private BaseServer httpServer;
    private BaseServer appServer;
    private BaseServer hardwareServer;
    private AsyncHttpClient httpclient;
    private String httpServerUrl;
    private ClientPair clientPair;

    @Before
    public void init() throws Exception {
        httpServer = new HttpAPIServer(holder).start();
        hardwareServer = new HardwareServer(holder).start();
        appServer = new AppServer(holder).start();
        httpServerUrl = String.format("http://localhost:%s/", httpPort);

        httpclient = new DefaultAsyncHttpClient(
                new DefaultAsyncHttpClientConfig.Builder()
                        .setUserAgent("")
                        .setKeepAlive(true)
                        .build());

        if (clientPair == null) {
            clientPair = initAppAndHardPair(tcpAppPort, tcpHardPort, properties);
        }
        clientPair.hardwareClient.reset();
        clientPair.appClient.reset();
    }

    @After
    public void shutdown() {
        httpServer.close();
        appServer.close();
        hardwareServer.close();
        clientPair.stop();
    }

    @Test
    public void testExportedAppFlowWithOneDynamicTest() throws Exception {
        clientPair.appClient.send("createApp {\"theme\":\"Laputa\",\"provisionType\":\"DYNAMIC\",\"color\":0,\"name\":\"My App\",\"icon\":\"myIcon\",\"projectIds\":[1]}");
        App app = JsonParser.parseApp(clientPair.appClient.getBody());
        assertNotNull(app);
        assertNotNull(app.id);

        TestAppClient appClient2 = new TestAppClient("localhost", tcpAppPort, properties);
        appClient2.start();

        appClient2.send("register test@laputa.cc a " + app.id);
        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(ok(1)));

        appClient2.send("login test@laputa.cc a Android 1.10.4 " + app.id);
        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(ok(2)));

        appClient2.send("loadProfileGzipped 1");
        DashBoard dashBoard = JsonParser.parseDashboard(appClient2.getBody(3));
        assertNotNull(dashBoard);

        Device device = dashBoard.devices[0];
        assertNotNull(device);
        assertNotNull(device.token);

        TestHardClient hardClient1 = new TestHardClient("localhost", tcpHardPort);
        hardClient1.start();

        hardClient1.send("login " + device.token);
        verify(hardClient1.responseMock, timeout(2000)).channelRead(any(), eq(ok(1)));
        verify(appClient2.responseMock, timeout(2000)).channelRead(any(), eq(new HardwareConnectedMessage(1, "1-0")));

        hardClient1.send("hardware vw 1 100");
        verify(appClient2.responseMock, timeout(2000)).channelRead(any(), eq(new HardwareMessage(2, b("1 vw 1 100"))));
    }

    private String workflowForUser(TestAppClient appClient, String email, String pass, String appName) throws Exception{
        appClient.send("register " + email + " " + pass + " " + appName);
        verify(appClient.responseMock, timeout(1000)).channelRead(any(), eq(ok(1)));
        appClient.send("login " + email + " " + pass + " Android 1.10.4 " + appName);
        //we should wait until login finished. Only after that we can send commands
        verify(appClient.responseMock, timeout(1000)).channelRead(any(), eq(ok(2)));

        DashBoard dash = new DashBoard();
        dash.id = 1;
        dash.name = "test";
        appClient.send("createDash " + dash.toString());
        verify(appClient.responseMock, timeout(1000)).channelRead(any(), eq(ok(3)));
        appClient.send("activate 1");
        verify(appClient.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(4, DEVICE_NOT_IN_NETWORK)));

        appClient.reset();
        appClient.send("getToken 1");

        String token = appClient.getBody();
        assertNotNull(token);
        return token;
    }

}
