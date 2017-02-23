package com.example.neutrino.maze;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.neutrino.maze.floorplan.FloorPlanSerializer;
import com.example.neutrino.maze.floorplan.IFloorPlanPrimitive;
import com.example.neutrino.maze.floorplan.PersistenceLayer;
import com.example.neutrino.maze.floorplan.Wall;
import com.example.neutrino.maze.floorplan.WifiMark;
import com.example.neutrino.maze.floorplan.vectorization.FloorplanVectorizer;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    // One human step
    private static final float STEP_LENGTH = 0.78f; // 78cm
    private static final float WIFIMARK_SPACING = 3*STEP_LENGTH;

    private float mTravelledDistance = 0;

    // GUI-related fields
    private FloorPlanView uiFloorPlanView;
    private Toolbar uiToolbar;
    private ToggleButton uiModeSwitch;
    private FloatingActionButton uiFabDeleteWall;
    private FloatingActionButton uiFabAddWall;
    private FloatingActionButton uiFabSetLocation;
    private FloatingActionButton uiFabAutoscanMode;
    private FloatingActionButton uiFabFindMeOnMap;
    private FloatingActionButton uiFabAddFloorplanFromPic;
    private FloatingActionButton uiFabAddFloorplanFromGallery;
    private FloatingActionButton uiFabMapRotateLock;
    private EditText uiWallLengthText;

    // Map north angle
    private float mapNorth = 0.0f;

    private float currentDegree = 0f;

    // device sensor manager
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private Sensor mGravity;
    private Sensor mRotation;
    private Sensor mStepDetector;
    private float[] mGravitySensorRawData;
    private float[] mGeomagneticSensorRawData;
    private static final float[] mRotationMatrix = new float[9];
    private static final float[] mInclinationMatrix = new float[9];
    private static final float[] mOrientation = new float[3];
    private boolean mHaveAccelerometer;
    private boolean mHaveMagnetometer;
    private boolean mHaveGravity;
    private boolean mHaveRotation;
    private boolean mHaveStepDetector;

    private WifiManager mWifiManager;
    private WifiScanner mWifiScanner;
    private WiFiTug mWiFiTug = new WiFiTug();
    private TugOfWar mTow = new TugOfWar();

    private boolean mPlacedMarkAtCurrentLocation = true;

    /*
     * time smoothing constant for low-pass filter
     * 0 ≤ alpha ≤ 1 ; a smaller value basically means more smoothing
     * See: http://en.wikipedia.org/wiki/Low-pass_filter#Discrete-time_realization
     */
    private static final float LOW_PASS_ALPHA = 0.5f;
    private static final PointF mCurrentLocation = new PointF();
    private float mCurrentWallLength = 1;
    private boolean mIsMapRotationLocked = false;
    private boolean mAutoScanEnabled = false;

    public MainActivity() {
        mTow.registerTugger(mWiFiTug);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        uiFloorPlanView = (FloorPlanView) findViewById(R.id.ui_MapContainer);
        uiToolbar = (Toolbar) findViewById(R.id.toolbar);
        uiFabDeleteWall = (FloatingActionButton) findViewById(R.id.fab_delete_wall);
        uiFabAddWall = (FloatingActionButton) findViewById(R.id.fab_add_wall);
        uiFabSetLocation = (FloatingActionButton) findViewById(R.id.fab_set_location);
        uiFabAutoscanMode = (FloatingActionButton) findViewById(R.id.fab_autoscan);
        uiFabFindMeOnMap = (FloatingActionButton) findViewById(R.id.fab_find_me_on_map);
        uiFabAddFloorplanFromPic = (FloatingActionButton) findViewById(R.id.fab_add_floorplan_from_camera);
        uiFabAddFloorplanFromGallery = (FloatingActionButton) findViewById(R.id.fab_add_floorplan_from_gallery);
        uiFabMapRotateLock = (FloatingActionButton) findViewById(R.id.fab_map_rotate_lock);
        uiModeSwitch = (ToggleButton) findViewById(R.id.tb_edit_mode);
        uiWallLengthText = (EditText) findViewById(R.id.et_wall_length);

        setSupportActionBar(uiToolbar);
        getSupportActionBar().setTitle("");

        AppSettings.init(this); //getApplicationContext()
        mFabAlpha = getAlphaFromRes();

        // initialize your android device sensor capabilities
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mWifiScanner = new WifiScanner(mWifiManager);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mRotation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mStepDetector = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        setUiListeners();
    }

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_SELECT = 2;
    protected String mCurrentImagePath;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) return;

        Bitmap floorplanBitmap = null;

        switch (requestCode) {
            case REQUEST_IMAGE_CAPTURE: {
                floorplanBitmap = loadBitmapFromFile(mCurrentImagePath);
                break;
            }
            case REQUEST_IMAGE_SELECT: {
                Uri selectedImageUri = data.getData();
                mCurrentImagePath = getPath(selectedImageUri);

                if (mCurrentImagePath == null) {
                    floorplanBitmap = loadPicasaImageFromGallery(selectedImageUri);
                } else {
                    floorplanBitmap = loadBitmapFromFile(mCurrentImagePath);
                }
                break;
            }
        }

        List<Wall> walls = FloorplanVectorizer.vectorize(floorplanBitmap);
        uiFloorPlanView.setFloorPlan(walls, false); // not in init phase
        mWiFiTug.walls = walls;
        uiFloorPlanView.showMap();
    }


    private static Bitmap loadBitmapFromFile(String mCurrentImagePath) {
        Bitmap bitmap = null;
        File imageFile = new File(mCurrentImagePath);

        if (imageFile.exists()) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inMutable = true;
            bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
        }

        return bitmap;
    }

    private void showTheImage(Bitmap b) {
        Toast toast = new Toast(this.getApplicationContext());
        ImageView view = new ImageView(this.getApplicationContext());

        view.setImageBitmap(b);
        toast.setView(view);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.show();
    }

    // NEW METHOD FOR PICASA IMAGE LOAD
    private Bitmap loadPicasaImageFromGallery(final Uri uri) {
        Bitmap floorplanBitmap = null;
        String[] projection = {  MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);

        if(cursor != null) {
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
            if (columnIndex != -1) {
                try {
                    floorplanBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        cursor.close();
        return floorplanBitmap;
    }

    public String getPath(Uri uri) {
        String[] projection = {MediaStore.MediaColumns.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            String filePath = cursor.getString(columnIndex);
            cursor.close();
            return filePath;
        } else
            return uri.getPath();               // FOR OI/ASTRO/Dropbox etc
    }

    private void setUiListeners() {
        uiWallLengthText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(uiWallLengthText.getWindowToken(), 0);

                    float realLength = Float.parseFloat(uiWallLengthText.getText().toString());
                    uiFloorPlanView.rescaleMap(realLength/mCurrentWallLength);

                    return true;
                }
                return false;
            }
        });

        uiFabMapRotateLock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mIsMapRotationLocked = !mIsMapRotationLocked;
                if (mIsMapRotationLocked) {
                    exciteFab(uiFabMapRotateLock);
                } else {
                    calmFab(uiFabMapRotateLock);
                }

                // if (enable) {
                //     rememberedNorth = currentNorth
                //     disable map rotation
                // } else {
                //     angleDiff = currentNorth - rememberedNorth
                //     rerotate map
                //     enable map rotation
                // }
            }
        });

        uiFabAddFloorplanFromGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"),
                        REQUEST_IMAGE_SELECT);
            }
        });

        uiFabAddFloorplanFromPic.setOnClickListener(new View.OnClickListener() {
            static final String IMAGE_FILENAME = "floorplan";

            // Here comes code for taking floorplan as picture from camera
            @Override
            public void onClick(View view) {
                // Dispatch Take Picture Intent
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                    try {
                        File imageFile = File.createTempFile(IMAGE_FILENAME, ".jpg", storageDir);
                        mCurrentImagePath = imageFile.getAbsolutePath();
                        Uri imageFileUri = Uri.fromFile(imageFile);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageFileUri);
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                    } catch (IOException e) {
                        Toast.makeText(getApplicationContext(), "Error saving image", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
            }
        });

        uiFabFindMeOnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PointF location = mTow.getCurrentPosition();
                uiFloorPlanView.putLocationMarkAt(location);
                uiFloorPlanView.highlightCentroidMarks(WiFiTug.centroidMarks);
            }
        });

        uiFabDeleteWall.setLongClickable(true);
        uiFabDeleteWall.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                uiFloorPlanView.clearFloorPlan();
                return false;
            }
        });

        mWifiScanner.setFingerprintAvailableListener(new WifiScanner.IFingerprintAvailableListener() {
            @Override
            public void onFingerprintAvailable(WiFiTug.Fingerprint fingerprint) {
                if (!mPlacedMarkAtCurrentLocation) {
                    uiFloorPlanView.placeWiFiMarkAt(mCurrentLocation, fingerprint);
                    mWiFiTug.marks = uiFloorPlanView.getPrimitives(WifiMark.class);
                    mWiFiTug.walls = uiFloorPlanView.getPrimitives(Wall.class);
                    mPlacedMarkAtCurrentLocation = true;
                }
                mWiFiTug.currentFingerprint = fingerprint;
            }
        });

        uiFloorPlanView.setOnLocationPlacedListener(new FloorPlanView.IOnLocationPlacedListener() {
            @Override
            public void onLocationPlaced(float x, float y) {
                mCurrentLocation.set(x, y);
                mPlacedMarkAtCurrentLocation = false;
            }
        });

        uiFloorPlanView.setOnWallLengthChangedListener(new FloorPlanRenderer.IWallLengthChangedListener() {
            @Override
            public void onWallLengthChanged(float wallLength) {
                mCurrentWallLength = wallLength;
                uiWallLengthText.setText(String.format(java.util.Locale.US,"%.2f", wallLength));
            }
        });

        ViewTreeObserver vto = uiFloorPlanView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                String jsonString = PersistenceLayer.loadFloorPlan();

