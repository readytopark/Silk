package com.afollestad.silk.fragments.feed;

import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import com.afollestad.silk.adapters.SilkAdapter;
import com.afollestad.silk.caching.SilkComparable;

import java.util.List;

/**
 * @author Aidan Follestad (afollestad)
 */
public abstract class SilkFeedFragment<ItemType extends SilkComparable> extends ListFragment {

    private boolean isLoading;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        onInitialRefresh();
    }

    protected void onPreLoad() {
    }

    protected void onPostLoad(List<ItemType> results) {
        ((SilkAdapter<ItemType>) getListView().getAdapter()).set(results);
        onLoadComplete(false);
    }

    protected abstract SilkAdapter<ItemType> initializeAdapter();

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

    protected SilkAdapter<ItemType> getAdapter() {
        if (getListView() == null) return null;
        return ((SilkAdapter<ItemType>) getListView().getAdapter());
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
        if (getEmptyText() != 0) setEmptyText(getString(getEmptyText()));
        if (getActivity() != null) getActivity().setTitle(getTitle());
    }

    @Override
    public void setEmptyText(CharSequence text) {
        if (getView() == null) return;
        View emptyText = getView().findViewById(android.R.id.empty);
        if (emptyText != null && emptyText instanceof TextView) {
            ((TextView) emptyText).setText(text);
        } else throw new IllegalStateException("Your layout does not have an empty text.");
    }

    @Override
    public void setListShown(boolean shown) {
        if (getView() == null) return;
        View emptyText = getView().findViewById(android.R.id.empty);
        View progressView = getView().findViewById(android.R.id.progress);
        if (shown) {
            if (getListView() != null) getListView().setVisibility(View.VISIBLE);
            if (emptyText != null) emptyText.setVisibility(getAdapter().getCount() == 0 ? View.VISIBLE : View.GONE);
            if (progressView != null) progressView.setVisibility(View.GONE);
        } else {
            if (getListView() != null) getListView().setVisibility(View.GONE);
            if (emptyText != null) emptyText.setVisibility(View.GONE);
            if (progressView != null) progressView.setVisibility(View.VISIBLE);
        }
    }

    protected abstract List<ItemType> refresh() throws Exception;

    protected abstract void onError(Exception e);

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

    protected void onInitialRefresh() {
        performRefresh(true);
    }
}