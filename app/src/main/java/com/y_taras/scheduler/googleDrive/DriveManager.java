package com.y_taras.scheduler.googleDrive;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.MetadataBufferResult;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFile.DownloadProgressListener;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveFolder.DriveFolderResult;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;

public class DriveManager implements OnLoadCompleteListener, DownloadProgressListener {

    public static final String TAG = "DriveManager";

    public static final int MODE_DOWNLOAD_SINLGE_GD_FOLDER = 0;
    public static final int MODE_UPLOAD_SINLGE_GD_FOLDER = 1;
    public static final int MODE_DOWNLOAD_FILE_BY_FILE = 2;
    public static final int MODE_UPLOAD_FILE_BY_FILE = 3;
    public static final int MODE_DOWNLOAD_FOLDER = 4;
    public static final int MODE_UPLOAD_FOLDER = 5;

    private int mode;
    private String[] driveFolderParentsList;
    private int driveFolderParentsListIndex;
    private GoogleApiClient googleApiClient;

    private FileLoader fileLoader;

    // for file by file mode
    private int filesIndex;
    private int filesCount;
    private File[] files;
    private String lastDrivePath;
    private DriveFolder lastDriveFolder;

    private GDTask[] tasks;
    private int tasksIndex;
    private int tasksCount;
    private ArrayList<GDTask> folderTasks;

    private boolean doDisconnectWhenDone;

    private boolean doOverwriteExistingFiles = true;

    private OnLoadCompleteListener onLoadCompleteListenerOuter;
    private DownloadProgressListener downloadProgressListener;

    private Activity activityToLeak;

    public DriveManager(final GoogleApiClient googleApiClient, Activity activity) {
        this.googleApiClient = googleApiClient;
        this.activityToLeak = activity;
    }

    public void setOnLoadCompleteListener(final OnLoadCompleteListener listener) {
        onLoadCompleteListenerOuter = listener;
    }

    //////////////////////////////////////////////////////////////////////////////
    //
    // DOWNLOAD PART
    //
    //////////////////////////////////////////////////////////////////////////////

    public void setDownloadProgressListener(final DownloadProgressListener listener) {
        downloadProgressListener = listener;
    }

    /**
     * Downloads files from given GD's drivePath and save them to given directory
     * and disconnect googleApiClient when done
     * ie download from GD's folder 'photos' to device folder '/mnt/sdcard/'
     * so if on GD its 'photos/Android/DCIM' that would be downloaded to
     * device to '/mnt/sdcard/Android/DCIM'
     *
     * @param directoryToStore
     * @param drivePath
     */
    public void downloadFolderAndDisconnect(final File directoryToStore, final File drivePath, final boolean doOverwriteExistingFiles) {
        doDisconnectWhenDone = true;
        downloadFolder(directoryToStore, drivePath, doOverwriteExistingFiles);
    }

    /**
     * Downloads files from given GD's drivePath and save them to given directory
     * ie download from GD's folder 'photos' to device folder '/mnt/sdcard/'
     * so if on GD its 'photos/Android/DCIM' that would be downloaded to
     * device to '/mnt/sdcard/Android/DCIM'
     *
     * @param directoryToStore
     * @param drivePath
     */
    public void downloadFolder(final File directoryToStore, final File drivePath, final boolean doOverwriteExistingFiles) {
        if (directoryToStore == null || !directoryToStore.exists() || !directoryToStore.isDirectory()) {
            onLoadFailed(directoryToStore, "downloadFile(): directoryToStore doesn't exists or is not a directory");
            return;
        }
        mode = MODE_DOWNLOAD_FOLDER;
        this.doOverwriteExistingFiles = doOverwriteExistingFiles;

        // ????????? ??? ????????? ????? drivePath ??????????
        driveFolderParentsList = getParentFoldersListFromPath(drivePath);
        if (driveFolderParentsList == null)
            return;
        driveFolderParentsListIndex = 0;
        Log.d(TAG, "downloadFolder() DriveFolder: " + drivePath + " -> checkPathOnDrive()");

        checkPathOnDrive(Drive.DriveApi.getAppFolder(googleApiClient), directoryToStore);
    }

    /**
     * Downloads single file from given DriveFolder
     * or downloads all existing files from DriveFolder
     * into given file (which means file is a directory)
     *
     * @param file
     * @param folder
     */
    private void downloadFile(final File file, final DriveFolder folder) {
        if (file == null || TextUtils.isEmpty(file.getName()) || folder == null) {
            onLoadFailed(file, "downloadFile() null or empty filename in parameters");
            return;
        }

        mode = MODE_DOWNLOAD_SINLGE_GD_FOLDER;

        folder.listChildren(googleApiClient).setResultCallback(
                new ResultCallback<MetadataBufferResult>() {
                    @Override
                    public void onResult(final MetadataBufferResult result) {
                        handleCheckPathOnDriveForFileResult(file, folder, result);
                    }
                });
    }

