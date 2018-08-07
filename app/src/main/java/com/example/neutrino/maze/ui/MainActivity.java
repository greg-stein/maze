package com.example.neutrino.maze.ui;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.neutrino.maze.AppSettings;
import com.example.neutrino.maze.Locator;
import com.example.neutrino.maze.Locator.ILocationUpdatedListener;
import com.example.neutrino.maze.Mapper;
import com.example.neutrino.maze.R;
import com.example.neutrino.maze.SensorListener;
import com.example.neutrino.maze.StepCalibratorService;
import com.example.neutrino.maze.WiFiLocator;
import com.example.neutrino.maze.WifiScanner;
import com.example.neutrino.maze.floorplan.FloorPlan;
import com.example.neutrino.maze.floorplan.IFloorPlanPrimitive;
import com.example.neutrino.maze.floorplan.Path;
import com.example.neutrino.maze.floorplan.Tag;
import com.example.neutrino.maze.floorplan.Wall;
import com.example.neutrino.maze.rendering.FloorPlanView;
import com.example.neutrino.maze.rendering.FloorPlanView.IOnLocationPlacedListener;
import com.example.neutrino.maze.vectorization.FloorplanVectorizer;
import com.github.fafaldo.fabtoolbar.widget.FABToolbarLayout;
import com.lapism.searchview.SearchView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.example.neutrino.maze.SensorListener.IDeviceRotationListener;
import static com.example.neutrino.maze.vectorization.HoughTransform.LineSegment;

public class MainActivity extends AppCompatActivity implements IDeviceRotationListener,
        ILocationUpdatedListener, IOnLocationPlacedListener, Locator.IDistributionUpdatedListener, VectorizeDialog.ICompleteVectorizationHandler {
    public static final int PERMISSION_LOCATION_REQUEST_CODE = 613;

    // GUI-related fields
    private SearchView uiSearchView;
    private RecyclerView uiRecView;
    private LinearLayout uiRecPanel;
    private View uiRecPanelSpacer;
    private TagsAdapter mAdapter;

    private FABToolbarLayout uiToolbarLayout;
    private Toolbar uiToolbar;
    private FloatingActionButton uiFabEditMode;
    private Spinner uiAddSpinner;
    private static final List<Pair<String, Integer>> addSpinnerData = new ArrayList<>();
    static {
        addSpinnerData.add(new Pair<>("Wall", R.drawable.ic_wall_black_24dp));
        addSpinnerData.add(new Pair<>("Short wall", R.drawable.ic_view_stream_black_24dp));
        addSpinnerData.add(new Pair<>("Place boundaries", R.drawable.ic_format_shapes_black_24dp));
        addSpinnerData.add(new Pair<>("Location tag", R.drawable.ic_map_marker_plus_black_24dp));
        addSpinnerData.add(new Pair<>("", R.drawable.ic_add_white_24dp));
    }

    private FloorPlanView uiFloorPlanView;
    private FloatingActionButton uiFabFindMeOnMap;
    // Map north angle
    private float mMapNorth = 0.0f;

    private boolean mIsMapRotationLocked = false;

    private float mDegreeOffset;
    private float mCurrentDegree = 0f;
    private FloorPlan mFloorPlan = FloorPlan.build();
    private SensorListener mSensorListener;
    private WifiScanner mWifiScanner;
    private Locator mLocator;
    private Mapper mMapper;

    private WiFiLocator mWiFiLocator = WiFiLocator.getInstance();

    private boolean mAutoScanEnabled = false;
    private Menu mEditMenu;
    private StepCalibratorService mStepCalibratorService;
    private Intent mStepCalibratorServiceIntent;

    private static boolean letDieSilently = false;
    public static boolean locationPermissionsGranted = false;

    public static boolean requestPermissions(Context context) {
        if (!locationPermissionsGranted(context)) {
            letDieSilently = true;
            // Request permissions
            if (context instanceof Activity) {
                final Activity activity = (Activity) context;

                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION) ||
                        ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {

                    // Show user justification why these permissions are needed
                    new android.app.AlertDialog.Builder(context).
                        setCancelable(true).
                        setTitle("Permissions necessary").
                        setMessage("We need your permission to get LOCATION for using in open areas " +
                                "and for calibrating positioning methods").
                        setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(
                                      activity,
                                      new String[] { Manifest.permission.ACCESS_COARSE_LOCATION,
                                                     Manifest.permission.ACCESS_FINE_LOCATION},
                                      PERMISSION_LOCATION_REQUEST_CODE);
                            }
                        }).
                        show();
                } else {
                    ActivityCompat.requestPermissions(
                            activity,
                            new String[] { Manifest.permission.ACCESS_COARSE_LOCATION,
                                           Manifest.permission.ACCESS_FINE_LOCATION},
                            PERMISSION_LOCATION_REQUEST_CODE);
                }
            }
            return false;
        }
        return true;
    }

    public static boolean locationPermissionsGranted(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                locationPermissionsGranted = true;
                // reload activity
            } else {
                Toast.makeText(this, "Maze was not allowed to use location. Hence it couldn't " +
                                "function properly and will be closed. Please consider " +
                                "granting it needed permissions.",
                        Toast.LENGTH_LONG).show();
                locationPermissionsGranted = false;
            }
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        uiSearchView = (SearchView) findViewById(R.id.searchView);
        uiRecView = (RecyclerView) findViewById(R.id.rec_list);
        uiRecView.setLayoutManager(new LinearLayoutManager(this));
        uiRecPanel = (LinearLayout) findViewById(R.id.rec_container);
        uiRecPanelSpacer = (View) findViewById(R.id.view_spacer);

        uiToolbarLayout = (FABToolbarLayout) findViewById(R.id.fabtoolbar_layout);
        uiToolbar = (Toolbar) findViewById(R.id.fabtoolbar_toolbar);
        uiFabEditMode = (FloatingActionButton) findViewById(R.id.fabtoolbar_fab);
        uiAddSpinner = (Spinner) findViewById(R.id.add_spinner);

        uiFloorPlanView = (FloorPlanView) findViewById(R.id.ui_MapContainer);
        uiFabFindMeOnMap = (FloatingActionButton) findViewById(R.id.fab_find_me_on_map);
