package com.example.neutrino.maze;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
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

import com.example.neutrino.maze.Locator.ILocationUpdatedListener;
import com.example.neutrino.maze.floorplan.FloorPlan;
import com.example.neutrino.maze.floorplan.IFloorPlanPrimitive;
import com.example.neutrino.maze.floorplan.Path;
import com.example.neutrino.maze.floorplan.Tag;
import com.example.neutrino.maze.rendering.FloorPlanView;
import com.example.neutrino.maze.rendering.FloorPlanView.IOnLocationPlacedListener;
import com.example.neutrino.maze.vectorization.FloorplanVectorizer;
import com.github.fafaldo.fabtoolbar.widget.FABToolbarLayout;
import com.lapism.searchview.SearchView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.example.neutrino.maze.SensorListener.IDeviceRotationListener;

public class MainActivity extends AppCompatActivity implements IDeviceRotationListener, ILocationUpdatedListener, IOnLocationPlacedListener, Locator.IDistributionUpdatedListener {
    static final String IMAGE_FILENAME = "floorplan";
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
        addSpinnerData.add(new Pair<>("Wall", R.drawable.ic_view_stream_black_24dp));
        addSpinnerData.add(new Pair<>("Short wall", R.drawable.ic_remove_black_24dp));
        addSpinnerData.add(new Pair<>("Place boundaries", R.drawable.ic_format_shapes_black_24dp));
        addSpinnerData.add(new Pair<>("Location tag", R.drawable.ic_add_location_black_24dp));
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

        // initialize your android device sensor capabilities
        mWifiScanner = WifiScanner.getInstance();
        mSensorListener = SensorListener.getInstance();
        mSensorListener.addDeviceRotationListener(this);
        mLocator = Locator.getInstance();
        mLocator.addLocationUpdatedListener(this);
        if (AppSettings.inDebug) {
//            mLocator.addDistributionUpdatedListener(this);
        }
        mMapper = Mapper.getInstance();

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
        uiToolbar.setTitle("");

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
            case R.id.action_settings:
                // User chose the "Settings" item, show the app settings UI...
                return true;

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

            case R.id.btn_floorplan_from_gallery:
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"),
                        REQUEST_IMAGE_SELECT);
                break;

            case R.id.btn_floorplan_from_cam:
                // Here comes code for taking floorplan as picture from camera
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
                    InputStream in_s = res.openRawResource(R.raw.haifa_mall_detailed_tags);

                    byte[] b = new byte[in_s.available()];
                    in_s.read(b);
                    jsonString = new String(b);
                } catch (Exception e) {
                     e.printStackTrace();
                }

                new LoadFloorPlanTask().onFinish(new LoadFloorPlanTask.AsyncResponse() {
                    @Override
                    public void onFinish(FloorPlan floorPlan) {
                        mFloorPlan = floorPlan;
                        mLocator.setFloorPlan(mFloorPlan);
                        mMapper.setFloorPlan(mFloorPlan);

                        mAdapter.updateListData(floorPlan.getTags());

                        // The main work is done on GL thread!
                        uiFloorPlanView.plot(floorPlan);
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
                btnRemove.setIcon(getResources().getDrawable(R.drawable.ic_delete_forever_white_24dp, null));
                uiAddSpinner.setSelection(addSpinnerData.size() - 1);
                break;
            case ADD:
                System.out.println("MODE: add");
                btnMove.setIcon(getResources().getDrawable(R.drawable.ic_cursor_move_white_24dp, null));
                btnRemove.setIcon(getResources().getDrawable(R.drawable.ic_delete_forever_white_24dp, null));
                break;
            case REMOVE:
                System.out.println("MODE: remove");
                btnMove.setIcon(getResources().getDrawable(R.drawable.ic_cursor_move_white_24dp, null));
                btnRemove.setIcon(getResources().getDrawable(R.drawable.ic_delete_forever_black_24dp, null));
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
}
