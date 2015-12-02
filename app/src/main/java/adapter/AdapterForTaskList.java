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
    private final int notStartedTaskColor;
    private final int startedTaskColor;
    private final int completedTaskColor;


    public AdapterForTaskList(Context ctx, ArrayList<Task> tasks) {
        mLayoutInflater = LayoutInflater.from(ctx);
        mTasks = tasks;

        mDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        notStartedTaskColor = ctx.getResources().getColor(R.color.not_started_task);
        startedTaskColor = ctx.getResources().getColor(R.color.started_task);
        completedTaskColor = ctx.getResources().getColor(R.color.completed_task);
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
            convertView = mLayoutInflater.inflate(R.layout.list_task_item, parent, false);
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
            convertView.setBackgroundColor(completedTaskColor);
            taskDateS = mDateFormat.format(task.getDateStart()) + " - " +
                    mDateFormat.format(task.getDateEnd()) +
                    " " + String.format("%02d", task.getSpentHours()) + ":" +
                    String.format("%02d", task.getSpentMinutes());
        } else if (task.getDateStart() != null) {
            convertView.setBackgroundColor(startedTaskColor);
            taskDateS = mDateFormat.format(task.getDateStart());
        } else
            convertView.setBackgroundColor(notStartedTaskColor);
        viewHolder.taskDate.setText(taskDateS);
        return convertView;
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
