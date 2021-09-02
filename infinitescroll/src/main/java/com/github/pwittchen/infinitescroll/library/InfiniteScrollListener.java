/*
 * Copyright (C) 2016 Piotr Wittchen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.pwittchen.infinitescroll.library;

import ohos.agp.components.BaseItemProvider;
import ohos.agp.components.Component;
import ohos.agp.components.ListContainer;
import com.github.pwittchen.infinitescroll.library.util.LogUtil;

/**
 * InfiniteScrollListener.
 */
public abstract class InfiniteScrollListener implements Component.ScrolledListener {

    /**
     * maxItemsPerRequest.
     */
    private int maxItemsPerRequest;

    /**
     * listView.
     */
    private ListContainer listView;

    /**
     * LABEL.
     */
    public static final String LABEL = "INFINITE_SCROLL";

    /**
     * InfiniteScrollListener.
     *
     * @param maxItemsPerRequest maxItemsPerRequest
     * @param listView           listView
     */
    protected InfiniteScrollListener(int maxItemsPerRequest, ListContainer listView) {
        this.maxItemsPerRequest = maxItemsPerRequest;
        this.listView = listView;
        Preconditions.checkNotNull(this.listView, "ListContainer null value passed");
        Preconditions.checkIfPositive(this.maxItemsPerRequest, "maxItemsPerRequest <= 0");
    }

    /**
     * onContentScrolled.
     *
     * @param component component
     * @param i         i
     * @param i1        i1
     * @param i2        i2
     * @param i3        i3
     */
    @Override
    public void onContentScrolled(Component component, int i, int i1, int i2, int i3) {
        LogUtil.info(LABEL, "onContentScrolled");
        if (canLoadMoreItems()) {
            onScrolledToEnd(listView.getItemPosByVisibleIndex(0));
        }
    }

    /**
     * canLoadMoreItems.
     *
     * @return boolean
     */
    protected boolean canLoadMoreItems() {
        final int visibleItemsCount = listView.getVisibleIndexCount();
        final int totalItemsCount = listView.getChildCount();
        final int pastVisibleItemsCount = listView.getFirstVisibleItemPosition();
        final boolean lastItemShown = visibleItemsCount + pastVisibleItemsCount >= totalItemsCount;
        return lastItemShown && totalItemsCount >= maxItemsPerRequest;
    }

    /**
     * refreshView.
     *
     * @param view     view
     * @param adapter  adapter
     * @param position position
     */
    protected void refreshView(ListContainer view, BaseItemProvider adapter, int position) {
        LogUtil.info(LABEL, "refreshView");
        adapter.notifyDataChanged();
        view.invalidate();
        view.scrollTo(position);
    }

    /**
     * onScrolledToEnd.
     *
     * @param firstVisibleItemPosition firstVisibleItemPosition
     */
    public abstract void onScrolledToEnd(final int firstVisibleItemPosition);
}