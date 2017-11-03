package com.yjarc.sonarus;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.yjarc.sonarus.MediaHelper.SpotifyPlayerService;
import com.yjarc.sonarus.UIHelper.BackStackFragment;

import com.yjarc.sonarus.UIHelper.CustomViewPager;
import com.yjarc.sonarus.UIHelper.HostFragment;
import com.yjarc.sonarus.UIHelper.ProgressBarMTE;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.text.SimpleDateFormat;
import java.util.Date;

import java.util.List;

import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;

import static com.yjarc.sonarus.AppConstants.CASTROOM_ID;
import static com.yjarc.sonarus.AppConstants.USER;


public class MainActivity extends FragmentActivity implements SpotifyPlayerService.UICallbacks{

//==================================================================================================
//      Fields
//==================================================================================================

    /* Spotify Auth */
    public static final String EXTRA_TOKEN = "EXTRA_TOKEN";
    private String token;

    /* Player */
    private static final int STATE_PAUSED = 0;
    private static final int STATE_PLAYING = 1;
    private int STATE = 0;

//==================================================================================================
//      Activity Initialization
//==================================================================================================

    public static Intent createIntent(Context context) {
        return new Intent(context, MainActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        token = intent.getStringExtra(EXTRA_TOKEN);

        /* Stop views from resizing on keyboard visible. */
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        /* Tab UI */
        initTabs();
        /* PlayerService and Player UI */
        initPlayer();

    }

//==================================================================================================
//      Tab UI
//==================================================================================================

    /**
     * Initialize the Bottom 'Main Tabs' and Tabs found inside the 'Fullscreen Player'
     */

    private TabLayout mainTabs;
    private CustomViewPager mainPager;
    private Fragment[] mainFrags;

    private TabLayout playerTabs;
    private CustomViewPager playerPager;
    private Fragment[] playerFrags;

    private void initTabs() {

        /* Main Tabs layout. These are located at the bottom of the app */

        mainTabs = (TabLayout) findViewById(R.id.main_tabs);
        mainPager = (CustomViewPager) findViewById(R.id.main_pager);
        mainFrags = new Fragment[] {
                  HostFragment.newInstance(new HomeFragment())
                , HostFragment.newInstance(new SearchFragment())
                , HostFragment.newInstance(new ExploreFragment())
                , HostFragment.newInstance(new LibraryFragment())
                , HostFragment.newInstance(new ProfileFragment())
        };

        mainPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return mainFrags[position];
            }

            @Override
            public int getCount() {
                return 5;
            }
        });
        mainTabs.setupWithViewPager(mainPager);

        /* Main tab icons */
        Drawable ic_home = ContextCompat.getDrawable(this, R.drawable.ic_home);
        Drawable ic_search = ContextCompat.getDrawable(this, R.drawable.ic_search);
        Drawable ic_explore = ContextCompat.getDrawable(this, R.drawable.ic_explore);
        Drawable ic_library = ContextCompat.getDrawable(this, R.drawable.ic_library);
        Drawable ic_profile = ContextCompat.getDrawable(this, R.drawable.ic_profile);

        mainTabs.getTabAt(0).setCustomView(createTabView(ic_home));
        mainTabs.getTabAt(1).setCustomView(createTabView(ic_search));
        mainTabs.getTabAt(2).setCustomView(createTabView(ic_explore));
        mainTabs.getTabAt(3).setCustomView(createTabView(ic_library));
        mainTabs.getTabAt(4).setCustomView(createTabView(ic_profile));



        /* Player Tabs layout. These are located in the Fullscreen Player. */

        playerTabs = (TabLayout) findViewById(R.id.player_tabs);
        playerPager = (CustomViewPager) findViewById(R.id.player_pager);
        playerFrags = new Fragment[]{new PlayerQueueFragment(), new PlayerChatFragment()};

        playerPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return playerFrags[position];
            }

            @Override
            public int getCount() {
                return 2;
            }
        });
        playerTabs.setupWithViewPager(playerPager);

        /* Player tab icons */
        Drawable ic_queue = ContextCompat.getDrawable(getBaseContext(), R.drawable.ic_queue);
        Drawable ic_chat = ContextCompat.getDrawable(getBaseContext(), R.drawable.ic_chat);

        playerTabs.getTabAt(0).setCustomView(createTabView(ic_queue));
        playerTabs.getTabAt(1).setCustomView(createTabView(ic_chat));

        playerTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                PlayerChatFragment pcf = (PlayerChatFragment) playerFrags[1];
                if (tab == playerTabs.getTabAt(1)){
                    pcf.getChat();
                } else {
                    pcf.stopChat();
                    updateUIQueue(spotifyPlayerService.getQueue());
                }

                if(broadcastControlPanel.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED)
                    broadcastControlPanel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                if(broadcastControlPanel.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED)
                    broadcastControlPanel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                else if(broadcastControlPanel.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED)
                    broadcastControlPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            }

        });

    }

    private View createTabView(final Drawable ic) {
        View view = LayoutInflater.from(this).inflate(R.layout.tab_view, null);
        ImageView imageView = (ImageView) view.findViewById(R.id.tabIcon);
        imageView.setImageDrawable(ic);

        return view;
    }


