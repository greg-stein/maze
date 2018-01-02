package com.example.neutrino.maze.ui;

import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.neutrino.maze.AppSettings;
import com.example.neutrino.maze.R;
import com.example.neutrino.maze.core.FloorWatcher;
import com.example.neutrino.maze.core.IFloorChangedHandler;
import com.example.neutrino.maze.core.IMainView;
import com.example.neutrino.maze.core.IMazePresenter;
import com.example.neutrino.maze.core.MazeClient;
import com.example.neutrino.maze.core.WiFiLocator;
import com.example.neutrino.maze.floorplan.Building;
import com.example.neutrino.maze.floorplan.Fingerprint;
import com.example.neutrino.maze.floorplan.FloorPlan;
import com.example.neutrino.maze.floorplan.IFloorPlanPrimitive;
import com.example.neutrino.maze.floorplan.Path;
import com.example.neutrino.maze.floorplan.Tag;
import com.example.neutrino.maze.floorplan.Wall;
import com.example.neutrino.maze.rendering.FloorPlanRenderer;
import com.example.neutrino.maze.rendering.FloorPlanView;
import com.example.neutrino.maze.rendering.FloorPlanView.IOnLocationPlacedListener;
import com.example.neutrino.maze.rendering.ElementsRenderGroup;
import com.example.neutrino.maze.util.IFuckingSimpleCallback;
import com.example.neutrino.maze.util.IFuckingSimpleGenericCallback;
import com.example.neutrino.maze.util.PermissionsHelper;
import com.example.neutrino.maze.vectorization.FloorplanVectorizer;
import com.github.fafaldo.fabtoolbar.widget.FABToolbarLayout;
import com.lapism.searchview.SearchView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import static com.example.neutrino.maze.vectorization.HoughTransform.LineSegment;

public class MainActivity extends AppCompatActivity implements IOnLocationPlacedListener, VectorizeDialog.ICompleteVectorizationHandler, IMainView {
    // GUI-related fields
    private SearchView uiSearchView;
    private RecyclerView uiRecView;
    private LinearLayout uiRecPanel;
    private View uiRecPanelSpacer;
    private TagsAdapter mAdapter;
    private FloorPlanView uiFloorPlanView;
    private FloatingActionButton uiFabFindMeOnMap;

