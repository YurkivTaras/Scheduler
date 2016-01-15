package com.y_taras.scheduler.utils;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.Date;

import com.y_taras.scheduler.other.Task;

public class DatabaseConnector {
    public static final String DATABASE_NAME = "UserTasks";
    public static final String TABLE_TASKS = "TASKS";
    public static final String TABLE_STATISTIC = "STATISTIC";

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TITLE = "TITLE";
    public static final String COLUMN_COMMENT = "COMMENT";
    public static final String COLUMN_TYPE_OF_TASK = "TYPE_OF_TASK";
    public static final String COLUMN_AVATAR_URI = "AVATAR_URI";
    public static final String COLUMN_DATA_START = "DATE_START";
    public static final String COLUMN_DATA_STOP = "DATE_STOP";
    public static final String COLUMN_DATA_END = "DATE_END";
    public static final String COLUMN_DATA_PAUSE = "DATE_PAUSE";
    public static final String COLUMN_MAX_RUNTIME = "MAX_RUNTIME";
    public static final String COLUMN_PAUSE_LENGTH_BEFORE_STOP = "PAUSE_LENGTH_BEFORE_STOP";
    public static final String COLUMN_PAUSE_LENGTH_AFTER_STOP = "PAUSE_LENGTH_AFTER_STOP";

    public static final String COLUMN_TASK_ID = "TASK_ID";
    public static final String COLUMN_SUM_PAUSE_LENGTH = "SUM_PAUSE_LENGTH";

    public static final String COLUMN_HAS_MAP_POINT = "HAS_MAP_POINT";
    public static final String COLUMN_LATITUDE = "LATITUDE";
    public static final String COLUMN_LONGITUDE = "LONGITUDE";


