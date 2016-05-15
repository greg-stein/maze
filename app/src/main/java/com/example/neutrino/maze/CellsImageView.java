package com.example.neutrino.maze;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by neutrino on 5/2/2016.
 */
public class CellsImageView extends ImageView {
//    private final int CELL_CORNER_RADIUS = 3;
    // Cells map
    private Cell[][] mCells = null;
    // Cell to highlight in the sigmap
    private Cell mHighlightedCell = null;
    // Size of Cells map
    private int mCellsX;
    private int mCellsY;
    // Paint for cells inited in ctor
    private Paint mCellPaint;
    private Paint mHlCellPaint;
    // Where the cells start in relation to map origin
    private int mCellsOffsetX = 0;
    private int mCellsOffsetY = 0;
    // How many pixels occupies one meter
    private int mMapScale = 10;
    // Cell size 5x5 meters. The whole floor plan is divided into cells
    private int mCellSize = 10; //meters(?)


    public CellsImageView(Context context) {this(context, null, 0);}

    public CellsImageView(Context context, AttributeSet attrs) {this(context, attrs, 0);}
    public CellsImageView(Context context, AttributeSet attrs, int defStyleRes) {
        super(context, attrs, defStyleRes);
        setWillNotDraw(false);
        mCellPaint = new Paint();
        mCellPaint.setColor(Color.GREEN);
        mCellPaint.setStrokeWidth(2);
        mCellPaint.setStyle(Paint.Style.STROKE);
        mHlCellPaint = new Paint();
        mHlCellPaint.setColor(Color.RED);
        mHlCellPaint.setStrokeWidth(5);
        mHlCellPaint.setStyle(Paint.Style.STROKE);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (null == mCells) return;
        if (mCells.length == 0) return;
        Paint paint;

        for (int x = 0; x < getWidthInCells(); x++) {
            for (int y = 0; y < getHeightInCells(); y++) {
                Cell cell = mCells[x][y];
                if (null != cell && cell.hasData()) {
                    if (cell == getHighlightedCell()) {
                        paint = mHlCellPaint;
                    } else {
                        paint = mCellPaint;
                    }

                    canvas.drawRect(toPxMetric(x) + mCellsOffsetX, toPxMetric(y) + mCellsOffsetY,
                            toPxMetric(x + 1) - 3 + mCellsOffsetX, toPxMetric(y + 1) - 3 + mCellsOffsetY, paint);

                }
            }
        }

    }

    public int getWidthInCells() {
        return toCellMetric(this.getWidth());
    }

    public int getHeightInCells() {
        return toCellMetric(this.getHeight());
    }

    public int toCellMetric(int px) {
        return (px/mMapScale)/mCellSize;
    }

    public int toPxMetric(int cellLocation) {
        return cellLocation*mMapScale*mCellSize;
    }

    public Cell[][] getCells() {
        return mCells;
    }

    public void setCells(Cell[][] mCells) {
        this.mCells = mCells;
    }

    public int getCellsX() {
        return mCellsX;
    }

    public void setCellsX(int mCellsX) {
        this.mCellsX = mCellsX;
    }

    public int getCellsY() {
        return mCellsY;
    }

    public void setCellsY(int mCellsY) {
        this.mCellsY = mCellsY;
    }

    public int getCellsOffsetX() {
        return mCellsOffsetX;
    }

    public void setCellsOffsetX(int mCellsOffsetX) {
        this.mCellsOffsetX = mCellsOffsetX;
    }

    public int getCellsOffsetY() {
        return mCellsOffsetY;
    }

    public void setCellsOffsetY(int mCellsOffsetY) {
        this.mCellsOffsetY = mCellsOffsetY;
    }

    public int getMapScale() {
        return mMapScale;
    }

    public void setMapScale(int mMapScale) {
        this.mMapScale = mMapScale;
    }

    public int getCellSize() {
        return mCellSize;
    }

    public void setCellSize(int mCellSize) {
        this.mCellSize = mCellSize;
    }

    public Cell getHighlightedCell() {
        return mHighlightedCell;
    }

    public void setHighlightedCell(Cell mHighlightedCell) {
        this.mHighlightedCell = mHighlightedCell;
    }
}
