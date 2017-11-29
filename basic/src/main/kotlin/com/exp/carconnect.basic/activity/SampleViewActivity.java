package com.exp.carconnect.basic.activity;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.exp.carconnect.basic.R;
import com.exp.carconnect.basic.view.SampleView;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;


public class SampleViewActivity extends AppCompatActivity {

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initDashboard();


    }


    private void initSampleView() {
        setContentView(R.layout.activity_sample_view);
        final SampleView sampleView = findViewById(R.id.sampleView);
        final SampleView sampleView1 = findViewById(R.id.sampleView1);
        final Random random = new Random();
        findViewById(R.id.startAnimation).setOnClickListener(new View.OnClickListener() {
            @SuppressLint("NewApi")
            @Override
            public void onClick(View v) {
                sampleView.setPoint(random.nextInt(100));

            }
        });
        findViewById(R.id.startAnimation1).setOnClickListener(new View.OnClickListener() {
            @SuppressLint("NewApi")
            @Override
            public void onClick(View v) {
                sampleView1.setPoint(random.nextInt(100));

            }
        });
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        sampleView.setPoint(random.nextInt(100));
                        sampleView1.setPoint(random.nextInt(100));
                    }
                });
            }
        };
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, 1000, 100);
    }


    private void initDashboard() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        setContentView(R.layout.activity_sample_dashboard);
    }
}
