package com.y_taras.scheduler.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ExpandableListView;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.Toast;

import com.daimajia.swipe.util.Attributes;
import com.example.scheduler.R;
import com.melnykov.fab.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import adapter.CustomExpandableListAdapter;
import adapter.CustomSpinnerAdapter;
import adapter.DividerItemDecoration;
import adapter.SwipeRecyclerViewAdapter;
import me.drakeet.materialdialog.MaterialDialog;
import other.Statistic;
import other.StringKeys;
import other.Task;
import utils.AlarmManagerBroadcastReceiver;
import utils.DatabaseConnector;
import utils.ImageLoader;

public class MainActivity extends AppCompatActivity {

    private static final String AlertBool = "alertBool";
    private static final int TimeForExit = 3500;
    private static final int RequestCodeAddTask = 1;
    public static final int REQUEST_CODE_EDIT_TASK = 2;
    private static final int RequestCodeSettings = 3;
    private static long timeBackPressed;
    private ArrayList<Task> mTasks;

    private SwipeRecyclerViewAdapter mSwipeListAdapter;
    private CustomExpandableListAdapter mExpListAdapter;
    private ArrayList<ArrayList<Task>> mGroups;
    private ArrayList<String> mGroupsName;
    private Comparator<Task> mTaskComparator;

    private int mCompletedTaskColor;
    private int mStartedTaskColor;
    private int mNotStartedTaskColor;

    private FloatingActionButton floatBtn;
    private Spinner mSpinnerSort;
    private TabHost mTabHost;
    private int mSpinnerPos;
    private String mTabHostPos;

    private Menu mOptionsMenu;

    private MaterialDialog mAlertForClear;
    private boolean mIfAlertDWasShown;
    private Toast mToast;
    private SharedPreferences mAppSettings;

