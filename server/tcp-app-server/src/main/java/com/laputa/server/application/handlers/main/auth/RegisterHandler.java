package com.laputa.server.application.handlers.main.auth;

import com.laputa.server.Holder;
import com.laputa.server.core.dao.TokenManager;
import com.laputa.server.core.dao.UserDao;
import com.laputa.server.core.dao.UserKey;
import com.laputa.server.core.model.AppName;
import com.laputa.server.core.model.DashBoard;
import com.laputa.server.core.model.auth.App;
import com.laputa.server.core.model.auth.User;
import com.laputa.server.core.model.device.Device;
import com.laputa.server.core.model.enums.ProvisionType;
import com.laputa.server.core.protocol.handlers.DefaultExceptionHandler;
import com.laputa.server.core.protocol.model.messages.appllication.RegisterMessage;
import com.laputa.server.workers.timer.TimerWorker;
import com.laputa.utils.JsonParser;
import com.laputa.utils.TokenGeneratorUtil;
import com.laputa.utils.validators.BlynkEmailValidator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.laputa.server.core.protocol.enums.Response.USER_ALREADY_REGISTERED;
import static com.laputa.utils.BlynkByteBufUtil.*;

/**
 * Process register message.
 * Divides input string by nil char on 3 parts:
 * "username" "password" "appName".
 * Checks if user not registered yet. If not - registering.
 *
 * For instance, incoming register message may be : "user@mail.ua my_password"
 *
 * The Laputa Project.
 * Created by Sommer
 * Created on 2/1/2015.
 */
@ChannelHandler.Sharable
public class RegisterHandler extends SimpleChannelInboundHandler<RegisterMessage> implements DefaultExceptionHandler {

    private final UserDao userDao;
    private final TokenManager tokenManager;
    private final TimerWorker timerWorker;
    private final Set<String> allowedUsers;

    public RegisterHandler(Holder holder) {
        this(holder.userDao, holder.tokenManager, holder.timerWorker, holder.props.getCommaSeparatedValueAsArray("allowed.users.list"));
    }

    //for tests only
    public RegisterHandler(UserDao userDao, TokenManager tokenManager, TimerWorker timerWorker, String[] allowedUsersArray) {
        this.userDao = userDao;
        this.tokenManager = tokenManager;
        this.timerWorker = timerWorker;

        if (allowedUsersArray != null && allowedUsersArray.length > 0 &&
                allowedUsersArray[0] != null && !allowedUsersArray[0].isEmpty()) {
            allowedUsers = new HashSet<>(Arrays.asList(allowedUsersArray));
            log.debug("Created allowed user list : {}", (Object[]) allowedUsersArray);
        } else {
            allowedUsers = null;
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RegisterMessage message) throws Exception {
        //warn: split may be optimized
        String[] messageParts = message.body.split("\0");

        //expecting message with 3 parts, described above in comment.
        if (messageParts.length < 2) {
            log.error("Register Handler. Wrong income message format. {}", message);
            ctx.writeAndFlush(illegalCommand(message.id), ctx.voidPromise());
            return;
        }

        String email = messageParts[0].trim().toLowerCase();
        String pass = messageParts[1];
        String appName = messageParts.length == 3 ? messageParts[2] : AppName.BLYNK;
        log.info("Trying register user : {}, app : {}", email, appName);

        if (BlynkEmailValidator.isNotValidEmail(email)) {
            log.error("Register Handler. Wrong email: {}", email);
            ctx.writeAndFlush(illegalCommand(message.id), ctx.voidPromise());
            return;
        }

        if (userDao.isUserExists(email, appName)) {
            log.warn("User with email {} already exists.", email);
            ctx.writeAndFlush(makeResponse(message.id, USER_ALREADY_REGISTERED), ctx.voidPromise());
            return;
        }

        if (allowedUsers != null && !allowedUsers.contains(email)) {
            log.warn("User with email {} not allowed to register.", email);
            ctx.writeAndFlush(notAllowed(message.id), ctx.voidPromise());
            return;
        }

        User newUser = userDao.add(email, pass, appName);

        log.info("Registered {}.", email);

        createProjectForExportedApp(newUser, appName);

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

    private void createProjectForExportedApp(User newUser, String appName) {
        if (appName.equals(AppName.BLYNK)) {
            return;
        }

        User parentUser = null;
        App app = null;

        for (User user : userDao.users.values()) {
            app = user.profile.getAppById(appName);
            if (app != null) {
                parentUser = user;
                break;
            }
        }

        if (app == null) {
            log.error("Unable to find app with id {}", appName);
            return;
        }

        if (app.isMultiFace) {
            log.info("App supports multi faces. Skipping profile creation.");
            return;
        }

        int dashId = app.projectIds[0];
        DashBoard dash = parentUser.profile.getDashByIdOrThrow(dashId);

        DashBoard clonedDash = JsonParser.parseDashboard(dash.toStringRestrictive());

        clonedDash.id = 1;
        clonedDash.parentId = dash.parentId;
        clonedDash.createdAt = System.currentTimeMillis();
        clonedDash.updatedAt = clonedDash.createdAt;
        clonedDash.isActive = true;
        clonedDash.pinsStorage = Collections.emptyMap();
        clonedDash.eraseValues();

        clonedDash.addTimers(timerWorker, new UserKey(newUser));

        newUser.profile.dashBoards = new DashBoard[] {clonedDash};

        if (app.provisionType == ProvisionType.STATIC) {
            for (Device device : clonedDash.devices) {
                device.erase();
            }
        } else {
            for (Device device : clonedDash.devices) {
                device.erase();
                String token = TokenGeneratorUtil.generateNewToken();
                tokenManager.assignToken(newUser, clonedDash.id, device.id, token);
            }
        }
    }

}
