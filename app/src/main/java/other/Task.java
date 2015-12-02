package other;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

public class Task implements Parcelable {
    private String mTitle;
    private String mComment;
    private Date mDateStart;

    private Date mDateEnd;
    private int mSpentHours;
    private int mSpentMinute;

    public Task(String title, String comment) {
        mTitle = title;
        mComment = comment;
    }

    public Task(String title, String comment, Date dateStart, Date dateEnd, int spentHours, int spentMinute) {
        mTitle = title;
        mComment = comment;
        mDateStart = dateStart;
        mDateEnd = dateEnd;
        mSpentHours = spentHours;
        mSpentMinute = spentMinute;
    }

    public Task(Parcel source) {
        mTitle = source.readString();
        mComment = source.readString();
        mSpentHours = source.readInt();
        mSpentMinute = source.readInt();

        //відновлення Date об`єктів з використанням конструктора з long параметром
        long dataStart = source.readLong();
        long dataEnd = source.readLong();
        if (dataStart != -1) {
            mDateStart = new Date(dataStart);
            if (dataEnd != -1)
                mDateEnd = new Date(dataEnd);
        }
    }

    //обрахунок кількості годин і хвилин потрачених на виконання завдання
    public void calcTimeSpent() {
        long spentTime = mDateEnd.getTime() - mDateStart.getTime();
        mSpentHours = (int) (spentTime / (1000 * 60 * 60));
        mSpentMinute = (int) (spentTime - mSpentHours * 1000 * 60 * 60) / 60000;
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

    public int getSpentHours() {
        return mSpentHours;
    }

    public int getSpentMinutes() {
        return mSpentMinute;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mTitle);
        dest.writeString(mComment);
        dest.writeInt(mSpentHours);
        dest.writeInt(mSpentMinute);

        //запис в parcel long представлення об`єкту Date
        dest.writeLong(mDateStart != null ? mDateStart.getTime() : -1);
        dest.writeLong(mDateEnd != null ? mDateEnd.getTime() : -1);
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
