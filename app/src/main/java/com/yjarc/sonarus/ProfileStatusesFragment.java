package com.yjarc.sonarus;

import android.os.Bundle;
import android.support.annotation.Nullable;
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
import com.yjarc.sonarus.RecylerViewAdapters.StatusObject;
import com.yjarc.sonarus.RecylerViewAdapters.StatusRecyclerViewAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


public class ProfileStatusesFragment extends Fragment {

    String userID, username, image;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_profile_statuses, container, false);

        Bundle args = getArguments();
        if(args != null){
            userID = args.getString("userID");
            username = args.getString("username");
            image = args.getString("image");

            Log.e("Load Posts", userID);

            initStatuses(v);
            loadStatuses();
        }

        return v;
    }

    RecyclerView statuses;
    StatusRecyclerViewAdapter statusAdapter;

    void initStatuses(View v){
        statusAdapter = new StatusRecyclerViewAdapter(getActivity(), new StatusRecyclerViewAdapter.ItemSelectedListener() {
            @Override
            public void onItemSelected(View itemView, StatusObject item) {}

            @Override
            public void onUserSelected(StatusObject item) {}
        });

        statusAdapter.setContext(getActivity());
        LinearLayoutManager m = new LinearLayoutManager(getActivity());
        statuses = (RecyclerView) v.findViewById(R.id.scroll);
        statuses.setLayoutManager(m);
        statuses.setAdapter(statusAdapter);

    }

    void loadStatuses(){
        DatabaseReference statusesRoot = FirebaseDatabase.getInstance().getReference()
                .child("Userbase")
                .child(userID)
                .child("6Statuses");
        statusesRoot.limitToLast(10).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List <StatusObject> statusList = new ArrayList<>();
                Iterator i = dataSnapshot.getChildren().iterator();
                while(i.hasNext()) {

                    DataSnapshot i_snap = (DataSnapshot) i.next();
                    Iterator j = i_snap.getChildren().iterator();
                    String status = (String) ((DataSnapshot) j.next()).getValue();
                    Long timestamp = (Long) ((DataSnapshot) j.next()).getValue();
                    StatusObject so = new StatusObject(userID, username, image, status, timestamp);
                    Log.e("so", username + "\n" + status);
                    statusList.add(so);
                }
                Collections.reverse(statusList);
                statusAdapter.clearData();
                statusAdapter.addData(statusList);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }


    public static ProfileStatusesFragment newInstance(String userID, String username, String image) {

        Bundle args = new Bundle();
        args.putString("userID", userID);
        args.putString("username", username);
        args.putString("image", image);

        ProfileStatusesFragment fragment = new ProfileStatusesFragment();
        fragment.setArguments(args);
        return fragment;
    }

}
