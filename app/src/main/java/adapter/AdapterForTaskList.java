package adapter;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.scheduler.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import other.Task;

public class AdapterForTaskList extends BaseAdapter {
    private final SimpleDateFormat mDateFormat;
    private final LayoutInflater mLayoutInflater;
    private final ArrayList<Task> mTasks;

    private int mNotStartedTaskColor;
    private int mStartedTaskColor;
    private int mCompletedTaskColor;


    public AdapterForTaskList(Context ctx, ArrayList<Task> tasks, int notStartedTaskColor, int startedTaskColor, int completedTaskColor) {
        mLayoutInflater = LayoutInflater.from(ctx);
        mTasks = tasks;

        mDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        mNotStartedTaskColor = notStartedTaskColor;
        mStartedTaskColor = startedTaskColor;
        mCompletedTaskColor = completedTaskColor;
    }

    static class ViewHolder {
        TextView taskTitle;
        TextView taskComment;
        TextView taskDate;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.row_list_task, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.taskTitle = (TextView) convertView.findViewById(R.id.txtListTitle);
            viewHolder.taskComment = (TextView) convertView.findViewById(R.id.txtListComment);
            viewHolder.taskDate = (TextView) convertView.findViewById(R.id.txtListDate);
            convertView.setTag(viewHolder);
        } else
            viewHolder = (ViewHolder) convertView.getTag();

        Task task = mTasks.get(position);

        viewHolder.taskTitle.setText(task.getTitle());
        viewHolder.taskComment.setText(task.getComment());

        String taskDateS = "";
        if (task.getDateEnd() != null) {
            convertView.setBackgroundColor(mCompletedTaskColor);
            taskDateS = mDateFormat.format(task.getDateStart()) + " - " +
                    mDateFormat.format(task.getDateEnd()) +
                    " " + String.format("%02d", task.getSpentHours()) + ":" +
                    String.format("%02d", task.getSpentMinutes());
        } else if (task.getDateStart() != null) {
            convertView.setBackgroundColor(mStartedTaskColor);
            taskDateS = mDateFormat.format(task.getDateStart());
        } else
            convertView.setBackgroundColor(mNotStartedTaskColor);
        viewHolder.taskDate.setText(taskDateS);
        return convertView;
    }

    public void setNotStartedTaskColor(int notStartedTaskColor) {
        mNotStartedTaskColor = notStartedTaskColor;
    }

    public void setStartedTaskColor(int startedTaskColor) {
        mStartedTaskColor = startedTaskColor;
    }

    public void setCompletedTaskColor(int completedTaskColor) {
        mCompletedTaskColor = completedTaskColor;
    }

    @Override
    public int getCount() {
        return mTasks.size();
    }

    @Override
    public Object getItem(int position) {
        return mTasks.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
}
