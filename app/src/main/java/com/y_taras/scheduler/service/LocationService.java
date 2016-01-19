package com.y_taras.scheduler.service;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.Date;

import com.y_taras.scheduler.other.Constants;
import com.y_taras.scheduler.other.Task;
import com.y_taras.scheduler.utils.AlarmManagerBroadcastReceiver;
import com.y_taras.scheduler.helper.DatabaseConnector;

public class LocationService extends Service {
    private static final int MaxDistance = 100;
    private LocationManager mLocationManager;

    public void onCreate() {
        super.onCreate();
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000 * 2, 1, locationListener);
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000 * 2, 1, locationListener);
    }

    private LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            checkLocation(location);
        }

        @Override
        public void onProviderEnabled(String provider) {
            checkLocation(mLocationManager.getLastKnownLocation(provider));
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    void checkLocation(final Location currentLoc) {
        if (currentLoc == null)
            return;
        final DatabaseConnector databaseConnector = new DatabaseConnector(this);
        databaseConnector.open();
        new AsyncTask<Void, Void, ArrayList<Task>>() {
            @Override
            protected ArrayList<Task> doInBackground(Void... params) {
                ArrayList<Task> tasks = new ArrayList<>();
                Cursor cursor = databaseConnector.getCursorWithMapPointTasks();
                if (cursor.moveToFirst()) {
                    int idColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_ID);
                    int typeOfTaskColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_TYPE_OF_TASK);
                    int dateStartColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_DATA_START);
                    int datePauseColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_DATA_PAUSE);
                    int dateStopColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_DATA_STOP);
                    int dateEndColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_DATA_END);
                    int pauseAfterStopColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_PAUSE_LENGTH_AFTER_STOP);
                    int pauseBeforeStopColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_PAUSE_LENGTH_BEFORE_STOP);
                    int latitudeColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_LATITUDE);
                    int longitudeColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_LONGITUDE);
                    do {
                        long id = cursor.getLong(idColIndex);
                        boolean isPeriodic = cursor.getInt(typeOfTaskColIndex) == 1;
                        long dateStart = cursor.getLong(dateStartColIndex);
                        long datePause = cursor.getLong(datePauseColIndex);
                        long dateStop = cursor.getLong(dateStopColIndex);
                        long dateEnd = cursor.getLong(dateEndColIndex);
                        long pauseLengthAfterStop = cursor.getLong(pauseAfterStopColIndex);
                        long pauseLengthBeforeStop = cursor.getLong(pauseBeforeStopColIndex);
                        double latitude = cursor.getDouble(latitudeColIndex);
                        double longitude = cursor.getDouble(longitudeColIndex);
                        Task task = new Task(id);
                        task.setPeriodic(isPeriodic);
                        task.setDateStart(dateStart != -1 ? new Date(dateStart) : null);
                        task.setDatePause(datePause != -1 ? new Date(datePause) : null);
                        task.setDateStop(dateStop != -1 ? new Date(dateStop) : null);
                        task.setDateEnd(dateEnd != -1 ? new Date(dateEnd) : null);
                        task.setPauseLengthAfterStop(pauseLengthAfterStop);
                        task.setPauseLengthBeforeStop(pauseLengthBeforeStop);
                        task.setLatitude(latitude);
                        task.setLongitude(longitude);
                        tasks.add(task);
                    } while (cursor.moveToNext());
                }
                cursor.close();
                ArrayList<Task> result = new ArrayList<>();
                for (int i = 0; i < tasks.size(); i++) {
                    Task task = tasks.get(i);
                    Location taskLoc = new Location("");
                    taskLoc.setLatitude(task.getLatitude());
                    taskLoc.setLongitude(task.getLongitude());
                    float distance = currentLoc.distanceTo(taskLoc);
                    if (task.getDateEnd() != null) {            //якщо завдання періодичне і уже завершене
                        if (task.isPeriodic() && distance < MaxDistance) {
                            task.setDateStart(new Date());
                            task.setDateStop(null);
                            task.setDateEnd(null);
                            task.setDatePause(null);
                            task.setPauseLengthAfterStop(0);
                            task.setPauseLengthBeforeStop(0);
                            databaseConnector.saveServiceUpdate(task);
                            result.add(task);
                        }
                    } else if (task.getDateStart() != null) {   //якщо завдання розпочате
                        if (task.getDatePause() == null) {      //якщо завдання виконується
                            if (distance > MaxDistance) {
                                task.setDatePause(new Date());
                                databaseConnector.saveServiceUpdate(task);
                                result.add(task);
                            }
                        } else if (distance < MaxDistance) {    //якщо завдання поставлено на паузу
                            Date currentDate = new Date();
                            task.setPauseLengthAfterStop(task.getPauseLengthAfterStop() +
                                    (currentDate.getTime() - task.getDatePause().getTime()));
                            task.setDatePause(null);
                            databaseConnector.saveServiceUpdate(task);
                            result.add(task);
                        }
                    } else if (distance < MaxDistance) {        //якщо завдання нерозпочате
                        task.setDateStart(new Date());
                        task.setDateStop(null);
                        task.setDateEnd(null);
                        task.setDatePause(null);
                        task.setPauseLengthAfterStop(0);
                        task.setPauseLengthBeforeStop(0);
                        databaseConnector.saveServiceUpdate(task);
                        result.add(task);
                    }
                }
                return result;
            }

            @Override
            protected void onPostExecute(ArrayList<Task> tasks) {
                databaseConnector.close();
                if (tasks.size() != 0) {
                    Intent i = new Intent(getApplicationContext(), AlarmManagerBroadcastReceiver.class);
                    sendBroadcast(i);
                    i = new Intent(Constants.MAIN_ACTIVITY_BROADCAST);
                    Bundle bundle = new Bundle();
                    bundle.putParcelableArrayList(Constants.ARRAY_OF_TASKS, tasks);
                    bundle.putString(Constants.ACTION, Constants.START_OR_PAUSE_TASK_ACTION);
                    i.putExtras(bundle);
                    sendBroadcast(i);
                }
            }
        }.execute();
    }
}
