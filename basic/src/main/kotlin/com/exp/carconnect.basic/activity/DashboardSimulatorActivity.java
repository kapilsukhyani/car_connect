package com.exp.carconnect.basic.activity;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.exp.carconnect.basic.R;
import com.exp.carconnect.basic.compass.CompassEvent;
import com.exp.carconnect.basic.view.Dashboard;
import com.exp.carconnect.basic.viewmodel.CompassVM;

import java.util.Random;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;


public class DashboardSimulatorActivity extends AppCompatActivity {

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initDashboard();
    }

    private Dashboard dashboard = null;
    final Random a = new Random();


    private void initDashboard() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        setContentView(R.layout.activity_dashboard);
        dashboard = findViewById(R.id.dashboard);
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

        CompassVM vm = ViewModelProviders.of(this).get(CompassVM.class);
        vm.getCompassLiveData().observe(this, new Observer<CompassEvent>() {
            @Override
            public void onChanged(@Nullable CompassEvent compassEvent) {
                Log.d("SampleViewActivity", "Current azimuth " + compassEvent.getAzimuth());
                dashboard.setCurrentAzimuth(compassEvent.getAzimuth());
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.sample_activity_menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        int i = item.getItemId();
        if (i == R.id.enableDisableTest) {
            if (dashboard.getCurrentRPM() <= 0) {
                dashboard.setCurrentRPM(a.nextInt(8));
                dashboard.setCurrentSpeed(a.nextInt(320));
                dashboard.setFuelPercentage(a.nextFloat());
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
        } else if (i == R.id.enableDisableDribble) {
            dashboard.setRpmDribbleEnabled(!dashboard.getRpmDribbleEnabled());
            dashboard.setSpeedDribbleEnabled(!dashboard.getSpeedDribbleEnabled());
        } else if (i == R.id.onlineOffline) {
            dashboard.setOnline(!dashboard.getOnline());
        } else if (i == R.id.updateRPM) {
            dashboard.setCurrentRPM(a.nextInt(8));

        } else if (i == R.id.updateSPEED) {
            dashboard.setCurrentSpeed(a.nextInt(320));
        } else if (i == R.id.updateFuel) {
            dashboard.setFuelPercentage(a.nextFloat());
        } else {
            return false;
        }

        return true;
    }
}