    /**
     * @hide
     */
    protected final void handleCheckPathOnDriveForFileResult(final File file,
                                                             final DriveFolder folder,
                                                             final MetadataBufferResult bufferResult) {
        // prevent forced hack-call
        if (file == null || bufferResult == null) {
            Log.d(TAG, "handleCheckPathOnDriveForFileResult() null in parameters");
            onLoadFailed(file, "handleCheckPathOnDriveForFileResult() null in parameters");
            return;
        }

        if (!bufferResult.getStatus().isSuccess()) {
            Log.d(TAG, "handleCheckPathOnDriveForFileResult() failed");
            onLoadFailed(file, "folder.listChildren() failed: " + bufferResult.getStatus().getStatusCode());
            return;
        }

        final String targetName = file.getName(); //folderList[folderListIndex];
        final MetadataBuffer metadataBuffer = bufferResult.getMetadataBuffer();
        final int metadataCount = metadataBuffer.getCount();
        Metadata metaData = null;
        for (int i = 0; i < metadataCount; i++) {
            metaData = metadataBuffer.get(i);
            if (targetName.equals(metaData.getTitle())) {
                if (metaData.isFolder()) {
                    if (file.exists() && file.isDirectory()) {
                        Log.d(TAG, "handleCheckPathOnDriveForFileResult() folder: <" + targetName + "> does exist and its last, metaData.isFolder() -> getFilesOnDrive()");
                        folderTasks = new ArrayList<GDTask>();
                        final GDTask task = new GDTask(file, Drive.DriveApi.getFolder(googleApiClient, metaData.getDriveId()), doOverwriteExistingFiles);
                        folderTasks.add(task);
                        getDriveFolderFiles(task);
                    } else
                        break;
                } else {
                    Log.d(TAG, "handleCheckPathOnDriveForFileResult() folder: <" + targetName + "> does exist and its last, metaData.isFile() -> start FileLoader()");
                    fileLoader = FileLoader.downloadFile(googleApiClient, this, this, new File[]{file}, folder, doOverwriteExistingFiles);
                }
                metadataBuffer.close();
                return;
            }
        }
        metadataBuffer.close();
        // path !not found
        onLoadFailed(file, "file not found in DriveFolder or file does not exists as directory");
        return;
    }

    private void getDriveFolderFiles(final GDTask folderTask) {
        folderTask.driveFolder.listChildren(googleApiClient).setResultCallback(
                new ResultCallback<MetadataBufferResult>() {
                    @Override
                    public void onResult(final MetadataBufferResult result) {
                        handleDriveFolderFilesList(folderTask, result);
                    }
                });
    }

    /**
     * hide
     */
    protected void handleDriveFolderFilesList(final GDTask folderTask, final MetadataBufferResult bufferResult) {
        // ????? ??????? ????, ?? ???????? ?????? ??? ?????? ???????
        folderTasks.remove(folderTask);
        // ???????? ?? ???? ????-???? ??? ?????????? ??????
        final File directory = folderTask.file;
        final LinkedList<GDTask> downloadTasks = new LinkedList<GDTask>();
        final MetadataBuffer metadataBuffer = bufferResult.getMetadataBuffer();
        final int metadataCount = metadataBuffer.getCount();
        Metadata metaData = null;
        GDTask newTask = null;
        File file = null;
        DriveFile driveFile = null;
        //DriveFolder driveFolder = null;
        for (int i = 0; i < metadataCount; i++) {
            metaData = metadataBuffer.get(i);
            Log.d(TAG, "handleDriveFolderFilesList() Drive object: " + metaData.getTitle() + " added to files[]");
            file = new File(directory.getAbsoluteFile() + File.separator + metaData.getTitle());
            if (metaData.isFolder()) {
                newTask = new GDTask(file, Drive.DriveApi.getFolder(googleApiClient, metaData.getDriveId()), doOverwriteExistingFiles);
                folderTasks.add(newTask);
                continue;
            } else {
                driveFile = Drive.DriveApi.getFile(googleApiClient, metaData.getDriveId());
                newTask = new GDTask(file, driveFile, doOverwriteExistingFiles);
                downloadTasks.add(newTask);
            }
        }
//		Log.i(TAG,"handleFilesList() closed MetadataBufferResult: "+bufferResult.getMetadataBuffer());
        metadataBuffer.close();

        if (tasks != null && tasks.length > 0)
            for (int i = tasksIndex + 1; i < tasksCount; i++)
                downloadTasks.add(tasks[i]);

        // ???????? ?????? ??????????? ??????
        tasks = downloadTasks.toArray(new GDTask[downloadTasks.size()]);

        // ???? ?????? ????? ????, ?????? ?? ????????? ??????? ??? ??????
        if (folderTasks.size() == 0)
            downloadTasks();
        else // ? ????? ?????? - ?????????? ??????? ??????
            getDriveFolderFiles(folderTasks.get(0));
    }

    private void downloadTasks() {
        Log.d(TAG, "downloadTasks() tasks count: " + tasks.length);
        Log.d(TAG, "downloadTasks() tasks: " + Arrays.toString(tasks));
        //????????? ????? ?? ????????
        fileLoader = FileLoader.downloadFile(googleApiClient, this, this, tasks, doOverwriteExistingFiles);
    }

    public void downloadFileAndDisconnect(final File[] filesToDownload, final boolean doOverwriteExistingFiles) {
        doDisconnectWhenDone = true;
        downloadFile(filesToDownload, doOverwriteExistingFiles);
    }

