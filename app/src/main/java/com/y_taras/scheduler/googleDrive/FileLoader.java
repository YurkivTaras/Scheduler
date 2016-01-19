package com.y_taras.scheduler.googleDrive;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import android.util.Log;

import com.google.android.gms.drive.DriveApi;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.DriveContentsResult;
import com.google.android.gms.drive.DriveApi.MetadataBufferResult;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFile.DownloadProgressListener;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveFolder.DriveFileResult;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;

class FileLoader {

    public static final String TAG = "FileLoader";

    private GDTask[] tasks;
    private FileWrapper[] files;
    private int fileIndex;
    private int filesCount;
    private DriveFolder folder;
    private final GoogleApiClient googleApiClient;
    private DownloadProgressListener uploadProgressListener, downloadProgressListener;
    private OnLoadCompleteListener onLoadCompleteListener;

    private boolean isStopped;

    protected boolean doOverwriteExistingFiles;

//	private FileLoader(GoogleApiClient googleApiClient) {
//		this.googleApiClient = googleApiClient;
//	}

    private FileLoader(final GoogleApiClient googleApiClient,
                       final OnLoadCompleteListener onLoadCompleteListener,
                       final DownloadProgressListener downloadProgressListener,
                       final File[] files,
                       final DriveFolder folder,
                       boolean doOverwriteExistingFiles) {
        this.googleApiClient = googleApiClient;
        this.onLoadCompleteListener = onLoadCompleteListener;
        this.downloadProgressListener = downloadProgressListener;
        this.folder = folder;
        this.files = getFileWrappersFromFiles(files);
        this.filesCount = files != null ? files.length : 0;
        this.fileIndex = -1;
        this.doOverwriteExistingFiles = doOverwriteExistingFiles;
    }

    private FileLoader(GoogleApiClient googleApiClient,
                       final OnLoadCompleteListener onLoadCompleteListener,
                       final DownloadProgressListener downloadProgressListener,
                       final GDTask[] tasks,
                       boolean doOverwriteExistingFiles) {
        this.googleApiClient = googleApiClient;
        this.onLoadCompleteListener = onLoadCompleteListener;
        this.downloadProgressListener = downloadProgressListener;
        this.tasks = tasks;
        this.filesCount = tasks != null ? tasks.length : 0;
        this.fileIndex = -1;
        this.doOverwriteExistingFiles = doOverwriteExistingFiles;
    }

    public void setOnLoadCompleteListener(OnLoadCompleteListener listener) {
        this.onLoadCompleteListener = listener;
    }

    public void setDownloadProgressListener(DownloadProgressListener listener) {
        this.downloadProgressListener = listener;
    }

    public void setUploadProgressListener(DownloadProgressListener listener) {
        this.uploadProgressListener = listener;
    }

    public static FileLoader uploadFile(final GoogleApiClient googleApiClient,
                                        final OnLoadCompleteListener onLoadCompleteListener,
                                        final DownloadProgressListener downloadProgressListener,
                                        final File[] files,
                                        final DriveFolder folder,
                                        final boolean doOverwriteExistingFiles) {

        if (googleApiClient == null || files == null || files.length == 0 || folder == null)
            return null;
        final FileLoader fileLoader = new FileLoader(googleApiClient, onLoadCompleteListener, downloadProgressListener, files, folder, doOverwriteExistingFiles);
        fileLoader.continueUploadFiles();
        return fileLoader;
    }


