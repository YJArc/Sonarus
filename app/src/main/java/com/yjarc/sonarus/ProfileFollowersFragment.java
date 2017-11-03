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

import com.yjarc.sonarus.RecylerViewAdapters.UserObject;
import com.yjarc.sonarus.RecylerViewAdapters.UserRecyclerViewAdapter;
import com.yjarc.sonarus.UIHelper.HostFragment;

import java.util.Iterator;


public class ProfileFollowersFragment extends Fragment {

    String userID;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_profile_users, container, false);

        Bundle args = getArguments();
        if(args != null){
            userID = args.getString("userID");
            Log.e("Load Followers", userID);
            initFollowers(v);
            loadFollowers();
        }

        return v;
    }

    RecyclerView followers;
    UserRecyclerViewAdapter userAdapter;

    void initFollowers(View v){
        userAdapter = new UserRecyclerViewAdapter(getActivity(), new UserRecyclerViewAdapter.ItemSelectedListener() {
            @Override
            public void onItemSelected(View itemView, UserObject item) {
                ((MainActivity)getActivity()).replaceFragment(HostFragment.newInstance(ProfileFragment.newInstance(item.uID)));
            }
        });

        userAdapter.setContext(getActivity());
        LinearLayoutManager m = new LinearLayoutManager(getActivity());
        followers = (RecyclerView) v.findViewById(R.id.scroll);
        followers.setLayoutManager(m);
        followers.setAdapter(userAdapter);

    }

    void loadFollowers(){
        userAdapter.clearData();

        final DatabaseReference followersRoot = FirebaseDatabase.getInstance().getReference()
                .child("Userbase")
                .child(userID)
                .child("4Followers");
        followersRoot.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                Iterator i = dataSnapshot.getChildren().iterator();
                while(i.hasNext()) {
                    final String uid = (String) ((DataSnapshot) i.next()).getValue();

                    DatabaseReference userRoot = FirebaseDatabase.getInstance().getReference()
                            .child("Userbase")
                            .child(uid);
                    userRoot.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot2) {
                            Iterator j = dataSnapshot2.getChildren().iterator();
                            String name = (String) ((DataSnapshot) j.next()).getValue();
                            j.next();
                            String image = (String) ((DataSnapshot) j.next()).getValue();

                            UserObject uo = new UserObject(image, name, uid);
                            userAdapter.addData(uo);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {}
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }


    public static ProfileFollowersFragment newInstance(String userID) {
        Bundle args = new Bundle();
        args.putString("userID", userID);

        ProfileFollowersFragment fragment = new ProfileFollowersFragment();
        fragment.setArguments(args);
        return fragment;
    }

}
