package com.yjarc.sonarus.RecylerViewAdapters;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

import com.loopeer.itemtouchhelperextension.ItemTouchHelperExtension;


//// TODO: 5/29/2017 rename to show use in searchTracks results and queueing
public class ItemTouchHelperCallback extends ItemTouchHelperExtension.Callback {

    Activity mAct;
    public ItemTouchHelperCallback(Activity act){
        mAct = act;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        return makeMovementFlags(0, ItemTouchHelper.RIGHT);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }


    boolean addTrack = false;
    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (dY != 0 && dX == 0) super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        TrackRecyclerViewAdapter.ViewHolder holder = (TrackRecyclerViewAdapter.ViewHolder) viewHolder;

        //// TODO: 5/25/2017 make general contraints on screen width
        if(dX <= 270){
            holder.mViewContent.setTranslationX(dX);
            holder.queue_icn.setTranslationX(dX/2 - holder.queue_icn.getWidth()/2);
            if (isCurrentlyActive) {
                holder.mActionContainer.setBackgroundColor(Color.rgb(0x2e, 0x36, 0x4f));
                addTrack = false;
            }
            else if(dX == 0){
                if (addTrack == true) {
                    addTrack = false;
                    holder.onOverswipeRight();
                }
            }
        }
        else{
            addTrack = true;
            holder.mViewContent.setTranslationX((dX * .25F + 202.5F));
            holder.queue_icn.setTranslationX((dX * .25F + 202.5F)/2 - holder.queue_icn.getWidth()/2);
            holder.mActionContainer.setBackgroundColor(Color.rgb(0x66,0xFF,0xFC));
        }

    }
}
