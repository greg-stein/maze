package world.maze.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;

import java.util.HashMap;
import java.util.Map;

import world.maze.ui.IUserNotifier;

import static android.content.Context.MODE_PRIVATE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

/**
 * Created by Greg Stein on 11/7/2017.
 */

public class PermissionsHelper {
    private static final String PREFS_FILE_NAME = "permissions.prefs";
    private static final int PERMISSION_REQUEST_CODES_BASE = 613;

    private static int currentPermissionRequestCodeUpperLimit = PERMISSION_REQUEST_CODES_BASE;
    private static Map<String, Object> waitTokens = new HashMap<>();
    private static Map<String, Boolean> permissionWaitResults = new HashMap<>();
    private static Map<Integer, String> permissionRequestCode2Permission = new HashMap<>();
    private static Map<String, Integer> permission2PermissionRequestCode = new HashMap<>();

    private static void firstTimeAskingPermission(Context context, String permission, boolean isFirstTime) {
        SharedPreferences sharedPreference = context.getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE);
        sharedPreference.edit().putBoolean(permission, isFirstTime).apply();
    }

    private static boolean isFirstTimeAskingPermission(Context context, String permission) {
        return context.getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE).getBoolean(permission, true);
    }

    private static boolean shouldAskPermission() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);
    }

    private static int registerPermission(String permission) {
        if (waitTokens.containsKey(permission)) {
            return permission2PermissionRequestCode.get(permission);
        }

        waitTokens.put(permission, new Object());
        permissionWaitResults.put(permission, false);
        permissionRequestCode2Permission.put(currentPermissionRequestCodeUpperLimit, permission);
        permission2PermissionRequestCode.put(permission, currentPermissionRequestCodeUpperLimit);
        return currentPermissionRequestCodeUpperLimit++;
    }

    public static void handlePermission(Context context, final String permission, final boolean isCritical, String rationale,
                                        final IUserNotifier userNotifier, final IFuckingSimpleCallback onPermissionGranted) {

        final Activity activity = CommonHelper.extractActivity(context);
        if (null == activity) return;

        if (ActivityCompat.checkSelfPermission(context, permission) == PERMISSION_GRANTED) {
            onPermissionGranted.onNotified();
        } else {
            final int permissionRequestCode = registerPermission(permission);
            boolean shouldWaitForPermission = false;

            // If on M or newer
            if (shouldAskPermission()) {
                // If permission denied previously
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                    // Show user justification why these permissions are needed
                    new android.app.AlertDialog.Builder(activity).
                            setCancelable(true).
                            setTitle("Permissions necessary").
                            setMessage(rationale).
                            setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    ActivityCompat.requestPermissions(activity, new String[]{permission}, permissionRequestCode);
                                }
                            }).
                            setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    // If user clicked cancel, notify waitToken & indicate that permission is not granted!
                                    permissionWaitResults.put(permission, false);
                                    Object waitToken = waitTokens.get(permission);

                                    synchronized (waitToken) {
                                        waitToken.notifyAll();
                                    }
                                }
                            }).
                            show();
                    shouldWaitForPermission = true;
                } else {
                    // Permission denied or first time requested
                    if (isFirstTimeAskingPermission(context, permission)) {
                        firstTimeAskingPermission(context, permission, false);
                        ActivityCompat.requestPermissions(activity, new String[]{permission}, permissionRequestCode);
                        shouldWaitForPermission = true;
                    } else {
                        // Handle the feature without permission or ask user to manually allow permission
                        if (isCritical) {
                            userNotifier.displayTragicError("Critical permission disabled!",
                                    "You have disabled us from asking for " + permission +
                                            " The app will be closed now. Please consider giving it manually.");
                        } else {
                            userNotifier.displayError("You have disabled requesting for " + permission +
                            ". The associated features of the app will be deactivated", false);
                        }
                        shouldWaitForPermission = false;
                    }

                }

                if (shouldWaitForPermission) {
                    AsyncTask<Void, Void, Boolean> permissionsWaiter = new AsyncTask<Void, Void, Boolean>() {
                        @Override
                        protected Boolean doInBackground(Void... voids) {
                            return PermissionsHelper.waitForPermission(activity, permission);
                        }

                        // Runs on UI thread
                        @Override
                        protected void onPostExecute(Boolean result) {
                            if (true == result) {
                                onPermissionGranted.onNotified();
                            } else {
                                // Permission was denied
                                if (isCritical) {
                                    userNotifier.displayTragicError("Critical permission denied!",
                                            "You have denied critical permission (" + permission + ")." +
                                                    " This time the app will quit. Please consider giving the permission when the app starts next time.");
                                } else {
                                    userNotifier.displayError("You have denied " + permission + " permission" +
                                            ". The associated features of the app will be deactivated", false);
                                }
                            }
                        }
                    };
                    permissionsWaiter.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
                }
            }
        }
    }

    private static Boolean waitForPermission(Activity activity, String permission) {
        if (!waitTokens.containsKey(permission)) return false;

        Object waitToken = waitTokens.get(permission);
        synchronized (waitToken) {
            try {
                waitToken.wait();
            } catch (InterruptedException e) {
                return false; // something went wrong and no permission approved
            }
        }
        Boolean waitResult = permissionWaitResults.get(permission);
        return waitResult;
    }


    public static void onRequestPermissionsResult(Context context, int requestCode, String[] permissions, @NonNull int[] grantResults) {
        String permission = permissionRequestCode2Permission.get(requestCode);
        final boolean isGranted = grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED;
        permissionWaitResults.put(permission, isGranted);
        Object waitToken = waitTokens.get(permission);

        synchronized (waitToken) {
            waitToken.notifyAll();
        }
    }

    public static boolean hasPermission(Context context, String permission) {
        if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }
}
