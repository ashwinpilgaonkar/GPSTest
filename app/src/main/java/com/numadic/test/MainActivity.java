package com.numadic.test;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    SharedPreferences sharedPrefs;
    SharedPreferences.Editor ed;

    @BindView(R.id.service_status) TextView serviceStatus;
    @BindView(R.id.service_time) TextView serviceTime;
    @BindView(R.id.location_files) ListView locationFiles;
    @BindView(R.id.system_files) ListView systemFiles;
    Handler timeHandler;
    Runnable time;
    boolean network=false;

    final static String FILENAME = "filename";
    final static String LOCATION = "location";
    final static String SYSTEM = "system";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        updateServiceUI();
        updateLocationFileUI();
        updateSystemFileUI();
    }

    @OnClick(R.id.start_btn)
    void StartButton(View view) {
        if (view.getId() == R.id.start_btn) {

            if (!isServiceRunning(LocationService.class)) {

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            final int ACCESS_FINE_LOCATION_CONSTANT = 100;

                            AlertDialog.Builder builder = new AlertDialog.Builder(this);
                            builder.setTitle(getString(R.string.permissions_dialog_title));
                            builder.setMessage(getString(R.string.permissions_dialog_message));
                            builder.setPositiveButton(getString(R.string.permissions_dialog_positive_btn), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_FINE_LOCATION_CONSTANT);
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
                    else {
                        //Start location service
                        Intent serviceIntent = new Intent(getApplicationContext(), LocationService.class);
                        startService(serviceIntent);

                        //set servicestarted true in sharedPrefs so that it can start on Boot
                        sharedPrefs = getSharedPreferences("servicePrefs", MODE_PRIVATE);
                        ed = sharedPrefs.edit();
                        ed.putBoolean("serviceStarted", true);
                        ed.apply();

                        updateServiceUI();
                        Toast.makeText(this, getString(R.string.service_started_toast), Toast.LENGTH_SHORT).show();
                    }
                } else
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
                updateServiceUI();

                Toast.makeText(this, getString(R.string.service_stopped_toast), Toast.LENGTH_SHORT).show();
            }

            else
                Toast.makeText(this, getString(R.string.service_not_running), Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.refresh_btn)
    void RefreshButton(View view) {
        if (view.getId() == R.id.refresh_btn) {
            updateLocationFileUI();
            updateSystemFileUI();
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

    private boolean checkNetworkConnectivity() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        //Check for Network Connectivity to prevent app crash in case no network connectivity is available
        if(!(activeNetworkInfo != null && activeNetworkInfo.isConnected())) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(false);
            builder.setTitle(getString(R.string.network_connectivity_error_title));
            builder.setMessage(getString(R.string.network_connectivity_error_message));

            builder.setPositiveButton(getString(R.string.network_connectivity_error_positive_btn),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

            builder.show();
            network = false;
        }
        else {
            network = true;
        }
        return network;
    }

    public void updateServiceUI(){
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

    public void updateLocationFileUI(){
        String path = getFilesDir().toString();
        File f = new File(path);
        File file[] = f.listFiles();
        final ArrayList<String> filenames = new ArrayList<>();

        for (File locationData : file) {
            if(locationData.getName().contains(LOCATION))
                filenames.add(locationData.getName());
        }

        ArrayAdapter<String> systemFilesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, filenames);
        locationFiles.setAdapter(systemFilesAdapter);

        //intercept scroll events to separate parent ScrollView scrolling from ListView scrolling
        locationFiles.setOnTouchListener(new ListView.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        // Disallow ScrollView to intercept touch events.
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        break;

                    case MotionEvent.ACTION_UP:
                        // Allow ScrollView to intercept touch events.
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }

                // Handle ListView touch events.
                v.onTouchEvent(event);
                return true;
            }
        });

        locationFiles.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if(checkNetworkConnectivity()) {
                    Intent intent = new Intent(MainActivity.this, LocationDetailActivity.class);
                    intent.putExtra(FILENAME, filenames.get(position));
                    startActivity(intent);
                }
            }
        });
    }

    public void updateSystemFileUI(){
        String path = getFilesDir().toString();
        File f = new File(path);
        File file[] = f.listFiles();
        final ArrayList<String> filenames = new ArrayList<>();

        for (File systemData : file) {
            if(systemData.getName().contains(SYSTEM))
            filenames.add(systemData.getName());
        }

        ArrayAdapter<String> systemFilesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, filenames);
        systemFiles.setAdapter(systemFilesAdapter);

        //intercept scroll events to separate parent ScrollView scrolling from ListView scrolling
        systemFiles.setOnTouchListener(new ListView.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        // Disallow ScrollView to intercept touch events.
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        break;

                    case MotionEvent.ACTION_UP:
                        // Allow ScrollView to intercept touch events.
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }

                // Handle ListView touch events.
                v.onTouchEvent(event);
                return true;
            }
        });

        systemFiles.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, SystemDetailActivity.class);
                intent.putExtra(FILENAME, filenames.get(position));
                startActivity(intent);
            }
        });
    }
}