    private TextView txtWallLength;
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
        addSpinnerData.add(new Pair<>("Teleport", R.drawable.ic_elevator_black_24dp));
        addSpinnerData.add(new Pair<>("", R.drawable.ic_add_white_24dp));
    }

    // Map north angle
    private float mMapNorth = 0.0f;
    private boolean mIsMapRotationLocked = false;
    private float mDegreeOffset;
    private float mCurrentDegree = 0f;
    private FloorPlan mFloorPlan = FloorPlan.build();
    private FloorWatcher mFloorWatcher;

    private UiMode mUiMode = UiMode.MAP_VIEW_MODE;
    private boolean mAutoScanEnabled = false;
    private Menu mEditMenu;

    private IFloorChangedHandler mFloorChangedHandler;
    private float mCurrentWallLength;

    private IMazePresenter mPresenter;
    private IFuckingSimpleGenericCallback<UiMode> mUiModeChangedListener;
    private IFuckingSimpleGenericCallback<Boolean> mMapperEnabledChangedListener;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (PermissionsHelper.handleLocationPermissions(this, requestCode, grantResults)) {
            this.finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mPresenter = new MazeClient(this, this);
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
        txtWallLength = (TextView) findViewById(R.id.txt_wall_length);
//        uiFabRemoveLastFingerprint = (FloatingActionButton) findViewById(R.id.fab_remove_last_fingerprint);

        mFabAlpha = getAlphaFromRes();

        // initialize your android device sensor capabilities
        if (AppSettings.inDebug) {
//            mLocator.addDistributionUpdatedListener(this);
        }

        mAdapter = new TagsAdapter(this);
        uiRecView.setAdapter(mAdapter);

        ImageSpinnerAdapter adapter =
                new ImageSpinnerAdapter(this, R.layout.add_spinner_item, R.id.lbl_item_text, addSpinnerData);
        uiAddSpinner.setAdapter(adapter);
        // Select last hidden item (+)
        uiAddSpinner.setSelection(addSpinnerData.size() - 1);
        setSupportActionBar(uiToolbar);
        getSupportActionBar().setTitle(null);

        setUiListeners();

        // TODO: commented-out temporarily
//        mFloorWatcher = FloorWatcher.getInstance(this);
//        mFloorWatcher.addOnFloorChangedListenerHandler(mFloorChangedHandler);
        mPresenter.onCreate();
    }

    @Override
    protected void onDestroy() {
        mPresenter.onDestroy();
        super.onDestroy();
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
            mUiMode = UiMode.MAP_VIEW_MODE;
            emitUiModeChangedEvent(mUiMode);
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
                uiFloorPlanView.mapOperation = IMainView.MapOperation.MOVE;
                break;

            case R.id.btn_delete:
                uiFloorPlanView.mapOperation = IMainView.MapOperation.REMOVE;
                break;

            case R.id.btn_lock_rotation:
                if (mIsMapRotationLocked) {
                    mMapNorth = mDegreeOffset;
                    mPresenter.setMapNorth(mMapNorth);
                }
                mIsMapRotationLocked = !mIsMapRotationLocked;
                break;

            case R.id.btn_set_location:
                uiFloorPlanView.mapOperation = IMainView.MapOperation.SET_LOCATION;
                break;

            case R.id.btn_vectorize_floorplan:
                if (Building.isFloorDefined()) {
                    VectorizeDialog newFragment = new VectorizeDialog();
                    newFragment.show(getFragmentManager(), "vectorize_dialog");
                } else {
                    Toast.makeText(this, "Before creating floor plan you should create " +
                            "building with at least single floor", Toast.LENGTH_LONG).show();
                }
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
                    emitMapperEnabledChangedEvent(true);
                } else {
                    emitMapperEnabledChangedEvent(false);
                }
                break;
                // Here come the rest of menu items

            case R.id.btn_new_floorplan:
                NewFloorDialog nfd = new NewFloorDialog(this);
                nfd.setFloorChangedHandler(mFloorChangedHandler);
                nfd.show();
                break;

            case R.id.btn_set_scale:
                AlertDialog.Builder scaleDialogBuilder = new AlertDialog.Builder(this);
                final EditText input = new EditText(this);
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                input.setText(txtWallLength.getText());

                scaleDialogBuilder
                        .setTitle("Set real length to scale floor plan")
                        .setView(input)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                final float realLength = Float.parseFloat(input.getText().toString());
