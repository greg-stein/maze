package com.example.neutrino.maze.ui;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.neutrino.maze.AppSettings;
import com.example.neutrino.maze.R;
import com.example.neutrino.maze.floorplan.Building;
import com.example.neutrino.maze.floorplan.Floor;

import org.apache.commons.lang3.math.NumberUtils;

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
    private ImageButton btnInsertFloor;
    private ImageButton btnDeleteFloor;
    private ListView lstFloors;
    private RecyclerView rcvBuildingLookup;

    private int mSelectedFloorIndex = NOT_SELECTED;
    private static String[] buildingTypes;
    // TODO: These should be recieved from server
    private String[] mFloorsMock = {"5", "4", "3", "2", "1", "G", "P", "-2", "-3"};
    private List<Floor> mBuildingFloors = new ArrayList<>();
    private boolean mUpdateFloorNameInList = false;
    private LocationManager mLocationManager;
    private FloorsAdapter mFloorsAdapter;
    private List<Building> mBuildings = new ArrayList<>();
    private BuildingsAdapter mBuildingsAdapter;

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
        btnInsertFloor = (ImageButton) findViewById(R.id.btn_insert_floor);
        btnDeleteFloor = (ImageButton) findViewById(R.id.btn_remove_floor);
        rcvBuildingLookup = (RecyclerView) findViewById(R.id.rcv_building_lookup);

        buildingTypes = getContext().getResources().getStringArray(R.array.buildings);
        ArrayAdapter<String> buildingTypesAdapter = new ArrayAdapter<>
                (this.getContext(), android.R.layout.select_dialog_item, buildingTypes);
        txtType.setThreshold(0);    // will start working from first character
        txtType.setAdapter(buildingTypesAdapter);

        for (String floor : mFloorsMock) {
            mBuildingFloors.add(new Floor(floor, "lkjwehrkjhewrkljhelrkjhkjerh"));
        }

        mFloorsAdapter = new FloorsAdapter(getContext(), mBuildingFloors, this);
        lstFloors.setAdapter(mFloorsAdapter);


        mBuildingsAdapter = new BuildingsAdapter(mBuildings);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        rcvBuildingLookup.setLayoutManager(mLayoutManager);
        rcvBuildingLookup.setItemAnimator(new DefaultItemAnimator());
        rcvBuildingLookup.setAdapter(mBuildingsAdapter);

        setUiListeners();
    }

    private void setUiListeners() {
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
                mFloorsAdapter.notifyDataSetChanged();
                if (mUpdateFloorNameInList) {
                    txtFloor.setText(mBuildingFloors.get(position).getName(), TextView.BufferType.EDITABLE);
                }
            }
        });

        lstFloors.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                mUpdateFloorNameInList = true;
                return false;
            }
        });

        txtFloor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (mUpdateFloorNameInList && mSelectedFloorIndex != NOT_SELECTED) {
                    mBuildingFloors.get(mSelectedFloorIndex).setName(charSequence.toString());
                    mFloorsAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        txtFloor.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (!mUpdateFloorNameInList) return false;

                if (keyEvent != null && keyEvent.getAction() != KeyEvent.ACTION_DOWN) {
                    return false;
                } else if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE
                        || keyEvent == null || keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    mUpdateFloorNameInList = false;
                }

                return false;
            }
        });

        btnUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSelectedFloorIndex != NOT_SELECTED && mSelectedFloorIndex > 0) {
                    Floor floor = mBuildingFloors.remove(mSelectedFloorIndex);
                    mBuildingFloors.add(mSelectedFloorIndex - 1, floor);
                    mSelectedFloorIndex--;
                    mFloorsAdapter.notifyDataSetChanged();
                }
            }
        });

        btnDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSelectedFloorIndex != NOT_SELECTED && mSelectedFloorIndex < mBuildingFloors.size() - 1) {
                    Floor floor = mBuildingFloors.remove(mSelectedFloorIndex);
                    mBuildingFloors.add(mSelectedFloorIndex + 1, floor);
                    mSelectedFloorIndex++;
                    mFloorsAdapter.notifyDataSetChanged();
                }
            }
        });

        btnInsertFloor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String floorName = txtFloor.getText().toString();
                if (floorName.isEmpty()) return;

                int proposedPosition = suggestPosition(floorName);

                // TODO: Here we should request floor creation from server and get real ID (second parameter)
                Floor newFloor = new Floor(floorName, "JOPAJOPAJOPA");
                mBuildingFloors.add(proposedPosition, newFloor);
                mSelectedFloorIndex = proposedPosition;
                mFloorsAdapter.notifyDataSetChanged();
            }
        });

        btnDeleteFloor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSelectedFloorIndex == NOT_SELECTED) {
                    Toast.makeText(getContext(), "To delete a floor, select it first.", Toast.LENGTH_SHORT).show();
                    return;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                builder.setMessage(String.format("Delete the entire floor %s ???", mBuildingFloors.get(mSelectedFloorIndex).getName()))
                        .setPositiveButton("Yes", new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mBuildingFloors.remove(mSelectedFloorIndex);
                                mSelectedFloorIndex = NOT_SELECTED;
                                mFloorsAdapter.notifyDataSetChanged();
                            }
                        })
                        .setNegativeButton("No", new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // do nothing
                            }
                        }).show();
            }
        });

        txtBuilding.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 2) {
                    findSimilarBuildings(s.toString());
                    mBuildingsAdapter.notifyDataSetChanged();
                    rcvBuildingLookup.setVisibility(View.VISIBLE);
                } else {
                    rcvBuildingLookup.setVisibility(View.GONE);
                }
            }
        });
    }

    // Finds best position to insert new floor based on its name
    private int suggestPosition(String floorName) {
        if (mBuildingFloors.isEmpty()) return 0;
        int currentPosition;

        // if numeric, place it close to next floor
        if (NumberUtils.isNumber(floorName)) {
            int iFloor = Integer.parseInt(floorName);
            if (iFloor >= 0) {
                currentPosition = 0;
                while (NumberUtils.isNumber(mBuildingFloors.get(currentPosition).getName())) {
                    final int iCurrentFloor = Integer.parseInt(mBuildingFloors.get(currentPosition).getName());
                    if (iCurrentFloor < iFloor) break;
                    if (currentPosition == mBuildingFloors.size()-1) break;
                    currentPosition++;
                }

            } else {
                currentPosition = mBuildingFloors.size() - 1;
                while (NumberUtils.isNumber(mBuildingFloors.get(currentPosition).getName())) {
                    final int iCurrentFloor = Integer.parseInt(mBuildingFloors.get(currentPosition).getName());
                    if (iCurrentFloor > iFloor) break;
                    if (currentPosition == 0) break;
                    currentPosition--;
                }

                currentPosition++;
            }

            return currentPosition;
        }

        // non-numeric - place near 0
        currentPosition = 0;
        while (NumberUtils.isNumber(mBuildingFloors.get(currentPosition).getName())) {
            final int iCurrentFloor = Integer.parseInt(mBuildingFloors.get(currentPosition).getName());
            if (iCurrentFloor <= 0) break;
            if (currentPosition == mBuildingFloors.size()-1) break;
            currentPosition++;
        }

        return currentPosition;
    }

    private void findSimilarBuildings(String pattern) {
        mBuildings.add(new Building("Haifa Mall", "Flieman st. Haifa", "Mall", "1"));
        mBuildings.add(new Building("Haifa Mall", "Flieman st. Haifa", "Mall", "1"));
        mBuildings.add(new Building("Haifa Mall", "Flieman st. Haifa", "Mall", "1"));
        mBuildings.add(new Building("Haifa Mall", "Flieman st. Haifa", "Mall", "1"));
        mBuildings.add(new Building("Haifa Mall", "Flieman st. Haifa", "Mall", "1"));
        mBuildings.add(new Building("Haifa Mall", "Flieman st. Haifa", "Mall", "1"));
        mBuildings.add(new Building("Haifa Mall", "Flieman st. Haifa", "Mall", "1"));
        mBuildings.add(new Building("Haifa Mall", "Flieman st. Haifa", "Mall", "1"));
        mBuildings.add(new Building("Haifa Mall", "Flieman st. Haifa", "Mall", "1"));
        mBuildings.add(new Building("Haifa Mall", "Flieman st. Haifa", "Mall", "1"));
        mBuildings.add(new Building("Haifa Mall", "Flieman st. Haifa", "Mall", "1"));
        mBuildings.add(new Building("Haifa Mall", "Flieman st. Haifa", "Mall", "1"));
    }

    private void getCurrentAddress() {
        mLocationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        if (checkPermission()) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10, mLocationListener);
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

    private final LocationListener mLocationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            mLocationManager.removeUpdates(this);

            new AddressObtainerTask().onFinish(new AsyncResponse() {
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

    private class AddressObtainerTask extends AsyncTask<Location, Void, String> {
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

        public AddressObtainerTask onFinish(AsyncResponse asyncResponse) {
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
        private final List<Floor> data;

        private class ViewHolder {
            TextView txtFloor;
        }

        public FloorsAdapter(@NonNull Context context, List<Floor> data, ISelectionProvider selectionProvider) {
            super(context, R.layout.floor_listview_item, data);
            mSelectioProvider = selectionProvider;
            this.data = data;
        }

        @Override
        public int getCount() {
            return data.size();
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

    public class BuildingsAdapter extends RecyclerView.Adapter<BuildingsAdapter.BuildingViewHolder> {
        private List<Building> mBuildings;

        public class BuildingViewHolder extends RecyclerView.ViewHolder {
            public TextView txtBuildingName;
            public TextView txtBuildingAddress;

            public BuildingViewHolder(View itemView) {
                super(itemView);
                // Works with simple_list_item_2
                txtBuildingName = (TextView) itemView.findViewById(R.id.text1);
                txtBuildingAddress = (TextView) itemView.findViewById(R.id.text2);
            }
        }

        public BuildingsAdapter(List<Building> buildings) {
            this.mBuildings = buildings;
        }

        @Override
        public BuildingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.building_recyclerview_item, parent, false);
//                    .inflate(simple_list_item_2, parent, false);
            return new BuildingViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(BuildingViewHolder holder, int position) {
            Building building = mBuildings.get(position);
            holder.txtBuildingName.setText(building.getName());
            holder.txtBuildingAddress.setText(building.getAddress());
        }

        @Override
        public int getItemCount() {
            return mBuildings.size();
        }

    }
}
