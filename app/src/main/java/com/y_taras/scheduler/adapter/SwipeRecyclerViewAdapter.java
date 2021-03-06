package com.y_taras.scheduler.adapter;


import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.daimajia.swipe.SwipeLayout;
import com.daimajia.swipe.adapters.RecyclerSwipeAdapter;
import com.y_taras.scheduler.R;
import com.y_taras.scheduler.activity.AddTaskActivity;
import com.y_taras.scheduler.activity.MainActivity;
import com.y_taras.scheduler.helper.DatabaseConnector;
import com.y_taras.scheduler.helper.ImageLoader;
import com.y_taras.scheduler.other.Constants;
import com.y_taras.scheduler.other.Task;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

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
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_list_task, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final Task task = mTasks.get(position);

        if (task.getCalendar_ID() == null) {
            holder.mCalendar.setText(R.string.export_to_gСalendar);
        } else
            holder.mCalendar.setText(R.string.update_task_in_gСalendar);
        holder.mTaskTitle.setText(task.getTitle());
        holder.mTaskComment.setText(task.getComment());
        if (task.isPeriodic())
            holder.mIfRepeat.setImageResource(R.drawable.repeat_icon);
        else
            holder.mIfRepeat.setImageDrawable(null);
        if (task.hasMapPoint())
            holder.mTaskAvatarMapPin.setImageResource(R.drawable.map_pin);
        else
            holder.mTaskAvatarMapPin.setImageDrawable(null);
        //загрузка іконки для завдання
        if (!task.getAvatarUri().equals(Task.DEFAULT_AVATAR_URI)) {
            Glide.with(mMainActivity)
                    .load(task.getAvatarUri())
                    .placeholder(R.drawable.placeholder)
                    .crossFade()
                    .into(holder.mTaskAvatar);
        } else
            holder.mTaskAvatar.setImageResource(R.drawable.default_avatar);
        String sTaskDate = "";
        if (task.getDateEnd() != null) {
            holder.mCalendar.setVisibility(View.VISIBLE);
            if (task.isPeriodic()) {
                holder.mViewItem.setBackgroundColor(mNotStartedTaskColor);
                holder.mBtnPause.setVisibility(View.GONE);
                holder.mBtnResume.setVisibility(View.GONE);
                holder.mBtnStart.setVisibility(View.VISIBLE);
                holder.mBtnFinish.setVisibility(View.GONE);
            } else {
                holder.mViewItem.setBackgroundColor(mCompletedTaskColor);
                holder.mBtnStart.setVisibility(View.INVISIBLE);
                holder.mBtnFinish.setVisibility(View.GONE);
                holder.mBtnPause.setVisibility(View.GONE);
                holder.mBtnResume.setVisibility(View.GONE);

                long spentTime = task.getDateEnd().getTime() - task.getDateStart().getTime() -
                        task.getPauseLengthAfterStop() - task.getPauseLengthBeforeStop();
                int spentHours = (int) (spentTime / (1000 * 60 * 60));
                int spentMinute = (int) (spentTime - spentHours * 1000 * 60 * 60) / 60000;
                sTaskDate = mDateFormat.format(task.getDateStart()) + " - " +
                        mDateFormat.format(task.getDateEnd()) + String.format(" %02d:%02d", spentHours, spentMinute);
            }
        } else if (task.getDateStart() != null) {
            holder.mCalendar.setVisibility(View.VISIBLE);
            holder.mViewItem.setBackgroundColor(mStartedTaskColor);
            sTaskDate = mDateFormat.format(task.getDateStart());
            holder.mBtnStart.setVisibility(View.GONE);
            if (task.getDatePause() == null) {
                holder.mBtnResume.setVisibility(View.GONE);
                holder.mBtnPause.setVisibility(View.VISIBLE);
                holder.mBtnFinish.setVisibility(View.VISIBLE);
            } else {
                holder.mBtnPause.setVisibility(View.GONE);
                holder.mBtnFinish.setVisibility(View.INVISIBLE);
                holder.mBtnResume.setVisibility(View.VISIBLE);
            }
        } else {
            holder.mViewItem.setBackgroundColor(mNotStartedTaskColor);
            holder.mBtnPause.setVisibility(View.GONE);
            holder.mBtnResume.setVisibility(View.GONE);
            holder.mBtnStart.setVisibility(View.VISIBLE);
            holder.mBtnFinish.setVisibility(View.GONE);
            holder.mCalendar.setVisibility(View.INVISIBLE);
        }
        holder.mTaskDate.setText(sTaskDate);
        holder.mSwipeLayout.setShowMode(SwipeLayout.ShowMode.PullOut);
        holder.mSwipeLayout.addDrag(SwipeLayout.DragEdge.Left, holder.mSwipeLayout.findViewById(R.id.left_swipe_menu));
        holder.mSwipeLayout.addDrag(SwipeLayout.DragEdge.Right, holder.mSwipeLayout.findViewById(R.id.right_swipe_menu));
        View.OnClickListener listenerForStateBtn = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder.mSwipeLayout.getOpenStatus() == SwipeLayout.Status.Close) {
                    Task clickTask = mTasks.get(position);
                    closeAllItems();
                    switch (v.getId()) {
                        case R.id.btnStart:
                            if (clickTask.getDateStart() == null || clickTask.isPeriodic()) {
                                clickTask.setDateStart(new Date());
                                clickTask.setDateStop(null);
                                clickTask.setDateEnd(null);
                                clickTask.setDatePause(null);
                                clickTask.setPauseLengthBeforeStop(0);
                                clickTask.setPauseLengthAfterStop(0);

                                DatabaseConnector.updateTask(clickTask, mMainActivity);
                                mMainActivity.sortTasks();
                                mMainActivity.setTimer();

                            }
                            break;
                        case R.id.btnFinish:
                            if (clickTask.getDateEnd() == null && clickTask.getDatePause() == null) {
                                clickTask.setDateEnd(new Date());
                                notifyItemChanged(position);
                                DatabaseConnector.updateTask(clickTask, mMainActivity);
                                //якщо завдання періодичне - добавляєм до таблці статистики нові дані
                                if (clickTask.isPeriodic())
                                    DatabaseConnector.addStatistic(clickTask.getDatabase_ID(),
                                            clickTask.getDateStart().getTime(), clickTask.getDateEnd().getTime(),
                                            clickTask.getPauseLengthAfterStop() + clickTask.getPauseLengthBeforeStop(), mMainActivity);
                                //запускаєм новий таймер,щоби двічі на закривався clickTask
                                mMainActivity.setTimer();
                            }
                            break;
                        case R.id.btnPause:
                            if (clickTask.getDateEnd() == null) {
                                clickTask.setDatePause(new Date());
                                notifyItemChanged(position);
                                DatabaseConnector.updateTask(clickTask, mMainActivity);
                                //запускаєм новий таймер, щоби не закривати завдання,
                                // яке було поставлено на паузу
                                mMainActivity.setTimer();
                            }
                            break;
                        case R.id.btnResume:
                            if (clickTask.getDatePause() != null) {
                                Date currentDate = new Date();
                                clickTask.setPauseLengthAfterStop(clickTask.getPauseLengthAfterStop() +
                                        (currentDate.getTime() - clickTask.getDatePause().getTime()));
                                clickTask.setDatePause(null);
                                notifyItemChanged(position);
                                DatabaseConnector.updateTask(clickTask, mMainActivity);
                                //запускаєм новий таймер, щоби врахувати завдання,
                                // яке щойно було знято із паузи
                                mMainActivity.setTimer();
                            }
                            break;
                    }
                }
            }
        };
        holder.mBtnStart.setOnClickListener(listenerForStateBtn);
        holder.mBtnFinish.setOnClickListener(listenerForStateBtn);
        holder.mBtnPause.setOnClickListener(listenerForStateBtn);
        holder.mBtnResume.setOnClickListener(listenerForStateBtn);

        View.OnClickListener listenerForSwipeItem = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Task clickTask = mTasks.get(position);
                mItemManger.closeAllItems();
                switch (v.getId()) {
                    case R.id.tvCalendar:
                        mMainActivity.exportTask(clickTask);
                        break;
                    case R.id.btnDelete:
                        mItemManger.removeShownLayouts(holder.mSwipeLayout);
                        mTasks.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, mTasks.size());
                        //запускаєм новий таймер,щоби на закривався таск, який уже був видалений
                        mMainActivity.setTimer();
                        //видаляєм файл з уже непотрібною іконкою
                        ImageLoader.delete(clickTask.getAvatarUri());
                        DatabaseConnector.deleteTask(clickTask.getDatabase_ID(), mMainActivity);
                        //видаляєм з таблиці статистики дані по видаленому завданню
                        DatabaseConnector.deleteStatistic(clickTask.getDatabase_ID(), mMainActivity);
                        break;
                    case R.id.tvResetStart:
                        if (clickTask.getDateStart() != null) {
                            //видаляєм останні дані статистики якщо завдання періодичне і
                            // було завершеним(тобто було збережено статистику)
                            if (clickTask.isPeriodic() && clickTask.getDateEnd() != null)
                                DatabaseConnector.deleteLastStatistic(clickTask.getDatabase_ID(), mMainActivity);
                            clickTask.setDateStart(null);
                            clickTask.setDateStop(null);
                            clickTask.setDateEnd(null);
                            clickTask.setDatePause(null);
                            clickTask.setPauseLengthBeforeStop(0);
                            clickTask.setPauseLengthAfterStop(0);
                            DatabaseConnector.updateTask(clickTask, mMainActivity);

                            mMainActivity.sortTasks();
                            //запускаєм новий таймер, щоби на закривався таск,
                            //який уже перейшов в стан нерозпочатого
                            mMainActivity.setTimer();
                        }
                        break;
                    case R.id.tvResetEnd:
                        if (clickTask.getDateEnd() != null) {
                            Date currentDate = new Date();
                            clickTask.setPauseLengthBeforeStop(clickTask.getPauseLengthBeforeStop() +
                                    clickTask.getPauseLengthAfterStop() +
                                    (currentDate.getTime() - clickTask.getDateEnd().getTime()));
                            clickTask.setPauseLengthAfterStop(0);

                            clickTask.setDateEnd(null);
                            clickTask.setDateStop(currentDate);
                            notifyItemChanged(position);
                            DatabaseConnector.updateTask(clickTask, mMainActivity);
                            //видаляєм останні дані статистики якщо завдання періодичне
                            if (clickTask.isPeriodic())
                                DatabaseConnector.deleteLastStatistic(clickTask.getDatabase_ID(), mMainActivity);
                            //запускаєм новий таймер, щоби зареєструвати таск,
                            //завершення якого було відхилено
                            mMainActivity.setTimer();
                        }
                        break;
                    case R.id.tvEdit:
                        Intent intent = new Intent(mMainActivity, AddTaskActivity.class);
                        intent.setAction(Constants.EDIT_TASK);
                        intent.putExtra(Constants.TASK_POSITION, position);
                        intent.putExtra(Constants.TASK_TITLE, clickTask.getTitle());
                        intent.putExtra(Constants.TASK_COMMENT, clickTask.getComment());
                        intent.putExtra(Constants.TYPE_OF_TASK, clickTask.isPeriodic());
                        intent.putExtra(Constants.MAX_RUNTIME_FOR_TASK, clickTask.getMaxRuntime());
                        intent.putExtra(Constants.BITMAP_AVATAR, clickTask.getAvatarUri());
                        intent.putExtra(Constants.MAP_POINT, clickTask.hasMapPoint());
                        if (clickTask.hasMapPoint()) {
                            intent.putExtra(Constants.LATITUDE, task.getLatitude());
                            intent.putExtra(Constants.LONGITUDE, task.getLongitude());
                        }
                        mMainActivity.startActivityForResult(intent, MainActivity.REQUEST_CODE_EDIT_TASK);
                        break;
                }
            }
        };
        holder.mCalendar.setOnClickListener(listenerForSwipeItem);
        holder.mBtnDelete.setOnClickListener(listenerForSwipeItem);
        holder.mTvResetStart.setOnClickListener(listenerForSwipeItem);
        holder.mTvResetEnd.setOnClickListener(listenerForSwipeItem);
        holder.mTvEdit.setOnClickListener(listenerForSwipeItem);

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
        ImageView mIfRepeat;
        ImageView mTaskAvatar;
        ImageView mTaskAvatarMapPin;
        TextView mTaskDate;
        View mViewItem;
        Button mBtnStart;
        Button mBtnFinish;
        ImageButton mBtnPause;
        ImageButton mBtnResume;

        SwipeLayout mSwipeLayout;
        TextView mCalendar;
        TextView mTvResetEnd;
        TextView mTvEdit;
        TextView mTvResetStart;
        ImageButton mBtnDelete;


        public ViewHolder(View itemView) {
            super(itemView);
            mSwipeLayout = (SwipeLayout) itemView.findViewById(R.id.swipe);
            mViewItem = itemView;
            mTaskTitle = (TextView) itemView.findViewById(R.id.txtListTitle);
            mTaskComment = (TextView) itemView.findViewById(R.id.txtListComment);
            mIfRepeat = (ImageView) itemView.findViewById(R.id.ifRepeatImageView);
            mTaskAvatar = (ImageView) itemView.findViewById(R.id.listAvatar);
            mTaskAvatarMapPin = (ImageView) itemView.findViewById(R.id.listAvatarMapPin);
            mTaskDate = (TextView) itemView.findViewById(R.id.txtListDate);
            mBtnStart = (Button) itemView.findViewById(R.id.btnStart);
            mBtnFinish = (Button) itemView.findViewById(R.id.btnFinish);
            mBtnPause = (ImageButton) itemView.findViewById(R.id.btnPause);
            mBtnResume = (ImageButton) itemView.findViewById(R.id.btnResume);

            mCalendar = (TextView) itemView.findViewById(R.id.tvCalendar);
            mTvResetEnd = (TextView) itemView.findViewById(R.id.tvResetEnd);
            mTvEdit = (TextView) itemView.findViewById(R.id.tvEdit);
            mTvResetStart = (TextView) itemView.findViewById(R.id.tvResetStart);
            mBtnDelete = (ImageButton) itemView.findViewById(R.id.btnDelete);
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
