package com.y_taras.scheduler.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageLoader {
    public static final String AVATAR_FOLDER = "avatar";

    public static String saveImageFile(Bitmap bitmap, Context context) {
        File cacheDir = new File(getCachePath(context));
        if (!cacheDir.exists())
            cacheDir.mkdirs();
        File btmFile = new File(cacheDir, File.separator + System.currentTimeMillis());
        try {
            FileOutputStream out = new FileOutputStream(btmFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return btmFile.getAbsolutePath();
    }

    public static Bitmap loadImage(String url) {
        File fileIMG = new File(url);
        if (!fileIMG.exists())
            return null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(fileIMG);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return BitmapFactory.decodeStream(fis);
    }

    public static boolean delete(String uri) {
        File file = new File(uri);
        boolean deleteSuccess = false;
        if (file.exists())
            deleteSuccess = file.delete();
        return deleteSuccess;
    }

    public static String getCachePath(Context context) {
        File cacheDir;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            //cacheDir = Environment.getExternalStorageDirectory();
            cacheDir = context.getExternalCacheDir();
        else
            cacheDir = context.getCacheDir();
        assert cacheDir != null;
        return cacheDir.getAbsolutePath() + File.separator + AVATAR_FOLDER;
    }

    public static boolean SDCardIsWritable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    public static void deleteAll(Context context) {
        File cacheDir = new File(getCachePath(context));
        File[] files = cacheDir.listFiles();
        if (files == null)
            return;
        for (File f : files)
            f.delete();
    }
}
