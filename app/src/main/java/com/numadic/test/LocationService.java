package com.numadic.test;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.GpsSatellite;
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

import org.json.JSONException;
import org.json.JSONObject;

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
    private long utcTime;
    static Timer timer;
    private int satellites;
    private LocationService activity;

    public LocationService() {
        startTime = SystemClock.elapsedRealtime();
        activity = this;
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

        satellites=0;
        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        //Gets number of satellites used in fix
        GPSListener = new GpsStatus.Listener() {
            @Override
            public void onGpsStatusChanged(final int event) {
                int satellitesInfix=0;

                if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    Log.e(TAG, getString(R.string.permissions_notgranted_toast));

                else {
                    for (GpsSatellite sat : locationManager.getGpsStatus(null).getSatellites()) {
                        if (sat.usedInFix())
                            satellitesInfix++;
                    }
                    satellites = satellitesInfix;
                }
            }
        };

        //Active mode location listener
        locationListenerActive = new LocationListener() {
            public void onLocationChanged(Location location) {
                float speed = location.getSpeed() * (18 / 5);
                float accuracy = location.getAccuracy();

                /* VALIDATE
                 * Speed > 5kmph (checked in if condition)
                 * Accuracy better than 10m (included in tolerance condition)
                 * Tolerance 15m to 2m (checked in if condition)
                 * Currently has 5 satellites locked (checked in if condition)
                 * moved 50m since last data (checked at locationManager.requestLocationUpdates)
                 * GPS Provider is GPS Chip (Cannot be anything else since I'm using LocationManager.GPS_PROVIDER when requesting for location updates)
                 */

                if (speed > 5 && accuracy>2 && accuracy<15 && satellites>=5) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    String velocity = location.getSpeed()+","+location.getBearing(); //Velocity = Speed + Direction (Velocity is a vector)

                    JSONObject locationData = new JSONObject();
                    try {
                        locationData.put("latitude", latitude);
                        locationData.put("longitude", longitude);
                        locationData.put("accuracy", accuracy);
                        locationData.put("velocity", velocity);
                        locationData.put("utctime", System.currentTimeMillis());
                        locationData.put("satellite_count", satellites);

                    } catch (JSONException e) {
                        Log.e(TAG, String.valueOf(e));
                    }

                    //Write to file
                    try {
                        String filename =  "location_"+utcTime;
                        File file = new File(getApplicationContext().getFilesDir(), filename);
                        FileOutputStream outputStream;

                        //If file length > 1mb, generate new UTC timestamp
                        if(file.length()>1000000){
                            utcTime = SystemClock.elapsedRealtime();
                            filename = "location_"+utcTime;
                            outputStream = openFileOutput(filename, Context.MODE_APPEND);
                            outputStream.write((locationData.toString()+"\n").getBytes());
                            outputStream.close();
                        }

                        else {
                            outputStream = openFileOutput(filename, Context.MODE_APPEND);
                            outputStream.write((locationData.toString()+"\n").getBytes());
                            outputStream.close();
                        }
                    }
                    catch (Exception e) {
                        Log.e(TAG, String.valueOf(e));
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
                double accuracy = location.getAccuracy();

                /*VALIDATE
                 * Used same criteria as active mode
                 * Except, checking for minimum distance moved is not required in Idle mode
                 */
                if (speed < 5 && satellites>=5) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    String velocity = location.getSpeed()+","+location.getBearing();  //Velocity = Speed + Direction (Velocity is a vector)

                    JSONObject locationData = new JSONObject();
                    try {
                        locationData.put("latitude", latitude);
                        locationData.put("longitude", longitude);
                        locationData.put("accuracy", accuracy);
                        locationData.put("velocity", velocity);
                        locationData.put("utctime", System.currentTimeMillis());
                        locationData.put("satellite_count", satellites);

                    } catch (JSONException e) {
                        Log.e(TAG, String.valueOf(e));
                    }

                    //Write to file
                    try {
                        String filename =  "location_"+utcTime;
                        File file = new File(getApplicationContext().getFilesDir(), filename);
                        FileOutputStream outputStream;

                        //If file length > 1mb, generate new UTC timestamp
                        if(file.length()>1000000){
                            utcTime = SystemClock.elapsedRealtime();
                            filename = "location_"+utcTime;
                            outputStream = openFileOutput(filename, Context.MODE_APPEND);
                            outputStream.write((locationData.toString()+"\n").getBytes());
                            outputStream.close();
                        }

                        else {
                            outputStream = openFileOutput(filename, Context.MODE_APPEND);
                            outputStream.write((locationData.toString()+"\n").getBytes());
                                outputStream.close();
                        }
                    }
                    catch (Exception e) {
                        Log.e(TAG, String.valueOf(e));
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
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 120000, 50, locationListenerActive);

            //Get location updates every 30 mins (Idle Mode)
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1800000, 0, locationListenerIdle);

            //Run task to write System Data (Battery% and Network status) to a file every 10 mins
            timer = new Timer();
            WriteSystemDataTask writeSystemDataTask = new WriteSystemDataTask();
            timer.scheduleAtFixedRate(writeSystemDataTask, 0, 600000);
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
                catch (Exception e) {
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

            JSONObject systemData = new JSONObject();

            try {
                systemData.put("battery_level", batteryLevel);
                systemData.put("cell_strength", cellStrength);
                systemData.put("network_type", networkType);
                systemData.put("operator_name", operatorName);
                systemData.put("utc_time", System.currentTimeMillis());
            }

            catch (JSONException e) {
                Log.e(TAG, String.valueOf(e));
            }

            //Write to file
            try {
                String filename =  "system_"+utcTime;
                File file = new File(getApplicationContext().getFilesDir(), filename);
                FileOutputStream outputStream;

                //If file length > 1mb, generate new UTC timestamp
                if(file.length()>1000000){
                    utcTime = SystemClock.elapsedRealtime();
                    filename = "system_"+utcTime;
                    outputStream = openFileOutput(filename, Context.MODE_APPEND);
                    outputStream.write((systemData.toString()+"\n").getBytes());
                    outputStream.close();
                }

                else {
                    outputStream = openFileOutput(filename, Context.MODE_APPEND);
                    outputStream.write((systemData.toString()+"\n").getBytes());
                    outputStream.close();
                }
            }
            catch (Exception e) {
                Log.e(TAG, String.valueOf(e));
            }
        }
    }
}