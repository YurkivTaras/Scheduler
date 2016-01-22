package com.y_taras.scheduler.googleCalendar;

import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.y_taras.scheduler.R;
import com.y_taras.scheduler.activity.MainActivity;
import com.y_taras.scheduler.helper.DatabaseConnector;
import com.y_taras.scheduler.other.Task;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


public class ExportTask extends AsyncTask<Void, Void, String> {
    public static final int UPDATE = 1;
    public static final int INSERT = 2;
    private static final String TAG = "EXPORT_TASK";
    private com.google.api.services.calendar.Calendar mService = null;
    private Task mOriginalTask;
    private Task mCopyOfTask;
    private String mEmail;
    private int mAction;
    private MainActivity mMainActivity;

    public ExportTask(GoogleAccountCredential credential, MainActivity activity, Task task, int action) {
        mOriginalTask = task;
        mMainActivity = activity;
        mAction = action;
        mCopyOfTask = new Task(task);
        mEmail = credential.getSelectedAccount().name;
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.calendar.Calendar.Builder(
                transport, jsonFactory, credential)
                .setApplicationName("Scheduler")
                .build();
    }

    public String getLocationFromLatLng(double latitude, double longitude) {
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(mMainActivity, Locale.getDefault());
        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1);
        } catch (IOException e) {
            return null;
        }
        Log.d(TAG, "ADDRESS= " + addresses.get(0).getAddressLine(0));
        return addresses.get(0).getAddressLine(0);
    }

    @Override
    protected String doInBackground(Void... params) {
        Event event = new Event()
                .setSummary(mCopyOfTask.getTitle())
                .setDescription(mCopyOfTask.getComment());
        if (mCopyOfTask.hasMapPoint()) {
            String location = getLocationFromLatLng(mCopyOfTask.getLatitude(), mCopyOfTask.getLongitude());
            if (location != null) event.setLocation(location);
        }
        DateTime startDateTime = new DateTime(mCopyOfTask.getDateStart());
        EventDateTime start = new EventDateTime()
                .setDateTime(startDateTime);
        event.setStart(start);

        DateTime endDateTime = new DateTime(mCopyOfTask.getDateEnd() != null ? mCopyOfTask.getDateEnd().getTime() : (System.currentTimeMillis() + (1000 * 60 * 60)));
        EventDateTime end = new EventDateTime()
                .setDateTime(endDateTime);
        event.setEnd(end);

        EventAttendee[] attendees = new EventAttendee[]{
                new EventAttendee().setEmail(mEmail),
        };
        event.setAttendees(Arrays.asList(attendees));

        String calendarId = "primary";
        try {
            if (mAction == INSERT)
                event = mService.events().insert(calendarId, event).execute();
            else if (mAction == UPDATE) {
                event = mService.events().update(calendarId, mCopyOfTask.getCalendar_ID(), event).execute();
            }
        } catch (IOException e) {
            Log.d(TAG, "Export task exception" + e);
            return null;
        }
        return event.getId();
    }

    @Override
    protected void onPostExecute(String id) {
        if (id != null) {
            if (mAction == INSERT) {
                mOriginalTask.setCalendar_ID(id);
                DatabaseConnector.updateTask(mOriginalTask, mMainActivity.getApplicationContext());
                mMainActivity.sortTasks();
                mMainActivity.showToast(String.format(mMainActivity.getString(R.string.export_task_success), mCopyOfTask.getTitle()));
            } else if (mAction == UPDATE)
                mMainActivity.showToast(String.format(mMainActivity.getString(R.string.update_task_success), mCopyOfTask.getTitle()));
        }
    }
}