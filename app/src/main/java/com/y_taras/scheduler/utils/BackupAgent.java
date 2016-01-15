package com.y_taras.scheduler.utils;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupManager;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.Context;

import com.y_taras.scheduler.other.StringKeys;

public class BackupAgent extends BackupAgentHelper {
    static final String PREFS_BACKUP_KEY = "prefs";

    @Override
    public void onCreate() {
        FileBackupHelper fileBackupHelper = new FileBackupHelper(this, "../databases/" + DatabaseConnector.DATABASE_NAME);
        addHelper(DatabaseConnector.DATABASE_NAME, fileBackupHelper);

        SharedPreferencesBackupHelper helper = new SharedPreferencesBackupHelper(this, StringKeys.APP_SETTINGS);
        addHelper(PREFS_BACKUP_KEY, helper);
    }

    public static void requestBackup(Context context) {
        BackupManager bm = new BackupManager(context);
        bm.dataChanged();
    }
}