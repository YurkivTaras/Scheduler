package com.y_taras.scheduler.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import android.widget.Toast;

import com.daimajia.swipe.util.Attributes;
import com.example.scheduler.R;
import com.melnykov.fab.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

import adapter.CustomSpinnerAdapter;
import adapter.SwipeRecyclerViewAdapter;
import me.drakeet.materialdialog.MaterialDialog;
import other.StringKeys;
import other.Task;
import utils.AlarmManagerBroadcastReceiver;
import utils.TasksLoader;

public class MainActivity extends AppCompatActivity {

    private static final String AlertBool = "alertBool";
    private static final int TimeForExit = 3500;
    private static final int RequestCodeAddTask = 1;
    public static final int REQUEST_CODE_EDIT_TASK = 2;
    private static final int RequestCodeSettings = 3;
    private static long timeBackPressed;
    private ArrayList<Task> mTasks;

    private SwipeRecyclerViewAdapter mSwipeListAdapter;
    private Comparator<Task> mTaskComparator;

    private int mCompletedTaskColor;
    private int mStartedTaskColor;
    private int mNotStartedTaskColor;

    private TasksLoader mTasksReader;
    private TasksLoader mTasksSaver;

    private FloatingActionButton floatBtn;
    private Spinner mSpinnerSort;
    private int mSpinnerPos;

    private MaterialDialog mAlertForClear;
    private boolean mIfAlertDWasShown;
    private Toast mToast;
    private SharedPreferences mAppSettings;

