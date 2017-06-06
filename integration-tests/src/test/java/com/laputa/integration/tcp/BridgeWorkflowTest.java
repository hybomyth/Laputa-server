package com.laputa.integration.tcp;

import com.laputa.integration.IntegrationBase;
import com.laputa.integration.model.tcp.ClientPair;
import com.laputa.integration.model.tcp.TestHardClient;
import com.laputa.server.application.AppServer;
import com.laputa.server.core.BaseServer;
import com.laputa.server.core.protocol.model.messages.ResponseMessage;
import com.laputa.server.core.protocol.model.messages.StringMessage;
import com.laputa.server.hardware.HardwareServer;
import com.laputa.utils.TokenGeneratorUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static com.laputa.server.core.protocol.enums.Command.*;
import static com.laputa.server.core.protocol.enums.Response.*;
import static com.laputa.server.core.protocol.model.messages.MessageFactory.produce;
import static org.mockito.Mockito.*;

/**
 * The Laputa Project.
 * Created by Sommer
 * Created on 2/2/2015.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class BridgeWorkflowTest extends IntegrationBase {

    private BaseServer appServer;
    private BaseServer hardwareServer;
    private ClientPair clientPair;
    private String token = TokenGeneratorUtil.generateNewToken();

    @Before
    public void init() throws Exception {
        this.hardwareServer = new HardwareServer(holder).start();
        this.appServer = new AppServer(holder).start();
        this.clientPair = initAppAndHardPair("user_profile_json_3_dashes.txt");
    }

    @After
    public void shutdown() {
        this.appServer.close();
        this.hardwareServer.close();
        this.clientPair.stop();
    }

    @Test
    public void testBridgeInitOk() throws Exception {
        clientPair.hardwareClient.send("bridge 1 i " + token);
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));
    }

    @Test
    public void testBridgeInitIllegalCommand() throws Exception {
        clientPair.hardwareClient.send("bridge 1 i");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, ILLEGAL_COMMAND)));

        clientPair.hardwareClient.send("bridge i");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, ILLEGAL_COMMAND)));

        clientPair.hardwareClient.send("bridge 1 auth_tone");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, ILLEGAL_COMMAND)));

        clientPair.hardwareClient.send("bridge 1");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(4, ILLEGAL_COMMAND)));

        clientPair.hardwareClient.send("bridge 1");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(5, ILLEGAL_COMMAND)));
    }

    @Test
    public void testSeveralBridgeInitOk() throws Exception {
        clientPair.hardwareClient.send("bridge 1 i " + token);
        clientPair.hardwareClient.send("bridge 2 i " + token);
        clientPair.hardwareClient.send("bridge 3 i " + token);
        clientPair.hardwareClient.send("bridge 4 i " + token);
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, OK)));
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(4, OK)));

        clientPair.hardwareClient.send("bridge 5 i " + token);
        clientPair.hardwareClient.send("bridge 5 i " + token);
        clientPair.hardwareClient.send("bridge 5 i " + token);
        clientPair.hardwareClient.send("bridge 5 i " + token);
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(5, OK)));
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(6, OK)));
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(7, OK)));
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(8, OK)));
    }

    @Test
    public void testBridgeInitAndOk() throws Exception {
        clientPair.hardwareClient.send("bridge 1 i " + token);
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));
    }

    @Test
    public void testBridgeWithoutInit() throws Exception {
        clientPair.hardwareClient.send("bridge 1 aw 10 10");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, NOT_ALLOWED)));
    }

    @Test
    public void testBridgeInitAndSendNoOtherDevices() throws Exception {
        clientPair.hardwareClient.send("bridge 1 i " + token);
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.hardwareClient.send("bridge 1 aw 10 10");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, DEVICE_NOT_IN_NETWORK)));
    }

    @Test
    public void testBridgeInitAndSendOtherDevicesButNoBridgeDevices() throws Exception {
        //creating 1 new hard client
        TestHardClient hardClient1 = new TestHardClient("localhost", tcpHardPort);
        hardClient1.start();
        hardClient1.send("login " + clientPair.token);
        verify(hardClient1.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(1, OK)));
        hardClient1.reset();

        clientPair.hardwareClient.send("bridge 1 i " + token);
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));
        clientPair.hardwareClient.send("bridge 1 aw 10 10");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, DEVICE_NOT_IN_NETWORK)));
    }

    @Test
    public void testSecondTokenNotInitialized() throws Exception {
        clientPair.hardwareClient.send("bridge 1 i " + token);
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));
        clientPair.hardwareClient.send("bridge 2 aw 10 10");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, NOT_ALLOWED)));
    }

    @Test
    public void testCorrectWorkflow2HardsSameToken() throws Exception {
        //creating 1 new hard client
        TestHardClient hardClient1 = new TestHardClient("localhost", tcpHardPort);
        hardClient1.start();
        hardClient1.send("login " + clientPair.token);
        verify(hardClient1.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(1, OK)));
        hardClient1.reset();

        clientPair.hardwareClient.send("bridge 1 i " + clientPair.token);
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));
        clientPair.hardwareClient.send("bridge 1 aw 10 10");
        verify(hardClient1.responseMock, timeout(500)).channelRead(any(), eq(produce(2, BRIDGE, b("aw 10 10"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, HARDWARE, b("1 aw 10 10"))));
    }

    @Test
    public void testCorrectWorkflow2HardsDifferentToken() throws Exception {
        clientPair.appClient.send("getToken 2");

        ArgumentCaptor<Object> objectArgumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(clientPair.appClient.responseMock, timeout(2000).times(1)).channelRead(any(), objectArgumentCaptor.capture());

        List<Object> arguments = objectArgumentCaptor.getAllValues();
        String token2 = ((StringMessage) arguments.get(0)).body;

        clientPair.appClient.send("activate 2");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, DEVICE_NOT_IN_NETWORK)));
        clientPair.appClient.reset();

        //creating 1 new hard client
        TestHardClient hardClient1 = new TestHardClient("localhost", tcpHardPort);
        hardClient1.start();
        hardClient1.send("login " + token2);
        verify(hardClient1.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(1, OK)));
        hardClient1.reset();

        clientPair.hardwareClient.send("bridge 1 i " + token2);
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));
        clientPair.hardwareClient.send("bridge 1 aw 11 11");
        verify(hardClient1.responseMock, timeout(500)).channelRead(any(), eq(produce(2, BRIDGE, b("aw 11 11"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE_CONNECTED, "2-0")));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, HARDWARE, b("2 aw 11 11"))));
    }

    @Test
    public void testCorrectWorkflow3HardsDifferentToken() throws Exception {
        clientPair.appClient.send("getToken 2");

        ArgumentCaptor<Object> objectArgumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(clientPair.appClient.responseMock, timeout(2000).times(1)).channelRead(any(), objectArgumentCaptor.capture());

        List<Object> arguments = objectArgumentCaptor.getAllValues();
        String token2 = ((StringMessage) arguments.get(0)).body;
        clientPair.appClient.reset();

        //creating 2 new hard clients
        TestHardClient hardClient1 = new TestHardClient("localhost", tcpHardPort);
        hardClient1.start();
        hardClient1.send("login " + token2);
        verify(hardClient1.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(1, OK)));
        hardClient1.reset();

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();
        hardClient2.send("login " + token2);
        verify(hardClient2.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(1, OK)));
        hardClient2.reset();


        clientPair.hardwareClient.send("bridge 1 i " + token2);
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.hardwareClient.send("bridge 1 aw 11 11");
        verify(hardClient1.responseMock, timeout(500)).channelRead(any(), eq(produce(2, BRIDGE, b("aw 11 11"))));
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(produce(2, BRIDGE, b("aw 11 11"))));

        verify(clientPair.appClient.responseMock, timeout(500).times(2)).channelRead(any(), eq(produce(1, HARDWARE_CONNECTED, "2-0")));
        verify(clientPair.appClient.responseMock, timeout(500).times(0)).channelRead(any(), eq(produce(2, HARDWARE, b("2 aw 11 11"))));
    }

    @Test
    public void testCorrectWorkflow4HardsDifferentToken() throws Exception {
        clientPair.appClient.send("getToken 2");
        clientPair.appClient.send("getToken 3");

        ArgumentCaptor<Object> objectArgumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(clientPair.appClient.responseMock, timeout(2000).times(2)).channelRead(any(), objectArgumentCaptor.capture());

        List<Object> arguments = objectArgumentCaptor.getAllValues();
        String token2 = ((StringMessage) arguments.get(0)).body;
        String token3 = ((StringMessage) arguments.get(1)).body;

        //creating 2 new hard clients
        TestHardClient hardClient1 = new TestHardClient("localhost", tcpHardPort);
        hardClient1.start();
        hardClient1.send("login " + token2);
        verify(hardClient1.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(1, OK)));
        hardClient1.reset();

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();
        hardClient2.send("login " + token2);
        verify(hardClient2.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(1, OK)));
        hardClient2.reset();

        TestHardClient hardClient3 = new TestHardClient("localhost", tcpHardPort);
        hardClient3.start();
        hardClient3.send("login " + token3);
        verify(hardClient3.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(1, OK)));
        hardClient3.reset();


        clientPair.hardwareClient.send("bridge 1 i " + token2);
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));
        clientPair.hardwareClient.send("bridge 2 i " + token3);
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));


        clientPair.hardwareClient.send("bridge 1 aw 11 11");
        verify(hardClient1.responseMock, timeout(500)).channelRead(any(), eq(produce(3, BRIDGE, b("aw 11 11"))));
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(produce(3, BRIDGE, b("aw 11 11"))));

        clientPair.hardwareClient.send("bridge 2 aw 13 13");
        verify(hardClient3.responseMock, timeout(500)).channelRead(any(), eq(produce(4, BRIDGE, b("aw 13 13"))));
    }

    @Test
    public void testCorrectWorkflow3HardsDifferentTokenAndSync() throws Exception {
        clientPair.appClient.send("getToken 2");

        ArgumentCaptor<Object> objectArgumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(clientPair.appClient.responseMock, timeout(2000).times(1)).channelRead(any(), objectArgumentCaptor.capture());

        List<Object> arguments = objectArgumentCaptor.getAllValues();
        String token2 = ((StringMessage) arguments.get(0)).body;
        clientPair.appClient.reset();

        //creating 2 new hard clients
        TestHardClient hardClient1 = new TestHardClient("localhost", tcpHardPort);
        hardClient1.start();
        hardClient1.send("login " + token2);
        verify(hardClient1.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(1, OK)));
        hardClient1.reset();

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();
        hardClient2.send("login " + token2);
        verify(hardClient2.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(1, OK)));
        hardClient2.reset();


        clientPair.hardwareClient.send("bridge 1 i " + token2);
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.hardwareClient.send("bridge 1 aw 11 11");
        verify(hardClient1.responseMock, timeout(500)).channelRead(any(), eq(produce(2, BRIDGE, b("aw 11 11"))));
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(produce(2, BRIDGE, b("aw 11 11"))));

        verify(clientPair.appClient.responseMock, timeout(500).times(2)).channelRead(any(), eq(produce(1, HARDWARE_CONNECTED, "2-0")));
        verify(clientPair.appClient.responseMock, timeout(500).times(0)).channelRead(any(), eq(produce(2, HARDWARE, b("2 aw 11 11"))));


        hardClient1.send("hardsync ar 11");
        verify(hardClient1.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 11 11"))));
        hardClient2.send("hardsync ar 11");
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 11 11"))));
    }

    @Test
    public void testCorrectWorkflow4HardsDifferentTokenAndSync() throws Exception {
        clientPair.appClient.send("getToken 2");
        clientPair.appClient.send("getToken 3");

        ArgumentCaptor<Object> objectArgumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(clientPair.appClient.responseMock, timeout(2000).times(2)).channelRead(any(), objectArgumentCaptor.capture());

        List<Object> arguments = objectArgumentCaptor.getAllValues();
        String token2 = ((StringMessage) arguments.get(0)).body;
        String token3 = ((StringMessage) arguments.get(1)).body;

        //creating 2 new hard clients
        TestHardClient hardClient1 = new TestHardClient("localhost", tcpHardPort);
        hardClient1.start();
        hardClient1.send("login " + token2);
        verify(hardClient1.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(1, OK)));
        hardClient1.reset();

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();
        hardClient2.send("login " + token2);
        verify(hardClient2.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(1, OK)));
        hardClient2.reset();

        TestHardClient hardClient3 = new TestHardClient("localhost", tcpHardPort);
        hardClient3.start();
        hardClient3.send("login " + token3);
        verify(hardClient3.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(1, OK)));
        hardClient3.reset();


        clientPair.hardwareClient.send("bridge 1 i " + token2);
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));
        clientPair.hardwareClient.send("bridge 2 i " + token3);
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));


        clientPair.hardwareClient.send("bridge 1 vw 11 12");
        verify(hardClient1.responseMock, timeout(500)).channelRead(any(), eq(produce(3, BRIDGE, b("vw 11 12"))));
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(produce(3, BRIDGE, b("vw 11 12"))));

        clientPair.hardwareClient.send("bridge 2 aw 13 13");
        verify(hardClient3.responseMock, timeout(500)).channelRead(any(), eq(produce(4, BRIDGE, b("aw 13 13"))));

        hardClient1.send("hardsync vr 11");
        verify(hardClient1.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("vw 11 12"))));
        hardClient2.send("hardsync vr 11");
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("vw 11 12"))));
        hardClient3.send("hardsync ar 13");
        verify(hardClient3.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 13 13"))));
        hardClient3.send("hardsync ar 13");
        verify(hardClient3.responseMock, never()).channelRead(any(), eq(produce(2, HARDWARE, b("aw 13 13"))));
    }

}