//--------------------------------------------------------------------------------------------------
//  Tab BackStack  Events
//--------------------------------------------------------------------------------------------------

    @Override
    public void onBackPressed() {
        SlidingUpPanelLayout sl2 = (SlidingUpPanelLayout) findViewById(R.id.player_control_slide);
        if (sl2.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
            sl2.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            return;
        }

        if(!BackStackFragment.handleBackPressed(getSupportFragmentManager())){
            super.onBackPressed();
        }
    }

    public void replaceFragment(Fragment fragment){
        HostFragment hostFragment = (HostFragment) mainFrags[mainTabs.getSelectedTabPosition()];
        hostFragment.replaceFragment(fragment, true);
    }


//==================================================================================================
//      PLAYER UI
//==================================================================================================

    /**
     * The app contains 2 modes to control music playback.
     * The top panel shows minimal song information, allowing users to browse tabs.
     * On tap or downward swipe of the panel, the panel fades and fullscreen media controls appear.
     */

    SpotifyPlayerService spotifyPlayerService = null;
    boolean isBound = false;
    private ServiceConnection serviceConnector = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("Service Connector","Connecting");
            SpotifyPlayerService.LocalBinder binder = (SpotifyPlayerService.LocalBinder) service;
            spotifyPlayerService = binder.getService();
            isBound = true;

            spotifyPlayerService.registerCallbacks(MainActivity.this);
            updateUIMetadata(spotifyPlayerService.getCurrentTrack());
            updateUIPlaybackState(spotifyPlayerService.isPlaying());
            updateUIProgressBar((int) spotifyPlayerService.getProgress());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("Service Connector", "Disconnecting");
            spotifyPlayerService = null;
            isBound = false;
        }
    };


    private void initPlayer() {
        /* Start the Spotify Player Service. */
        Intent ir= new Intent(this, SpotifyPlayerService.class);
        bindService(ir, serviceConnector, Context.BIND_AUTO_CREATE);
        /* Send token to service so SpotifyPlayer can instantiate. */
        ir.putExtra(EXTRA_TOKEN, token);
        this.startService(ir);

        /* Player UI button Setup */
        initPlayerUI();
    }

    /** Handles setup of Playback buttons and progressBars for the 'fullscreen player' and the
     *  'panel player'
     */

    SlidingUpPanelLayout playerControlPanel, broadcastControlPanel;

    /* Panel player members */
    private Button btn_panel_play;
    private Button btn_panel_next;
    private ProgressBarMTE panel_progress;

    /* Fullscreen player members */
    private Button btn_player_play;
    private Button btn_player_next;
    private SeekBar player_scrubber;
    private RelativeLayout player_controls;
    private TextView player_duration;
    private TextView player_progress;
    private ImageView player_art_blur_ambient;

    /* On click listeners used in player media buttons */
    View.OnClickListener onPlayButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!CASTROOM_ID.equals("") && !CASTROOM_ID.equals(USER.id))
                return;
            if (STATE == STATE_PAUSED) {
                spotifyPlayerService.requestPlay();
            } else if (STATE == STATE_PLAYING) {
                spotifyPlayerService.requestPause();
            }
        }
    };
    View.OnClickListener onNextButtonClick = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            if (!CASTROOM_ID.equals("") && !CASTROOM_ID.equals(USER.id))
                return;
            spotifyPlayerService.requestSkipNext();
        }
    };

    boolean isControlShowing = false;
