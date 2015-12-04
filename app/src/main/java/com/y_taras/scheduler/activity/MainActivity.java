package com.y_taras.scheduler.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.scheduler.R;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Random;

import adapter.AdapterForTaskList;
import adapter.CustomSpinnerAdapter;
import other.StringKeys;
import other.Task;
import utils.TasksLoader;

public class MainActivity extends ActionBarActivity {
    private static final String ArrayOfTasks = "tasks";
    private static final String AlertBool = "alertBool";
    private static final int TimeForExit = 3500;
    private static final int RequestCodeAddTask = 1;
    private static final int RequestCodeEditTask = 2;
    private static final int RequestCodeSettings = 3;
    private static long timeBackPressed;
    private ArrayList<Task> mTasks;
    private AdapterForTaskList mListAdapter;
    private Comparator<Task> mTaskComparator;

    private int mCompletedTaskColor;
    private int mStartedTaskColor;
    private int mNotStartedTaskColor;

    private TasksLoader mTasksReader;
    private TasksLoader mTasksSaver;

    private Spinner mSpinnerSort;
    private int mSpinnerPos;

    private AlertDialog mAlertForClear;
    private boolean mIfAlertDWasShown;
    private Toast mToast;
    private SharedPreferences mAppSettings;

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
            if (savedInstanceState.containsKey(ArrayOfTasks))
                mTasks = savedInstanceState.getParcelableArrayList(ArrayOfTasks);
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
        final ListView listView = (ListView) findViewById(R.id.listView);
        //текст для пустого списку
        listView.setEmptyView(findViewById(R.id.emptyList));
        mListAdapter = new AdapterForTaskList(this, mTasks,
                mNotStartedTaskColor, mStartedTaskColor, mCompletedTaskColor);
        listView.setAdapter(mListAdapter);
        //ініціалізація класу для записау і читання збережених завдань
        mTasksReader = TasksLoader.createReader(mTasks, this, mListAdapter);
        mTasksSaver = TasksLoader.createSaver(mTasks, this);

        //спроба загрузити таски збережені в SharedPreference
        if (savedInstanceState == null)
            mTasksReader.execute();
        //обробка одиничного кліку по елементу із списку завдань
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                Task clickTask = mTasks.get(position);
                if (clickTask.getDateStart() == null) {
                    clickTask.setDateStart(new Date());
                    sortTasks();
                    mListAdapter.notifyDataSetChanged();
                    //Збереження змін в списку завдань
                    mTasksSaver.execute();
                } else if (clickTask.getDateEnd() == null) {
                    clickTask.setDateEnd(new Date());
                    clickTask.calcTimeSpent();
                    mListAdapter.notifyDataSetChanged();
                    //Збереження змін в списку завдань
                    mTasksSaver.execute();
                }
            }
        });
        //обробка longClick по елементу із списку завдань
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Task clickTask = mTasks.get(position);
                Intent intent = new Intent(MainActivity.this, AddScheduleActivity.class);
                intent.setAction(StringKeys.EDIT_TASK);
                intent.putExtra(StringKeys.TASK_POSITION, position);
                intent.putExtra(StringKeys.TASK_TITLE, clickTask.getTitle());
                intent.putExtra(StringKeys.TASK_COMMENT, clickTask.getComment());
                startActivityForResult(intent, RequestCodeEditTask);
                return true;
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
            switch (requestCode) {
                case RequestCodeAddTask:
                    mTasks.add(new Task(name, comment));
                    sortTasks();
                    break;
                case RequestCodeEditTask:
                    Task editTask = mTasks.get(data.getIntExtra(StringKeys.TASK_POSITION, -1));
                    editTask.setTitle(name);
                    editTask.setComment(comment);
                    sortTasks();
                    break;
                case RequestCodeSettings:
                    int notStartedTaskColor = data.getIntExtra(StringKeys.NOT_STARTED_TASK, -1);
                    int startedTaskColor = data.getIntExtra(StringKeys.STARTED_TASK, -1);
                    int completedTaskColor = data.getIntExtra(StringKeys.COMPLETED_TASK, -1);
                    SharedPreferences.Editor editor = mAppSettings.edit();
                    boolean ifWasChanges = false;
                    if (mNotStartedTaskColor != notStartedTaskColor) {
                        ifWasChanges = true;
                        mNotStartedTaskColor = notStartedTaskColor;
                        editor.putInt(StringKeys.NOT_STARTED_TASK, mNotStartedTaskColor);
                        mListAdapter.setNotStartedTaskColor(mNotStartedTaskColor);
                    }
                    if (mStartedTaskColor != startedTaskColor) {
                        ifWasChanges = true;
                        mStartedTaskColor = startedTaskColor;
                        editor.putInt(StringKeys.STARTED_TASK, mStartedTaskColor);
                        mListAdapter.setStartedTaskColor(mStartedTaskColor);
                    }
                    if (mCompletedTaskColor != completedTaskColor) {
                        ifWasChanges = true;
                        mCompletedTaskColor = completedTaskColor;
                        editor.putInt(StringKeys.COMPLETED_TASK, mCompletedTaskColor);
                        mListAdapter.setCompletedTaskColor(mCompletedTaskColor);
                    }
                    if (ifWasChanges) {
                        editor.apply();
                        mListAdapter.notifyDataSetChanged();
                    }
                    break;
            }
            mListAdapter.notifyDataSetChanged();
            //Збереження змін в списку завдань
            mTasksSaver.execute();
        }
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(ArrayOfTasks, mTasks);
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
            case R.id.action_add_task:
                Intent intent = new Intent(MainActivity.this, AddScheduleActivity.class);
                intent.setAction(StringKeys.ADD_TASK);
                startActivityForResult(intent, RequestCodeAddTask);
                break;
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
                mListAdapter.notifyDataSetChanged();
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
                startActivityForResult(intentForSettings, RequestCodeSettings);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //показ діалога для підтвердження стирання усіх завдань
    private void showAlertDialogForDelete() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.clearAlertDialogTitle)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mIfAlertDWasShown = false;
                        mTasks.clear();
                        mListAdapter.notifyDataSetChanged();
                        mTasksSaver.execute();
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mIfAlertDWasShown = false;
                        dialog.cancel();
                    }
                });
        mAlertForClear = builder.create();
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
    private void sortTasks() {
        Collections.sort(mTasks, mTaskComparator);
        mListAdapter.notifyDataSetChanged();
        mTasksSaver.execute();
    }
}