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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class StatusRecyclerViewAdapter extends RecyclerView.Adapter<StatusRecyclerViewAdapter.ViewHolder> {

    private Context rContext;

    private final List<StatusObject> rItems = new ArrayList<>();

    private final ItemSelectedListener rListener;

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        public ImageView image;
        public TextView status, username, timestamp;

        public ViewHolder (final View itemView){
            super(itemView);
            image = (ImageView) itemView.findViewById(R.id.status_user_pic);
            username = (TextView) itemView.findViewById(R.id.status_name);
            status = (TextView) itemView.findViewById(R.id.status_text);
            timestamp = (TextView) itemView.findViewById(R.id.status_timestamp);
            image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    rListener.onUserSelected(rItems.get(getAdapterPosition()));
                }
            });

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick (View v){
            rListener.onItemSelected(v, rItems.get(getAdapterPosition()));
        }

    }


    public interface ItemSelectedListener{
        void onItemSelected(View itemView, StatusObject item);
        void onUserSelected(StatusObject item);
    }

    public StatusRecyclerViewAdapter(Context context, ItemSelectedListener listener){
        rContext = context;
        rListener = listener;
    }

    public void setContext(Context context){rContext = context;}

    public void clearData(){rItems.clear();}

    public void addData(List <StatusObject> items){
        rItems.addAll(items);
        notifyDataSetChanged();
    }

    public void addData(StatusObject item){
        rItems.add(0,item);
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_status, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position){
        StatusObject item = rItems.get(position);

        holder.username.setText(item.username);
        holder.status.setText(item.status);
        String data = new SimpleDateFormat("MM/dd hh:mm a").format(new Date(item.timestamp));
        holder.timestamp.setText(data);

        Picasso.with(rContext).load(item.imgURI).into(holder.image);

    }

    @Override
    public int getItemCount() {
        return rItems.size();
    }
}

