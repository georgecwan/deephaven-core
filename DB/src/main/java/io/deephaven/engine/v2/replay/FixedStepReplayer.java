/*
 * Copyright (c) 2016-2021 Deephaven Data Labs and Patent Pending
 */

package io.deephaven.engine.v2.replay;

import io.deephaven.engine.time.DateTime;
import io.deephaven.engine.time.DateTimeUtils;

public class FixedStepReplayer extends Replayer {
    private long incrementNanos;
    private DateTime currentTime;

    public FixedStepReplayer(DateTime startTime, DateTime endTime, long incrementNanos) {
        super(startTime, endTime);
        this.incrementNanos = incrementNanos;
        currentTime = startTime;
    }

    @Override
    public DateTime currentTime() {
        return currentTime;
    }

    @Override
    public void run() {
        currentTime = DateTimeUtils.plus(currentTime, incrementNanos);
        if (currentTime.getNanos() > endTime.getNanos()) {
            currentTime = endTime;
        }
        super.run();
    }

    @Override
    public void setTime(long updatedTime) {
        currentTime = DateTimeUtils.millisToTime(Math.max(updatedTime, currentTime.getMillis()));
    }
}
