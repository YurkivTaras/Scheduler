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

    public AdapterForTaskList(Context ctx, ArrayList<Task> tasks) {
        mLayoutInflater = LayoutInflater.from(ctx);
        mTasks = tasks;
        mDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = mLayoutInflater.inflate(R.layout.list_task_item, parent, false);
        Task task = mTasks.get(position);

        TextView taskName = (TextView) convertView.findViewById(R.id.txtListTitle);
        taskName.setText(task.getTitle());
        TextView taskComment = (TextView) convertView.findViewById(R.id.txtListComment);
        taskComment.setText(task.getComment());

        TextView taskDate = (TextView) convertView.findViewById(R.id.txtListDate);
        String taskDateS = "";
        if (task.getDateEnd() != null) {
            taskDateS = mDateFormat.format(task.getDateStart()) + " - " +
                    mDateFormat.format(task.getDateEnd()) +
                    " " + String.format("%02d", task.getSpentHours()) + ":" +
                    String.format("%02d", task.getSpentMinute());
        } else if (task.getDateStart() != null)
            taskDateS = mDateFormat.format(task.getDateStart());
        taskDate.setText(taskDateS);
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
