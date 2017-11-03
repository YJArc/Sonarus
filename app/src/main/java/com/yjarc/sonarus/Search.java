package com.yjarc.sonarus;

import java.util.List;

import kaaes.spotify.webapi.android.models.Track;

public class Search {

    public interface View {
        void reset();

        void addData(List<Track> items);

    }

    public interface SearchListener {

        void init();

        void searchTracks(String query);

//        void searchURI(String uri);

        void loadMoreResults();

    }

}
