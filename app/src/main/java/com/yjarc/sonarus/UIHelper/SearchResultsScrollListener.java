package com.yjarc.sonarus.UIHelper;


import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

public abstract class SearchResultsScrollListener extends RecyclerView.OnScrollListener {

    private static final String TAG = SearchResultsScrollListener.class.getSimpleName();

    private final LinearLayoutManager mLayoutManager;

    private static final int SCROLL_BUFFER = 3;
    private int mCurrentItemCount = 0;

    private boolean mAwaitingItems = true;

    public SearchResultsScrollListener(LinearLayoutManager layoutManager) {
        mLayoutManager = layoutManager;
    }

    public void reset() {
        mCurrentItemCount = 0;
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        int itemCount = mLayoutManager.getItemCount();
        int itemPosition = mLayoutManager.findLastVisibleItemPosition();

        if (mAwaitingItems && itemCount > mCurrentItemCount) {
            mCurrentItemCount = itemCount;
            mAwaitingItems = false;
        }

        if (!mAwaitingItems && itemPosition + 1 >= itemCount - SCROLL_BUFFER) {
            mAwaitingItems = true;
            onLoadMore();
        }
    }

    public abstract void onLoadMore();
}
