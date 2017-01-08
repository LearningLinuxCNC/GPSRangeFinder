package com.jecenterprises.shootingrangefinder;

//import android.*;
import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
//import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

//import com.google.android.gms.location.places.ui.PlaceAutocomplete;
//import com.google.android.gms.common.api.BooleanResult;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
//import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
//import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
//import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
//import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

//import com.google.android.gms.vision.text.Text;

import java.util.ArrayList;
//import java.util.List;

public class MapsActivity extends FragmentActivity
        implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        GoogleMap.OnMarkerClickListener{

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Location FiringLine;
    private Marker flMarker;
    private Circle flCircle;
    private ArrayList<Location> Targets = new ArrayList<>();
    private ArrayList<Marker> TargetMarkers = new ArrayList<>();
    private Polyline lastPL;
    private Marker lastMarker;
    private Marker lastSelMarker;
    private LocationRequest mLocationRequest;
    private TextView tvLatLong;
    private TextView tvElevation;
    private TextView tvAccuracy;
    private TextView tvSlope;
    private TextView tvDistance;
    private TextView tvCurrent;
    private TextView tvAzimuth;
    private Button buttonSetFL;
    private Button buttonAddTarget;
    private ToggleButton tbTracking;
    private ToggleButton tbUnits;
    private boolean tracking = true;
    private ArrayList<Location> compLocs = new ArrayList<>();
    private double deltathreshold = 0.5;  //this can be a setting to determine how accurate the lock position has to be
    private float camerazoom;
    private double cameralong;
    private double cameralat;
    private String unit = "yards";
    private boolean showalert = true;

    /* GPS Constant Permission */
    private static final int MY_PERMISSION_ACCESS_COARSE_LOCATION = 11;
    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 12;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Create an instance of GoogleAPIClient.
        // ATTENTION: This "addApi(AppIndex.API)"was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(AppIndex.API).build();

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(2000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSION_ACCESS_FINE_LOCATION);
        }

        tvLatLong = (TextView) findViewById(R.id.textView_curLatLong);
        tvAccuracy = (TextView) findViewById(R.id.textView_curAccuracy);
        tvElevation = (TextView) findViewById(R.id.textView_curElevation);
        tvSlope = (TextView) findViewById(R.id.textView_curSlope);
        tvDistance = (TextView) findViewById(R.id.textView_curDistance);
        tvCurrent = (TextView) findViewById(R.id.textView_curLoc);
        tvAzimuth = (TextView) findViewById(R.id.textView_curAzimuth);
        tbTracking = (ToggleButton) findViewById(R.id.toggleButton_track);
        buttonAddTarget = (Button) findViewById(R.id.button_add_target);
        buttonSetFL = (Button) findViewById(R.id.button_set_FL);
        tbUnits = (ToggleButton) findViewById(R.id.toggleButton_units);

        setButtonsUnlocked();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_ACCESS_COARSE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                } else {
                    // permission denied
                }
                break;
            }

            case MY_PERMISSION_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                } else {
                    // permission denied
                }
                break;
            }

        }

    }


    @Override
    protected void onPause() {
        super.onPause();
        if(mGoogleApiClient.isConnected()){
            stopLocationUpdates();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //Boolean mRequestingLocationUpdates = LocationServices.FusedLocationApi.
        //if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
        // Not sure how to handle the mRequestingLocationUpdates boolean variable.

        if (mGoogleApiClient.isConnected()){
            startLocationUpdates();
        }
    }

    protected void startLocationUpdates() {
        if ( Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED) {
            return  ;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        cameralat = mMap.getCameraPosition().target.latitude;
        cameralong = mMap.getCameraPosition().target.longitude;
        camerazoom = mMap.getCameraPosition().zoom;

        savedInstanceState.putBoolean("tracking", tracking);
        savedInstanceState.putParcelableArrayList("compLocs", compLocs);
        savedInstanceState.putParcelable("FiringLine", FiringLine);
        savedInstanceState.putParcelableArrayList("Targets", Targets);
        savedInstanceState.putDouble("cameralat",cameralat);
        savedInstanceState.putDouble("cameralong",cameralong);
        savedInstanceState.putFloat("camerazoom",camerazoom);
        savedInstanceState.putString("unit",unit);
        savedInstanceState.putBoolean("showalert",showalert);

    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore UI state from the savedInstanceState.
        // This bundle has also been passed to onCreate.
        tracking = savedInstanceState.getBoolean("tracking");
        compLocs = savedInstanceState.getParcelableArrayList("compLocs");
        FiringLine = savedInstanceState.getParcelable("FiringLine");
        Targets = savedInstanceState.getParcelableArrayList("Targets");
        camerazoom = savedInstanceState.getFloat("camerazoom");
        cameralat = savedInstanceState.getDouble("cameralat");
        cameralong = savedInstanceState.getDouble("cameralong");
        unit = savedInstanceState.getString("unit");
        showalert = savedInstanceState.getBoolean("showalert");

    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.start(mGoogleApiClient, getIndexApiAction());
    }


    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();// ATTENTION: This was auto-generated to implement the App Indexing API.
// See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(mGoogleApiClient, getIndexApiAction());
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        if(FiringLine != null){
            LatLng fl = new LatLng(FiringLine.getLatitude(),FiringLine.getLongitude());
            flMarker = mMap.addMarker(new MarkerOptions()
                    .position(fl)
                    .title("Firing Line")
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.firing_line)));
            flCircle = mMap.addCircle(new CircleOptions()
                    .center(fl)
                    .radius(FiringLine.getAccuracy())
                    .strokeColor(Color.BLUE)
                    .fillColor(Color.BLUE));
        }
        if(Targets.size() > 0){
            double targetDistance;
            LatLng target;
            for(int i = 0; i < Targets.size(); i++){
                target = new LatLng(Targets.get(i).getLatitude(),Targets.get(i).getLongitude());
                if (FiringLine != null){
                    targetDistance = FiringLine.distanceTo(Targets.get(i));
                } else targetDistance = 0.0;
                TargetMarkers.add(mMap.addMarker(new MarkerOptions()
                        .position(target)
                        .title("Target " + (i+1))
                        .snippet(calcDistance(targetDistance,"yards"))
                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.target)))
                );
            }
        }
        //LatLng mCurrentLatLng = new LatLng(cameralat,cameralong);
        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mCurrentLatLng,camerazoom));

        if(showalert) {
            AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
            builder1.setTitle(getString(R.string.warning));
            builder1.setMessage(getString(R.string.warning_msg));
            builder1.setCancelable(true);
            builder1.setNeutralButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            showalert = false;
                        }
                    });

            AlertDialog alert11 = builder1.create();
            alert11.show();
        }
    }


    @Override
    public void onConnected(Bundle connectionHint) {
        if (tracking) {
            if (Build.VERSION.SDK_INT >= 23 &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (cameralat + cameralong + camerazoom != 0.0) {
                LatLng mCurrentLatLng = new LatLng(cameralat, cameralong);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mCurrentLatLng, camerazoom));
            } else if (mLastLocation != null) {
                LatLng mCurrentLatLng = new LatLng(mLastLocation.getLatitude(),
                        mLastLocation.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mCurrentLatLng, 15));
            }
            //locationManager.requestLocationUpdates( mGoogleApiClient, mLocationRequest, this);
            //LocationServices.FusedLocationApi.requestLocationUpdates(
            //        mGoogleApiClient, mLocationRequest, this);
            startLocationUpdates();
        }

    }

    @Override
    public void onLocationChanged(Location mLastLocation){
        if (mLastLocation != null) {
            LatLng mCurrentLatLng = new LatLng(mLastLocation.getLatitude(),
                    mLastLocation.getLongitude());
            calcDeltas(mLastLocation);

            if (FiringLine != null) {
                removelastmarker();
                if (tracking) {
                    Location flLocation = FiringLine;
                    LatLng fl = new LatLng(flLocation.getLatitude(),flLocation.getLongitude());
                    double distance = flLocation.distanceTo(mLastLocation);
                    lastMarker = mMap.addMarker(new MarkerOptions().position(mCurrentLatLng)
                            .title(calcDistance(distance,unit)));
                    lastMarker.showInfoWindow();

                    lastPL = mMap.addPolyline(new PolylineOptions().add(fl, mCurrentLatLng));

                    tvCurrent.setText(getString(R.string.current_loc));
                    tvDistance.setText(getString(R.string.distance, calcDistance(distance, unit)));
                    tvSlope.setText(getString(R.string.slope, calcSlope(flLocation, mLastLocation)));
                    tvAccuracy.setText(getString(R.string.accuracy, calcDistance(mLastLocation.getAccuracy(), unit)));
                    tvLatLong.setText(mLastLocation.getLatitude() + ", " + mLastLocation.getLongitude());
                    tvElevation.setText(getString(R.string.elevation, calcDistance(mLastLocation.getAltitude(), unit)));
                    tvAzimuth.setText(getString(R.string.azimuth, calcAzimuth(flLocation, mLastLocation)));
                }

            } else {
                tvCurrent.setText(getString(R.string.current_loc));
                tvDistance.setText(getString(R.string.distance_na));
                tvSlope.setText(getString(R.string.slope_na));
                tvAccuracy.setText(getString(R.string.accuracy, calcDistance(mLastLocation.getAccuracy(), unit)));
                tvLatLong.setText(mLastLocation.getLatitude()+ ", "+mLastLocation.getLongitude());
                tvElevation.setText(getString(R.string.elevation, calcDistance(mLastLocation.getAltitude(), unit)));
                tvAzimuth.setText(getString(R.string.azimuth_na));

            }
        }
    }

    public void calcDeltas(Location newLoc){
        // update the comparison locations array with the new value
        if (compLocs.size() < 5) {
            compLocs.add(newLoc);
        } else {
            compLocs.remove(0);
            compLocs.add(newLoc);
        }

        // compare the locations if the last one is within the threshold of the other 5 then we
        // are locked in on position.
        double distance;
        double maxdistance = 0.0;
        if (compLocs.size() == 5){
            for (int i = 0; i < 3; i++) {
                distance = compLocs.get(4).distanceTo(compLocs.get(i));
                if (distance > maxdistance) {
                    maxdistance = distance;
                }
            }
        } else {
            maxdistance = deltathreshold + 1.0;
        }
        if (maxdistance <= deltathreshold) {
            setButtonsLocked();
        } else {
            setButtonsUnlocked();
        }


    }

    private void setButtonsLocked(){
        buttonAddTarget.setBackgroundResource(R.drawable.button_locked);
        buttonSetFL.setBackgroundResource(R.drawable.button_locked);
    }

    private void setButtonsUnlocked(){
        buttonAddTarget.setBackgroundResource(R.drawable.button_unlocked);
        buttonSetFL.setBackgroundResource(R.drawable.button_unlocked);
    }

    @Override
    public void onConnectionSuspended(int cause) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult mConnectionResult) {
        //right now this is for debugging connection issues only.
        ConnectionResult conresult = mConnectionResult;
    }

    @Override
    public boolean onMarkerClick(final Marker marker){
        if (marker.getTitle().contains("Firing Line")){
            // disable tracking and show data for the firing line
            tracking = false;
            removelastmarker();
            stopLocationUpdates();
            setfiringlineinfowindow(marker);

        } else if (marker.getTitle().contains("Target")){
            if(FiringLine != null) {
                // disable tracking and show data for the selected target
                tracking = false;
                removelastmarker();
                stopLocationUpdates();
                settargetinfowindow(marker);
            }

        } else {
            // enable tracking of the current location marker.
            tracking = true;
            startLocationUpdates();
        }

        tbTracking.setChecked(tracking);
        marker.showInfoWindow();
        lastSelMarker = marker;
        return true;

    }

    private void setfiringlineinfowindow(Marker marker){
        tvCurrent.setText(getString(R.string.firing_line));
        tvDistance.setText(getString(R.string.distance_na));
        tvSlope.setText(getString(R.string.slope_na));
        tvAccuracy.setText(getString(R.string.accuracy, calcDistance(FiringLine.getAccuracy(), unit)));
        tvLatLong.setText(FiringLine.getLatitude() + ", " + FiringLine.getLongitude());
        tvElevation.setText(getString(R.string.elevation, calcDistance(FiringLine.getAltitude(), unit)));
        tvAzimuth.setText(getString(R.string.azimuth_na));
    }

    private void settargetinfowindow(Marker marker){
        // find the target in the list
        String targetnumstr = marker.getTitle().substring(marker.getTitle().lastIndexOf(" ") + 1);
        Integer targetnum = Integer.valueOf(targetnumstr);
        Location target = Targets.get(targetnum - 1);

        // calculate the distance between the target and the firing line
        double distance = target.distanceTo(FiringLine);

        // set the information window data
        tvCurrent.setText(getString(R.string.target_num,targetnum));
        tvDistance.setText(getString(R.string.distance, calcDistance(distance, unit)));
        tvSlope.setText(getString(R.string.slope, calcSlope(FiringLine, target)));
        tvAccuracy.setText(getString(R.string.accuracy, calcDistance(target.getAccuracy(), unit)));
        tvLatLong.setText(target.getLatitude() + ", " + target.getLongitude());
        tvElevation.setText(getString(R.string.elevation, calcDistance(target.getAltitude(),unit)));
        tvAzimuth.setText(getString(R.string.azimuth, calcAzimuth(FiringLine, target)));
    }

    // called when the user clicks the tracking button.
    public void toggleTracking(View view){
        tracking = tbTracking.isChecked();
        if (!tracking){
            stopLocationUpdates();
            setButtonsUnlocked();
            removelastmarker();
        } else {
            startLocationUpdates();
        }
    }

    private void removelastmarker(){
        if (lastPL != null) {
            lastPL.remove();
        }
        if (lastMarker != null) {
            lastMarker.remove();
        }
    }

    public void toggleUnits(View view){
        if (tbUnits.isChecked()){
            unit = "yards";
        } else {
            unit = "meters";
        }
        changetargetunits();
        if (!tracking) {
            if (lastSelMarker != null) {
                lastSelMarker.hideInfoWindow();
                lastSelMarker.showInfoWindow();
                if (lastSelMarker.getTitle().contains(getString(R.string.target_search))) {
                    settargetinfowindow(lastSelMarker);
                } else if (lastSelMarker.getTitle().contains(getString(R.string.firing_line_search))) {
                    setfiringlineinfowindow(lastSelMarker);
                }
            }
        }
    }

    // called whn the user clicks the center button.
    public void mapRecenter(View mView){

        if (tracking) {
            if ( Build.VERSION.SDK_INT >= 23 &&
                    ContextCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED) {
                return  ;
            }
            Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mLastLocation != null) {
                LatLng mCurrentLatLng = new LatLng(mLastLocation.getLatitude(),
                        mLastLocation.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLng(mCurrentLatLng));
            }
        } else if (lastSelMarker != null) {
            if (lastSelMarker.getTitle().contains("Firing Line")) {
                //recenter on the firing line coordinates
                LatLng mCurrentLatLng = new LatLng(FiringLine.getLatitude(), FiringLine.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLng(mCurrentLatLng));

            } else if (lastSelMarker.getTitle().contains("Target")) {
                // find the target in the list
                String targetnumstr = lastSelMarker.getTitle().substring(lastSelMarker.getTitle().lastIndexOf(" ") + 1);
                Integer targetnum = Integer.valueOf(targetnumstr);
                Location target = Targets.get(targetnum - 1);

                LatLng mCurrentLatLng = new LatLng(target.getLatitude(), target.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLng(mCurrentLatLng));

            }
        }
    }

    // called when the user clicks the set fl button.
    public void setFL(View mView) {
        if (tracking) {
            if (Build.VERSION.SDK_INT >= 23 &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            //add a marker at the current latitude and longitude
            Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            LatLng fl = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            FiringLine = mLastLocation;
            if (flMarker != null) {
                flMarker.remove();
            }
            if (flCircle != null) {
                flCircle.remove();
            }
            flMarker = mMap.addMarker(new MarkerOptions()
                    .position(fl)
                    .title("Firing Line")
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.firing_line)));
            flCircle = mMap.addCircle(new CircleOptions()
                    .center(fl)
                    .radius(mLastLocation.getAccuracy())
                    .strokeColor(Color.BLUE)
                    .fillColor(Color.BLUE));
            if (Targets.size() > 0) {
                double targetDistance;
                for (int i = 0; i < Targets.size(); i++) {
                    targetDistance = FiringLine.distanceTo(Targets.get(i));
                    TargetMarkers.get(i).setSnippet(calcDistance(targetDistance, unit));
                }
            }
        }
    }


    // called when the user clicks the add target button.
    public void addTarget(View mView) {
        if (tracking) {
            //add a marker at the current latitude and longitude
            double targetDistance;
            if (Build.VERSION.SDK_INT >= 23 &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            LatLng target = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            Targets.add(mLastLocation);
            if (FiringLine != null) {
                targetDistance = FiringLine.distanceTo(mLastLocation);
            } else targetDistance = 0.0;
            TargetMarkers.add(mMap.addMarker(new MarkerOptions()
                    .position(target)
                    .title(getString(R.string.target_num, Targets.size()))
                    .snippet(calcDistance(targetDistance, unit))
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.target)))
            );
        }
    }

    //called when the user clicks the delete target button.
    public void delTarget(View mView) {
        // check to see if the selected marker is a target marker
        String currentsel = tvCurrent.getText().toString();
        if (currentsel.contains(getString(R.string.target_search))) {
            //find the target in the list.
            String targetnumstr = currentsel.substring(currentsel.lastIndexOf(" ")+1);
            Integer targetnum = Integer.valueOf(targetnumstr);
            Targets.remove(targetnum-1);
            TargetMarkers.get(targetnum-1).remove();
            TargetMarkers.remove(targetnum-1);
            lastSelMarker = null;

            // loop through the targets with numbers higher than the deleted target and renumber them.
            for (int i = targetnum-1; i <= Targets.size()-1; i++) {
                TargetMarkers.get(i).setTitle(getString(R.string.target_num, i+1));
            }
        }
    }

    public void changetargetunits(){
        double targetDistance;
        for (int i = 0; i <= Targets.size()-1; i++) {
            targetDistance = FiringLine.distanceTo(Targets.get(i));
            TargetMarkers.get(i).setSnippet(calcDistance(targetDistance,unit));
        }
    }

    //takes a distance in meters and converts it to a specified unit and rounds to 0.1 unit.
    private String calcDistance(double distance,String unit){
        String returnval;
        if (unit.equals("yards")) {
            double unitdistance = distance * 1.09361;
            double rounddistance = Math.round(unitdistance*10.0)/10.0;
            returnval = rounddistance + " " + getString(R.string.yards);
        } else  {
            double rounddistance = Math.round(distance*10.0)/10.0;
            returnval = rounddistance + " " + getString(R.string.meters);
        }
        return returnval;
    }

    private String calcSlope(Location loc1,Location loc2){
        String returnval;
        double run = loc1.distanceTo(loc2);
        double rise = Math.abs(loc1.getAltitude()-loc2.getAltitude());
        double angle = Math.round(Math.atan(rise/run)/3.14159*180.0);
        returnval = angle + "°";
        return returnval;
    }

    private double calcAzimuth(Location loc1,Location loc2){

        double azimuth;
        double lat1 = degToRad(loc1.getLatitude());
        double lat2 = degToRad(loc2.getLatitude());
        double long1 = degToRad(loc1.getLongitude());
        double long2 = degToRad(loc2.getLongitude());

        azimuth = Math.atan2(Math.sin(long2-long1)*Math.cos(lat2),
                Math.cos(lat1)*Math.sin(lat2)-Math.sin(lat1)*Math.cos(lat2)*Math.cos(long2-long1));

        azimuth = Math.round(radToDeg(azimuth)*10.0)/10.0;
        if (azimuth < 0) {
            azimuth = 360.0 + azimuth;
        }
        return azimuth;
    }

    private double degToRad(double degrees){
        return Math.toRadians(degrees);
    }

    private double radToDeg(double radians) {
        return Math.toDegrees(radians);
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Maps Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }
}
