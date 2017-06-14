package com.example.neutrino.maze;

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

    public View getView(int position, View convertView, ViewGroup parent ){
        View itemView = mInflater.inflate(mGroupid, parent, false);
        ImageView imageView = (ImageView)itemView.findViewById(R.id.im_item_icon);
        imageView.setImageResource(mData.get(position).second);
        TextView textView = (TextView)itemView.findViewById(R.id.lbl_item_text);
        textView.setText(mData.get(position).first);
        return itemView;
    }

    public View getDropDownView(int position, View convertView, ViewGroup parent){
        return getView(position, convertView, parent);
    }

//    public ImageSpinnerAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
//        super(context, data, resource, from, to);
//        this.data = data;
//        mInflater = LayoutInflater.from(context);
//    }
//
//    @SuppressWarnings("unchecked")
//    public View getView(int position, View convertView, ViewGroup parent) {
//        if (convertView == null) {
//            convertView = mInflater.inflate(R.layout.spinner_view,
//                    null);
//        }
//        //  HashMap<String, Object> data = (HashMap<String, Object>) getItem(position);
//        ((TextView) convertView.findViewById(R.id.imageNameSpinner))
//                .setText((String) dataRecieved.get(position).get("Name"));
//        ((ImageView) convertView.findViewById(R.id.imageIconSpinner))
//                .setBackgroundResource(dataRecieved.get(position).get("Icon")));
//        return convertView;
//    }
}
