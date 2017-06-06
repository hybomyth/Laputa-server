package com.laputa.server.core.processors;

import com.laputa.server.core.protocol.exceptions.QuotaLimitException;

/**
 * The Laputa Project.
 * Created by Sommer
 * Created on 29.07.15.
 */
public abstract class NotificationBase {

    private final long NOTIFICATION_QUOTA_LIMIT;
    private long lastSentTs;
    @SuppressWarnings("ThrowableInstanceNeverThrown")
    protected final static QuotaLimitException EXCEPTION_CACHE = new QuotaLimitException("Notification limit reached.");

    public NotificationBase(long defaultNotificationQuotaLimit) {
        this.NOTIFICATION_QUOTA_LIMIT = defaultNotificationQuotaLimit;
    }

    public void checkIfNotificationQuotaLimitIsNotReached() {
        checkIfNotificationQuotaLimitIsNotReached(System.currentTimeMillis());
    }

    public void checkIfNotificationQuotaLimitIsNotReached(final long currentTs) {
        final long timePassedSinceLastMessage = (currentTs - lastSentTs);
        if (timePassedSinceLastMessage < NOTIFICATION_QUOTA_LIMIT) {
            throw EXCEPTION_CACHE;
        }
        this.lastSentTs = currentTs;
    }

}
