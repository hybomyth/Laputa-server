package com.laputa.integration.tcp;

import com.laputa.integration.IntegrationBase;
import com.laputa.integration.model.tcp.ClientPair;
import com.laputa.integration.model.tcp.TestAppClient;
import com.laputa.integration.model.tcp.TestHardClient;
import com.laputa.server.application.AppServer;
import com.laputa.server.core.BaseServer;
import com.laputa.server.core.dao.ReportingDao;
import com.laputa.server.core.model.device.Device;
import com.laputa.server.core.model.device.Status;
import com.laputa.server.core.model.enums.GraphType;
import com.laputa.server.core.model.enums.PinType;
import com.laputa.server.core.protocol.model.messages.BinaryMessage;
import com.laputa.server.core.protocol.model.messages.ResponseMessage;
import com.laputa.server.core.protocol.model.messages.appllication.CreateDevice;
import com.laputa.server.core.protocol.model.messages.appllication.sharing.AppSyncMessage;
import com.laputa.server.core.protocol.model.messages.common.HardwareConnectedMessage;
import com.laputa.server.core.protocol.model.messages.common.HardwareMessage;
import com.laputa.server.hardware.HardwareServer;
import com.laputa.utils.ByteUtils;
import com.laputa.utils.FileUtils;
import com.laputa.utils.JsonParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.runners.MockitoJUnitRunner;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.laputa.server.core.protocol.enums.Command.HARDWARE;
import static com.laputa.server.core.protocol.enums.Response.OK;
import static com.laputa.server.core.protocol.model.messages.MessageFactory.produce;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * The Laputa Project.
 * Created by Sommer
 * Created on 2/2/2015.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class DeviceSelectorWorkflowTest extends IntegrationBase {

    private BaseServer appServer;
    private BaseServer hardwareServer;
    private ClientPair clientPair;

    private static void assertEqualDevice(Device expected, Device real) {
        assertEquals(expected.id, real.id);
        //assertEquals(expected.name, real.name);
        assertEquals(expected.boardType, real.boardType);
        assertNotNull(real.token);
        assertEquals(expected.status, real.status);
    }

    @Before
    public void init() throws Exception {
        this.hardwareServer = new HardwareServer(holder).start();
        this.appServer = new AppServer(holder).start();

        this.clientPair = initAppAndHardPair();
    }

    @After
    public void shutdown() {
        this.appServer.close();
        this.hardwareServer.close();
        this.clientPair.stop();
    }

    @Test
    public void testSendHardwareCommandViaDeviceSelector() throws Exception {
        Device device0 = new Device(0, "My Dashboard", "UNO");
        device0.status = Status.ONLINE;
        Device device1 = new Device(1, "My Device", "ESP8266");
        device1.status = Status.OFFLINE;

        clientPair.appClient.send("createDevice 1\0" + device1.toString());
        String createdDevice = clientPair.appClient.getBody();
        Device device = JsonParser.parseDevice(createdDevice);
        assertNotNull(device);
        assertNotNull(device.token);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new CreateDevice(1, device.toString())));

        clientPair.appClient.send("createWidget 1\0{\"id\":200000, \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"DEVICE_SELECTOR\"}");
        clientPair.appClient.send("createWidget 1\0{\"id\":88, \"width\":1, \"height\":1, \"deviceId\":200000, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"BUTTON\", \"pinType\":\"VIRTUAL\", \"pin\":88}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, OK)));

        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        String response = clientPair.appClient.getBody();

        Device[] devices = JsonParser.mapper.readValue(response, Device[].class);
        assertNotNull(devices);
        assertEquals(2, devices.length);

        assertEqualDevice(device0, devices[0]);
        assertEqualDevice(device1, devices[1]);

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();

        hardClient2.send("login " + devices[1].token);
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));
        device1.status = Status.ONLINE;

        clientPair.appClient.send("hardware 1-200000 vw 88 1");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new HardwareMessage(2, b("vw 88 1"))));
        verify(hardClient2.responseMock, never()).channelRead(any(), eq(new HardwareMessage(2, b("vw 88 1"))));

        //change device
        clientPair.appClient.send("hardware 1 vu 200000 1");
        verify(clientPair.hardwareClient.responseMock, never()).channelRead(any(), eq(new HardwareMessage(3, b("vu 200000 1"))));
        verify(hardClient2.responseMock, never()).channelRead(any(), eq(new HardwareMessage(3, b("vu 200000 1"))));

        clientPair.appClient.send("hardware 1-200000 vw 88 2");
        verify(clientPair.hardwareClient.responseMock, never()).channelRead(any(), eq(new HardwareMessage(4, b("vw 88 2"))));
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(new HardwareMessage(4, b("vw 88 2"))));

        //change device back
        clientPair.appClient.send("hardware 1 vu 200000 0");
        verify(clientPair.hardwareClient.responseMock, never()).channelRead(any(), eq(new HardwareMessage(5, b("vu 200000 0"))));
        verify(hardClient2.responseMock, never()).channelRead(any(), eq(new HardwareMessage(5, b("vu 200000 0"))));

        clientPair.appClient.send("hardware 1-200000 vw 88 0");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new HardwareMessage(6, b("vw 88 0"))));
        verify(hardClient2.responseMock, never()).channelRead(any(), eq(new HardwareMessage(6, b("vw 88 0"))));
    }

    @Test
    public void testSendHardwareCommandViaDeviceSelectorInSharedApp() throws Exception {
        Device device0 = new Device(0, "My Dashboard", "UNO");
        device0.status = Status.ONLINE;
        Device device1 = new Device(1, "My Device", "ESP8266");
        device1.status = Status.OFFLINE;

        clientPair.appClient.send("createDevice 1\0" + device1.toString());
        String createdDevice = clientPair.appClient.getBody();
        Device device = JsonParser.parseDevice(createdDevice);
        assertNotNull(device);
        assertNotNull(device.token);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new CreateDevice(1, device.toString())));

        clientPair.appClient.send("createWidget 1\0{\"id\":200000, \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"DEVICE_SELECTOR\"}");
        clientPair.appClient.send("createWidget 1\0{\"id\":88, \"width\":1, \"height\":1, \"deviceId\":200000, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"BUTTON\", \"pinType\":\"VIRTUAL\", \"pin\":88}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, OK)));

        clientPair.appClient.send("getShareToken 1");

        String sharedToken = clientPair.appClient.getBody(4);
        assertNotNull(sharedToken);
        assertEquals(32, sharedToken.length());

        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        String response = clientPair.appClient.getBody();

        Device[] devices = JsonParser.mapper.readValue(response, Device[].class);
        assertNotNull(devices);
        assertEquals(2, devices.length);

        assertEqualDevice(device0, devices[0]);
        assertEqualDevice(device1, devices[1]);

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();

        hardClient2.send("login " + devices[1].token);
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));
        device1.status = Status.ONLINE;
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new HardwareConnectedMessage(1, "1-1")));


        //login with shared app
        TestAppClient appClient2 = new TestAppClient("localhost", tcpAppPort, properties);
        appClient2.start();
        appClient2.send("shareLogin " + "dima@mail.ua " + sharedToken + " Android 24");
        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        appClient2.send("hardware 1-200000 vw 88 1");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new HardwareMessage(2, b("vw 88 1"))));
        verify(hardClient2.responseMock, never()).channelRead(any(), eq(new HardwareMessage(2, b("vw 88 1"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new AppSyncMessage(2, b("1-200000 vw 88 1"))));

        //change device
        appClient2.send("hardware 1 vu 200000 1");
        verify(clientPair.hardwareClient.responseMock, never()).channelRead(any(), eq(new HardwareMessage(3, b("vu 200000 1"))));
        verify(hardClient2.responseMock, never()).channelRead(any(), eq(new HardwareMessage(3, b("vu 200000 1"))));
        verify(clientPair.appClient.responseMock, never()).channelRead(any(), eq(new AppSyncMessage(3, b("1 vu 200000 1"))));

        appClient2.send("hardware 1-200000 vw 88 2");
        verify(clientPair.hardwareClient.responseMock, never()).channelRead(any(), eq(new HardwareMessage(4, b("vw 88 2"))));
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(new HardwareMessage(4, b("vw 88 2"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new AppSyncMessage(4, b("1-200000 vw 88 2"))));

        //change device back
        appClient2.send("hardware 1 vu 200000 0");
        verify(clientPair.hardwareClient.responseMock, never()).channelRead(any(), eq(new HardwareMessage(5, b("vu 200000 0"))));
        verify(hardClient2.responseMock, never()).channelRead(any(), eq(new HardwareMessage(5, b("vu 200000 0"))));
        verify(clientPair.appClient.responseMock, never()).channelRead(any(), eq(new AppSyncMessage(4, b("1 vu 200000 0"))));

        appClient2.send("hardware 1-200000 vw 88 0");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new HardwareMessage(6, b("vw 88 0"))));
        verify(hardClient2.responseMock, never()).channelRead(any(), eq(new HardwareMessage(6, b("vw 88 0"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new AppSyncMessage(6, b("1-200000 vw 88 0"))));
    }

    @Test
    public void testGetHistoryGraphDataForDeviceSelector() throws Exception {
        Device device0 = new Device(0, "My Dashboard", "UNO");
        device0.status = Status.ONLINE;
        Device device1 = new Device(1, "My Device", "ESP8266");
        device1.status = Status.OFFLINE;

        clientPair.appClient.send("createDevice 1\0" + device1.toString());
        String createdDevice = clientPair.appClient.getBody();
        Device device = JsonParser.parseDevice(createdDevice);
        assertNotNull(device);
        assertNotNull(device.token);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new CreateDevice(1, device.toString())));

        clientPair.appClient.send("createWidget 1\0{\"id\":200000, \"width\":1, \"height\":1, \"value\":0, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"DEVICE_SELECTOR\"}");
        clientPair.appClient.send("createWidget 1\0{\"id\":88, \"width\":1, \"height\":1, \"deviceId\":200000, \"x\":0, \"y\":0, \"label\":\"Button\", \"type\":\"BUTTON\", \"pinType\":\"VIRTUAL\", \"pin\":88}");
        clientPair.appClient.send("createWidget 1\0{\"id\":89, \"width\":1, \"height\":1, \"deviceId\":200000, \"x\":0, \"y\":0, \"label\":\"Display\", \"type\":\"DIGIT4_DISPLAY\", \"pinType\":\"VIRTUAL\", \"pin\":89}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, OK)));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(4, OK)));

        clientPair.appClient.send("getDevices 1");
        String response = clientPair.appClient.getBody(5);

        clientPair.appClient.reset();

        Device[] devices = JsonParser.mapper.readValue(response, Device[].class);
        assertNotNull(devices);
        assertEquals(2, devices.length);

        assertEqualDevice(device0, devices[0]);
        assertEqualDevice(device1, devices[1]);

        String tempDir = holder.props.getProperty("data.folder");

        final Path userReportFolder = Paths.get(tempDir, "data", DEFAULT_TEST_USER);
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }

        Path pinReportingDataPath = Paths.get(tempDir, "data", DEFAULT_TEST_USER, ReportingDao.generateFilename(1, 0, PinType.DIGITAL.pintTypeChar, (byte) 8, GraphType.HOURLY));
        Path pinReportingDataPath2 = Paths.get(tempDir, "data", DEFAULT_TEST_USER, ReportingDao.generateFilename(1, 1, PinType.DIGITAL.pintTypeChar, (byte) 8, GraphType.HOURLY));

        FileUtils.write(pinReportingDataPath, 1.11D, 1111111);
        FileUtils.write(pinReportingDataPath, 1.22D, 2222222);

        FileUtils.write(pinReportingDataPath2, 3D, 33);
        FileUtils.write(pinReportingDataPath2, 4D, 44);

        clientPair.appClient.send("getgraphdata 1-200000 d 8 24 h");

        ArgumentCaptor<BinaryMessage> objectArgumentCaptor = ArgumentCaptor.forClass(BinaryMessage.class);
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), objectArgumentCaptor.capture());
        BinaryMessage graphDataResponse = objectArgumentCaptor.getValue();

        assertNotNull(graphDataResponse);
        byte[] decompressedGraphData = ByteUtils.decompress(graphDataResponse.getBytes());
        ByteBuffer bb = ByteBuffer.wrap(decompressedGraphData);

        assertEquals(1, bb.getInt());
        assertEquals(2, bb.getInt());
        assertEquals(1.11D, bb.getDouble(), 0.1);
        assertEquals(1111111, bb.getLong());
        assertEquals(1.22D, bb.getDouble(), 0.1);
        assertEquals(2222222, bb.getLong());

        //changing device
        clientPair.appClient.send("hardware 1 vu 200000 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        clientPair.appClient.reset();

        clientPair.appClient.send("getgraphdata 1-200000 d 8 24 h");

        objectArgumentCaptor = ArgumentCaptor.forClass(BinaryMessage.class);
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), objectArgumentCaptor.capture());
        graphDataResponse = objectArgumentCaptor.getValue();

        assertNotNull(graphDataResponse);
        decompressedGraphData = ByteUtils.decompress(graphDataResponse.getBytes());
        bb = ByteBuffer.wrap(decompressedGraphData);

        assertEquals(1, bb.getInt());
        assertEquals(2, bb.getInt());
        assertEquals(3D, bb.getDouble(), 0.1);
        assertEquals(33, bb.getLong());
        assertEquals(4D, bb.getDouble(), 0.1);
        assertEquals(44, bb.getLong());
    }

    @Test
    public void testBasicSelectorWorkflow() throws Exception {
        Device device0 = new Device(0, "My Dashboard", "UNO");
        device0.status = Status.ONLINE;
        Device device1 = new Device(1, "My Device", "ESP8266");
        device1.status = Status.OFFLINE;

        clientPair.appClient.send("createDevice 1\0" + device1.toString());
        String createdDevice = clientPair.appClient.getBody();
        Device device = JsonParser.parseDevice(createdDevice);
        assertNotNull(device);
        assertNotNull(device.token);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new CreateDevice(1, device.toString())));

        clientPair.appClient.send("createWidget 1\0{\"id\":200000, \"width\":1, \"height\":1, \"value\":0, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"DEVICE_SELECTOR\"}");
        clientPair.appClient.send("createWidget 1\0{\"id\":88, \"width\":1, \"height\":1, \"deviceId\":200000, \"x\":0, \"y\":0, \"label\":\"Button\", \"type\":\"BUTTON\", \"pinType\":\"VIRTUAL\", \"pin\":88}");
        clientPair.appClient.send("createWidget 1\0{\"id\":89, \"width\":1, \"height\":1, \"deviceId\":200000, \"x\":0, \"y\":0, \"label\":\"Display\", \"type\":\"DIGIT4_DISPLAY\", \"pinType\":\"VIRTUAL\", \"pin\":89}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, OK)));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(4, OK)));

        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        String response = clientPair.appClient.getBody();

        Device[] devices = JsonParser.mapper.readValue(response, Device[].class);
        assertNotNull(devices);
        assertEquals(2, devices.length);

        assertEqualDevice(device0, devices[0]);
        assertEqualDevice(device1, devices[1]);

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();

        hardClient2.send("login " + devices[1].token);
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new HardwareConnectedMessage(1, "1-1")));
        device1.status = Status.ONLINE;

        clientPair.hardwareClient.send("hardware vw 89 value_from_device_0");
        hardClient2.send("hardware vw 89 value_from_device_1");

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new HardwareMessage(1, b("1 vw 89 value_from_device_0"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new HardwareMessage(2, b("1-1 vw 89 value_from_device_1"))));

        clientPair.appClient.send("hardware 1 vw 88 100");

        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, HARDWARE, b("vw 88 100"))));

        //change device, expecting syncs and OK
        clientPair.appClient.send("hardware 1 vu 200000 1");
        verify(clientPair.hardwareClient.responseMock, never()).channelRead(any(), eq(new HardwareMessage(3, b("vu 200000 1"))));
        verify(hardClient2.responseMock, never()).channelRead(any(), eq(new HardwareMessage(3, b("vu 200000 1"))));

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(3)));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new AppSyncMessage(b("1-1 vw 89 value_from_device_1"))));

        //switch device back, expecting syncs and OK
        clientPair.appClient.send("hardware 1 vu 200000 0");
        verify(clientPair.hardwareClient.responseMock, never()).channelRead(any(), eq(new HardwareMessage(4, b("vu 200000 0"))));
        verify(hardClient2.responseMock, never()).channelRead(any(), eq(new HardwareMessage(4, b("vu 200000 0"))));

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(4)));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new AppSyncMessage(b("1 vw 89 value_from_device_0"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new AppSyncMessage(b("1 vw 88 100"))));
    }

    @Test
    public void testDeviceSelectorSyncTimeInput() throws Exception {
        Device device0 = new Device(0, "My Dashboard", "UNO");
        device0.status = Status.ONLINE;
        Device device1 = new Device(1, "My Device", "ESP8266");
        device1.status = Status.OFFLINE;

        clientPair.appClient.send("createDevice 1\0" + device1.toString());
        String createdDevice = clientPair.appClient.getBody();
        Device device = JsonParser.parseDevice(createdDevice);
        assertNotNull(device);
        assertNotNull(device.token);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new CreateDevice(1, device.toString())));

        clientPair.appClient.send("createWidget 1\0{\"id\":200000, \"width\":1, \"height\":1, \"value\":0, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"DEVICE_SELECTOR\"}");
        clientPair.appClient.send(("createWidget 1\0{\"type\":\"TIME_INPUT\",\"id\":99, \"pin\":99, \"pinType\":\"VIRTUAL\", " +
                "\"x\":0,\"y\":0,\"width\":1,\"height\":1, \"deviceId\":200000}"));

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, OK)));

        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        String response = clientPair.appClient.getBody();

        Device[] devices = JsonParser.mapper.readValue(response, Device[].class);
        assertNotNull(devices);
        assertEquals(2, devices.length);

        assertEqualDevice(device0, devices[0]);
        assertEqualDevice(device1, devices[1]);

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();

        hardClient2.send("login " + devices[1].token);
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new HardwareConnectedMessage(1, "1-1")));
        device1.status = Status.ONLINE;

        clientPair.appClient.send("hardware 1 vw " + b("99 82800 82860 Europe/Kiev 1"));
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(produce(2, HARDWARE, b("vw 99 82800 82860 Europe/Kiev 1"))));

        //change device, expecting syncs and OK
        clientPair.appClient.send("hardware 1 vu 200000 1");
        verify(clientPair.hardwareClient.responseMock, never()).channelRead(any(), eq(new HardwareMessage(3, b("vu 200000 1"))));
        verify(hardClient2.responseMock, never()).channelRead(any(), eq(new HardwareMessage(3, b("vu 200000 1"))));

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(3)));

        //switch device back, expecting syncs and OK
        clientPair.appClient.send("hardware 1 vu 200000 0");
        verify(clientPair.hardwareClient.responseMock, never()).channelRead(any(), eq(new HardwareMessage(4, b("vu 200000 0"))));
        verify(hardClient2.responseMock, never()).channelRead(any(), eq(new HardwareMessage(4, b("vu 200000 0"))));

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(4)));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new AppSyncMessage(b("1 dw 1 1"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new AppSyncMessage(b("1 dw 2 1"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new AppSyncMessage(b("1 aw 3 0"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new AppSyncMessage(b("1 dw 5 1"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new AppSyncMessage(b("1 vw 4 244"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new AppSyncMessage(b("1 aw 7 3"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new AppSyncMessage(b("1 aw 30 3"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new AppSyncMessage(b("1 vw 0 89.888037459418"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new AppSyncMessage(b("1 vw 1 -58.74774244674501"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new AppSyncMessage(b("1 vw 13 60 143 158"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new AppSyncMessage(b("1 vw 99 82800 82860 Europe/Kiev 1"))));
    }

    @Test
    public void testNoSyncForDeviceSelectorWidget() throws Exception {
        Device device0 = new Device(0, "My Dashboard", "UNO");
        device0.status = Status.ONLINE;
        Device device1 = new Device(1, "My Device", "ESP8266");
        device1.status = Status.OFFLINE;

        clientPair.appClient.send("createDevice 1\0" + device1.toString());
        String createdDevice = clientPair.appClient.getBody();
        Device device = JsonParser.parseDevice(createdDevice);
        assertNotNull(device);
        assertNotNull(device.token);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new CreateDevice(1, device.toString())));

        clientPair.appClient.send("createWidget 1\0{\"id\":200000, \"width\":1, \"height\":1, \"value\":0, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"DEVICE_SELECTOR\"}");
        clientPair.appClient.send("createWidget 1\0{\"id\":88, \"width\":1, \"height\":1, \"deviceId\":200000, \"x\":0, \"y\":0, \"label\":\"Button\", \"type\":\"BUTTON\", \"pinType\":\"VIRTUAL\", \"pin\":88}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, OK)));

        clientPair.appClient.send("hardware 1 vw 88 100");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new HardwareMessage(4, b("vw 88 100"))));

        clientPair.appClient.send("appsync 1");
        verify(clientPair.appClient.responseMock, timeout(1000).times(15)).channelRead(any(), any());
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(5)));
        verify(clientPair.appClient.responseMock, never()).channelRead(any(), eq(new AppSyncMessage(b("1-200000 vw 88 100"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new AppSyncMessage(b("1 vw 88 100"))));
    }

    @Test
    public void testDeviceSelectorWorksAfterDeviceRemoval() throws Exception {
        Device device0 = new Device(0, "My Dashboard", "UNO");
        device0.status = Status.ONLINE;
        Device device1 = new Device(1, "My Device", "ESP8266");
        device1.status = Status.OFFLINE;

        clientPair.appClient.send("createDevice 1\0" + device1.toString());
        String createdDevice = clientPair.appClient.getBody();
        Device device = JsonParser.parseDevice(createdDevice);
        assertNotNull(device);
        assertNotNull(device.token);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new CreateDevice(1, device.toString())));

        clientPair.appClient.send("createWidget 1\0{\"id\":200000, \"width\":1, \"height\":1, \"value\":0, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"DEVICE_SELECTOR\"}");
        clientPair.appClient.send("createWidget 1\0{\"id\":88, \"width\":1, \"height\":1, \"deviceId\":200000, \"x\":0, \"y\":0, \"label\":\"Button\", \"type\":\"BUTTON\", \"pinType\":\"VIRTUAL\", \"pin\":88}");
        clientPair.appClient.send("createWidget 1\0{\"id\":89, \"width\":1, \"height\":1, \"deviceId\":200000, \"x\":0, \"y\":0, \"label\":\"Display\", \"type\":\"DIGIT4_DISPLAY\", \"pinType\":\"VIRTUAL\", \"pin\":89}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, OK)));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(4, OK)));

        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        String response = clientPair.appClient.getBody();

        Device[] devices = JsonParser.mapper.readValue(response, Device[].class);
        assertNotNull(devices);
        assertEquals(2, devices.length);

        assertEqualDevice(device0, devices[0]);
        assertEqualDevice(device1, devices[1]);

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();

        hardClient2.send("login " + devices[1].token);
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.appClient.send("hardware 1-200000 vw 88 100");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, HARDWARE, b("vw 88 100"))));

        //change device, expecting syncs and OK
        clientPair.appClient.send("hardware 1 vu 200000 1");
        verify(clientPair.hardwareClient.responseMock, never()).channelRead(any(), eq(new HardwareMessage(3, b("vu 200000 1"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(3)));

        clientPair.appClient.send("hardware 1-200000 vw 88 101");
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(produce(4, HARDWARE, b("vw 88 101"))));

        clientPair.appClient.send("deleteDevice 1\0" + "1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(5)));

        //channel should be closed. so will not receive message
        clientPair.appClient.send("hardware 1-200000 vw 88 102");
        verify(hardClient2.responseMock, after(500).never()).channelRead(any(), eq(produce(5, HARDWARE, b("vw 88 100"))));
    }

}
