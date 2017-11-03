package com.yjarc.sonarus;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.util.List;
import kaaes.spotify.webapi.android.models.Track;


public class SearchPresenter implements Search.SearchListener {

    private static final String TAG = SearchPresenter.class.getSimpleName();
    public static final int PAGE_SIZE = 10;

    private Context mContext;
    private final Search.View mView;
    private String mCurrentQuery;

    private SearchPager mSearchPager;
    private SearchPager.CompleteListener mSearchListener;

    public SearchPresenter(Context context, Search.View view) {
        mContext = context;
        mView = view;
    }


    @Override
    public void init() {
        mSearchPager = new SearchPager();
    }


    @Override
    public void searchTracks(@Nullable String searchQuery) {
        if (searchQuery != null && !searchQuery.isEmpty() && !searchQuery.equals(mCurrentQuery)) {

            mCurrentQuery = searchQuery;
            mView.reset();
            mSearchListener = new SearchPager.CompleteListener() {
                @Override
                public void onComplete(List<Track> items) {
                    mView.addData(items);
                }

                @Override
                public void onError(Throwable error) {
                    logMessage(error.getMessage());
                }
            };
            mSearchPager.getFirstPage(searchQuery, PAGE_SIZE+2, mSearchListener);
        }
    }


    @Override
    public void loadMoreResults() {
        Log.i(TAG,"Loading more...");
        mSearchPager.getNextPage(mSearchListener);
    }



    private void logMessage(String msg) {
        Log.e(TAG, msg);
        Toast.makeText(mContext,msg, Toast.LENGTH_SHORT).show();
    }

}