package com.example.neutrino.maze;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
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
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Toast;

import com.example.neutrino.maze.vectorization.FloorplanVectorizer;
import com.example.neutrino.maze.vectorization.ImageArray;
import com.example.neutrino.maze.vectorization.LineSegmentsRecognizer;
import com.example.neutrino.maze.vectorization.Thinning;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static android.app.Activity.RESULT_OK;
import static com.example.neutrino.maze.vectorization.HoughTransform.LineSegment;

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
    private Button btnOtsu;
    private Button btnVectorize;
    private ProgressBar pbVectorization;
    private SeekBar sbMinLineLength;
    private Button btnApply;

    protected String mCurrentImagePath;
    private Bitmap mFloorPlanBitmap;
    private Bitmap mGrayscaled;
    private Bitmap mBinary;
    private int mThreshold;
    private BinarizeImageTask mBinarizingTask;

    private ICompleteVectorizationHandler mCompleteVectorizationHandler;
    private SortedSet<LineSegment> mRecognizedLineSegments = null;
    private float mScalingFactor;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.vectorize_dialog, container, false);
        getDialog().setTitle("Simple Dialog");

        btnCamera = (Button) rootView.findViewById(R.id.btn_image_from_cam);
        btnGallery = (Button) rootView.findViewById(R.id.btn_image_from_gallery);
        imgGrayscale = (ImageView) rootView.findViewById(R.id.img_grayscale);
        sbThreshold = (SeekBar) rootView.findViewById(R.id.sb_threshold);
        btnOtsu = (Button) rootView.findViewById(R.id.btn_otsu);
        btnVectorize = (Button) rootView.findViewById(R.id.btn_vectorize);
        pbVectorization = (ProgressBar) rootView.findViewById(R.id.pb_vectorization);
        sbMinLineLength = (SeekBar) rootView.findViewById(R.id.sb_min_line_length);
        btnApply = (Button) rootView.findViewById(R.id.btn_apply);

        setUiListeners();
        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) return;

        if (mFloorPlanBitmap != null) mFloorPlanBitmap.recycle();
        mFloorPlanBitmap = null;

        switch (requestCode) {
            case REQUEST_IMAGE_CAPTURE: {
                mFloorPlanBitmap = loadBitmapFromFile(mCurrentImagePath);
                break;
            }
            case REQUEST_IMAGE_SELECT: {
                Uri selectedImageUri = data.getData();
                mCurrentImagePath = getPath(selectedImageUri);

                if (mCurrentImagePath == null) {
                    mFloorPlanBitmap = loadPicasaImageFromGallery(selectedImageUri);
                } else {
                    mFloorPlanBitmap = loadBitmapFromFile(mCurrentImagePath);
                }
                break;
            }
        }

        int maxWidth = imgGrayscale.getMaxWidth();
        int maxHeight = this.getDialog().getWindow().getDecorView().getHeight()/2;
        Bitmap resized = FloorplanVectorizer.resize(mFloorPlanBitmap, maxWidth, maxHeight);
        mGrayscaled = FloorplanVectorizer.toGrayscale(resized, FloorplanVectorizer.PADDING);
        resized.recycle();

        mThreshold = FloorplanVectorizer.calcOtsuThreshold(mGrayscaled);
        mScalingFactor = (float)mGrayscaled.getWidth() / mFloorPlanBitmap.getWidth();
        sbThreshold.setProgress(mThreshold); // This will trigger asynchronous binarization & setting image to imgGrayscale upon finishing