//    Animation fade_in = AnimationUtils.loadAnimation(getApplicationContext(),
//            R.anim.fade_in);
//    Animation fade_out = AnimationUtils.loadAnimation(getApplicationContext(),
//            R.anim.fade_out);
    View.OnClickListener onControlsClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(!isControlShowing){
                btn_player_play.setVisibility(View.VISIBLE);
//                btn_player_play.startAnimation(fade_in);
            } else{
//                btn_player_play.startAnimation(fade_out);
                btn_player_play.setVisibility(View.INVISIBLE);
            }
            isControlShowing = !isControlShowing;
        }
    };

    public void initPlayerUI(){
        /* If the Player service was running and it carries a track, load it into the activity */
        if (spotifyPlayerService != null) {
            spotifyPlayerService.registerCallbacks(MainActivity.this);
            updateUIMetadata(spotifyPlayerService.getCurrentTrack());
            updateUIPlaybackState(spotifyPlayerService.isPlaying());
            updateUIQueue(spotifyPlayerService.getQueue());
            updateUIProgressBar((int) spotifyPlayerService.getProgress());
        }

        /* Fade in or fade out the top panel on down-swipe or tap. */
        final View playerPanel = findViewById(R.id.panel);

        player_art_blur_ambient = (ImageView) findViewById(R.id.player_art_ambient);

        playerControlPanel = (SlidingUpPanelLayout) findViewById(R.id.player_control_slide);
        playerControlPanel.setDragView(R.id.player_art_ambient);
        playerControlPanel.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                /* Fade panel in and reveal fullscreen media control. */
                playerPanel.setAlpha(1 - slideOffset);
                mainTabs.setTranslationY(mainTabs.getHeight()*slideOffset);
            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                /* Remove focus from text entries and hide keyboard. */
                View current = getCurrentFocus();
                if (current != null)
                    current.clearFocus();
                hideKeyboard(MainActivity.this);

                /* Hide view on full expansion otherwise keep panel button functionality. */
                if (newState == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    playerPanel.setVisibility(View.GONE);
                } else if (previousState == SlidingUpPanelLayout.PanelState.COLLAPSED &&
                        newState == SlidingUpPanelLayout.PanelState.DRAGGING) {
                    playerPanel.setVisibility(View.VISIBLE);
                    playerPager.setVisibility(View.VISIBLE);
                    Log.e("chat","showing?");
                    if(playerTabs.getSelectedTabPosition() == 1){
                        Log.e("chat","getting?");
                        PlayerChatFragment pcf = (PlayerChatFragment) playerFrags[1];
                        pcf.getChat();
                    } else{
                        Log.e("chat","else?");
                        updateUIQueue(spotifyPlayerService.getQueue());
                    }

                } else if (previousState == SlidingUpPanelLayout.PanelState.EXPANDED &&
                        newState == SlidingUpPanelLayout.PanelState.DRAGGING) {
                    playerPanel.setVisibility(View.VISIBLE);
                } else if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                    playerPager.setVisibility(View.GONE);
                    PlayerChatFragment pcf = (PlayerChatFragment) playerFrags[1];
                    pcf.stopChat();
                }
            }

        });

        broadcastControlPanel = (SlidingUpPanelLayout) findViewById(R.id.broadcast_slide);
        broadcastControlPanel.setDragView(R.id.player_tabs);
        broadcastControlPanel.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            int height = 0;
            @Override
            public void onPanelSlide(View panel, float slideOffset) {

                height = (int) (1560-(708)*slideOffset);
                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(1080,height);
                player_art_blur_ambient.setLayoutParams(layoutParams);
                player_art_blur_ambient.requestLayout();
            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {

            }
        });


        /* Player Panel */
        panel_progress = (ProgressBarMTE) findViewById(R.id.panel_progress);

        btn_panel_play = (Button) findViewById(R.id.panel_play_btn);
        btn_panel_play.setOnClickListener(onPlayButtonClick);
        btn_panel_next = (Button) findViewById(R.id.panel_next_btn);
        btn_panel_next.setOnClickListener(onNextButtonClick);


        /* Fullscreen Player */
        player_scrubber = (SeekBar) findViewById(R.id.player_seekbar);
        player_scrubber.getProgressDrawable().setColorFilter(
                new PorterDuffColorFilter(0xFF66FFFC, PorterDuff.Mode.SRC_IN));
        player_duration = (TextView) findViewById(R.id.player_duration);
        player_progress = (TextView) findViewById(R.id.player_progress);

        btn_player_play = (Button) findViewById(R.id.player_play_btn);
        btn_player_play.setOnClickListener(onPlayButtonClick);

        btn_player_next = (Button) findViewById(R.id.player_next_btn);
        btn_player_next.setOnClickListener(onNextButtonClick);

        player_controls = (RelativeLayout) findViewById(R.id.player_show_control_area);
        player_controls.setOnClickListener(onControlsClicked);
    }

