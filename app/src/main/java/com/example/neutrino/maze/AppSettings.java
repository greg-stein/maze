package com.example.neutrino.maze;

import android.content.Context;
import android.support.v4.content.ContextCompat;

/**
 * Created by Greg Stein on 8/21/2016.
 */
public class AppSettings {
    public static int wallColor;
    public static int mapBgColor;
    public static int primaryColor;
    public static int primaryDarkColor;
    public static int editModeColor;
    public static int accentColor;

    public static void init(Context context) {
        wallColor = ContextCompat.getColor(context, R.color.colorWall);
        mapBgColor = ContextCompat.getColor(context, R.color.colorMapBackground);
        primaryColor = ContextCompat.getColor(context, R.color.colorPrimary);
        primaryDarkColor = ContextCompat.getColor(context, R.color.colorPrimaryDark);
        editModeColor = ContextCompat.getColor(context, R.color.colorEditMode);
        accentColor = ContextCompat.getColor(context, R.color.colorAccent);
    }
}
