package com.y_taras.scheduler.googleDrive;


import java.io.File;

import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;

class GDTask {

    public final File file;
    public final DriveFile driveFile;
    public final DriveFolder driveFolder;

    public boolean isCompleted;
    public boolean isFailed;

    public final boolean doOverwriteExistingFiles;

    public GDTask(final File file,
                  final DriveFile driveFile,
                  final boolean doOverwriteExistingFiles) {

        this.file = file;
        this.driveFile = driveFile;
        this.driveFolder = null;
        this.doOverwriteExistingFiles = doOverwriteExistingFiles;
    }

    public GDTask(final File file,
                  final DriveFolder driveFolder,
                  final boolean doOverwriteExistingFiles) {

        this.file = file;
        this.driveFile = null;
        this.driveFolder = driveFolder;
        this.doOverwriteExistingFiles = doOverwriteExistingFiles;
    }

    @Override
    public String toString() {
        return "GDTask [file=" + file + ", driveFile=" + driveFile + ", driveFolder=" + driveFolder + "]";
    }

}
