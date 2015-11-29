package other;


import java.io.Serializable;
import java.util.Date;

public class Task implements Serializable {
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

    public void calcTimeSpent() {
        long spentTime = mDateEnd.getTime() - mDateStart.getTime();
        mSpentHours = (int) spentTime / (1000 * 60 * 60);
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

    public int getSpentMinute() {
        return mSpentMinute;
    }
}
