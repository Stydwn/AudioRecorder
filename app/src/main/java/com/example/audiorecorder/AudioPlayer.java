package com.example.audiorecorder;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jtransforms.fft.DoubleFFT_1D;


public class AudioPlayer {

    // public variable can be read by other classes
    public static int N_ZC_UP = MainActivity.signal_len;;
    public static int N_ZC = MainActivity.N_ZC;

    public static boolean[] SPEAKER_CHANNEL_MASK = MainActivity.SPEAKER_CHANNEL_MAKS;;
    public static final int FC = MainActivity.FC;
    public static final int SAMPLE_RATE = MainActivity.FS;
    private static final boolean USE_WINDOW = false;
    private static final int U = MainActivity.ZC_ROOT;
    private static final double SCALE = 0.9;

    private static double[][] TX_SEQ;
    private Speaker speaker;
    private static WavReader wave;
    private static int numSamples=1920*10;
    public static int BUFFER_SIZE = numSamples;
    private static double sample[] = new double[numSamples];
    private static int count=0;
    private static byte generatedSound[] = new byte[2 * numSamples];//corresponding to numSamples,final buffer size bytes to play

    public AudioPlayer() {
        TX_SEQ = new double[N_ZC_UP][2];

        genZCSeq();
    }

    public void start() {
        speaker = new SeqSpeaker();
        speaker.start();
    }

    public void stop() {
        speaker.terminate();
    }


    private static abstract class Speaker extends Thread {
        protected final float[] buffer = new float[AudioPlayer.BUFFER_SIZE];
        // AudioTrack https://www.jianshu.com/p/632dce664c3d
        // int streamType, 音频流类型，音乐，电话等，系统对不同类型会分别进行管理
        // int sampleRateInHz, 采样率，inaudible 18kHz-22kHz
        // int channelConfig, 出声通道数目
        // int audioFormat, 音频量化位数 8/16bit
        // int bufferSizeInBytes, 缓冲区大小，最小缓冲区的大小取决于采样率、声道数、采样深度
        // int mode，stream和static两种，后者一次性就把数据交付给接收方，对较大的数据量而言是无法胜任的

        protected final AudioTrack audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                AudioPlayer.SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                AudioPlayer.BUFFER_SIZE,
                AudioTrack.MODE_STREAM
        );
        protected boolean running;
        protected final Lock lock = new ReentrantLock();

        public abstract void terminate();
    }

    private static class SeqSpeaker extends Speaker {
        public SeqSpeaker() {
            wave = new WavReader("/sdcard/FMCW.wav");
        }

        @Override
        public void run() {
            audioTrack.play();
            running = true;
            while (running) {
                genTone();
                try {
                    CommonVariables.acqPlay();
                    Log.i("fmcw_code", ""+audioTrack.write(generatedSound, 0, AudioPlayer.BUFFER_SIZE,AudioTrack.WRITE_BLOCKING));
                } catch (InterruptedException e) {
                    Log.i("sync","get record mutex failed");
                    throw new RuntimeException(e);
                }
//                if (lock.tryLock()) {
//                    Log.i("fmcw_code", ""+audioTrack.write(generatedSound, 0, AudioPlayer.BUFFER_SIZE,AudioTrack.WRITE_BLOCKING));
////                    lock.unlock();
//                } else {
//                    break;
//                }
                finally {
                    CommonVariables.relRecord();
                }
            }
        }

        @Override
        public void terminate() {
            running = false;

            lock.lock();

            audioTrack.stop();
            audioTrack.release();

            lock.unlock();
        }

        private void prepareBuffer() {
            for (int i = 0; i < AudioPlayer.BUFFER_SIZE / 2; i++) {
                buffer[2 * i] = (float) AudioPlayer.TX_SEQ[i % AudioPlayer.N_ZC_UP][0];
                buffer[2 * i + 1] = (float) AudioPlayer.TX_SEQ[i % AudioPlayer.N_ZC_UP][1];
            }
        }

        public void genTone(){
            // fill out the array
            sample = wave.getData();
             Log.i("sample",sample[0]+" "+sample[1]+" "+sample[2]);
            // convert to 16 bit pcm sound array
            // assumes the sample buffer is normalised.
            int idx = 0;
            // repeat times
            for (int i = 1; i <= numSamples; i++) {
                // scale to maximum amplitude
                short val = (short) ((sample[i]));
                // in 16 bit wav PCM, first byte is the low order byte
                generatedSound[idx++] = (byte) (val & 0x00ff);
                generatedSound[idx++] = (byte) ((val & 0xff00) >>> 8);
            }
            Log.i("generatedSound",generatedSound[0]+" "+generatedSound[1]+" "+generatedSound[2]);
        }
    }

    private void genZCSeq() {
        // Generate raw zc seq
        double[] rawZC = new double[2 * N_ZC];
        for (int i = 0; i < N_ZC; i++) {
            double theta = -Math.PI * U * i * (i + 1) / N_ZC;
            rawZC[2 * i] = Math.cos(theta);
            rawZC[2 * i + 1] = Math.sin(theta);
        }

        // Transfer raw zc to freq domain
        DoubleFFT_1D fft = new DoubleFFT_1D(N_ZC);
        fft.complexForward(rawZC);

        // Apply hann window based on the reference url: https://ww2.mathworks.cn/help/signal/ref/hann.html
        if (USE_WINDOW) {
            for (int i = 0; i < N_ZC; i++) {
                rawZC[i] *= 0.5 * (1 - Math.cos(2 * Math.PI * i / (N_ZC - 1)));
            }
        }

        // Padding zeros in freq domain
        double[] freqPadZC = new double[2 * N_ZC_UP];
        int len = N_ZC_UP - N_ZC;
        for (int i = 0; i < N_ZC; i++) {
            if (i < (N_ZC + 1) / 2) {
                freqPadZC[2 * i] = rawZC[2 * i];
                freqPadZC[2 * i + 1] = rawZC[2 * i + 1];
            } else {
                freqPadZC[2 * (i + len)] = rawZC[2 * i];
                freqPadZC[2 * (i + len) + 1] = rawZC[2 * i + 1];
            }
        }

        // Back to time domain
        DoubleFFT_1D ifft = new DoubleFFT_1D(N_ZC_UP);
        ifft.complexInverse(freqPadZC, true);

        // Up conversion
        double[] seqFrame = new double[N_ZC_UP];
        double maxValue = 0.0;
        for (int i = 0; i < N_ZC_UP; i++) {
            double theta = -2 * Math.PI * FC * (i + 1) / SAMPLE_RATE;
            double real = Math.cos(theta),
                    imag = Math.sin(theta);
            seqFrame[i] = freqPadZC[2 * i] * real + freqPadZC[2 * i + 1] * imag;
            maxValue = Math.max(maxValue, Math.abs(seqFrame[i]));
        }

        // Scaling
        for (int i = 0; i < N_ZC_UP; i++) {
            if (SPEAKER_CHANNEL_MASK[0]) {
                TX_SEQ[i][0] = seqFrame[i % N_ZC_UP] / maxValue * SCALE;
            }
            if (SPEAKER_CHANNEL_MASK[1]) {
                TX_SEQ[i][1] = seqFrame[i % N_ZC_UP] / maxValue * SCALE;
            }
        }
    }
}
