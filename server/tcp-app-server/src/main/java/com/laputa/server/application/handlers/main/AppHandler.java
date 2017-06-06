package com.laputa.server.application.handlers.main;

import com.laputa.server.Holder;
import com.laputa.server.application.handlers.main.auth.AppStateHolder;
import com.laputa.server.application.handlers.main.logic.*;
import com.laputa.server.application.handlers.main.logic.dashboard.CreateDashLogic;
import com.laputa.server.application.handlers.main.logic.dashboard.DeleteDashLogic;
import com.laputa.server.application.handlers.main.logic.dashboard.UpdateDashLogic;
import com.laputa.server.application.handlers.main.logic.dashboard.UpdateDashSettingLogic;
import com.laputa.server.application.handlers.main.logic.dashboard.device.CreateDeviceLogic;
import com.laputa.server.application.handlers.main.logic.dashboard.device.DeleteDeviceLogic;
import com.laputa.server.application.handlers.main.logic.dashboard.device.GetDevicesLogic;
import com.laputa.server.application.handlers.main.logic.dashboard.device.UpdateDeviceLogic;
import com.laputa.server.application.handlers.main.logic.dashboard.tags.CreateTagLogic;
import com.laputa.server.application.handlers.main.logic.dashboard.tags.DeleteTagLogic;
import com.laputa.server.application.handlers.main.logic.dashboard.tags.GetTagsLogic;
import com.laputa.server.application.handlers.main.logic.dashboard.tags.UpdateTagLogic;
import com.laputa.server.application.handlers.main.logic.dashboard.widget.CreateWidgetLogic;
import com.laputa.server.application.handlers.main.logic.dashboard.widget.DeleteWidgetLogic;
import com.laputa.server.application.handlers.main.logic.dashboard.widget.UpdateWidgetLogic;
import com.laputa.server.application.handlers.main.logic.reporting.ExportGraphDataLogic;
import com.laputa.server.application.handlers.main.logic.reporting.GetGraphDataLogic;
import com.laputa.server.application.handlers.main.logic.sharing.GetShareTokenLogic;
import com.laputa.server.application.handlers.main.logic.sharing.GetSharedDashLogic;
import com.laputa.server.application.handlers.main.logic.sharing.RefreshShareTokenLogic;
import com.laputa.server.application.handlers.main.logic.sharing.ShareLogic;
import com.laputa.server.core.protocol.model.messages.StringMessage;
import com.laputa.server.core.session.StateHolderBase;
import com.laputa.server.core.stats.GlobalStats;
import com.laputa.server.handlers.BaseSimpleChannelInboundHandler;
import com.laputa.server.handlers.common.PingLogic;
import io.netty.channel.ChannelHandlerContext;

import static com.laputa.server.core.protocol.enums.Command.*;

/**
 * The Laputa Project.
 * Created by Sommer
 * Created on 2/1/2015.
 *
 */
public class AppHandler extends BaseSimpleChannelInboundHandler<StringMessage> {

    public final AppStateHolder state;
    private final GetTokenLogic token;
    private final AssignTokenLogic assignTokenLogic;
    private final HardwareAppLogic hardwareApp;
    private final RefreshTokenLogic refreshToken;
    private final GetGraphDataLogic graphData;
    private final ExportGraphDataLogic exportGraphData;
    private final AppMailLogic appMailLogic;
    private final GetShareTokenLogic getShareTokenLogic;
    private final RefreshShareTokenLogic refreshShareTokenLogic;
    private final GetSharedDashLogic getSharedDashLogic;
    private final CreateDashLogic createDashLogic;
    private final UpdateDashLogic updateDashLogic;
    private final UpdateDashSettingLogic updateDashSettingLogic;
    private final ActivateDashboardLogic activateDashboardLogic;
    private final DeActivateDashboardLogic deActivateDashboardLogic;
    private final CreateWidgetLogic createWidgetLogic;
    private final UpdateWidgetLogic updateWidgetLogic;
    private final DeleteWidgetLogic deleteWidgetLogic;
    private final DeleteDashLogic deleteDashLogic;
    private final ShareLogic shareLogic;
    private final RedeemLogic redeemLogic;
    private final AddEnergyLogic addEnergyLogic;
    private final CreateDeviceLogic createDeviceLogic;
    private final DeleteDeviceLogic deleteDeviceLogic;
    private final LoadProfileGzippedLogic loadProfileGzippedLogic;
    private final CreateAppLogic createAppLogic;
    private final UpdateAppLogic updateAppLogic;
    private final GetProjectByTokenLogic getProjectByTokenLogic;
    private final MailQRsLogic mailQRsLogic;
    private final UpdateFaceLogic updateFaceLogic;