    /**
     * Downloads given files[] assuming that on GD they
     * all stored with the same path. Like if file has path
     * on device /mnt/sdcard/dcim/camera/photo1.jpg then
     * on GD this file has the same path from GD's root.
     *
     * @param filesToDownload
     * @param doOverwriteExistingFiles
     */
    public void downloadFile(final File[] filesToDownload, final boolean doOverwriteExistingFiles) {
        this.doOverwriteExistingFiles = doOverwriteExistingFiles;
        files = filesToDownload;
        Log.d(TAG, "downloadFile() allFiles: " + Arrays.toString(files));
        filesCount = files.length;
        filesIndex = -1;
        tasks = null;
        tasksIndex = 0;
        tasksCount = 0;
        donwloadTasksList = new LinkedList<GDTask>();
        mode = MODE_DOWNLOAD_FILE_BY_FILE;
        continueDownloadFilesListCompiling();
    }

    private LinkedList<GDTask> donwloadTasksList;

    private void continueDownloadFilesListCompiling() {
        filesIndex++;

        if (filesIndex == filesCount) {
            tasks = donwloadTasksList.toArray(new GDTask[donwloadTasksList.size()]);
            donwloadTasksList.clear();
            donwloadTasksList = null;
            downloadTasks();
            return;
        }

        final File nextFile = files[filesIndex];
        driveFolderParentsList = getParentFoldersListFromPath(nextFile);
        if (driveFolderParentsList == null)
            return;
        Log.d(TAG, "continueDownloadFilesListCompiling() nextFile: " + nextFile + " -> checkPathOnDrive()");
        checkPathOnDrive(Drive.DriveApi.getAppFolder(googleApiClient), nextFile);
    }

    public void downloadFileAndDisconnect(final File[] files, final File drivePath, final boolean doOverwriteExistingFiles) {
        doDisconnectWhenDone = true;
        downloadFile(files, drivePath, doOverwriteExistingFiles);
    }

    /**
     * Download already known (by names) files from given drive's folder
     * or download single driveFile to single file or several files
     *
     * @param files
     * @param drivePath - GD folder or GD file
     */
    public void downloadFile(final File[] files, final File drivePath, final boolean doOverwriteExistingFiles) {

        if (files == null || files.length == 0 || drivePath == null) {
            Log.e(TAG, "downloadFile() invalid parameters");
            return;
        }

        // TODO drive root folder ?
        this.driveFolderParentsList = getParentFoldersListFromPath(drivePath);
        if (this.driveFolderParentsList == null) {
            Log.e(TAG, "downloadFile() drivePath parameter");
            return;
        }

        this.files = files;
        this.filesIndex = 0;
        this.filesCount = files.length;
        this.mode = MODE_DOWNLOAD_SINLGE_GD_FOLDER;
        this.doOverwriteExistingFiles = doOverwriteExistingFiles;

        checkPathOnDrive(Drive.DriveApi.getAppFolder(googleApiClient), null);
    }


    private void checkPathOnDrive(final DriveFolder folder, final File file) {
        folder.listChildren(googleApiClient).setResultCallback(
                new ResultCallback<MetadataBufferResult>() {
                    @Override
                    public void onResult(final MetadataBufferResult result) {
                        handleCheckListChildrenResult(folder, file, result);
                    }
                });
    }

