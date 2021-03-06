//        Copyright 2015 Miguel Catalan Bañuls
//
//        Licensed under the Apache License, Version 2.0 (the "License");
//        you may not use this file except in compliance with the License.
//        You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//        Unless required by applicable law or agreed to in writing, software
//        distributed under the License is distributed on an "AS IS" BASIS,
//        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//        See the License for the specific language governing permissions and
//        limitations under the License.

// https://github.com/MiguelCatalan/MaterialSearchView

package com.example.arifinfirdaus.cato;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.AvoidType;
import com.akexorcist.googledirection.model.Direction;
import com.example.arifinfirdaus.cato.Model.Pembeli;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;

import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.miguelcatalan.materialsearchview.MaterialSearchView;

public class MainPembeliActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        MaterialSearchView.OnQueryTextListener,
        MaterialSearchView.SearchViewListener,
        FirebaseNetworkCalls {

    private GoogleMap mGoogleMap;
    private GoogleApiClient mGoogleApiClient;
    private MaterialSearchView mMaterialSearchView;
    private String mQuerySearch;
    private TextView mTvNamaUserMain;
    private TextView mTvTipeUserMain;
    private DatabaseReference mDatabaseReference;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private String mTipeUser;

    private Marker mMarker;

    // MARK: - AppCompatActivity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initFirebase();

        if (isGoogleServicesAvailable()) {
            Toast.makeText(this, "google service tersedia di device Anda", Toast.LENGTH_LONG).show();
            setContentView(R.layout.activity_main);
            initMap();
        } else {
            // No Google Maps Layout
            Toast.makeText(this, "Google service tidak ditemukan pada perangkat Anda", Toast.LENGTH_LONG).show();
            setContentView(R.layout.activity_main_no_google_maps);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View header = ((NavigationView) findViewById(R.id.nav_view)).getHeaderView(0);
        mTvNamaUserMain = ((TextView) header.findViewById(R.id.tv_nama_user_main));
        mTvTipeUserMain = ((TextView) header.findViewById(R.id.tv_tipe_user_main));

        mMaterialSearchView = (MaterialSearchView) findViewById(R.id.search_view);
        mMaterialSearchView.setHint("Cari toko disini...");
        mMaterialSearchView.setHintTextColor(Color.GRAY);
        mMaterialSearchView.setOnQueryTextListener(this);
        mMaterialSearchView.setOnSearchViewListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchData();
    }

    @Override
    public void fetchData() {
        fetchUserInfo();
    }

    @Override
    public void initFirebase() {
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
    }

    // MARK : - Fetch user info with Firebase
    private void fetchUserInfo() {
        mDatabaseReference.child("user").child(mFirebaseUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Pembeli pembeli = dataSnapshot.getValue(Pembeli.class);
                        // setText
                        mTvNamaUserMain.setText(pembeli.getEmail());
                        mTvTipeUserMain.setText(pembeli.getTipeUser());
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    public void geoLocate(View view) throws IOException {
        // String location = etCariToko.getText().toString();
        String location = this.mQuerySearch;
        Geocoder gc = new Geocoder(this);
        List<Address> list = gc.getFromLocationName(location, 1);
        Address address = list.get(0);
        String locality = address.getLocality();

        Toast.makeText(this, locality, Toast.LENGTH_LONG).show();

        double lat = address.getLatitude();
        double lng = address.getLongitude();

        float zoomLevel = 15;
        goToLocationZoom(lat, lng, zoomLevel);
    }

    public void geoLocate(String hasilSearch) throws IOException {
        String location = hasilSearch;
        Geocoder gc = new Geocoder(this);
        List<Address> list = gc.getFromLocationName(location, 1);
        Address address = list.get(0);
        String locality = address.getLocality();

        Toast.makeText(this, locality, Toast.LENGTH_LONG).show();

        double lat = address.getLatitude();
        double lng = address.getLongitude();

        float zoomLevel = 15;
        goToLocationZoom(lat, lng, zoomLevel);

        // cek apakah sudah ada mMarker
        if (mMarker != null) {
            mMarker.remove();
        }

        // add mMarker
        MarkerOptions markerOptions = new MarkerOptions()
                .title(address.getAddressLine(0))
                .snippet("Buka jam 8 - 9 Pagi")
                .position(new LatLng(lat, lng));
        mMarker = mGoogleMap.addMarker(markerOptions);

//        // modify
//        // misal ambil 3 alamat
//        int tokoDatabaseCount = 3;
//        LatLng[] latLngs = new LatLng[tokoDatabaseCount];
//        for (int i = 0; i < tokoDatabaseCount; i++) {
//            address = list.get(i); // ambil tiap adress
//            latLngs[i] = new LatLng(address.getLatitude(), address.getLongitude()); // ambil tiap2 lat lng
//        }
//        // add markers
//        MarkerOptions[] markerOptionses = new MarkerOptions[tokoDatabaseCount];
//        for (int i = 0; i < tokoDatabaseCount; i++) {
//            markerOptionses[i] = new MarkerOptions()
//                    .title("asdfads")
//                    .snippet("asdfasdfasdfasd")
//                    .position(latLngs[i]);
//        }
    }


    // Maps Method stuff ---------------------------------------------------------------------------
    private boolean isGoogleServicesAvailable() {
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int isAvailable = api.isGooglePlayServicesAvailable(this);
        if (isAvailable == ConnectionResult.SUCCESS) {
            return true;
        } else if (api.isUserResolvableError(isAvailable)) {
            Dialog dialog = api.getErrorDialog(this, isAvailable, 0);
            dialog.show();
        } else {
            Toast.makeText(this, "Can't connect to play services", Toast.LENGTH_LONG).show();
        }
        return false;
    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a mMarker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    // MARK : - OnMapReadyCallback
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.mGoogleMap = googleMap;
        setupUserLocation();
        directionTest();

    }

    private void setupUserLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
        }
        mGoogleMap.setMyLocationEnabled(true);

//        mGoogleApiClient = new GoogleApiClient.Builder(this)
//                .addApi(LocationServices.API)
//                .addConnectionCallbacks(this)
//                .addOnConnectionFailedListener(this)
//                .build();
//        mGoogleApiClient.connect();
    }


    private LocationRequest locationRequest;

    // MARK : GoogleApiClient.ConnectionCallbacks
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_LOW_POWER)
                .setInterval(1000);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
    }

    // MARK : - GoogleApiClient.ConnectionCallbacks
    @Override
    public void onConnectionSuspended(int i) {

    }

    // MARK : - GoogleApiClient.OnConnectionFailedListener
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    // MARK : - LocationListener
    @Override
    public void onLocationChanged(Location location) {
        if (location == null) {
            Toast.makeText(this, "Tidak dapat mendapatkan lokasi sekarang", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Dapat lokasi Anda sekarang", Toast.LENGTH_SHORT).show();
            LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latlng, 15f);
            mGoogleMap.animateCamera(cameraUpdate);
        }
    }

    private void goToLocation(int lat, int lng) {
        LatLng destinationLatLng = new LatLng(lat, lng);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(destinationLatLng);

        MarkerOptions markerOptions = new MarkerOptions()
                .position(destinationLatLng)
                .title(mGoogleMap.getCameraPosition().toString());

        mGoogleMap.addMarker(markerOptions);
        mGoogleMap.moveCamera(cameraUpdate);
    }

    private void goToLocationZoom(double lat, double lng, float zoomLevel) {
        LatLng destinationLatLng = new LatLng(lat, lng);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(destinationLatLng, zoomLevel);
        mGoogleMap.moveCamera(cameraUpdate);


    }

    // End of Maps method --------------------------------------------------------------------------


    // MARK : - MaterialSearchView.OnQueryTextListener
    @Override
    public boolean onQueryTextSubmit(String query) {
        //Do some magic
        Toast.makeText(MainPembeliActivity.this, "query : " + query, Toast.LENGTH_SHORT).show();
        mQuerySearch = query;
        // geoLocate(query);
        if (!query.equals(null) || !query.equals("")) {
            try {
                geoLocate(mQuerySearch);

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(MainPembeliActivity.this, "Mohon lengkapi keyword pencarian Anda", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    private void directionTest() {

        String newMapsDirectionAPiKey = "AIzaSyApn04DGO6TPYnSNCrgiX3ET4L-4B-fhJQ";
        String link = "https://maps.googleapis.com/maps/api/directions/json?origin=Toronto&destination=Montreal&key=";
        String linkWithApiKey = link + newMapsDirectionAPiKey;
        Toast.makeText(this, "directionTest()", Toast.LENGTH_SHORT).show();
        GoogleDirection.withServerKey(newMapsDirectionAPiKey)
                .from(new LatLng(37.7681994, -122.444538))
                .to(new LatLng(37.7749003, -122.4034934))
                .avoid(AvoidType.FERRIES)
                .avoid(AvoidType.HIGHWAYS)
                .execute(new DirectionCallback() {
                    @Override
                    public void onDirectionSuccess(Direction direction, String rawBody) {
                        if (direction.isOK()) {
                            // Do something
                        } else {
                            // Do something
                        }
                    }

                    @Override
                    public void onDirectionFailure(Throwable t) {
                        // Do something
                    }
                });
    }

    // MARK : - MaterialSearchView.OnQueryTextListener
    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    // MARK : - MaterialSearchView.SearchViewListener
    @Override
    public void onSearchViewShown() {

    }

    // MARK : - MaterialSearchView.SearchViewListener
    @Override
    public void onSearchViewClosed() {

    }


    // MARK: - AppCompatActivity
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }

        if (mMaterialSearchView.isSearchOpen()) {
            mMaterialSearchView.closeSearch();
        } else {
            super.onBackPressed();
        }
    }

    // MARK: - AppCompatActivity
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        MenuItem item = menu.findItem(R.id.action_search);
        mMaterialSearchView.setMenuItem(item);

        return true;
    }

    // MARK: - AppCompatActivity
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_search:
                Toast.makeText(this, "unsupported yet", Toast.LENGTH_SHORT).show();
                break;
            case R.id.map_type_normal:
                mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                break;
            case R.id.map_type_terrain:
                mGoogleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                break;
            case R.id.map_type_satellite:
                mGoogleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                break;
            case R.id.map_type_hybrid:
                mGoogleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    // MARK : - NavigationView.OnNavigationItemSelectedListener
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_stuff_adround_you:
                Toast.makeText(this, "stuff around you", Toast.LENGTH_SHORT).show();
                break;
            case R.id.nav_favorite_place:
                toFavoritePlaceActivity();
                break;
            case R.id.nav_profile:
                toProfilePembeliActivity();
                break;
            case R.id.nav_logout:
                handleLogout();
                break;
            case R.id.nav_about:
                Toast.makeText(this, "about", Toast.LENGTH_SHORT).show();
                break;
            case R.id.nav_feedback:
                Toast.makeText(this, "feedback", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void toProfilePembeliActivity() {
        Intent intent = new Intent(MainPembeliActivity.this, ProfilePembeliScrollingActivity.class);
        startActivity(intent);
    }

    private void toSignInActivity() {
        Intent intent = new Intent(MainPembeliActivity.this, SignInActivity.class);
        startActivity(intent);
    }

    private void handleLogout() {
        FirebaseAuth.getInstance().signOut();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            this.finishAffinity();
        }


//        /** on your logout method:**/
//        Intent broadcastIntent = new Intent();
//        broadcastIntent.setAction("com.package.ACTION_LOGOUT");
//        sendBroadcast(broadcastIntent);
    }

    // MARK : - Navigation
    private void toFavoritePlaceActivity() {
        Intent intent = new Intent(MainPembeliActivity.this, FavoritePlaceListActivity.class);
        startActivity(intent);
    }


}