//                try {
//                    Resources res = getResources();
//                    InputStream in_s = res.openRawResource(R.raw.floorplan_greg_home_2nd_floor);
//
//                    byte[] b = new byte[in_s.available()];
//                    in_s.read(b);
//                    jsonString = new String(b);
//                } catch (Exception e) {
//                     e.printStackTrace();
//                }

                if (jsonString != null) {
                    List<IFloorPlanPrimitive> floorplan = FloorPlanSerializer.deserializeFloorPlan(jsonString);
                    uiFloorPlanView.setFloorPlan(floorplan);
                    mWiFiTug.marks = CommonHelper.getPrimitives(WifiMark.class, floorplan);
                    mWiFiTug.walls = CommonHelper.getPrimitives(Wall.class, floorplan);
                    uiFloorPlanView.showMap();
                }


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

        uiModeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                uiFloorPlanView.setMode(isChecked);
                if (isChecked) {
                    uiToolbar.setBackgroundColor(AppSettings.editModeColor);
                    uiFabDeleteWall.show(mPreserveAlphaOnShow);
                    uiFabSetLocation.show(mPreserveAlphaOnShow);
                    uiFabAddWall.show(mPreserveAlphaOnShow);
                    uiFabAutoscanMode.show(mPreserveAlphaOnShow);
                    uiFabAddFloorplanFromPic.show(mPreserveAlphaOnShow);
                    uiFabAddFloorplanFromGallery.show(mPreserveAlphaOnShow);
                    uiFabMapRotateLock.show(mPreserveAlphaOnShow);
                } else {
                    uiToolbar.setBackgroundColor(AppSettings.primaryColor);
                    uiFabDeleteWall.hide();
                    uiFabSetLocation.hide();
                    uiFabAddWall.hide();
                    uiFabAutoscanMode.hide();
                    uiFabAddFloorplanFromPic.hide();
                    uiFabAddFloorplanFromGallery.hide();
                    uiFabMapRotateLock.hide();

                    String jsonString = uiFloorPlanView.getFloorPlanAsJSon();
                    PersistenceLayer.saveFloorPlan(jsonString);
//                    if (MazeServer.connectionAvailable(getApplicationContext())) {
//                        MazeServer server = new MazeServer(getApplicationContext());
//                        server.uploadFloorPlan(uiFloorPlanView.getFloorPlanAsJSon());
//                    } else {
//                        Toast.makeText(getApplicationContext(), "No Internet connection", Toast.LENGTH_SHORT).show();
//                    }
                }
            }
        });

        uiFabAutoscanMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAutoScanEnabled = !mAutoScanEnabled;
                if (mAutoScanEnabled) {
                    exciteFab(uiFabAutoscanMode);
                } else {
                    calmFab(uiFabAutoscanMode);
                }
            }
        });

        uiFabDeleteWall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (uiFloorPlanView.operation == FloorPlanView.Operation.REMOVE_WALL) {
                    uiFloorPlanView.operation = FloorPlanView.Operation.NONE;
                }
                else {
                    uiFloorPlanView.operation = FloorPlanView.Operation.REMOVE_WALL;
                }
                updateOperationFabsState();
            }
        });


        uiFabAddWall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (uiFloorPlanView.operation == FloorPlanView.Operation.ADD_WALL) {
                    uiFloorPlanView.operation = FloorPlanView.Operation.NONE;
                }
                else {
                    uiFloorPlanView.operation = FloorPlanView.Operation.ADD_WALL;
                }
                updateOperationFabsState();
            }
        });

        uiFabSetLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (uiFloorPlanView.operation == FloorPlanView.Operation.SET_LOCATION) {
                    uiFloorPlanView.operation = FloorPlanView.Operation.NONE;
                }
                else {
                    uiFloorPlanView.operation = FloorPlanView.Operation.SET_LOCATION;
                }
                updateOperationFabsState();
            }
        });

        uiFabDeleteWall.hide();
        uiFabSetLocation.hide();
        uiFabAddWall.hide();
        uiFabAutoscanMode.hide();
        uiFabAddFloorplanFromPic.hide();
        uiFabAddFloorplanFromGallery.hide();
        uiFabMapRotateLock.hide();
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

    private void updateOperationFabsState() {
        switch (uiFloorPlanView.operation) {
            case ADD_WALL:
                calmFab(uiFabDeleteWall);
                exciteFab(uiFabAddWall);
                calmFab(uiFabSetLocation);
                break;
            case REMOVE_WALL:
                exciteFab(uiFabDeleteWall);
                calmFab(uiFabAddWall);
                calmFab(uiFabSetLocation);
                break;
            case SET_LOCATION:
                calmFab(uiFabDeleteWall);
                calmFab(uiFabAddWall);
                exciteFab(uiFabSetLocation);
                break;
            case NONE:
                calmFab(uiFabDeleteWall);
                calmFab(uiFabAddWall);
                calmFab(uiFabSetLocation);
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(mWifiScanner, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        // for the system's orientation sensor registered listeners
        mHaveRotation = mSensorManager.registerListener(this, mRotation, SensorManager.SENSOR_DELAY_GAME);
        if (!mHaveRotation) {
            mHaveGravity = mSensorManager.registerListener(this, mGravity, SensorManager.SENSOR_DELAY_GAME);

            // if there is a gravity sensor we do not need the accelerometer
            if (!mHaveGravity) {
                mHaveAccelerometer = mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
            }
            mHaveMagnetometer = mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_GAME);
        }

        // Step detector
        mHaveStepDetector = mSensorManager.registerListener(this, mStepDetector, SensorManager.SENSOR_DELAY_UI);

        mWifiScanner.enable();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // to stop the listener and save battery
        mSensorManager.unregisterListener(this);
        unregisterReceiver(mWifiScanner);
    }

    protected float[] lowPass( float[] newSensorData, float[] oldSensorData ) {
        if ( oldSensorData == null ) return newSensorData;

        for ( int i=0; i < newSensorData.length; i++ ) {
            oldSensorData[i] = newSensorData[i] + LOW_PASS_ALPHA * (oldSensorData[i] - newSensorData[i]);
        }
        return oldSensorData;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        boolean gotRotationMatrix = false;

        switch (event.sensor.getType()) {
            case Sensor.TYPE_GRAVITY: {
                mGravitySensorRawData = lowPass(event.values.clone(), mGravitySensorRawData);
                break;
            }
            case Sensor.TYPE_ACCELEROMETER: {
                mGravitySensorRawData = lowPass(event.values.clone(), mGravitySensorRawData);
                break;
            }
            case Sensor.TYPE_MAGNETIC_FIELD: {
                mGeomagneticSensorRawData = lowPass(event.values.clone(), mGeomagneticSensorRawData);
                break;
            }
            case Sensor.TYPE_ROTATION_VECTOR: {
                // calculate the rotation matrix
                SensorManager.getRotationMatrixFromVector( mRotationMatrix, event.values );
                gotRotationMatrix = true;
                break;
            }
            case Sensor.TYPE_STEP_DETECTOR: {
                if (mAutoScanEnabled) {
                    final float offsetX = (float) (Math.sin(Math.toRadians(currentDegree)) * STEP_LENGTH);
                    final float offsetY = (float) (Math.cos(Math.toRadians(currentDegree)) * STEP_LENGTH);
                    uiFloorPlanView.updateOffset(offsetX, -offsetY);

                    mTravelledDistance += STEP_LENGTH;
                    if (mTravelledDistance >= WIFIMARK_SPACING) {
                        // Place WifiMark at center of the screen
                        uiFloorPlanView.setLocation(uiFloorPlanView.getWidth()/2, uiFloorPlanView.getHeight()/2);
                        mTravelledDistance = 0;
                    }
                }
            }
         }

        if (event.sensor.getType() != Sensor.TYPE_ROTATION_VECTOR) {
            if (mGravitySensorRawData != null && mGeomagneticSensorRawData != null) {
                gotRotationMatrix = SensorManager.getRotationMatrix(mRotationMatrix, mInclinationMatrix,
                        mGravitySensorRawData, mGeomagneticSensorRawData);
            }
        }

        if (gotRotationMatrix) {
            SensorManager.getOrientation(mRotationMatrix, mOrientation);
            float degree = Math.round(Math.toDegrees(mOrientation[0]) + mapNorth);
            if (!mIsMapRotationLocked) {
                uiFloorPlanView.updateAngle(currentDegree - degree);
            }
            currentDegree = degree;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not in use
    }
}