//        uiFabRemoveLastFingerprint = (FloatingActionButton) findViewById(R.id.fab_remove_last_fingerprint);

        AppSettings.init(this);
        mFabAlpha = getAlphaFromRes();

        locationPermissionsGranted = requestPermissions(this);

        // TODO: instead of just killing the app, consider reloading activity when the permission is granted.
        if (!locationPermissionsGranted) return;

        // initialize your android device sensor capabilities
        mWifiScanner = WifiScanner.getInstance(this);
        mSensorListener = SensorListener.getInstance(this);
        mSensorListener.addDeviceRotationListener(this);
        mLocator = Locator.getInstance(this);
        mLocator.addLocationUpdatedListener(this);
        if (AppSettings.inDebug) {
//            mLocator.addDistributionUpdatedListener(this);
        }
        mMapper = Mapper.getInstance(this);

        if (AppSettings.inDebug) {
            mMapper.setFloorPlanView(uiFloorPlanView);
        }

        mAdapter = new TagsAdapter(AppSettings.appActivity);
        uiRecView.setAdapter(mAdapter);

        ImageSpinnerAdapter adapter =
                new ImageSpinnerAdapter(this, R.layout.add_spinner_item, R.id.lbl_item_text, addSpinnerData);
        uiAddSpinner.setAdapter(adapter);
        // Select last hidden item (+)
        uiAddSpinner.setSelection(addSpinnerData.size() - 1);
        setSupportActionBar(uiToolbar);
        getSupportActionBar().setTitle(null);

        setUiListeners();

        mStepCalibratorService = new StepCalibratorService(this);
        mStepCalibratorServiceIntent = new Intent(this, mStepCalibratorService.getClass());


        if (!isMyServiceRunning(mStepCalibratorService.getClass())) {
            startService(mStepCalibratorServiceIntent);
        }
    }

    // TODO: Move this to StepCalibratorService as static method
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i ("isMyServiceRunning?", true+"");
                return true;
            }
        }
        Log.i ("isMyServiceRunning?", false+"");
        return false;
    }

    @Override
    protected void onDestroy() {
        if (!letDieSilently) {
            stopService(mStepCalibratorServiceIntent);
        }
        mLocator.onDestroy();
        Log.i("MAINACT", "onDestroy!");
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
    }

    private void showTheImage(Bitmap b) {
        Toast toast = new Toast(this.getApplicationContext());
        ImageView view = new ImageView(this.getApplicationContext());

        view.setImageBitmap(b);
        toast.setView(view);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.show();
    }

    @Override
    public void onBackPressed() {
        if (uiToolbarLayout.isToolbar()) {
            uiToolbarLayout.hide();
            uiFabFindMeOnMap.animate().translationYBy(uiToolbar.getHeight());
            uiFloorPlanView.setFloorplanEditMode(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        mEditMenu = menu;
        getMenuInflater().inflate(R.menu.menu_edit_mode, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            updateActionsUiStates();
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.btn_move:
                uiFloorPlanView.mapOperation = FloorPlanView.MapOperation.MOVE;
                break;

            case R.id.btn_delete:
                uiFloorPlanView.mapOperation = FloorPlanView.MapOperation.REMOVE;
                break;

            case R.id.btn_lock_rotation:
                if (mIsMapRotationLocked) {
                    mMapNorth = mDegreeOffset;
                    mLocator.setNorth(mMapNorth);
                }
                mIsMapRotationLocked = !mIsMapRotationLocked;
                break;

            case R.id.btn_set_location:
                uiFloorPlanView.mapOperation = FloorPlanView.MapOperation.SET_LOCATION;
                break;

            case R.id.btn_vectorize_floorplan:
                VectorizeDialog newFragment = new VectorizeDialog();
                newFragment.show(getFragmentManager(), "vectorize_dialog");
                break;

            case R.id.btn_erase_floorplan:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Erase entire floor plan?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mFloorPlan.clear();
                                uiFloorPlanView.clearFloorPlan();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // do nothing
                            }
                        }).show();
                break;

            case R.id.btn_autoscan:
                mAutoScanEnabled = !mAutoScanEnabled;
                if (mAutoScanEnabled) {
                    mMapper.enable();
                } else {
                    mMapper.disable();
                }
                break;
                // Here come the rest of menu items

            case R.id.btn_new_floorplan:
                NewFloorDialog nfd = new NewFloorDialog(this);
                nfd.show();
                break;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }

        invalidateOptionsMenu();
        return true;
    }

    private void setUiListeners() {
//        uiToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
//            @Override
//            public boolean onMenuItemClick(MenuItem item) {
//                return onOptionsItemSelected(item);
//            }
//        });

        uiAddSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                switch (position) {
                    case 0:
                        uiFloorPlanView.operand = FloorPlanView.MapOperand.WALL;
                        break;
                    case 1:
                        uiFloorPlanView.operand = FloorPlanView.MapOperand.SHORT_WALL;
                        break;
                    case 2:
                        uiFloorPlanView.operand = FloorPlanView.MapOperand.BOUNDARIES;
                        break;
                    case 3:
                        uiFloorPlanView.operand = FloorPlanView.MapOperand.LOCATION_TAG;
                        break;
                }
                if (position < 4) {
                    uiFloorPlanView.mapOperation = FloorPlanView.MapOperation.ADD;
                }

                invalidateOptionsMenu();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {

            }
        });

        uiFabEditMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uiFabFindMeOnMap.animate().translationYBy(-uiToolbar.getHeight());
                uiToolbarLayout.show();
                uiFloorPlanView.setFloorplanEditMode(true);
            }
        });

        mAdapter.setItemClickListener(new TagsAdapter.ItemClickListener() {
            @Override
            public void onItemClick(Tag t) {
                uiSearchView.close(true);
                // tag "enter" from res location:
                PointF enter = new PointF(244.76593f, 55.589268f);
                List<PointF> pathPoints = mFloorPlan.getPathFinder().constructPath(enter, t.getLocation());
//                List<PointF> pathPoints = pathFinder.constructPath(mLocator.getLocation(), t.getLocation());
                Path path = new Path(pathPoints);
                uiFloorPlanView.renderPath(path);
            }
        });

        uiSearchView.setHint("Search Maze");
        uiSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mAdapter.updateListData(mFloorPlan.searchMostSimilarTags(newText, 20));
                return true;
            }
        });

        uiSearchView.setOnOpenCloseListener(new SearchView.OnOpenCloseListener() {
            @Override
            public boolean onOpen() {
                uiRecPanel.setVisibility(View.VISIBLE);
                uiRecPanelSpacer.setVisibility(View.VISIBLE);

                // Hide fabs
                uiFabFindMeOnMap.hide();
                return true;
            }

            @Override
            public boolean onClose() {
                uiRecPanel.setVisibility(View.GONE);
                uiRecPanelSpacer.setVisibility(View.GONE);

                /// Show fabs
                uiFabFindMeOnMap.show(mPreserveAlphaOnShow);
                return true;
            }
        });

