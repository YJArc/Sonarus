package com.yjarc.sonarus.SpotifyHelper;

import android.util.Log;

import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import kaaes.spotify.webapi.android.models.UserPrivate;
import retrofit.RetrofitError;


public class SimpleRequestListener implements SpotifyRequester.RequestListener {

    private void NoOverrideMessage(String tag) {
        Log.w("SimpleRequestListener", tag + " method not overridden");
    }


    @Override
    public  void getTrack(Track track){
        NoOverrideMessage("void getTrack (String trackID)");
    }

    @Override
    public void getTracks(Tracks tracks){
        NoOverrideMessage("void getTracks (String trackIDs)");
    }

    @Override
    public void getMe(UserPrivate user){
        NoOverrideMessage("void getMe ()");
    }

    @Override
    public void requestError(RetrofitError error){
        NoOverrideMessage("void requestError (Retrofit error)");
        error.printStackTrace();
    }

}
