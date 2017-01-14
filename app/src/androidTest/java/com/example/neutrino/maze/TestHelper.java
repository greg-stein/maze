package com.example.neutrino.maze;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

import java.io.InputStream;

/**
 * Created by Greg Stein on 1/12/2017.
 */
public class TestHelper {
    public static InputStream openFileFromResources(String path) {
        Class<? extends TestHelper> aClass = TestHelper.class;
        InputStream in_s = aClass.getResourceAsStream(path);
        return in_s;
    }

    public static Bitmap readBitmapFromResources(String bitmapRes) {
        Bitmap bitmapFromRes = null;

        try {
            InputStream in_s = openFileFromResources(bitmapRes);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            Rect erect = new Rect();
            bitmapFromRes = BitmapFactory.decodeStream(in_s, erect, options);
        } catch (Exception e) {
            e.printStackTrace(); // АААА! Жопа!! жопА!!
        }

        return bitmapFromRes;
    }
}