    /**
     * @hide
     */
    protected final void handleCheckListChildrenResult(final DriveFolder folder,
                                                       final File file,
                                                       final MetadataBufferResult bufferResult) {

//		Log.i(TAG,"handleCheckListChildrenResult() got MetadataBufferResult: "+bufferResult.getMetadataBuffer());

        // prevent forced hack-call
        if (driveFolderParentsList == null || folder == null || bufferResult == null) {
            Log.d(TAG, "handleCheckListChildrenResult() null in parameters");
            return;
        }

        if (!bufferResult.getStatus().isSuccess()) {
            Log.d(TAG, "handleCheckListChildrenResult() failed");
            return;
        }

        final String targetFolderName = driveFolderParentsList[driveFolderParentsListIndex];
        final MetadataBuffer metadataBuffer = bufferResult.getMetadataBuffer();
        final int metadataCount = metadataBuffer.getCount();
        Metadata metaData = null;
        boolean isFound = false;
        for (int i = 0; i < metadataCount; i++) {

            metaData = metadataBuffer.get(i);

            if (!targetFolderName.equals(metaData.getTitle()))
                continue;

            isFound = true;
            if (driveFolderParentsListIndex < driveFolderParentsList.length - 1) {
                //Log.d(TAG, "handleCheckListChildrenResult() folder: <"+ targetFolder + "> does exist but its not last by index");
                // path is not checked completely so lets continue
                driveFolderParentsListIndex++;
                checkPathOnDrive(Drive.DriveApi.getFolder(googleApiClient, metaData.getDriveId()), file);
            } else {
                driveFolderParentsListIndex = 0;
                if (metaData.isFolder()) {
                    Log.d(TAG, "handleCheckListChildrenResult() folder: <" + targetFolderName + "> does exist and its last, metaData.isFolder() -> getFilesOnDrive()");
                    if (mode == MODE_DOWNLOAD_FOLDER)
                        downloadFile(file, folder);
                    else if (mode == MODE_DOWNLOAD_SINLGE_GD_FOLDER)
                        //startFileLoader(files, folder);
                        compileTasksForFilesInDriveFolder(metaData.getDriveId(), metaData.isFolder());
                    else //if (mode == MODE_DOWNLOAD_FILE_BY_FILE)
                        getDriveFolderFiles(Drive.DriveApi.getFolder(googleApiClient, metaData.getDriveId()), file);
                } else {
                    if (mode == MODE_DOWNLOAD_FOLDER) {
                        Log.d(TAG, "handleCheckListChildrenResult() folder: <" + targetFolderName + "> does exist and its last, metaData.isFile() -> downloadFile()");
                        downloadFile(file, folder);
                    } else if (mode == MODE_DOWNLOAD_SINLGE_GD_FOLDER) {
                        Log.d(TAG, "handleCheckListChildrenResult() folder: <" + targetFolderName + "> does exist and its last, metaData.isFile() -> compileTasksForFilesInDriveFolder()");
                        //startFileLoader(files, folder);
                        compileTasksForFilesInDriveFolder(metaData.getDriveId(), metaData.isFolder());
                    } else {//if (mode == MODE_DOWNLOAD_FILE_BY_FILE)
                        Log.d(TAG, "handleCheckListChildrenResult() folder: <" + targetFolderName + "> does exist and its last, metaData.isFile() -> donwloadTasksList.add() -> continueDownloadFilesListCompiling()");
                        donwloadTasksList.add(new GDTask(file, Drive.DriveApi.getFile(googleApiClient, metaData.getDriveId()), doOverwriteExistingFiles));
                        continueDownloadFilesListCompiling();
                    }
                }
            }
            break;
        }

        metadataBuffer.close();

        if (!isFound) {
            // path !not found
            if (mode == MODE_DOWNLOAD_FOLDER) {
                onLoadFailed(file, "given DriveFolder doesn't exist");
            } else {//if (mode == MODE_DOWNLOAD_FILE_BY_FILE)
                this.onLoadFailed(files[filesIndex], "file not found on GD");
                continueDownloadFilesListCompiling();
            }
        }
    }

    private void compileTasksForFilesInDriveFolder(final DriveId driveId, final boolean isDriveFolderReallyFolder) {
        // mode==MODE_DOWNLOAD_SINLGE_GD_FOLDER
        // ? ?????? ?????? ?????????????? ??? ??? ?????????? ?? ??????????
        // ????? ????????? ? ??????, ??????????? ?? ?????????????, driveFolder-?
        // ??????? ? ???, ??? ? ?????? ?????? ????? ???? ? ????? ????
        // ? driveFolder ?????? ????? ????????? ??????
        if (isDriveFolderReallyFolder) {
            //TODO ????????? ??? ?????
            fileLoader = FileLoader.downloadFile(googleApiClient, this, this, files, Drive.DriveApi.getFolder(googleApiClient, driveId), doOverwriteExistingFiles);
        } else {
            // driveFolder ???????? ??????. ? ?????? ?????? ????
            // ?? ???????? ??????? ?????? ??????, ?? ??????????
            // ???? ????????? ???? driveFile ? ????????? ???? (files[])
            // ?? ?? ?????? ?????? ?????????? 1 ???? - 1 driveFile
            donwloadTasksList = new LinkedList<GDTask>();
            final DriveFile friveFile = Drive.DriveApi.getFile(googleApiClient, driveId);
            for (int i = 0; i < filesCount; i++)
                donwloadTasksList.add(new GDTask(files[i], friveFile, doOverwriteExistingFiles));
            tasks = donwloadTasksList.toArray(new GDTask[donwloadTasksList.size()]);
            donwloadTasksList.clear();
            donwloadTasksList = null;
            downloadTasks();
        }
    }

    private void getDriveFolderFiles(final DriveFolder folder, final File directory) {
        folder.listChildren(googleApiClient).setResultCallback(
                new ResultCallback<MetadataBufferResult>() {
                    @Override
                    public void onResult(final MetadataBufferResult result) {
                        handleDriveFolderFilesList(result, folder, directory);
                    }
                });
    }

    /**
     * hide
     */
    protected void handleDriveFolderFilesList(final MetadataBufferResult bufferResult, final DriveFolder driveFolder, final File directory) {
//		Log.i(TAG,"handleFilesList() got MetadataBufferResult: "+bufferResult.getMetadataBuffer());

        LinkedList<File> downloadfiles = new LinkedList<File>();
        final MetadataBuffer metadataBuffer = bufferResult.getMetadataBuffer();
        final int metadataCount = metadataBuffer.getCount();
        Metadata metaData = null;
        for (int i = 0; i < metadataCount; i++) {
            metaData = metadataBuffer.get(i);
            Log.d(TAG, "handleFilesList() Drive object: " + metaData.getTitle() + " added to files[]");
            driveFolderParentsListIndex = 0;
            downloadfiles.add(new File(directory.getAbsoluteFile() + File.separator + metaData.getTitle()));
        }
//		Log.i(TAG,"handleFilesList() closed MetadataBufferResult: "+bufferResult.getMetadataBuffer());
        metadataBuffer.close();

        // ??????? ? ???????? ??? ????????? ????? ??? ?????
        // ??? ???? ?? ??????? ??????
        for (int i = filesIndex + 1; i < filesCount; i++)
            downloadfiles.add(files[i]);

        // ???????? ?????? ??????????? ??????
        files = downloadfiles.toArray(new File[downloadfiles.size()]);
        filesCount = files.length;
        Log.d(TAG, "handleFilesList() renewed files[]: " + Arrays.toString(files));
        // ?.?. ??????? ?????? ????????? ????? ?? GD, ?????? ?????? ??????? ?? ????????
        // ? ?? ???? ????? ???? ???? ?????, ?? ??? ??? ?????????? (?????? ? tasks)
        // ?? ? ?????? ????? ????? ??????????? ?????? ?????, ????????? ?? ??????? (?? ???????)
        // ?.?. ???????????? ????? ????? ? ?????? ?? ????????, ????? ?????????? ???? ? 0 (-1++)
        // ?.?. ? ??????? ????? ? ?????? ?????? ???? GD-?????
        filesIndex = -1;
        //lastDriveFolder = driveFolder;
        continueDownloadFilesListCompiling();
        //startFileLoader(files, driveFolder);
    }