//        uiWallLengthText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
//            @Override
//            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
//                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//                    imm.hideSoftInputFromWindow(uiWallLengthText.getWindowToken(), 0);
//
//                    float realLength = Float.parseFloat(uiWallLengthText.getText().toString());
//                    uiFloorPlanView.rescaleMap(realLength/mCurrentWallLength);
//
//                    return true;
//                }
//                return false;
//            }
//        });

        // TODO: Handle this in menus if needed
//        uiFabRemoveLastFingerprint.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                mMapper.undoLastFingerprint();
//            }
//        });

        uiFabFindMeOnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uiFloorPlanView.centerToLocation();
            }
        });

        // TODO: Handle this in menu:
//        uiFabDeleteWall.setLongClickable(true);
//        uiFabDeleteWall.setOnLongClickListener(new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View view) {
//                uiFloorPlanView.clearFloorPlan();
//                return false;
//            }
//        });

        uiFloorPlanView.setOnLocationPlacedListener(this);

//        uiFloorPlanView.setOnWallLengthChangedListener(new FloorPlanRenderer.IWallLengthChangedListener() {
//            @Override
//            public void onWallLengthChanged(float wallLength) {
//                mCurrentWallLength = wallLength;
//                uiWallLengthText.setText(String.format(Locale.US,"%.2f", wallLength));
//            }
//        });

        ViewTreeObserver vto = uiFloorPlanView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
