package com.yjarc.sonarus.MediaHelper;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.yjarc.sonarus.MainActivity;
import com.yjarc.sonarus.SpotifyHelper.SimpleRequestListener;
import com.yjarc.sonarus.SpotifyHelper.SpotifyRequester;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Connectivity;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.PlaybackBitrate;
import com.spotify.sdk.android.player.PlaybackState;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.UserPrivate;

import static com.yjarc.sonarus.AppConstants.CASTROOM_ID;
import static com.yjarc.sonarus.AppConstants.CASTROOM_TITLE;
import static com.yjarc.sonarus.AppConstants.CLIENT_ID;
import static com.yjarc.sonarus.AppConstants.SPOTIFY_API;
import static com.yjarc.sonarus.AppConstants.SPOTIFY_PLAYER;
import static com.yjarc.sonarus.AppConstants.SPOTIFY_SERVICE;
import static com.yjarc.sonarus.AppConstants.USER;


public class SpotifyPlayerService extends Service implements
        AudioManager.OnAudioFocusChangeListener, Player.NotificationCallback,
        ConnectionStateCallback {
    private static final String TAG = "SPS ";

    /* Member variables */

    /* Binding */
    private final IBinder binder = new LocalBinder();
    @Override
    public IBinder onBind (Intent arg){
        return binder;
    }

    public class LocalBinder extends Binder {
        public SpotifyPlayerService getService(){
            return SpotifyPlayerService.this;
        }
    }

    /* Notifications */
    public NotificationManagerCompat mNotificationManager;
    public static final int NOTIFICATION_ID = 428;

    /* Spotify */
    public static final String EXTRA_TOKEN = "EXTRA_TOKEN";
    private SpotifyPlayer spotifyPlayer = null;
    private PlaybackState playbackState = null;
    private Track track = null;
    private List<Track> queue = new ArrayList<>();
    private final Player.OperationCallback mOperationCallback = new Player.OperationCallback() {
        @Override
        public void onSuccess() {
            Log.d("Player", "OK!");
        }

        @Override
        public void onError(Error error) {
            Log.e("Player", "Error:" + error);
        }
    };


    /* Media session */
    public MediaSessionCompat mediaSession = null;
    private static final int REQUEST_CODE = 99;
    private MediaSessionCompat.Callback mediaSessionCallback = new MediaSessionCompat.Callback() {

        @Override
        public void onPlay() {
            super.onPlay();
            if(CASTROOM_ID.equals("") || CASTROOM_ID.equals(USER.id))
                requestPlay();
        }

        @Override
        public void onPause() {
            super.onPause();
            if(CASTROOM_ID.equals("") || CASTROOM_ID.equals(USER.id))
                requestPause();
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            if(CASTROOM_ID.equals("") || CASTROOM_ID.equals(USER.id))
                requestSkipNext();
        }
    };

//==================================================================================================
//      Service Initialization
//==================================================================================================

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            /* Token passed from LoginActivity -> MainActivity -> SpotifyPlayerService */
            initSpotifyPlayer(intent.getStringExtra(EXTRA_TOKEN));
            initMediaSession();
            mNotificationManager = NotificationManagerCompat.from(SpotifyPlayerService.this);
            initNoisyReceiver();
            /* Redirect media button presses through intent into a button handler*/
            MediaButtonReceiver.handleIntent(mediaSession, intent);
        }
        return super.onStartCommand(intent, flags, startId);
    }

