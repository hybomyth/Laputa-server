package com.laputa.server.core.stats;

import java.util.concurrent.atomic.LongAdder;

/**
 * The Laputa Project.
 * Created by Sommer
 * Created on 2/13/2015.
 */
public class GlobalStats {

    private static final int LAST_COMMAND_INDEX = 72 + 2;

    private static final int APP_STAT_COUNTER_INDEX = LAST_COMMAND_INDEX - 1;
    private static final int MQTT_STAT_COUNTER_INDEX = LAST_COMMAND_INDEX - 2;

    //separate by income/outcome?
    public final Meter totalMessages;

    //2 last load adders are used as separate counters
    public final LongAdder[] specificCounters;

    public GlobalStats() {
        this.totalMessages = new Meter();

        //yeah, this is a bit ugly code, but as fast as possible =).
        this.specificCounters = new LongAdder[LAST_COMMAND_INDEX];
        for (int i = 0; i < LAST_COMMAND_INDEX; i++) {
            specificCounters[i] = new LongAdder();
        }
    }

    public void mark(final short cmd) {
        totalMessages.mark(1);
        specificCounters[cmd].increment();
    }

    public void incrementAppStat() {
        specificCounters[APP_STAT_COUNTER_INDEX].increment();
    }

    public void incrementMqttStat() {
        specificCounters[MQTT_STAT_COUNTER_INDEX].increment();
    }

    public long getTotalAppCounter(boolean reset) {
        LongAdder longAdder = specificCounters[APP_STAT_COUNTER_INDEX];
        return reset ? longAdder.sumThenReset() : longAdder.sum();
    }

    public long getTotalMqttCounter(boolean reset) {
        LongAdder longAdder = specificCounters[MQTT_STAT_COUNTER_INDEX];
        return reset ? longAdder.sumThenReset() : longAdder.sum();
    }

}
