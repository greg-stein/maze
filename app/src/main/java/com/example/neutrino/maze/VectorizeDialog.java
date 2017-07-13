package com.example.neutrino.maze;

import android.app.DialogFragment;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.example.neutrino.maze.vectorization.FloorplanVectorizer;

import java.io.File;
import java.io.IOException;

import static android.app.Activity.RESULT_OK;

/**
 * Created by Greg Stein on 7/11/2017.
 */

public class VectorizeDialog extends DialogFragment {
    static final String IMAGE_FILENAME = "floorplan";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_SELECT = 2;

    private Button btnGallery;
    private Button btnCamera;
    private ImageView imgGrayscale;
    private SeekBar sbThreshold;

    protected String mCurrentImagePath;
    private Bitmap mGrayscaled;
    private Bitmap mBinary;
    private int mThreshold;
    private BinarizeImageTask mBinarizingTask;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.vectorize_dialog, container, false);
        getDialog().setTitle("Simple Dialog");

        btnCamera = (Button) rootView.findViewById(R.id.btn_image_from_cam);
        btnGallery = (Button) rootView.findViewById(R.id.btn_image_from_gallery);
        imgGrayscale = (ImageView) rootView.findViewById(R.id.img_grayscale);
        sbThreshold = (SeekBar) rootView.findViewById(R.id.sb_threshold);

        setUiListeners();
        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) return;

        Bitmap floorplanBitmap = null;

        switch (requestCode) {
            case REQUEST_IMAGE_CAPTURE: {
                floorplanBitmap = loadBitmapFromFile(mCurrentImagePath);
                break;
            }
            case REQUEST_IMAGE_SELECT: {
                Uri selectedImageUri = data.getData();
                mCurrentImagePath = getPath(selectedImageUri);

                if (mCurrentImagePath == null) {
                    floorplanBitmap = loadPicasaImageFromGallery(selectedImageUri);
                } else {
                    floorplanBitmap = loadBitmapFromFile(mCurrentImagePath);
                }
                break;
            }
        }

        int maxWidth = imgGrayscale.getMaxWidth();
        int maxHeight = this.getDialog().getWindow().getDecorView().getHeight()/2;
        Bitmap resized = FloorplanVectorizer.resize(floorplanBitmap, maxWidth, maxHeight);
        mGrayscaled = FloorplanVectorizer.toGrayscale(resized, FloorplanVectorizer.PADDING);
        resized.recycle();

        mThreshold = FloorplanVectorizer.calcOtsuThreshold(mGrayscaled);
        sbThreshold.setProgress(mThreshold);
        mBinary = FloorplanVectorizer.toBinary(mGrayscaled, mThreshold, null);

        imgGrayscale.setImageBitmap(mBinary);
//        List<IFloorPlanPrimitive> walls = FloorplanVectorizer.vectorize(floorplanBitmap);
//        mFloorPlan.setSketch(walls);
//        uiFloorPlanView.plot(walls, false); // not in init phase
//        uiFloorPlanView.showMap();

    }

    public String getPath(Uri uri) {
        String[] projection = {MediaStore.MediaColumns.DATA};
        Cursor cursor = getActivity().getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            String filePath = cursor.getString(columnIndex);
            cursor.close();
            return filePath;
        } else
            return uri.getPath();               // FOR OI/ASTRO/Dropbox etc
    }

    private static Bitmap loadBitmapFromFile(String mCurrentImagePath) {
        Bitmap bitmap = null;
        File imageFile = new File(mCurrentImagePath);

        if (imageFile.exists()) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inMutable = true;
            bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
        }

        return bitmap;
    }

    // NEW METHOD FOR PICASA IMAGE LOAD
    private Bitmap loadPicasaImageFromGallery(final Uri uri) {
        Bitmap floorplanBitmap = null;
        String[] projection = {  MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME };
        Cursor cursor = getActivity().getContentResolver().query(uri, projection, null, null, null);

        if(cursor != null) {
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
            if (columnIndex != -1) {
                try {
                    floorplanBitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        cursor.close();
        return floorplanBitmap;
    }

    private void setUiListeners() {
        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Here comes code for taking floorplan as picture from camera
                // Dispatch Take Picture Intent
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                    File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                    try {
                        File imageFile = File.createTempFile(IMAGE_FILENAME, ".jpg", storageDir);
                        mCurrentImagePath = imageFile.getAbsolutePath();
                        Uri imageFileUri = Uri.fromFile(imageFile);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageFileUri);
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                    } catch (IOException e) {
                        Toast.makeText(getActivity(), "Error saving image", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }

            }
        });

        btnGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"),
                        REQUEST_IMAGE_SELECT);
            }
        });

        sbThreshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mThreshold = progress;
                if (fromUser & mGrayscaled != null) {
                    // Throw old image
                    if (mBinary != null) {
                        mBinary.recycle();
                        mBinary = null;
                    }
                    if (mBinarizingTask != null && mBinarizingTask.getStatus() == AsyncTask.Status.RUNNING) {
                        mBinarizingTask.cancel(true);
                        mBinarizingTask = null;
                    }
                    mBinarizingTask = new BinarizeImageTask();
                    mBinarizingTask.execute();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    public class BinarizeImageTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            mBinary = FloorplanVectorizer.toBinary(mGrayscaled, mThreshold, this);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            imgGrayscale.setImageBitmap(mBinary);
        }
    }

}
