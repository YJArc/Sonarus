package com.yjarc.sonarus.SpotifyHelper;

import android.util.Log;

import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import kaaes.spotify.webapi.android.models.UserPrivate;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static com.yjarc.sonarus.AppConstants.SPOTIFY_SERVICE;


public class SpotifyRequester {

    public interface RequestListener{
        void getTrack(Track track);
        void getTracks(Tracks tracks);
        void getMe(UserPrivate user);
        void requestError(RetrofitError error);
    }

    public void getMe(final RequestListener requestListener){
        SPOTIFY_SERVICE.getMe(new Callback<UserPrivate>() {
            @Override
            public void success(UserPrivate userPrivate, Response response) {
                requestListener.getMe(userPrivate);
            }

            @Override
            public void failure(RetrofitError error) {
                requestListener.requestError(error);
            }
        });
    }

    public void getTrack(String trackID, final RequestListener requestListener){
        Log.e("fetching", trackID);
        SPOTIFY_SERVICE.getTrack(trackID, new Callback<Track>() {
            @Override
            public void success(Track track, Response response) {
                requestListener.getTrack(track);
            }

            @Override
            public void failure(RetrofitError error) {
                requestListener.requestError(error);
            }
        });
    }


    /* trackIds is a String concat of Spotify's unique track ids separated by a comma (No spaces) */
    public void getTracks(String trackIds, final RequestListener requestListener)
    {
        Log.e("requester fetching", trackIds);
        SPOTIFY_SERVICE.getTracks(trackIds, new Callback<Tracks>() {
            @Override
            public void success(Tracks tracks, Response response) {
                Log.e("success", "success");
                requestListener.getTracks(tracks);
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e("failure", "failure");
                requestListener.requestError(error);
            }
        });

    }



}