    //////////////////////////////////////////////////////////////////////////////
    //
    // UPLOAD PART
    //
    //////////////////////////////////////////////////////////////////////////////

    public void setUploadProgressListener(final DownloadProgressListener listener) {
        downloadProgressListener = listener;
    }

    /**
     * Downloads files from given GD's drivePath and save them to given directory
     * and disconnect googleApiClient when done
     * ie download from GD's folder 'photos' to device folder '/mnt/sdcard/'
     * so if on GD its 'photos/Android/DCIM' that would be downloaded to
     * device to '/mnt/sdcard/Android/DCIM'
     *
     * @param directoryToUpload
     * @param drivePathToStore
     */
    public void uploadFolderAndDisconnect(final File directoryToUpload, final File drivePathToStore, final boolean doOverwriteExistingFiles) {
        doDisconnectWhenDone = true;
        uploadFolder(directoryToUpload, drivePathToStore, doOverwriteExistingFiles);
    }

    /**
     * Downloads files from given GD's drivePath and save them to given directory
     * ie download from GD's folder 'photos' to device folder '/mnt/sdcard/'
     * so if on GD its 'photos/Android/DCIM' that would be downloaded to
     * device to '/mnt/sdcard/Android/DCIM'
     *
     * @param directoryToUpload
     * @param drivePathToStore
     */
    public void uploadFolder(final File directoryToUpload, final File drivePathToStore, final boolean doOverwriteExistingFiles) {
        if (directoryToUpload == null || !directoryToUpload.exists() || !directoryToUpload.isDirectory()) {
            onLoadFailed(directoryToUpload, "uyploadFolder(): directoryToUpload doesn't exists or is not a directory");
            return;
        }
        this.doOverwriteExistingFiles = doOverwriteExistingFiles;

        realFilesList = new ArrayList<File>();
        uploadTasks = new ArrayList<GDTask>();

        uploadSubfoldersList = new ArrayList<File>();
        uploadSubfoldersList.add(directoryToUpload);

        String drivePath = drivePathToStore.getPath();
        if (drivePath.startsWith(File.separator))
            drivePath = drivePath.substring(1);
        directoryToUploadFrom = directoryToUpload.getAbsolutePath();
        driveFolderToUploadTo = drivePath;

        buildUploadSubfoldersList(directoryToUpload.listFiles());
        //files = realFilesList.toArray(new File[realFilesList.size()]);
//		filesCount = files.length;
//		filesIndex = -1;

        mode = MODE_UPLOAD_FOLDER;

        // ??????? ?? ?????? ????????? ?????? ????? ??? ?????????
        // ??? ??? ??? ?????????? ? ???? ??, ??????? ? ????????
        continueUploadFoldersCreating(null, null);
    }

    private ArrayList<File> uploadSubfoldersList;

    private void buildUploadSubfoldersList(final File[] filesToUpload) {
//		Log.i(TAG,"buildUploadSubfoldersList() filesToUpload: "+Arrays.toString(filesToUpload));
        final int filesCount = filesToUpload.length;
        for (int i = 0; i < filesCount; i++) {
            if (filesToUpload[i].isDirectory()) {
                uploadSubfoldersList.add(filesToUpload[i]);
                buildUploadSubfoldersList(filesToUpload[i].listFiles());
            } else
                realFilesList.add(filesToUpload[i]);
        }
    }


    private String directoryToUploadFrom;
    private String driveFolderToUploadTo;
    private ArrayList<GDTask> uploadTasks;

