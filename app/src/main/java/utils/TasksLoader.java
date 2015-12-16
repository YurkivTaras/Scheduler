package utils;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.y_taras.scheduler.activity.MainActivity;

import java.lang.reflect.Type;
import java.util.ArrayList;

import adapter.SwipeRecyclerViewAdapter;
import other.StringKeys;
import other.Task;

public class TasksLoader {

    public static final int SAVE = 1;
    public static final int READ = 2;
    private int mAction;

    private final Gson mGSON;
    private ArrayList<Task> mTasks;
    private ArrayList<Task> mJTasks;
    private SwipeRecyclerViewAdapter mListAdapter;
    private Context mContext;

    private BackgroundThread backgroundThread;
    private SharedPreferences jSonSharedPreferences;

    private TasksLoader(ArrayList<Task> tasks, Context context, int action) {
        mGSON = new Gson();
        mTasks = tasks;
        mJTasks = new ArrayList<Task>();
        mAction = action;
        mContext = context;
        jSonSharedPreferences = context.getSharedPreferences(StringKeys.JSON_PREFERENCES_FOR_TASKS, Context.MODE_PRIVATE);
    }

    public static TasksLoader createSaver(ArrayList<Task> tasks, Context context) {
        return new TasksLoader(tasks, context, SAVE);
    }

    public static TasksLoader createReader(ArrayList<Task> tasks, Context context, SwipeRecyclerViewAdapter listAdapter) {
        TasksLoader tasksLoader = new TasksLoader(tasks, context, READ);
        tasksLoader.mListAdapter = listAdapter;

        return tasksLoader;
    }

    public void execute() {
        backgroundThread = new BackgroundThread();
        backgroundThread.execute();
    }

    class BackgroundThread extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mJTasks.clear();
            if (mAction == SAVE)
                for (Task task : mTasks)
                    mJTasks.add(task);
        }


        @Override
        protected Void doInBackground(Void... params) {
            if (mAction == SAVE) {
                SharedPreferences.Editor editor = jSonSharedPreferences.edit();
                editor.putString(StringKeys.JSON_PREFERENCES_FOR_TASKS, mGSON.toJson(mTasks));
                editor.apply();
            } else if (jSonSharedPreferences.contains(StringKeys.JSON_PREFERENCES_FOR_TASKS)) {
                Type collectionType = new TypeToken<ArrayList<Task>>() {
                }.getType();
                mJTasks = mGSON.fromJson(jSonSharedPreferences.getString(StringKeys.JSON_PREFERENCES_FOR_TASKS, null), collectionType);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (mAction == READ) {
                for (Task task : mJTasks)
                    mTasks.add(task);
                ((MainActivity) mContext).setTimer();
                mListAdapter.notifyDataSetChanged();
            }
        }
    }
}

