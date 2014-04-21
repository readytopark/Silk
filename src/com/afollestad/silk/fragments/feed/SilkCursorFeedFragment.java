package com.afollestad.silk.fragments.feed;

import android.content.ContentResolver;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.afollestad.silk.adapters.SilkAdapter;
import com.afollestad.silk.adapters.SilkCursorAdapter;
import com.afollestad.silk.caching.SilkComparable;
import com.afollestad.silk.caching.SilkCursorItem;
import com.afollestad.silk.fragments.list.SilkCursorListFragment;

import java.util.List;

/**
 * @author Aidan Follestad (afollestad)
 */
public abstract class SilkCursorFeedFragment<ItemType extends SilkCursorItem & SilkComparable> extends SilkCursorListFragment<ItemType> {

    private boolean isLoading;

    protected void onPreLoad() {
        clearProvider();
    }

    protected void onPostLoad(List<ItemType> items) {
        onLoadComplete(false);
        if (items == null || items.size() == 0) return;
        ContentResolver resolver = getActivity().getContentResolver();
        for (ItemType item : items) {
            if (item.getContentValues() == null) continue;
            resolver.insert(getLoaderUri(), item.getContentValues());
        }
        super.onInitialRefresh();
    }

    protected abstract SilkCursorAdapter<ItemType> initializeAdapter();

    public abstract int getEmptyText();

    public abstract int getLayout();

    public abstract String getTitle();

    protected void onItemTapped(int index, ItemType item, View view) {
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        onItemTapped(position, getAdapter().getItem(position), v);
    }

    protected SilkCursorAdapter<ItemType> getAdapter() {
        if (getListView() == null) return null;
        return ((SilkCursorAdapter<ItemType>) getListView().getAdapter());
    }

    protected void onLoadComplete(boolean error) {
        isLoading = false;
        setListShown(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(getLayout(), null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setListAdapter(initializeAdapter());
        setEmptyText(getString(getEmptyText()));
        if (getActivity() != null) getActivity().setTitle(getTitle());
    }

    protected abstract List<ItemType> refresh() throws Exception;

    protected abstract void onError(Exception e);

    @Override
    protected void onCursorEmpty() {
        super.onCursorEmpty();
        performRefresh(true);
    }

    protected void runOnUiThread(Runnable runnable) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(runnable);
    }

    public void performRefresh(boolean showProgress) {
        if (isLoading) return;
        isLoading = true;
        setListShown(!showProgress);
        onPreLoad();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final List<ItemType> items = refresh();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onPostLoad(items);
                        }
                    });
                } catch (final Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onError(e);
                            onLoadComplete(true);
                        }
                    });
                }
            }
        });
        t.setPriority(Thread.MAX_PRIORITY);
        t.start();
    }
}