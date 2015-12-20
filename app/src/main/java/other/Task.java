package other;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Comparator;
import java.util.Date;

public class Task implements Parcelable {
    private long mDatabase_ID;

    private String mTitle;
    private String mComment;

    private Date mDateStart;
    private Date mDateEnd;
    private Date mDateStop;

    public Task(String title, String comment) {
        mTitle = title;
        mComment = comment;
    }

    public Task(long id, String title, String comment, long dateStart, long dateStop, long dateEnd) {
        mDatabase_ID = id;
        mTitle = title;
        mComment = comment;
        mDateStart = dateStart != -1 ? new Date(dateStart) : null;
        mDateStop = dateStop != -1 ? new Date(dateStop) : null;
        mDateEnd = dateEnd != -1 ? new Date(dateEnd) : null;
    }

    public Task(Task task) {
        mDatabase_ID = task.mDatabase_ID;
        mTitle = task.mTitle;
        mComment = task.mComment;
        mDateStart = task.mDateStart != null ? new Date(task.mDateStart.getTime()) : null;
        mDateStop = task.mDateStop != null ? new Date(task.mDateStop.getTime()) : null;
        mDateEnd = task.mDateEnd != null ? new Date(task.mDateEnd.getTime()) : null;
    }

    public Task(Parcel source) {
        mTitle = source.readString();
        mComment = source.readString();
        mDatabase_ID = source.readLong();

        //відновлення Date об`єктів з використанням конструктора з long параметром
        long dataStart = source.readLong();
        long dataEnd = source.readLong();
        long dataStop = source.readLong();
        if (dataStart != -1) {
            mDateStart = new Date(dataStart);
            if (dataEnd != -1)
                mDateEnd = new Date(dataEnd);
            if (dataStop != -1)
                mDateStop = new Date(dataStop);
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mTitle);
        dest.writeString(mComment);
        dest.writeLong(mDatabase_ID);

        //запис в parcel long представлення об`єкту Date
        dest.writeLong(mDateStart != null ? mDateStart.getTime() : -1);
        dest.writeLong(mDateEnd != null ? mDateEnd.getTime() : -1);
        dest.writeLong(mDateStop != null ? mDateStop.getTime() : -1);
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
