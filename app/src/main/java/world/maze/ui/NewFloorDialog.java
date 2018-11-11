package world.maze.ui;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

import world.maze.AppSettings;
import world.maze.R;
import world.maze.core.IFloorChangedHandler;
import world.maze.core.IMainView;
import world.maze.floorplan.Building;
import world.maze.floorplan.Floor;
import world.maze.util.IFuckingSimpleCallback;
import world.maze.util.IFuckingSimpleGenericCallback;
import world.maze.util.PermissionsHelper;

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
    private Button btnGuessAddress;
    private EditText txtFloor;
    private ImageButton btnUp;
    private ImageButton btnDown;
    private ImageButton btnInsertFloor;
    private ImageButton btnDeleteFloor;
    private ListView lstFloors;
    private RecyclerView rcvBuildingLookup;
    private Button btnCreateBuilding;

    private int mSelectedFloorIndex = NOT_SELECTED;
    private static String[] buildingTypes;
    private List<Floor> mBuildingFloors;
    private boolean mUpdateFloorNameInList = false;
    private LocationManager mLocationManager;
    private FloorsAdapter mFloorsAdapter;
    private List<Building> mBuildings = new ArrayList<>();
    private BuildingsAdapter mBuildingsAdapter;
    private boolean mIsBuildingDirty = false;
    private IFloorChangedHandler mFloorChangedHandler;
    private IMainView.IAsyncIdProvider mBuildingIdProvider;
    private IMainView.IAsyncIdProvider mFloorIdProvider;
    private IMainView.IAsyncSimilarBuildingsFinder mSimilarBuildingsFinder;
    private IMainView.IAsyncBuildingCreator mBuildingCreator;

    private IFuckingSimpleGenericCallback<Building> mBuildingUpdater;

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
        btnCreateBuilding = (Button) findViewById(R.id.btn_create_building);

        buildingTypes = getContext().getResources().getStringArray(R.array.buildings);
        ArrayAdapter<String> buildingTypesAdapter = new ArrayAdapter<>
                (this.getContext(), android.R.layout.select_dialog_item, buildingTypes);
        txtType.setThreshold(0);    // will start working from first character
        txtType.setAdapter(buildingTypesAdapter);

        if (Building.current == null) {
            // TODO: Current building comes from two sources: depending on fingerprint from server or the one we are working on now
            Toast.makeText(getContext(), "No building is defined. Either create a new one or try finding existing one", Toast.LENGTH_LONG).show();

            mBuildingFloors = new ArrayList<>();
        } else {
            txtBuilding.setText(Building.current.getName());
            txtType.setText(Building.current.getType());
            txtAddress.setText(Building.current.getAddress());

            mBuildingFloors = Building.current.getFloors();
            Floor currentFloor = Building.current.getCurrentFloor();
            if (currentFloor != null && mBuildingFloors != null && !mBuildingFloors.isEmpty()) {
                // Find index of current floor within building floors
                int index = 0;
                for (Floor floor : mBuildingFloors) {
                    if (floor.getId().equals(currentFloor.getId())) {
                        mSelectedFloorIndex = index;
                        break;
                    }
                    index++;
                }
            }
        }

        mFloorsAdapter = new FloorsAdapter(getContext(), mBuildingFloors, this);
        lstFloors.setAdapter(mFloorsAdapter);

        mBuildingsAdapter = new BuildingsAdapter(mBuildings, new BuildingsAdapter.OnBuildingClickListener() {
            @Override
            public void onBuildingClick(Building building) {
                if (Building.current.isDirty()) {
                    // Save changes to current building
                }
                // TODO: Ask if user really wants to switch to this building?
                Building.current = building;
                Toast.makeText(getContext(),  building.getName(), Toast.LENGTH_SHORT).show();
                setCreatingFloorsAllowed(true);
            }
        });
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        rcvBuildingLookup.setLayoutManager(mLayoutManager);
        rcvBuildingLookup.setItemAnimator(new DefaultItemAnimator());
        rcvBuildingLookup.setAdapter(mBuildingsAdapter);

        setUiListeners();
        setCreatingFloorsAllowed(Building.current != null);
    }

    @Override
    protected void onStop() {
        if (Building.current != null) {
            if (mIsBuildingDirty) {
                Building.current.setName(txtBuilding.getText().toString());
                Building.current.setType(txtType.getText().toString());
                Building.current.setAddress(txtAddress.getText().toString());
                Building.current.setFloors(mBuildingFloors);
                Building.current.setDirty(true);
                mBuildingUpdater.onNotify(Building.current);
            }
        }

        final Floor selectedFloor = mBuildingFloors.get(mSelectedFloorIndex);
        emitFloorChangedEvent(selectedFloor);
        super.onStop();
    }

    private void setUiListeners() {
        txtType.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
               mIsBuildingDirty = true; // indicate to save building upon exit
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        txtAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mIsBuildingDirty = true; // indicate to save building upon exit
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        txtBuilding.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (keyEvent != null && keyEvent.getAction() != KeyEvent.ACTION_DOWN) {
                    return false;
                } else if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE
                        || keyEvent == null || keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    rcvBuildingLookup.setVisibility(View.GONE);
                }

                return false;
            }
        });

        btnCreateBuilding.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // This method will send these fields to server
                mBuildingCreator.createBuilding(
                        txtBuilding.getText().toString(),
                        txtType.getText().toString(),
                        txtAddress.getText().toString(),
                        // This async callback will be executed after getting newly created building
                        new IFuckingSimpleGenericCallback<Building>() {
                            @Override
                            public void onNotify(Building building) {
                                Building.current = building;
                                setCreatingFloorsAllowed(true);
                            }
                        }
                );
            }
        });

        btnGuessAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PermissionsHelper.handlePermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION, false,
                        "Please grant Fine Location permission, so we can find out the address of the building automatically.",
                        (MainActivity) getOwnerActivity(), new IFuckingSimpleCallback() {
                            @Override
                            public void onNotified() {
                                getCurrentAddress();
                            }
                        });
            }
        });

        lstFloors.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                mSelectedFloorIndex = position;
                mFloorsAdapter.notifyDataSetChanged();
                if (mUpdateFloorNameInList) {
                    txtFloor.setText(mBuildingFloors.get(position).getName(), TextView.BufferType.EDITABLE);
                    if (Building.current != null) {
                        Building.current.setCurrentFloor(mBuildingFloors.get(position));
                    }
                }
            }
        });

        lstFloors.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                mUpdateFloorNameInList = true;
                mIsBuildingDirty = true; // indicate to save building upon exit
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
                final String floorName = txtFloor.getText().toString();
                if (floorName.isEmpty()) return;

                final int proposedPosition = suggestPosition(floorName);

                mFloorIdProvider.generateId(new IFuckingSimpleGenericCallback<String>() {
                    @Override
                    public void onNotify(String floorId) {
                        Floor newFloor = new Floor(floorName, floorId);
                        mBuildingFloors.add(proposedPosition, newFloor);
                        mSelectedFloorIndex = proposedPosition;
                        mFloorsAdapter.notifyDataSetChanged();
                        mIsBuildingDirty = true; // indicate to save building upon exit
                    }
                });
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
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mIsBuildingDirty = true; // indicate to save building upon exit
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 2) {
                    mBuildings.clear();
                    mSimilarBuildingsFinder.findBuildings(s.toString(), new IFuckingSimpleGenericCallback<List<Building>>() {
                        @Override
                        public void onNotify(List<Building> buildings) {
                            mBuildings.addAll(buildings);
                            mBuildingsAdapter.notifyDataSetChanged();
                            rcvBuildingLookup.setVisibility(View.VISIBLE);
                        }
                    });
                } else {
                    rcvBuildingLookup.setVisibility(View.GONE);
                }
            }
        });
    }

    public void setBuildingCreator(IMainView.IAsyncBuildingCreator buildingCreator) {
        mBuildingCreator = buildingCreator;
    }

    public void setBuildingUpdater(IFuckingSimpleGenericCallback<Building> buildingUpdater) {
        mBuildingUpdater = buildingUpdater;
    }

    public void setBuildingIdProvider(IMainView.IAsyncIdProvider buildingIdProvider) {
        mBuildingIdProvider = buildingIdProvider;
    }

    public void setFloorIdProvider(IMainView.IAsyncIdProvider floorIdProvider) {
        mFloorIdProvider = floorIdProvider;
    }

    public void setSimilarBuildingsFinder(IMainView.IAsyncSimilarBuildingsFinder similarBuildingsFinder) {
        mSimilarBuildingsFinder = similarBuildingsFinder;
    }

    private void setCreatingFloorsAllowed(boolean allowed) {
        txtFloor.setEnabled(allowed);
        btnUp.setEnabled(allowed);
        btnDown.setEnabled(allowed);
        btnInsertFloor.setEnabled(allowed);
        btnDeleteFloor.setEnabled(allowed);
        lstFloors.setEnabled(allowed);
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

    private void getCurrentAddress() {
        mLocationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10, mLocationListener);
    }

    public void setFloorChangedHandler(IFloorChangedHandler floorChangedHandler) {
        mFloorChangedHandler = floorChangedHandler;
    }

    private void emitFloorChangedEvent(Floor floor) {
        if (mFloorChangedHandler != null) {
            mFloorChangedHandler.onFloorChanged(floor);
        }
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
        private final ISelectionProvider mSelectionProvider;
        private List<Floor> data;

        private class ViewHolder {
            TextView txtFloor;
        }

        public FloorsAdapter(@NonNull Context context, List<Floor> data, ISelectionProvider selectionProvider) {
            super(context, R.layout.floor_listview_item, data);
            mSelectionProvider = selectionProvider;
            this.data = data;
        }

        public void setFloors(List<Floor> floorsData) {
            this.data = floorsData;
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Nullable
        @Override
        public Floor getItem(int position) {
            return data.get(position);
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
            if (position == mSelectionProvider.getSelectedIndex()) {
                viewHolder.txtFloor.setBackgroundColor(AppSettings.primaryDarkColor);
            }
            else {
                viewHolder.txtFloor.setBackgroundColor(AppSettings.accentColor);
            }
            // Return the completed view to render on screen
            return convertView;
        }

    }

    public static class BuildingsAdapter extends RecyclerView.Adapter<BuildingsAdapter.BuildingViewHolder> {
        private List<Building> mBuildings;

        private OnBuildingClickListener mBuildingClickListener;
        public class BuildingViewHolder extends RecyclerView.ViewHolder {

            public TextView txtBuildingName;

            public TextView txtBuildingAddress;
            public BuildingViewHolder(View itemView) {
                super(itemView);
                // Works with simple_list_item_2
                txtBuildingName = (TextView) itemView.findViewById(R.id.text1);
                txtBuildingAddress = (TextView) itemView.findViewById(R.id.text2);
            }
            public void bind(final Building building, final OnBuildingClickListener listener) {
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        listener.onBuildingClick(building);
                    }
                });
            }

        }
        public interface OnBuildingClickListener {

            void onBuildingClick(Building building);
        }
        public BuildingsAdapter(List<Building> buildings) {
            this.mBuildings = buildings;
        }

        public BuildingsAdapter(List<Building> buildings, OnBuildingClickListener listener) {
            this(buildings);
            setBuildingClickListener(listener);
        }

        public void setBuildings(List<Building> buildings) {
            mBuildings = buildings;
        }

        public void setBuildingClickListener(OnBuildingClickListener listener) {
            mBuildingClickListener = listener;
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
            if (mBuildingClickListener != null) {
                holder.bind(building, mBuildingClickListener);
            }
        }

        @Override
        public int getItemCount() {
            return mBuildings.size();
        }

    }
}
