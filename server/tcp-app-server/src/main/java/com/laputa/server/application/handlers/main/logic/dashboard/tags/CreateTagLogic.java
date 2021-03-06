package com.laputa.server.application.handlers.main.logic.dashboard.tags;

import com.laputa.server.core.model.DashBoard;
import com.laputa.server.core.model.auth.User;
import com.laputa.server.core.model.device.Tag;
import com.laputa.server.core.protocol.exceptions.IllegalCommandException;
import com.laputa.server.core.protocol.model.messages.StringMessage;
import com.laputa.utils.ArrayUtil;
import com.laputa.utils.JsonParser;
import com.laputa.utils.ParseUtil;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.laputa.server.core.protocol.enums.Command.CREATE_TAG;
import static com.laputa.utils.LaputaByteBufUtil.makeUTF8StringMessage;
import static com.laputa.utils.StringUtils.split2;

/**
 * The Laputa Project.
 * Created by Sommer
 * Created on 01.02.16.
 */
public class CreateTagLogic {

    private static final Logger log = LogManager.getLogger(CreateTagLogic.class);

    public static void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String[] split = split2(message.body);

        if (split.length < 2) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        int dashId = ParseUtil.parseInt(split[0]) ;
        String deviceString = split[1];

        if (deviceString == null || deviceString.isEmpty()) {
            throw new IllegalCommandException("Income tag message is empty.");
        }

        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);

        Tag newTag = JsonParser.parseTag(deviceString);

        log.debug("Creating new tag {}.", newTag);

        if (newTag.isNotValid()) {
            throw new IllegalCommandException("Income tag name is not valid.");
        }

        for (Tag tag : dash.tags) {
            if (tag.id == newTag.id || tag.name.equals(newTag.name)) {
                throw new IllegalCommandException("Tag with same id/name already exists.");
            }
        }

        dash.tags = ArrayUtil.add(dash.tags, newTag, Tag.class);
        dash.updatedAt = System.currentTimeMillis();
        user.lastModifiedTs = dash.updatedAt;

        if (ctx.channel().isWritable()) {
            ctx.writeAndFlush(makeUTF8StringMessage(CREATE_TAG, message.id, newTag.toString()), ctx.voidPromise());
        }
    }

}
