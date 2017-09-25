package com.numadic.test;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SystemDetailActivity extends AppCompatActivity {

    final String TAG = "SystemDetailActivity";
    ArrayList<Integer> batteryLevel = new ArrayList<>();
    ArrayList<Integer> cellStrength = new ArrayList<>();
    ArrayList<String> networkType = new ArrayList<>();
    ArrayList<String> utcTime = new ArrayList<>();

    @BindView(R.id.battery_chart) BarChart batteryChart;
    @BindView(R.id.cellStrength_chart) BarChart cellStrengthChart;
    @BindView(R.id.networkType_chart) BarChart networkTypeChart;

    @BindView(R.id.legend_battery) TextView batteryLegend;
    @BindView(R.id.legend_cellstrength) TextView cellStrengthLegend;
    @BindView(R.id.legend_networktype) TextView networkTypeLegend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_detail);
        setTitle(getString(R.string.system_activity_name));
        ButterKnife.bind(this);

        getDataFromFile();
        showGraphs();
    }

    private void getDataFromFile(){
        String filename = getIntent().getStringExtra(MainActivity.FILENAME);
        String filepath = getFilesDir().toString() + "/" + filename;

        FileInputStream is;
        BufferedReader reader;
        File file = new File(filepath);

        //Get data from system health file
        if (file.exists()) {
            try {
                is = new FileInputStream(file);
                reader = new BufferedReader(new InputStreamReader(is));
                String line = reader.readLine();

                ArrayList<String> systemData = new ArrayList<>();

                while (line != null) {
                    systemData.add(line);
                    line = reader.readLine();
                }

                JSONObject LocationDataJSON;
                for(int i=0; i<systemData.size(); i++) {
                    LocationDataJSON = new JSONObject(systemData.get(i));
                    batteryLevel.add(LocationDataJSON.getInt("battery_level"));
                    cellStrength.add(LocationDataJSON.getInt("cell_strength"));
                    networkType.add(LocationDataJSON.getString("network_type"));
                    utcTime.add(LocationDataJSON.getString("utc_time"));
                }
            }
            catch (IOException | JSONException e){
                Log.e(TAG, String.valueOf(e));
            }
        }
    }

    private void showGraphs(){
        BarData batteryData = new BarData(getXAxisValues(), getBatteryDataSet());
        batteryChart.setDescription("Battery Percentage v/s Time");
        batteryChart.setData(batteryData);
        batteryChart.animateXY(2000, 2000);
        batteryChart.invalidate();

        BarData cellStrengthData = new BarData(getXAxisValues(), getCellStrengthDataSet());
        cellStrengthChart.setDescription("Cellular Signal v/s Time");
        cellStrengthChart.getAxisLeft().setStartAtZero(false);
        cellStrengthChart.getAxisRight().setStartAtZero(false);
        cellStrengthChart.setData(cellStrengthData);
        cellStrengthChart.setDrawValueAboveBar(false);
        cellStrengthChart.animateXY(2000, 2000);
        cellStrengthChart.invalidate();

        BarData networkTypeData = new BarData(getXAxisValues(), getNetworkTypeDataSet());
        networkTypeChart.setDescription("Network Type v/s Time");
        networkTypeChart.setData(networkTypeData);
        networkTypeChart.animateXY(2000, 2000);
        networkTypeChart.invalidate();

        String batLegend="", cellLegend="", networkLegend="";

        //Convert UTC milliseconds time to local time
        //Add it to legend textviews below graph
        Calendar calendar = Calendar.getInstance();
        for(int i=0; i<utcTime.size(); i++) {
            calendar.setTimeInMillis(Long.parseLong(utcTime.get(i)));
            batLegend = batLegend + (i+1)+" = "+calendar.getTime()+"\n";
            cellLegend = cellLegend + (i+1)+" = "+calendar.getTime()+"\n";
            networkLegend = networkLegend + (i+1)+" = "+calendar.getTime()+"\n";
        }

        batteryLegend.setText("X-axis: "+"\n"+batLegend);
        cellStrengthLegend.setText("X-axis: "+"\n"+cellLegend);
        networkTypeLegend.setText("X-axis: "+"\n"+networkLegend);
    }

    private ArrayList<BarDataSet> getBatteryDataSet() {
        ArrayList<BarDataSet> dataSets = new ArrayList<>();
        ArrayList<BarEntry> valueSet = new ArrayList<>();

            for (int i = 0; i < batteryLevel.size(); i++) {
                BarEntry v1e1 = new BarEntry(batteryLevel.get(i), i);
                valueSet.add(v1e1);
            }

            BarDataSet barDataSet = new BarDataSet(valueSet, "Battery %");
            dataSets.add(barDataSet);
            setColor(barDataSet);

        return dataSets;
    }

    private ArrayList<BarDataSet> getCellStrengthDataSet() {
        ArrayList<BarDataSet> dataSets = new ArrayList<>();
        ArrayList<BarEntry> valueSet = new ArrayList<>();

        for(int i=0; i<cellStrength.size(); i++){
            BarEntry v1e1 = new BarEntry(cellStrength.get(i), i);
            valueSet.add(v1e1);
        }

        BarDataSet barDataSet = new BarDataSet(valueSet, "Cell Strength (dbm)");

        dataSets.add(barDataSet);
        setColor(barDataSet);

        return dataSets;
    }

    private ArrayList<BarDataSet> getNetworkTypeDataSet() {
        ArrayList<BarDataSet> dataSets = new ArrayList<>();
        ArrayList<BarEntry> valueSet = new ArrayList<>();

        for(int i=0; i<networkType.size(); i++){

            String entry = networkType.get(i);
            int type;
            switch (entry){
                case "GSM": type=1;
                    break;
                case "WCDMA": type=2;
                    break;
                case "LTE": type=3;
                    break;
                default: type=0;
            }

            BarEntry v1e1 = new BarEntry(type, i);
            valueSet.add(v1e1);
        }

        BarDataSet barDataSet = new BarDataSet(valueSet, "Network Type (2G/3G/4G)");

        dataSets.add(barDataSet);
        setColor(barDataSet);

        return dataSets;
    }

    private ArrayList<String> getXAxisValues() {
        ArrayList<String> xAxis = new ArrayList<>();

        for(int i=1; i<=utcTime.size(); i++)
            xAxis.add(String.valueOf(i));

        return xAxis;
    }

    private void setColor(BarDataSet barDataSet){

        //Random colour generator
        Random rnd = new Random();
        int color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
        barDataSet.setColor(color);
    }
}
