package utils;


import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;

import com.y_taras.scheduler.activity.MainActivity;

import java.util.ArrayList;
import java.util.Date;

import other.StringKeys;
import other.Task;

public class DatabaseConnector {
    private static final String DATABASE_NAME = "UserTasks";
    public static final String COLUMN_ID = "_id";
    public static final String TABLE_NAME = "TASKS";
    public static final String COLUMN_TITLE = "TITLE";
    public static final String COLUMN_COMMENT = "COMMENT";
    public static final String COLUMN_AVATAR_URI = "AVATAR_URI";
    public static final String COLUMN_DATA_START = "DATE_START";
    public static final String COLUMN_DATA_STOP = "DATE_STOP";
    public static final String COLUMN_DATA_END = "DATE_END";

    public static final String COLUMN_DATA_PAUSE = "DATE_PAUSE";
    public static final String COLUMN_MAX_RUNTIME = "MAX_RUNTIME";
    public static final String COLUMN_PAUSE_LENGTH_BEFORE_STOP = "PAUSE_LENGTH_BEFORE_STOP";
    public static final String COLUMN_PAUSE_LENGTH_AFTER_STOP = "PAUSE_LENGTH_AFTER_STOP";

    private static final int DATABASE_VERSION = 2;

    private SQLiteDatabase database;
    private DatabaseOpenHelper databaseOpenHelper;

