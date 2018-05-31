package com.exp.carconnect.dashboard.activity;

import android.app.Dialog;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.exp.carconnect.dashboard.R;
import com.exp.carconnect.dashboard.view.Dashboard;

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
        setContentView(R.layout.view_dashboard);
        dashboard = findViewById(R.id.dashboard);
        dashboard.setOnVINChangedListener(new Function1<String, Unit>() {
            @Override
            public Unit invoke(String s) {
//                System.out.println("Vin: " + s);

                return null;
            }
        });
        dashboard.setOnOnlineChangedListener(new Function1<Boolean, Unit>() {
            @Override
            public Unit invoke(Boolean aBoolean) {
//                System.out.println("Online: " + aBoolean);

                return null;
            }
        });
        dashboard.setOnIgnitionChangedListener(new Function1<Boolean, Unit>() {
            @Override
            public Unit invoke(Boolean aBoolean) {
//                System.out.println("Ignition: " + aBoolean);

                return null;
            }
        });

        dashboard.setOnCheckEngineLightChangedListener(new Function1<Boolean, Unit>() {
            @Override
            public Unit invoke(Boolean aBoolean) {
//                System.out.println("Check Engine Light: " + aBoolean);

                return null;
            }
        });
        dashboard.setOnFuelPercentageChangedListener(new Function1<Float, Unit>() {
            @Override
            public Unit invoke(Float aFloat) {
//                System.out.println("Fuel: " + aFloat);

                return null;
            }
        });
        dashboard.setOnSpeedChangedListener(new Function1<Float, Unit>() {
            @Override
            public Unit invoke(Float aFloat) {
//                System.out.println("SPEED: " + aFloat);

                return null;
            }
        });

        dashboard.setOnRPMChangedListener(new Function1<Float, Unit>() {
            @Override
            public Unit invoke(Float aFloat) {
//                System.out.println("RPM: " + aFloat);
                return null;
            }
        });

        dashboard.setOnRPMGaugeCLickListener(new Function1<Float, Unit>() {
            @Override
            public Unit invoke(Float aFloat) {
                Toast.makeText(DashboardSimulatorActivity.this, "rpm clicked: " + aFloat, Toast.LENGTH_LONG).show();
                return null;
            }
        });

        dashboard.setOnIgnitionIconCLickListener(new Function1<Boolean, Unit>() {
            @Override
            public Unit invoke(Boolean aFloat) {
                Toast.makeText(DashboardSimulatorActivity.this, "ignition clicked: " + aFloat, Toast.LENGTH_LONG).show();
                return null;
            }
        });
        dashboard.setOnCheckEngineLightIconCLickListener(new Function1<Boolean, Unit>() {
            @Override
            public Unit invoke(Boolean aFloat) {
                Toast.makeText(DashboardSimulatorActivity.this, "check engine clicked: " + aFloat, Toast.LENGTH_LONG).show();
                return null;
            }
        });
        dashboard.setOnSpeedStripClickListener(new Function1<Float, Unit>() {
            @Override
            public Unit invoke(Float aFloat) {
                Toast.makeText(DashboardSimulatorActivity.this, "speed clicked: " + aFloat, Toast.LENGTH_LONG).show();
                return null;
            }
        });

        dashboard.setOnFuelIconClickListener(new Function1<Float, Unit>() {
            @Override
            public Unit invoke(Float aFloat) {
                Toast.makeText(DashboardSimulatorActivity.this, "fuel clicked: " + aFloat, Toast.LENGTH_LONG).show();
                return null;
            }
        });

        dashboard.setOnAirIntakeTempClickListener(new Function1<Float, Unit>() {
            @Override
            public Unit invoke(Float aFloat) {
                Toast.makeText(DashboardSimulatorActivity.this, "air intake clicked: " + aFloat, Toast.LENGTH_LONG).show();
                return null;
            }
        });

        dashboard.setOnAmbientTempClickListener(new Function1<Float, Unit>() {
            @Override
            public Unit invoke(Float aFloat) {
                Toast.makeText(DashboardSimulatorActivity.this, "ambient clicked: " + aFloat, Toast.LENGTH_LONG).show();
                return null;
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
                dashboard.setCurrentAirIntakeTemp(a.nextInt(65));
                dashboard.setCurrentAmbientTemp(a.nextInt(55));
            } else {
                dashboard.setCurrentRPM(0);
                dashboard.setCurrentSpeed(0);
                dashboard.setFuelPercentage(0);
                dashboard.setShowCheckEngineLight(false);
                dashboard.setShowIgnitionIcon(false);
                dashboard.setOnline(false);
                dashboard.setVin("EFGH123454321HGFE");
                dashboard.setCurrentAirIntakeTemp(0);
                dashboard.setCurrentAmbientTemp(0);
            }
        } else if (i == R.id.enableDisableDribble) {
            dashboard.setRpmDribbleEnabled(!dashboard.getRpmDribbleEnabled());
            dashboard.setSpeedDribbleEnabled(!dashboard.getSpeedDribbleEnabled());
        } else if (i == R.id.onlineOffline) {
            dashboard.setOnline(!dashboard.getOnline());
        } else if (i == R.id.updateRPM) {
            showDialog("RPM", new Function1<String, String>() {
                @Override
                public String invoke(String s) {
                    if (s.equals("RANDOM")) {
                        dashboard.setCurrentRPM(a.nextInt(8));
                        return null;

                    }
                    dashboard.setCurrentRPM(Integer.valueOf(s));
                    return null;
                }
            });

        } else if (i == R.id.updateSPEED) {
            showDialog("SPEED", new Function1<String, String>() {
                @Override
                public String invoke(String s) {
                    if (s.equals("RANDOM")) {
                        dashboard.setCurrentSpeed(a.nextInt(320));
                        return null;

                    }
                    dashboard.setCurrentSpeed(Integer.valueOf(s));
                    return null;
                }
            });
        } else if (i == R.id.updateFuel) {

            showDialog("Fuel", new Function1<String, String>() {
                @Override
                public String invoke(String s) {
                    if (s.equals("RANDOM")) {
                        dashboard.setFuelPercentage(a.nextFloat());
                        return null;

                    }
                    dashboard.setFuelPercentage(Float.valueOf(s));
                    return null;
                }
            });

        } else if (i == R.id.hideShowSideGauges) {
            dashboard.setShowSideGauges(!dashboard.getShowSideGauges());
        } else if (i == R.id.updateCurrentAirIntake) {


            showDialog("AIR-INTAKE", new Function1<String, String>() {
                @Override
                public String invoke(String s) {
                    if (s.equals("RANDOM")) {
                        dashboard.setCurrentAirIntakeTemp(a.nextInt(65));
                        return null;

                    }
                    dashboard.setCurrentAirIntakeTemp(Integer.valueOf(s));
                    return null;
                }
            });
        } else if (i == R.id.updateCurrentAmbient) {
            showDialog("AMBIENT", new Function1<String, String>() {
                @Override
                public String invoke(String s) {
                    if (s.equals("RANDOM")) {
                        dashboard.setCurrentAmbientTemp(a.nextInt(55));
                        return null;

                    }
                    dashboard.setCurrentAmbientTemp(Integer.valueOf(s));
                    return null;
                }
            });

        } else {
            return false;
        }

        return true;
    }

    private Dialog dialog;

    private void showDialog(String attributeName, final Function1<String, String> callback) {
        View view = getLayoutInflater().inflate(R.layout.field_setter, null);
        final EditText valueField = view.findViewById(R.id.value);
        valueField.setHint("Add a value for " + attributeName);
        view.findViewById(R.id.setRandomButton)
                .setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                callback.invoke("RANDOM");
                                dialog.dismiss();
                            }
                        }

                );
        view.findViewById(R.id.setFieldButton)
                .setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                callback.invoke(valueField.getText().toString());
                                dialog.dismiss();
                            }
                        }

                );
        dialog = new AlertDialog.Builder(this).setView(view).show();

    }


}
