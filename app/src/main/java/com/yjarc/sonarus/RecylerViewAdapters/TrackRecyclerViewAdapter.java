package com.yjarc.sonarus.RecylerViewAdapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import com.yjarc.sonarus.R;
import com.loopeer.itemtouchhelperextension.Extension;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.models.ArtistSimple;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;

public class TrackRecyclerViewAdapter extends RecyclerView.Adapter<TrackRecyclerViewAdapter.ViewHolder> {

    private Context tContext;

    private final List<Track> tItems = new ArrayList<>();
    private int selectedPos = -1;

    private final ItemSelectedListener tListener;

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, Extension {

        public final TextView title;
        public final TextView subtitle;
        public final ImageView image;
        public final ImageView queue_icn;
        public final View mViewContent;
        public final View mActionContainer;

        public ViewHolder(View itemView) {
            super(itemView);
            mViewContent = itemView.findViewById(R.id.track_item_main_content);
            title = (TextView) itemView.findViewById(R.id.entity_title);
            subtitle = (TextView) itemView.findViewById(R.id.entity_subtitle);
            image = (ImageView) itemView.findViewById(R.id.entity_image);

            mActionContainer = itemView.findViewById(R.id.track_item_right_swipe);
            queue_icn = (ImageView) itemView.findViewById(R.id.add_queue_icn);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int newSelectedPos = getLayoutPosition();
            if(selectedPos != newSelectedPos){
                title.setSelected(true);
                notifyItemChanged(selectedPos);
                selectedPos = newSelectedPos;
            }

            tListener.onItemSelected(v, tItems.get(getAdapterPosition()));
        }

        public void onOverswipeRight(){
            tListener.onItemSwipeRight(tItems.get(getAdapterPosition()));
        }

        @Override
        public float getActionWidth(){
            return 0;
        }

    }

    public interface ItemSelectedListener {
        void onItemSelected(View itemView, Track item);
        void onItemSwipeRight(Track item);
    }

    public TrackRecyclerViewAdapter(Context context, ItemSelectedListener listener) {
        tContext = context;
        tListener = listener;
    }

    public void setContext(Context context){
        tContext = context;
    }

    public void clearData() {
        tItems.clear();
        selectedPos = -1;
    }

    public boolean isEmpty(){
        return tItems.isEmpty();
    }

    public void addData(Track item) {
        tItems.add(item);
        notifyDataSetChanged();
    }

    public void addData(List<Track> items) {
        tItems.addAll(items);
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_track, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Track item = tItems.get(position);

        /* Set track Title text. */
        holder.title.setText(item.name);

        /* Set Artist text (all artists concatenated with a ", ") */
        List<String> names = new ArrayList<>();
        for (ArtistSimple i : item.artists) {
            names.add(i.name);
        }
//        Joiner joiner = Joiner.on(", ");
        holder.subtitle.setText(names.get(0));

        /* Set Image to album art (2) (2 being the icon-sized art) */
        Image image = item.album.images.get(2);
        if (image != null) {
            Picasso.with(tContext).load(image.url).into(holder.image);
        }

        /* Set selection state. If currently selected the Title will be colored. */
        if(position == selectedPos){
            holder.title.setSelected(true);
        } else {
            holder.title.setSelected(false);
        }


    }

    @Override
    public int getItemCount() {
        return tItems.size();
    }
}