//                String jsonString = PersistenceLayer.loadFloorPlan();

                String jsonString = null;
                try {
                    Resources res = getResources();
                    InputStream in_s = res.openRawResource(R.raw.floorplan_greg_home_2nd_floor);

                    byte[] b = new byte[in_s.available()];
                    in_s.read(b);
                    jsonString = new String(b);
                } catch (Exception e) {
                     e.printStackTrace();
                }

                new LoadFloorPlanTask(MainActivity.this).onFinish(new LoadFloorPlanTask.AsyncResponse() {
                    @Override
                    public void onFinish(FloorPlan floorPlan) {
                        mFloorPlan = floorPlan;
                        mLocator.setFloorPlan(mFloorPlan);
                        mMapper.setFloorPlan(mFloorPlan);

                        mAdapter.updateListData(floorPlan.getTags());

                        // Find point that should be visible after the floorplan is loaded
                        PointF pointToShow = null;

                        List<IFloorPlanPrimitive> sketch = mFloorPlan.getSketch();
                        for (IFloorPlanPrimitive primitive : sketch) {
                            if (primitive instanceof Wall) {
                                pointToShow = ((Wall)primitive).getStart();
                                break;
                            }
                        }
                        // The main work is done on GL thread!
                        uiFloorPlanView.plot(floorPlan, pointToShow);
                    }
                }).execute(jsonString);

