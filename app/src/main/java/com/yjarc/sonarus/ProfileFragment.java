package com.yjarc.sonarus;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.yjarc.sonarus.UIHelper.CustomViewPager;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.yjarc.sonarus.AppConstants.USER;


public class ProfileFragment extends Fragment{

    private String userID;
    Button followBtn, addStatusBtn;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        final View v = inflater.inflate(R.layout.fragment_profile, container, false);

        Bundle args = getArguments();
        if (args != null) {
            userID = args.getString("userID");
            Log.e("Profile", "loading user-" + userID);

            followBtn = (Button) v.findViewById(R.id.profile_follow_btn);
            v.findViewById(R.id.profile_write_btn).setVisibility(View.GONE);

            DatabaseReference myFollowing = FirebaseDatabase.getInstance().getReference().child("Userbase").child(USER.id).child("5Following");
            myFollowing.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(dataSnapshot.hasChild(userID)){
                        followBtn.setBackgroundResource(R.drawable.ic_unfollow);
                        followBtn.setOnClickListener(unfollow);
                        return;
                    }
                    followBtn.setOnClickListener(follow);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {}
            });

            loadUser(userID, v);

        } else {
            userID = USER.id;
            Log.e("Profile", "loading self");
            v.findViewById(R.id.profile_follow_btn).setVisibility(View.GONE);
            addStatusBtn = (Button) v.findViewById(R.id.profile_write_btn);
            addStatusBtn.setOnClickListener(addStatus);
            loadUser(USER.id, v);
        }

        return v;
    }
//==================================================================================================
//      User Methods
//==================================================================================================

    private String img_uri="", username="", bio = "", banner_uri;
    long followers, followings, posts;

    public void loadUser(String userID, final View v){
        DatabaseReference userroot = FirebaseDatabase.getInstance().getReference()
                .child("Userbase")
                .child(userID);
        userroot.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterator i = dataSnapshot.getChildren().iterator();

                username = (String) ((DataSnapshot) i.next()).getValue();   // 1Name
                bio = (String) ((DataSnapshot) i.next()).getValue();        // 2Bio
                img_uri = (String) ((DataSnapshot) i.next()).getValue();    // 3Image
                banner_uri = (String) ((DataSnapshot) i.next()).getValue(); // 4Banner

                followers =  ((DataSnapshot) i.next()).getChildrenCount();  // 4Followings
                followings = ((DataSnapshot) i.next()).getChildrenCount();  // 5Followers
                posts = ((DataSnapshot) i.next()).getChildrenCount();       // 6Statuses

                initHeader(v);
                initTabs(v);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    public static ProfileFragment newInstance(String userID) {
        Bundle args = new Bundle();
        args.putString("userID", userID);

        ProfileFragment fragment = new ProfileFragment();
        fragment.setArguments(args);
        return fragment;
    }


//==================================================================================================
//    Header
//==================================================================================================

    private ImageView headerBanner;
    private CircleImageView headerPic;
    private TextView headerName, headerBio;

    public void initHeader (final View v){
        headerPic = (CircleImageView) v.findViewById(R.id.header_profile_img);
        headerBanner = (ImageView) v.findViewById(R.id.header_profile_banner);
        headerName = (TextView) v.findViewById(R.id.header_profile_name);
        headerBio = (TextView) v.findViewById(R.id.header_profile_bio);

        if (!img_uri.isEmpty())
            Picasso.with(getActivity()).load(img_uri).into(headerPic);
        if(!banner_uri.isEmpty()) {
            Picasso.with(getActivity()).load(banner_uri).into(headerBanner);
            v.findViewById(R.id.header_profile_banner_darkener).setVisibility(View.VISIBLE);
        }
        headerName.setText(username);
        headerBio.setText(bio);
    }

//==================================================================================================
//    Tabs
//==================================================================================================

    private TabLayout profileTabs;
    private CustomViewPager mPager;
    private Fragment[] profileFrags;

    public void initTabs(final View v) {
        profileTabs = (TabLayout) v.findViewById(R.id.profile_tabs);
        mPager = (CustomViewPager) v.findViewById(R.id.profile_pager);

        profileFrags = new Fragment[]{
                ProfileStatusesFragment.newInstance(userID, username, img_uri),
                ProfileFollowersFragment.newInstance(userID),
                ProfileFollowingFragment.newInstance(userID)
        };

        mPager.setAdapter(new FragmentPagerAdapter(getChildFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return profileFrags[position];
            }

            @Override
            public int getCount() {
                return 3;
            }
        });

        profileTabs.setupWithViewPager(mPager);

        profileTabs.getTabAt(0).setCustomView(createTabView(""+posts,"Posts"));
        profileTabs.getTabAt(1).setCustomView(createTabView(""+followers,"Followers"));
        profileTabs.getTabAt(2).setCustomView(createTabView(""+followings, "Following"));
    }


    private View createTabView(String count, String title) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.tab_view2, null);

        TextView count_tv = (TextView) view.findViewById(R.id.tab_count);
        TextView title_tv = (TextView) view.findViewById(R.id.tab_title);
        count_tv.setText(count);
        title_tv.setText(title);

        return view;
    }



