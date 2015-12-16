package adapter;


import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.scheduler.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import other.Task;

public class RVAdapterForTaskList extends RecyclerView.Adapter<RVAdapterForTaskList.TasksViewHolder> {
    private final SimpleDateFormat mDateFormat;

    private final ArrayList<Task> mTasks;

    private int mNotStartedTaskColor;
    private int mStartedTaskColor;
    private int mCompletedTaskColor;
    public OnItemClickListener mItemClickListener;
    public OnItemLongClickListener mItemLongClickListener;

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClick(View view, int position);
    }

    public RVAdapterForTaskList(ArrayList<Task> tasks, int notStartedTaskColor, int startedTaskColor, int completedTaskColor) {
        mTasks = tasks;
        mDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        mNotStartedTaskColor = notStartedTaskColor;
        mStartedTaskColor = startedTaskColor;
        mCompletedTaskColor = completedTaskColor;
    }

    public class TasksViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        private TextView mTaskTitle;
        private TextView mTaskComment;
        private TextView mTaskDate;
        private View mViewItem;

        TasksViewHolder(View itemView) {
            super(itemView);
            mViewItem = itemView;
            mTaskTitle = (TextView) itemView.findViewById(R.id.txtListTitle);
            mTaskComment = (TextView) itemView.findViewById(R.id.txtListComment);
            mTaskDate = (TextView) itemView.findViewById(R.id.txtListDate);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);

        }

        @Override
        public void onClick(View v) {
            if (mItemClickListener != null) {
                mItemClickListener.onItemClick(v,getAdapterPosition());
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (mItemLongClickListener != null)
                mItemLongClickListener.onItemLongClick(v, getPosition());
            return false;
        }
    }

    public void SetOnItemClickListener(final OnItemClickListener mItemClickListener) {
        this.mItemClickListener = mItemClickListener;
    }

    public void SetOnItemLongClickListener(final OnItemLongClickListener itemLongClickListener) {
        this.mItemLongClickListener = itemLongClickListener;
    }

    @Override
    public TasksViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_list_task, parent, false);
        TasksViewHolder vh = new TasksViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(TasksViewHolder holder, int position) {
        Task task = mTasks.get(position);
        holder.mTaskTitle.setText(task.getTitle());
        holder.mTaskComment.setText(task.getComment());

        String taskDateS = "";
        if (task.getDateEnd() != null) {
            holder.mViewItem.setBackgroundColor(mCompletedTaskColor);
            taskDateS = mDateFormat.format(task.getDateStart()) + " - " +
                    mDateFormat.format(task.getDateEnd()) +
                    " " + String.format("%02d", task.getSpentHours()) + ":" +
                    String.format("%02d", task.getSpentMinutes());
        } else if (task.getDateStart() != null) {
            holder.mViewItem.setBackgroundColor(mStartedTaskColor);
            taskDateS = mDateFormat.format(task.getDateStart());
        } else
            holder.mViewItem.setBackgroundColor(mNotStartedTaskColor);
        holder.mTaskDate.setText(taskDateS);
    }

    @Override
    public int getItemCount() {
        return mTasks.size();
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
}
