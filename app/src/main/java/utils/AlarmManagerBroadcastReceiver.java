package utils;


import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.example.scheduler.R;
import com.y_taras.scheduler.activity.MainActivity;

import java.util.ArrayList;
import java.util.Date;

import other.StringKeys;
import other.Task;

public class AlarmManagerBroadcastReceiver extends BroadcastReceiver {

    private static final String LOG_ON_RECEIVE = "BroadcastReceiver onReceive()";
    private static final String LOG_ON_SET_TIMER = "BroadcastReceiver setTimer()";
    private static final int NOTIFY_ID = 101;


    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences appSettings = context.getSharedPreferences(
                StringKeys.APP_SETTINGS, Context.MODE_PRIVATE);

        Bundle extras = intent.getExtras();
        ArrayList<Task> tasks = extras.getParcelableArrayList(StringKeys.ARRAY_OF_TASKS);
        int maxRuntime = extras.getInt(StringKeys.MAX_RUNTIME_FOR_TASK);

        //у випадку, якщо отримано пустий інтент(при перезавантаженні телефона)
        if (tasks == null) {
            maxRuntime = appSettings.getInt(StringKeys.MAX_RUNTIME_FOR_TASK, 60);
            tasks = new ArrayList<>();

            DatabaseConnector databaseConnector = new DatabaseConnector(context);
            databaseConnector.open();
            Cursor cursor = databaseConnector.getCursorWithAllTasks();
            if (cursor.moveToFirst()) {
                int idColIndex = cursor.getColumnIndex(DatabaseConnector.TABLE_ID);
                int titleColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_TITLE);
                int commentColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_COMMENT);
                int dateStartColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_DATA_START);
                int dateStopColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_DATA_STOP);
                int dateEndColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_DATA_END);
                do {
                    long id = cursor.getLong(idColIndex);
                    String title = cursor.getString(titleColIndex);
                    String comment = cursor.getString(commentColIndex);
                    long dateStart = cursor.getLong(dateStartColIndex);
                    long dateStop = cursor.getLong(dateStopColIndex);
                    long dateEnd = cursor.getLong(dateEndColIndex);
                    tasks.add(new Task(id, title, comment, dateStart, dateStop, dateEnd));
                } while (cursor.moveToNext());
            }
            cursor.close();
            databaseConnector.close();
        }

        Date currentDate = new Date();
        boolean ifWasChanged = false;
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);

            if (task.getDateEnd() == null && task.getDateStart() != null) {
                if (task.getDateStop() == null && (currentDate.getTime() - task.getDateStart().getTime()) >= maxRuntime * 60000) {
                    task.setDateEnd(currentDate);
                    DatabaseConnector.updateTask(task, context);
                    ifWasChanged = true;
                    showNotification(context, task);
                } else if (task.getDateStop() != null &&
                        (currentDate.getTime() - task.getDateStop().getTime()) >= maxRuntime * 60000) {
                    task.setDateEnd(currentDate);
                    DatabaseConnector.updateTask(task, context);
                    ifWasChanged = true;
                    showNotification(context, task);
                }
            }
        }
        if (ifWasChanged) {
            Intent i = new Intent("broadCastName");
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList(StringKeys.ARRAY_OF_TASKS, tasks);
            i.putExtras(bundle);
            context.sendBroadcast(i);
        }
        setTimer(context, maxRuntime, tasks);
    }


    public void setTimer(Context context, int maxRuntime, ArrayList<Task> tasks) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmManagerBroadcastReceiver.class);

        long minStartDate = new Date().getTime();

        boolean ifHaveNotCompletedTask = false;
        for (Task task : tasks) {
            if (task.getDateEnd() == null && task.getDateStart() != null &&
                    minStartDate >= task.getDateStart().getTime()) {
                ifHaveNotCompletedTask = true;
                if (task.getDateStop() != null && minStartDate >= task.getDateStop().getTime())
                    minStartDate = task.getDateStop().getTime();
                else
                    minStartDate = task.getDateStart().getTime();
            }
        }

        if (ifHaveNotCompletedTask) {
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList(StringKeys.ARRAY_OF_TASKS, tasks);
            intent.putExtras(bundle);
            intent.putExtra(StringKeys.MAX_RUNTIME_FOR_TASK, maxRuntime);

            PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            am.set(AlarmManager.RTC, (maxRuntime * 60000 + minStartDate), pi);
        } else {
            //якщо немає незавершених завдань відміняєм alarm,
            // який міг бути раніше запущений з уже неактуальними даними
            PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
            am.cancel(pi);
        }
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
                .setContentText(String.format(context.getString(R.string.notificationContentText), task.getTitle()));

        Notification notification = builder.build();
        //notification.defaults |= Notification.DEFAULT_SOUND;
        //notification.defaults |= Notification.DEFAULT_VIBRATE;
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(NOTIFY_ID, notification);
    }
}