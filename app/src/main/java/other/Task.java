package other;


import java.io.Serializable;

public class Task implements Serializable{
    private String mTitle;
    private String mComment;

    public Task(String title, String comment) {
        mTitle = title;
        mComment = comment;
    }

    public String getComment() {
        return mComment;
    }

    public String getTitle() {
        return mTitle;
    }
}