//==================================================================================================
//      Initialization of Member Variables
//==================================================================================================

    public void initSpotifyPlayer(final String token) {
        Log.e("Spotify Token", ""+token);

        if (SPOTIFY_PLAYER == null) {
            Config playerConfig = new Config(getApplicationContext(), token, CLIENT_ID);
            SPOTIFY_PLAYER = Spotify.getPlayer(playerConfig, this, new SpotifyPlayer.InitializationObserver() {
                @Override
                public void onInitialized(SpotifyPlayer player) {
                    Log.i("Spotify Streaming", "Success");
                    SPOTIFY_PLAYER.setConnectivityStatus(mOperationCallback, getNetworkConnectivity(SpotifyPlayerService.this));
                    SPOTIFY_PLAYER.addNotificationCallback(SpotifyPlayerService.this);
                    SPOTIFY_PLAYER.addConnectionStateCallback(SpotifyPlayerService.this);

                    /* Give spotify our access token so we can use web API calls*/
                    SPOTIFY_API = new SpotifyApi();
                    if (token != null) {
                        SPOTIFY_API.setAccessToken(token);
                    } else {
                        Log.e("Spotify", "Access token not granted");
                    }
                    SPOTIFY_SERVICE = SPOTIFY_API.getService();
                    spotifyPlayer = SPOTIFY_PLAYER;
                    initUser();
                }

                @Override
                public void onError(Throwable error) {
                    Log.e("Error in initialization", "" + error.getMessage());
                }
            });
        }

    }

    SpotifyRequester spotifyRequester = new SpotifyRequester();
    SpotifyRequester.RequestListener requestUserListener = new SimpleRequestListener(){
        @Override
        public void getMe(UserPrivate user){
            USER = user;
            Log.e("Spotify User", USER.id);

            /* Create user if not found in Database*/
            createUser();

            /* Update home page with user's timeline */
            if(uiCallbacks != null){
                uiCallbacks.updateUIHome();
            }
        }
    };

    void initUser(){
        spotifyRequester.getMe(requestUserListener);
    }

    private Connectivity getNetworkConnectivity(Context context) {
        ConnectivityManager connectivityManager;
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            return Connectivity.fromNetworkType(activeNetwork.getType());
        } else {
            return Connectivity.OFFLINE;
        }
    }

    private void initMediaSession() {
        if (mediaSession != null)
            return;
        ComponentName mediaButtonReceiver = new ComponentName(getApplicationContext(), MediaButtonReceiver.class);
        mediaSession = new MediaSessionCompat(getApplicationContext(), TAG, mediaButtonReceiver, null);
        mediaSession.setCallback(mediaSessionCallback);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setClass(this, MediaButtonReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);
        mediaSession.setMediaButtonReceiver(pendingIntent);

//        setSessionToken(mediaSession.getSessionToken());

        // This is an Intent to launch the app's UI, used primarily by the ongoing notification.
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        mediaSession.setSessionActivity(pi);
    }

    public void createUser(){
        final DatabaseReference userbase = FirebaseDatabase.getInstance().getReference().child("Userbase");
        userbase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChild(USER.id)){

                }
                else{
                    Map <String, Object> map = new HashMap<>();
                    map.put(USER.id, "");
                    userbase.updateChildren(map);
                    DatabaseReference user = userbase.child(USER.id);

                    Map <String, Object> map2 = new HashMap<>();
                    map2.put("1Name", getUserName());
                    map2.put("2Bio", "Sonarus User");
                    map2.put("3Image", "https://media.licdn.com/mpr/mpr/shrinknp_400_400/AAEAAQAAAAAAAAnRAAAAJGEzNDhlMjU3LTgwODUtNDkxMy05ODU4LTFjMWU3MzFmYWVjNA.jpg");
                    map2.put("4Banner", "");
                    map2.put("4Followers", "");
                    map2.put("5Following", "");
                    map2.put("6Statuses", "");
                    map2.put("7Timeline", "");

                    user.updateChildren(map2);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public String getUserName(){
        if(USER != null){
            if(USER.display_name == null)
                return USER.id;
            return USER.display_name;
        }
        return "USER_NOT_SET";
    }

//==================================================================================================
//      Spotify Player Requests
//==================================================================================================

    /* Current track playback requests. */

    public void setTrack(Track item) {
        track = item;
        Log.i(TAG + "Request", "Track was set: " +  track.name);

        if(!requestAudioFocus()){
            updateMetadata();
            return;
        }

        mediaSession.setActive(true);
        pushTrackToCast(true,0);
        spotifyPlayer.playUri(mOperationCallback, track.uri, 0, 0);
        spotifyPlayer.setPlaybackBitrate(mOperationCallback, PlaybackBitrate.BITRATE_LOW);

        updateMetadata();
    }

    public void syncTrackPos(int position){
        if(!requestAudioFocus()){
            return;
        }
        mediaSession.setActive(true);
        if(position >= track.duration_ms)
            return;
        else {
            spotifyPlayer.playUri(mOperationCallback, track.uri, 0, position);
            spotifyPlayer.setPlaybackBitrate(mOperationCallback, PlaybackBitrate.BITRATE_LOW);
        }

    }

    public void requestPlay() {
        if(track == null) {
            if (!queue.isEmpty())
                popQueue();
            else
                return;
        }

        Log.i(TAG + "Request", "Resume playback: " +  track.name);
        if(!requestAudioFocus()){
            return;
        }

        mediaSession.setActive(true);
        pushTrackToCast(true, SPOTIFY_PLAYER.getPlaybackState().positionMs);
        spotifyPlayer.resume(mOperationCallback);
    }

    public void requestPause() {
        if(track == null)
            return;

        Log.i(TAG + "Request", "Pause playback: " +  track.name);
        pushTrackToCast(false, -1);
        spotifyPlayer.pause(mOperationCallback);
    }

    public void requestSkipNext(){
        Log.i(TAG + "Request", "Skip to next");
        popQueue();
    }

    /* Playback state methods */

    public Track getCurrentTrack(){
        return track;
    }

    public boolean isPlaying(){
        if(playbackState == null)
            return false;
        return playbackState.isPlaying;
    }

    public long getProgress(){
        if(playbackState != null){
            return playbackState.positionMs;
        }
        return 0;
    }

    /* Queue methods. */

    public void addToQueue(Track item){
        queue.add(item);

        pushQueueToCast();

        if(uiCallbacks != null)
            uiCallbacks.updateUIQueue(queue);
    }

    public void popQueue(){
        if(!queue.isEmpty()) {
            Log.i(TAG + "Queue", "Popping next track from queue");
            setTrack(queue.remove(0));

            pushQueueToCast();

            if(uiCallbacks != null) {
                uiCallbacks.updateUIQueue(queue);
            }
        }
    }

    public List<Track> getQueue(){
        return queue;
    }

    /* Broadcasting methods */

    public void pushTrackToCast(boolean isPlaying, long position) {
        if (!CASTROOM_ID.equals(USER.id))
            return;

        Log.e("pushing", "ok!");

        DatabaseReference song_root = FirebaseDatabase.getInstance().getReference()
                .child("Roombase")
                .child(CASTROOM_ID)
                .child("6Songs");

        Map<String, Object> map = new HashMap<>();
        long timestamp = System.currentTimeMillis();
        map.put("1trackId", track.id);
        map.put("2isPlaying", isPlaying);
        map.put("3position", position );
        map.put("4timestamp", timestamp);

        song_root.updateChildren(map);
    }

    public void pushQueueToCast(){
        if(USER != null && CASTROOM_ID.equals(USER.id+1)){
            String trackIDs = "";
            for( Track i : queue){
                trackIDs += i.id +",";
            }

            DatabaseReference queue_root = FirebaseDatabase.getInstance().getReference()
                    .child("Roombase")
                    .child(CASTROOM_ID);

            Map<String,Object> map = new HashMap<>();
            map.put("7Queue", trackIDs);
            queue_root.updateChildren(map);
        }
    }

    DatabaseReference song_root;
    ValueEventListener song_listener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            /* No song was loaded when Cast began. */
            final Iterator i = dataSnapshot.getChildren().iterator();
            if(!i.hasNext())
                return;

            spotifyRequester.getTrack((String) ((DataSnapshot) i.next()).getValue()
                    , new SimpleRequestListener(){
                        @Override
                        public void getTrack(Track item){
                            // FIREBASE path: Root -> Roombase -> userID -> 6Songs
                            track = item;                                                           // 1trackID
                            Boolean isPlaying = (Boolean) ((DataSnapshot) i.next()).getValue();     // 2isPlaying
                            int position = ((Long) ((DataSnapshot) i.next()).getValue()).intValue();// 3position
                            long timestamp = (long) ((DataSnapshot) i.next()).getValue();           // 4timestamp
                            Log.e("Long timestamp", ""+timestamp);

                            if (!isPlaying) {
                                syncTrackPos(position);
                                requestPause();
                            } else {
                                position += (int) (System.currentTimeMillis() - timestamp);
                                syncTrackPos(position);
                            }
                            updateMetadata();
                        }

                    });
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };
    public void pullTracksFromCast(){
        song_root = FirebaseDatabase.getInstance().getReference()
                .child("Roombase")
                .child(CASTROOM_ID)
                .child("6Songs");
        song_root.addValueEventListener(song_listener);
    }


    public void startCast(){
        cast_root = FirebaseDatabase.getInstance().getReference()
                .child("Roombase");
        cast_root.addChildEventListener(cast_listener);

    }

    public void endCast(){
        if(song_root != null)
            song_root.removeEventListener(song_listener);
        if(cast_root != null){
            cast_root.removeEventListener(cast_listener);
        }
    }

    DatabaseReference cast_root;
    ChildEventListener cast_listener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            String removed_cast = dataSnapshot.getKey();
            Log.e("removed", removed_cast);
            if (removed_cast.equals(CASTROOM_ID)){
                if(uiCallbacks != null)
                    uiCallbacks.endCast();
                requestPause();
            }
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

