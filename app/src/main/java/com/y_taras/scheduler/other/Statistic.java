package com.y_taras.scheduler.other;

import java.util.Date;

public class Statistic {
    private long mTask_ID;

    private Date mDateStart, mDateEnd;
    private long mPauseLength;
    private boolean mUsed;

    public Statistic(long task_id, Date dateStart, Date dateEnd, long pauseLength) {
        mTask_ID = task_id;
        mDateStart = dateStart;
        mDateEnd = dateEnd;
        mPauseLength = pauseLength;
    }


    public Date getDateStart() {
        return mDateStart;
    }

    public Date getDateEnd() {
        return mDateEnd;
    }

    public long getPauseLength() {
        return mPauseLength;
    }

    public long getTask_ID() {
        return mTask_ID;
    }

    public boolean isUsed() {
        return mUsed;
    }

    public void setUsed(boolean used) {
        mUsed = used;
    }
}
