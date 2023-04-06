package com.example.audiorecorder;

import com.example.audiorecorder.utils.FileUtil;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AudioRecorder {

    // variable can be read by other classes
    public static int BUFFER_SIZE = MainActivity.signal_len * 2; ;
    public static final int SAMPLE_RATE = AudioPlayer.SAMPLE_RATE;
    private static final int CHANNEL_TYPE = AudioFormat.CHANNEL_IN_STEREO;
    private static final int ENCODING_TYPE = AudioFormat.ENCODING_PCM_16BIT;

    private static FileOutputStream fileOutputStream;
    private static long timestamp;
    private static long lastSaveTime;
    private Phone phone;

    public AudioRecorder() {
        BUFFER_SIZE = MainActivity.signal_len * 2;
    }

    public void start() {
        phone = new Microphone();
        timestamp = System.currentTimeMillis();
        lastSaveTime = System.currentTimeMillis();
        fileOutputStream = FileUtil.getFileOutputStream( timestamp + ".pcm");
        phone.start();
    }

    public void stop() {
        phone.terminate();

        try {
            fileOutputStream.close();
        } catch (IOException e) {
            Log.e("AudioRecorder", e.getMessage());
        }
    }


    private static abstract class Phone extends Thread {
        protected final byte[] buffer = new byte[AudioRecorder.BUFFER_SIZE];
        protected boolean running;
        protected final Lock lock = new ReentrantLock();

        public abstract void terminate();
    }

    private static class Microphone extends Phone {
        /**
         * AudioRecord 五个参数
         * audioSource 表示数据来源 一般为麦克风 MediaRecorder.AudioSource.MIC
         * sampleRateInHz 表示采样率 一般设置为 44100
         * channelConfig 表示声道 一般设置为 AudioFormat.CHANNEL_IN_MONO
         * audioFormat 数据编码方式 这里使用 AudioFormat.ENCODING_PCM_16BIT
         * bufferSizeInBytes 数据大小 这里使用AudioRecord.getMinBufferSize 获取
         */
        @SuppressLint("MissingPermission")
        private final AudioRecord audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.UNPROCESSED,
                AudioRecorder.SAMPLE_RATE,
                AudioRecorder.CHANNEL_TYPE,
                AudioRecorder.ENCODING_TYPE,
                AudioRecorder.BUFFER_SIZE
        );

        public Microphone() { }

        @Override
        public void run() {
            Log.d("----", "run: start recording");
            audioRecord.startRecording();
            running = true;
            while (running) {
                try {
                    CommonVariables.acqRecord();
                    audioRecord.read(buffer, 0, AudioRecorder.BUFFER_SIZE, AudioRecord.READ_BLOCKING);
                    Log.i("receive buffer",buffer[0]+" "+buffer[1]+" "+buffer[2]);
                    FileUtil.streamWriteMusic(fileOutputStream, buffer);
                    long time = System.currentTimeMillis();
                    if(time - timestamp > 1000*60*60) { // 1h
                        timestamp = time;
                        lastSaveTime = time;
                        fileOutputStream = FileUtil.saveInNewFile(fileOutputStream, timestamp+".pcm");
                    }else if(time - lastSaveTime > 1000*60*5){ // 5min
                        lastSaveTime = time;
                        fileOutputStream = FileUtil.saveInStage(fileOutputStream, timestamp+".pcm");
                    }
                } catch (InterruptedException e) {
                    Log.i("sync","get record mutex failed");
                    throw new RuntimeException(e);
                }
                finally {
                    CommonVariables.relPlay();
                }

//                if (lock.tryLock()) {
//                    // float[] audioData, int offsetInFloats, int sizeInFloats,int readMode
//                    // With READ_BLOCKING, the read will block until all the requested data is read.
//                    audioRecord.read(buffer, 0, AudioRecorder.BUFFER_SIZE, AudioRecord.READ_BLOCKING);
//                    Log.i("receive buffer",buffer[0]+" "+buffer[1]+" "+buffer[2]);
//                    FileUtil.streamWriteMusic(fileOutputStream, buffer);
//                    long time = System.currentTimeMillis();
//                    if(time - timestamp > 1000*60*60) { // 1h
//                        timestamp = time;
//                        lastSaveTime = time;
//                        fileOutputStream = FileUtil.saveInNewFile(fileOutputStream, timestamp+".pcm");
//                    }else if(time - lastSaveTime > 1000*60*5){ // 5min
//                        lastSaveTime = time;
//                        fileOutputStream = FileUtil.saveInStage(fileOutputStream, timestamp+".pcm");
//                    }
////                    lock.unlock();
//                } else {
//                    break;
//                }
            }
        }

        public void terminate() {
            running = false;
            Log.d("----", "run: stop recording");

            lock.lock();
            audioRecord.stop();
            audioRecord.release();
            lock.unlock();
        }
    }
}