//==================================================================================================
//      User Events
//==================================================================================================

    public String getUserName(){
        if(USER != null){
            if(USER.display_name == null)
                return USER.id;
            return USER.display_name;
        }
        return "USER_NOT_SET";
    }

    public String getUserID(){
        if(USER != null){
            return USER.id;
        }
        return "USER_NOT_SET";
    }

//==================================================================================================
//      Player Events
//==================================================================================================

    public void setTrack(Track item) {
        spotifyPlayerService.setTrack(item);
    }

    public void addToQueue(Track item){
        /* Add items to track queue only if
         * you are not broadcasting or you are not in a broadcast*/

        if(CASTROOM_ID.equals(getUserID()) || CASTROOM_ID.isEmpty()) {
            spotifyPlayerService.addToQueue(item);
            Toast.makeText(this, "Added to Queue", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void updateUIPlaybackState(boolean isPlaying){
        if(isPlaying) {
            STATE = STATE_PLAYING;
            btn_panel_play.setBackgroundResource(R.drawable.ic_pause);
            btn_player_play.setBackgroundResource(R.drawable.ic_pause);

        }
        else {
            STATE = STATE_PAUSED;
            btn_panel_play.setBackgroundResource(R.drawable.ic_play_arrow);
            btn_player_play.setBackgroundResource(R.drawable.ic_play_arrow);
        }
    }

    @Override
    public void updateUIMetadata(Track item){
        if(item == null)
            return;

        /* Set Panel max. */
        panel_progress.setMax((int) item.duration_ms);
        /* Set Player max. */
        player_scrubber.setMax((int) item.duration_ms);
        player_duration.setText(new SimpleDateFormat("mm:ss").format(new Date(item.duration_ms)));

        /* Set Panel with track metadata */
        Image image = item.album.images.get(2);
        Picasso.with(this).load(image.url).into((ImageView) findViewById(R.id.panel_art));
        TextView song = (TextView) findViewById(R.id.panel_song);
        song.setText(item.name);
        TextView artist = (TextView) findViewById(R.id.panel_artist);
        artist.setText(item.artists.get(0).name);

        /* Set fullscreen with track traits */
        image = item.album.images.get(0);
        Picasso.with(this).load(image.url).into((ImageView) findViewById(R.id.player_art));

        Picasso.with(this).load(image.url).into(target);

        song = (TextView) findViewById(R.id.player_song);
        song.setText(item.name);
        artist = (TextView) findViewById(R.id.player_artist);
        artist.setText(item.artists.get(0).name);
    }

    @Override
    public void updateUIProgressBar(int position){
        panel_progress.setProgress(position);
        player_scrubber.setProgress(position);
        final int pos = position;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                player_progress.setText(new SimpleDateFormat("mm:ss").format(new Date(pos)));
            }
        });
    }

    @Override
    public void updateUIQueue(List<Track> queue){
        /* Update queue with the one found in service only if
         * you are the broadcaster or you are not in a broadcast
         */
        if(CASTROOM_ID.equals(getUserID()) || CASTROOM_ID.isEmpty()) {
            PlayerQueueFragment pqf = (PlayerQueueFragment) playerFrags[0];
            pqf.reset();
            pqf.addData(spotifyPlayerService.getQueue());
        }
    }

    @Override
    public void updateUIHome(){
        HostFragment hf = (HostFragment) mainFrags[0];
        HomeFragment homeFragment = (HomeFragment) hf.getHostedFragment();
        if(homeFragment != null){
            homeFragment.loadTimeline();
        }
    }


