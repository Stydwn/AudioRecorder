package com.example.audiorecorder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;

import com.example.audiorecorder.databinding.ActivityMainBinding;
import com.example.audiorecorder.utils.FileUtil;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private AudioPlayer audioPlayer;
    private AudioRecorder audioRecorder;
    private Boolean isPlaying = false;
    // timer
    private Handler mHandler = new Handler();
    private long timer = 0;
    private TimerRunnable timerRunnable;

    // tunable parameters
    public static final boolean[] SPEAKER_CHANNEL_MAKS = {false, true};
    public static int signal_len = 1920*10;
    public static int FS = 48000;
    public static int FC = 19000;
    public static int BW = 4000;
    public static int N_ZC = signal_len * BW / FS - 1;
    public static int ZC_ROOT = (N_ZC - 1) / 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupPermission();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        audioPlayer = new AudioPlayer();
        audioRecorder = new AudioRecorder();
        FileUtil.init(this);

        final Button button = binding.button;
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isPlaying){
                    isPlaying = false;
                    audioPlayer.stop();
                    audioRecorder.stop();

                    mHandler.removeCallbacks(timerRunnable);
                    timerRunnable = null;

                    button.setText("Start");
                }else{
                    isPlaying = true;
                    audioPlayer.start();
                    audioRecorder.start();

                    timer = System.currentTimeMillis() + TimeZone.getDefault().getRawOffset();
                    timerRunnable= new TimerRunnable();
                    mHandler.postDelayed(timerRunnable, 0);

                    binding.timer.setText("00:00:00");
                    button.setText("Stop");
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(isPlaying){
            if(timerRunnable == null){
                timerRunnable= new TimerRunnable();
                mHandler.postDelayed(timerRunnable, 0);
            }
        }
    }

    // ******************************
    // setup permissions
    // ******************************
    // required permissions
    public static final String[] PERMISSIONS = {
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private void setupPermission() {
        while (getPermission() != PackageManager.PERMISSION_GRANTED) {
            setupPermission();
        }
    }

    private int getPermission() {
        ActivityCompat.requestPermissions(this, PERMISSIONS, 1);

        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return PackageManager.PERMISSION_DENIED;
            }
        }

        return PackageManager.PERMISSION_GRANTED;
    }

    private SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
    private int DELAY = 1000;
    private class TimerRunnable implements Runnable {
        @Override
        public void run() {
            binding.timer.setText(formatter.format(System.currentTimeMillis()-timer));
            mHandler.postDelayed(this, DELAY);
        }
    };
}