    private AlarmManagerBroadcastReceiver alarm;
    private BroadcastReceiver broadcastReceiver;
    private int mMaxRuntimeForTask;

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle b = intent.getExtras();
                ArrayList<Task> tasks = b.getParcelableArrayList(StringKeys.ARRAY_OF_TASKS);
                if (tasks == null || tasks.size() != mTasks.size()) {
                    sortTasks();
                    return;
                }
                //Log.d("======", "MainActivity tasks.size()=" + tasks.size());
                for (int i = 0; i < mTasks.size(); i++) {
                    Task taskOriginal = mTasks.get(i);
                    Task taskFromIntent = tasks.get(i);
                    if (taskFromIntent.getDateEnd() != null && taskOriginal.getDateEnd() == null/* &&
                            taskFromIntent.getDateStart().equals(taskOriginal.getDateStart()) &&
                            taskFromIntent.getTitle().equals(taskOriginal.getTitle()) &&
                            taskFromIntent.getComment().equals(taskOriginal.getComment())*/) {
                        taskOriginal.setDateEnd(taskFromIntent.getDateEnd());
                        taskOriginal.calcTimeSpent();
                        mSwipeListAdapter.notifyItemChanged(i);
                        //   Log.d("======", "MainActivity tasks[" + i + "] is modified");
                    }
                }

            }
        };
        registerReceiver(broadcastReceiver, new IntentFilter("broadCastName"));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAlertForClear != null)
            mAlertForClear.dismiss();
    }

    private void initUI(Bundle savedInstanceState) {
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbarForMainActivity);
        mToolbar.setTitle(R.string.mainToolbarTitle);
        setSupportActionBar(mToolbar);
        if (savedInstanceState == null)
            mTasks = new ArrayList<Task>();
        else {
            if (savedInstanceState.containsKey(StringKeys.ARRAY_OF_TASKS))
                mTasks = savedInstanceState.getParcelableArrayList(StringKeys.ARRAY_OF_TASKS);
            if (savedInstanceState.containsKey(AlertBool) && savedInstanceState.getBoolean(AlertBool))
                showAlertDialogForDelete();
        }

        //отримання попередніх користувацьких налаштувань
        getSettingsFromSharedPref();

        //ініціалізація спінера для вибору режиму сортування
        mSpinnerSort = (Spinner) findViewById(R.id.spinner_nav);
        CustomSpinnerAdapter spinnerAdapter = new CustomSpinnerAdapter(getApplicationContext(), getResources().getStringArray(R.array.spinner_items));
        mSpinnerSort.setAdapter(spinnerAdapter);
        mSpinnerSort.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapter, View v,
                                       int position, long id) {
                switch (position) {
                    case 0: //A-Z
                        mTaskComparator = Task.NameUPComparator;
                        break;
                    case 1: //Z-A
                        mTaskComparator = Task.NameDownComparator;
                        break;
                    case 2: //Data_up
                        mTaskComparator = Task.DateUPComparator;
                        break;
                    case 3://Data_down
                        mTaskComparator = Task.DateDownComparator;
                        break;
                }
                SharedPreferences.Editor edit = mAppSettings.edit();
                mSpinnerPos = position;
                edit.putInt(StringKeys.SPINNER_POS, mSpinnerPos);
                edit.apply();
                sortTasks();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
        //переключення на попередньо вибраний режим сортування
        mSpinnerSort.setSelection(mSpinnerPos);

        //ініціалізація списку із завданнями
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(llm);

        mSwipeListAdapter = new SwipeRecyclerViewAdapter(this, mTasks, mNotStartedTaskColor, mStartedTaskColor, mCompletedTaskColor);
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
        //ініціалізація класу для запису і читання збережених завдань
        mTasksReader = TasksLoader.createReader(mTasks, this, mSwipeListAdapter);
        mTasksSaver = TasksLoader.createSaver(mTasks, this);

        //спроба загрузити таски збережені в SharedPreference
        if (savedInstanceState == null)
            mTasksReader.execute();
        alarm = new AlarmManagerBroadcastReceiver();
        //ініціалізація плаваючої кнопки
        floatBtn = (FloatingActionButton) findViewById(R.id.fab);
        floatBtn.attachToRecyclerView(recyclerView);
        floatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddScheduleActivity.class);
                intent.setAction(StringKeys.ADD_TASK);
                startActivityForResult(intent, RequestCodeAddTask);
            }
        });
    }

    //отримання збережених налаштувань
    private void getSettingsFromSharedPref() {
        mAppSettings = getPreferences(MODE_PRIVATE);
        mNotStartedTaskColor = mAppSettings.getInt(
                StringKeys.NOT_STARTED_TASK, getResources().getColor(R.color.not_started_task));
        mStartedTaskColor = mAppSettings.getInt(
                StringKeys.STARTED_TASK, getResources().getColor(R.color.started_task));
        mCompletedTaskColor = mAppSettings.getInt(
                StringKeys.COMPLETED_TASK, getResources().getColor(R.color.completed_task));
        mSpinnerPos = mAppSettings.getInt(StringKeys.SPINNER_POS, 0);

        SharedPreferences sharedPrefForTasks = getSharedPreferences(StringKeys.JSON_PREFERENCES_FOR_TASKS, MODE_PRIVATE);
        mMaxRuntimeForTask = sharedPrefForTasks.getInt(StringKeys.MAX_RUNTIME_FOR_TASK, 60);
        //отримання збереженого типу сортування
        String compId = mAppSettings.getString(StringKeys.COMPARATOR_TYPE, StringKeys.COMPARATOR_A_Z);
        switch (compId) {
            case StringKeys.COMPARATOR_A_Z:
                mTaskComparator = Task.NameUPComparator;
                break;
            case StringKeys.COMPARATOR_Z_A:
                mTaskComparator = Task.NameDownComparator;
                break;
            case StringKeys.COMPARATOR_DATE_UP:
                mTaskComparator = Task.DateUPComparator;
                break;
            case StringKeys.COMPARATOR_DATE_DOWN:
                mTaskComparator = Task.DateDownComparator;
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            String name = data.getStringExtra(StringKeys.TASK_TITLE);
            String comment = data.getStringExtra(StringKeys.TASK_COMMENT);
            boolean ifWasChanges = false;
            switch (requestCode) {
                case RequestCodeAddTask:
                    mTasks.add(new Task(name, comment));
                    sortTasks();
                    ifWasChanges = true;
                    break;
                case REQUEST_CODE_EDIT_TASK:
                    Task editTask = mTasks.get(data.getIntExtra(StringKeys.TASK_POSITION, -1));
                    editTask.setTitle(name);
                    editTask.setComment(comment);
                    sortTasks();
                    break;
                case RequestCodeSettings:
                    int notStartedTaskColor = data.getIntExtra(StringKeys.NOT_STARTED_TASK, -1);
                    int startedTaskColor = data.getIntExtra(StringKeys.STARTED_TASK, -1);
                    int completedTaskColor = data.getIntExtra(StringKeys.COMPLETED_TASK, -1);
                    int maxRuntimeForTask = data.getIntExtra(StringKeys.MAX_RUNTIME_FOR_TASK, -1);
                    SharedPreferences.Editor editor = mAppSettings.edit();
                    boolean ifWasColorChanges = false;
                    if (mNotStartedTaskColor != notStartedTaskColor) {
                        ifWasColorChanges = true;
                        mNotStartedTaskColor = notStartedTaskColor;
                        editor.putInt(StringKeys.NOT_STARTED_TASK, mNotStartedTaskColor);
                        mSwipeListAdapter.setNotStartedTaskColor(mNotStartedTaskColor);
                    }
                    if (mStartedTaskColor != startedTaskColor) {
                        ifWasColorChanges = true;
                        mStartedTaskColor = startedTaskColor;
                        editor.putInt(StringKeys.STARTED_TASK, mStartedTaskColor);
                        mSwipeListAdapter.setStartedTaskColor(mStartedTaskColor);
                    }
                    if (mCompletedTaskColor != completedTaskColor) {
                        ifWasColorChanges = true;
                        mCompletedTaskColor = completedTaskColor;
                        editor.putInt(StringKeys.COMPLETED_TASK, mCompletedTaskColor);
                        mSwipeListAdapter.setCompletedTaskColor(mCompletedTaskColor);
                    }
                    //якщо було змінено максимальний час виконання завдання
                    if (mMaxRuntimeForTask != maxRuntimeForTask) {
                        mMaxRuntimeForTask = maxRuntimeForTask;
                        SharedPreferences jSonSharedPreferences = getSharedPreferences(
                                StringKeys.JSON_PREFERENCES_FOR_TASKS, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editorForMaxRuntime = jSonSharedPreferences.edit();
                        editorForMaxRuntime.putInt(StringKeys.MAX_RUNTIME_FOR_TASK, mMaxRuntimeForTask);
                        editorForMaxRuntime.apply();
                        //перезапускаєм таймер
                        setTimer();
                    }
                    if (ifWasColorChanges) {
                        ifWasChanges = true;
                        editor.apply();
                        mSwipeListAdapter.notifyDataSetChanged();
                    }
                    break;
            }
            //Збереження змін в списку завдань
            if (ifWasChanges)
                mTasksSaver.execute();
        }
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(StringKeys.ARRAY_OF_TASKS, mTasks);
        outState.putBoolean(AlertBool, mIfAlertDWasShown);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    //обробка натисків на елементи toolbar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_clear:
                showAlertDialogForDelete();
                break;
            case R.id.action_generate:
                Random random = new Random();
                for (int i = 0; i < 30; i++) {
                    int randomNumb = random.nextInt(30);
                    mTasks.add(new Task("Title" + randomNumb, "Comment" + randomNumb));
                }
                sortTasks();
                mTasksSaver.execute();
                mSwipeListAdapter.notifyDataSetChanged();
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
                        mSwipeListAdapter.notifyDataSetChanged();
                        mTasksSaver.execute();
                        floatBtn.show();
                        mAlertForClear.dismiss();
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
        setTimer();
        mTasksSaver.execute();
    }

    public void setTimer() {
        int i = 0;
        /*for (Task t : mTasks) {
            if (t.getDateStart() != null && t.getDateEnd() == null)
                Log.d("SET_TIMER", "t[" + i + "] can be change");
            i++;
        }*/
        alarm.setTimer(this.getApplicationContext(), mMaxRuntimeForTask, mTasks);
    }

    public TasksLoader getTasksSaver() {
        return mTasksSaver;
    }
}