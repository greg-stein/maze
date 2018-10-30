package world.maze.util;

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
    public static final int PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST_CODE  = 612;

    public static boolean permissionsWereAlreadyGranted = true;
    public static boolean locationPermissionsGranted = false;
    public static boolean externalStoragePermissionsGranted = false;

    public static boolean requestPermissions(Context context) {
        if (!(context instanceof Activity)) return false;

        final Activity activity = (Activity) context;

        if (!locationPermissionsGranted(context)) {
            permissionsWereAlreadyGranted = false;
            // Request permissions
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
                                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                                                Manifest.permission.ACCESS_FINE_LOCATION},
                                        PERMISSION_LOCATION_REQUEST_CODE);
                            }
                        }).
                        show();
            } else {
                ActivityCompat.requestPermissions(
                        activity,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSION_LOCATION_REQUEST_CODE);
            }
        }

        // WRITE_EXTERNAL_STORAGE implicitly brings the READ_EXTERNAL_STORAGE permission
        if (!externalStoragePermissionGranted(context)) {
            permissionsWereAlreadyGranted = false;

            // Request permissions
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show user justification why these permissions are needed
                new android.app.AlertDialog.Builder(context).
                        setCancelable(true).
                        setTitle("Permissions necessary").
                        setMessage("We need your permission to access external storage (typically an SD card). " +
                                "This is where Maze stores newly created maps before uploading them to the cloud.").
                        setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(
                                        activity,
                                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                Manifest.permission.READ_EXTERNAL_STORAGE},
                                        PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
                            }
                        }).
                        show();
            } else {
                ActivityCompat.requestPermissions(
                        activity,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
            }
        }
        return true;
    }

    public static boolean locationPermissionsGranted(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED;
    }

    public static boolean externalStoragePermissionGranted(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED;
    }

    public static boolean onRequestPermissionsResult(Context context, int requestCode, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_LOCATION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                    locationPermissionsGranted = true;
                    // reload activity
                } else {
                    Toast.makeText(context, "Maze was not allowed to use location. Hence it couldn't " +
                                    "function properly and will be closed.",
                            Toast.LENGTH_LONG).show();
                    locationPermissionsGranted = false;
                }
                return true; // Critical permission

            case PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                    externalStoragePermissionsGranted = true;
                    // reload activity
                } else {
                    Toast.makeText(context, "Maze was not allowed to use external storage. " +
                                    "This will prevent you from creating new maps. Please consider " +
                                    "granting it if you desire to map indoors.",
                            Toast.LENGTH_LONG).show();
                    externalStoragePermissionsGranted = false;
                }
                return false; // Not critical permission
        }

        return false;
    }

}
