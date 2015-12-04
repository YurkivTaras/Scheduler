package utils;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

import adapter.AdapterForTaskList;
import other.Task;

public class TasksLoader {
    public static final String JSON_PREFERENCES = "listOfTasks";
    public static final int SAVE = 1;
    public static final int READ = 2;
    private int mAction;

    private final Gson mGson;
    private ArrayList<Task> mTasks;
    private ArrayList<Task> mJTasks;
    private AdapterForTaskList mListAdapter;

    private BackgroundThread backgroundThread;
    private SharedPreferences jSonSharedPreferences;

    private TasksLoader(ArrayList<Task> tasks, Context context, int action) {
        mGson = new Gson();
        mTasks = tasks;
        mJTasks = new ArrayList<Task>();
        mAction = action;
        jSonSharedPreferences = context.getSharedPreferences(JSON_PREFERENCES, Context.MODE_PRIVATE);
    }

    public static TasksLoader createSaver(ArrayList<Task> tasks, Context context) {
        return new TasksLoader(tasks, context, SAVE);
    }

    public static TasksLoader createReader(ArrayList<Task> tasks, Context context, AdapterForTaskList listAdapter) {
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
                editor.putString(JSON_PREFERENCES, mGson.toJson(mTasks));
                editor.apply();
            } else if (jSonSharedPreferences.contains(JSON_PREFERENCES)) {
                Type collectionType = new TypeToken<ArrayList<Task>>() {
                }.getType();
                mJTasks = mGson.fromJson(jSonSharedPreferences.getString(JSON_PREFERENCES, null), collectionType);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (mAction == READ) {
                for (Task task : mJTasks)
                    mTasks.add(task);
                mListAdapter.notifyDataSetChanged();
            }
        }
    }
}

