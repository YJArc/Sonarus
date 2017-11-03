package com.yjarc.sonarus.RecylerViewAdapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;


import com.yjarc.sonarus.R;

import java.util.ArrayList;
import java.util.List;


public class RecentSearchesRecyclerViewAdapter extends RecyclerView.Adapter<RecentSearchesRecyclerViewAdapter.ViewHolder>{
    private Context tContext;

    private final List<String> tItems = new ArrayList<>();

    private final ItemSelectedListener tListener;

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        public final TextView title;

        public ViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.search_item_title);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            tListener.onItemSelected(v, tItems.get(getAdapterPosition()));
        }

        public void onOverswipeLeft(){
            tListener.onItemSwipeLeft(tItems.get(getAdapterPosition()));
        }

//        @Override
//        public float getActionWidth(){
//            return 0;
//        }

    }

    public interface ItemSelectedListener {
        void onItemSelected(View itemView, String item);
        void onItemSwipeLeft(String item);
    }

    public RecentSearchesRecyclerViewAdapter(Context context, ItemSelectedListener listener) {
        tContext = context;
        tListener = listener;
    }

    public void setContext(Context context){
        tContext = context;
    }

    public void clearData() {
        tItems.clear();
        notifyDataSetChanged();
    }

    public boolean isEmpty(){
        return tItems.isEmpty();
    }

    public void addData(String item) {
        tItems.add(item);
        notifyDataSetChanged();
    }

    public void addData(List<String> items) {
        tItems.addAll(items);
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position){
        holder.title.setText(tItems.get(position));
    }

    @Override
    public int getItemCount() {
        return tItems.size();
    }
}