//==================================================================================================
//      Spotify Player Callbacks
//==================================================================================================

    @Override
    public void onConnectionMessage(final String message) {
        Log.i("Connection MSG", message);
    }

    @Override
    public void onLoggedIn() {
        Log.i("Login Status", "True");
    }

    @Override
    public void onLoggedOut() {
        Log.i("Login Status", "False");
    }

    @Override
    public void onLoginFailed(Error error) {
        Log.e("Login error", "" + error);
    }

    @Override
    public void onTemporaryError() {
        Log.e("Callback", "Temporary error occurred");
    }

    @Override
    public void onPlaybackEvent(PlayerEvent event) {
//        Log.i("Playback Event", "" + event);
        if(event == PlayerEvent.kSpPlaybackNotifyTrackDelivered){
            popQueue();
            return;
        }
        playbackState = SPOTIFY_PLAYER.getPlaybackState();
//        Log.i("Playback State", "" + playbackState);
        setMediaPlaybackState();
    }

    @Override
    public void onPlaybackError(Error error) {
        Log.e("Playback Error", "" + error);
    }


//==================================================================================================
//      Update UI and Notification
//==================================================================================================

    private void setMediaPlaybackState() {
        PlaybackStateCompat.Builder playbackStateBuilder = new PlaybackStateCompat.Builder();
        if (playbackState.isPlaying) {
            playbackStateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT);
            playbackStateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, playbackState.positionMs, 1);
            if(uiCallbacks != null){
                uiCallbacks.updateUIPlaybackState(true);
            }
        } else {
            playbackStateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_SKIP_TO_NEXT);
            playbackStateBuilder.setState(PlaybackStateCompat.STATE_PAUSED, playbackState.positionMs, 1);
            if(uiCallbacks != null){
                uiCallbacks.updateUIPlaybackState(false);
            }
            if(mThreadProgress!= null){
                mThreadProgress.interrupt();
            }
        }
        mediaSession.setPlaybackState(playbackStateBuilder.build());

        if (playbackState.isPlaying) {
            Notification notification = postNotification();
            startForeground(NOTIFICATION_ID, notification);
        } else {
            postNotification();
            stopForeground(false);
        }
    }

    Thread mThreadProgress;
    boolean isProgressRunning = false;
    private Notification postNotification() {
        final NotificationCompat.Builder builder = MediaNotificationHelper.from(SpotifyPlayerService.this, mediaSession);

        /* Update the Progress Bar in UI thread to reflect playback position periodically*/
        if(!isProgressRunning && playbackState.isPlaying) {
            mThreadProgress = new Thread(){
                    @Override
                    public void run() {
                        Log.e("Progress", "starting");
                        isProgressRunning = true;
                        while (!Thread.currentThread().isInterrupted()) {
                            try {
                                if(uiCallbacks != null)
                                    uiCallbacks.updateUIProgressBar((int) spotifyPlayer.getPlaybackState().positionMs);
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                Log.w(TAG+"Track Progress", "No longer monitoring");
                                Thread.currentThread().interrupt();
                                isProgressRunning = false;
                            }
                        }
                    }
            };
            mThreadProgress.start();
        }

        Notification notification = builder.build();
        if (notification == null)
            return null;
        mNotificationManager.notify(NOTIFICATION_ID, notification);
        return notification;
    }

    Thread getMeta;
    public void updateMetadata() {
        if(uiCallbacks != null)
            uiCallbacks.updateUIMetadata(track);

        getMeta = (new Thread (new Runnable() {

            @Override
            public void run() {
                try {

                    URL url = new URL(track.album.images.get(0).url);
                    Bitmap artLarge = BitmapFactory.decodeStream(url.openConnection().getInputStream());

                    MediaMetadataCompat metadata = new MediaMetadataCompat.Builder()
                            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, track.id)
                            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, track.uri)
                            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, getSubtitle())
                            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artists.get(0).name)
                            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, track.duration_ms)
                            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, track.album.uri)
                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.name)
                            .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, track.track_number)
                            .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, artLarge) //fullscreen art background*/
                            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artLarge) //pre lolly
                            .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, artLarge) //post lolly
                            .build();
                    mediaSession.setMetadata(metadata);

                } catch (IOException e) {
                    System.out.println(e);
                    return;
                }
            }
        }));
        getMeta.start();

        try{
            getMeta.join();
        } catch(InterruptedException e){

        }
    }

    public String getSubtitle(){

        if(!CASTROOM_ID.isEmpty())
            return CASTROOM_TITLE;
        return track.album.name;
    }

