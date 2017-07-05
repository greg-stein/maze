package com.example.neutrino.maze;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by Greg Stein on 6/23/2017.
 */

public class NewFloorDialog extends Dialog {
    private EditText txtBuilding;
    private AutoCompleteTextView txtType;
    private EditText txtAddress;
    private EditText txtFloor;
    private static String[] buildingTypes = {"Airport", "Hospital", "Mall", "University"};
    private LocationManager locationManager;

    public NewFloorDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.new_floor_dialog);

        txtBuilding = (EditText) findViewById(R.id.txt_building);
        txtType = (AutoCompleteTextView) findViewById(R.id.txt_building_type);
        txtAddress = (EditText) findViewById(R.id.txt_address);
        txtFloor = (EditText) findViewById(R.id.txt_floor);

        ArrayAdapter<String> adapter = new ArrayAdapter<>
                (this.getContext(), android.R.layout.select_dialog_item, buildingTypes);
        txtType.setThreshold(1);    // will start working from first character
        txtType.setAdapter(adapter);

        getCurrentAddress();
    }

    private void getCurrentAddress() {
        locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        if (checkPermission()) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10, locationListener);
        }
    }

    private boolean checkPermission() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return true;
        }
        return true;
    }

    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            locationManager.removeUpdates(this);

            new AddressOtainerTask().onFinish(new AsyncResponse() {
                @Override
                public void onFinish(String address) {
                    if (address != null && address.length() > 0) {
                        int pipePos = address.indexOf("|");
                        if (pipePos != -1) {
                            // TODO: What if user typed something in these fields?
                            txtAddress.setText(address.substring(0, pipePos));
                            txtBuilding.setText(address.substring(pipePos + 1));
                        }
                        else txtAddress.setText(address);
                    }
                }
            }).execute(location);
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

    private class AddressOtainerTask extends AsyncTask<Location, Void, String> {
        private AsyncResponse onFinishHandler;

        @Override
        protected String doInBackground(Location... locations) {
            Location location = locations[0];
            double longitude = location.getLongitude();
            double latitude = location.getLatitude();

            Geocoder geocoder;
            List<Address> addresses;
            geocoder = new Geocoder(NewFloorDialog.this.getContext(), Locale.getDefault());

            try {
                // Here 1 represent max location result to returned, by documents it recommended 1 to 5
                addresses = geocoder.getFromLocation(latitude, longitude, 1);
                StringBuilder addressBuilder = new StringBuilder();

                // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
                String addressLine = addresses.get(0).getAddressLine(0);
                if (addressLine != null && addressLine.length() > 0)
                    addressBuilder.append(addressLine).append(", ");

                String city = addresses.get(0).getLocality();
                if (city != null && city.length() > 0)
                    addressBuilder.append(city).append(", "); // city

                String state = addresses.get(0).getAdminArea();
                if (state != null && state.length() > 0)
                    addressBuilder.append(state).append(", "); // State

                String country = addresses.get(0).getCountryName();
                if (country != null && country.length() > 0)
                    addressBuilder.append(country);

//                addressBuilder.append(addresses.get(0).getPostalCode()).append(", ");
                String knownName = addresses.get(0).getFeatureName();
                if (knownName != null && knownName.length() > 0) {
                    addressBuilder.append('|').append(knownName);
                }

                return addressBuilder.toString();
            } catch (IOException e) {
                e.printStackTrace();
                return "";
            }
        }

        public AddressOtainerTask onFinish(AsyncResponse asyncResponse) {
            this.onFinishHandler = asyncResponse;
            return this;
        }

        @Override
        protected void onPostExecute(String address) {
            if (onFinishHandler != null) {
                onFinishHandler.onFinish(address);
            }
        }

    }

    private interface AsyncResponse {
        void onFinish(String address);
    }
}
