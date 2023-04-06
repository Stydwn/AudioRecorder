package com.example.audiorecorder;

import android.app.Application;

import java.util.concurrent.Semaphore;

public class CommonVariables {

    private static boolean blnPlayRecord = false;
    //    private static boolean socket_in = false;
    private static Semaphore mutexPlay = new Semaphore(1);
    private static Semaphore mutexRecord = new Semaphore(0);

    public static boolean getbtnFlag() {
        return blnPlayRecord;
    }
    public static void setbtnFlag(boolean blnPlayRecord_) {
        blnPlayRecord = blnPlayRecord_;
    }
    //    public static boolean getSocketFlag(){return socket_in;}
//    public static void setSocket_in(boolean in){socket_in = in;}
    public static void acqPlay() throws InterruptedException {mutexPlay.acquire();}
    public  static void relPlay(){mutexPlay.release();}
    public  static void acqRecord() throws InterruptedException {mutexRecord.acquire();}
    public static void relRecord(){mutexRecord.release();}

}