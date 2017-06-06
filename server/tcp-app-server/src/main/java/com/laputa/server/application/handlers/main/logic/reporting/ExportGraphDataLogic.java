package com.laputa.server.application.handlers.main.logic.reporting;

import com.laputa.server.Holder;
import com.laputa.server.core.BlockingIOProcessor;
import com.laputa.server.core.dao.ReportingDao;
import com.laputa.server.core.model.DashBoard;
import com.laputa.server.core.model.Pin;
import com.laputa.server.core.model.auth.User;
import com.laputa.server.core.model.enums.PinType;
import com.laputa.server.core.model.widgets.Widget;
import com.laputa.server.core.model.widgets.outputs.HistoryGraph;
import com.laputa.server.core.protocol.exceptions.IllegalCommandException;
import com.laputa.server.core.protocol.model.messages.StringMessage;
import com.laputa.server.notifications.mail.MailWrapper;
import com.laputa.utils.ParseUtil;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;

import static com.laputa.server.core.protocol.enums.Response.NO_DATA;
import static com.laputa.utils.BlynkByteBufUtil.*;
import static com.laputa.utils.StringUtils.BODY_SEPARATOR_STRING;
import static com.laputa.utils.StringUtils.split2Device;

/**
 * Sends graph pins data in csv format via to user email.
 *
 * The Laputa Project.
 * Created by Sommer
 * Created on 2/1/2015.
 *
 */
public class ExportGraphDataLogic {

    private static final Logger log = LogManager.getLogger(ExportGraphDataLogic.class);

    private final BlockingIOProcessor blockingIOProcessor;
    private final ReportingDao reportingDao;
    private final MailWrapper mailWrapper;
    private final String csvDownloadUrl;

    public ExportGraphDataLogic(Holder holder) {
        this.reportingDao = holder.reportingDao;
        this.blockingIOProcessor = holder.blockingIOProcessor;
        this.mailWrapper = holder.mailWrapper;
        this.csvDownloadUrl = holder.csvDownloadUrl;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String[] messageParts = message.body.split(BODY_SEPARATOR_STRING);

        if (messageParts.length < 2) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        String[] dashIdAndDeviceId = split2Device(messageParts[0]);
        int dashId = ParseUtil.parseInt(dashIdAndDeviceId[0]);

        //todo new device code. remove after migration.
        int deviceId;
        if (dashIdAndDeviceId.length == 2) {
            deviceId = ParseUtil.parseInt(dashIdAndDeviceId[1]);
        } else {
            deviceId = 0;
        }

        long widgetId = ParseUtil.parseLong(messageParts[1]);

        DashBoard dashBoard = user.profile.getDashByIdOrThrow(dashId);

        Widget widget = dashBoard.getWidgetByIdOrThrow(widgetId);
        if (!(widget instanceof HistoryGraph)) {
            throw new IllegalCommandException("Passed wrong widget id.");
        }

        HistoryGraph historyGraph = (HistoryGraph) widget;

        blockingIOProcessor.execute(() -> {
            try {
                String dashName = dashBoard.name == null ? "" : dashBoard.name;
                ArrayList<FileLink> pinsCSVFilePath = new ArrayList<>();
                for (Pin pin : historyGraph.pins) {
                    if (pin != null) {
                        try {
                            Path path = reportingDao.csvGenerator.createCSV(user, dashId, deviceId, pin.pinType, pin.pin);
                            pinsCSVFilePath.add(new FileLink(path.getFileName(), dashName, pin.pinType, pin.pin));
                        } catch (Exception e) {
                            //ignore eny exception.
                        }
                    }
                }

                if (pinsCSVFilePath.size() == 0) {
                    ctx.writeAndFlush(makeResponse(message.id, NO_DATA), ctx.voidPromise());
                } else {

                    String title = "History graph data for project " + dashName;
                    mailWrapper.sendHtml(user.email, title, makeBody(pinsCSVFilePath));
                    ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
                }

            } catch (Exception e) {
                log.error("Error making csv file for data export. Reason {}", e.getMessage());
                ctx.writeAndFlush(notificationError(message.id), ctx.voidPromise());
            }
        });
    }

    private String makeBody(ArrayList<FileLink> fileUrls) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        for (FileLink link : fileUrls) {
            sb.append(link.toString()).append("<br>");
        }
        return sb.append("</body></html>").toString();
    }

    private class FileLink {
        final Path path;
        final String dashName;
        final PinType pinType;
        final byte pin;

        public FileLink(Path path, String dashName, PinType pinType, byte pin) {
            this.path = path;
            this.dashName = dashName;
            this.pinType = pinType;
            this.pin = pin;
        }

        @Override
        public String toString() {
            return "<a href=\"" + csvDownloadUrl + path + "\">" + dashName + " " + pinType.pintTypeChar + pin + "</a>";
        }
    }

}
