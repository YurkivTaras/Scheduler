package com.y_taras.scheduler.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileInfo;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;
import com.dropbox.sync.android.DbxSyncStatus;
import com.google.api.client.util.IOUtils;
import com.y_taras.scheduler.R;

import com.y_taras.scheduler.helper.ImageLoader;
import com.y_taras.scheduler.other.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import yuku.ambilwarna.AmbilWarnaDialog;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String AppKey = "z71er3sdgp1sxtv";
    private static final String AppSecret = "z71er3sdgp1sxtv";
    private static final int RequestCodeLinkToDBX = 1001;
    private static final String TAG = "=============";
    private static int defaultNotStartedTaskColor;
    private static int defaultStartedTaskColor;
    private static int defaultCompletedTaskColor;

    private int mCompletedTaskColor;
    private int mStartedTaskColor;
    private int mNotStartedTaskColor;

    private static final int NotStartedTask = 1;
    private static final int StartedTask = 2;
    private static final int CompletedTask = 3;

    private Toast mToast;

    private Button mCompletedTaskBtn;
    private Button mStartedTaskBtn;
    private Button mNotStartedTaskBtn;
    private Button mDRBBackupBtn;
    private Button mDRBRestoreBtn;
    private EditText mMaxRuntimeEditTxt;

    private DbxAccountManager mDbxAcctMgr;
    private boolean mIsSyncDRXComplete = true;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        initUI(savedInstanceState);
    }

    private void initUI(Bundle savedInstanceState) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarForSettingsActivity);
        toolbar.setTitle(R.string.settingsToolbarTitle);
        setSupportActionBar(toolbar);

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }
        defaultNotStartedTaskColor = ContextCompat.getColor(this, R.color.not_started_task);
        defaultStartedTaskColor = ContextCompat.getColor(this, R.color.started_task);
        defaultCompletedTaskColor = ContextCompat.getColor(this, R.color.completed_task);

        int mMaxRuntimeForTask;
        if (savedInstanceState == null) {
            Intent intent = getIntent();
            mNotStartedTaskColor = intent.getIntExtra(Constants.NOT_STARTED_TASK, defaultNotStartedTaskColor);
            mStartedTaskColor = intent.getIntExtra(Constants.STARTED_TASK, defaultStartedTaskColor);
            mCompletedTaskColor = intent.getIntExtra(Constants.COMPLETED_TASK, defaultCompletedTaskColor);
            mMaxRuntimeForTask = intent.getIntExtra(Constants.MAX_RUNTIME_FOR_TASK, 60);
        } else {
            mNotStartedTaskColor = savedInstanceState.getInt(Constants.NOT_STARTED_TASK);
            mStartedTaskColor = savedInstanceState.getInt(Constants.STARTED_TASK);
            mCompletedTaskColor = savedInstanceState.getInt(Constants.COMPLETED_TASK);
            mMaxRuntimeForTask = savedInstanceState.getInt(Constants.MAX_RUNTIME_FOR_TASK);
        }

        mNotStartedTaskBtn = (Button) findViewById(R.id.btnNotStartedTask);
        mNotStartedTaskBtn.setOnClickListener(this);
        mStartedTaskBtn = (Button) findViewById(R.id.btnStartedTask);
        mStartedTaskBtn.setOnClickListener(this);
        mCompletedTaskBtn = (Button) findViewById(R.id.btnCompletedTask);
        mCompletedTaskBtn.setOnClickListener(this);
        commitColors();

        mDRBBackupBtn = (Button) findViewById(R.id.DRBBackup);
        mDRBBackupBtn.setOnClickListener(this);
        mDRBRestoreBtn = (Button) findViewById(R.id.DRBRestore);
        mDRBRestoreBtn.setOnClickListener(this);

        mMaxRuntimeEditTxt = (EditText) findViewById(R.id.editTxtMaxRuntime);
        mMaxRuntimeEditTxt.setText(String.format("%d", mMaxRuntimeForTask));

        mDbxAcctMgr = DbxAccountManager.getInstance(getApplicationContext(), AppKey, AppSecret);
    }

    public void getColor(int color, final int stateOfTask) {
        AmbilWarnaDialog dialog = new AmbilWarnaDialog(this, color, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onCancel(AmbilWarnaDialog dialog) {
            }

            @Override
            public void onOk(AmbilWarnaDialog dialog, int newColor) {
                switch (stateOfTask) {
                    case NotStartedTask:
                        mNotStartedTaskColor = newColor;
                        break;
                    case StartedTask:
                        mStartedTaskColor = newColor;
                        break;
                    case CompletedTask:
                        mCompletedTaskColor = newColor;
                        break;
                }
                commitColors();
            }
        });
        dialog.show();
    }


    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(Constants.NOT_STARTED_TASK, mNotStartedTaskColor);
        outState.putInt(Constants.STARTED_TASK, mStartedTaskColor);
        outState.putInt(Constants.COMPLETED_TASK, mCompletedTaskColor);
        outState.putInt(Constants.MAX_RUNTIME_FOR_TASK, Integer.parseInt(mMaxRuntimeEditTxt.getText().toString()));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.action_reset_colors:
                mNotStartedTaskColor = defaultNotStartedTaskColor;
                mStartedTaskColor = defaultStartedTaskColor;
                mCompletedTaskColor = defaultCompletedTaskColor;
                break;
        }
        commitColors();
        return super.onOptionsItemSelected(item);
    }

    private void commitColors() {
        mNotStartedTaskBtn.setBackgroundColor(mNotStartedTaskColor);
        mStartedTaskBtn.setBackgroundColor(mStartedTaskColor);
        mCompletedTaskBtn.setBackgroundColor(mCompletedTaskColor);
    }


    @Override
    public void onBackPressed() {
        String maxRuntime = mMaxRuntimeEditTxt.getText().toString();
        if (maxRuntime.length() == 0) {
            if (mToast != null)
                mToast.cancel();
            mToast = Toast.makeText(this, R.string.error_edit_txt_for_maxRuntime, Toast.LENGTH_SHORT);
            mToast.show();
            return;
        }
        Intent intent = new Intent();
        intent.putExtra(Constants.NOT_STARTED_TASK, mNotStartedTaskColor);
        intent.putExtra(Constants.STARTED_TASK, mStartedTaskColor);
        intent.putExtra(Constants.COMPLETED_TASK, mCompletedTaskColor);
        intent.putExtra(Constants.MAX_RUNTIME_FOR_TASK, Integer.parseInt(maxRuntime));
        setResult(RESULT_OK, intent);
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RequestCodeLinkToDBX) {
            if (resultCode == Activity.RESULT_OK) {

            } else {
                Toast.makeText(SettingsActivity.this, "Link to Dropbox failed or was cancelled.", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnNotStartedTask:
                getColor(mNotStartedTaskColor, NotStartedTask);
                break;
            case R.id.btnStartedTask:
                getColor(mNotStartedTaskColor, StartedTask);
                break;
            case R.id.btnCompletedTask:
                getColor(mNotStartedTaskColor, CompletedTask);
                break;
            case R.id.DRBBackup:
                if (mDbxAcctMgr.hasLinkedAccount()) {
                    if (isDeviceOnline()) {
                        mIsSyncDRXComplete = false;
                        mDRBBackupBtn.setEnabled(false);
                        File localDBDir = new File(getFilesDir().getParent() + "/databases/");
                        File localSPDir = new File(getFilesDir().getParent() + "/shared_prefs/");
                        File localPXDir = new File(ImageLoader.getCachePath(this) + File.separator);
                        try {
                            // Створюєм DbxFileSystem для доступу до файлів.
                            DbxFileSystem dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());

                            DbxPath drbDBDir = new DbxPath(DbxPath.ROOT, "databases");
                            List<DbxFileInfo> infos;
                            if (dbxFs.exists(drbDBDir)) {
                                infos = dbxFs.listFolder(drbDBDir);
                                for (DbxFileInfo info : infos)
                                    dbxFs.delete(info.path);
                            } else
                                dbxFs.createFolder(drbDBDir);
                            DbxPath drbSPDir = new DbxPath(DbxPath.ROOT, "shared_prefs");
                            if (dbxFs.exists(drbSPDir)) {
                                infos = dbxFs.listFolder(drbSPDir);
                                for (DbxFileInfo info : infos)
                                    dbxFs.delete(info.path);
                            } else
                                dbxFs.createFolder(drbSPDir);
                            DbxPath drbPXDir = new DbxPath(DbxPath.ROOT, ImageLoader.AVATAR_FOLDER);
                            if (dbxFs.exists(drbPXDir)) {
                                infos = dbxFs.listFolder(drbPXDir);
                                for (DbxFileInfo info : infos)
                                    dbxFs.delete(info.path);
                            } else
                                dbxFs.createFolder(drbPXDir);

                            addFiles(drbDBDir, localDBDir, dbxFs);
                            addFiles(drbSPDir, localSPDir, dbxFs);
                            if (localPXDir.exists())
                                addFiles(drbPXDir, localPXDir, dbxFs);
                            Toast.makeText(SettingsActivity.this, "Запит для резервного копіювання відправлено", Toast.LENGTH_SHORT).show();
                            dbxFs.addSyncStatusListener(new DbxFileSystem.SyncStatusListener() {
                                @Override
                                public void onSyncStatusChange(DbxFileSystem fs) {
                                    try {
                                        DbxSyncStatus fsStatus = fs.getSyncStatus();
                                        if (!fsStatus.upload.inProgress) {
                                            mDRBBackupBtn.setEnabled(true);
                                            mIsSyncDRXComplete = true;
                                            Toast.makeText(SettingsActivity.this, "Синхринізацію завершено", Toast.LENGTH_SHORT).show();
                                        }
                                    } catch (DbxException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        } catch (IOException e) {
                            Toast.makeText(SettingsActivity.this, "Розервне копіювання не вдалось", Toast.LENGTH_SHORT).show();
                            mDRBBackupBtn.setEnabled(false);
                            mIsSyncDRXComplete = true;
                        }
                    } else {
                        Toast.makeText(SettingsActivity.this, getString(R.string.no_internet_connection), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    mDbxAcctMgr.startLink((Activity) this, RequestCodeLinkToDBX);
                }
                break;
            case R.id.DRBRestore:
                if (isDeviceOnline()) {
                    if (!mIsSyncDRXComplete) {
                        Toast.makeText(SettingsActivity.this, "Дочекайтеся завершення резервного копіювання", Toast.LENGTH_SHORT).show();
                    } else if (mDbxAcctMgr.hasLinkedAccount()) {

                        File localDBDir = new File(getFilesDir().getParent() + "/databases/");
                        File localSPDir = new File(getFilesDir().getParent() + "/shared_prefs/");
                        File localPXDir = new File(ImageLoader.getCachePath(this) + File.separator);
                        try {
                            /// Створюєм DbxFileSystem для доступу до файлів.
                            DbxFileSystem dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
                            List<DbxFileInfo> infos;
                            DbxPath drbDBDir = new DbxPath(DbxPath.ROOT, "databases");
                            if (dbxFs.isFolder(drbDBDir)) {
                                infos = dbxFs.listFolder(drbDBDir);
                                if (!infos.isEmpty())
                                    restoreFiles(localDBDir, infos, dbxFs);
                            }
                            DbxPath drbSPDir = new DbxPath(DbxPath.ROOT, "shared_prefs");
                            if (dbxFs.isFolder(drbSPDir)) {
                                infos = dbxFs.listFolder(drbSPDir);
                                if (!infos.isEmpty()) {
                                    getSharedPreferences(Constants.APP_SETTINGS, MODE_PRIVATE).edit().clear().commit();
                                    restoreFiles(localSPDir, infos, dbxFs);
                                }
                            }
                            DbxPath drbPXDir = new DbxPath(DbxPath.ROOT, ImageLoader.AVATAR_FOLDER);
                            if (dbxFs.isFolder(drbPXDir)) {
                                infos = dbxFs.listFolder(drbPXDir);
                                if (!infos.isEmpty())
                                    restoreFiles(localPXDir, infos, dbxFs);
                            }
                            ProcessPhoenix.triggerRebirth(getApplicationContext());
                        } catch (IOException e) {
                        }
                    } else {
                        mDbxAcctMgr.startLink((Activity) this, RequestCodeLinkToDBX);
                    }
                } else {
                    Toast.makeText(SettingsActivity.this, getString(R.string.no_internet_connection), Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private boolean isDeviceOnline() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private void addFiles(DbxPath rootFolder, File dir, DbxFileSystem dbxFs) throws IOException {
        DbxFile dbxFile;
        for (File localeFile : dir.listFiles()) {
            Log.d(TAG, localeFile.getAbsolutePath());
            dbxFile = dbxFs.create(new DbxPath(rootFolder, localeFile.getName()));
            dbxFile.writeFromExistingFile(localeFile, false);
            dbxFile.close();
        }
    }

    private void restoreFiles(File dirToRestore, List<DbxFileInfo> infos, DbxFileSystem dbxFs) throws IOException {
        File[] files = dirToRestore.listFiles();
        if (files == null)
            return;
        for (File f : files)
            f.delete();
        for (DbxFileInfo info : infos) {
            if (dbxFs.isFile(info.path)) {
                DbxFile drxFile = dbxFs.open(info.path);

                InputStream inputStream = drxFile.getReadStream();
                Log.d(TAG, "new FILE" + dirToRestore.getAbsolutePath() + File.separator + info.path.getName());
                File localFile = new File(dirToRestore.getAbsolutePath() + File.separator + info.path.getName());
                OutputStream outputStream = new FileOutputStream(localFile);
                IOUtils.copy(inputStream, outputStream);
                drxFile.close();
            }
        }
    }
}