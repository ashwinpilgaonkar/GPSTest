package com.numadic.test;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class LocationDetailActivity extends AppCompatActivity implements OnMapReadyCallback, OnMarkerClickListener {

    private GoogleMap mMap;
    private final String TAG = "LocationDetailActivity";

    @BindView(R.id.waypoint_details_text) TextView waypointDetailsText;
    @BindView(R.id.play_route_btn) Button playRoute;
    private Handler playRouteHandler = new Handler();
    private boolean routePlaying = false;
    private Runnable routeRun;

    private ArrayList<Double> latitude = new ArrayList<>();
    private ArrayList<Double> longitude = new ArrayList<>();
    private ArrayList<Double> accuracy = new ArrayList<>();
    private ArrayList<String> velocity = new ArrayList<>();
    private ArrayList<String> utcTime = new ArrayList<>();
    private ArrayList<Double> satelliteCount = new ArrayList<>();
    private ArrayList<Marker> markersList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_detail);
        ButterKnife.bind(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        getDatafromFile();
    }

    @OnClick(R.id.play_route_btn)
    void playRouteButton(View view) {
        if (view.getId() == R.id.play_route_btn) {

            if(routePlaying) {
                playRouteHandler.removeCallbacks(routeRun);
                routePlaying = false;
                playRoute.setText(getString(R.string.play_route_btn_text));
            }

            else {
                routeRun = new Runnable() {
                    int i = 0;

                    @Override
                    public void run() {
                        routePlaying = true;
                        playRoute.setText(getString(R.string.stop_route_btn_text));
                        if (i < markersList.size()) {
                            markersList.get(i).showInfoWindow();
                            float zoom = mMap.getCameraPosition().zoom;
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude.get(i), longitude.get(i)), zoom));
                            i++;
                        } else {
                            playRoute.setText(getString(R.string.play_route_btn_text));
                            return;
                        }

                        playRouteHandler.postDelayed(this, 1000);
                    }
                };

                playRouteHandler.postDelayed(routeRun, 1000);
            }
        }
    }

    private void getDatafromFile(){
        String filename = getIntent().getStringExtra(MainActivity.FILENAME);
        String filepath = getFilesDir().toString() + "/" + filename;

        FileInputStream is;
        BufferedReader reader;
        File file = new File(filepath);

        //Read data from file and store it in ArrayLists
        if (file.exists()) {
            try {
                is = new FileInputStream(file);
                reader = new BufferedReader(new InputStreamReader(is));
                String line = reader.readLine();

                ArrayList<String> locationData = new ArrayList<>();

                while (line != null) {
                    locationData.add(line);
                    line = reader.readLine();
                }

                JSONObject LocationDataJSON;
                for(int i=0; i<locationData.size(); i++) {
                    LocationDataJSON = new JSONObject(locationData.get(i));
                    latitude.add(LocationDataJSON.getDouble("latitude"));
                    longitude.add(LocationDataJSON.getDouble("longitude"));
                    accuracy.add(LocationDataJSON.getDouble("accuracy"));
                    velocity.add(LocationDataJSON.getString("velocity"));
                    utcTime.add(LocationDataJSON.getString("utctime"));
                    satelliteCount.add(LocationDataJSON.getDouble("satellite_count"));
                }
            }
            catch (IOException | JSONException e){
                Log.e(TAG, String.valueOf(e));
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

            MarkerOptions options = new MarkerOptions();
            Marker marker;

            //Add markers to map
            for (int i = 0; i < latitude.size(); i++) {
                LatLng locations = new LatLng(latitude.get(i), longitude.get(i));
                options.position(locations);

                if (mMap != null) {

                    //Set colour of origin and destination waypoint as different from the others
                    if(i==0 || i==latitude.size()-1) {
                        marker = mMap.addMarker(options.position(locations)
                                .title("Waypoint " + (i + 1)));
                        markersList.add(marker);
                    }

                    else {
                        marker = mMap.addMarker(options.position(locations)
                                .title("Waypoint " + (i + 1)));
                        marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
                        markersList.add(marker);
                    }
                }
            }

        //Hack to split requests if number of waypoints are > 8
        //Google Services API (Free) has a limitation of 8 waypoints per request
            if(latitude.size()>7) {
                ArrayList<Double> lat;
                ArrayList<Double> lon;

                int iterations = latitude.size() / 7;
                int start = 0;
                int end = 7;

                for (int j = 0; j <= iterations; j++) {
                    lat = new ArrayList<>();
                    lon = new ArrayList<>();
                    for (int i = start; i < end; i++) {
                        lat.add(latitude.get(i));
                        lon.add(longitude.get(i));
                    }
                    String url = getMapsApiDirectionsUrl(lat, lon);
                    ReadTask downloadTask = new ReadTask();
                    downloadTask.execute(url);
                    start = end - 1;
                    if (end + 7 > latitude.size())
                        end = latitude.size();

                    else
                        end = end + 7;

                }
            }

            else {
                String url = getMapsApiDirectionsUrl(latitude, longitude);
                ReadTask downloadTask = new ReadTask();
                downloadTask.execute(url);
            }

            //Set camera to first waypoint as default
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude.get(0), longitude.get(0)), 13));
            mMap.setOnMarkerClickListener(this);
        }

    private String getMapsApiDirectionsUrl(ArrayList<Double> latitude, ArrayList<Double> longitude) {

        String waypoints = "waypoints=optimize:true|";

        for(int i=0; i<latitude.size(); i++)
            waypoints = waypoints + latitude.get(i) + "," + longitude.get(i) + "|";

        //Remove last "|" character from string
        waypoints = waypoints.substring(0, waypoints.length() - 1);

        String origin = "origin="
                +latitude.get(0) + "," + longitude.get(0);

        String destination = "destination="
                +latitude.get(latitude.size()-1) + "," + longitude.get(longitude.size()-1);

        String sensor = "sensor=false";
        String params = origin + "&" + destination + "&" + waypoints + "&" + sensor;
        String output = "json";
        return "https://maps.googleapis.com/maps/api/directions/" + output + "?" + params;
    }

    @Override
    public boolean onMarkerClick(com.google.android.gms.maps.model.Marker marker) {
        //get marker position
        //getId returns a string in the format m<number>
        //so strip first character and convert the resultant substring to an Integer
        int position = Integer.parseInt(marker.getId().substring(1));;
        String vel = velocity.get(position);
        String speed = vel.substring(0, vel.indexOf(','));
        String direction = vel.substring(vel.indexOf(',')+1);

        //Convert UTC milliseconds to current local time
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(Long.parseLong(utcTime.get(position)));
        Date date = calendar.getTime();

        String waypointText = "Waypoint "+(position+1)+": "
                            +"\n"+"Latitute: "+latitude.get(position)
                            +"\n"+"Longitude: "+longitude.get(position)
                            +"\n"+"Speed: "+speed+" km/h"
                            +"\n"+date;
        waypointDetailsText.setText(waypointText);
        marker.setSnippet("Direction: "+direction+" degrees");
        marker.showInfoWindow();

        //Center camera to position of selected marker
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude.get(position), longitude.get(position)), 17));
        return true;
    }

    private class ReadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... url) {
            String data = "";
            try {
                HTTPConnection http = new HTTPConnection();
                data = http.readUrl(url[0]);
            }
            catch (Exception e) {
                Log.e(TAG, e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.d(TAG, result);
            new ParserTask().execute(result);
        }
    }

    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        private final String TAG = "ParserTask";

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(
                String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                PathJSONParser parser = new PathJSONParser();
                routes = parser.parse(jObject);
            } catch (Exception e) {
                Log.e(TAG, String.valueOf(e));
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> routes) {
            ArrayList<LatLng> points;
            PolylineOptions polyLineOptions = null;

            // traverse through routes
            for (int i=0; i<routes.size(); i++) {
                points = new ArrayList<>();
                polyLineOptions = new PolylineOptions();
                List<HashMap<String, String>> path = routes.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                //Add Blue line connecting all the waypoints
                polyLineOptions.addAll(points);
                polyLineOptions.width(10);
                polyLineOptions.color(Color.BLUE);
            }

            if(null == polyLineOptions)
                Log.e(TAG, "Internet Connection problem");

            else {
                mMap.addPolyline(polyLineOptions);
                //Enable Play route button once route has been set
                playRoute.setEnabled(true);
            }
        }
    }
}