//                if (MazeServer.connectionAvailable(getApplicationContext())) {
//                    MazeServer server = new MazeServer(getApplicationContext());
//                    server.downloadFloorPlan(new MazeServer.AsyncResponse() {
//                        @Override
//                        public void processFinish(String jsonString) {
//                            uiFloorPlanView.setFloorPlanAsJSon(jsonString);
//                        }
//                    });
//                } else {
//                    Toast.makeText(getApplicationContext(), "No Internet connection", Toast.LENGTH_SHORT).show();
//                }

                uiFloorPlanView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

    }

    private FloatingActionButton.OnVisibilityChangedListener mPreserveAlphaOnShow = new FloatingActionButton.OnVisibilityChangedListener() {
        @Override
        public void onShown(FloatingActionButton fab) {
            super.onShown(fab);
            fab.setAlpha(mFabAlpha);
        }
    };
    private float mFabAlpha;
    private float getAlphaFromRes() {
        TypedValue outValue = new TypedValue();
        getResources().getValue(R.dimen.alpha_default, outValue, true);
        return outValue.getFloat();
    }
    private void exciteFab(FloatingActionButton fab) {
        fab.setBackgroundTintList(ColorStateList.valueOf(AppSettings.primaryDarkColor));
    }
    private void calmFab(FloatingActionButton fab) {
        fab.setBackgroundTintList(ColorStateList.valueOf(AppSettings.accentColor));
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void updateActionsUiStates() {
        MenuItem btnMove = uiToolbar.getMenu().getItem(0);
        MenuItem btnRemove = uiToolbar.getMenu().getItem(1);
        MenuItem btnRotationLock = uiToolbar.getMenu().getItem(6);
        switch (uiFloorPlanView.mapOperation) {
            case MOVE:
                System.out.println("MODE: move");
                btnMove.setIcon(getResources().getDrawable(R.drawable.ic_cursor_move_black_24dp, null));
                btnRemove.setIcon(getResources().getDrawable(R.drawable.ic_eraser_white_24dp, null));
                uiAddSpinner.setSelection(addSpinnerData.size() - 1);
                break;
            case ADD:
                System.out.println("MODE: add");
                btnMove.setIcon(getResources().getDrawable(R.drawable.ic_cursor_move_white_24dp, null));
                btnRemove.setIcon(getResources().getDrawable(R.drawable.ic_eraser_white_24dp, null));
                break;
            case REMOVE:
                System.out.println("MODE: remove");
                btnMove.setIcon(getResources().getDrawable(R.drawable.ic_cursor_move_white_24dp, null));
                btnRemove.setIcon(getResources().getDrawable(R.drawable.ic_eraser_black_24dp, null));
                uiAddSpinner.setSelection(addSpinnerData.size() - 1);
                break;
        }

        if (mIsMapRotationLocked) {
            btnRotationLock.getIcon().setColorFilter(Color.DKGRAY, PorterDuff.Mode.MULTIPLY);
        } else {
            btnRotationLock.getIcon().clearColorFilter();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (locationPermissionsGranted(this)) {
            mSensorListener.addDeviceRotationListener(this);
            mWifiScanner.onActivityResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // to stop the listener and save battery
        if (locationPermissionsGranted(this)) {
            mSensorListener.removeDeviceRotationListener(this);
            mWifiScanner.onActivityPause();
        }
    }

    @Override
    public void onDeviceRotated(double orientation) {
        float degree = (float) (orientation - mMapNorth);

        mDegreeOffset = degree - mCurrentDegree;
        if (!mIsMapRotationLocked) {
            uiFloorPlanView.updateAngle(mDegreeOffset);
            mCurrentDegree = degree;
        }
    }

    @Override
    public void onLocationUpdated(PointF location) {
        uiFloorPlanView.setLocation(location);

        if (AppSettings.inDebug) {
            uiFloorPlanView.highlightCentroidMarks(WiFiLocator.centroidMarks);
        }
    }

    @Override
    public void onLocationPlaced(PointF location) {
        mLocator.resetLocationTo(location);
    }

    @Override
    public void onDistributionUpdated(PointF mean, float stdev) {
        uiFloorPlanView.drawDistribution(mean, stdev);
    }

    @Override
    public void onCompleteVectorization(Collection<LineSegment> segments) {
        if (segments == null || segments.isEmpty()) {
            Toast.makeText(getApplicationContext(), "No data returned from vectorization process.", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: Add possibility to augment existing floor plan with another parts instead of clearing
        List<IFloorPlanPrimitive> walls = FloorplanVectorizer.translateToWalls(segments);
        mFloorPlan.setSketch(walls);

        PointF pointToShow = ((Wall)walls.get(0)).getStart(); // we know it is a wall
        uiFloorPlanView.plot(walls, pointToShow);
    }
}