    private final GlobalStats stats;

    public AppHandler(Holder holder, AppStateHolder state) {
        super(StringMessage.class, holder.limits);
        this.token = new GetTokenLogic(holder);
        this.assignTokenLogic = new AssignTokenLogic(holder);
        this.hardwareApp = new HardwareAppLogic(holder, state.user.email);
        this.refreshToken = new RefreshTokenLogic(holder);
        this.graphData = new GetGraphDataLogic(holder.reportingDao, holder.blockingIOProcessor);
        this.exportGraphData = new ExportGraphDataLogic(holder);
        this.appMailLogic = new AppMailLogic(holder);
        this.getShareTokenLogic = new GetShareTokenLogic(holder.tokenManager);
        this.refreshShareTokenLogic = new RefreshShareTokenLogic(holder.tokenManager, holder.sessionDao);
        this.getSharedDashLogic = new GetSharedDashLogic(holder.tokenManager);

        this.createDashLogic = new CreateDashLogic(holder.timerWorker, holder.tokenManager, holder.limits.DASHBOARDS_LIMIT, holder.limits.PROFILE_SIZE_LIMIT_BYTES);
        this.updateDashLogic = new UpdateDashLogic(holder.timerWorker, holder.limits.PROFILE_SIZE_LIMIT_BYTES);

        this.activateDashboardLogic = new ActivateDashboardLogic(holder.sessionDao);
        this.deActivateDashboardLogic = new DeActivateDashboardLogic(holder.sessionDao);

        this.createWidgetLogic = new CreateWidgetLogic(holder.limits.WIDGET_SIZE_LIMIT_BYTES, holder.timerWorker);
        this.updateWidgetLogic = new UpdateWidgetLogic(holder.limits.WIDGET_SIZE_LIMIT_BYTES, holder.timerWorker);
        this.deleteWidgetLogic = new DeleteWidgetLogic(holder.timerWorker);
        this.deleteDashLogic = new DeleteDashLogic(holder);
        this.updateDashSettingLogic = new UpdateDashSettingLogic(holder.limits.WIDGET_SIZE_LIMIT_BYTES);

        this.createDeviceLogic = new CreateDeviceLogic(holder);
        this.deleteDeviceLogic = new DeleteDeviceLogic(holder.tokenManager, holder.sessionDao);

        this.shareLogic = new ShareLogic(holder.sessionDao);
        this.redeemLogic = new RedeemLogic(holder.dbManager, holder.blockingIOProcessor);
        this.addEnergyLogic = new AddEnergyLogic(holder.dbManager, holder.blockingIOProcessor);

        this.createAppLogic = new CreateAppLogic(holder.limits.WIDGET_SIZE_LIMIT_BYTES);
        this.updateAppLogic = new UpdateAppLogic(holder.limits.WIDGET_SIZE_LIMIT_BYTES);

        this.loadProfileGzippedLogic = new LoadProfileGzippedLogic(holder);
        this.getProjectByTokenLogic = new GetProjectByTokenLogic(holder);
        this.mailQRsLogic = new MailQRsLogic(holder);
        this.updateFaceLogic = new UpdateFaceLogic(holder);

        this.state = state;
        this.stats = holder.stats;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, StringMessage msg) {
        this.stats.incrementAppStat();
        switch (msg.command) {
            case HARDWARE:
                hardwareApp.messageReceived(ctx, state, msg);
                break;
            case ACTIVATE_DASHBOARD :
                activateDashboardLogic.messageReceived(ctx, state, msg);
                break;
            case DEACTIVATE_DASHBOARD :
                deActivateDashboardLogic.messageReceived(ctx, state, msg);
                break;
            case LOAD_PROFILE_GZIPPED :
                loadProfileGzippedLogic.messageReceived(ctx, state, msg);
                break;
            case SHARING :
                shareLogic.messageReceived(ctx, state, msg);
                break;
            case GET_TOKEN :
                token.messageReceived(ctx, state.user, msg);
                break;
            case ASSIGN_TOKEN :
                assignTokenLogic.messageReceived(ctx, state.user, msg);
                break;
            case ADD_PUSH_TOKEN :
                AddPushLogic.messageReceived(ctx, state, msg);
                break;
            case REFRESH_TOKEN :
                refreshToken.messageReceived(ctx, state.user, msg);
                break;
            case GET_GRAPH_DATA :
                graphData.messageReceived(ctx, state.user, msg);
                break;
            case EXPORT_GRAPH_DATA :
                exportGraphData.messageReceived(ctx, state.user, msg);
                break;
            case PING :
                PingLogic.messageReceived(ctx, msg.id);
                break;
            case GET_SHARE_TOKEN :
                getShareTokenLogic.messageReceived(ctx, state.user, msg);
                break;
            case REFRESH_SHARE_TOKEN :
                refreshShareTokenLogic.messageReceived(ctx, state, msg);
                break;
            case GET_SHARED_DASH :
                getSharedDashLogic.messageReceived(ctx, msg);
                break;
            case EMAIL :
                appMailLogic.messageReceived(ctx, state.user, msg);
                break;
            case CREATE_DASH :
                createDashLogic.messageReceived(ctx, state, msg);
                break;
            case UPDATE_DASH:
                updateDashLogic.messageReceived(ctx, state, msg);
                break;
            case DELETE_DASH :
                deleteDashLogic.messageReceived(ctx, state, msg);
                break;
            case CREATE_WIDGET :
                createWidgetLogic.messageReceived(ctx, state, msg);
                break;
            case UPDATE_WIDGET :
                updateWidgetLogic.messageReceived(ctx, state, msg);
                break;
            case DELETE_WIDGET :
                deleteWidgetLogic.messageReceived(ctx, state, msg);
                break;
            case REDEEM :
                redeemLogic.messageReceived(ctx, state.user, msg);
                break;
            case GET_ENERGY :
                GetEnergyLogic.messageReceived(ctx, state.user, msg);
                break;
            case ADD_ENERGY :
                addEnergyLogic.messageReceived(ctx, state.user, msg);
                break;
            case UPDATE_PROJECT_SETTINGS :
                updateDashSettingLogic.messageReceived(ctx, state, msg);
                break;
            case CREATE_DEVICE :
                createDeviceLogic.messageReceived(ctx, state.user, msg);
                break;
            case UPDATE_DEVICE :
                UpdateDeviceLogic.messageReceived(ctx, state.user, msg);
                break;
            case DELETE_DEVICE :
                deleteDeviceLogic.messageReceived(ctx, state, msg);
                break;
            case GET_DEVICES :
                GetDevicesLogic.messageReceived(ctx, state.user, msg);
                break;
            case CREATE_TAG :
                CreateTagLogic.messageReceived(ctx, state.user, msg);
                break;
            case UPDATE_TAG :
                UpdateTagLogic.messageReceived(ctx, state.user, msg);
                break;
            case DELETE_TAG :
                DeleteTagLogic.messageReceived(ctx, state.user, msg);
                break;
            case GET_TAGS :
                GetTagsLogic.messageReceived(ctx, state.user, msg);
                break;
            case APP_SYNC :
                AppSyncLogic.messageReceived(ctx, state, msg);
                break;
            case CREATE_APP :
                createAppLogic.messageReceived(ctx, state, msg);
                break;
            case UPDATE_APP :
                updateAppLogic.messageReceived(ctx, state, msg);
                break;
            case DELETE_APP :
                DeleteAppLogic.messageReceived(ctx, state, msg);
                break;
            case GET_PROJECT_BY_TOKEN :
                getProjectByTokenLogic.messageReceived(ctx, state.user, msg);
                break;
            case EMAIL_QR :
                mailQRsLogic.messageReceived(ctx, state.user, msg);
                break;
            case UPDATE_FACE :
                updateFaceLogic.messageReceived(ctx, state.user, msg);
                break;
        }
    }

    @Override
    public StateHolderBase getState() {
        return state;
    }
}
