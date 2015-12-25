package other;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Comparator;
import java.util.Date;

public class Task implements Parcelable {
    public static final String DEFAULT_AVATAR_URI = "default";
    private long mDatabase_ID;

    private String mTitle;
    private String mComment;
    private String mAvatarUri;

    private int mMaxRuntime;

    private Date mDateStart;
    private Date mDateEnd;
    private Date mDateStop;
    private Date mDatePause;

    private long mPauseLengthBeforeStop;
    private long mPauseLengthAfterStop;

    public Task(String title, String comment, int maxRuntime) {
        mTitle = title;
        mComment = comment;
        mMaxRuntime = maxRuntime;
        mAvatarUri = DEFAULT_AVATAR_URI;
    }

    public Task(long id, String title, String comment, String avatarUri, int maxRuntime, long dateStart, long dateStop,
                long dateEnd, long datePause, long pauseLengthBeforeStop, long pauseLengthAfterStop) {
        mDatabase_ID = id;
        mTitle = title;
        mComment = comment;
        mAvatarUri = avatarUri;
        mMaxRuntime = maxRuntime;
        mDateStart = dateStart != -1 ? new Date(dateStart) : null;
        mDateStop = dateStop != -1 ? new Date(dateStop) : null;
        mDateEnd = dateEnd != -1 ? new Date(dateEnd) : null;
        mDatePause = datePause != -1 ? new Date(datePause) : null;
        mPauseLengthBeforeStop = pauseLengthBeforeStop;
        mPauseLengthAfterStop = pauseLengthAfterStop;
    }

    public Task(Task task) {
        mDatabase_ID = task.mDatabase_ID;
        mTitle = task.mTitle;
        mComment = task.mComment;
        mAvatarUri = task.mAvatarUri;
        mMaxRuntime = task.mMaxRuntime;
        mDateStart = task.mDateStart != null ? new Date(task.mDateStart.getTime()) : null;
        mDateStop = task.mDateStop != null ? new Date(task.mDateStop.getTime()) : null;
        mDateEnd = task.mDateEnd != null ? new Date(task.mDateEnd.getTime()) : null;
        mDatePause = task.mDatePause != null ? new Date(task.mDatePause.getTime()) : null;
        mPauseLengthBeforeStop = task.mPauseLengthBeforeStop;
        mPauseLengthAfterStop = task.mPauseLengthAfterStop;
    }

    public Task(Parcel source) {
        mDatabase_ID = source.readLong();
        mTitle = source.readString();
        mComment = source.readString();
        mAvatarUri = source.readString();
        mMaxRuntime = source.readInt();
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mDatabase_ID);
        dest.writeString(mTitle);
        dest.writeString(mComment);
        dest.writeString(mAvatarUri);
        dest.writeInt(mMaxRuntime);
        dest.writeLong(mPauseLengthBeforeStop);
        dest.writeLong(mPauseLengthAfterStop);

        //запис в parcel long представлення об`єкту Date
        dest.writeLong(mDateStart != null ? mDateStart.getTime() : -1);
        dest.writeLong(mDateEnd != null ? mDateEnd.getTime() : -1);
        dest.writeLong(mDateStop != null ? mDateStop.getTime() : -1);
        dest.writeLong(mDatePause != null ? mDatePause.getTime() : -1);
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
