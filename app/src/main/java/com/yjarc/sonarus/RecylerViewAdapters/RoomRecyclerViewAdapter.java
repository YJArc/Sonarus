package com.yjarc.sonarus.RecylerViewAdapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.yjarc.sonarus.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;


public class RoomRecyclerViewAdapter extends RecyclerView.Adapter<RoomRecyclerViewAdapter.ViewHolder> {

    private Context rContext;

    private final List<RoomObject> rItems = new ArrayList<>();
    private int selectedPos = -1;

    private final ItemSelectedListener rListener;

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        public final TextView creator;
        public final TextView title;
        public final TextView description;
        public final ImageView image;
        public final ImageView image2;

        public ViewHolder (final View itemView){
            super(itemView);
            creator = (TextView) itemView.findViewById(R.id.room_creator);
            title = (TextView) itemView.findViewById(R.id.room_title);
            description = (TextView) itemView.findViewById(R.id.room_desc);
            image = (ImageView) itemView.findViewById(R.id.room_img);
            image2 = (ImageView) itemView.findViewById(R.id.room_creator_pic);
            image2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    rListener.onUserSelected(rItems.get(getAdapterPosition()));
                }
            });

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick (View v){
            int newSelectedPos = getLayoutPosition();
            if(selectedPos != newSelectedPos){
                title.setSelected(true);
                notifyItemChanged(selectedPos);
                selectedPos = newSelectedPos;
            }
            rListener.onItemSelected(v, rItems.get(getAdapterPosition()));
        }

    }


    public interface ItemSelectedListener{
        void onItemSelected(View itemView, RoomObject item);
        void onUserSelected(RoomObject item);
    }

    public RoomRecyclerViewAdapter(Context context, ItemSelectedListener listener){
        rContext = context;
        rListener = listener;
    }

    public void setContext(Context context){rContext = context;}

    public void clearData(){rItems.clear();}

    public void addData(List <RoomObject> items){
        rItems.addAll(items);
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_room, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position){
        RoomObject item = rItems.get(position);

        holder.title.setText(item.roomTitle);
        holder.description.setText(item.roomDesc);
        holder.creator.setText(item.creatorName);

        if(position == selectedPos){
            holder.title.setSelected(true);
        } else {
            holder.title.setSelected(false);
        }


        Picasso.with(rContext).load(item.banner).into(holder.image);
        Picasso.with(rContext).load(item.creatorImg).into(holder.image2);

    }

    @Override
    public int getItemCount() {
        return rItems.size();
    }
}
