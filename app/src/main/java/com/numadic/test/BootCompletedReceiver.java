package com.numadic.test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import static android.content.Context.MODE_PRIVATE;

/*  This class implements a BroacastReceiver
 *  that receives an intent from the system when boot is completed
 *  and starts the location service
 */
public class BootCompletedReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {

        SharedPreferences sharedPrefs = context.getSharedPreferences("servicePrefs", MODE_PRIVATE);

        if(sharedPrefs.contains("serviceStarted")) {
            if (sharedPrefs.getBoolean("serviceStarted", false)) {
                Intent serviceIntent = new Intent(context, LocationService.class);
                context.startService(serviceIntent);
            }
        }
    }
}
