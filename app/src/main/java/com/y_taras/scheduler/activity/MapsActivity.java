package com.y_taras.scheduler.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.y_taras.scheduler.R;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.y_taras.scheduler.other.StringKeys;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Marker mMarker;
    private String mAction;

    private boolean mHasMarker;
    private double mLatitude;
    private double mLongitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        initUI(savedInstanceState);
    }

    private void initUI(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(StringKeys.LATITUDE) &&
                    savedInstanceState.containsKey(StringKeys.LONGITUDE)) {
                mHasMarker = true;
                mLatitude = savedInstanceState.getDouble(StringKeys.LATITUDE);
                mLongitude = savedInstanceState.getDouble(StringKeys.LONGITUDE);
            }
        }
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarForMapsActivity);
        toolbar.setTitle(R.string.mapsToolbarTitle);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Intent intent = getIntent();
        mAction = intent.getAction();
        if (mAction.equals(StringKeys.EDIT_POINT) && savedInstanceState == null) {
            mLatitude = intent.getDoubleExtra(StringKeys.LATITUDE, 0);
            mLongitude = intent.getDoubleExtra(StringKeys.LONGITUDE, 0);
        }
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mMarker != null) {
            LatLng latLng = mMarker.getPosition();
            outState.putDouble(StringKeys.LATITUDE, latLng.latitude);
            outState.putDouble(StringKeys.LONGITUDE, latLng.longitude);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (mAction.equals(StringKeys.EDIT_POINT) || mHasMarker) {
            LatLng editPoint = new LatLng(mLatitude, mLongitude);
            mMarker = mMap.addMarker(new MarkerOptions().position(editPoint));
            if (!mHasMarker) {
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(editPoint)
                        .zoom(17)
                        .build();
                CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
                mMap.moveCamera(cameraUpdate);
            }
        }

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (mMarker != null) {
                    mMarker.remove();
                }
                mMarker = mMap.addMarker(new MarkerOptions().position(latLng));
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mMarker != null) {
            Intent intent = new Intent();
            LatLng position = mMarker.getPosition();
            intent.putExtra(StringKeys.LATITUDE, position.latitude);
            intent.putExtra(StringKeys.LONGITUDE, position.longitude);
            setResult(RESULT_OK, intent);
        } else {
            setResult(RESULT_CANCELED);
        }
        finish();
        overridePendingTransition(R.anim.left_in_map_activity, R.anim.left_out_map_activity);
    }
}
