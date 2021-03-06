package com.y_taras.scheduler.other;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;

public class Task implements Parcelable, Serializable {
    public static final String DEFAULT_AVATAR_URI = "default";

    private long mDatabase_ID;
    private String mCalendar_ID;
    //заголовок
    private String mTitle;
    //коментар
    private String mComment;
    //посилання на іконку для завдання
    private String mAvatarUri;

    //періодичне завдання чи ні
    private boolean mIfPeriodic;
    //змінна для підрахування статистики
    private long mRuntime;

    //максимальний час виконання для завдання
    private int mMaxRuntime;
    private Date mDateStart, mDateEnd, mDateStop, mDatePause;

    //довжина паузи до моменту, останньго відновлення і після нього
    private long mPauseLengthBeforeStop, mPauseLengthAfterStop;

    //встановлено місце виконання чи ні
    private boolean mMapPoint;
    //координати завдання на карті
    private double mLatitude;
    private double mLongitude;

    public Task(String title, String comment, boolean ifPeriodic, int maxRuntime) {
        mTitle = title;
        mComment = comment;
        mIfPeriodic = ifPeriodic;
        mMaxRuntime = maxRuntime;
        mAvatarUri = DEFAULT_AVATAR_URI;
    }

    public Task(long id) {
        mDatabase_ID = id;
    }

    public Task(long id, String title, String comment, String avatarUri, long runtime) {
        mDatabase_ID = id;
        mTitle = title;
        mComment = comment;
        mAvatarUri = avatarUri;
        mRuntime = runtime;
    }

    public Task(long id, String calendar_id, String title, String comment, boolean ifPeriodic, String avatarUri, int maxRuntime, long dateStart, long dateStop,
                long dateEnd, long datePause, long pauseLengthBeforeStop, long pauseLengthAfterStop, boolean hasMapPoint, double latitude, double longitude) {
        mDatabase_ID = id;
        mCalendar_ID = calendar_id;
        mTitle = title;
        mComment = comment;
        mIfPeriodic = ifPeriodic;
        mAvatarUri = avatarUri;
        mMaxRuntime = maxRuntime;
        mDateStart = dateStart != -1 ? new Date(dateStart) : null;
        mDateStop = dateStop != -1 ? new Date(dateStop) : null;
        mDateEnd = dateEnd != -1 ? new Date(dateEnd) : null;
        mDatePause = datePause != -1 ? new Date(datePause) : null;
        mPauseLengthBeforeStop = pauseLengthBeforeStop;
        mPauseLengthAfterStop = pauseLengthAfterStop;
        mMapPoint = hasMapPoint;
        mLatitude = latitude;
        mLongitude = longitude;
    }

    public Task(Task task) {
        mDatabase_ID = task.mDatabase_ID;
        mCalendar_ID = task.getCalendar_ID();
        mTitle = task.mTitle;
        mComment = task.mComment;
        mIfPeriodic = task.mIfPeriodic;
        mAvatarUri = task.mAvatarUri;
        mMaxRuntime = task.mMaxRuntime;
        mRuntime = task.mRuntime;
        mDateStart = task.mDateStart != null ? new Date(task.mDateStart.getTime()) : null;
        mDateStop = task.mDateStop != null ? new Date(task.mDateStop.getTime()) : null;
        mDateEnd = task.mDateEnd != null ? new Date(task.mDateEnd.getTime()) : null;
        mDatePause = task.mDatePause != null ? new Date(task.mDatePause.getTime()) : null;
        mPauseLengthBeforeStop = task.mPauseLengthBeforeStop;
        mPauseLengthAfterStop = task.mPauseLengthAfterStop;
        mMapPoint = task.mMapPoint;
        mLatitude = task.mLatitude;
        mLongitude = task.mLongitude;
    }

    public Task(Parcel source) {
        mDatabase_ID = source.readLong();
        mCalendar_ID = source.readString();
        mTitle = source.readString();
        mComment = source.readString();
        mIfPeriodic = source.readInt() == 1;
        mAvatarUri = source.readString();
        mMaxRuntime = source.readInt();
        mRuntime = source.readLong();
        mPauseLengthBeforeStop = source.readLong();
        mPauseLengthAfterStop = source.readLong();

        //відновлення Date об`єктів з використанням конструктора з long параметром
        long dataStart = source.readLong();
        long dataEnd = source.readLong();
        long dataStop = source.readLong();
        long dataPause = source.readLong();
        if (dataStart != -1) {
            mDateStart = new Date(dataStart);
            if (dataEnd != -1)
                mDateEnd = new Date(dataEnd);
            if (dataStop != -1)
                mDateStop = new Date(dataStop);
            if (dataPause != -1)
                mDatePause = new Date(dataPause);
        }
        mMapPoint = source.readInt() == 1;
        mLatitude = source.readDouble();
        mLongitude = source.readDouble();
    }