//==================================================================================================
//      Update UI Callbacks
//==================================================================================================

    private UICallbacks uiCallbacks;
    public interface UICallbacks {
        void updateUIMetadata(Track item);
        void updateUIPlaybackState(boolean isPlaying);
        void updateUIQueue(List<Track> queue);
        void updateUIProgressBar(int position);
        void updateUIHome();
        void endCast();
    }
    public void registerCallbacks(UICallbacks uiCallbacks){
        this.uiCallbacks = uiCallbacks;
    }
    public void unregisterCallbacks(){
        uiCallbacks = null;
    }

//==================================================================================================
//    Friendly System Environment Methods
//==================================================================================================

    /* A receiver to catch events in which the system detects a change in speaker
     * i.e. attaching/detaching headphones, switching to bluetooth speakers, etc
     */
    private BroadcastReceiver mNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (playbackState != null && playbackState.isPlaying) {
                Log.w(TAG, "Noisy Request received... Pause Default action");
                requestPause();
            }
        }
    };

    private void initNoisyReceiver() {
        IntentFilter filter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(mNoisyReceiver, filter);
    }

    /* Nicely allow other Apps to lend Audio Focus or lower volume during certain
     * System sounds (Notification tones, phone rings, etc.)
     */
    private boolean requestAudioFocus() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        return result == AudioManager.AUDIOFOCUS_GAIN;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
                if (playbackState != null && playbackState.isPlaying) {
                    Log.w("AudioFocus", "Loss -> pausing");
                    requestPause();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                Log.w("AudioFocus", "Loss transient -> pausing");
                requestPause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                Log.w("AudioFocus", "Can duck -> lowering volume");
                Log.e("Spotify Player", "Volume changes not supported in Spotify SDK 23");
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                Log.w("AudioFocus", "Gained -> resetting volume");
                Log.e("Spotify Player", "Volume changes not supported in Spotify SDK 23");
                break;
        }
    }


//==================================================================================================
//    Destruction
//==================================================================================================

    @Override
    public void onDestroy() {
        super.onDestroy();

        mediaSession.release();
        if(getMeta != null) {
            getMeta.interrupt();
        }

        if (SPOTIFY_PLAYER != null) {
            SPOTIFY_PLAYER.removeNotificationCallback(SpotifyPlayerService.this);
            SPOTIFY_PLAYER.removeConnectionStateCallback(SpotifyPlayerService.this);
        }
        Spotify.destroyPlayer(this);

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.abandonAudioFocus(this);
        unregisterReceiver(mNoisyReceiver);
        mNotificationManager.cancel(NOTIFICATION_ID);
    }

}