//                                uiFloorPlanView.rescaleMap(realLength/mCurrentWallLength);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                            }
                        })
                        .show();
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
                        uiFloorPlanView.operand = IMainView.MapOperand.WALL;
                        break;
                    case 1:
                        uiFloorPlanView.operand = IMainView.MapOperand.SHORT_WALL;
                        break;
                    case 2:
                        uiFloorPlanView.operand = IMainView.MapOperand.BOUNDARIES;
                        break;
                    case 3:
                        uiFloorPlanView.operand = IMainView.MapOperand.LOCATION_TAG;
                        break;
                    case 4:
                        uiFloorPlanView.operand = IMainView.MapOperand.TELEPORT;
                        break;
                }
                if (position < 5) {
                    uiFloorPlanView.mapOperation = IMainView.MapOperation.ADD;
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
                mUiMode = UiMode.MAP_EDIT_MODE;
                emitUiModeChangedEvent(mUiMode);
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

//        txtWallLength.setOnEditorActionListener(new EditText.OnEditorActionListener() {
//            @Override
//            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
//                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//                    imm.hideSoftInputFromWindow(txtWallLength.getWindowToken(), 0);
//
//                    float realLength = Float.parseFloat(txtWallLength.getText().toString());
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
//                uiFloorPlanView.clearSketch();
//                return false;
//            }
//        });

        uiFloorPlanView.setOnLocationPlacedListener(this);

        uiFloorPlanView.setOnWallLengthChangedListener(new FloorPlanRenderer.IWallLengthChangedListener() {
            @Override
            public void onWallLengthChanged(float wallLength) {
                mCurrentWallLength = wallLength;
                txtWallLength.setText(String.format(Locale.US,"%.2fm", wallLength));
            }
        });

        uiFloorPlanView.setOnWallLengthDisplay(new FloorPlanRenderer.IWallLengthChangedListener() {
            @Override
            public void onWallLengthChanged(float wallLength) {
                txtWallLength.setVisibility(View.VISIBLE);
                uiAddSpinner.setVisibility(View.GONE);
                mCurrentWallLength = wallLength;
                txtWallLength.setText(String.format(Locale.US,"%.2fm", wallLength));
            }
        });

        uiFloorPlanView.setOnWallLengthHide(new IFuckingSimpleCallback() {
            @Override
            public void onNotified() {
                txtWallLength.setVisibility(View.GONE);
                uiAddSpinner.setVisibility(View.VISIBLE);
            }
        });

//        ViewTreeObserver vto = uiFloorPlanView.getViewTreeObserver();
//        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//            @Override
//            public void onGlobalLayout() {
//                // TODO: Floor Id should be recieved from NewFloorPlanDialog or from server
//                String jsonString = MazeServerMock.getInstance(MainActivity.this).downloadFloorPlanJson("mock");
//
//                // TODO: This code shgould be removed as it appears in onFloorChanged event handler
//                new LoadFloorPlanTask(MainActivity.this).onFinish(new LoadFloorPlanTask.AsyncResponse() {
//                    @Override
//                    public void onFinish(FloorPlan floorPlan) {
//                        mFloorPlan = floorPlan;
//                        mLocator.setFloorPlan(mFloorPlan);
//                        mMapper.setFloorPlan(mFloorPlan);
//
//                        mAdapter.updateListData(floorPlan.getTags());
//
//                        // Find point that should be visible after the floorplan is loaded
//                        PointF pointToShow = null;
//
//                        List<IFloorPlanPrimitive> sketch = mFloorPlan.getSketch();
//                        for (IFloorPlanPrimitive primitive : sketch) {
//                            if (primitive instanceof Wall) {
//                                pointToShow = ((Wall)primitive).getStart();
//                                break;
//                            }
//                        }
//                        // The main work is done on GL thread!
//                        uiFloorPlanView.plot(floorPlan, pointToShow);
//                    }
//                }).execute(jsonString);
//
//                uiFloorPlanView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
//            }
//        });
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
        mPresenter.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPresenter.onPause();
    }

    public void setMapRotation(double orientation) {
        float degree = (float) (orientation - mMapNorth);

        mDegreeOffset = degree - mCurrentDegree;
        if (!mIsMapRotationLocked) {
            uiFloorPlanView.updateAngle(mDegreeOffset);
            mCurrentDegree = degree;
        }
    }

    @Override
    public UiMode getUiMode() {
        return mUiMode;
    }

    @Override
    public void setUiModeChangedListener(IFuckingSimpleGenericCallback<UiMode> listener) {
        mUiModeChangedListener = listener;
    }

    @Override
    public void setMapperEnabledChangedListener(IFuckingSimpleGenericCallback<Boolean> listener) {
        mMapperEnabledChangedListener = listener;
    }

    @Override
    public void renderFingeprint(Fingerprint fingerprint) {
        uiFloorPlanView.placeFingerprint(fingerprint);
    }

    @Override
    public void setElementFactory(IElementFactory factory) {
        uiFloorPlanView.setElementFactory(factory);
    }

    private void emitUiModeChangedEvent(UiMode newMode) {
        if (mUiModeChangedListener != null) {
            mUiModeChangedListener.onNotify(newMode);
        }
    }

    private void emitMapperEnabledChangedEvent(boolean enabled) {
        if (mMapperEnabledChangedListener != null) {
            mMapperEnabledChangedListener.onNotify(enabled);
        }
    }

    public void updateLocation(PointF location) {
        uiFloorPlanView.setLocation(location);

        if (AppSettings.inDebug) {
            uiFloorPlanView.highlightCentroidMarks(WiFiLocator.centroidMarks);
        }
    }

    @Override
    public void onLocationSetByUser(PointF location) {
        mPresenter.setLocationByUser(location);
    }

    public void drawDistribution(PointF mean, float stdev) {
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
        // TODO: Instead  of setting sketch, new floor plan should be instantiated. This is because
        // TODO: the collection of elements is kept threadsafe within floorplan.
        mFloorPlan.setSketch(walls);

        PointF pointToShow = ((Wall)walls.get(0)).getStart(); // we know it is a wall
        uiFloorPlanView.plot(mFloorPlan, pointToShow);
    }

    @Override
    public void init() {

    }

    @Override
    public ElementsRenderGroup render(List<IFloorPlanPrimitive> elements) {
        ElementsRenderGroup floorPlanGroup = uiFloorPlanView.renderAsGroup(elements);

        return floorPlanGroup;
    }

    @Override
    public void centerMapView(PointF point) {
        uiFloorPlanView.centerToPoint(point);
    }

    @Override
    public void setTags(List<Tag> tags) {
        mAdapter.updateListData(tags);
    }
}
