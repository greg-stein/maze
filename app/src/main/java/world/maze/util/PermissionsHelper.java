package world.maze.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;

import static android.content.Context.MODE_PRIVATE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

/**
 * Created by Greg Stein on 11/7/2017.
 */

public class PermissionsHelper {
    public static final int PERMISSION_FINE_LOCATION_REQUEST_CODE = 613;
    public static final int PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST_CODE  = 612;
    public static final int PERMISSION_COARSE_LOCATION_REQUEST_CODE = 611;
    public static final String PREFS_FILE_NAME = "permissions.prefs";

    private static final Object fineLocationPermissionWaitToken = new Object();
    private static final Object coarseLocationPermissionWaitToken = new Object();
    private static final Object storagePermissionWaitToken = new Object();
    private static boolean isFineLocationPermissionGranted;
    private static boolean isStoragePermissionGranted;
    private static boolean isCoarseLocationPermissionGranted;

    /**
     * Callback on various cases on checking permission
     *
     * 1.  Below M, runtime permission not needed. In that case onPermissionGranted() would be called.
     *     If permission is already granted, onPermissionGranted() would be called.
     *
     * 2.  Above M, if the permission is being asked first time onNeedPermission() would be called.
     *
     * 3.  Above M, if the permission is previously asked but not granted, onPermissionPreviouslyDenied()
     *     would be called.
     *
     * 4.  Above M, if the permission is disabled by device policy or the user checked "Never ask again"
     *     check box on previous request permission, onPermissionDisabled() would be called.
     */
    public interface PermissionAskListener {
        /**
         * Callback to ask permission
         */
        void onNeedPermission();
        /**
         * Callback on permission denied
         */
        void onPermissionPreviouslyDenied();
        /**
         * Callback on permission "Never show again" checked and denied
         */
        void onPermissionDisabled();
        /**
         * Callback on permission granted
         */
        void onPermissionGranted();
    }

