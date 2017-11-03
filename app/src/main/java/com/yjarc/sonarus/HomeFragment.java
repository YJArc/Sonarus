package com.yjarc.sonarus;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.yjarc.sonarus.RecylerViewAdapters.StatusObject;
import com.yjarc.sonarus.RecylerViewAdapters.StatusRecyclerViewAdapter;
import com.yjarc.sonarus.UIHelper.HostFragment;

import java.util.Iterator;

import static com.yjarc.sonarus.AppConstants.USER;


public class HomeFragment extends Fragment {

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View v = inflater.inflate(R.layout.fragment_home, container, false);
        initTimeline(v);
        if(USER == null)
            return v;
        loadTimeline();
        return v;
    }

    RecyclerView timeline;
    StatusRecyclerViewAdapter tAdapter;

    public void initTimeline(View v){

        tAdapter = new StatusRecyclerViewAdapter(getActivity(), new StatusRecyclerViewAdapter.ItemSelectedListener() {
            @Override
            public void onItemSelected(View itemView, StatusObject item) {
            }

            @Override
            public void onUserSelected(StatusObject item) {
                ((MainActivity)getActivity()).replaceFragment(HostFragment.newInstance(ProfileFragment.newInstance(item.uID)));
            }
        });
        LinearLayoutManager m = new LinearLayoutManager(getActivity());
        timeline = (RecyclerView) v.findViewById(R.id.scroll);
        timeline.setLayoutManager(m);
        timeline.setAdapter(tAdapter);
    }

    public void loadTimeline(){
        DatabaseReference timeline_root = FirebaseDatabase.getInstance().getReference()
                .child("Userbase")
                .child(USER.id)
                .child("7Timeline");
        timeline_root.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterator i = dataSnapshot.getChildren().iterator();
                while (i.hasNext()) {
                    DataSnapshot i_snap = (DataSnapshot) i.next();
                    Iterator j = i_snap.getChildren().iterator();
                    String uID = (String) ((DataSnapshot) j.next()).getValue();
                    String username = (String) ((DataSnapshot)j.next()).getValue();
                    String image = (String) ((DataSnapshot)j.next()).getValue();
                    String status = (String) ((DataSnapshot)j.next()).getValue();
                    Long timestamp = (Long)((DataSnapshot)j.next()).getValue();
                    StatusObject so = new StatusObject(uID,username,image,status, timestamp);
                    tAdapter.addData(so);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

}
