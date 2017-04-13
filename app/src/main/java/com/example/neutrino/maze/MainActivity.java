package com.example.neutrino.maze;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.net.Uri;
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

import com.example.neutrino.maze.Locator.ILocationUpdatedListener;
import com.example.neutrino.maze.floorplan.FloorPlan;
import com.example.neutrino.maze.floorplan.FloorPlanSerializer;
import com.example.neutrino.maze.floorplan.IFloorPlanPrimitive;
import com.example.neutrino.maze.floorplan.PersistenceLayer;
import com.example.neutrino.maze.floorplan.Wall;
import com.example.neutrino.maze.rendering.FloorPlanRenderer;
import com.example.neutrino.maze.rendering.FloorPlanView;
import com.example.neutrino.maze.rendering.VectorHelper;
import com.example.neutrino.maze.vectorization.FloorplanVectorizer;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.example.neutrino.maze.SensorListener.IDeviceRotationListener;

public class MainActivity extends AppCompatActivity implements IDeviceRotationListener, ILocationUpdatedListener {
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
    private float mMapNorth = 0.0f;

    private boolean mIsMapRotationLocked = false;

    private float mDegreeOffset;
    private float mCurrentDegree = 0f;
    private FloorPlan mFloorPlan = FloorPlan.build();
    private SensorListener mSensorListener;
    private WifiScanner mWifiScanner;
    private Locator mLocator;
    private WiFiLocator mWiFiLocator = WiFiLocator.getInstance();

    private boolean mPlacedMarkAtCurrentLocation = true;

    private static final PointF mCurrentLocation = new PointF();
    private float mCurrentWallLength = 1;
    private boolean mAutoScanEnabled = false;

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

        AppSettings.init(this);
        mFabAlpha = getAlphaFromRes();

        // initialize your android device sensor capabilities
        mWifiScanner = WifiScanner.getInstance();
        mSensorListener = SensorListener.getInstance();
        mSensorListener.addDeviceRotationListener(this);
        mLocator = Locator.getInstance();
        mLocator.addLocationUpdatedListener(this);

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

        List<IFloorPlanPrimitive> walls = FloorplanVectorizer.vectorize(floorplanBitmap);
        mFloorPlan.setSketch(walls);
        uiFloorPlanView.plot(walls, false); // not in init phase
//        mWiFiLocator.walls = walls;
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
                    mMapNorth = mDegreeOffset;
                }
                mDegreeOffset = 0;

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
                uiFloorPlanView.centerToPoint(mCurrentLocation);
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

//        mWifiScanner.addFingerprintAvailableListener(new WifiScanner.IFingerprintAvailableListener() {
//            @Override
//            public void onFingerprintAvailable(WiFiFingerprint wiFiFingerprint) {
//                if (!mPlacedMarkAtCurrentLocation) {
//                    uiFloorPlanView.placeWiFiMarkAt(mCurrentLocation, wiFiFingerprint);
//                    // TODO: There is costly stupidity in these two lines:
//                    mWiFiLocator.marks = uiFloorPlanView.getPrimitives(Fingerprint.class);
//                    mWiFiLocator.walls = uiFloorPlanView.getPrimitives(Wall.class);
//                    mPlacedMarkAtCurrentLocation = true;
//                }
//                mWiFiLocator.setCurrentFingerprint(wiFiFingerprint);
//            }
//        });

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
                    List<Object> floorplan = FloorPlanSerializer.deserializeFloorPlan(jsonString);
                    mFloorPlan = FloorPlan.build(floorplan);
                    uiFloorPlanView.plot(mFloorPlan.getSketch());
                    mWiFiLocator.setFingerprintsMap(mFloorPlan.getFingerprints());
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

                    String jsonString = FloorPlanSerializer.serializeFloorPlan(mFloorPlan.disassemble());
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
        mSensorListener.onActivityResume();
        mWifiScanner.onActivityResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // to stop the listener and save battery
        mSensorListener.onActivityPause();
        mWifiScanner.onActivityPause();
    }

    private boolean obstaclesBetween(PointF p1, PointF p2) {
        List<IFloorPlanPrimitive> sketch = mFloorPlan.getSketch();
        for (IFloorPlanPrimitive primitive : sketch) {
            if (primitive instanceof Wall) {
                Wall wall = (Wall) primitive;
                if (VectorHelper.linesIntersect(wall.getA(), wall.getB(), p1, p2)) return true;
            }
        }
        return false;
    }

    @Override
    public void onDeviceRotated(float degree) {
        float correctedDegree = degree + mMapNorth;

        mDegreeOffset = mCurrentDegree - correctedDegree;
        if (!mIsMapRotationLocked) {
            uiFloorPlanView.updateAngle(mDegreeOffset);
            mCurrentDegree = correctedDegree;
        }
    }

    @Override
    public void onLocationUpdated(PointF location) {
        mCurrentLocation.set(location);
        uiFloorPlanView.setLocation(location);
//        uiFloorPlanView.highlightCentroidMarks(WiFiLocator.centroidMarks);
    }
}
