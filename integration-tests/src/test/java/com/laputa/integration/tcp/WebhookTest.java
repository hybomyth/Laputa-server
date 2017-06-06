package com.laputa.integration.tcp;

import com.laputa.integration.IntegrationBase;
import com.laputa.integration.model.tcp.ClientPair;
import com.laputa.server.api.http.HttpAPIServer;
import com.laputa.server.application.AppServer;
import com.laputa.server.core.BaseServer;
import com.laputa.server.core.model.enums.PinType;
import com.laputa.server.core.model.widgets.others.webhook.Header;
import com.laputa.server.core.model.widgets.others.webhook.WebHook;
import com.laputa.server.core.protocol.model.messages.common.HardwareMessage;
import com.laputa.server.hardware.HardwareServer;
import com.laputa.utils.JsonParser;
import com.laputa.utils.StringUtils;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Response;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.concurrent.Future;

import static com.laputa.server.core.model.widgets.others.webhook.SupportedWebhookMethod.GET;
import static com.laputa.server.core.model.widgets.others.webhook.SupportedWebhookMethod.PUT;
import static com.laputa.server.core.protocol.enums.Command.HARDWARE;
import static com.laputa.server.core.protocol.model.messages.MessageFactory.produce;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * The Laputa Project.
 * Created by Sommer
 * Created on 5/09/2016.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class WebhookTest extends IntegrationBase {

    private BaseServer httpServer;
    private BaseServer appServer;
    private BaseServer hardwareServer;
    private ClientPair clientPair;

    private static AsyncHttpClient httpclient;
    private static String httpServerUrl;

    @BeforeClass
    public static void initHttpClient() {
        httpServerUrl = String.format("http://localhost:%s/", httpPort);
        httpclient = new DefaultAsyncHttpClient(
                new DefaultAsyncHttpClientConfig.Builder()
                        .setUserAgent("")
                        .setKeepAlive(true)
                        .build());
    }

    @AfterClass
    public static void closeHttpClient() throws Exception {
        httpclient.close();
    }

    @Before
    public void init() throws Exception {
        httpServer = new HttpAPIServer(holder).start();
        hardwareServer = new HardwareServer(holder).start();
        appServer = new AppServer(holder).start();


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

    @Override
    public String getDataFolder() {
        return getRelativeDataFolder("/profiles");
    }

    @Test
    @Ignore
    public void testThingsSpeakIntegrationTest() throws Exception {
        WebHook webHook = new WebHook();
        webHook.url = "https://api.thingspeak.com/update?api_key=API_KEY&field1=%s".replace("API_KEY", "");
        webHook.method = GET;
        webHook.pin = 123;
        webHook.pinType = PinType.VIRTUAL;

        clientPair.appClient.send("createWidget 1\0" + JsonParser.mapper.writeValueAsString(webHook));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.hardwareClient.send("hardware vw 123 10");
        verify(clientPair.hardwareClient.responseMock, after(1000).times(0)).channelRead(any(), any());
    }

    @Test
    @Ignore
    public void testSome3dPartyWeatherServiceTest() throws Exception {
        WebHook webHook = new WebHook();
        webHook.url = "http://api.sunrise-sunset.org/json?lat=36.7201600&lng=-4.4203400&date=2016-08-25";
        webHook.method = GET;
        webHook.pin = 123;
        webHook.pinType = PinType.VIRTUAL;

        clientPair.appClient.send("createWidget 1\0" + JsonParser.mapper.writeValueAsString(webHook));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.hardwareClient.send("hardware vw 123 10");
        String expectedResponse = "vw" + StringUtils.BODY_SEPARATOR_STRING + "123" + StringUtils.BODY_SEPARATOR_STRING +
                "{\"results\":{\"sunrise\":\"7:30:27 AM\",\"sunset\":\"5:14:34 PM\",\"solar_noon\":\"12:22:31 PM\",\"day_length\":\"09:44:07\",\"civil_twilight_begin\":\"7:01:53 AM\",\"civil_twilight_end\":\"5:43:08 PM\",\"nautical_twilight_begin\":\"6:29:39 AM\",\"nautical_twilight_end\":\"6:15:23 PM\",\"astronomical_twilight_begin\":\"5:58:15 AM\",\"astronomical_twilight_end\":\"6:46:46 PM\"},\"status\":\"OK\"}";
        verify(clientPair.hardwareClient.responseMock, timeout(3000)).channelRead(any(), eq(produce(888, HARDWARE, expectedResponse)));
    }

    @Test
    @Ignore
    public void testSome3dPartyWeatherServiceTriggerFromAppTest() throws Exception {
        WebHook webHook = new WebHook();
        webHook.url = "http://api.sunrise-sunset.org/json?lat=36.7201600&lng=-4.4203400&date=2016-08-25";
        webHook.method = GET;
        webHook.pin = 123;
        webHook.pinType = PinType.VIRTUAL;

        clientPair.appClient.send("createWidget 1\0" + JsonParser.mapper.writeValueAsString(webHook));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("hardware 1 vw 123 10");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, HARDWARE, b("vw 123 10"))));

        String expectedResponse = "vw" + StringUtils.BODY_SEPARATOR_STRING + "123" + StringUtils.BODY_SEPARATOR_STRING +
                "{\"results\":{\"sunrise\":\"7:30:27 AM\",\"sunset\":\"5:14:34 PM\",\"solar_noon\":\"12:22:31 PM\",\"day_length\":\"09:44:07\",\"civil_twilight_begin\":\"7:01:53 AM\",\"civil_twilight_end\":\"5:43:08 PM\",\"nautical_twilight_begin\":\"6:29:39 AM\",\"nautical_twilight_end\":\"6:15:23 PM\",\"astronomical_twilight_begin\":\"5:58:15 AM\",\"astronomical_twilight_end\":\"6:46:46 PM\"},\"status\":\"OK\"}";
        verify(clientPair.hardwareClient.responseMock, timeout(3000)).channelRead(any(), eq(produce(888, HARDWARE, expectedResponse)));
    }

    @Test
    public void testWebhookWorksWithBlynkHttpApiNoPlaceHolder() throws Exception {
        WebHook webHook = new WebHook();
        webHook.url = httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/V124";
        webHook.method = PUT;
        webHook.headers = new Header[] {new Header("Content-Type", "application/json")};
        webHook.body = "[\"124\"]";
        webHook.pin = 123;
        webHook.pinType = PinType.VIRTUAL;
        webHook.width = 2;
        webHook.height = 1;

        clientPair.appClient.send("createWidget 1\0" + JsonParser.mapper.writeValueAsString(webHook));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.hardwareClient.send("hardware vw 123 10");
        verify(clientPair.hardwareClient.responseMock, after(1000).times(0)).channelRead(any(), any());

        Future<Response> f = httpclient.prepareGet(httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/V124").execute();
        Response response = f.get();

        assertEquals(200, response.getStatusCode());
        List<String> values = consumeJsonPinValues(response.getResponseBody());
        assertEquals(1, values.size());
        assertEquals("124", values.get(0));
    }

    @Test
    public void testWebhookWorksWithBlynkHttpApiPlaceHolderAndTextPlain() throws Exception {
        WebHook webHook = new WebHook();
        webHook.url = httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/V124";
        webHook.method = PUT;
        webHook.headers = new Header[] {new Header("Content-Type", "text/plain")};
        webHook.body = "[\"/pin/\"]";
        webHook.pin = 123;
        webHook.pinType = PinType.VIRTUAL;
        webHook.width = 2;
        webHook.height = 1;

        clientPair.appClient.send("createWidget 1\0" + JsonParser.mapper.writeValueAsString(webHook));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.hardwareClient.send("hardware vw 123 10");
        verify(clientPair.hardwareClient.responseMock, after(1000).times(0)).channelRead(any(), any());

        Future<Response> f = httpclient.prepareGet(httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/V124").execute();
        Response response = f.get();

        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void testWebhookWorksWithBlynkHttpApiWithDateTimePlaceholder() throws Exception {
        WebHook webHook = new WebHook();
        webHook.url = httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/V124";
        webHook.method = PUT;
        webHook.headers = new Header[] {new Header("Content-Type", "application/json")};
        webHook.body = "[\"/datetime_iso/\"]";
        webHook.pin = 123;
        webHook.pinType = PinType.VIRTUAL;
        webHook.width = 2;
        webHook.height = 1;

        clientPair.appClient.send("createWidget 1\0" + JsonParser.mapper.writeValueAsString(webHook));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.hardwareClient.send("hardware vw 123 10");
        verify(clientPair.hardwareClient.responseMock, after(1000).times(0)).channelRead(any(), any());

        Future<Response> f = httpclient.prepareGet(httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/V124").execute();
        Response response = f.get();

        assertEquals(200, response.getStatusCode());
        List<String> values = consumeJsonPinValues(response.getResponseBody());
        assertEquals(1, values.size());
        assertTrue(values.get(0).endsWith("Z"));
        assertTrue(values.get(0).contains("T"));
    }

    @Test
    public void testWebhookWorksWithBlynkHttpApiWithPlaceholder() throws Exception {
        WebHook webHook = new WebHook();
        webHook.url = httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/V124";
        webHook.method = PUT;
        webHook.headers = new Header[] {new Header("Content-Type", "application/json")};
        webHook.body = "[%s]";
        webHook.pin = 123;
        webHook.pinType = PinType.VIRTUAL;
        webHook.width = 2;
        webHook.height = 1;

        clientPair.appClient.send("createWidget 1\0" + JsonParser.mapper.writeValueAsString(webHook));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.hardwareClient.send("hardware vw 123 10");
        verify(clientPair.hardwareClient.responseMock, after(1000).times(0)).channelRead(any(), any());

        Future<Response> f = httpclient.prepareGet(httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/V124").execute();
        Response response = f.get();

        assertEquals(200, response.getStatusCode());
        List<String> values = consumeJsonPinValues(response.getResponseBody());
        assertEquals(1, values.size());
        assertEquals("10", values.get(0));
    }

    @Test
    public void testWebhookWorksWithBlynkHttpApiWithArrayPlaceholder() throws Exception {
        WebHook webHook = new WebHook();
        webHook.url = httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/V124";
        webHook.method = PUT;
        webHook.headers = new Header[] {new Header("Content-Type", "application/json")};
        webHook.body = "[/pin[0]/,/pin[1]/,/pin[2]/]";
        webHook.pin = 123;
        webHook.pinType = PinType.VIRTUAL;
        webHook.width = 2;
        webHook.height = 1;

        clientPair.appClient.send("createWidget 1\0" + JsonParser.mapper.writeValueAsString(webHook));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.hardwareClient.send("hardware vw 123 " + b("10 11 12"));
        verify(clientPair.hardwareClient.responseMock, after(1000).times(0)).channelRead(any(), any());

        Future<Response> f = httpclient.prepareGet(httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/V124").execute();
        Response response = f.get();

        assertEquals(200, response.getStatusCode());
        List<String> values = consumeJsonPinValues(response.getResponseBody());
        assertEquals(3, values.size());
        assertEquals("10", values.get(0));
        assertEquals("11", values.get(1));
        assertEquals("12", values.get(2));
    }

    @Test
    public void testWebhookWorksWithBlynkHttpApiWithArrayPlaceholder2() throws Exception {
        WebHook webHook = new WebHook();
        webHook.url = httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/V124";
        webHook.method = PUT;
        webHook.headers = new Header[] {new Header("Content-Type", "application/json")};
        webHook.body = "[/pin[0]/]";
        webHook.pin = 123;
        webHook.pinType = PinType.VIRTUAL;
        webHook.width = 2;
        webHook.height = 1;

        clientPair.appClient.send("createWidget 1\0" + JsonParser.mapper.writeValueAsString(webHook));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.hardwareClient.send("hardware vw 123 10");
        verify(clientPair.hardwareClient.responseMock, after(1000).times(0)).channelRead(any(), any());

        Future<Response> f = httpclient.prepareGet(httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/V124").execute();
        Response response = f.get();

        assertEquals(200, response.getStatusCode());
        List<String> values = consumeJsonPinValues(response.getResponseBody());
        assertEquals(1, values.size());
        assertEquals("10", values.get(0));
    }

    @Test
    public void testWebhookWorksWithBlynkHttpApiWithPlaceholderQuotaLimit() throws Exception {
        WebHook webHook = new WebHook();
        webHook.url = httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/V124";
        webHook.method = PUT;
        webHook.headers = new Header[] {new Header("Content-Type", "application/json")};
        webHook.body = "[%s]";
        webHook.pin = 123;
        webHook.pinType = PinType.VIRTUAL;
        webHook.width = 2;
        webHook.height = 1;

        clientPair.appClient.send("createWidget 1\0" + JsonParser.mapper.writeValueAsString(webHook));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.hardwareClient.send("hardware vw 123 10");
        verify(clientPair.hardwareClient.responseMock, after(500).times(0)).channelRead(any(), any());

        Future<Response> f = httpclient.prepareGet(httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/V124").execute();
        Response response = f.get();

        assertEquals(200, response.getStatusCode());
        List<String> values = consumeJsonPinValues(response.getResponseBody());
        assertEquals(1, values.size());
        assertEquals("10", values.get(0));

        clientPair.hardwareClient.send("hardware vw 123 11");
        verify(clientPair.hardwareClient.responseMock, after(500).times(0)).channelRead(any(), any());

        f = httpclient.prepareGet(httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/V124").execute();
        response = f.get();

        assertEquals(200, response.getStatusCode());
        values = consumeJsonPinValues(response.getResponseBody());
        assertEquals(1, values.size());
        assertEquals("10", values.get(0));


        clientPair.hardwareClient.send("hardware vw 123 12");
        verify(clientPair.hardwareClient.responseMock, after(500).times(0)).channelRead(any(), any());

        f = httpclient.prepareGet(httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/V124").execute();
        response = f.get();

        assertEquals(200, response.getStatusCode());
        values = consumeJsonPinValues(response.getResponseBody());
        assertEquals(1, values.size());
        assertEquals("12", values.get(0));
    }

    @Test
    public void testWebhookWorksWithBlynkHttpApiNoPlaceHolderAppSideTrigger() throws Exception {
        WebHook webHook = new WebHook();
        webHook.url = httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/V124";
        webHook.method = PUT;
        webHook.headers = new Header[] {new Header("Content-Type", "application/json")};
        webHook.body = "[\"124\"]";
        webHook.pin = 123;
        webHook.pinType = PinType.VIRTUAL;
        webHook.width = 2;
        webHook.height = 1;

        clientPair.appClient.send("createWidget 1\0" + JsonParser.mapper.writeValueAsString(webHook));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        //125564119 is id of project with 4ae3851817194e2596cf1b7103603ef8 token
        clientPair.appClient.send("hardware 1 vw 123 10");
        verify(clientPair.hardwareClient.responseMock, after(500).times(1)).channelRead(any(), eq(new HardwareMessage(2, b("vw 123 10"))));

        Future<Response> f = httpclient.prepareGet(httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/V124").execute();
        Response response = f.get();

        assertEquals(200, response.getStatusCode());
        List<String> values = consumeJsonPinValues(response.getResponseBody());
        assertEquals(1, values.size());
        assertEquals("124", values.get(0));
    }

    @Test
    public void testWebhookWorksWithBlynkHttpApiAppSideTriggerCheckLimit() throws Exception {
        WebHook webHook = new WebHook();
        webHook.url = httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/update/V124";
        webHook.method = PUT;
        webHook.headers = new Header[] {new Header("Content-Type", "application/json")};
        webHook.body = "[\"/pin/\"]";
        webHook.pin = 123;
        webHook.pinType = PinType.VIRTUAL;
        webHook.width = 2;
        webHook.height = 1;

        clientPair.appClient.send("createWidget 1\0" + JsonParser.mapper.writeValueAsString(webHook));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("hardware 1 vw 123 10");
        verify(clientPair.hardwareClient.responseMock, after(500)).channelRead(any(), eq(new HardwareMessage(2, b("vw 123 10"))));

        Future<Response> f = httpclient.prepareGet(httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/get/V124").execute();
        Response response = f.get();

        assertEquals(200, response.getStatusCode());
        List<String> values = consumeJsonPinValues(response.getResponseBody());
        assertEquals(1, values.size());
        assertEquals("10", values.get(0));

        clientPair.appClient.send("hardware 1 vw 123 11");
        verify(clientPair.hardwareClient.responseMock, after(1000)).channelRead(any(), eq(new HardwareMessage(3, b("vw 123 11"))));

        f = httpclient.prepareGet(httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/get/V124").execute();
        response = f.get();

        assertEquals(200, response.getStatusCode());
        values = consumeJsonPinValues(response.getResponseBody());
        assertEquals(1, values.size());
        assertEquals("10", values.get(0));


        clientPair.appClient.send("hardware 1 vw 123 11");
        verify(clientPair.hardwareClient.responseMock, after(500)).channelRead(any(), eq(new HardwareMessage(4, b("vw 123 11"))));

        f = httpclient.prepareGet(httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/get/V124").execute();
        response = f.get();

        assertEquals(200, response.getStatusCode());
        values = consumeJsonPinValues(response.getResponseBody());
        assertEquals(1, values.size());
        assertEquals("11", values.get(0));
    }

    @Test
    public void testWebhookInvalidUrl() throws Exception {
        WebHook webHook = new WebHook();
        webHook.url = "";
        webHook.method = PUT;
        webHook.headers = new Header[]{new Header("Content-Type", "application/json")};
        webHook.body = "[\"/pin/\"]";
        webHook.pin = 123;
        webHook.pinType = PinType.VIRTUAL;
        webHook.width = 2;
        webHook.height = 1;
        webHook.id = 111;


        webHook.url = "http://adasd.com";
        clientPair.appClient.send("createWidget 1\0" + JsonParser.mapper.writeValueAsString(webHook));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        webHook.id = 222;
        webHook.url = "https://adasd.com";
        clientPair.appClient.send("createWidget 1\0" + JsonParser.mapper.writeValueAsString(webHook));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        webHook.id = 333;
        webHook.url = "Http://adasd.com";
        clientPair.appClient.send("createWidget 1\0" + JsonParser.mapper.writeValueAsString(webHook));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(3)));
    }

}
