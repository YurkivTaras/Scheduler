package com.y_taras.scheduler.googleDrive;


import java.io.File;

public interface OnLoadCompleteListener {

    public void onLoadComplete(File file);

    public void onAllLoadComplete();

    public void onLoadFailed(File file, String... errMSg);
}
