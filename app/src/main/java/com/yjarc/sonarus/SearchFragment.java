package com.yjarc.sonarus;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v7.widget.LinearLayoutManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;
import com.yjarc.sonarus.RecylerViewAdapters.RecentSearchesRecyclerViewAdapter;
import com.yjarc.sonarus.RecylerViewAdapters.TrackRecyclerViewAdapter;

import com.yjarc.sonarus.RecylerViewAdapters.ItemTouchHelperCallback;
import com.yjarc.sonarus.UIHelper.SearchResultsScrollListener;
import com.loopeer.itemtouchhelperextension.ItemTouchHelperExtension;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.models.Track;


public class SearchFragment extends Fragment implements Search.View{

//==================================================================================================
//      Fields
//==================================================================================================

    private RecyclerView resultSearch;
    private TrackRecyclerViewAdapter searchResultsAdapter;
    private ItemTouchHelperExtension itemTouchHelperExtension;
    private ItemTouchHelperExtension.Callback  mCallback;
    private Search.SearchListener searchListener;
    private class ScrollListener extends SearchResultsScrollListener {
        public ScrollListener(LinearLayoutManager layoutManager) {
            super(layoutManager);
        }

        @Override
        public void onLoadMore() {
            searchListener.loadMoreResults();
        }
    }



    private RecyclerView recentSearch;
    private RecentSearchesRecyclerViewAdapter recentResultsAdapter;

    Boolean doOnce = false;

    String mCurrentQuery = "";
    List<String> recent = new ArrayList<>();

//==================================================================================================
//      Create
//==================================================================================================

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState){
        View v = inflater.inflate(R.layout.fragment_search, container, false);

        initSearch(v);
        loadRecentSearches();

        return v;
}

//==================================================================================================
//      Initialization
//==================================================================================================

    void initSearch(View v){
        /* Allow querying using text entry in searchTracks bar */
        final EditText query = (EditText) v.findViewById(R.id.search_bar);


        //// TODO: 5/21/2017 do you need do once?
        if (!doOnce) {
            searchListener = new SearchPresenter(getActivity(), this);
            searchListener.init();
            searchResultsAdapter = new TrackRecyclerViewAdapter(getActivity(), new TrackRecyclerViewAdapter.ItemSelectedListener() {
                @Override
                public void onItemSelected(View itemView, Track item) {
                    ((MainActivity) getActivity()).setTrack(item);
                }
                @Override
                public void onItemSwipeRight(Track item){
                    ((MainActivity) getActivity()).addToQueue(item);
                }
            });
            recentResultsAdapter = new RecentSearchesRecyclerViewAdapter(getActivity(), new RecentSearchesRecyclerViewAdapter.ItemSelectedListener() {
                @Override
                public void onItemSelected(View itemView, String item) {
                    query.setText(item);
                    query.onEditorAction(EditorInfo.IME_NULL);
                }

                @Override
                public void onItemSwipeLeft(String item) {

                }
            });

            doOnce = true;
        }

        /* Fragment contexts are recreated on tab changes, inform our Listener and Adapter */
        searchResultsAdapter.setContext(getActivity());
        recentResultsAdapter.setContext(getActivity());

        /* Setup RecyclerView to work on current Fragment instance */
        LinearLayoutManager m = new LinearLayoutManager(getActivity());
        ScrollListener mScrollListener = new ScrollListener(m);
        resultSearch = (RecyclerView) v.findViewById(R.id.search_results);
        resultSearch.setLayoutManager(m);
        resultSearch.setAdapter(searchResultsAdapter);
        resultSearch.setHasFixedSize(true);
        resultSearch.addOnScrollListener(mScrollListener);

        mCallback = new ItemTouchHelperCallback(getActivity());
        itemTouchHelperExtension = new ItemTouchHelperExtension(mCallback);
        itemTouchHelperExtension.attachToRecyclerView(resultSearch);

        LinearLayoutManager m2 = new LinearLayoutManager(getActivity());

        recentSearch =(RecyclerView) v.findViewById(R.id.search_recent);
        recentSearch.setLayoutManager(m2);
        recentSearch.setAdapter(recentResultsAdapter);
        recentSearch.setHasFixedSize(true);

        query.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus)
                    if(!recent.isEmpty() && resultSearch.getVisibility() == View.INVISIBLE)
                        recentSearch.setVisibility(View.VISIBLE);
            }
        });

        query.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(query.getText().toString().isEmpty()){
                    recentSearch.setVisibility(View.VISIBLE);
                    reset();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        query.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_NULL
                        && (event == null || event.getAction() == KeyEvent.ACTION_DOWN)) {
                    recentSearch.setVisibility(View.INVISIBLE);
                    resultSearch.setVisibility(View.VISIBLE);
                    String search = query.getText().toString();
                    if (search != null && !search.isEmpty() && !search.equals(mCurrentQuery)) {
                        mCurrentQuery = search;
                        searchListener.searchTracks(search);

                        if(recent.isEmpty()) {
                            recent.add(search);
                        } else {
                            if (recent.indexOf(search) != -1) {
                                Log.e("pos" , ""+(recent.indexOf(search)));
                                recent.remove((recent.indexOf(search)));
                            }
                            recent.add(0, search);
                        }
                        if (recent.size() > 15)
                            recent.remove(recent.size() - 1);

                        recentResultsAdapter.clearData();
                        recentResultsAdapter.addData(recent);

                        storeRecentSearches();

                        ((MainActivity)getActivity()).hideKeyboard(getActivity());
                    }
                }
                return true;
            }
        });

    }

//==================================================================================================
//      Search Entry Storage
//==================================================================================================

    public void storeRecentSearches(){
        for(String i : recent)
            Log.e("storing", i);

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("Sonarus", Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = sharedPreferences.edit();

        Gson gson = new Gson();
        String jsonText = gson.toJson(recent);
        prefEditor.putString("recent_searches", jsonText);
        prefEditor.commit();
    }

    public void loadRecentSearches(){
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("Sonarus", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String jsonText = sharedPreferences.getString("recent_searches", null);
        ArrayList<String> hi = gson.fromJson(jsonText, ArrayList.class);
        if (hi != null){
            for (String i : hi) {
                Log.e("loading recent", i);
                recent.add(i);
                recentResultsAdapter.clearData();
                recentResultsAdapter.addData(recent);
            }
            sharedPreferences.edit().clear().commit();
        } else{
            recentSearch.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void reset(){
        searchResultsAdapter.clearData();
    }

    @Override
    public void addData(List<Track> items){
        searchResultsAdapter.addData(items);
    }



}
