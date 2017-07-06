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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.example.neutrino.maze.floorplan.Floor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
/**
 * Created by Greg Stein on 6/23/2017.
 */

public class NewFloorDialog extends Dialog implements ISelectionProvider {
    private static final int NOT_SELECTED = Integer.MAX_VALUE;
    private EditText txtBuilding;
    private AutoCompleteTextView txtType;
    private EditText txtAddress;
    private EditText txtFloor;
    private Button btnGuessAddress;
    private ImageButton btnUp;
    private ImageButton btnDown;
    private ListView lstFloors;

    private int mSelectedFloorIndex = NOT_SELECTED;
    private static String[] buildingTypes;
    private String[] mFloors = {"5", "4", "3", "2", "1", "G", "P", "-2", "-3"};
    private List<Floor> mBuildingFloors = new ArrayList<>();

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
        btnGuessAddress = (Button) findViewById(R.id.btn_guess_address);
        lstFloors = (ListView) findViewById(R.id.lst_floors);
        btnUp = (ImageButton) findViewById(R.id.btn_up);
        btnDown = (ImageButton) findViewById(R.id.btn_down);

        buildingTypes = getContext().getResources().getStringArray(R.array.buildings);
        ArrayAdapter<String> buildingTypesAdapter = new ArrayAdapter<>
                (this.getContext(), android.R.layout.select_dialog_item, buildingTypes);
        txtType.setThreshold(0);    // will start working from first character
        txtType.setAdapter(buildingTypesAdapter);

        for (String floor : mFloors) {
            mBuildingFloors.add(new Floor(floor, "lkjwehrkjhewrkljhelrkjhkjerh"));
        }
        final FloorsAdapter adapter = new FloorsAdapter(getContext(), mBuildingFloors, this);
        lstFloors.setAdapter(adapter);

        btnGuessAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getCurrentAddress();
            }
        });

        lstFloors.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                mSelectedFloorIndex = position;
                adapter.notifyDataSetChanged();
            }
        });

        btnUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSelectedFloorIndex != NOT_SELECTED && mSelectedFloorIndex > 0) {
                    Floor floor = mBuildingFloors.remove(mSelectedFloorIndex);
                    mBuildingFloors.add(mSelectedFloorIndex - 1, floor);
                    mSelectedFloorIndex--;
                    adapter.notifyDataSetChanged();
                }
            }
        });

        btnDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSelectedFloorIndex != NOT_SELECTED && mSelectedFloorIndex < mFloors.length - 1) {
                    Floor floor = mBuildingFloors.remove(mSelectedFloorIndex);
                    mBuildingFloors.add(mSelectedFloorIndex + 1, floor);
                    mSelectedFloorIndex++;
                    adapter.notifyDataSetChanged();
                }
            }
        });
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

    @Override
    public int getSelectedIndex() {
        return mSelectedFloorIndex;
    }

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

    protected static class FloorsAdapter extends ArrayAdapter<Floor> {
        private final ISelectionProvider mSelectioProvider;

        private class ViewHolder {
            TextView txtFloor;
        }

        public FloorsAdapter(@NonNull Context context, List<Floor> data, ISelectionProvider selectionProvider) {
            super(context, R.layout.floor_listview_item, data);
            mSelectioProvider = selectionProvider;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            Floor floor = getItem(position);
            ViewHolder viewHolder; // view lookup cache stored in tag

            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                viewHolder = new ViewHolder();
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.floor_listview_item, parent, false);
                viewHolder.txtFloor = (TextView) convertView.findViewById(R.id.txt_floor_label);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            viewHolder.txtFloor.setText(floor.getName());
            if (position == mSelectioProvider.getSelectedIndex()) {
                viewHolder.txtFloor.setBackgroundColor(AppSettings.primaryDarkColor);
            }
            else {
                viewHolder.txtFloor.setBackgroundColor(AppSettings.accentColor);
            }
            // Return the completed view to render on screen
            return convertView;
        }

    }
}
