package com.y_taras.scheduler.utils;


import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.y_taras.scheduler.R;
import com.y_taras.scheduler.activity.MainActivity;

import java.util.ArrayList;
import java.util.Date;

import com.y_taras.scheduler.helper.DatabaseConnector;
import com.y_taras.scheduler.other.Constants;
import com.y_taras.scheduler.other.Task;
import com.y_taras.scheduler.service.LocationService;

public class AlarmManagerBroadcastReceiver extends BroadcastReceiver {

    private static final int NOTIFY_ID = 101;

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        ArrayList<Task> tasks = null;
        if (extras != null)
            tasks = extras.getParcelableArrayList(Constants.ARRAY_OF_TASKS);
        //у випадку, якщо отримано пустий інтент(при перезавантаженні телефона)
        if (tasks == null) {
            //запускаєм сервіс при включенні телефона
            context.startService(new Intent(context, LocationService.class));
            tasks = new ArrayList<>();
            DatabaseConnector databaseConnector = new DatabaseConnector(context);
            databaseConnector.open();
            Cursor cursor = databaseConnector.getCursorWithAllTasks();
            if (cursor.moveToFirst()) {
                int idColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_ID);
                int calendarIdColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_CALENDAR_ID);
                int titleColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_TITLE);
                int commentColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_COMMENT);
                int typeOfTaskColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_TYPE_OF_TASK);
                int dateStartColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_DATA_START);
                int dateStopColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_DATA_STOP);
                int dateEndColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_DATA_END);
                int datePauseColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_DATA_PAUSE);
                int maxRuntimeColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_MAX_RUNTIME);
                int pauseLengthBeforeStopColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_PAUSE_LENGTH_BEFORE_STOP);
                int pauseLengthAfterStopColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_PAUSE_LENGTH_AFTER_STOP);
                int avatarUriColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_AVATAR_URI);
                int hasMapPointColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_HAS_MAP_POINT);
                int latitudeColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_LATITUDE);
                int longitudeColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_LONGITUDE);
                do {
                    long id = cursor.getLong(idColIndex);
                    String calendar_id = cursor.getString(calendarIdColIndex);
                    String title = cursor.getString(titleColIndex);
                    String comment = cursor.getString(commentColIndex);
                    boolean typeOfTask = cursor.getInt(typeOfTaskColIndex) == 1;
                    long dateStart = cursor.getLong(dateStartColIndex);
                    long dateStop = cursor.getLong(dateStopColIndex);
                    long dateEnd = cursor.getLong(dateEndColIndex);
                    long datePause = cursor.getLong(datePauseColIndex);
                    int maxRuntime = cursor.getInt(maxRuntimeColIndex);
                    long pauseLengthBeforeStop = cursor.getLong(pauseLengthBeforeStopColIndex);
                    long pauseLengthAfterStop = cursor.getLong(pauseLengthAfterStopColIndex);
                    String avatarUri = cursor.getString(avatarUriColIndex);
                    boolean hasMapPoint = cursor.getInt(hasMapPointColIndex) == 1;
                    double latitude = cursor.getDouble(latitudeColIndex);
                    double longitude = cursor.getDouble(longitudeColIndex);
                    tasks.add(new Task(id, calendar_id, title, comment, typeOfTask, avatarUri, maxRuntime,
                            dateStart, dateStop, dateEnd, datePause, pauseLengthBeforeStop, pauseLengthAfterStop, hasMapPoint, latitude, longitude));
                } while (cursor.moveToNext());
            }
            cursor.close();
            databaseConnector.close();
        }
        boolean ifWasChanged = false;
        Date currentDate = new Date();
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            if (task.getDateEnd() == null && task.getDateStart() != null && task.getDatePause() == null) {
                int maxRuntime = task.getMaxRuntime() * 60000;
                long runtime;
                if (task.getDateStop() == null)
                    runtime = currentDate.getTime() - task.getDateStart().getTime() - task.getPauseLengthAfterStop();
                else
                    runtime = currentDate.getTime() - task.getDateStop().getTime() - task.getPauseLengthAfterStop();
                if (maxRuntime <= runtime) {
                    task.setDateEnd(currentDate);
                    DatabaseConnector.updateTask(task, context);
                    //якщо завдання періодичне - добавляєм до таблці статистики нові дані
                    if (task.isPeriodic())
                        DatabaseConnector.addStatistic(task.getDatabase_ID(),
                                task.getDateStart().getTime(), task.getDateEnd().getTime(),
                                task.getPauseLengthAfterStop() + task.getPauseLengthBeforeStop(),
                                context);
                    ifWasChanged = true;
                    showNotification(context, task);
                }
            }
        }

        if (ifWasChanged) {
            Intent i = new Intent(Constants.MAIN_ACTIVITY_BROADCAST);

            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList(Constants.ARRAY_OF_TASKS, tasks);
            bundle.putString(Constants.ACTION, Constants.ClOSE_TASK_ACTION);
            i.putExtras(bundle);
            context.sendBroadcast(i);
        }
        setTimer(context, tasks);
    }


    public void setTimer(Context context, ArrayList<Task> tasks) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmManagerBroadcastReceiver.class);

        long triggerForAlarm = Long.MAX_VALUE;
        long currentTime = new Date().getTime();
        boolean ifHaveNotCompletedTask = false;
        for (Task task : tasks) {
            if (task.getDateEnd() == null && task.getDateStart() != null && task.getDatePause() == null) {
                int maxRuntime = task.getMaxRuntime() * 60000;
                long runtime;
                if (task.getDateStop() == null)
                    runtime = currentTime - task.getDateStart().getTime() - task.getPauseLengthAfterStop();
                else
                    runtime = currentTime - task.getDateStop().getTime() - task.getPauseLengthAfterStop();
                if (maxRuntime - runtime < triggerForAlarm) {
                    triggerForAlarm = maxRuntime - runtime;
                    ifHaveNotCompletedTask = true;
                }
            }
        }
        if (ifHaveNotCompletedTask) {
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList(Constants.ARRAY_OF_TASKS, tasks);
            intent.putExtras(bundle);

            PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            am.set(AlarmManager.RTC, currentTime + triggerForAlarm, pi);
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