package com.example.neutrino.maze.ui;

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.neutrino.maze.AppSettings;
import com.example.neutrino.maze.R;
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

    private ItemClickListener mItemClickListener;

    public interface ItemClickListener {

        void onItemClick(Tag tag);
    }
    public void setItemClickListener(final ItemClickListener listener) {
        this.mItemClickListener = listener;
    }

    public TagsAdapter(List<Tag> listItems, Context context) {
        this.inflater = LayoutInflater.from(context);
        this.listData.addAll(listItems);
    }

    public TagsAdapter(Context context) {
        this.inflater = LayoutInflater.from(context);
    }

    public void updateListData(List<Tag> newData) {
        listData.clear();
        listData.addAll(newData);
        notifyDataSetChanged();
    }

    @Override
    public TagsHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.tag_item, parent, false);
        return new TagsHolder(view);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onBindViewHolder(TagsHolder holder, int position) {
        Tag item = listData.get(position);
        holder.title.setText(item.getLabel());
        holder.icon.setImageResource(R.drawable.ic_add_location_white_24dp);
        holder.container.setBackgroundColor(AppSettings.accentColor);
    }

    @Override
    public int getItemCount() {
        return listData.size();
    }

    class TagsHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView title;
        private ImageView icon;
        private View container;

        public TagsHolder(View itemView) {
            super(itemView);

            title = (TextView) itemView.findViewById(R.id.lbl_item_text);
            title.setOnClickListener(this);
            icon = (ImageView) itemView.findViewById(R.id.im_item_icon);
            icon.setOnClickListener(this);
            container = itemView.findViewById(R.id.cont_item_root);
            container.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            // To test what exactly was clicked:
//            if (view.getId() == R.id.cont_item_root) {
                if (mItemClickListener != null) {
                    mItemClickListener.onItemClick(listData.get(getAdapterPosition()));
                }
//            }
        }
    }

}
