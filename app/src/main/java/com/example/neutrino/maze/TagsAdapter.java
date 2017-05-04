package com.example.neutrino.maze;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.neutrino.maze.floorplan.FloorPlan;
import com.example.neutrino.maze.floorplan.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Greg Stein on 5/2/2017.
 */
public class TagsAdapter extends RecyclerView.Adapter<TagsAdapter.TagsHolder> {

    private List<Tag> listData = new ArrayList<>();
    private LayoutInflater inflater;

    public TagsAdapter(List<Tag> listItems, Context context) {
        this.inflater = LayoutInflater.from(context);
        this.listData.addAll(listItems);
    }

    public void updateListData(List<Tag> newData) {
        listData.clear();
        synchronized (FloorPlan.mTagsListLocker) {
            listData.addAll(newData);
        }
        notifyDataSetChanged();
    }

    @Override
    public TagsHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.tag_item, parent, false);
        return new TagsHolder(view);
    }

    @Override
    public void onBindViewHolder(TagsHolder holder, int position) {
        Tag item = listData.get(position);
        holder.title.setText(item.getLabel());
        holder.icon.setImageResource(R.drawable.ic_add_location_white_24dp);
    }

    @Override
    public int getItemCount() {
        return listData.size();
    }

    class TagsHolder extends RecyclerView.ViewHolder {

        private TextView title;
        private ImageView icon;
        private View container;

        public TagsHolder(View itemView) {
            super(itemView);

            title = (TextView)itemView.findViewById(R.id.lbl_item_text);
            icon = (ImageView)itemView.findViewById(R.id.im_item_icon);
            container = itemView.findViewById(R.id.cont_item_root);
        }
    }

}
