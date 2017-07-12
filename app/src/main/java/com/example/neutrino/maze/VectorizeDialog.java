package com.example.neutrino.maze;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.Window;
import android.widget.ImageView;

/**
 * Created by Greg Stein on 7/11/2017.
 */

public class VectorizeDialog extends Dialog {
    private ImageView imgGrayscale;

    public VectorizeDialog(@NonNull Context context) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.vectorize_dialog);

        imgGrayscale = (ImageView) findViewById(R.id.img_grayscale);
    }


}
