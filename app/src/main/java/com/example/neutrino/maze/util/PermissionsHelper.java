package com.example.neutrino.maze.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

/**
 * Created by Greg Stein on 11/7/2017.
 */

public class PermissionsHelper {
    public static final int PERMISSION_LOCATION_REQUEST_CODE = 613;

    public static boolean letDieSilently = false;
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

    public static boolean handleLocationPermissions(Context context, int requestCode, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                locationPermissionsGranted = true;
                // reload activity
            } else {
                Toast.makeText(context, "Maze was not allowed to use location. Hence it couldn't " +
                                "function properly and will be closed. Please consider " +
                                "granting it needed permissions.",
                        Toast.LENGTH_LONG).show();
                locationPermissionsGranted = false;
            }
            return true;
        }

        return false;
    }

}