//==================================================================================================
//      Cast Events
//==================================================================================================

    public void setCast(){
        if(CASTROOM_ID != USER.id) {
            spotifyPlayerService.startCast();
            spotifyPlayerService.pullTracksFromCast();
            PlayerQueueFragment pqf = (PlayerQueueFragment) playerFrags[0];
            pqf.getQueueFromBroadCast();
        }
    }

    @Override
    public void endCast(){
        spotifyPlayerService.endCast();
        PlayerQueueFragment pqf = (PlayerQueueFragment) playerFrags[0];
        if(pqf != null)
            pqf.endQueueFromBroadCast();
        updateUIQueue(spotifyPlayerService.getQueue());
        PlayerChatFragment pcf = (PlayerChatFragment) playerFrags[1];
        if(pcf != null)
            pcf.stopChat();
        CASTROOM_ID = "";
    }

//==================================================================================================
//      Misc Helper Methods
//==================================================================================================

    /**
     * Helper methods
     */

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        /* Find the currently focused view, so we can grab the correct window token from it. */
        View view = activity.getCurrentFocus();
        /* If no view currently has focus, create a new one, just so we can grab a window token from it */
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private Target target = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            bitmap = fastBlur(bitmap, 1, 30);

            player_art_blur_ambient.setImageBitmap(bitmap);

            View v = findViewById(R.id.player_controls);
            v.setBackgroundColor(0xAA111112);
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
        }
    };

    public Bitmap fastBlur(Bitmap sentBitmap, float scale, int radius) {

        int width = Math.round(sentBitmap.getWidth() * scale);
        int height = Math.round(sentBitmap.getHeight() * scale);
        sentBitmap = Bitmap.createScaledBitmap(sentBitmap, width, height, false);

        Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);

        if (radius < 1) {
            return (null);
        }

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int[] pix = new int[w * h];
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;

        int r[] = new int[wh];
        int g[] = new int[wh];
        int b[] = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int vmin[] = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int dv[] = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) {
            dv[i] = (i / divsum);
        }

        yw = yi = 0;

        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;

            for (x = 0; x < w; x++) {

                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pix[yw + vmin[x]];

                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;

                sir = stack[i + radius];

                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];

                rbs = r1 - Math.abs(i);

                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;

                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }

                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = ( 0xff000000 & pix[yi] ) | ( dv[rsum] << 16 ) | ( dv[gsum] << 8 ) | dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vmin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += w;
            }
        }
        bitmap.setPixels(pix, 0, w, 0, 0, w, h);

        return (bitmap);
    }

//==================================================================================================
//      Destruction
//==================================================================================================

    @Override
    public void onResume(){
        if(spotifyPlayerService != null){
            spotifyPlayerService.registerCallbacks(MainActivity.this);
            updateUIMetadata(spotifyPlayerService.getCurrentTrack());
            updateUIPlaybackState(spotifyPlayerService.isPlaying());
            updateUIQueue(spotifyPlayerService.getQueue());
            updateUIProgressBar((int) spotifyPlayerService.getProgress());
        }
        super.onResume();
    }

    @Override
    public void onStop(){
        if(isBound){
            spotifyPlayerService.unregisterCallbacks();
            unbindService(serviceConnector);
            isBound = false;
        }
        Picasso.with(this).cancelRequest(target);
        super.onStop();
    }

//==================================================================================================
//      Error Handling
//==================================================================================================
    //Log d=debug, w=warning, e=error, i=info, v=verbose, wtf=should never happen

}
