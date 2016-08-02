package com.example.neutrino.maze;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // GUI-related fields
    private FloorPlanView uiFloorPlanView;
    private Toolbar uiToolbar;
    private FloatingActionButton uiFab;

    // Map north angle
    private float mapNorth = 0.0f;

    // record the compass picture angle turned
    private float currentDegree = 0f;

    // device sensor manager
    private SensorManager mSensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        uiFloorPlanView = (FloorPlanView) findViewById(R.id.ui_MapContainer);
        uiToolbar = (Toolbar) findViewById(R.id.toolbar);
        uiFab = (FloatingActionButton) findViewById(R.id.fab);
        setSupportActionBar(uiToolbar);

        uiFab.setOnTouchListener(new View.OnTouchListener() {
                                     @Override
                                     public boolean onTouch(View view, MotionEvent motionEvent) {
                                         uiFloorPlanView.loadEngine();
                                         return true;
                                     }
                                 }
        );

        // initialize your android device sensor capabilities
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
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

        // for the system's orientation sensor registered listeners
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // to stop the listener and save battery
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ORIENTATION: {
                // get the angle around the z-axis rotated
                float degree = Math.round(event.values[0] + mapNorth);

                uiFloorPlanView.updateAngle(currentDegree - degree);
                currentDegree = degree;
                break;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not in use
    }
}