    /**
     * @hide
     */
    protected final void continueUploadFiles() {
        if (isStopped)
            return;
        fileIndex++; // starts with -1
        if (fileIndex == filesCount) {
            fileIndex = 0;
            if (onLoadCompleteListener != null)
                onLoadCompleteListener.onAllLoadComplete();
//			if (doDisconnectWhenDone){
//				Log.d(TAG,"on doDisconnectWhenDone -> googleApiClient.disconnect()");
//				googleApiClient.disconnect();
//			}
            return;
        }
        if (files == null) {
            if (!tasks[fileIndex].file.exists()) {
                Log.d(TAG, "continueUploadFiles() file " + tasks[fileIndex].file + " does not exist");
                // try to process rest files
                continueUploadFiles();
                return;
            }

            tasks[fileIndex].driveFolder.listChildren(googleApiClient).setResultCallback(
                    new ResultCallback<MetadataBufferResult>() {
                        @Override
                        public void onResult(MetadataBufferResult result) {
                            // inFolderIndex = 0;
                            handleUploadListChildrenResult(result);
                        }
                    });
        } else {
            if (!files[fileIndex].exists()) {
                Log.d(TAG, "continueUploadFiles() file " + files[fileIndex] + " does not exist");
                // try to process rest files
                continueUploadFiles();
                return;
            }

            folder.listChildren(googleApiClient).setResultCallback(
                    new ResultCallback<MetadataBufferResult>() {
                        @Override
                        public void onResult(MetadataBufferResult result) {
                            //						inFolderIndex = 0;
                            handleUploadListChildrenResult(result);
                        }
                    });
        }
    }

    public static FileLoader uploadFile(final GoogleApiClient googleApiClient,
                                        final OnLoadCompleteListener onLoadCompleteListener,
                                        final DownloadProgressListener downloadProgressListener,
                                        final GDTask[] tasks,
                                        final boolean doOverwriteExistingFiles) {

        if (googleApiClient == null || tasks == null || tasks.length == 0)
            return null;
        final FileLoader fileLoader = new FileLoader(googleApiClient, onLoadCompleteListener, downloadProgressListener, tasks, doOverwriteExistingFiles);
        fileLoader.continueUploadFiles();
        return fileLoader;
    }


//	/** @hide */
//	protected final void continueUploadTasks() {
//		if (isStopped)
//			return;
//		fileIndex++; // starts with -1
//		if (fileIndex == filesCount) {
//			fileIndex = 0;
//			if (onLoadCompleteListener != null)
//				onLoadCompleteListener.onAllLoadComplete();
////			if (doDisconnectWhenDone) {
////				Log.d(TAG,"on doDisconnectWhenDone -> googleApiClient.disconnect()");
////				googleApiClient.disconnect();
////			}
//			return;
//		}
//
//		if (!tasks[fileIndex].file.exists()) {
//			Log.d(TAG, "continueUploadFiles() file " + tasks[fileIndex].file + " does not exist");
//			// try to process rest files
//			continueUploadTasks();
//			return;
//		}
//
//		tasks[fileIndex].driveFolder.listChildren(googleApiClient).setResultCallback(
//				new ResultCallback<MetadataBufferResult>() {
//					@Override
//					public void onResult(MetadataBufferResult result) {
//						// inFolderIndex = 0;
//						handleUploadListChildrenResult(result);
//					}
//				});
//
//	}

