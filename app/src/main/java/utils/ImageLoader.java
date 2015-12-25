package utils;

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
    public static final String APP_PREFS_NAME = "com.y_taras.scheduler";

    public static String saveImageFile(Bitmap bitmap, Context context) {
        String cachePath;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            cachePath = Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/Android/data/" + APP_PREFS_NAME + "/avatar/";
        } else
            cachePath = context.getCacheDir().getAbsolutePath() + "/Android/data/" + APP_PREFS_NAME + "/avatar/";
        File cacheDir = new File(cachePath);
        if (!cacheDir.exists())
            cacheDir.mkdirs();
        File f = new File(cacheDir, "" + System.currentTimeMillis());
        try {
            FileOutputStream out = new FileOutputStream(f);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return f.getAbsolutePath();
    }

    public static Bitmap loadImage(String url) {
        File fileIMG = new File(url);
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
    public static void deleteAll(Context context) {
        String cachePath;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            cachePath = Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/Android/data/" + APP_PREFS_NAME + "/avatar/";
        else
            cachePath = context.getCacheDir().getAbsolutePath() + "/Android/data/" + APP_PREFS_NAME + "/avatar/";
        File cacheDir = new File(cachePath);
        File[] files = cacheDir.listFiles();
        if (files == null)
            return;
        for (File f : files)
            f.delete();
    }
}