//==================================================================================================
//      Create Status
//==================================================================================================

    View.OnClickListener addStatus = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("New Status:");

            final EditText input_field = new EditText(getContext());

            builder.setView(input_field);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final String status = input_field.getText().toString();
                    fanStatus(status);
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

    public void fanStatus(final String status){
        final long timestamp = System.currentTimeMillis();
        // Push post to my statuses for easy retrieval in my profile
        DatabaseReference status_root = FirebaseDatabase.getInstance().getReference()
                .child("Userbase")
                .child(USER.id)
                .child("6Statuses");
        Map<String, Object> map = new HashMap<>();
        String temp_key = status_root.push().getKey();
        status_root.updateChildren(map);

        status_root = status_root.child(temp_key);
        Map<String, Object> map1 = new HashMap<>();
        map1.put("1status", status);
        map1.put("2timestamp", timestamp);
        status_root.updateChildren(map1);

        // Push post to each Follower's Timeline
        DatabaseReference myFollowers = FirebaseDatabase.getInstance().getReference().child("Userbase").child(USER.id).child("4Followers");
        myFollowers.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterator i = dataSnapshot.getChildren().iterator();
                while(i.hasNext()) {
                    String fid = (String) ((DataSnapshot) i.next()).getValue();

                    DatabaseReference fTimeline = FirebaseDatabase.getInstance().getReference().child("Userbase").child(fid).child("7Timeline");
                    Map<String, Object> map2 = new HashMap<>();
                    String temp_key2 = fTimeline.push().getKey();
                    fTimeline.updateChildren(map2);

                    fTimeline = fTimeline.child(temp_key2);

                    Map<String,Object> map3 = new HashMap<>();

                    map3.put("1user",USER.id);
                    map3.put("2name", "Yohanan Arciniega");//((MainActivity) getActivity()).getUserName());
                    map3.put("3image", img_uri);
                    map3.put("4status", status);
                    map3.put("5timestamp", timestamp);

                    fTimeline.updateChildren(map3);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        // Push post to own Timeline
        DatabaseReference mTimeline = FirebaseDatabase.getInstance().getReference().child("Userbase").child(USER.id).child("7Timeline");
        Map<String, Object> map4 = new HashMap<>();
        String temp_key3 = mTimeline.push().getKey();
        mTimeline.updateChildren(map4);
        mTimeline = mTimeline.child(temp_key3);

        Map<String, Object> map5 = new HashMap<>();

        map5.put("1user", USER.id);
        map5.put("2name", "Yohanan Arciniega");//((MainActivity)getActivity()).getUserName());
        map5.put("3image", img_uri);
        map5.put("4status", status);
        map5.put("5timestamp", timestamp);

        TextView tv = (TextView) profileTabs.getTabAt(0).getCustomView().findViewById(R.id.tab_count);
        tv.setText(""+(posts+1));

        mTimeline.updateChildren(map5);
        ProfileStatusesFragment psf = (ProfileStatusesFragment) profileFrags[0];
        psf.loadStatuses();
    }


//==================================================================================================
//  Follow tools
//==================================================================================================

    View.OnClickListener follow = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            DatabaseReference follow_root = FirebaseDatabase.getInstance().getReference()
                    .child("Userbase")
                    .child(USER.id)
                    .child("5Following");
            Map<String, Object> map = new HashMap<>();
            map.put(userID,userID);
            follow_root.updateChildren(map);
            followBtn.setBackgroundResource(R.drawable.ic_unfollow);
            followBtn.setOnClickListener(unfollow);
        }
    };

    View.OnClickListener unfollow = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            DatabaseReference follow_root = FirebaseDatabase.getInstance().getReference()
                    .child("Userbase")
                    .child(USER.id)
                    .child("5Following")
                    .child(userID);
            follow_root.removeValue();
            followBtn.setBackgroundResource(R.drawable.ic_follow);
            followBtn.setOnClickListener(follow);
        }
    };

}


