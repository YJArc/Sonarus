package com.yjarc.sonarus;

import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.spotify.sdk.android.player.SpotifyPlayer;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.UserPrivate;


public class AppConstants {

    /* Constants */
    public static final String CLIENT_ID = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";

    /* For android spotify api */
    public static SpotifyApi SPOTIFY_API;
    public static SpotifyPlayer SPOTIFY_PLAYER;

    /* For spotify web api */
    public static SpotifyService SPOTIFY_SERVICE;


    public static String CASTROOM_ID = "";
    public static String CASTROOM_TITLE = "";
    public static UserPrivate USER;

    public static ObservableScrollViewCallbacks observableScrollViewCallbacks;

}