    public DatabaseConnector(Context context) {
        databaseOpenHelper = new DatabaseOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void open() throws SQLException {
        database = databaseOpenHelper.getWritableDatabase();
    }

    public void close() {
        if (database != null)
            database.close();
    }

    public Cursor getCursorWithAllTasks() {
        return database.query(TABLE_NAME, null, null, null, null, null, null);
    }

    public static void updateTask(final Task task, Context context) {
        final ContentValues editTask = new ContentValues();
        editTask.put(COLUMN_TITLE, task.getTitle());
        editTask.put(COLUMN_COMMENT, task.getComment());

        Date dateStart = task.getDateStart();
        editTask.put(COLUMN_DATA_START, dateStart == null ? -1 : dateStart.getTime());

        Date dateStop = task.getDateStop();
        editTask.put(COLUMN_DATA_STOP, dateStop == null ? -1 : dateStop.getTime());

        Date dateEnd = task.getDateEnd();
        editTask.put(COLUMN_DATA_END, dateEnd == null ? -1 : dateEnd.getTime());

        Date datePause = task.getDatePause();
        editTask.put(COLUMN_DATA_PAUSE, datePause == null ? -1 : datePause.getTime());

        editTask.put(COLUMN_MAX_RUNTIME, task.getMaxRuntime());
        editTask.put(COLUMN_PAUSE_LENGTH_BEFORE_STOP, task.getPauseLengthBeforeStop());
        editTask.put(COLUMN_PAUSE_LENGTH_AFTER_STOP, task.getPauseLengthAfterStop());
        editTask.put(COLUMN_AVATAR_URI, task.getAvatarUri());
        final DatabaseConnector databaseConnector = new DatabaseConnector(context);
        databaseConnector.open();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                databaseConnector.database.update(TABLE_NAME, editTask, COLUMN_ID + "=" + task.getDatabase_ID(), null);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                databaseConnector.close();
            }
        }.execute();
    }


    public static void deleteTask(final long id, Context context) {
        final DatabaseConnector databaseConnector = new DatabaseConnector(context);
        databaseConnector.open();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                databaseConnector.database.delete(TABLE_NAME, COLUMN_ID + "=" + id, null);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                databaseConnector.close();
            }
        }.execute();
    }

    public static void deleteAllTasks(Context context) {
        final DatabaseConnector databaseConnector = new DatabaseConnector(context);
        databaseConnector.open();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                databaseConnector.database.delete(TABLE_NAME, null, null);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                databaseConnector.close();
            }
        }.execute();
    }

    public static void downloadTasks(final ArrayList<Task> arrayForTasks, final Context context) {
        final DatabaseConnector databaseConnector = new DatabaseConnector(context);
        databaseConnector.open();
        new AsyncTask<Void, Void, Cursor>() {
            @Override
            protected Cursor doInBackground(Void... params) {
                return databaseConnector.getCursorWithAllTasks();
            }

            @Override
            protected void onPostExecute(Cursor cursor) {
                if (cursor.moveToFirst()) {
                    int idColIndex = cursor.getColumnIndex(COLUMN_ID);
                    int titleColIndex = cursor.getColumnIndex(COLUMN_TITLE);
                    int commentColIndex = cursor.getColumnIndex(COLUMN_COMMENT);
                    int dateStartColIndex = cursor.getColumnIndex(COLUMN_DATA_START);
                    int dateStopColIndex = cursor.getColumnIndex(COLUMN_DATA_STOP);
                    int dateEndColIndex = cursor.getColumnIndex(COLUMN_DATA_END);
                    int datePauseColIndex = cursor.getColumnIndex(COLUMN_DATA_PAUSE);
                    int maxRuntimeColIndex = cursor.getColumnIndex(COLUMN_MAX_RUNTIME);
                    int pauseLengthBeforeStopColIndex = cursor.getColumnIndex(COLUMN_PAUSE_LENGTH_BEFORE_STOP);
                    int pauseLengthAfterStopColIndex = cursor.getColumnIndex(COLUMN_PAUSE_LENGTH_AFTER_STOP);
                    int avatarUriColIndex = cursor.getColumnIndex(COLUMN_AVATAR_URI);
                    do {
                        long id = cursor.getLong(idColIndex);
                        String title = cursor.getString(titleColIndex);
                        String comment = cursor.getString(commentColIndex);
                        long dateStart = cursor.getLong(dateStartColIndex);
                        long dateStop = cursor.getLong(dateStopColIndex);
                        long dateEnd = cursor.getLong(dateEndColIndex);
                        long datePause = cursor.getLong(datePauseColIndex);
                        int maxRuntime = cursor.getInt(maxRuntimeColIndex);
                        long pauseLengthBeforeStop = cursor.getLong(pauseLengthBeforeStopColIndex);
                        long pauseLengthAfterStop = cursor.getLong(pauseLengthAfterStopColIndex);
                        String avatarUri = cursor.getString(avatarUriColIndex);
                        arrayForTasks.add(new Task(id, title, comment, avatarUri, maxRuntime,
                                dateStart, dateStop, dateEnd, datePause, pauseLengthBeforeStop, pauseLengthAfterStop));
                    } while (cursor.moveToNext());
                }
                cursor.close();
                databaseConnector.close();
                ((MainActivity) (context)).sortTasks();
                ((MainActivity) (context)).setTimer();
            }
        }.execute();
    }

    // добавлення в базу нового завдання
    public static void addTask(final Task task, Context context) {
        final Task copyOfTask = new Task(task);
        final DatabaseConnector databaseConnector = new DatabaseConnector(context);
        databaseConnector.open();
        new AsyncTask<Void, Void, Long>() {
            @Override
            protected Long doInBackground(Void... params) {
                ContentValues newTask = new ContentValues();
                newTask.put(COLUMN_TITLE, copyOfTask.getTitle());
                newTask.put(COLUMN_COMMENT, copyOfTask.getComment());

                Date dateStart = copyOfTask.getDateStart();
                newTask.put(COLUMN_DATA_START, dateStart == null ? -1 : dateStart.getTime());

                Date dateStop = copyOfTask.getDateStop();
                newTask.put(COLUMN_DATA_STOP, dateStop == null ? -1 : dateStop.getTime());

                Date dateEnd = copyOfTask.getDateEnd();
                newTask.put(COLUMN_DATA_END, dateEnd == null ? -1 : dateEnd.getTime());

                Date datePause = copyOfTask.getDatePause();
                newTask.put(COLUMN_DATA_PAUSE, datePause == null ? -1 : datePause.getTime());

                newTask.put(COLUMN_MAX_RUNTIME, copyOfTask.getMaxRuntime());
                newTask.put(COLUMN_PAUSE_LENGTH_BEFORE_STOP, copyOfTask.getPauseLengthBeforeStop());
                newTask.put(COLUMN_PAUSE_LENGTH_AFTER_STOP, copyOfTask.getPauseLengthAfterStop());
                newTask.put(COLUMN_AVATAR_URI, copyOfTask.getAvatarUri());
                return databaseConnector.database.insert(TABLE_NAME, null, newTask);
            }

            @Override
            protected void onPostExecute(Long id) {
                databaseConnector.close();
                task.setDatabase_ID(id);
            }
        }.execute();
    }

    // добавлення в базу одразу декілька нових завдань
    public static void addAllTasks(final ArrayList<Task> tasks, Context context) {
        final ArrayList<Task> copyOfTasks = new ArrayList<>();
        for (int i = 0; i < tasks.size(); i++)
            copyOfTasks.add(new Task(tasks.get(i)));
        final DatabaseConnector databaseConnector = new DatabaseConnector(context);
        databaseConnector.open();
        new AsyncTask<Void, Void, ArrayList<Long>>() {
            @Override
            protected ArrayList<Long> doInBackground(Void... params) {
                ArrayList<Long> id_for_tasks = new ArrayList<>();
                ContentValues newTask = new ContentValues();
                for (int i = 0; i < copyOfTasks.size(); i++) {
                    newTask.clear();
                    Task copyOfTask = copyOfTasks.get(i);
                    newTask.put(COLUMN_TITLE, copyOfTask.getTitle());
                    newTask.put(COLUMN_COMMENT, copyOfTask.getComment());

                    Date dateStart = copyOfTask.getDateStart();
                    newTask.put(COLUMN_DATA_START, dateStart == null ? -1 : dateStart.getTime());

                    Date dateStop = copyOfTask.getDateStop();
                    newTask.put(COLUMN_DATA_STOP, dateStop == null ? -1 : dateStop.getTime());

                    Date dateEnd = copyOfTask.getDateEnd();
                    newTask.put(COLUMN_DATA_END, dateEnd == null ? -1 : dateEnd.getTime());

                    Date datePause = copyOfTask.getDatePause();
                    newTask.put(COLUMN_DATA_PAUSE, datePause == null ? -1 : datePause.getTime());

                    newTask.put(COLUMN_MAX_RUNTIME, copyOfTask.getMaxRuntime());
                    newTask.put(COLUMN_PAUSE_LENGTH_BEFORE_STOP, copyOfTask.getPauseLengthBeforeStop());
                    newTask.put(COLUMN_PAUSE_LENGTH_AFTER_STOP, copyOfTask.getPauseLengthAfterStop());

                    newTask.put(COLUMN_AVATAR_URI, copyOfTask.getAvatarUri());
                    id_for_tasks.add(databaseConnector.database.insert(TABLE_NAME, null, newTask));
                }
                return id_for_tasks;
            }

            @Override
            protected void onPostExecute(ArrayList<Long> id) {
                databaseConnector.close();
                for (int i = 0; i < tasks.size(); i++)
                    tasks.get(i).setDatabase_ID(id.get(i));
            }
        }.execute();
    }

    private class DatabaseOpenHelper extends SQLiteOpenHelper {
        private Context mContext;

        public DatabaseOpenHelper(Context context, String name,
                                  SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
            mContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String createQuery = "CREATE TABLE " + TABLE_NAME +
                    "(" + COLUMN_ID + " integer primary key autoincrement," +
                    COLUMN_TITLE + " TEXT, " +
                    COLUMN_COMMENT + " TEXT, " +
                    COLUMN_DATA_START + " LONG, " +
                    COLUMN_DATA_STOP + " LONG, " +
                    COLUMN_DATA_END + " LONG, " +
                    COLUMN_DATA_PAUSE + " LONG, " +
                    COLUMN_AVATAR_URI + " TEXT, " +
                    COLUMN_MAX_RUNTIME + " INTEGER, " +
                    COLUMN_PAUSE_LENGTH_BEFORE_STOP + " LONG, " +
                    COLUMN_PAUSE_LENGTH_AFTER_STOP + " LONG);";
            db.execSQL(createQuery);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion,
                              int newVersion) {
            if (oldVersion == 1 && newVersion == 2) {
                SharedPreferences appSettings = mContext.getSharedPreferences(StringKeys.APP_SETTINGS, Context.MODE_PRIVATE);
                int maxRuntime = appSettings.getInt(StringKeys.MAX_RUNTIME_FOR_TASK, 60);
                ContentValues cv = new ContentValues();
                db.execSQL("alter table " + TABLE_NAME + " add column " + COLUMN_DATA_PAUSE + " LONG;");
                db.execSQL("alter table " + TABLE_NAME + " add column " + COLUMN_MAX_RUNTIME + " INTEGER;");
                db.execSQL("alter table " + TABLE_NAME + " add column " + COLUMN_PAUSE_LENGTH_BEFORE_STOP + " LONG;");
                db.execSQL("alter table " + TABLE_NAME + " add column " + COLUMN_PAUSE_LENGTH_AFTER_STOP + " LONG;");
                db.execSQL("alter table " + TABLE_NAME + " add column " + COLUMN_AVATAR_URI + " TEXT;");
                cv.put(COLUMN_DATA_PAUSE, -1);
                cv.put(COLUMN_MAX_RUNTIME, maxRuntime);
                cv.put(COLUMN_PAUSE_LENGTH_BEFORE_STOP, 0);
                cv.put(COLUMN_PAUSE_LENGTH_AFTER_STOP, 0);
                cv.put(COLUMN_AVATAR_URI, Task.DEFAULT_AVATAR_URI);
                db.update(TABLE_NAME, cv, null, null);
            }
        }
    }
}