    public static Comparator<Task> NameUPComparator = new Comparator<Task>() {
        public int compare(Task t1, Task t2) {
            String title1 = t1.getTitle();
            String title2 = t2.getTitle();
            String comment1 = t1.getComment();
            String comment2 = t2.getComment();
            if (title1.compareTo(title2) == 0)
                return comment1.compareTo(comment2);
            else
                return title1.compareTo(title2);
        }
    };
    public static Comparator<Task> NameDownComparator = new Comparator<Task>() {
        public int compare(Task t1, Task t2) {
            return NameUPComparator.compare(t2, t1);
        }
    };
    public static Comparator<Task> DateUPComparator = new Comparator<Task>() {
        public int compare(Task t1, Task t2) {
            Date dateStart1 = t1.getDateStart();
            Date dateStart2 = t2.getDateStart();
            Long time1 = dateStart1 != null ? dateStart1.getTime() : 0;
            Long time2 = dateStart2 != null ? dateStart2.getTime() : 0;
            return time1.compareTo(time2);
        }
    };
    public static Comparator<Task> DateDownComparator = new Comparator<Task>() {
        public int compare(Task t1, Task t2) {
            return DateUPComparator.compare(t2, t1);
        }
    };

    public long getDatabase_ID() {
        return mDatabase_ID;
    }

    public void setDatabase_ID(long database_ID) {
        mDatabase_ID = database_ID;
    }

    public String getCalendar_ID() {
        return mCalendar_ID;
    }

    public void setCalendar_ID(String calendar_ID) {
        mCalendar_ID = calendar_ID;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public void setComment(String comment) {
        mComment = comment;
    }

    public String getComment() {
        return mComment;
    }

    public String getTitle() {
        return mTitle;
    }

    public int getMaxRuntime() {
        return mMaxRuntime;
    }

    public void setMaxRuntime(int maxRuntime) {
        mMaxRuntime = maxRuntime;
    }

    public Date getDateStart() {
        return mDateStart;
    }

    public void setDateStart(Date dateStart) {
        mDateStart = dateStart;
    }

    public Date getDateEnd() {
        return mDateEnd;
    }

    public void setDateEnd(Date dateEnd) {
        mDateEnd = dateEnd;
    }

    public Date getDateStop() {
        return mDateStop;
    }

    public void setDateStop(Date dateStop) {
        mDateStop = dateStop;
    }

    public long getPauseLengthBeforeStop() {
        return mPauseLengthBeforeStop;
    }

    public void setPauseLengthBeforeStop(long pauseLength) {
        mPauseLengthBeforeStop = pauseLength;
    }

    public long getPauseLengthAfterStop() {
        return mPauseLengthAfterStop;
    }

    public void setPauseLengthAfterStop(long pauseLength) {
        mPauseLengthAfterStop = pauseLength;
    }

    public Date getDatePause() {
        return mDatePause;
    }

    public void setDatePause(Date datePause) {
        mDatePause = datePause;
    }

    public String getAvatarUri() {
        return mAvatarUri;
    }

    public void setAvatarUri(String avatarUri) {
        mAvatarUri = avatarUri;
    }

    public long getRuntime() {
        return mRuntime;
    }

    public void setRuntime(long runtime) {
        this.mRuntime = runtime;
    }

    public boolean hasMapPoint() {
        return mMapPoint;
    }

    public void setMapPoint(boolean mapPoint) {
        mMapPoint = mapPoint;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public void setLatitude(double latitude) {
        mLatitude = latitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public void setLongitude(double longitude) {
        mLongitude = longitude;
    }

    public boolean isPeriodic() {
        return mIfPeriodic;
    }

    public void setPeriodic(boolean periodic) {
        mIfPeriodic = periodic;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mDatabase_ID);
        dest.writeString(mCalendar_ID);

        dest.writeString(mTitle);
        dest.writeString(mComment);
        dest.writeInt(mIfPeriodic ? 1 : 0);
        dest.writeString(mAvatarUri);
        dest.writeInt(mMaxRuntime);
        dest.writeLong(mRuntime);
        dest.writeLong(mPauseLengthBeforeStop);
        dest.writeLong(mPauseLengthAfterStop);

        //запис в parcel long представлення об`єкту Date
        dest.writeLong(mDateStart != null ? mDateStart.getTime() : -1);
        dest.writeLong(mDateEnd != null ? mDateEnd.getTime() : -1);
        dest.writeLong(mDateStop != null ? mDateStop.getTime() : -1);
        dest.writeLong(mDatePause != null ? mDatePause.getTime() : -1);

        dest.writeInt(mMapPoint ? 1 : 0);
        dest.writeDouble(mLatitude);
        dest.writeDouble(mLongitude);
    }

    public static final Parcelable.Creator<Task> CREATOR = new Parcelable.Creator<Task>() {
        @Override
        public Task createFromParcel(Parcel source) {
            return new Task(source);
        }

        @Override
        public Task[] newArray(int size) {
            return new Task[size];
        }
    };
}
