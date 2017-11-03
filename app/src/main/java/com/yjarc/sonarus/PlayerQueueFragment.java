package com.yjarc.sonarus;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.yjarc.sonarus.RecylerViewAdapters.TrackRecyclerViewAdapter;
import com.yjarc.sonarus.SpotifyHelper.SimpleRequestListener;
import com.yjarc.sonarus.SpotifyHelper.SpotifyRequester;


import java.util.List;

import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;

import static com.yjarc.sonarus.AppConstants.CASTROOM_ID;


//TODO show queueRecycler and use regular itemtouchlistener with dragger
public class PlayerQueueFragment extends Fragment implements Search.View{

    private RecyclerView queueRecycler;
    private TrackRecyclerViewAdapter queueAdapter;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View v = inflater.inflate(R.layout.fragment_player_queue, container, false);

        initQueue(v);

        return v;
    }


    private void initQueue(View v){

        queueAdapter = new TrackRecyclerViewAdapter(getActivity(), new TrackRecyclerViewAdapter.ItemSelectedListener() {
            @Override
            public void onItemSelected(View itemView, Track item) {

            }

            @Override
            public void onItemSwipeRight(Track item) {

            }
        });

        queueAdapter.setContext(getActivity());

        LinearLayoutManager m = new LinearLayoutManager(getActivity());
        queueRecycler = (RecyclerView) v.findViewById(R.id.queue_results);
        queueRecycler.setLayoutManager(m);
        queueRecycler.setAdapter(queueAdapter);
        queueRecycler.setHasFixedSize(true);
    }


    DatabaseReference queue_root;
    ValueEventListener queue_listener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            createQueueFromCast(dataSnapshot);
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };
    public void getQueueFromBroadCast(){
        /* if you own the castroom or you are not in a castroom... don't update from DB */
        if(CASTROOM_ID.equals("") || CASTROOM_ID.equals(((MainActivity) getActivity()).getUserID())){
            Log.i("Queue", "castroom not set");
            if(queue_root != null)
                queue_root.removeEventListener(queue_listener);
        } else {
            Log.e("Queue", "Getting queue from broadcast");
            queue_root = FirebaseDatabase.getInstance().getReference()
                    .child("Roombase")
                    .child(CASTROOM_ID)
                    .child("7Queue");
            queue_root.addValueEventListener(queue_listener);
        }

    }

    public void endQueueFromBroadCast(){
        if(queue_root != null)
            queue_root.removeEventListener(queue_listener);
    }


    SpotifyRequester spotifyRequester = new SpotifyRequester();
    SpotifyRequester.RequestListener requestQueueListener = new SimpleRequestListener(){
        @Override
        public void getTracks(Tracks tracks){
            reset();
            addData(tracks.tracks);
        }
    };
    public void createQueueFromCast(DataSnapshot dataSnapshot){
        String trackIds = (String) dataSnapshot.getValue();
        if(trackIds== null)
            return;
        trackIds = trackIds.substring(0,trackIds.length()-1);
        Log.e("Gathering Queue", ""+trackIds);
        spotifyRequester.getTracks(trackIds, requestQueueListener);
    }

    @Override
    public void reset(){
        queueAdapter.clearData();
    }

    @Override
    public void addData(List<Track> items){
        queueAdapter.addData(items);
    }


}

