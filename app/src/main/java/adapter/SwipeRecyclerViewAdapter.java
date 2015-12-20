package adapter;


import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;
import com.daimajia.swipe.adapters.RecyclerSwipeAdapter;
import com.example.scheduler.R;
import com.y_taras.scheduler.activity.AddScheduleActivity;
import com.y_taras.scheduler.activity.MainActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import other.StringKeys;
import other.Task;
import utils.DatabaseConnector;

public class SwipeRecyclerViewAdapter extends RecyclerSwipeAdapter<SwipeRecyclerViewAdapter.ViewHolder> {
    private final SimpleDateFormat mDateFormat;

    private final ArrayList<Task> mTasks;

    private int mNotStartedTaskColor;
    private int mStartedTaskColor;
    private int mCompletedTaskColor;
    private MainActivity mMainActivity;

    public SwipeRecyclerViewAdapter(MainActivity mainActivity, ArrayList<Task> tasks, int notStartedTaskColor, int startedTaskColor, int completedTaskColor) {
        mMainActivity = mainActivity;

        mTasks = tasks;
        mDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        mNotStartedTaskColor = notStartedTaskColor;
        mStartedTaskColor = startedTaskColor;
        mCompletedTaskColor = completedTaskColor;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_list_task, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {

        Task task = mTasks.get(position);
        holder.mTaskTitle.setText(task.getTitle());
        holder.mTaskComment.setText(task.getComment());

        String sTaskDate = "";
        if (task.getDateEnd() != null) {
            holder.mViewItem.setBackgroundColor(mCompletedTaskColor);
            holder.mBtnStart.setVisibility(View.INVISIBLE);
            holder.mBtnFinish.setVisibility(View.GONE);

            long spentTime = task.getDateEnd().getTime() - task.getDateStart().getTime();
            long spentHours = (int) (spentTime / (1000 * 60 * 60));
            long spentMinute = (int) (spentTime - spentHours * 1000 * 60 * 60) / 60000;
            sTaskDate = mDateFormat.format(task.getDateStart()) + " - " +
                    mDateFormat.format(task.getDateEnd()) +
                    " " + String.format("%02d", spentHours) + ":" +
                    String.format("%02d", spentMinute);
        } else if (task.getDateStart() != null) {
            holder.mViewItem.setBackgroundColor(mStartedTaskColor);
            sTaskDate = mDateFormat.format(task.getDateStart());
            holder.mBtnStart.setVisibility(View.GONE);
            holder.mBtnFinish.setVisibility(View.VISIBLE);
        } else {
            holder.mViewItem.setBackgroundColor(mNotStartedTaskColor);
            holder.mBtnStart.setVisibility(View.VISIBLE);
            holder.mBtnFinish.setVisibility(View.GONE);
        }
        holder.mTaskDate.setText(sTaskDate);

        holder.swipeLayout.setShowMode(SwipeLayout.ShowMode.PullOut);
        holder.swipeLayout.addDrag(SwipeLayout.DragEdge.Left, holder.swipeLayout.findViewById(R.id.left_swipe_menu));
        holder.swipeLayout.addDrag(SwipeLayout.DragEdge.Right, holder.swipeLayout.findViewById(R.id.right_swipe_menu));
        View.OnClickListener listenerForStateBtn = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder.swipeLayout.getOpenStatus() == SwipeLayout.Status.Close) {
                    Task clickTask = mTasks.get(position);
                    switch (v.getId()) {
                        case R.id.btnStart:
                            if (clickTask.getDateStart() == null) {
                                clickTask.setDateStart(new Date());
                                DatabaseConnector.updateTask(clickTask, mMainActivity);
                                mMainActivity.sortTasks();
                                mMainActivity.setTimer();
                            }
                            break;
                        case R.id.btnFinish:
                            if (clickTask.getDateEnd() == null) {
                                clickTask.setDateEnd(new Date());
                                notifyItemChanged(position);
                                DatabaseConnector.updateTask(clickTask, mMainActivity);
                                //запускаєм новий таймер,щоби двічі на закривався clickTask
                                mMainActivity.setTimer();
                            }
                            break;
                    }
                }
            }
        };
        holder.mBtnStart.setOnClickListener(listenerForStateBtn);
        holder.mBtnFinish.setOnClickListener(listenerForStateBtn);

        View.OnClickListener listenerForSwipeItem = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Task clickTask = mTasks.get(position);
                mItemManger.closeAllItems();
                switch (v.getId()) {
                    case R.id.btnDelete:
                        mItemManger.removeShownLayouts(holder.swipeLayout);
                        mTasks.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, mTasks.size());
                        //запускаєм новий таймер,щоби на закривався таск, який уже був видалений
                        mMainActivity.setTimer();
                        DatabaseConnector.deleteTask(clickTask.getDatabase_ID(), mMainActivity);
                        break;
                    case R.id.tvResetStart:
                        if (clickTask.getDateStart() != null) {
                            clickTask.setDateStart(null);
                            clickTask.setDateStop(null);
                            clickTask.setDateEnd(null);
                            DatabaseConnector.updateTask(clickTask, mMainActivity);
                            mMainActivity.sortTasks();
                            //запускаєм новий таймер,щоби на закривався таск,
                            //який уже перейшов в стан нерозпочатого
                            mMainActivity.setTimer();
                        }
                        break;
                    case R.id.tvResetEnd:
                        if (clickTask.getDateEnd() != null) {
                            clickTask.setDateEnd(null);
                            clickTask.setDateStop(new Date());
                            notifyItemChanged(position);
                            DatabaseConnector.updateTask(clickTask, mMainActivity);
                            //запускаєм новий таймер, щоби зареєструвати таск,
                            //завершення якого було відхилено
                            mMainActivity.setTimer();
                        }
                        break;
                    case R.id.tvEdit:
                        Intent intent = new Intent(mMainActivity, AddScheduleActivity.class);
                        intent.setAction(StringKeys.EDIT_TASK);
                        intent.putExtra(StringKeys.TASK_POSITION, position);
                        intent.putExtra(StringKeys.TASK_TITLE, clickTask.getTitle());
                        intent.putExtra(StringKeys.TASK_COMMENT, clickTask.getComment());
                        mMainActivity.startActivityForResult(intent, MainActivity.REQUEST_CODE_EDIT_TASK);
                        break;
                }
            }
        };
        holder.btnDelete.setOnClickListener(listenerForSwipeItem);
        holder.tvResetStart.setOnClickListener(listenerForSwipeItem);
        holder.tvResetEnd.setOnClickListener(listenerForSwipeItem);
        holder.tvEdit.setOnClickListener(listenerForSwipeItem);

        mItemManger.bindView(holder.itemView, position);
    }

    @Override
    public int getItemCount() {
        return mTasks.size();
    }

    @Override
    public int getSwipeLayoutResourceId(int position) {
        return R.id.swipe;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView mTaskTitle;
        TextView mTaskComment;
        TextView mTaskDate;
        View mViewItem;
        Button mBtnStart;
        Button mBtnFinish;

        SwipeLayout swipeLayout;
        TextView tvResetEnd;
        TextView tvEdit;
        TextView tvResetStart;
        ImageButton btnDelete;


        public ViewHolder(View itemView) {
            super(itemView);
            swipeLayout = (SwipeLayout) itemView.findViewById(R.id.swipe);
            mViewItem = itemView;
            mTaskTitle = (TextView) itemView.findViewById(R.id.txtListTitle);
            mTaskComment = (TextView) itemView.findViewById(R.id.txtListComment);
            mTaskDate = (TextView) itemView.findViewById(R.id.txtListDate);
            mBtnStart = (Button) itemView.findViewById(R.id.btnStart);
            mBtnFinish = (Button) itemView.findViewById(R.id.btnFinish);

            tvResetEnd = (TextView) itemView.findViewById(R.id.tvResetEnd);
            tvEdit = (TextView) itemView.findViewById(R.id.tvEdit);
            tvResetStart = (TextView) itemView.findViewById(R.id.tvResetStart);
            btnDelete = (ImageButton) itemView.findViewById(R.id.btnDelete);

        }

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