    /**
     * @hide
     */
    protected final void continueUploadFoldersCreating(final File createdDirectory, final DriveFolder folder) {
        if (folder != null) {
//			final File createdDirectory = foldersToCreate[0];
            uploadSubfoldersList.remove(createdDirectory);
//			Log.i(TAG,"continueUploadFoldersCreating() createdDirectory: "+createdDirectory+" uploadSubfoldersList.size(): "+uploadSubfoldersList.size()+" realFilesList.size(): "+realFilesList.size());
            GDTask uploadTask = null;
            for (File file : realFilesList) {
                if (!file.isDirectory() && file.getParentFile().equals(createdDirectory)) {
                    uploadTask = new GDTask(file, folder, this.doOverwriteExistingFiles);
                    uploadTasks.add(uploadTask);
                }
            }
        }

        if (uploadSubfoldersList.size() > 0) {
            final File folderToCreate = uploadSubfoldersList.get(0);
            final File driveFolderToCreate = new File(folderToCreate.getAbsolutePath().replace(directoryToUploadFrom, driveFolderToUploadTo));
            driveFolderParentsList = getParentFoldersListFromPath(driveFolderToCreate);
            if (driveFolderParentsList == null)
                return;
            driveFolderParentsListIndex = 0;
            Log.d(TAG, "continueUploadFoldersCreating() nextFolder: " + folderToCreate + " -> checkPathOnDrive()");

            createPathOnDrive(Drive.DriveApi.getAppFolder(googleApiClient), folderToCreate);
        } else {
            tasks = uploadTasks.toArray(new GDTask[uploadTasks.size()]);
            Log.d(TAG, "continueUploadFoldersCreating() allTasks count: " + tasks.length);

            //upload tasks
            uploadTasks();
        }
    }

    private void uploadTasks() {
        Log.d(TAG, "uploadTasks() tasks count: " + tasks.length);
        Log.d(TAG, "uploadTasks() tasks: " + Arrays.toString(tasks));
        fileLoader = FileLoader.uploadFile(googleApiClient, this, this, tasks, doOverwriteExistingFiles);
    }

    public void uploadFileAndDisconnect(final File[] files, final File drivePath, final boolean doOverwriteExistingFiles) {
        doDisconnectWhenDone = true;
        uploadFile(files, drivePath, doOverwriteExistingFiles);
    }

    public void uploadFile(final File file, final DriveFolder folder, final boolean doOverwriteExistingFiles) {
        fileLoader = FileLoader.uploadFile(googleApiClient, this, this, new File[]{file}, folder, doOverwriteExistingFiles);
    }

    public void uploadFile(final File[] files, final File drivePath, final boolean doOverwriteExistingFiles) {
        this.files = files;
        this.filesIndex = -1;
        this.filesCount = files.length;
        this.donwloadTasksList = new LinkedList<GDTask>();
        this.doOverwriteExistingFiles = doOverwriteExistingFiles;
        this.driveFolderParentsList = getParentFoldersListFromPath(drivePath);
        if (this.driveFolderParentsList == null)
            return;
        this.mode = MODE_UPLOAD_SINLGE_GD_FOLDER;
        createPathOnDrive(Drive.DriveApi.getAppFolder(googleApiClient), null);
    }

    public void uploadFileAndDisconnect(final File[] files, final boolean doOverwriteExistingFiles) {
        doDisconnectWhenDone = true;
        uploadFile(files, doOverwriteExistingFiles);
    }

    /**
     * Uploads files to GD building on GD same paths
     * as given files has. So if filesToUpload[] contains 2 files:
     * [/mnt/sdcard/dcim/photo1.jpg] and [/mnt/sdcard/download/coolapp.apk]
     * those would be uploaded to GD into the same paths (from GD root):
     * mnt/sdcard/dcim/photo1.jpg and mnt/sdcard/download/coolapp.apk
     *
     * @param filesToUpload
     * @param doOverwriteExistingFiles
     */
    public void uploadFile(final File[] filesToUpload, final boolean doOverwriteExistingFiles) {
        this.doOverwriteExistingFiles = doOverwriteExistingFiles;
        realFilesList = new ArrayList<File>();
        buildFilesList(filesToUpload);
        files = realFilesList.toArray(new File[realFilesList.size()]);
        Log.d(TAG, "uploadFile() allFiles count: " + files.length);
        Log.d(TAG, "uploadFile() allFiles: " + Arrays.toString(files));
        filesCount = files.length;
        filesIndex = -1;
        mode = MODE_UPLOAD_FILE_BY_FILE;
        uploadTasksList = new LinkedList<GDTask>();
        continueUploadFilesListCompiling();
    }

    private ArrayList<File> realFilesList;

    private void buildFilesList(final File[] filesToUpload) {
        final int filesCount = filesToUpload.length;
        for (int i = 0; i < filesCount; i++) {
            if (filesToUpload[i].isDirectory()) {
                Log.d(TAG, "buildFilesList() " + filesToUpload[i] + "isDirectory() -> buildFilesList() " + Arrays.toString(filesToUpload[i].listFiles()));
                buildFilesList(filesToUpload[i].listFiles());
            } else
                realFilesList.add(filesToUpload[i]);
        }
    }

    private LinkedList<GDTask> uploadTasksList;

