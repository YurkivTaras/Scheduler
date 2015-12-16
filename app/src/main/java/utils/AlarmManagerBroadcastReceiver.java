package utils;


import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.example.scheduler.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.y_taras.scheduler.activity.MainActivity;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;

import other.StringKeys;
import other.Task;

public class AlarmManagerBroadcastReceiver extends BroadcastReceiver {

    private static final String LOG_ON_RECEIVE = "BroadcastReceiver onReceive()";
    private static final String LOG_ON_SET_TIMER = "BroadcastReceiver setTimer()";
    private static final int NOTIFY_ID = 101;


    @SuppressLint("LongLogTag")
    @Override
    public void onReceive(Context context, Intent intent) {
        Gson GSON = new Gson();
        SharedPreferences jSonSharedPreferences = context.getSharedPreferences(
                StringKeys.JSON_PREFERENCES_FOR_TASKS, Context.MODE_PRIVATE);

        Bundle extras = intent.getExtras();
        Log.d(LOG_ON_RECEIVE, extras.getInt(StringKeys.MAX_RUNTIME_FOR_TASK) + "");

        ArrayList<Task> tasks = extras.getParcelableArrayList(StringKeys.ARRAY_OF_TASKS);
        int maxRuntime = extras.getInt(StringKeys.MAX_RUNTIME_FOR_TASK);

        if (tasks == null) {
            Log.d(LOG_ON_RECEIVE, "task==null");
            Type collectionType = new TypeToken<ArrayList<Task>>() {
            }.getType();

            tasks = GSON.fromJson(jSonSharedPreferences.getString(StringKeys.JSON_PREFERENCES_FOR_TASKS, null), collectionType);
            maxRuntime = jSonSharedPreferences.getInt(StringKeys.MAX_RUNTIME_FOR_TASK, 0);
        }

        Log.d(LOG_ON_RECEIVE, "maxRuntime =" + maxRuntime);
        Log.d(LOG_ON_RECEIVE, "tasks.size()=" + tasks.size());

        Date currentDate = new Date();
        boolean ifWasChanged = false;
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            if (task.getDateStart() != null)
                Log.d(LOG_ON_RECEIVE, "task[" + i + "].getDateStart()!=null " + (currentDate.getTime() - task.getDateStart().getTime()));
            if (task.getDateEnd() == null && task.getDateStart() != null &&
                    (currentDate.getTime() - task.getDateStart().getTime()) >= maxRuntime * 60000 && task.getDateStop() == null) {
                Log.d(LOG_ON_RECEIVE, (
                        "rizn [" + i + "]= " + (maxRuntime * 60000 - (currentDate.getTime() - task.getDateStart().getTime()))));
                task.setDateEnd(currentDate);
                task.calcTimeSpent();
                ifWasChanged = true;
                showNotification(context, task);
            } else if (task.getDateStop() != null && task.getDateStart() != null && task.getDateEnd() == null &&
                    (currentDate.getTime() - task.getDateStop().getTime()) >= maxRuntime * 60000) {
                Log.d(LOG_ON_RECEIVE, (
                        "dataStop[" + i + "]= " + (maxRuntime * 60000 - (currentDate.getTime() - task.getDateStop().getTime()))));
                task.setDateEnd(currentDate);
                task.calcTimeSpent();
                ifWasChanged = true;
                showNotification(context, task);
            }
        }
        if (ifWasChanged) {
            SharedPreferences.Editor editor = jSonSharedPreferences.edit();
            editor.putString(StringKeys.JSON_PREFERENCES_FOR_TASKS, GSON.toJson(tasks));
            editor.apply();

            Intent i = new Intent("broadCastName");
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList(StringKeys.ARRAY_OF_TASKS, tasks);
            i.putExtras(bundle);
            context.sendBroadcast(i);
        }
        setTimer(context, maxRuntime, tasks);
    }


    @SuppressLint("LongLogTag")
    public void setTimer(Context context, int maxRuntime, ArrayList<Task> tasks) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmManagerBroadcastReceiver.class);
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(StringKeys.ARRAY_OF_TASKS, tasks);
        intent.putExtras(bundle);
        intent.putExtra(StringKeys.MAX_RUNTIME_FOR_TASK, maxRuntime);

        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        long currentDate = new Date().getTime();
        long minStartDate = currentDate;

        boolean ifHaveNotCompletedTask = false;
        int i = 0;
        for (Task task : tasks) {
            i++;
            if (task.getDateEnd() == null && task.getDateStart() != null &&
                    minStartDate >= task.getDateStart().getTime()) {
                ifHaveNotCompletedTask = true;
                if (task.getDateStop() != null && minStartDate >= task.getDateStop().getTime()) {
                    minStartDate = task.getDateStop().getTime();
                    Log.d(LOG_ON_SET_TIMER, "minStartDate=dateStop[" + i + "]");
                } else {
                    minStartDate = task.getDateStart().getTime();
                    Log.d(LOG_ON_SET_TIMER, (
                            "rizn [" + i + "]= " + (maxRuntime * 60000 - (currentDate - task.getDateStart().getTime()))));
                }
            }
        }
        Log.d(LOG_ON_SET_TIMER, "tasks.size()=" + tasks.size());
        Log.d(LOG_ON_SET_TIMER, "maxRuntime= " + maxRuntime);
        Log.d(LOG_ON_SET_TIMER, "minStartDate= " + minStartDate);
        Log.d(LOG_ON_SET_TIMER, "currentDate= " + currentDate);
        if (ifHaveNotCompletedTask)
            am.set(AlarmManager.RTC, (maxRuntime * 60000 + minStartDate) + 1, pi);
    }

    private void showNotification(Context context, Task task) {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        //для повернення на MainActivity, якщо при переході
        // по Notification додаток працював з іншими активностями
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context,
                0, notificationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.finish_task_icon)
                .setContentTitle(context.getString(R.string.app_name))
                .setAutoCancel(true)
                .setContentText("Завдання " + task.getTitle() + " автоматично завершилось");

        Notification notification = builder.build();
        //notification.defaults |= Notification.DEFAULT_SOUND;
        //notification.defaults |= Notification.DEFAULT_VIBRATE;
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(NOTIFY_ID, notification);

    }
}