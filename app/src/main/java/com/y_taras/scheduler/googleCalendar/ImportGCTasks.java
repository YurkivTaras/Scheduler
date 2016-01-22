package com.y_taras.scheduler.googleCalendar;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;

import com.google.android.gms.maps.model.LatLng;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.y_taras.scheduler.R;
import com.y_taras.scheduler.activity.MainActivity;
import com.y_taras.scheduler.helper.DatabaseConnector;
import com.y_taras.scheduler.other.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class ImportGCTasks extends AsyncTask<Void, Void, List<Task>> {
    private static final String TAG = "IMPORT_TASKS";
    private com.google.api.services.calendar.Calendar mService = null;
    private Exception mLastError = null;
    private List<Task> copyOfTasks;
    private int mMaxRuntime;
    private MainActivity mMainActivity;
    private Context mContext;
    private List<Task> mResult;

    public ImportGCTasks(GoogleAccountCredential credential, MainActivity activity, int maxRuntime) {
        mMainActivity = activity;
        mContext = activity.getApplicationContext();
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.calendar.Calendar.Builder(
                transport, jsonFactory, credential)
                .setApplicationName("Scheduler")
                .build();
        mMaxRuntime = maxRuntime;
        copyOfTasks = new ArrayList<>();
        for (Task task : mMainActivity.getTasks())
            copyOfTasks.add(new Task(task));

    }

    private LatLng getLocationFromAddress(Context context, String strAddress) {
        Geocoder coder = new Geocoder(context);
        List<Address> address;
        LatLng p1 = null;
        try {
            address = coder.getFromLocationName(strAddress, 1);
            if (address == null) return null;
            Address location = address.get(0);
            location.getLatitude();
            location.getLongitude();
            p1 = new LatLng(location.getLatitude(), location.getLongitude());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return p1;
    }

    @Override
    protected void onPreExecute() {
        mMainActivity.showProgressDialog(mMainActivity.getString(R.string.import_task_progress_massage));
    }

    @Override
    protected List<Task> doInBackground(Void... params) {
        try {
            return getDataFromApi();
        } catch (Exception e) {
            mLastError = e;
            cancel(true);
            return null;
        }
    }

    private List<Task> getDataFromApi() throws IOException {
        long currentTime = System.currentTimeMillis();
        List<Task> listTasks = new ArrayList<>();
        Events events = mService.events().list("primary")
                .setSingleEvents(true)
                .execute();
        List<Event> items = events.getItems();
        Task newTask;
        for (Event event : items) {
            String id = event.getId();
            if (taskAlreadyExists(id))
                continue;
            DateTime start = event.getStart().getDateTime();
            if (start == null)
                start = event.getStart().getDate();
            DateTime end = event.getEnd().getDateTime();
            if (end == null)
                end = event.getEnd().getDate();
            String title = (event.getSummary() == null ?
                    mContext.getString(R.string.no_title) : event.getSummary());
            String comment = (event.getDescription() == null ? "" : event.getDescription());
            newTask = new Task(title, comment, false, mMaxRuntime);
            newTask.setCalendar_ID(id);
            if (start.getValue() < currentTime) {
                newTask.setDateStart(new Date(start.getValue()));
                if (end.getValue() < currentTime)
                    newTask.setDateEnd(new Date(end.getValue()));
            }

            String location = event.getLocation();
            if (location != null) {
                LatLng latLng = getLocationFromAddress(mContext, location);
                if (latLng != null) {
                    newTask.setMapPoint(true);
                    newTask.setLatitude(latLng.latitude);
                    newTask.setLongitude(latLng.longitude);
                }
            }
            listTasks.add(newTask);
        }
        return listTasks;
    }

    private boolean taskAlreadyExists(String id) {
        for (Task task : copyOfTasks)
            if (id.equals(task.getCalendar_ID()))
                return true;
        return false;
    }

    @Override
    protected void onPostExecute(List<Task> tasks) {
        if (mMainActivity != null) {
            List<Task> originalTasks = mMainActivity.getTasks();
            mMainActivity.hideProgressDialog();
            if (tasks == null || tasks.size() == 0)
                mMainActivity.showToast(mContext.getString(R.string.no_task_find));
            else {
                for (int i = 0; i < tasks.size(); i++) {
                    DatabaseConnector.addTask(tasks.get(i), mContext);
                    originalTasks.add(tasks.get(i));
                }
                mMainActivity.sortTasks();
                mMainActivity.showToast(String.format(mContext.getString(R.string.gCalendar_success_import), tasks.size()));
            }
        } else if (tasks == null || tasks.size() == 0)
            mResult = tasks;
    }

    @Override
    protected void onCancelled() {
        if (mMainActivity != null) {
            mMainActivity.hideProgressDialog();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    mMainActivity.showGooglePlayServicesAvailabilityErrorDialog(((GooglePlayServicesAvailabilityIOException) mLastError)
                            .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    mMainActivity.startActivityForResult(((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else
                    mMainActivity.showToast("The following error occurred:\n" + mLastError);
            } else
                mMainActivity.showToast(mContext.getString(R.string.request_rejected));
        }
    }

    public List<Task> getResult() {
        return mResult;
    }

    public void link(MainActivity act) {
        mMainActivity = act;
    }

    public void unLink() {
        mMainActivity = null;
    }
}