    private void continueUploadFilesListCompiling() {
        filesIndex++;

        if (filesIndex == filesCount) {
            //TODO
//			if (onLoadCompleteListenerOuter!=null)
//				onLoadCompleteListenerOuter.onAllLoadComplete();
//			if (doDisconnectWhenDone)
//				finish();
//			return;
            tasks = uploadTasksList.toArray(new GDTask[uploadTasksList.size()]);
            uploadTasksList.clear();
            uploadTasksList = null;
            uploadTasks();
            return;
        }

        final File nextFile = files[filesIndex];
        if (!nextFile.getParent().equals(lastDrivePath)) {
            lastDrivePath = files[filesIndex].getParent();
            driveFolderParentsList = getParentFoldersListFromPath(nextFile.getParentFile());
            if (driveFolderParentsList == null)
                return;
            Log.d(TAG, "continueUploadFilesListCompiling() nextFile: " + nextFile + " -> createPathOnDrive()");
            createPathOnDrive(Drive.DriveApi.getAppFolder(googleApiClient), nextFile);
        } else {
            Log.d(TAG, "continueUploadFilesListCompiling() nextFile: " + nextFile + "\nlastDriveFolder: " + lastDriveFolder.getDriveId() + " -> continueUploadFilesListCompiling()");
            uploadTasksList.add(new GDTask(nextFile, lastDriveFolder, doOverwriteExistingFiles));
            continueUploadFilesListCompiling();
            //startFileLoader(new File[]{nextFile}, lastDriveFolder);
        }
    }

    private void createPathOnDrive(final DriveFolder folder, final File file) {
        folder.listChildren(googleApiClient).setResultCallback(
                new ResultCallback<MetadataBufferResult>() {
                    @Override
                    public void onResult(final MetadataBufferResult result) {
                        handleListChildrenResult(folder, file, result);
                    }
                });
    }

    /**
     * @hide
     */
    protected final void handleListChildrenResult(final DriveFolder folder,
                                                  final File file, //if (mode==MODE_UPLOAD_FOLDER)
                                                  final MetadataBufferResult bufferResult) {

        // prevent forced hack-call
        if (driveFolderParentsList == null || folder == null || files == null || bufferResult == null) {
            Log.d(TAG, "handleListChildrenResult() null in parameters");
            return;
        }

        if (!bufferResult.getStatus().isSuccess()) {
            Log.d(TAG, "handleListChildrenResult() failed");
            return;
        }

        final String targetFolder = driveFolderParentsList[driveFolderParentsListIndex];
        final MetadataBuffer metadataBuffer = bufferResult.getMetadataBuffer();
        final int metadataCount = metadataBuffer.getCount();
        Metadata metaData = null;
        for (int i = 0; i < metadataCount; i++) {
            metaData = metadataBuffer.get(i);
            if (targetFolder.equals(metaData.getTitle())) {
                if (driveFolderParentsListIndex < driveFolderParentsList.length - 1) {
                    //Log.d(TAG, "handleListChildrenResult() folder: <"+ targetFolder + "> does exist but its not last by index");
                    // path is not checked completely so lets continue
                    driveFolderParentsListIndex++;
                    createPathOnDrive(Drive.DriveApi.getFolder(googleApiClient, metaData.getDriveId()), file);
                } else {
                    driveFolderParentsListIndex = 0;
                    if (mode == MODE_UPLOAD_FOLDER) {
                        Log.d(TAG, "handleListChildrenResult() folder: <" + targetFolder + "> does exist and its last -> continueUploadFoldersCreating()");
                        continueUploadFoldersCreating(file, Drive.DriveApi.getFolder(googleApiClient, metaData.getDriveId()));
                    } else { // mode == MODE_UPLOAD_FILE_BY_FILE;
                        Log.d(TAG, "handleListChildrenResult() folder: <" + targetFolder + "> does exist and its last -> uploadTasksList.add() -> continueUploadFilesListCompiling()");
                        lastDriveFolder = Drive.DriveApi.getFolder(googleApiClient, metaData.getDriveId());
                        uploadTasksList.add(new GDTask(files[filesIndex], lastDriveFolder, doOverwriteExistingFiles));
                        continueUploadFilesListCompiling();
                        //startFileLoader(files, Drive.DriveApi.getFolder(googleApiClient, metaData.getDriveId()));
                    }
                }
//				Log.i(TAG,"handleListChildrenResult() CLOSED MetadataBufferResult: "+bufferResult.getMetadataBuffer());
                metadataBuffer.close();
                return;
            }
        }
//		Log.i(TAG,"handleListChildrenResult() CLOSED MetadataBufferResult: "+bufferResult.getMetadataBuffer());
        metadataBuffer.close();
        // targetFolder was not found -> create it and following
        final MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle(driveFolderParentsList[driveFolderParentsListIndex]).build();
        folder.createFolder(googleApiClient, changeSet).setResultCallback(
                new ResultCallback<DriveFolderResult>() {
                    @Override
                    public void onResult(final DriveFolderResult result) {
                        handleCreateFolderResult(result, file);
                    }
                });

    }