    public static void firstTimeAskingPermission(Context context, String permission, boolean isFirstTime){
        SharedPreferences sharedPreference = context.getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE);
        sharedPreference.edit().putBoolean(permission, isFirstTime).apply();
    }

    public static boolean isFirstTimeAskingPermission(Context context, String permission){
        return context.getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE).getBoolean(permission, true);
    }

    public static boolean shouldAskPermission() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);
    }

    private static boolean shouldAskPermission(Context context, String permission){
        if (shouldAskPermission()) {
            int permissionResult = ActivityCompat.checkSelfPermission(context, permission);
            if (permissionResult != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    // Returns boolean value indicating whether the permission is enabled
    public static boolean checkPermission(Activity context, String permission, PermissionAskListener listener){
/*
        * If permission is not granted
        * */
        if (shouldAskPermission(context, permission)){
/*
            * If permission denied previously
            * */
            if (ActivityCompat.shouldShowRequestPermissionRationale(context, permission)) {
                listener.onPermissionPreviouslyDenied();
            } else {
                /*
                * Permission denied or first time requested
                * */
                if (isFirstTimeAskingPermission(context, permission)) {
                    firstTimeAskingPermission(context, permission, false);
                    listener.onNeedPermission();
                } else {
                    /*
                    * Handle the feature without permission or ask user to manually allow permission
                    * */
                    listener.onPermissionDisabled();
                    return false;
                }
            }
        } else {
            listener.onPermissionGranted();
        }
        return true;
    }

    public static boolean requestStoragePermission(Context context) {
        if (!(context instanceof Activity)) return false;
        final Activity activity = (Activity) context;

        boolean permissionEnabled =
                checkPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE, new PermissionAskListener() {
            @Override
            public void onNeedPermission() {
                ActivityCompat.requestPermissions(
                        activity,
                        // WRITE_EXTERNAL_STORAGE implicitly brings READ_EXTERNAL_STORAGE
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
            }

            @Override
            public void onPermissionPreviouslyDenied() {
                // Show user justification why these permissions are needed
                new android.app.AlertDialog.Builder(activity).
                        setCancelable(true).
                        setTitle("Permissions necessary").
                        setMessage("We need your permission to access external storage (typically an SD card). " +
                                "This is where Maze stores newly created maps before uploading them to the cloud.").
                        setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(
                                        activity,
                                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
                            }
                        }).
                        show();
            }

            @Override
            public void onPermissionDisabled() {

            }

            @Override
            public void onPermissionGranted() {
                // Notify all threads that the permission is granted.
            }
        });

        return permissionEnabled;
    }

    public static boolean requestCoarseLocationPermission(final Context context) {
        if (!(context instanceof Activity)) return false;
        final Activity activity = (Activity) context;

        boolean permissionEnabled =
            checkPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION, new PermissionAskListener() {
            @Override
            public void onNeedPermission() {
                ActivityCompat.requestPermissions(
                        activity,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSION_COARSE_LOCATION_REQUEST_CODE);
            }

            @Override
            public void onPermissionPreviouslyDenied() {
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
                                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                        PERMISSION_COARSE_LOCATION_REQUEST_CODE);
                            }
                        }).
                        show();
            }

            @Override
            public void onPermissionDisabled() {
            }

            @Override
            public void onPermissionGranted() {
            }
        });
        return permissionEnabled;
    }

    public static void requestFineLocationPermission(final Context context) {
        if (!(context instanceof Activity)) return;
        final Activity activity = (Activity) context;

        checkPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION, new PermissionAskListener() {
            @Override
            public void onNeedPermission() {
                ActivityCompat.requestPermissions(
                        activity,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSION_FINE_LOCATION_REQUEST_CODE);
            }

            @Override
            public void onPermissionPreviouslyDenied() {
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
                                        PERMISSION_FINE_LOCATION_REQUEST_CODE);
                            }
                        }).
                        show();
            }

            @Override
            public void onPermissionDisabled() {
                new android.app.AlertDialog.Builder(context).
                        setCancelable(false).
                        setTitle("Critical permission denied").
                        setMessage("You have denied critical permission (FINE/COARSE_LOCATION). " +
                                "Maze will now quit. Please consider giving it manually.").
                        show();
            }

            @Override
            public void onPermissionGranted() {

            }
        });
    }

    public static boolean requestPermissions(Context context) {
        if (!(context instanceof Activity)) return false;
        final Activity activity = (Activity) context;

        if (!fineLocationPermissionsGranted(context)) {
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
                                        PERMISSION_FINE_LOCATION_REQUEST_CODE);
                            }
                        }).
                        show();
            } else {
                ActivityCompat.requestPermissions(
                        activity,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSION_FINE_LOCATION_REQUEST_CODE);
            }
        }

        // WRITE_EXTERNAL_STORAGE implicitly brings the READ_EXTERNAL_STORAGE permission
        if (!externalStoragePermissionGranted(context)) {

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

    public static boolean fineLocationPermissionsGranted(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PERMISSION_GRANTED;
    }

    public static boolean coarseLocationPermissionsGranted(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED;
    }

    public static boolean externalStoragePermissionGranted(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED;
    }

    public static boolean onRequestPermissionsResult(Context context, int requestCode, String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_FINE_LOCATION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                    isFineLocationPermissionGranted = true;
                } else {
                    isFineLocationPermissionGranted = false;
                }
                // Notify threads who are waiting for this permission
                synchronized (fineLocationPermissionWaitToken) {
                    fineLocationPermissionWaitToken.notifyAll();
                }
                return true; // Critical permission

            case PERMISSION_COARSE_LOCATION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                    isCoarseLocationPermissionGranted = true;
                } else {
                    isCoarseLocationPermissionGranted = false;
                }
                // Notify threads who are waiting for this permission
                synchronized (coarseLocationPermissionWaitToken) {
                    coarseLocationPermissionWaitToken.notifyAll();
                }
                return true; // Critical permission

            case PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                    isStoragePermissionGranted = true;
                } else {
                    isStoragePermissionGranted = false;
                }
                // Notify threads who are waiting for this permission
                synchronized (storagePermissionWaitToken) {
                    storagePermissionWaitToken.notifyAll();
                }
                return false; // Not critical permission
        }

        return false;
    }

    public static boolean waitForFineLocationPermission(Context context) {
        if (!(context instanceof Activity)) return false;
        final Activity activity = (Activity) context;

        if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
            if (!isFirstTimeAskingPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                return false; // this permission was disabled and user checked "Don't ask me again"
            }
        }

        synchronized (fineLocationPermissionWaitToken) {
            try {
                fineLocationPermissionWaitToken.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false; // something went wrong and no permission approved
            }
        }

        return isFineLocationPermissionGranted;
    }

    public static boolean waitForCoarseLocationPermission(Context context) {
        if (!(context instanceof Activity)) return false;
        final Activity activity = (Activity) context;

        if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            if (!isFirstTimeAskingPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                return false; // this permission was disabled and user checked "Don't ask me again"
            }
        }

        synchronized (coarseLocationPermissionWaitToken) {
            try {
                coarseLocationPermissionWaitToken.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false; // something went wrong and no permission approved
            }
        }

        return isCoarseLocationPermissionGranted;
    }

    public static boolean waitForStoragePermission(Context context) {
        if (!(context instanceof Activity)) return false;
        final Activity activity = (Activity) context;
//        if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
//            if (!isFirstTimeAskingPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
//                return false; // this permission was disabled and user checked "Don't ask me again"
//            }
//        }

        synchronized (storagePermissionWaitToken) {
            try {
                storagePermissionWaitToken.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false; // something went wrong and no permission approved
            }
        }

        return isStoragePermissionGranted;
    }
}
