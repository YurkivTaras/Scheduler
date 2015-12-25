package adapter;


import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import utils.ImageLoader;

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

        final Task task = mTasks.get(position);
        holder.mTaskTitle.setText(task.getTitle());
        holder.mTaskComment.setText(task.getComment());
        if (!task.getAvatarUri().equals(Task.DEFAULT_AVATAR_URI)) {
            new AsyncTask<String, Void, Bitmap>() {
                @Override
                protected Bitmap doInBackground(String... params) {
                    return ImageLoader.loadImage(params[0]);
                }

                @Override
                protected void onPostExecute(Bitmap img) {
                    holder.mTaskAvatar.setImageBitmap(img);
                }
            }.execute(task.getAvatarUri());
        } else
            holder.mTaskAvatar.setImageResource(R.drawable.default_avatar);
        String sTaskDate = "";
        if (task.getDateEnd() != null) {
            holder.mViewItem.setBackgroundColor(mCompletedTaskColor);
            holder.mBtnStart.setVisibility(View.INVISIBLE);
            holder.mBtnFinish.setVisibility(View.GONE);
            holder.mBtnPause.setVisibility(View.GONE);
            holder.mBtnResume.setVisibility(View.GONE);

            long spentTime = task.getDateEnd().getTime() - task.getDateStart().getTime() -
                    task.getPauseLengthAfterStop() - task.getPauseLengthBeforeStop();
            long spentHours = (int) (spentTime / (1000 * 60 * 60));
            long spentMinute = (int) (spentTime - spentHours * 1000 * 60 * 60) / 60000;
            sTaskDate = mDateFormat.format(task.getDateStart()) + " - " +
                    mDateFormat.format(task.getDateEnd()) +
                    " " + String.format("%02d:%02d", spentHours, spentMinute);
        } else if (task.getDateStart() != null) {
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
                            if (clickTask.getDateEnd() == null && clickTask.getDatePause() == null) {
                                clickTask.setDateEnd(new Date());
                                notifyItemChanged(position);
                                DatabaseConnector.updateTask(clickTask, mMainActivity);
                                //запускаєм новий таймер,щоби двічі на закривався clickTask
                                mMainActivity.setTimer();
                            }
                            break;
                        case R.id.btnPause:
                            if (task.getDateEnd() == null) {
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
                    case R.id.btnDelete:
                        mItemManger.removeShownLayouts(holder.swipeLayout);
                        mTasks.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, mTasks.size());
                        //запускаєм новий таймер,щоби на закривався таск, який уже був видалений
                        mMainActivity.setTimer();
                        //видаляєм файл з уже непотрібною іконкою
                        ImageLoader.delete(task.getAvatarUri());
                        DatabaseConnector.deleteTask(clickTask.getDatabase_ID(), mMainActivity);
                        break;
                    case R.id.tvResetStart:
                        if (clickTask.getDateStart() != null) {
                            clickTask.setDateStart(null);
                            clickTask.setDateStop(null);
                            clickTask.setDateEnd(null);
                            clickTask.setDatePause(null);
                            clickTask.setPauseLengthBeforeStop(0);
                            clickTask.setPauseLengthAfterStop(0);
                            DatabaseConnector.updateTask(clickTask, mMainActivity);
                            mMainActivity.sortTasks();
                            //запускаєм новий таймер,щоби на закривався таск,
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
                        intent.putExtra(StringKeys.MAX_RUNTIME_FOR_TASK, clickTask.getMaxRuntime());
                        intent.putExtra(StringKeys.BITMAP_AVATAR, clickTask.getAvatarUri());
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
        ImageView mTaskAvatar;
        TextView mTaskDate;
        View mViewItem;
        Button mBtnStart;
        Button mBtnFinish;
        ImageButton mBtnPause;
        ImageButton mBtnResume;

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
            mTaskAvatar = (ImageView) itemView.findViewById(R.id.listAvatar);
            mTaskDate = (TextView) itemView.findViewById(R.id.txtListDate);
            mBtnStart = (Button) itemView.findViewById(R.id.btnStart);
            mBtnFinish = (Button) itemView.findViewById(R.id.btnFinish);
            mBtnPause = (ImageButton) itemView.findViewById(R.id.btnPause);
            mBtnResume = (ImageButton) itemView.findViewById(R.id.btnResume);

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