    /**
     * @hide
     */
    protected final void handleCreateFolderResult(final DriveFolderResult result, final File file) {

        // prevent forced hack-call
        if (driveFolderParentsList == null || result == null) {
            Log.d(TAG, "handleCreateFolderResult() null in parameters");
            return;
        }

        if (!result.getStatus().isSuccess()) {
            Log.d(TAG, "handleCreateFolderResult() result is failed");
            return;
        }

        Log.d(TAG, "handleCreateFolderResult() create folder " + driveFolderParentsList[driveFolderParentsListIndex]);

        if (driveFolderParentsListIndex < driveFolderParentsList.length - 1) {
            driveFolderParentsListIndex++;
            // createPathOnDrive(result.getDriveFolder(), files);
            // targetFolder was CREATED -> continue creating without checking if folder exists
            final MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle(driveFolderParentsList[driveFolderParentsListIndex]).build();
            result.getDriveFolder().createFolder(googleApiClient, changeSet).setResultCallback(
                    new ResultCallback<DriveFolderResult>() {
                        @Override
                        public void onResult(final DriveFolderResult result) {
                            handleCreateFolderResult(result, file);
                        }
                    });
        } else {
            driveFolderParentsListIndex = 0;
            if (mode == MODE_UPLOAD_FOLDER) {
                continueUploadFoldersCreating(file, result.getDriveFolder());
            } else { // mode == MODE_UPLOAD_FILE_BY_FILE;
                lastDriveFolder = result.getDriveFolder();
                uploadTasksList.add(new GDTask(files[filesIndex], lastDriveFolder, doOverwriteExistingFiles));
                continueUploadFilesListCompiling();
                //startFileLoader(files, result.getDriveFolder());
            }
            return;
        }
    }

    //////////////////////////////////////////////////////////////////////////////
    //
    // OTHER/GENERAL/SHARED PART
    //
    //////////////////////////////////////////////////////////////////////////////

//	private void startFileLoader(final File[] files, final DriveFolder folder) {
//		Log.d(TAG,"startFileLoader() DriveFolder."+folder.getDriveId()+"\nFiles: "+Arrays.toString(files));
//		switch (mode) {
//		case MODE_DOWNLOAD_SINLGE_GD_FOLDER:
//			fileLoader = FileLoader.downloadFile(googleApiClient, this, this, files, folder, doOverwriteExistingFiles);
//			break;
//		case MODE_UPLOAD_SINLGE_GD_FOLDER:
//			fileLoader = FileLoader.uploadFile(googleApiClient, this, this, files, folder, doOverwriteExistingFiles);
//			break;
//		case MODE_DOWNLOAD_FILE_BY_FILE:
//			Log.e(TAG,"startFileLoader() MODE_DOWNLOAD_FILE_BY_FILE!");
////			lastDriveFolder = folder;
////			fileLoader = FileLoader.downloadFile(googleApiClient, this, this, files, folder, doOverwriteExistingFiles);
//			break;
//		case MODE_UPLOAD_FILE_BY_FILE:
//			lastDriveFolder = folder;
//			fileLoader = FileLoader.uploadFile(googleApiClient, this, this, files, folder, doOverwriteExistingFiles);
//			break;
//		}
//	}

    @Override
    public void onLoadComplete(final File file) {
        Log.d(TAG, "onLoadComplete() for file: " + file);
        if (onLoadCompleteListenerOuter != null)
            onLoadCompleteListenerOuter.onLoadComplete(file);
    }

    @Override
    public void onLoadFailed(final File file, final String... errMSg) {
        Log.w(TAG, "onLoadFailed() for file: " + file + Arrays.toString(errMSg));
        if (onLoadCompleteListenerOuter != null)
            onLoadCompleteListenerOuter.onLoadFailed(file, errMSg);
    }

    @Override
    public void onAllLoadComplete() {
        Log.d(TAG, "onAllLoadComplete() mode: " + mode);

//		switch (mode) {
//
//		case MODE_UPLOAD_FILE_BY_FILE:
//			continueUploadFilesListCompiling();
//			break;
//
//		case MODE_DOWNLOAD_FILE_BY_FILE:
////			continueDownloadFilesListCompiling();
////			break;
//		case MODE_DOWNLOAD_SINLGE_GD_FOLDER:
//		case MODE_UPLOAD_SINLGE_GD_FOLDER:
        activityToLeak = null;
        if (onLoadCompleteListenerOuter != null)
            onLoadCompleteListenerOuter.onAllLoadComplete();
        if (doDisconnectWhenDone)
            googleApiClient.disconnect();
//			break;
//		}

    }

    @Override
    public void onProgress(final long bytesDownloaded, final long bytesExpected) {
        Log.d(TAG, "onProgress(): bytesDownloaded: " + bytesDownloaded + " bytesExpected: " + bytesExpected);
        if (downloadProgressListener != null) {
            downloadProgressListener.onProgress(bytesDownloaded, bytesExpected);
        }
    }

    /**
     * stops processing files (FileLoader) and googleApiClient disconnect
     */
    public void cancel() {
        if (fileLoader != null)
            fileLoader.stop();
        if (activityToLeak != null)
            activityToLeak = null;
        googleApiClient.disconnect();
    }

    public void finish() {
        cancel();
        googleApiClient = null;
        activityToLeak = null;
        onLoadCompleteListenerOuter = null;
        downloadProgressListener = null;
        fileLoader = null;
        driveFolderParentsList = null;
        files = null;
        lastDrivePath = null;
        lastDriveFolder = null;
        tasks = null;
        folderTasks = null;
    }

    private String[] getParentFoldersListFromPath(final File drivePath) {
        if (drivePath == null)
            return null;

        final String pathToSplit = drivePath.getAbsolutePath();
        if (TextUtils.isEmpty(pathToSplit))
            return null;
        final String[] folderList;
        if (pathToSplit.startsWith(File.separator))
            folderList = pathToSplit.substring(1).split(File.separator);
        else
            folderList = pathToSplit.split(File.separator);

        return folderList;
    }

}