    private static final int DATABASE_VERSION = 4;

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
        return database.query(TABLE_TASKS, null, null, null, null, null, null);
    }

    public Cursor getCursorWithAllStatistics() {
        /*String sqlQuery = "select " +
                COLUMN_TASK_ID + ", " + COLUMN_TITLE + ", " + COLUMN_COMMENT + ", " + COLUMN_AVATAR_URI +
                ", SC." + COLUMN_DATA_START + ", SC." + COLUMN_DATA_END + ", " + COLUMN_SUM_PAUSE_LENGTH
                + " from " + TABLE_STATISTIC + " as SC "
                + "inner join " + TABLE_TASKS + " as TK "
                + "on SC." + COLUMN_TASK_ID + " = TK." + COLUMN_ID;*/
        return database.query(TABLE_STATISTIC, null, null, null, null, null, null);
    }

    public Cursor getCursorWithPeriodicalTasks() {
        String selection = COLUMN_TYPE_OF_TASK + " = ?";
        String[] selectionArgs = new String[]{"1"};
        String[] columns = new String[]{COLUMN_ID, COLUMN_TITLE, COLUMN_COMMENT, COLUMN_AVATAR_URI};
        return database.query(TABLE_TASKS, columns, selection, selectionArgs, null, null, null);
    }

    public Cursor getCursorWithMapPointTasks() {
        String selection = COLUMN_HAS_MAP_POINT + " = ?";
        String[] selectionArgs = new String[]{"1"};
        String[] columns = new String[]{COLUMN_ID, COLUMN_TYPE_OF_TASK, COLUMN_DATA_START, COLUMN_DATA_PAUSE, COLUMN_DATA_STOP,
                COLUMN_DATA_END, COLUMN_PAUSE_LENGTH_AFTER_STOP, COLUMN_PAUSE_LENGTH_BEFORE_STOP,
                COLUMN_LATITUDE, COLUMN_LONGITUDE};
        return database.query(TABLE_TASKS, columns, selection, selectionArgs, null, null, null);
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

        editTask.put(COLUMN_HAS_MAP_POINT, task.hasMapPoint() ? 1 : 0);
        editTask.put(COLUMN_LATITUDE, task.getLatitude());
        editTask.put(COLUMN_LONGITUDE, task.getLongitude());
        final DatabaseConnector databaseConnector = new DatabaseConnector(context);
        databaseConnector.open();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                databaseConnector.database.update(TABLE_TASKS, editTask, COLUMN_ID + "=" + task.getDatabase_ID(), null);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                databaseConnector.close();
            }
        }.execute();
    }

    public void saveServiceUpdate(Task task) {
        final ContentValues editTask = new ContentValues();
        Date dateStart = task.getDateStart();
        editTask.put(COLUMN_DATA_START, dateStart == null ? -1 : dateStart.getTime());
        Date datePause = task.getDatePause();
        editTask.put(COLUMN_DATA_PAUSE, datePause == null ? -1 : datePause.getTime());
        Date dateStop = task.getDateStop();
        editTask.put(COLUMN_DATA_STOP, dateStop == null ? -1 : dateStop.getTime());
        Date dateEnd = task.getDateEnd();
        editTask.put(COLUMN_DATA_END, dateEnd == null ? -1 : dateEnd.getTime());
        editTask.put(COLUMN_PAUSE_LENGTH_AFTER_STOP, task.getPauseLengthAfterStop());
        editTask.put(COLUMN_PAUSE_LENGTH_BEFORE_STOP, task.getPauseLengthBeforeStop());
        editTask.put(COLUMN_LATITUDE, task.getLatitude());
        editTask.put(COLUMN_LONGITUDE, task.getLongitude());
        database.update(TABLE_TASKS, editTask, COLUMN_ID + "=" + task.getDatabase_ID(), null);
    }


    public static void deleteTask(final long id, Context context) {
        final DatabaseConnector databaseConnector = new DatabaseConnector(context);
        databaseConnector.open();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                databaseConnector.database.delete(TABLE_TASKS, COLUMN_ID + "=" + id, null);
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
                databaseConnector.database.delete(TABLE_TASKS, null, null);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                databaseConnector.close();
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
                newTask.put(COLUMN_TYPE_OF_TASK, copyOfTask.isPeriodic() ? 1 : 0);

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

                newTask.put(COLUMN_HAS_MAP_POINT, copyOfTask.hasMapPoint() ? 1 : 0);
                newTask.put(COLUMN_LATITUDE, copyOfTask.getLatitude());
                newTask.put(COLUMN_LONGITUDE, copyOfTask.getLongitude());
                return databaseConnector.database.insert(TABLE_TASKS, null, newTask);
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
                    newTask.put(COLUMN_TYPE_OF_TASK, copyOfTask.isPeriodic() ? 1 : 0);

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

                    newTask.put(COLUMN_HAS_MAP_POINT, copyOfTask.hasMapPoint() ? 1 : 0);
                    newTask.put(COLUMN_LATITUDE, copyOfTask.getLatitude());
                    newTask.put(COLUMN_LONGITUDE, copyOfTask.getLongitude());
                    id_for_tasks.add(databaseConnector.database.insert(TABLE_TASKS, null, newTask));
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

    // добавлення в базу нових даних для статистики
    public static void addStatistic(final long task_id, final long dateStart,
                                    final long dateEnd, final long pauseLength, Context context) {
        final DatabaseConnector databaseConnector = new DatabaseConnector(context);
        databaseConnector.open();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                ContentValues newStatistic = new ContentValues();
                newStatistic.put(COLUMN_TASK_ID, task_id);
                newStatistic.put(COLUMN_DATA_START, dateStart);
                newStatistic.put(COLUMN_DATA_END, dateEnd);
                newStatistic.put(COLUMN_SUM_PAUSE_LENGTH, pauseLength);
                databaseConnector.database.insert(TABLE_STATISTIC, null, newStatistic);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                databaseConnector.close();
            }
        }.execute();
    }

    public static void deleteStatistic(final long task_id, Context context) {
        final DatabaseConnector databaseConnector = new DatabaseConnector(context);
        databaseConnector.open();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                databaseConnector.database.delete(TABLE_STATISTIC, COLUMN_TASK_ID + "=" + task_id, null);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                databaseConnector.close();
            }
        }.execute();
    }

    public static void deleteLastStatistic(final long task_id, Context context) {
        final DatabaseConnector databaseConnector = new DatabaseConnector(context);
        databaseConnector.open();

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                String selection = COLUMN_TASK_ID + " = ?";
                String[] selectionArgs = new String[]{Long.toString(task_id)};
                Cursor cursor = databaseConnector.database.query(TABLE_STATISTIC, new String[]{COLUMN_ID}, selection, selectionArgs, null, null, null);
                long max_id = -1;
                if (cursor.moveToFirst()) {
                    int idColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_ID);
                    do {
                        long id = cursor.getLong(idColIndex);
                        if (id > max_id)
                            max_id = id;
                    } while (cursor.moveToNext());
                }
                cursor.close();
                if (max_id != -1)
                    databaseConnector.database.delete(TABLE_STATISTIC, COLUMN_ID + "=" + max_id, null);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                databaseConnector.close();
            }
        }.execute();
    }

    public static void deleteAllStatistics(Context context) {
        final DatabaseConnector databaseConnector = new DatabaseConnector(context);
        databaseConnector.open();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                databaseConnector.database.delete(TABLE_STATISTIC, null, null);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                databaseConnector.close();
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
            String createQuery = "CREATE TABLE " + TABLE_TASKS +
                    "(" + COLUMN_ID + " integer primary key autoincrement," +
                    COLUMN_TITLE + " TEXT, " +
                    COLUMN_COMMENT + " TEXT, " +
                    COLUMN_TYPE_OF_TASK + " INTEGER, " +
                    COLUMN_DATA_START + " LONG, " +
                    COLUMN_DATA_STOP + " LONG, " +
                    COLUMN_DATA_END + " LONG, " +
                    COLUMN_DATA_PAUSE + " LONG, " +
                    COLUMN_AVATAR_URI + " TEXT, " +
                    COLUMN_MAX_RUNTIME + " INTEGER, " +
                    COLUMN_PAUSE_LENGTH_BEFORE_STOP + " LONG, " +
                    COLUMN_PAUSE_LENGTH_AFTER_STOP + " LONG, " +
                    COLUMN_HAS_MAP_POINT + " INTEGER, " +
                    COLUMN_LATITUDE + " REAL, " +
                    COLUMN_LONGITUDE + " REAL);";
            db.execSQL(createQuery);

            createQuery = "CREATE TABLE " + TABLE_STATISTIC +
                    "(" + COLUMN_ID + " INTEGER primary key autoincrement," +
                    COLUMN_TASK_ID + " INTEGER, " +//LONG
                    COLUMN_DATA_START + " LONG, " +
                    COLUMN_DATA_END + " LONG, " +
                    COLUMN_SUM_PAUSE_LENGTH + " LONG);";
            db.execSQL(createQuery);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion,
                              int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_STATISTIC);
            onCreate(db);
           /* if (oldVersion == 1 && newVersion == 2) {
                SharedPreferences appSettings = mContext.getSharedPreferences(StringKeys.APP_SETTINGS, Context.MODE_PRIVATE);
                int maxRuntime = appSettings.getInt(StringKeys.MAX_RUNTIME_FOR_TASK, 60);
                ContentValues cv = new ContentValues();
                db.execSQL("alter table " + TABLE_TASKS + " add column " + COLUMN_DATA_PAUSE + " LONG;");
                db.execSQL("alter table " + TABLE_TASKS + " add column " + COLUMN_MAX_RUNTIME + " INTEGER;");
                db.execSQL("alter table " + TABLE_TASKS + " add column " + COLUMN_PAUSE_LENGTH_BEFORE_STOP + " LONG;");
                db.execSQL("alter table " + TABLE_TASKS + " add column " + COLUMN_PAUSE_LENGTH_AFTER_STOP + " LONG;");
                db.execSQL("alter table " + TABLE_TASKS + " add column " + COLUMN_AVATAR_URI + " TEXT;");
                cv.put(COLUMN_DATA_PAUSE, -1);
                cv.put(COLUMN_MAX_RUNTIME, maxRuntime);
                cv.put(COLUMN_PAUSE_LENGTH_BEFORE_STOP, 0);
                cv.put(COLUMN_PAUSE_LENGTH_AFTER_STOP, 0);
                cv.put(COLUMN_AVATAR_URI, Task.DEFAULT_AVATAR_URI);
                db.update(TABLE_TASKS, cv, null, null);
            }*/
        }
    }
}