    /**
     * @hide
     */
    protected final void handleUploadListChildrenResult(final MetadataBufferResult bufferResult) {
//		Log.i(TAG,"handleUploadListChildrenResult() got MetadataBufferResult: "+bufferResult.getMetadataBuffer());

        if (isStopped) {
            bufferResult.getMetadataBuffer().close();
            return;
        }

        final File file = files != null ? files[fileIndex].file : tasks[fileIndex].file;

        if (!bufferResult.getStatus().isSuccess()) {
            Log.w(TAG, "handleUploadFileListChildrenResult() failed for file: " + file.getName());
            if (onLoadCompleteListener != null)
                onLoadCompleteListener.onLoadFailed(file, "listChildren() failed: " + bufferResult.getStatus().getStatusCode());
            // trying to process rest of files
            continueDownloadFiles();
            return;
        }
        final MetadataBuffer metadataBuffer = bufferResult.getMetadataBuffer();
        final int metadataCount = metadataBuffer.getCount();
        //final int startMetadataIndex = startIndex!=null && startIndex.length>0 ? startIndex[0] : 0;
        final String targetFileName = file.getName();
        // looking4 a file files[fileIndex] in contents
        // and start downloading if found
        boolean isFound = false;
        Metadata metadata = null;
        for (int i = 0; i < metadataCount; i++) {
            metadata = metadataBuffer.get(i);
            if (metadata.getTitle().equals(targetFileName)) {
                isFound = true;
                break;
            }
        }

        if (isFound) {
            if (doOverwriteExistingFiles) {
                Log.d(TAG, "handleUploadListChildrenResult() driveFile: " + file + " exists -> driveFile.openContents()");
                final DriveFile driveFile = Drive.DriveApi.getFile(googleApiClient, metadata.getDriveId());
                driveFile.open(googleApiClient, DriveFile.MODE_WRITE_ONLY, uploadProgressListener).setResultCallback(
                        new ResultCallback<DriveContentsResult>() {
                            @Override
                            public void onResult(final DriveContentsResult result) {
                                handleUploadOpenContentsResult(result, driveFile);
                            }
                        });
            } else {
                Log.d(TAG, "handleUploadListChildrenResult() driveFile: " + file + " exists & no overwrite mode -> continueUpload()");
                if (onLoadCompleteListener != null)
                    onLoadCompleteListener.onLoadFailed(file, "File already exists and overwrite is forbidden");
                metadataBuffer.close();
                continueUploadFiles();
                return;
            }
        } else {
            Log.d(TAG, "handleUploadListChildrenResult() driveFile: " + file + " does NOT exists -> Drive.DriveApi.newContents()");
            Drive.DriveApi.newDriveContents(googleApiClient).setResultCallback(
                    new ResultCallback<DriveContentsResult>() {
                        @Override
                        public void onResult(final DriveContentsResult result) {
                            handleUploadNewContentsResult(result);
                        }
                    });
        }

        metadataBuffer.close();
    }


    /**
     * @hide
     */
    protected final void handleUploadNewContentsResult(final DriveContentsResult result) {

        if (isStopped)
            return;

        final File file = files != null ? files[fileIndex].file : tasks[fileIndex].file;
        final DriveFolder targetFolder = files != null ? folder : tasks[fileIndex].driveFolder;

        if (!result.getStatus().isSuccess()) {
            if (onLoadCompleteListener != null)
                onLoadCompleteListener.onLoadFailed(file, "newContents() failed: " + result.getStatus().getStatusCode());
            Log.w(TAG, "handleUploadNewContentsResult() failed for file: " + file);
            // trying to process rest of files
            continueUploadFiles();
        }

        final String fileName = file.getName();
        final MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle(fileName).setMimeType("text/plain").build();
        targetFolder.createFile(googleApiClient, changeSet, result.getDriveContents()).setResultCallback(
                new ResultCallback<DriveFileResult>() {
                    @Override
                    public void onResult(DriveFileResult result) {
                        if (!result.getStatus().isSuccess()) {
                            if (onLoadCompleteListener != null)
                                onLoadCompleteListener.onLoadFailed(file, "createFile() failed: " + result.getStatus().getStatusCode());
                            Log.w(TAG, "handleUploadNewContentsResult() folder.createFile() failed");
                            continueUploadFiles();
                            return;
                        }
                        final DriveFile driveFile = result.getDriveFile();
                        driveFile.open(googleApiClient, DriveFile.MODE_WRITE_ONLY, uploadProgressListener).setResultCallback(
                                new ResultCallback<DriveContentsResult>() {
                                    @Override
                                    public void onResult(final DriveContentsResult result) {
                                        handleUploadOpenContentsResult(result, driveFile);
                                    }
                                });
                    }
                });
    }


