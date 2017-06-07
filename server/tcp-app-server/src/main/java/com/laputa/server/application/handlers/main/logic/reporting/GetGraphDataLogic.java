package com.laputa.server.application.handlers.main.logic.reporting;

import com.laputa.server.core.BlockingIOProcessor;
import com.laputa.server.core.dao.ReportingDao;
import com.laputa.server.core.model.DashBoard;
import com.laputa.server.core.model.auth.User;
import com.laputa.server.core.model.enums.PinType;
import com.laputa.server.core.model.widgets.Target;
import com.laputa.server.core.protocol.exceptions.IllegalCommandBodyException;
import com.laputa.server.core.protocol.exceptions.IllegalCommandException;
import com.laputa.server.core.protocol.exceptions.NoDataException;
import com.laputa.server.core.protocol.model.messages.StringMessage;
import com.laputa.server.core.reporting.GraphPinRequest;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

import static com.laputa.server.core.protocol.enums.Command.GET_GRAPH_DATA_RESPONSE;
import static com.laputa.server.core.protocol.enums.Response.NO_DATA;
import static com.laputa.server.core.protocol.enums.Response.SERVER_ERROR;
import static com.laputa.utils.LaputaByteBufUtil.*;
import static com.laputa.utils.ByteUtils.compress;
import static com.laputa.utils.StringUtils.split2Device;

/**
 * The Laputa Project.
 * Created by Sommer
 * Created on 2/1/2015.
 *
 */
public class GetGraphDataLogic {

    private static final Logger log = LogManager.getLogger(GetGraphDataLogic.class);

    private final BlockingIOProcessor blockingIOProcessor;
    private final ReportingDao reportingDao;

    public GetGraphDataLogic(ReportingDao reportingDao, BlockingIOProcessor blockingIOProcessor) {
        this.reportingDao = reportingDao;
        this.blockingIOProcessor = blockingIOProcessor;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        //warn: split may be optimized
        //todo remove space after app migration
        String[] messageParts = message.body.split(" |\0");

        if (messageParts.length < 3) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        String[] dashIdTargetId = split2Device(messageParts[0]);
        int dashId = Integer.parseInt(dashIdTargetId[0]);
        int targetId = 0;
        if (dashIdTargetId.length == 2) {
            targetId = Integer.parseInt(dashIdTargetId[1]);
        }

        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);

        Target target = dash.getTarget(targetId);
        if (target == null) {
            log.debug("No assigned target for received command.");
            ctx.writeAndFlush(makeResponse(message.id, NO_DATA), ctx.voidPromise());
            return;
        }

        //history graph could be assigned only to device or device selector
        final int deviceId = target.getDeviceId();

        //special case for delete command
        if (messageParts.length == 4) {
            deleteGraphData(messageParts, user, dashId, deviceId);
            ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
        } else {
            process(ctx.channel(), dashId, deviceId, Arrays.copyOfRange(messageParts, 1, messageParts.length), user, message.id, 4);
        }
    }

    private void process(Channel channel, int dashId, int deviceId, String[] messageParts, User user, int msgId, int valuesPerPin) {
        int numberOfPins = messageParts.length / valuesPerPin;

        GraphPinRequest[] requestedPins = new GraphPinRequestData[numberOfPins];

        for (int i = 0; i < numberOfPins; i++) {
            requestedPins[i] = new GraphPinRequestData(dashId, deviceId, messageParts, i, valuesPerPin);
        }

        readGraphData(channel, user, requestedPins, msgId);
    }

    private void readGraphData(Channel channel, User user, GraphPinRequest[] requestedPins, int msgId) {
        blockingIOProcessor.executeHistory(() -> {
            try {
                byte[][] data = reportingDao.getAllFromDisk(user, requestedPins);
                byte[] compressed = compress(requestedPins[0].dashId, data);

                if (channel.isWritable()) {
                    channel.writeAndFlush(makeBinaryMessage(GET_GRAPH_DATA_RESPONSE, msgId, compressed), channel.voidPromise());
                }
            } catch (NoDataException noDataException) {
                channel.writeAndFlush(makeResponse(msgId, NO_DATA), channel.voidPromise());
            } catch (Exception e) {
                log.error("Error reading reporting data. For user {}", user.email);
                channel.writeAndFlush(makeResponse(msgId, SERVER_ERROR), channel.voidPromise());
            }
        });
    }

    private void deleteGraphData(String[] messageParts, User user, int dashId, int deviceId) {
        try {
            PinType pinType = PinType.getPinType(messageParts[1].charAt(0));
            byte pin = Byte.parseByte(messageParts[2]);
            String cmd = messageParts[3];
            if (!"del".equals(cmd)) {
                throw new IllegalCommandBodyException("Wrong body format. Expecting 'del'.");
            }
            reportingDao.delete(user, dashId, deviceId, pinType, pin);
        } catch (NumberFormatException e) {
            throw new IllegalCommandException("HardwareLogic command body incorrect.");
        }
    }

}
