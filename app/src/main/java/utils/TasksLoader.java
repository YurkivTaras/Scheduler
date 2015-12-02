package utils;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;

import adapter.AdapterForTaskList;
import other.Task;

public class TasksLoader {
    private static final String jTitle = "title";
    private static final String jComment = "comment";
    private static final String jSpentHours = "spentHours";
    private static final String jSpentMinutes = "spentMinutes";
    private static final String jStartDate = "startDate";
    private static final String jEndDate = "endDate";

    public static final String JSON_PREFERENCES = "listOfTasks";
    public static final int SAVE = 1;
    public static final int READ = 2;
    private int mAction;

    private ArrayList<Task> mTasks;
    private ArrayList<Task> mJTasks;
    private AdapterForTaskList mListAdapter;

    private BackgroundThread backgroundThread;
    private SharedPreferences jSonSharedPreferences;

    private TasksLoader(ArrayList<Task> tasks, Context context, int action) {
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

    private String jSonSerializer() {
        try {
            JSONArray jArr = new JSONArray();
            for (Task task : mTasks) {
                JSONObject jTask = new JSONObject();
                jTask.put(jTitle, task.getTitle());
                jTask.put(jComment, task.getComment());
                jTask.put(jSpentHours, task.getSpentHours());
                jTask.put(jSpentMinutes, task.getSpentMinutes());
                Date startDate = task.getDateStart();
                jTask.put(jStartDate, startDate != null ? startDate.getTime() : -1);
                Date endDate = task.getDateEnd();
                jTask.put(jEndDate, endDate != null ? endDate.getTime() : -1);
                jArr.put(jTask);
            }
            Log.d("JSON", jArr.toString());
            return jArr.toString();
        } catch (JSONException ex) {
            Log.e("JSON", "помилка при сереалізації");
            ex.printStackTrace();
        }
        return null;
    }

    private void jSonDeserializer(String data) {
        try {
            JSONArray jArr = new JSONArray(data);
            for (int i = 0; i < jArr.length(); i++) {
                JSONObject jTask = jArr.getJSONObject(i);
                String title = jTask.getString(jTitle);
                String comment = jTask.getString(jComment);
                int spentHours = jTask.getInt(jSpentHours);
                int spentMinutes = jTask.getInt(jSpentMinutes);
                long startDateLong = jTask.getLong(jStartDate);
                long endDateLong = jTask.getLong(jEndDate);
                Date startDate = startDateLong != -1 ? new Date(startDateLong) : null;
                Date endDate = endDateLong != -1 ? new Date(endDateLong) : null;
                mJTasks.add(new Task(title, comment, startDate, endDate, spentHours, spentMinutes));
            }
        } catch (JSONException e) {
            Log.e("JSON", "помилка при десереалізації");
            e.printStackTrace();
        }
    }

    class BackgroundThread extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d("BackgroundThread", "onPreExecute()" + mAction);
            mJTasks.clear();
            if (mAction == SAVE)
                for (Task task : mTasks)
                    mJTasks.add(task);
        }


        @Override
        protected Void doInBackground(Void... params) {
            Log.d("BackgroundThread", "doInBackground()" + mAction);
            if (mAction == SAVE) {
                SharedPreferences.Editor editor = jSonSharedPreferences.edit();
                editor.putString(JSON_PREFERENCES, jSonSerializer());
                editor.apply();
            } else if (jSonSharedPreferences.contains(JSON_PREFERENCES))
                jSonDeserializer(jSonSharedPreferences.getString(JSON_PREFERENCES, null));
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            Log.d("BackgroundThread", "onPostExecute()" + mAction);
            if (mAction == READ) {
                for (Task task : mJTasks)
                    mTasks.add(task);
                mListAdapter.notifyDataSetChanged();
            }
        }
    }
}

