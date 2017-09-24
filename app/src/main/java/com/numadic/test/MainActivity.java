package com.numadic.test;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    SharedPreferences sharedPrefs;
    SharedPreferences.Editor ed;

    @BindView(R.id.service_status) TextView serviceStatus;
    @BindView(R.id.service_time) TextView serviceTime;
    Handler timeHandler;
    Runnable time;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        updateUI();
    }

    @OnClick(R.id.start_btn)
    void StartButton(View view) {
        if (view.getId() == R.id.start_btn) {

            if(!isServiceRunning(LocationService.class)) {

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                        final int ACCESS_FINE_LOCATION_CONSTANT = 100;

                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle(getString(R.string.permissions_dialog_title));
                        builder.setMessage(getString(R.string.permissions_dialog_message));
                        builder.setPositiveButton(getString(R.string.permissions_dialog_positive_btn), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE}, ACCESS_FINE_LOCATION_CONSTANT);
                                Toast.makeText(MainActivity.this, getString(R.string.permissions_granted_toast), Toast.LENGTH_SHORT).show();
                            }
                        });
                        builder.setNegativeButton(getString(R.string.permissions_dialog_negative_btn), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                        builder.show();
                    }
                }

                else {
                    //Start location service
                    Intent serviceIntent = new Intent(getApplicationContext(), LocationService.class);
                    startService(serviceIntent);

                    //set servicestarted true in sharedPrefs so that it can start on Boot
                    sharedPrefs = getSharedPreferences("servicePrefs", MODE_PRIVATE);
                    ed = sharedPrefs.edit();
                    ed.putBoolean("serviceStarted", true);
                    ed.apply();

                    updateUI();
                    Toast.makeText(this, getString(R.string.service_started_toast), Toast.LENGTH_SHORT).show();
                }
            }

            else
                Toast.makeText(this, getString(R.string.service_running_toast), Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.stop_btn)
    void StopButton(View view) {
        if (view.getId() == R.id.stop_btn) {

            if(isServiceRunning(LocationService.class)) {

                //Stop location service
                Intent serviceIntent = new Intent(getApplicationContext(), LocationService.class);
                stopService(serviceIntent);

                //set servicestarted false in sharedPrefs so that it does not start on Boot
                sharedPrefs = getSharedPreferences("servicePrefs", MODE_PRIVATE);
                ed = sharedPrefs.edit();
                ed.putBoolean("serviceStarted", false);
                ed.apply();

                LocationService.stopLocationUpdates();
                timeHandler.removeCallbacks(time); //to stop thread that calculates service run time
                updateUI();

                Toast.makeText(this, getString(R.string.service_stopped_toast), Toast.LENGTH_SHORT).show();
            }

            else
                Toast.makeText(this, getString(R.string.service_not_running), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void updateUI(){
        if(!isServiceRunning(LocationService.class)) {
            serviceStatus.setText(getString(R.string.service_status_notrunning));
            serviceStatus.setTextColor(Color.RED);
            serviceTime.setText(getString(R.string.service_timeelapsed_null));
        }

        else {
            serviceStatus.setText(getString(R.string.service_status_running));
            serviceStatus.setTextColor(Color.GREEN);

            //Updates elapsed time of Service by continuously checking time in a new thread
            timeHandler = new Handler(getMainLooper());
            timeHandler.postDelayed(time = new Runnable() {
                @Override
                public void run() {
                    LocationService.calculateTime();
                    float hours = LocationService.elapsedSeconds / 3600;
                    float minutes = (LocationService.elapsedSeconds % 3600) / 60;
                    float seconds = LocationService.elapsedSeconds % 60;

                    String timeString = String.format("%02d:%02d:%02d", (int)hours, (int)minutes, (int)seconds);
                    serviceTime.setText(getString(R.string.service_timeelapsed)+timeString);

                    timeHandler.postDelayed(this, 1000);
                }
            }, 10);
        }
    }
}