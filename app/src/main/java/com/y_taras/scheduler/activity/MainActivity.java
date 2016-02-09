package com.y_taras.scheduler.activity;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.Toast;

import com.daimajia.swipe.util.Attributes;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
import com.melnykov.fab.FloatingActionButton;
import com.y_taras.scheduler.R;
import com.y_taras.scheduler.adapter.CustomExpandableListAdapter;
import com.y_taras.scheduler.adapter.CustomSpinnerAdapter;
import com.y_taras.scheduler.adapter.DividerItemDecoration;
import com.y_taras.scheduler.adapter.SwipeRecyclerViewAdapter;
import com.y_taras.scheduler.googleCalendar.ExportTask;
import com.y_taras.scheduler.googleCalendar.ImportGCTasks;
import com.y_taras.scheduler.googleDrive.DriveManager;
import com.y_taras.scheduler.googleDrive.OnLoadCompleteListener;
import com.y_taras.scheduler.helper.DatabaseConnector;
import com.y_taras.scheduler.helper.ImageLoader;
import com.y_taras.scheduler.other.Constants;
import com.y_taras.scheduler.other.Statistic;
import com.y_taras.scheduler.other.Task;
import com.y_taras.scheduler.service.LocationService;
import com.y_taras.scheduler.utils.AlarmManagerBroadcastReceiver;
import com.y_taras.scheduler.utils.AnimatedTabHostListener;
import com.y_taras.scheduler.utils.BackupAgent;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import fr.castorflex.android.circularprogressbar.CircularProgressBar;
import me.drakeet.materialdialog.MaterialDialog;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {
    public static final String TAG = "MainActivity";
    private static final String AlertBool = "alertBool";
    private static final int TimeForExit = 3500;
    private static final int RequestCodeAddTask = 1001;
    public static final int REQUEST_CODE_EDIT_TASK = 1002;
    public static final int REQUEST_AUTHORIZATION = 1006;
    private static final int RequestCodeSettings = 1003;
    private static final int AllTasksLoaderID = 1008;
    private static final int RequestCodeGDResolution = 1004;
    private static final int RequestAccountPicker = 1005;

    private static final int RequestGooglePlayServices = 1007;

    private static long sTimeBackPressed;
    private static boolean sFirstStart;

    private ShowcaseView mShowcaseView;
    private int mTargetPos;

    private ArrayList<Task> mTasks;
    private ArrayList<ArrayList<Task>> mGroups;
    private ArrayList<String> mGroupsName;

    private SwipeRecyclerViewAdapter mSwipeListAdapter;
    private CustomExpandableListAdapter mExpListAdapter;

    private CircularProgressBar mLoadListProgress;
    private Toolbar mToolbar;
    private FloatingActionButton mFloatBtn;
    private Spinner mSpinnerSort;
    private TabHost mTabHost;
    private int mSpinnerPos;
    private String mTabHostPos;
    private Menu mOptionsMenu;
    private MaterialDialog mAlertForClear;
    private boolean mIfAlertDWasShown;
    private Toast mToast;

    private Comparator<Task> mTaskComparator;

    private int mCompletedTaskColor;
    private int mStartedTaskColor;
    private int mNotStartedTaskColor;

    private AlarmManagerBroadcastReceiver alarm;
    private BroadcastReceiver mBroadcastReceiver;
    private int mMaxRuntimeForTask;

    private DatabaseConnector mDatabaseConnector;
    private GoogleApiClient mGoogleApiClient;

    private GoogleAccountCredential mCredential;
    private ProgressDialog mProgress;
    private ImportGCTasks mImportGCTasks;

    private static final String[] SCOPES = {CalendarScopes.CALENDAR};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BackupAgent.requestBackup(this);
        setContentView(R.layout.activity_main);
        initUI(savedInstanceState);

        //отримуєм силку на обєкт AsyncTask, що відповідає за імпорт завдань із GCalendar
        mImportGCTasks = (ImportGCTasks) getLastCustomNonConfigurationInstance();
        if (mImportGCTasks != null) {
            List<Task> result = mImportGCTasks.getResult();
            if (mImportGCTasks.getResult() != null) {
                for (int i = 0; i < result.size(); i++) {
                    DatabaseConnector.addTask(result.get(i), this);
                    mTasks.add(result.get(i));
                }
                sortTasks();
                showToast(String.format(getString(R.string.gCalendar_success_import), result.size()));
            } else
                mImportGCTasks.link(this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        connectToGD();
    }

    @Override
    protected void onPause() {
        disconnectGD();
        super.onPause();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        BackupAgent.requestBackup(this);
        unregisterReceiver(mBroadcastReceiver);
        if (mAlertForClear != null)
            mAlertForClear.dismiss();
        mDatabaseConnector.close();
    }

    private void initUI(Bundle savedInstanceState) {

        SharedPreferences appSettings = getSharedPreferences(Constants.APP_SETTINGS, MODE_PRIVATE);
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(appSettings.getString(Constants.ACCOUNT_NAME, null));

        mDatabaseConnector = new DatabaseConnector(this);
        mDatabaseConnector.open();

        mToolbar = (Toolbar) findViewById(R.id.toolbarForMainActivity);
        mToolbar.setTitle(R.string.mainToolbarTitle);
        mToolbar.inflateMenu(R.menu.menu_main);
        setSupportActionBar(mToolbar);

        mLoadListProgress = (CircularProgressBar) findViewById(R.id.load_list_progress_bar);
        if (savedInstanceState == null) {
            mTasks = new ArrayList<>();
            mGroups = new ArrayList<>();
            mGroupsName = new ArrayList<>();
            sFirstStart = appSettings.getBoolean(Constants.FIRST_START, true);
            if (sFirstStart)
                showTutorial(0);
        } else {
            if (savedInstanceState.containsKey(Constants.ARRAY_OF_TASKS))
                mTasks = savedInstanceState.getParcelableArrayList(Constants.ARRAY_OF_TASKS);
            if (savedInstanceState.containsKey(Constants.ARRAY_OF_STATISTICS))
                //noinspection unchecked
                mGroups = (ArrayList<ArrayList<Task>>) savedInstanceState.getSerializable(Constants.ARRAY_OF_STATISTICS);
            if (savedInstanceState.containsKey(Constants.ARRAY_OF_NAMES_FOR_STATISTICS))
                mGroupsName = savedInstanceState.getStringArrayList(Constants.ARRAY_OF_NAMES_FOR_STATISTICS);
            if (savedInstanceState.containsKey(Constants.TAB_HOST_POS))
                mTabHostPos = savedInstanceState.getString(Constants.TAB_HOST_POS);
            if (savedInstanceState.containsKey(AlertBool) && savedInstanceState.getBoolean(AlertBool))
                showAlertDialogForDelete();
            //відновлення незавершеного туторіала, якщо такий був
            if (savedInstanceState.containsKey(Constants.TUTORIAL))
                showTutorial(savedInstanceState.getInt(Constants.TUTORIAL));
        }

        //отримання попередніх користувацьких налаштувань
        initSettingsFromSharedPref();

        //ініціалізація спінера для вибору режиму сортування
        mSpinnerSort = (Spinner) findViewById(R.id.spinner_nav);
        CustomSpinnerAdapter spinnerAdapter = new CustomSpinnerAdapter(getApplicationContext(),
                getResources().getStringArray(R.array.spinner_items));
        mSpinnerSort.setAdapter(spinnerAdapter);
        mSpinnerSort.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapter, View v,
                                       int position, long id) {
                SharedPreferences settings = getSharedPreferences(Constants.APP_SETTINGS, MODE_PRIVATE);
                SharedPreferences.Editor edit = settings.edit();
                mSpinnerPos = position;
                edit.putInt(Constants.SPINNER_POS, mSpinnerPos);
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

        tabSpec.setIndicator(getString(R.string.tab1_indicator));
        tabSpec.setContent(R.id.tab1);
        mTabHost.addTab(tabSpec);
        tabSpec = mTabHost.newTabSpec("tag2");
        tabSpec.setIndicator(getString(R.string.tab2_indicator));
        tabSpec.setContent(R.id.tab2);
        mTabHost.addTab(tabSpec);
        //встановлення анімації при зміні вкладки
        mTabHost.setOnTabChangedListener(new AnimatedTabHostListener(getApplicationContext(), mTabHost));
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
        mSwipeListAdapter.notifyDataSetChanged();

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
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

        if (savedInstanceState == null && !sFirstStart) {
            // створюєм лоадер для читання даних
            getLoaderManager().initLoader(AllTasksLoaderID, null, this);
            showLoadProgress();
            //запускаєм лоадер
            getLoaderManager().getLoader(AllTasksLoaderID).forceLoad();
            downloadStatistic(false);
        }

        //ініціалізуєм та регіструєм ресівер, що відловлює повідомлення(broadcast)
        //із автоматично завершеними завданнями
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle b = intent.getExtras();
                String action = b.getString(Constants.ACTION);
                ArrayList<Task> tasks = b.getParcelableArrayList(Constants.ARRAY_OF_TASKS);
                if (tasks == null)
                    return;
                if (action != null && action.equals(Constants.ClOSE_TASK_ACTION))
                    for (int i = 0; i < tasks.size(); i++) {
                        Task taskFromIntent = tasks.get(i);
                        Date dateEndForTaskFormIntent = taskFromIntent.getDateEnd();
                        if (dateEndForTaskFormIntent != null) {
                            long taskFromIntentID = taskFromIntent.getDatabase_ID();
                            for (int j = 0; j < mTasks.size(); j++) {
                                Task originalTask = mTasks.get(j);
                                if (originalTask.getDatabase_ID() == taskFromIntentID) {
                                    if (originalTask.getDateEnd() == null) {
                                        originalTask.setDateEnd(dateEndForTaskFormIntent);
                                        mSwipeListAdapter.notifyItemChanged(j);
                                    }
                                    break;
                                }
                            }
                        }
                    }
                else if (action != null && action.equals(Constants.START_OR_PAUSE_TASK_ACTION)) {
                    for (int i = 0; i < tasks.size(); i++) {
                        Task taskFromIntent = tasks.get(i);
                        long taskFromIntentID = taskFromIntent.getDatabase_ID();
                        for (int j = 0; j < mTasks.size(); j++)
                            if (taskFromIntentID == mTasks.get(j).getDatabase_ID()) {
                                Task task = mTasks.get(j);
                                task.setDateStart(taskFromIntent.getDateStart());
                                task.setDateStop(taskFromIntent.getDateStop());
                                task.setDateEnd(taskFromIntent.getDateEnd());
                                task.setDatePause(taskFromIntent.getDatePause());
                                task.setPauseLengthAfterStop(taskFromIntent.getPauseLengthAfterStop());
                                task.setPauseLengthBeforeStop(taskFromIntent.getPauseLengthBeforeStop());
                                mSwipeListAdapter.notifyItemChanged(j);
                                break;
                            }
                    }
                }
            }
        };
        registerReceiver(mBroadcastReceiver, new IntentFilter(Constants.MAIN_ACTIVITY_BROADCAST));

        //ініціалізація плаваючої кнопки
        mFloatBtn = (FloatingActionButton) findViewById(R.id.fab);
        mFloatBtn.attachToRecyclerView(recyclerView);
        mFloatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
                intent.setAction(Constants.ADD_TASK);
                intent.putExtra(Constants.MAX_RUNTIME_FOR_TASK, mMaxRuntimeForTask);
                startActivityForResult(intent, RequestCodeAddTask);
            }
        });
        //запускаєм сервіс, що слідкує за місцезнаходженням
        startService(new Intent(this, LocationService.class));
    }


    //отримання збережених налаштувань
    private void initSettingsFromSharedPref() {
        SharedPreferences settings = getSharedPreferences(Constants.APP_SETTINGS, MODE_PRIVATE);
        mNotStartedTaskColor = settings.getInt(
                Constants.NOT_STARTED_TASK, ContextCompat.getColor(this, R.color.not_started_task));
        mStartedTaskColor = settings.getInt(
                Constants.STARTED_TASK, ContextCompat.getColor(this, R.color.started_task));
        mCompletedTaskColor = settings.getInt(
                Constants.COMPLETED_TASK, ContextCompat.getColor(this, R.color.completed_task));
        mMaxRuntimeForTask = settings.getInt(Constants.MAX_RUNTIME_FOR_TASK, 60);

        //отримання збереженого типу сортування
        mSpinnerPos = settings.getInt(Constants.SPINNER_POS, 0);
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

        switch (resultCode) {
            case Activity.RESULT_OK:
                String title, comment;
                String avatarUri = null;
                int maxRuntime;
                boolean hasMapPoint;
                double latitude, longitude;
                switch (requestCode) {
                    case RequestCodeAddTask:
                        title = data.getStringExtra(Constants.TASK_TITLE);
                        comment = data.getStringExtra(Constants.TASK_COMMENT);
                        boolean typeOfTask = data.getBooleanExtra(Constants.TYPE_OF_TASK, false);
                        maxRuntime = data.getIntExtra(Constants.MAX_RUNTIME_FOR_TASK, mMaxRuntimeForTask);
                        Task newTask = new Task(title, comment, typeOfTask, maxRuntime);
                        if (data.hasExtra(Constants.BITMAP_AVATAR))
                            avatarUri = data.getStringExtra(Constants.BITMAP_AVATAR);
                        if (avatarUri != null)
                            newTask.setAvatarUri(avatarUri);
                        hasMapPoint = data.getBooleanExtra(Constants.MAP_POINT, false);
                        if (hasMapPoint) {
                            latitude = data.getDoubleExtra(Constants.LATITUDE, 0);
                            longitude = data.getDoubleExtra(Constants.LONGITUDE, 0);
                            newTask.setMapPoint(true);
                            newTask.setLatitude(latitude);
                            newTask.setLongitude(longitude);
                        }
                        DatabaseConnector.addTask(newTask, this);
                        mTasks.add(newTask);
                        sortTasks();
                        break;
                    case REQUEST_CODE_EDIT_TASK:
                        title = data.getStringExtra(Constants.TASK_TITLE);
                        comment = data.getStringExtra(Constants.TASK_COMMENT);
                        maxRuntime = data.getIntExtra(Constants.MAX_RUNTIME_FOR_TASK, mMaxRuntimeForTask);
                        int position = data.getIntExtra(Constants.TASK_POSITION, -1);
                        Task editTask = mTasks.get(position);
                        boolean ifWasChange = false;
                        if (!editTask.getTitle().equals(title) || !editTask.getComment().equals(comment)) {
                            editTask.setTitle(title);
                            editTask.setComment(comment);
                            //пересортовуєм масив завдань, якщо було змінено title або comment
                            sortTasks();
                            ifWasChange = true;
                        }
                        if (data.hasExtra(Constants.BITMAP_AVATAR))
                            avatarUri = data.getStringExtra(Constants.BITMAP_AVATAR);
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
                        hasMapPoint = data.getBooleanExtra(Constants.MAP_POINT, false);
                        latitude = data.getDoubleExtra(Constants.LATITUDE, 0);
                        longitude = data.getDoubleExtra(Constants.LONGITUDE, 0);
                        //якщо було включено або виключено привязку до місця
                        if (editTask.hasMapPoint() != hasMapPoint) {
                            editTask.setMapPoint(hasMapPoint);
                            mSwipeListAdapter.notifyItemChanged(position);
                            ifWasChange = true;
                        }
                        //якщо було змінено координати
                        if (hasMapPoint && (editTask.getLatitude() != latitude || editTask.getLongitude() != longitude)) {
                            editTask.setLatitude(latitude);
                            editTask.setLongitude(longitude);
                            ifWasChange = true;
                        }
                        if (ifWasChange)
                            DatabaseConnector.updateTask(editTask, this);
                        break;
                    case RequestCodeSettings:
                        SharedPreferences settings = getSharedPreferences(Constants.APP_SETTINGS, MODE_PRIVATE);
                        int notStartedTaskColor = data.getIntExtra(Constants.NOT_STARTED_TASK, -1);
                        int startedTaskColor = data.getIntExtra(Constants.STARTED_TASK, -1);
                        int completedTaskColor = data.getIntExtra(Constants.COMPLETED_TASK, -1);
                        int maxRuntimeForTask = data.getIntExtra(Constants.MAX_RUNTIME_FOR_TASK, -1);

                        SharedPreferences.Editor editor = settings.edit();
                        boolean ifWasChanges = false;
                        if (mNotStartedTaskColor != notStartedTaskColor) {
                            ifWasChanges = true;
                            mNotStartedTaskColor = notStartedTaskColor;
                            editor.putInt(Constants.NOT_STARTED_TASK, mNotStartedTaskColor);
                            mSwipeListAdapter.setNotStartedTaskColor(mNotStartedTaskColor);
                        }
                        if (mStartedTaskColor != startedTaskColor) {
                            ifWasChanges = true;
                            mStartedTaskColor = startedTaskColor;
                            editor.putInt(Constants.STARTED_TASK, mStartedTaskColor);
                            mSwipeListAdapter.setStartedTaskColor(mStartedTaskColor);
                        }
                        if (mCompletedTaskColor != completedTaskColor) {
                            ifWasChanges = true;
                            mCompletedTaskColor = completedTaskColor;
                            editor.putInt(Constants.COMPLETED_TASK, mCompletedTaskColor);
                            mSwipeListAdapter.setCompletedTaskColor(mCompletedTaskColor);
                        }
                        //оновлюєм список, якщо було змінено кольори
                        if (ifWasChanges)
                            mSwipeListAdapter.notifyDataSetChanged();
                        //якщо було змінено максимальний час виконання завдання
                        if (mMaxRuntimeForTask != maxRuntimeForTask) {
                            ifWasChanges = true;
                            mMaxRuntimeForTask = maxRuntimeForTask;
                            editor.putInt(Constants.MAX_RUNTIME_FOR_TASK, mMaxRuntimeForTask);
                            //перезапускаєм таймер
                            setTimer();
                        }
                        //зберігаєм зміни в SharedPreferences, якщо такі були
                        if (ifWasChanges)
                            editor.apply();
                        break;
                    case RequestAccountPicker:
                        if (data != null && data.getExtras() != null) {
                            String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                            if (accountName != null) {
                                mCredential.setSelectedAccountName(accountName);
                                SharedPreferences.Editor e = getSharedPreferences(Constants.APP_SETTINGS, MODE_PRIVATE).edit();
                                e.putString(Constants.ACCOUNT_NAME, accountName);
                                e.apply();
                            }
                        }
                        break;
                    case REQUEST_AUTHORIZATION:
                        chooseAccount();
                        break;
                    case RequestGooglePlayServices:
                        isGooglePlayServicesAvailable();
                        break;
                    case RequestCodeGDResolution:
                        mGoogleApiClient.connect();
                        break;
                }
                break;
            case Activity.RESULT_CANCELED:
                switch (requestCode) {
                    case RequestAccountPicker:
                        showToast(getString(R.string.undefined_account));
                    case RequestCodeGDResolution:
                        onConnectionFailed(null);
                        break;
                }
                break;
        }
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(Constants.ARRAY_OF_TASKS, mTasks);
        outState.putSerializable(Constants.ARRAY_OF_STATISTICS, mGroups);
        outState.putStringArrayList(Constants.ARRAY_OF_NAMES_FOR_STATISTICS, mGroupsName);
        outState.putString(Constants.TAB_HOST_POS, mTabHost.getCurrentTabTag());
        outState.putBoolean(AlertBool, mIfAlertDWasShown);
        if (mShowcaseView != null && mShowcaseView.isShowing())
            outState.putInt(Constants.TUTORIAL, mTargetPos);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mOptionsMenu = menu;
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //обробка натисків на елементи toolbar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_calendar_import:
                if (isGooglePlayServicesAvailable())
                    refreshResults();
                else
                    showToast("Google Play Services required: after installing, close and relaunch this app.");
                break;
            case R.id.action_restore_data:
                if (isGDConnected()) {
                    doRestoreDBFromGD();
                } else {
                    Log.d(TAG, "NO CONNECTION");
                }
                break;
            case R.id.action_backupData:
                final DriveManager manager = new DriveManager(getGoogleApiClient(), this);
                final File dbDir = new File(getFilesDir().getParent() + "/databases/");
                final File spDir = new File(getFilesDir().getParent() + "/shared_prefs/");
                final File pxDir = new File(ImageLoader.getCachePath(this) + File.separator);
                final File[] dirsToBackup = new File[]{dbDir, spDir, pxDir};
                if (isGDConnected()) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            manager.uploadFile(dirsToBackup, true);
                        }
                    }).start();
                } else {
                    Log.d(TAG, "NO CONNECTION");
                }
                break;
            case R.id.action_show_tutorial:
                showTutorial(0);
                break;
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
                intentForSettings.putExtra(Constants.NOT_STARTED_TASK, mNotStartedTaskColor);
                intentForSettings.putExtra(Constants.STARTED_TASK, mStartedTaskColor);
                intentForSettings.putExtra(Constants.COMPLETED_TASK, mCompletedTaskColor);
                intentForSettings.putExtra(Constants.MAX_RUNTIME_FOR_TASK, mMaxRuntimeForTask);
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
                        mFloatBtn.show();
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
        if (mShowcaseView != null && mShowcaseView.isShowing()) {
            mShowcaseView.hide();
            return;
        }
        if (sTimeBackPressed + TimeForExit > System.currentTimeMillis()) {
            if (mToast != null) mToast.cancel();
            super.onBackPressed();
        } else {
            if (mToast != null)
                mToast.cancel();
            mToast = Toast.makeText(this, R.string.toastMessageIfBackPressed,
                    Toast.LENGTH_LONG);
            mToast.show();
        }
        sTimeBackPressed = System.currentTimeMillis();
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

    public List<Task> getTasks() {
        return mTasks;
    }

    public void showToast(String text) {
        if (mToast != null) mToast.cancel();
        mToast = Toast.makeText(this, text, Toast.LENGTH_LONG);
        mToast.show();
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
                //перевірка роботи анімації
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

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        if (mImportGCTasks != null)
            mImportGCTasks.unLink();
        return mImportGCTasks;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == AllTasksLoaderID)
            return new TasksCursorLoader(this, mDatabaseConnector);
        else return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mTasks.clear();
        if (loader.getId() == AllTasksLoaderID) {
            if (data.moveToFirst()) {
                int idColIndex = data.getColumnIndex(DatabaseConnector.COLUMN_ID);
                int calendarIdColIndex = data.getColumnIndex(DatabaseConnector.COLUMN_CALENDAR_ID);
                int titleColIndex = data.getColumnIndex(DatabaseConnector.COLUMN_TITLE);
                int commentColIndex = data.getColumnIndex(DatabaseConnector.COLUMN_COMMENT);
                int typeOfTaskColIndex = data.getColumnIndex(DatabaseConnector.COLUMN_TYPE_OF_TASK);
                int dateStartColIndex = data.getColumnIndex(DatabaseConnector.COLUMN_DATA_START);
                int dateStopColIndex = data.getColumnIndex(DatabaseConnector.COLUMN_DATA_STOP);
                int dateEndColIndex = data.getColumnIndex(DatabaseConnector.COLUMN_DATA_END);
                int datePauseColIndex = data.getColumnIndex(DatabaseConnector.COLUMN_DATA_PAUSE);
                int maxRuntimeColIndex = data.getColumnIndex(DatabaseConnector.COLUMN_MAX_RUNTIME);
                int pauseLengthBeforeStopColIndex = data.getColumnIndex(DatabaseConnector.COLUMN_PAUSE_LENGTH_BEFORE_STOP);
                int pauseLengthAfterStopColIndex = data.getColumnIndex(DatabaseConnector.COLUMN_PAUSE_LENGTH_AFTER_STOP);
                int avatarUriColIndex = data.getColumnIndex(DatabaseConnector.COLUMN_AVATAR_URI);
                int hasMapPointColIndex = data.getColumnIndex(DatabaseConnector.COLUMN_HAS_MAP_POINT);
                int latitudeColIndex = data.getColumnIndex(DatabaseConnector.COLUMN_LATITUDE);
                int longitudeColIndex = data.getColumnIndex(DatabaseConnector.COLUMN_LONGITUDE);
                do {
                    long id = data.getLong(idColIndex);
                    String calendar_id = data.getString(calendarIdColIndex);
                    String title = data.getString(titleColIndex);
                    String comment = data.getString(commentColIndex);
                    boolean typeOfTask = data.getInt(typeOfTaskColIndex) == 1;
                    long dateStart = data.getLong(dateStartColIndex);
                    long dateStop = data.getLong(dateStopColIndex);
                    long dateEnd = data.getLong(dateEndColIndex);
                    long datePause = data.getLong(datePauseColIndex);
                    int maxRuntime = data.getInt(maxRuntimeColIndex);
                    long pauseLengthBeforeStop = data.getLong(pauseLengthBeforeStopColIndex);
                    long pauseLengthAfterStop = data.getLong(pauseLengthAfterStopColIndex);
                    String avatarUri = data.getString(avatarUriColIndex);
                    boolean hasMapPoint = data.getInt(hasMapPointColIndex) == 1;
                    double latitude = data.getDouble(latitudeColIndex);
                    double longitude = data.getDouble(longitudeColIndex);
                    mTasks.add(new Task(id, calendar_id, title, comment, typeOfTask, avatarUri, maxRuntime,
                            dateStart, dateStop, dateEnd, datePause, pauseLengthBeforeStop, pauseLengthAfterStop, hasMapPoint, latitude, longitude));
                } while (data.moveToNext());
            }
            closeLoadProgress();
            sortTasks();
            setTimer();
        }
    }

    private void showTutorial(int pos) {
        final Target viewTarget1, viewTarget2_4, viewTarget3, viewTarget5;
        viewTarget1 = new ViewTarget(R.id.fab, this);
        viewTarget2_4 = Target.NONE;
        viewTarget3 = new ViewTarget(mToolbar.findViewById(R.id.action_sort));
        viewTarget5 = new ViewTarget(mToolbar.findViewById(R.id.action_refresh));
        if (mFloatBtn != null)
            mFloatBtn.show();
        mTargetPos = pos;
        String title = null;
        String contentText = null;
        Target target = null;
        switch (mTargetPos) {
            case 0:
                target = Target.NONE;
                contentText = getString(R.string.text_for_target_0);
                break;
            case 1:
                target = viewTarget1;
                title = getString(R.string.title_for_target_1);
                contentText = getString(R.string.text_for_target_1);
                break;
            case 2:
                target = viewTarget2_4;
                title = getString(R.string.title_for_target_2_4);
                contentText = getString(R.string.text_for_target_2);
                break;
            case 3:
                target = viewTarget3;
                title = getString(R.string.title_for_target_3);
                contentText = getString(R.string.text_for_target_3);
                break;
            case 4:
                target = viewTarget2_4;
                title = getString(R.string.title_for_target_2_4);
                contentText = getString(R.string.text_for_target_4);
                break;
            case 5:
                target = viewTarget5;
                title = getString(R.string.title_for_target_5);
                contentText = getString(R.string.text_for_target_5);
                break;
        }
        mShowcaseView = new ShowcaseView.Builder(this)
                .setTarget(target)
                .setContentTitle(title)
                .setContentText(contentText)
                .setStyle(R.style.tutorialStyle)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "tutorial onClick " + mTargetPos);
                        switch (mTargetPos) {
                            case 0:
                                mTabHost.setCurrentTabByTag("tag1");
                                mShowcaseView.setShowcase(viewTarget1, false);
                                mShowcaseView.setContentTitle(getString(R.string.title_for_target_1));
                                mShowcaseView.setContentText(getString(R.string.text_for_target_1));
                                break;
                            case 1:
                                mShowcaseView.setShowcase(viewTarget2_4, false);
                                mShowcaseView.setContentTitle(getString(R.string.title_for_target_2_4));
                                mShowcaseView.setContentText(getString(R.string.text_for_target_2));
                                break;
                            case 2:
                                mShowcaseView.setShowcase(viewTarget3, false);
                                mShowcaseView.setContentTitle(getString(R.string.title_for_target_3));
                                mShowcaseView.setContentText(getString(R.string.text_for_target_3));
                                break;
                            case 3:
                                mShowcaseView.setShowcase(viewTarget2_4, false);
                                mShowcaseView.setContentTitle(getString(R.string.title_for_target_2_4));
                                mShowcaseView.setContentText(getString(R.string.text_for_target_4));
                                break;
                            case 4:
                                mShowcaseView.setShowcase(viewTarget5, false);
                                mShowcaseView.setContentTitle(getString(R.string.title_for_target_5));
                                mShowcaseView.setContentText(getString(R.string.text_for_target_5));
                                break;
                            case 5:
                                mShowcaseView.hide();
                                SharedPreferences appSettings = getSharedPreferences(Constants.APP_SETTINGS, MODE_PRIVATE);
                                SharedPreferences.Editor edit = appSettings.edit();
                                edit.putBoolean(Constants.FIRST_START, false);
                                edit.apply();
                                break;
                        }
                        mTargetPos++;
                    }
                })
                .blockAllTouches()
                .build();
        mShowcaseView.setButtonText(getString(R.string.understand));
    }

    public void setRefreshActionButtonState(boolean refreshing) {
        /*if (mOptionsMenu != null) {
            MenuItem refreshItem = mOptionsMenu.findItem(R.id.action_refresh);
            if (refreshItem != null)
                if (refreshing)
                    refreshItem.setActionView(R.layout.refresh_progress);
                else
                    refreshItem.setActionView(null);
        }*/
        if (mOptionsMenu != null) {
            MenuItem refreshItem = mOptionsMenu.findItem(R.id.action_refresh);
            if (refreshItem != null)
                if (refreshing) {
                    LayoutInflater inflater = getLayoutInflater();//(LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    FrameLayout container = (FrameLayout) inflater.inflate(R.layout.refresh_progress_img_anim, null);
                    ImageView imageView = (ImageView) container.findViewById(R.id.refreshImg);
                    Animation rotation = AnimationUtils.loadAnimation(this, R.anim.refresh_item_rotate);
                    rotation.setRepeatCount(Animation.INFINITE);
                    imageView.startAnimation(rotation);
                    refreshItem.setActionView(container);
                } else {
                    refreshItem.getActionView().clearAnimation();
                    refreshItem.setActionView(null);
                }
        }
    }

    private void showLoadProgress() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        mLoadListProgress.setVisibility(View.VISIBLE);
    }

    private void closeLoadProgress() {
        mLoadListProgress.setVisibility(View.GONE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    public void showProgressDialog(String message) {
        if (mProgress == null)
            mProgress = new ProgressDialog(this);
        mProgress.setMessage(message);
        mProgress.show();
    }

    public void hideProgressDialog() {
        if (mProgress != null)
            mProgress.hide();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    static class TasksCursorLoader extends CursorLoader {
        DatabaseConnector db;

        public TasksCursorLoader(Context context, DatabaseConnector db) {
            super(context);
            this.db = db;
        }

        @Override
        public Cursor loadInBackground() {
           /* //пауза, щоби було видно роботу прогрес бару
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
            return db.getCursorWithAllTasks();
        }

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                    GOOGLE DRIVE                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void connectToGD() {
        Log.i(TAG, "connectToGD()");
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addScope(Drive.SCOPE_APPFOLDER) // required for App Folder
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        if (!mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();
        else
            onConnected(null);
    }

    public void disconnectGD() {
        Log.i(TAG, "disconnectGD()");
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (result == null) {
            Log.w(TAG, "GoogleApiClient connection failed: no result");
        } else {
            Log.w(TAG, "GoogleApiClient connection failed: " + result.toString());
            if (!result.hasResolution()) {
                GoogleApiAvailability.getInstance().getErrorDialog(
                        MainActivity.this, result.getErrorCode(), 0).show();
                return;
            }
            try {
                result.startResolutionForResult(this, RequestCodeGDResolution);
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, "Exception while starting resolution activity", e);
            }
        }
    }

    public boolean isGDConnected() {
        return mGoogleApiClient != null && mGoogleApiClient.isConnected();
    }

    private void doRestoreDBFromGD() {
        if (!isGDConnected()) {
            connectToGD();
            return;
        }
        showLoadProgress();

        final DriveManager manager = new DriveManager(getGoogleApiClient(), this);
        final File dbDir = new File(getFilesDir().getParent() + "/databases/");
        final File spDir = new File(getFilesDir().getParent() + "/shared_prefs/");
        final File[] dirsToRestore = new File[]{dbDir, spDir};

        manager.setOnLoadCompleteListener(new OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(final File file) {
                Log.d(TAG, "doRestoreDBFromGD() onLoadComplete() file: " + file);
            }

            @Override
            public void onLoadFailed(final File file, final String... errMSg) {
                Log.w(TAG, "doRestoreDBFromGD() onLoadFailed() file: " + file + " errMsg: " + Arrays.toString(errMSg));
                restoreFailed();
            }

            @Override
            public void onAllLoadComplete() {
                //оновлюєм користувацькі налаштування
                initSettingsFromSharedPref();
                Log.d(TAG, "doRestoreDBFromGD() onAllLoadComplete()");
                DatabaseConnector databaseConnector = new DatabaseConnector(getApplicationContext());
                databaseConnector.open();
                databaseConnector.close();

                doRestorePhotosFromGD();
            }
        });
        manager.downloadFile(dirsToRestore, true);
    }

    protected void doRestorePhotosFromGD() {
        File f = new File(ImageLoader.getCachePath(this) + File.separator);
        if (!f.exists())
            f.mkdirs();
        if (!ImageLoader.SDCardIsWritable()) {
            showToast(getString(R.string.sc_card_not_writable));
            restorePhotosFailed();
            restoreCompleted();
            return;
        }

        // makes dirs
        DriveManager manager = new DriveManager(getGoogleApiClient(), this);
        manager.setOnLoadCompleteListener(new OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(final File file) {
                Log.i(TAG, "doRestorePhotosFromGD() onLoadComplete() file: " + file);
            }

            @Override
            public void onLoadFailed(final File file, final String... errMSg) {
                Log.w(TAG, "doRestorePhotosFromGD() onLoadFailed() file: " + file + " errMsg: " + Arrays.toString(errMSg));
                restorePhotosFailed();
            }

            @Override
            public void onAllLoadComplete() {
                Log.i(TAG, "doRestorePhotosFromGD() onAllLoadComplete()");
                restoreCompleted();
            }
        });

        final File pixDir = new File(ImageLoader.getCachePath(getApplicationContext()) + File.separator);

        manager.downloadFile(new File[]{pixDir}, true);
    }

    void restoreFailed() {
        closeLoadProgress();
        Toast.makeText(this, R.string.restore_failed, Toast.LENGTH_LONG).show();
    }

    void restorePhotosFailed() {
        getLoaderManager().initLoader(AllTasksLoaderID, null, MainActivity.this);
        //оновлюєм дані для списку завдань
        getLoaderManager().getLoader(AllTasksLoaderID).forceLoad();
        Toast.makeText(this, R.string.restore_photo_failed, Toast.LENGTH_LONG).show();
    }

    protected void restoreCompleted() {
        getLoaderManager().initLoader(AllTasksLoaderID, null, MainActivity.this);
        //оновлюєм дані для списку завдань
        getLoaderManager().getLoader(AllTasksLoaderID).forceLoad();
        Toast.makeText(this, R.string.restore_completed, Toast.LENGTH_LONG).show();
    }

    public GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                   GOOGLE CALENDAR                                          //
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private void refreshResults() {
        if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else {
            if (isDeviceOnline()) {
                mImportGCTasks = new ImportGCTasks(mCredential, this, mMaxRuntimeForTask);
                mImportGCTasks.execute();
            } else {
                showToast(getString(R.string.no_internet_connection));
            }
        }
    }

    public void exportTask(Task task) {
        if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else {
            if (isDeviceOnline()) {
                if (task.getCalendar_ID() == null)
                    new ExportTask(mCredential, this, task, ExportTask.INSERT).execute();
                else
                    new ExportTask(mCredential, this, task, ExportTask.UPDATE).execute();
            } else {
                showToast(getString(R.string.no_internet_connection));
            }
        }
    }

    private void chooseAccount() {
        startActivityForResult(mCredential.newChooseAccountIntent(), RequestAccountPicker);
    }

    private boolean isDeviceOnline() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = api.isGooglePlayServicesAvailable(this);
        if (api.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
            return false;
        } else if (connectionStatusCode != ConnectionResult.SUCCESS)
            return false;
        return true;
    }

    public void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        Dialog dialog = api.getErrorDialog(MainActivity.this, connectionStatusCode, RequestGooglePlayServices);
        dialog.show();
    }
}
