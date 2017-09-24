package com.numadic.test;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Timer;
import java.util.TimerTask;

public class LocationService extends Service {

    String TAG = "LocationService";

    static GpsStatus.Listener GPSListener;
    static LocationManager locationManager;
    static LocationListener locationListenerActive;
    static LocationListener locationListenerIdle;
    static long startTime;
    static long elapsedSeconds;
    long utcTime;
    static Timer timer;
    GpsStatus status;

    public LocationService() {
        startTime = SystemClock.elapsedRealtime();
    }

    public static void stopLocationUpdates() {
        locationManager.removeGpsStatusListener(GPSListener);
        locationManager.removeUpdates(locationListenerActive);
        locationManager.removeUpdates(locationListenerIdle);
        timer.cancel();
    }

    public static void calculateTime() {
        long endTime = SystemClock.elapsedRealtime();
        elapsedSeconds = (endTime - startTime) / 1000;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        //Get UTC time when service starts
        utcTime = System.currentTimeMillis();

        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        GPSListener = new GpsStatus.Listener() {
            @Override
            public void onGpsStatusChanged(final int event) {
                Log.e(TAG, "---onGpsStatusChanged---");
                switch (event) {
                    case GpsStatus.GPS_EVENT_STARTED:
                        Log.e(TAG, "GPS_EVENT_STARTED");
                        break;
                    case GpsStatus.GPS_EVENT_FIRST_FIX:
                        Log.e(TAG, "GPS_EVENT_FIRST_FIX");
                        break;
                    case GpsStatus.GPS_EVENT_STOPPED:
                        Log.e(TAG, "GPS_EVENT_STOPPED");
                        break;
                    case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                        Log.e(TAG, "GPS_EVENT_SATELLITE_STATUS");
                        break;
                    default:
                        Log.e(TAG, "SOMETHING_ELSE");
                        break;
                }
            }
        };

        //Active mode location listener
        locationListenerActive = new LocationListener() {
            public void onLocationChanged(Location location) {
                float speed = location.getSpeed() * (18 / 5);
                float accuracy = location.getAccuracy();

                //Validate
                if (speed > 5 && accuracy < 10) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    //accuracy//
                    //Velocity = Speed + Direction (Velocity is a vector)
                    String velocity = location.getSpeed()+","+location.getBearing();
                    //utc time//
                    double satellites;

                    //Write to file
                    try {
                        String filename =  "location_"+utcTime;
                        File file = new File(getApplicationContext().getFilesDir(), filename);
                        FileOutputStream outputStream;

                        //If file length > 1mb, generate new UTC timestamp
                        if(file.length()>1000000){
                            utcTime = SystemClock.elapsedRealtime();
                            filename = "location_"+utcTime;
                            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                            outputStream.write("ASD".getBytes());
                            outputStream.close();
                        }

                        else {
                            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                            outputStream.write("ASD".getBytes());
                            outputStream.close();
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
            }
            @Override
            public void onProviderEnabled(String s) {
            }
            @Override
            public void onProviderDisabled(String s) {
            }
        };

        //Idle mode location listener
        locationListenerIdle = new LocationListener() {
            public void onLocationChanged(Location location) {
                float speed = location.getSpeed() * (18 / 5);

                //Validate
                if (speed < 5) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    double accuracy = location.getAccuracy();
                    //Velocity = Speed + Direction (Velocity is a vector)
                    String velocity = location.getSpeed()+","+location.getBearing();
                    //utc time//
                    double satellites;

                    //Write to file
                    try {
                        String filename =  "system_"+utcTime;
                        File file = new File(getApplicationContext().getFilesDir(), filename);
                        FileOutputStream outputStream;

                        //If file length > 1mb, generate new UTC timestamp
                        if(file.length()>1000000){
                            utcTime = SystemClock.elapsedRealtime();
                            filename = "system_"+utcTime;
                            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                            outputStream.write("ASD".getBytes());
                            outputStream.close();
                        }

                        else {
                            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                            outputStream.write("ASD".getBytes());
                            outputStream.close();
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
            }
            @Override
            public void onProviderEnabled(String s) {
            }
            @Override
            public void onProviderDisabled(String s) {
            }
        };

        //if location permission is not granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, getString(R.string.permissions_notgranted_toast));
        } else {

            //Get Satellite info from GPS
            locationManager.addGpsStatusListener(GPSListener);

            //Get location updates every 2 mins and 50m (Active Mode)
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListenerActive);

            //Get location updates every 30 mins (Idle Mode)
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1800000, 0, locationListenerIdle);

            //Run task to write System Data (Battery% and Network status) to a file every 10 mins
            timer = new Timer();
            WriteSystemDataTask writeSystemDataTask = new WriteSystemDataTask();
            timer.scheduleAtFixedRate(writeSystemDataTask, 0, 1000);
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Exception");
    }

    private class WriteSystemDataTask extends TimerTask {

        int getBatteryPercentage() {

            IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = getApplicationContext().registerReceiver(null, iFilter);

            int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
            int scale = (batteryStatus != null) ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;

            float batteryPct = level / (float) scale;

            return (int) (batteryPct * 100);
        }

        String getSIMOperatorName(){
            TelephonyManager telephonyManager = ((TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE));
            return telephonyManager.getSimOperatorName();
        }

        int getCellStrength(){
            //Check if Airplane Mode is on to prevent app crash when getting cell strength
            if(Settings.Global.getInt(getApplicationContext().getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) == 0) {
                TelephonyManager telephonyManager = ((TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE));

                try {
                    switch (getNetworkType()) {
                        case "GSM":
                            CellInfoGsm cellinfogsm = (CellInfoGsm) telephonyManager.getAllCellInfo().get(0);
                            CellSignalStrengthGsm cellSignalStrengthGsm = cellinfogsm.getCellSignalStrength();
                            return cellSignalStrengthGsm.getDbm();

                        case "WCDMA":
                            CellInfoWcdma cellinfowcdma = (CellInfoWcdma) telephonyManager.getAllCellInfo().get(0);
                            CellSignalStrengthWcdma cellSignalStrengthWcdma = cellinfowcdma.getCellSignalStrength();
                            return cellSignalStrengthWcdma.getDbm();

                        case "LTE":
                            CellInfoLte cellinfolte = (CellInfoLte) telephonyManager.getAllCellInfo().get(0);
                            CellSignalStrengthLte cellSignalStrengthLte = cellinfolte.getCellSignalStrength();
                            return cellSignalStrengthLte.getDbm();

                        default:
                            return 0;
                    }
                }
                //Sometimes when switching networks, getNetworkType() responds to changes faster than getAllCellInfo().get(0)
                //Which throws a ClassCastException. In that case, return 0.
                catch (ClassCastException e) {
                    return 0;
                }
            }
            else
                return 0;
        }

        String getNetworkType(){
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

            //Mode 0 occurs when switching between networks
            switch (telephonyManager.getNetworkType()){
                case 2: return "GSM";
                case 10: return "WCDMA";
                case 13: return "LTE";
                case 0: return "NetworkSwitching";
                default: return "Undefined";
            }
        }

        @Override
        public void run() {
            int batteryLevel = getBatteryPercentage();
            int cellStrength = getCellStrength();
            String networkType = getNetworkType();
            String operatorName = getSIMOperatorName();

           // Log.e(TAG, batteryLevel+" "+cellStrength+" "+networkType+" "+operatorName);
        }
    }
}