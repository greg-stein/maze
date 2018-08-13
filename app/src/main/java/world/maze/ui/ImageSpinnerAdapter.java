package com.example.neutrino.maze.ui;

import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.example.neutrino.maze.R;

import java.util.List;
import java.util.Map;

/**
 * Created by Greg Stein on 6/13/2017.
 */

public class ImageSpinnerAdapter extends ArrayAdapter<Pair<String, Integer>> {
    private LayoutInflater mInflater;
    private int mGroupid;
    private List<Pair<String, Integer>> mData;

    public ImageSpinnerAdapter(Context context, int groupid, int id, List<Pair<String, Integer>> objects) {
        super(context, id, objects);
        mData = objects;
        mGroupid = groupid;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent ){
        View itemView = mInflater.inflate(mGroupid, parent, false);
        ImageView imageView = (ImageView)itemView.findViewById(R.id.im_item_icon);
        imageView.setImageResource(mData.get(position).second);
        TextView textView = (TextView)itemView.findViewById(R.id.lbl_item_text);
        textView.setText(mData.get(position).first);
        return itemView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent){
        return getView(position, convertView, parent);
    }

    @Override
    public int getCount() {
        return mData.size() - 1;
    }
}
