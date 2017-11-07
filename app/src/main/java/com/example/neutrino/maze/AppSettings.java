package com.example.neutrino.maze;

import android.content.Context;
import android.support.v4.content.ContextCompat;

/**
 * Created by Greg Stein on 8/21/2016.
 */
public class AppSettings {
    public static final String CONFIG_FILE = "maze.conf";

    public static int locationMarkColor;
    public static int footprintColor;
    public static int wallColor;
    public static int mapBgColor;
    public static int primaryColor;
    public static int primaryDarkColor;
    public static int editModeColor;
    public static int accentColor;
    public static int oglProgram;
    public static int oglTextRenderProgram;
    public static boolean inDebug;
    public static int pathColor;

    public static void init(Context context) {
        pathColor = ContextCompat.getColor(context, R.color.colorPath);
        locationMarkColor = ContextCompat.getColor(context, R.color.colorLocationMark);
        footprintColor = ContextCompat.getColor(context, R.color.colorFootprint);
        wallColor = ContextCompat.getColor(context, R.color.colorWall);
        mapBgColor = ContextCompat.getColor(context, R.color.colorMapBackground);
        primaryColor = ContextCompat.getColor(context, R.color.colorPrimary);
        primaryDarkColor = ContextCompat.getColor(context, R.color.colorPrimaryDark);
        editModeColor = ContextCompat.getColor(context, R.color.colorEditMode);
        accentColor = ContextCompat.getColor(context, R.color.colorAccent);
        inDebug = true;
    }
}