    private AlarmManagerBroadcastReceiver alarm;
    private BroadcastReceiver broadcastReceiver;
    private int mMaxRuntimeForTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
        if (mAlertForClear != null)
            mAlertForClear.dismiss();
    }

    private void initUI(Bundle savedInstanceState) {
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbarForMainActivity);
        mToolbar.setTitle(R.string.mainToolbarTitle);
        setSupportActionBar(mToolbar);
        if (savedInstanceState == null) {
            mTasks = new ArrayList<>();
            mGroups = new ArrayList<>();
            mGroupsName = new ArrayList<>();
        } else {
            if (savedInstanceState.containsKey(StringKeys.ARRAY_OF_TASKS))
                mTasks = savedInstanceState.getParcelableArrayList(StringKeys.ARRAY_OF_TASKS);
            if (savedInstanceState.containsKey(StringKeys.ARRAY_OF_STATISTICS))
                //noinspection unchecked
                mGroups = (ArrayList<ArrayList<Task>>) savedInstanceState.getSerializable(StringKeys.ARRAY_OF_STATISTICS);
            if (savedInstanceState.containsKey(StringKeys.ARRAY_OF_NAMES_FOR_STATISTICS))
                mGroupsName = savedInstanceState.getStringArrayList(StringKeys.ARRAY_OF_NAMES_FOR_STATISTICS);
            if (savedInstanceState.containsKey(StringKeys.TAB_HOST_POS))
                mTabHostPos = savedInstanceState.getString(StringKeys.TAB_HOST_POS);
            if (savedInstanceState.containsKey(AlertBool) && savedInstanceState.getBoolean(AlertBool))
                showAlertDialogForDelete();
        }

        //отримання попередніх користувацьких налаштувань
        getSettingsFromSharedPref();

        //ініціалізація спінера для вибору режиму сортування
        mSpinnerSort = (Spinner) findViewById(R.id.spinner_nav);
        CustomSpinnerAdapter spinnerAdapter = new CustomSpinnerAdapter(getApplicationContext(),
                getResources().getStringArray(R.array.spinner_items));
        mSpinnerSort.setAdapter(spinnerAdapter);
        mSpinnerSort.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapter, View v,
                                       int position, long id) {
                SharedPreferences.Editor edit = mAppSettings.edit();
                mSpinnerPos = position;
                edit.putInt(StringKeys.SPINNER_POS, mSpinnerPos);
                edit.apply();
                switch (position) {
                    case 0:     //A-Z
                        mTaskComparator = Task.NameUPComparator;
                        break;
                    case 1:     //Z-A
                        mTaskComparator = Task.NameDownComparator;
                        break;
                    case 2:     //Data_up
                        mTaskComparator = Task.DateUPComparator;
                        break;
                    case 3:     //Data_down
                        mTaskComparator = Task.DateDownComparator;
                        break;
                }
                sortTasks();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
        //переключення на попередньо вибраний режим сортування
        mSpinnerSort.setSelection(mSpinnerPos);

        //ініціалізація вкладок (TabHost)
        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup();
        TabHost.TabSpec tabSpec;
        tabSpec = mTabHost.newTabSpec("tag1");
        tabSpec.setIndicator("Завдання");
        tabSpec.setContent(R.id.tab1);
        mTabHost.addTab(tabSpec);
        tabSpec = mTabHost.newTabSpec("tag2");
        tabSpec.setIndicator("Статистика");
        tabSpec.setContent(R.id.tab2);
        mTabHost.addTab(tabSpec);
        //встановлення попередньо вибраної вкладки(при повороті екрану)
        if (mTabHostPos != null)
            mTabHost.setCurrentTabByTag(mTabHostPos);

        //ініціалізація списку із завданнями
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(llm);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, null, true, true));
        mSwipeListAdapter = new SwipeRecyclerViewAdapter(this, mTasks,
                mNotStartedTaskColor, mStartedTaskColor, mCompletedTaskColor);
        mSwipeListAdapter.setMode(Attributes.Mode.Single);

        recyclerView.setAdapter(mSwipeListAdapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                mSwipeListAdapter.closeAllItems();
            }
        });

        // ініціалізуєм список із статистикою
        ExpandableListView listView = (ExpandableListView) findViewById(R.id.tab2);
        mExpListAdapter = new CustomExpandableListAdapter(getApplicationContext(), mGroups, mGroupsName);
        listView.setAdapter(mExpListAdapter);

        alarm = new AlarmManagerBroadcastReceiver();
        if (savedInstanceState == null) {
            downloadTask();
            downloadStatistic(false);
        }
        //ініціалізуєм та регіструєм ресівер, що відловлює повідомлення(broadcast)
        //із автоматично завершеними завданнями
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle b = intent.getExtras();
                ArrayList<Task> tasks = b.getParcelableArrayList(StringKeys.ARRAY_OF_TASKS);
                if (tasks == null)
                    return;
                for (int i = 0; i < tasks.size(); i++) {
                    Task taskFromIntent = tasks.get(i);
                    Date dateEndForTaskFormIntent = taskFromIntent.getDateEnd();
                    if (dateEndForTaskFormIntent != null) {
                        long id_taskFromIntent = taskFromIntent.getDatabase_ID();
                        for (int j = 0; j < mTasks.size(); j++) {
                            Task originalTask = mTasks.get(j);
                            if (originalTask.getDatabase_ID() == id_taskFromIntent) {
                                if (originalTask.getDateEnd() == null) {
                                    originalTask.setDateEnd(dateEndForTaskFormIntent);
                                    mSwipeListAdapter.notifyItemChanged(j);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        };
        registerReceiver(broadcastReceiver, new IntentFilter("broadCastName"));

        //ініціалізація плаваючої кнопки
        floatBtn = (FloatingActionButton) findViewById(R.id.fab);
        floatBtn.attachToRecyclerView(recyclerView);
        floatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddScheduleActivity.class);
                intent.setAction(StringKeys.ADD_TASK);
                intent.putExtra(StringKeys.MAX_RUNTIME_FOR_TASK, mMaxRuntimeForTask);
                startActivityForResult(intent, RequestCodeAddTask);

            }
        });
    }

    //отримання збережених налаштувань
    private void getSettingsFromSharedPref() {
        mAppSettings = getSharedPreferences(StringKeys.APP_SETTINGS, MODE_PRIVATE);
        mNotStartedTaskColor = mAppSettings.getInt(
                StringKeys.NOT_STARTED_TASK, getResources().getColor(R.color.not_started_task));
        mStartedTaskColor = mAppSettings.getInt(
                StringKeys.STARTED_TASK, getResources().getColor(R.color.started_task));
        mCompletedTaskColor = mAppSettings.getInt(
                StringKeys.COMPLETED_TASK, getResources().getColor(R.color.completed_task));
        mMaxRuntimeForTask = mAppSettings.getInt(StringKeys.MAX_RUNTIME_FOR_TASK, 60);

        //отримання збереженого типу сортування
        mSpinnerPos = mAppSettings.getInt(StringKeys.SPINNER_POS, 0);
        switch (mSpinnerPos) {
            case 0:     //A-Z
                mTaskComparator = Task.NameUPComparator;
                break;
            case 1:     //Z-A
                mTaskComparator = Task.NameDownComparator;
                break;
            case 2:     //DATE_UP
                mTaskComparator = Task.DateUPComparator;
                break;
            case 3:     //DATE_DOWN
                mTaskComparator = Task.DateDownComparator;
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            String title = data.getStringExtra(StringKeys.TASK_TITLE);
            String comment = data.getStringExtra(StringKeys.TASK_COMMENT);
            boolean typeOfTask = data.getBooleanExtra(StringKeys.TYPE_OF_TASK, false);
            int maxRuntime = data.getIntExtra(StringKeys.MAX_RUNTIME_FOR_TASK, mMaxRuntimeForTask);
            String avatarUri = null;
            if (data.hasExtra(StringKeys.BITMAP_AVATAR))
                avatarUri = data.getStringExtra(StringKeys.BITMAP_AVATAR);
            switch (requestCode) {
                case RequestCodeAddTask:
                    Task newTask = new Task(title, comment, typeOfTask, maxRuntime);
                    if (avatarUri != null)
                        newTask.setAvatarUri(avatarUri);
                    DatabaseConnector.addTask(newTask, this);
                    mTasks.add(newTask);
                    sortTasks();
                    break;
                case REQUEST_CODE_EDIT_TASK:
                    int position = data.getIntExtra(StringKeys.TASK_POSITION, -1);
                    Task editTask = mTasks.get(position);
                    boolean ifWasChange = false;
                    if (!editTask.getTitle().equals(title) || !editTask.getComment().equals(comment)) {
                        editTask.setTitle(title);
                        editTask.setComment(comment);
                        //пересортовуєм масив завдань, якщо було змінено title або comment
                        sortTasks();
                        ifWasChange = true;
                    }
                    if (avatarUri != null) {
                        editTask.setAvatarUri(avatarUri);
                        ifWasChange = true;
                        mSwipeListAdapter.notifyItemChanged(position);
                    }
                    if (editTask.getMaxRuntime() != maxRuntime) {
                        editTask.setMaxRuntime(maxRuntime);
                        //перезапускаєм таймер якщо було змінено максимальний час виконання для завдання
                        setTimer();
                        ifWasChange = true;
                    }
                    if (ifWasChange)
                        DatabaseConnector.updateTask(editTask, this);
                    break;
                case RequestCodeSettings:
                    int notStartedTaskColor = data.getIntExtra(StringKeys.NOT_STARTED_TASK, -1);
                    int startedTaskColor = data.getIntExtra(StringKeys.STARTED_TASK, -1);
                    int completedTaskColor = data.getIntExtra(StringKeys.COMPLETED_TASK, -1);
                    int maxRuntimeForTask = data.getIntExtra(StringKeys.MAX_RUNTIME_FOR_TASK, -1);
                    SharedPreferences.Editor editor = mAppSettings.edit();
                    boolean ifWasChanges = false;
                    if (mNotStartedTaskColor != notStartedTaskColor) {
                        ifWasChanges = true;
                        mNotStartedTaskColor = notStartedTaskColor;
                        editor.putInt(StringKeys.NOT_STARTED_TASK, mNotStartedTaskColor);
                        mSwipeListAdapter.setNotStartedTaskColor(mNotStartedTaskColor);
                    }
                    if (mStartedTaskColor != startedTaskColor) {
                        ifWasChanges = true;
                        mStartedTaskColor = startedTaskColor;
                        editor.putInt(StringKeys.STARTED_TASK, mStartedTaskColor);
                        mSwipeListAdapter.setStartedTaskColor(mStartedTaskColor);
                    }
                    if (mCompletedTaskColor != completedTaskColor) {
                        ifWasChanges = true;
                        mCompletedTaskColor = completedTaskColor;
                        editor.putInt(StringKeys.COMPLETED_TASK, mCompletedTaskColor);
                        mSwipeListAdapter.setCompletedTaskColor(mCompletedTaskColor);
                    }
                    //оновлюєм список, якщо було змінено кольори
                    if (ifWasChanges)
                        mSwipeListAdapter.notifyDataSetChanged();
                    //якщо було змінено максимальний час виконання завдання
                    if (mMaxRuntimeForTask != maxRuntimeForTask) {
                        ifWasChanges = true;
                        mMaxRuntimeForTask = maxRuntimeForTask;
                        editor.putInt(StringKeys.MAX_RUNTIME_FOR_TASK, mMaxRuntimeForTask);
                        //перезапускаєм таймер
                        setTimer();
                    }
                    //зберігаєм зміни в SharedPreferences, якщо такі були
                    if (ifWasChanges)
                        editor.apply();
                    break;
            }
        }
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(StringKeys.ARRAY_OF_TASKS, mTasks);
        outState.putSerializable(StringKeys.ARRAY_OF_STATISTICS, mGroups);
        outState.putStringArrayList(StringKeys.ARRAY_OF_NAMES_FOR_STATISTICS, mGroupsName);
        outState.putString(StringKeys.TAB_HOST_POS, mTabHost.getCurrentTabTag());
        outState.putBoolean(AlertBool, mIfAlertDWasShown);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mOptionsMenu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //обробка натисків на елементи toolbar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_refresh:
                downloadStatistic(true);
                break;
            case R.id.action_clear:
                showAlertDialogForDelete();
                break;
            case R.id.action_generate:
                Random random = new Random();
                ArrayList<Task> tasks = new ArrayList<>();
                for (int i = 0; i < 30; i++) {
                    int randomForTitle = random.nextInt(30);
                    int randomForComment = random.nextInt(30);
                    Task newTask = new Task("Title" + randomForTitle, "Comment" + randomForComment,
                            randomForComment % 2 == 0, mMaxRuntimeForTask);
                    tasks.add(newTask);
                    mTasks.add(newTask);
                }
                DatabaseConnector.addAllTasks(tasks, this);
                sortTasks();
                setTimer();
                break;
            case R.id.action_exit:
                finish();
                break;
            case R.id.action_sort:
                mSpinnerSort.performClick();
                break;
            case R.id.action_settings:
                Intent intentForSettings = new Intent(MainActivity.this, SettingsActivity.class);
                intentForSettings.putExtra(StringKeys.NOT_STARTED_TASK, mNotStartedTaskColor);
                intentForSettings.putExtra(StringKeys.STARTED_TASK, mStartedTaskColor);
                intentForSettings.putExtra(StringKeys.COMPLETED_TASK, mCompletedTaskColor);
                intentForSettings.putExtra(StringKeys.MAX_RUNTIME_FOR_TASK, mMaxRuntimeForTask);
                startActivityForResult(intentForSettings, RequestCodeSettings);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //показ діалога для підтвердження стирання усіх завдань
    private void showAlertDialogForDelete() {
        mAlertForClear = new MaterialDialog(this)
                .setMessage(R.string.clearAlertDialogTitle)
                .setPositiveButton(R.string.yes, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mIfAlertDWasShown = false;
                        mTasks.clear();

                        //очищаєм таблиці завдань та статистики
                        DatabaseConnector.deleteAllTasks(getApplicationContext());
                        DatabaseConnector.deleteAllStatistics(getApplicationContext());
                        mSwipeListAdapter.notifyDataSetChanged();
                        floatBtn.show();
                        mAlertForClear.dismiss();
                        //відміняєм запущений раніше запит(якщо такий був)
                        setTimer();
                        //видаляєм з памяті зображення усіх іконок для завдань
                        ImageLoader.deleteAll(getBaseContext());
                    }
                })
                .setNegativeButton(R.string.no, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mIfAlertDWasShown = false;
                        mAlertForClear.dismiss();
                    }
                });
        mAlertForClear.show();
        mIfAlertDWasShown = true;
    }

    @Override
    public void onBackPressed() {
        if (timeBackPressed + TimeForExit > System.currentTimeMillis())
            super.onBackPressed();
        else {
            if (mToast != null)
                mToast.cancel();
            mToast = Toast.makeText(this, R.string.toastMessageIfBackPressed,
                    Toast.LENGTH_LONG);
            mToast.show();
        }
        timeBackPressed = System.currentTimeMillis();
    }

    //сортування завдань
    public void sortTasks() {
        Collections.sort(mTasks, mTaskComparator);
        mSwipeListAdapter.notifyDataSetChanged();
    }

    //виставлення таймера на автоматичне завершення завдань
    public void setTimer() {
        alarm.setTimer(this.getApplicationContext(), mTasks);
    }

    //завантаження завдань з бази даних
    private void downloadTask() {
        final DatabaseConnector databaseConnector = new DatabaseConnector(this);
        databaseConnector.open();
        new AsyncTask<Void, Void, Cursor>() {
            @Override
            protected Cursor doInBackground(Void... params) {
                return databaseConnector.getCursorWithAllTasks();
            }

            @Override
            protected void onPostExecute(Cursor cursor) {
                if (cursor.moveToFirst()) {
                    int idColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_ID);
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
                    do {
                        long id = cursor.getLong(idColIndex);
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
                        mTasks.add(new Task(id, title, comment, typeOfTask, avatarUri, maxRuntime,
                                dateStart, dateStop, dateEnd, datePause, pauseLengthBeforeStop, pauseLengthAfterStop));
                    } while (cursor.moveToNext());
                }
                cursor.close();
                databaseConnector.close();
                sortTasks();
                setTimer();
            }
        }.execute();
    }

    //завантаження з бази даних статистики по завданнях
    private void downloadStatistic(final boolean showProgress) {
        if (showProgress)
            setRefreshActionButtonState(true);
        final DatabaseConnector databaseConnector = new DatabaseConnector(this);
        databaseConnector.open();
        final ArrayList<Statistic> statistics = new ArrayList<>();
        final ArrayList<HashMap<Long, Task>> result = new ArrayList<>();
        final ArrayList<String> groupsName = new ArrayList<>();
        final String[] months = getResources().getStringArray(R.array.names_of_months);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                //перевірка роботи прогресбару
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Cursor cursor = databaseConnector.getCursorWithAllStatistics();
                if (cursor.moveToFirst()) {
                    int idColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_TASK_ID);
                    int dateStartColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_DATA_START);
                    int dateEndColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_DATA_END);
                    int sumPauseLengthColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_SUM_PAUSE_LENGTH);
                    do {
                        long id = cursor.getLong(idColIndex);
                        long dateStart = cursor.getLong(dateStartColIndex);
                        long dateEnd = cursor.getLong(dateEndColIndex);
                        long sumPauseLength = cursor.getLong(sumPauseLengthColIndex);
                        // Log.d("=============", "id=" + id + "start=" + new Date(dateStart) + "end=" + new Date(dateEnd) + "pause=" + sumPauseLength);
                        statistics.add(new Statistic(id, new Date(dateStart), new Date(dateEnd), sumPauseLength));
                    } while (cursor.moveToNext());
                }
                cursor.close();
                //заповнюєм масив з усіма періодичними тасками
                HashMap<Long, Task> tasks = new HashMap<>();
                cursor = databaseConnector.getCursorWithPeriodicalTasks();
                if (cursor.moveToFirst()) {
                    int idColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_ID);
                    int titleColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_TITLE);
                    int commentColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_COMMENT);
                    int avatarUriColIndex = cursor.getColumnIndex(DatabaseConnector.COLUMN_AVATAR_URI);
                    do {
                        long id = cursor.getLong(idColIndex);
                        String title = cursor.getString(titleColIndex);
                        String comment = cursor.getString(commentColIndex);
                        String avatarUri = cursor.getString(avatarUriColIndex);
                        tasks.put(id, new Task(id, title, comment, avatarUri, 0));
                    } while (cursor.moveToNext());
                }
                cursor.close();
                databaseConnector.close();

                //знаходимо найпізнішу дату
                long lastD = new Date().getTime();
                for (Statistic statistic : statistics) {
                    long date = statistic.getDateStart().getTime();
                    if (date < lastD)
                        lastD = date;
                }

                Calendar lastDate = Calendar.getInstance();
                lastDate.setTime(new Date(lastD));
                Calendar statisticDate = Calendar.getInstance();
                Calendar currentDate = Calendar.getInstance();
                do {
                    HashMap<Long, Task> group = new HashMap<>();
                    for (int i = 0; i < statistics.size(); i++) {
                        Statistic statistic = statistics.get(i);
                        if (!statistic.isUsed()) {
                            statisticDate.setTime(statistic.getDateStart());
                            if (lastDate.get(Calendar.MONTH) == statisticDate.get(Calendar.MONTH) &&
                                    lastDate.get(Calendar.YEAR) == statisticDate.get(Calendar.YEAR)) {
                                statistic.setUsed(true);
                                long task_id = statistic.getTask_ID();
                                long runtime = statistic.getDateEnd().getTime() - statistic.getDateStart().getTime() - statistic.getPauseLength();
                                if (!group.containsKey(task_id)) {
                                    Task task = tasks.get(task_id);
                                    group.put(task_id, new Task(task_id, task.getTitle(), task.getComment(), task.getAvatarUri(), runtime));
                                } else {
                                    Task task = group.get(task_id);
                                    task.setRuntime(task.getRuntime() + runtime);
                                }
                            }
                        }
                    }
                    if (group.size() != 0) {
                        result.add(group);
                        groupsName.add(String.format("%s %d", months[lastDate.get(Calendar.MONTH)], lastDate.get(Calendar.YEAR)));
                    }
                    lastDate.add(Calendar.MONTH, 1);
                }
                while ((lastDate.get(Calendar.MONTH) <= currentDate.get(Calendar.MONTH) &&
                        lastDate.get(Calendar.YEAR) <= currentDate.get(Calendar.YEAR)) ||
                        lastDate.get(Calendar.YEAR) < currentDate.get(Calendar.YEAR));
                return null;
            }

            @Override
            protected void onPostExecute(Void res) {
                //заповнюєм масив із назвами для груп
                mGroupsName.clear();
                for (String title : groupsName)
                    mGroupsName.add(title);
                //заповнюєм масив із даними для статистики
                mGroups.clear();
                for (int i = 0; i < result.size(); i++) {
                    HashMap<Long, Task> map = result.get(i);
                    ArrayList<Task> group = new ArrayList<>();
                    for (Map.Entry<Long, Task> me : map.entrySet())
                        group.add(me.getValue());
                    Collections.sort(group, mTaskComparator);
                    mGroups.add(group);
                }
                mExpListAdapter.notifyDataSetChanged();
                if (showProgress)
                    setRefreshActionButtonState(false);
            }
        }.execute();
    }

    public void setRefreshActionButtonState(final boolean refreshing) {
        if (mOptionsMenu != null) {
            final MenuItem refreshItem = mOptionsMenu.findItem(R.id.action_refresh);
            if (refreshItem != null)
                if (refreshing)
                    refreshItem.setActionView(R.layout.refresh_progress);
                else
                    refreshItem.setActionView(null);
        }
    }
}