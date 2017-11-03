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


public class UserRecyclerViewAdapter extends RecyclerView.Adapter<UserRecyclerViewAdapter.ViewHolder> {

    private Context rContext;

    private final List<UserObject> rItems = new ArrayList<>();

    private final ItemSelectedListener rListener;

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        public ImageView image;
        public TextView username;

        public ViewHolder (final View itemView){
            super(itemView);
            image = (ImageView) itemView.findViewById(R.id.user_pic);
            username = (TextView) itemView.findViewById(R.id.user_name);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick (View v){
            rListener.onItemSelected(v, rItems.get(getAdapterPosition()));
        }
    }


    public interface ItemSelectedListener{
        void onItemSelected(View itemView, UserObject item);
    }

    public UserRecyclerViewAdapter(Context context, ItemSelectedListener listener){
        rContext = context;
        rListener = listener;
    }

    public void setContext(Context context){rContext = context;}

    public void clearData(){rItems.clear();}

    public void addData(List <UserObject> items){
        rItems.addAll(items);
        notifyDataSetChanged();
    }

    public void addData(UserObject item){
        rItems.add(item);
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position){
        UserObject item = rItems.get(position);

        holder.username.setText(item.username);

        Picasso.with(rContext).load(item.imgURI).into(holder.image);

    }

    @Override
    public int getItemCount() {
        return rItems.size();
    }
}