    /**
     * @hide
     */
    protected final void handleUploadOpenContentsResult(final DriveContentsResult result, final DriveFile driveFile) {
        if (isStopped)
            return;

        final File file = files != null ? files[fileIndex].file : tasks[fileIndex].file;

        if (!result.getStatus().isSuccess()) {
            if (onLoadCompleteListener != null)
                onLoadCompleteListener.onLoadFailed(file, "openContents() failed: " + result.getStatus().getStatusCode());
            Log.w(TAG, "handleUploadOpenContentsResult() failed for file: " + file);
            continueUploadFiles();
            return;
        }

        final OutputStream fileOutput = result.getDriveContents().getOutputStream();
        InputStream fileInput = null;
        try {
            fileInput = new FileInputStream(file);
            final byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileInput.read(buffer)) != -1) {
                fileOutput.write(buffer, 0, bytesRead);
            }
            fileOutput.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileInput != null)
                try {
                    fileInput.close();
                } catch (IOException ignored) {
                }
            if (fileOutput != null)
                try {
                    fileOutput.close();
                } catch (IOException ignored) {
                }

        }
//
        new Thread() {
            @Override
            public void run() {
                try {
                    if (googleApiClient != null) {
                        result.getDriveContents().commit(googleApiClient, null).await();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
// 									driveContentsResult.getDriveContents().commit(googleApiClient, null).await();
                Log.e("Thread started", "bla");
            }
        }.start();


//		driveFile.commitAndCloseContents (googleApiClient, result.getDriveContents()).setResultCallback(new ResultCallback<Status>() {
//			@Override
//			public void onResult(final Status status) {
//				if (status.isSuccess()) {
//					Log.d(TAG, "handleUploadOpenContentsResult() commitAndCloseContents() succefully created file " + file.getName());
//					if (onLoadCompleteListener != null)
//						onLoadCompleteListener.onLoadComplete(file);
//				} else {
//					Log.w(TAG, "handleUploadOpenContentsResult() commitAndCloseContents() cannot create file " + file.getName());
//					if (onLoadCompleteListener != null)
//						onLoadCompleteListener.onLoadFailed(file, "commitAndCloseContents() failed: " + status.getStatusCode());
//				}
//				continueUploadFiles();
//			}
//		});

//				driveFile.open(googleApiClient, null).setResultCallback(new ResultCallback<DriveContentsResult>() {
//					@Override
//					public void onResult(final DriveContentsResult driveContentsResult) {
//						if (driveContentsResult.getStatus().isSuccess()) {
//							new Thread(){
//								@Override
//								public void run() {
//									driveContentsResult.getDriveContents().commit(googleApiClient, null).await();
//								}
//							}.start();
//
//
//							Log.d(TAG, "handleUploadOpenContentsResult() commitAndCloseContents() succefully created file " + file.getName());
//							if (onLoadCompleteListener != null)
//								onLoadCompleteListener.onLoadComplete(file);
//						} else {
//							Log.w(TAG, "handleUploadOpenContentsResult() commitAndCloseContents() cannot create file " + file.getName());
//							if (onLoadCompleteListener != null)
//								onLoadCompleteListener.onLoadFailed(file, "commitAndCloseContents() failed: " + driveContentsResult.getStatus().getStatusCode());
//						}
//					}
//				});


        continueUploadFiles();


    }


//	public void downloadFile(final File[] files, final DriveFolder folder) {
//		if (files==null || files.length==0 || folder==null)
//			return;
//		//this.files = files;
//		this.folder = folder;
//		this.files = getFileWrappersFromFiles(files);
//		this.filesCount = files.length;
//		this.fileIndex = -1;
//		continueDownloadFiles();
//	}

    public static FileLoader downloadFile(final GoogleApiClient googleApiClient,
                                          final OnLoadCompleteListener onLoadCompleteListener,
                                          final DownloadProgressListener uploadProgressListener,
                                          final File[] files,
                                          final DriveFolder folder,
                                          final boolean doOverwriteExistingFiles) {

        if (googleApiClient == null || files == null || files.length == 0 || folder == null)
            return null;
        //this.files = files;
        final FileLoader fileLoader = new FileLoader(googleApiClient, onLoadCompleteListener, uploadProgressListener, files, folder, doOverwriteExistingFiles);
        fileLoader.continueDownloadFiles();
        return fileLoader;
    }


    private void continueDownloadFiles() {
        if (isStopped)
            return;

        fileIndex++;

        if (fileIndex == filesCount) {

            downloadFinished();

        } else {
            // tasks mode
            if (files == null) {
                if (tasks[fileIndex].file.exists() && !doOverwriteExistingFiles) {
                    Log.d(TAG, "continueDownloadTasks() file: " + tasks[fileIndex].file + " exists & no overwrite mode -> continueDownloadFiles()");
                    if (onLoadCompleteListener != null)
                        onLoadCompleteListener.onLoadFailed(tasks[fileIndex].file, "File already exists and overwrite is forbidden");
                    continueDownloadFiles();
                } else
                    Drive.DriveApi
                            .getFile(googleApiClient, tasks[fileIndex].driveFile.getDriveId())
                            .open(googleApiClient, DriveFile.MODE_READ_ONLY, downloadProgressListener)
                            .setResultCallback(
                                    new ResultCallback<DriveContentsResult>() {
                                        @Override
                                        public void onResult(final DriveContentsResult contentsResult) {
                                            handleDownloadOpenContentsResult(contentsResult, tasks[fileIndex]);
                                        }
                                    });
            } else { // files mode
                folder.listChildren(googleApiClient).setResultCallback(
                        new ResultCallback<MetadataBufferResult>() {
                            @Override
                            public void onResult(final MetadataBufferResult result) {
                                //						inFolderIndex = 0;
                                handleDownloadListChildrenResult(result);
                            }
                        });
            }
        }
    }

    public static FileLoader downloadFile(final GoogleApiClient googleApiClient,
                                          final OnLoadCompleteListener onLoadCompleteListener,
                                          final DownloadProgressListener uploadProgressListener,
                                          final GDTask[] tasks,
                                          final boolean doOverwriteExistingFiles) {

        if (googleApiClient == null || tasks == null || tasks.length == 0)
            return null;
        //this.files = files;
        final FileLoader fileLoader = new FileLoader(googleApiClient, onLoadCompleteListener, uploadProgressListener, tasks, doOverwriteExistingFiles);
        fileLoader.continueDownloadFiles();
        return fileLoader;
    }

    private void downloadFinished() {
        fileIndex = 0;
        if (onLoadCompleteListener != null)
            onLoadCompleteListener.onAllLoadComplete();
//		if (doDisconnectWhenDone){
//			Log.d(TAG,"on doDisconnectWhenDone -> googleApiClient.disconnect()");
//			googleApiClient.disconnect();
//		}
    }

    private void continueDownloadFiles(final MetadataBufferResult bufferResult) {
        if (isStopped) {
//			Log.i(TAG,"continueDownloadFiles() STOP! -> closed MetadataBufferResult: "+bufferResult.getMetadataBuffer());
            bufferResult.getMetadataBuffer().close();
            return;
        }

        fileIndex++;
        if (fileIndex == filesCount) {
            fileIndex = 0;
            // check if really all files are processed
            for (int i = 0; i < filesCount; i++)
                if (!files[i].isCompleted && !files[i].isFailed) {
                    //set index to current not processed file to continue
                    fileIndex = i;
                    continueDownloadFiles();
                    return;
                }

            if (onLoadCompleteListener != null)
                onLoadCompleteListener.onAllLoadComplete();

            return;
        }

        handleDownloadListChildrenResult(bufferResult);
    }

    //private int inFolderIndex;

    /**
     * @hide
     */
    protected final void handleDownloadListChildrenResult(final MetadataBufferResult bufferResult) {
//		Log.i(TAG,"handleDownloadListChildrenResult() got MetadataBufferResult: "+bufferResult.getMetadataBuffer());

        if (isStopped) {
            bufferResult.getMetadataBuffer().close();
            return;
        }

        if (!bufferResult.getStatus().isSuccess()) {
            Log.w(TAG, "handleDownloadFileListChildrenResult() failed for file: " + files[fileIndex]);
            if (onLoadCompleteListener != null)
                onLoadCompleteListener.onLoadFailed(files[fileIndex].file);
            // trying to process rest of files
            continueDownloadFiles();
            return;
        }

        final MetadataBuffer metadataBuffer = bufferResult.getMetadataBuffer();
        final int metadataCount = metadataBuffer.getCount();
        //final int startMetadataIndex = startIndex!=null && startIndex.length>0 ? startIndex[0] : 0;
        final String targetFileName = files[fileIndex].getName();
        // looking4 a file files[fileIndex] in contents
        // and start downloading if found
        boolean isFound = false;
        // isDirectory() for downloading file always false since
        // such file doesn't exist in file system yet to check if its directory
//		if (files[fileIndex].file.isDirectory()){
//
//			Log.d(TAG,"file.isDirectory(): "+files[fileIndex]);
//
//			if ( inFolderIndex < metadataCount ) {
//				Drive.DriveApi
//						.getFile(googleApiClient, metadataBuffer.get(inFolderIndex).getDriveId())
//						.openContents(googleApiClient, DriveFile.MODE_READ_ONLY, downloadProgressListener)
//						.setResultCallback(
//								new ResultCallback<ContentsResult>() {
//									@Override
//									public void onResult(final ContentsResult contentsResult) {
//										handleDownloadOpenContentsResult(contentsResult, bufferResult);
//									}
//								});
//				isFound = true;
//				inFolderIndex++;
//			}
//		}
//		else {
        Log.d(TAG, "handleDownloadListChildrenResult() file: " + files[fileIndex]);

        Metadata metadata = null;
        for (int i = 0; i < metadataCount; i++) {
            metadata = metadataBuffer.get(i);
            if (metadata.getTitle().equals(targetFileName)) {
                if (files[fileIndex].exists() && !doOverwriteExistingFiles) {
                    Log.d(TAG, "continueDownloadFiles() file: " + files[fileIndex] + " exists & no overwrite mode -> continueDownloadFiles()");
                    if (onLoadCompleteListener != null)
                        onLoadCompleteListener.onLoadFailed(files[fileIndex].file, "File already exists and overwrite is forbidden");
                    continueDownloadFiles(bufferResult);
                } else
                    Drive.DriveApi
                            .getFile(googleApiClient, metadata.getDriveId())
                            .open(googleApiClient, DriveFile.MODE_READ_ONLY, downloadProgressListener)
                            .setResultCallback(
                                    new ResultCallback<DriveContentsResult>() {
                                        @Override
                                        public void onResult(final DriveContentsResult contentsResult) {
                                            handleDownloadOpenContentsResult(contentsResult, bufferResult);
                                        }
                                    });
                isFound = true;
                break;
            }
        }
//		}

        if (!isFound) {
//			Log.i(TAG,"handleDownloadListChildrenResult() closed MetadataBufferResult: "+bufferResult.getMetadataBuffer());
            metadataBuffer.close();

            boolean isAllFilesProcessed = true;
            for (int i = 0; i < filesCount; i++)
                if (!files[i].isCompleted && !files[i].isFailed) {
                    isAllFilesProcessed = false;
                    fileIndex = i;
                }

            if (isAllFilesProcessed)
                fileIndex = filesCount - 1;


            continueDownloadFiles();
        }
    }

    /**
     * hide
     */
    protected final void handleDownloadOpenContentsResult(final DriveContentsResult result, final MetadataBufferResult bufferResult) {
        if (isStopped) {
            Log.w(TAG, "handleDownloadOpenContentsResult() STOP! -> closed MetadataBufferResult: " + bufferResult.getMetadataBuffer());
            bufferResult.getMetadataBuffer().close();
            return;
        }

        if (!result.getStatus().isSuccess()) {
            Log.w(TAG, "handleDownloadFileOpenContentsResult() failed for file: " + files[fileIndex]);
            if (onLoadCompleteListener != null)
                onLoadCompleteListener.onLoadFailed(files[fileIndex].file);
            continueDownloadFiles();
            return;
        }
        saveToFile(result, files[fileIndex].file);

        continueDownloadFiles(bufferResult);
    }

    /**
     * hide
     */
    protected final void handleDownloadOpenContentsResult(final DriveContentsResult result, final GDTask task) {
        if (isStopped) {
            Log.w(TAG, "handleDownloadOpenContentsResult() STOP!");
            return;
        }

        if (!result.getStatus().isSuccess()) {
            Log.w(TAG, "handleDownloadFileOpenContentsResult() failed for file: " + files[fileIndex]);
            if (onLoadCompleteListener != null)
                onLoadCompleteListener.onLoadFailed(files[fileIndex].file);
            continueDownloadFiles();
            return;
        }

        saveToFile(result, task.file);

        continueDownloadFiles();
    }

    private void saveToFile(final DriveContentsResult result, final File targetFile) {
        final File file = files != null ? files[fileIndex].file : tasks[fileIndex].file;
        final InputStream fileInput = result.getDriveContents().getInputStream();
        OutputStream fileOutput = null;
        try {
            fileOutput = new FileOutputStream(targetFile);
            final byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileInput.read(buffer)) != -1)
                fileOutput.write(buffer, 0, bytesRead);

            fileOutput.flush();

            if (onLoadCompleteListener != null)
                onLoadCompleteListener.onLoadComplete(file);
            if (files != null)
                files[fileIndex].isCompleted = true;
            else
                tasks[fileIndex].isCompleted = true;

            Log.d(TAG, "completed downloading file: " + file.getAbsolutePath());

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            files[fileIndex].isFailed = true;
        } catch (IOException e) {
            e.printStackTrace();
            files[fileIndex].isFailed = true;
        } finally {
            if (fileInput != null)
                try {
                    fileInput.close();
                } catch (IOException ignored) {
                }
            if (fileOutput != null)
                try {
                    fileOutput.close();
                } catch (IOException ignored) {
                }
            result.getDriveContents();
        }
    }

    /**
     * stops processing of downloading or uploading
     */
    public void stop() {
        isStopped = true;
    }

    private FileWrapper[] getFileWrappersFromFiles(final File[] files) {
        final int filesCount = files.length;
        //final FileWrapper[] fileWrappers = new FileWrapper[filesCount];
        final ArrayList<FileWrapper> fileWrappers = new ArrayList<FileWrapper>();
        for (int i = 0; i < filesCount; i++) {
            if (files[i].isDirectory()) {
                addFilesFromSubdir(files[i], fileWrappers);
            } else
                fileWrappers.add(new FileWrapper(files[i]));
        }
        final FileWrapper[] fileWrappersArray = new FileWrapper[fileWrappers.size()];
        return fileWrappers.toArray(fileWrappersArray);
    }

    private void addFilesFromSubdir(final File subDir, final ArrayList<FileWrapper> fileWrappers) {
        final File[] filesInSubDir = subDir.listFiles();
        final int filesInSubDirCount = filesInSubDir.length;
        for (fileIndex = 0; fileIndex < filesInSubDirCount; fileIndex++) {
            if (filesInSubDir[fileIndex].isDirectory()) {
                addFilesFromSubdir(filesInSubDir[fileIndex], fileWrappers);
            } else {
                fileWrappers.add(new FileWrapper(filesInSubDir[fileIndex]));
            }
        }
    }

    private static class FileWrapper {
        public final File file;
        public boolean isCompleted;
        public boolean isFailed;

        public FileWrapper(File file) {
            this.file = file;
        }

//		public String getAbsolutePath(){
//			return file.getAbsolutePath();
//		}

        public String getName() {
            return file.getName();
        }


        public boolean exists() {
            return file.exists();
        }

        @Override
        public String toString() {
            return file.toString();
        }

//		public boolean isCompleted() {
//			return isCompleted;
//		}
//
//		public void setCompleted(boolean isCompleted) {
//			this.isCompleted = isCompleted;
////			if(onLoadCompleteListener!=null)
////				onLoadCompleteListener.onLoadComplete(files[fileIndex].file);
//		}
    }

}
