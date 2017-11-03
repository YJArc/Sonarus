package com.yjarc.sonarus;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.yjarc.sonarus.RecylerViewAdapters.RoomRecyclerViewAdapter;
import com.yjarc.sonarus.RecylerViewAdapters.RoomObject;
import com.yjarc.sonarus.UIHelper.HostFragment;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.yjarc.sonarus.AppConstants.CASTROOM_ID;
import static com.yjarc.sonarus.AppConstants.CASTROOM_TITLE;
import static com.yjarc.sonarus.AppConstants.USER;


public class ExploreFragment extends Fragment {

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_explore, container, false);

        initRoomListAdapter(v);
        initRooms(v);
        initHeader(v);

        return v;
    }

//==================================================================================================
//
//==================================================================================================

    private Button room_btn;
    private List <RoomObject> roomList = new ArrayList<>();

    private String room;

    private void initRooms(View v){
        room_btn = (Button) v.findViewById(R.id.btn_session);
        room_btn.setOnClickListener(addRoom);
        roomList.clear();

        DatabaseReference root = FirebaseDatabase.getInstance().getReference().child("Roombase");

        /* Grab our room database upon entering Explore tab. To refresh, change tab and retener explore tab*/
        // TODO: add scroll down to refresh
        root.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                Iterator i = dataSnapshot.getChildren().iterator();

                while(i.hasNext()){
                    DataSnapshot i_snap = (DataSnapshot)i.next();

                    String creatorID = i_snap.getKey();                                 // uID

                    Iterator j = i_snap.getChildren().iterator();
                    String roomTitle = (String) ((DataSnapshot)j.next()).getValue();    // 1RoomName
                    String creator = (String) ((DataSnapshot)j.next()).getValue();      // 2Creator
                    String desc = (String) ((DataSnapshot)j.next()).getValue();         // 3Desc
                    String banner = (String) ((DataSnapshot)j.next()).getValue();       // 4banner
                    String img = (String) ((DataSnapshot)j.next()).getValue();          // 4img
                                                                                        // skip 5Messages 6Songs

                    RoomObject ro = new RoomObject(creator,creatorID, img, banner, roomTitle,desc);
                    roomList.add(ro);
                }

                exploreRoomsAdapter.clearData();
                exploreRoomsAdapter.addData(roomList);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private View.OnClickListener addRoom = new View.OnClickListener(){
        @Override
        public void onClick(View v){
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Enter Room Name:");

            final EditText input_field = new EditText(getContext());

            builder.setView(input_field);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    DatabaseReference root = FirebaseDatabase.getInstance().getReference().child("Roombase");
                    room = input_field.getText().toString();
                    Map<String, Object> map = new HashMap<>();
                    Map<String, Object> map2 = new HashMap<>();

                    map.put(((MainActivity) getActivity()).getUserID(), "");
                    root.updateChildren(map);

                    //todo -1
                    root = root.child(USER.id);

                    map2.put("1Room Name", room);
                    map2.put("2Creator", ((MainActivity) getActivity()).getUserName());
                    map2.put("3Desc","Calvin Migos Mash-up");
                    map2.put("4Img", "https://media.licdn.com/mpr/mpr/shrinknp_400_400/AAEAAQAAAAAAAAnRAAAAJGEzNDhlMjU3LTgwODUtNDkxMy05ODU4LTFjMWU3MzFmYWVjNA.jpg");
                    map2.put("5Messages", "");
                    map2.put("6Songs","");
                    map2.put("7Queue", "");
                    root.updateChildren(map2);

                    CASTROOM_ID = USER.id;
                    CASTROOM_TITLE = room;

                    room_btn.setOnClickListener(endRoom);
                    room_btn.setText("END SESSION");
                    room_btn.setBackgroundResource(R.color.TertiaryAccent);
                }
            });

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    return;
                }
            });
            builder.show();
        }
    };

    public  View.OnClickListener endRoom = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            DatabaseReference root = FirebaseDatabase.getInstance().getReference()
                    .child("Roombase")
                    .child(USER.id);
            root.removeValue();

            room_btn.setOnClickListener(addRoom);
            room_btn.setText("START A SESSION");
            room_btn.setBackgroundResource(R.color.PrimaryAccent);
        }
    };

    public View.OnClickListener exitRoom  = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ((MainActivity)getActivity()).endCast();

            room_btn.setOnClickListener(addRoom);
            room_btn.setText("START A SESSION");
            room_btn.setBackgroundResource(R.color.PrimaryAccent);
        }
    };

//==================================================================================================
//    ROOM LIST UI
//==================================================================================================

    private RoomRecyclerViewAdapter exploreRoomsAdapter;
    private RecyclerView results;
    boolean doOnce = false;

    private void initRoomListAdapter(View v){
        if(!doOnce){
            exploreRoomsAdapter = new RoomRecyclerViewAdapter(getActivity(), new RoomRecyclerViewAdapter.ItemSelectedListener() {
                @Override
                public void onItemSelected(View itemView, RoomObject item) {
                    if(item.creatorID.equals(USER.id)){
                        /* Do not enter rooms that belong to yourself */
                        Toast.makeText(getActivity(),"You own this room.", Toast.LENGTH_SHORT).show();
                        return;
                    } else if (CASTROOM_ID.equals(item.creatorID)){
                        Toast.makeText(getActivity(),"You are in this room.", Toast.LENGTH_SHORT).show();
                        return;
                    } else {
                        CASTROOM_ID = item.creatorID;
                        CASTROOM_TITLE = item.roomTitle;
                        Toast.makeText(getActivity(), "You entered: " + item.roomTitle, Toast.LENGTH_SHORT).show();
                        ((MainActivity) getActivity()).setCast();

                        room_btn.setOnClickListener(exitRoom);
                        room_btn.setText("EXIT SESSION:" + item.roomTitle);
                        room_btn.setBackgroundResource(R.color.SecondaryAccent);
                    }
                }
                @Override
                public void onUserSelected(RoomObject item){
                    ((MainActivity)getActivity()).replaceFragment(HostFragment.newInstance(ProfileFragment.newInstance(item.creatorID)));
                }
            });
            doOnce = true;
        }

        exploreRoomsAdapter.setContext(getActivity());
        // // TODO: 5/13/2017 add onscrolllistener to load more results instead of all at once
        LinearLayoutManager m = new LinearLayoutManager(getActivity());
        results = (RecyclerView) v.findViewById(R.id.room_results);
        results.setLayoutManager(m);
        results.setAdapter(exploreRoomsAdapter);

    }


//==================================================================================================
//    HEADER SCROLL UI
//==================================================================================================

    /**
     *  The header is an aesthetic method to encourage using Sonarus Stream. The header hides
     *  when the user scrolls down to see other content
     */
    private View exploreHeader;
    private Button sessionBtn;


    private void initHeader(View v){
        exploreHeader = v.findViewById(R.id.header_explore);
        sessionBtn = (Button) v.findViewById(R.id.btn_session);

    }

}
