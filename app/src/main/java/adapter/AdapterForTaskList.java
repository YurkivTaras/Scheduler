package adapter;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.scheduler.R;

import java.util.ArrayList;

import other.Task;

public class AdapterForTaskList extends BaseAdapter {
    private final LayoutInflater mLayoutInflater;
    private final ArrayList<Task> mTasks;

    public AdapterForTaskList(Context ctx, ArrayList<Task> tasks) {
        mLayoutInflater = LayoutInflater.from(ctx);
        mTasks = tasks;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = mLayoutInflater.inflate(R.layout.list_task_item, parent, false);
        TextView taskTitle = (TextView) convertView
                .findViewById(R.id.txtListTitle);
        taskTitle
                .setText(mTasks.get(position).getTitle());
        TextView taskComment = (TextView) convertView
                .findViewById(R.id.txtListComment);
        taskComment
                .setText(mTasks.get(position).getComment());
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
