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
import com.exp.carconnect.basic.view.Dashboard;
import com.exp.carconnect.basic.view.SampleView;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;


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
        final Dashboard dashboard = findViewById(R.id.dashboard);
        dashboard.setOnVINChangedListener(new Function1<String, Unit>() {
            @Override
            public Unit invoke(String s) {
                System.out.println("Vin: " + s);

                return null;
            }
        });
        dashboard.setOnOnlineChangedListener(new Function1<Boolean, Unit>() {
            @Override
            public Unit invoke(Boolean aBoolean) {
                System.out.println("Online: " + aBoolean);

                return null;
            }
        });
        dashboard.setOnIgnitionChangedListener(new Function1<Boolean, Unit>() {
            @Override
            public Unit invoke(Boolean aBoolean) {
                System.out.println("Ignition: " + aBoolean);

                return null;
            }
        });

        dashboard.setOnCheckEngineLightChangedListener(new Function1<Boolean, Unit>() {
            @Override
            public Unit invoke(Boolean aBoolean) {
                System.out.println("Check Engine Light: " + aBoolean);

                return null;
            }
        });
        dashboard.setOnFuelPercentageChangedListener(new Function1<Float, Unit>() {
            @Override
            public Unit invoke(Float aFloat) {
                System.out.println("Fuel: " + aFloat);

                return null;
            }
        });
        dashboard.setOnSpeedChangedListener(new Function1<Float, Unit>() {
            @Override
            public Unit invoke(Float aFloat) {
                System.out.println("SPEED: " + aFloat);

                return null;
            }
        });

        dashboard.setOnRPMChangedListener(new Function1<Float, Unit>() {
            @Override
            public Unit invoke(Float aFloat) {
                System.out.println("RPM: " + aFloat);
                return null;
            }
        });
        findViewById(R.id.updateRPM).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dashboard.getCurrentRPM() <= 0) {
                    dashboard.setCurrentRPM(8);
                    dashboard.setCurrentSpeed(320);
                    dashboard.setFuelPercentage(1);
                    dashboard.setShowCheckEngineLight(true);
                    dashboard.setShowIgnitionIcon(true);
                    dashboard.setOnline(true);
                    dashboard.setVin("ABCD123454321ABCD");
                } else {
                    dashboard.setCurrentRPM(0);
                    dashboard.setCurrentSpeed(0);
                    dashboard.setFuelPercentage(0);
                    dashboard.setShowCheckEngineLight(false);
                    dashboard.setShowIgnitionIcon(false);
                    dashboard.setOnline(false);
                    dashboard.setVin("EFGH123454321HGFE");
                }
            }
        });
    }
}