//        List<IFloorPlanPrimitive> walls = FloorplanVectorizer.vectorize(floorplanBitmap);
//        mFloorPlan.setSketch(walls);
//        uiFloorPlanView.plot(walls, false); // not in init phase
//        uiFloorPlanView.showMap();

    }

    // make sure the Activity implemented it
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity) {
            try {
                this.mCompleteVectorizationHandler = (ICompleteVectorizationHandler) context;
            } catch (final ClassCastException e) {
                throw new ClassCastException(context.toString() + " must implement ICompleteVectorizationHandler");
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinary.recycle();
        mGrayscaled.recycle();
        mFloorPlanBitmap.recycle();
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
                if (mGrayscaled != null) {
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

        btnOtsu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mThreshold = FloorplanVectorizer.calcOtsuThreshold(mGrayscaled);
                sbThreshold.setProgress(mThreshold); // this will trigger binarization
            }
        });

        btnVectorize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // mGrayscaled is wrong image it is resized!!
                new VectorizeTask().execute(mFloorPlanBitmap);
            }
        });

        btnApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCompleteVectorizationHandler != null) {
                    mCompleteVectorizationHandler.onCompleteVectorization(mRecognizedLineSegments);
                }
                getDialog().dismiss();
            }
        });

        sbMinLineLength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                drawLines(mRecognizedLineSegments, progress, mBinary, imgGrayscale, mScalingFactor);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    public interface ICompleteVectorizationHandler {
        void onCompleteVectorization(Iterable<LineSegment> segments);
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

    public class VectorizeTask extends AsyncTask<Bitmap, Integer, List<LineSegment>> {

        @Override
        protected List<LineSegment> doInBackground(Bitmap... bitmaps) {
            Bitmap source = bitmaps[0];

            publishProgress(0);
            Bitmap grayed = FloorplanVectorizer.toGrayscale(source, FloorplanVectorizer.PADDING);
            ImageArray imageArray = new ImageArray(grayed);
            grayed.recycle();
            publishProgress(10);
            FloorplanVectorizer.binarize(imageArray, mThreshold);
            publishProgress(15);
            imageArray.findBlackPixels(); // this updates internal multiarray with black pixels
            publishProgress(20);
            Thinning.doZhangSuenThinning(imageArray);
            publishProgress(80);
            LineSegmentsRecognizer kht = new LineSegmentsRecognizer(imageArray);
            List<LineSegment> lineSegments = kht.findStraightSegments();
            publishProgress(100);

            return lineSegments;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (values.length > 0)
                pbVectorization.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(List<LineSegment> lineSegments) {
            // Draw lines on imgView
//            BitmapDrawable bitmapDrawable = ((BitmapDrawable) imgGrayscale.getDrawable());

            VectorizeDialog.this.mRecognizedLineSegments = new TreeSet<>(new LineLengthComparator());
            VectorizeDialog.this.mRecognizedLineSegments.addAll(lineSegments);
            sbMinLineLength.setMax((int) VectorizeDialog.this.mRecognizedLineSegments.last().getLength());

            drawLines(VectorizeDialog.this.mRecognizedLineSegments, 0, mBinary, imgGrayscale, mScalingFactor);
            super.onPostExecute(lineSegments);
        }
    }

    private static void drawLines(SortedSet<LineSegment> lineSegments, float minLength, Bitmap bgBitmap, ImageView imageView, float scalingFactor) {
        Bitmap bitmap = bgBitmap.copy(bgBitmap.getConfig(), true);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(4);
        imageView.setImageBitmap(bitmap);

        // Scaling factor (to display on resized image shown in imageView)
        float s = scalingFactor;
        SortedSet<LineSegment> linesToDraw = lineSegments.tailSet(getSpecificLengthSegment(minLength));
        for (LineSegment line : linesToDraw) {
            canvas.drawLine(s*line.start.x, s*line.start.y, s*line.end.x, s*line.end.y, paint);
        }
    }

    public static class SpecificLengthLineSegment extends LineSegment {

        private float mDesiredLength;

        public SpecificLengthLineSegment() {
            super(new Point(), new Point(), null);
        }

        @Override
        public float getLength() {
            return mDesiredLength;
        }

        public void setDesiredLength(float desiredLength) {
            this.mDesiredLength = desiredLength;
        }
    }

    private static final SpecificLengthLineSegment specificLengthSegment = new SpecificLengthLineSegment();

    public static LineSegment getSpecificLengthSegment(float desiredLength) {
        specificLengthSegment.setDesiredLength(desiredLength);
        return specificLengthSegment;
    }

    public static class LineLengthComparator implements Comparator<LineSegment> {

        @Override
        public int compare(LineSegment l1, LineSegment l2) {
            return Float.compare(l1.getLength(), l2.getLength());
        }
    }
}
