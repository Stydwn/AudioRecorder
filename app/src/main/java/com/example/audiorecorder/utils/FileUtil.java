package com.example.audiorecorder.utils;

import com.example.audiorecorder.MainActivity;

import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtil {
    private static MainActivity mainActivity;
    private static String dirName;

    public static void init(MainActivity mainActivity) {
        FileUtil.mainActivity = mainActivity;
    }

    public static FileOutputStream getFileOutputStream(String filePath) {
        if (filePath == null) {
            return null;
        }

        dirName = String.valueOf(System.currentTimeMillis());
        File dirs = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), dirName); //for pixel3
//        File dirs = new File(mainActivity.getExternalFilesDir(null), dirName);       // using abs path for pixelXL
        if(!dirs.exists()){
            Log.i("dirs path ",dirs.toString());
            dirs.mkdir();
            if(dirs.exists()){
                Log.i("dirs path","hhh");
            }
        }

        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)+"/"+dirName, filePath);
//        File file = new File(mainActivity.getExternalFilesDir(null)+"/"+dirName, filePath);
        Log.i("save path ",file.toString());
        try {
            return new FileOutputStream(file);
        } catch (IOException e) {
            Log.e("FileUtil", e.getMessage());
            return null;
        }
    }

    public static void streamWriteMusic(FileOutputStream writer, byte[] buffer) {
        if (buffer == null) {
            return;
        }
        try {
            writer.write(buffer);
        } catch (IOException e) {
            Log.e("FileUtil", e.getMessage());
        }
    }

    public static FileOutputStream saveInNewFile(FileOutputStream fileOutputStream, String fileName){
        try {
            fileOutputStream.close();
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)+"/"+dirName, fileName);
//            File file = new File(mainActivity.getExternalFilesDir(null)+"/"+dirName, fileName);
            return new FileOutputStream(file);
        } catch (IOException e) {
            Log.e("FileUtil", e.getMessage());
            return null;
        }
    }

    public static FileOutputStream saveInStage(FileOutputStream fileOutputStream, String fileName){
        try {
            fileOutputStream.close();
            return new FileOutputStream(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC) +"/"+dirName + "/" + fileName, true);
//            return new FileOutputStream(mainActivity.getExternalFilesDir(null) +"/"+dirName + "/" + fileName, true);
        } catch (IOException e) {
            Log.e("FileUtil", e.getMessage());
            return null;
        }
    }
}
