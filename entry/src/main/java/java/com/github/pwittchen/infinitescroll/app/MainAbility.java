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
package java.com.github.pwittchen.infinitescroll.app;

import ohos.aafwk.ability.Ability;
import ohos.aafwk.content.Intent;
import ohos.agp.components.*;
import ohos.eventhandler.EventHandler;
import ohos.eventhandler.EventRunner;
import java.com.github.pwittchen.infinitescroll.app.slice.MainAbilitySlice;
import java.com.github.pwittchen.infinitescroll.library.InfiniteScrollListener;
import java.com.github.pwittchen.infinitescroll.library.util.LogUtil;
import java.util.LinkedList;
import java.util.List;

/**
 * MainAbility class to launch the Application.
 */
public class MainAbility extends Ability {

    /**
     * Defines maximum items per request.
     */
    private static final int MAX_ITEMS_PER_REQUEST = 10;

    /**
     * Defines number of items.
     */
    private static final int NUMBER_OF_ITEMS = 100;

    /**
     * Simulated loading time.
     */
    private static final int SIMULATED_LOADING_TIME_IN_MS = 1500;

    /**
     * Label.
     */
    public static final String LABEL = "INFINITE_SCROLL";

    /**
     * To store list of items.
     */
    private List<String> items;

    /**
     * ListContainer.
     */
    private ListContainer list;

    /**
     * RoundedProgressBar.
     */
    private RoundProgressBar mProgressBar;

    /**
     * Adapter.
     */
    private Adapter mAdapter;

    /**
     * page defines Integer value.
     */
    private int page = 0;

    /**
     * EventHandler Object to handle the Events.
     */
    private EventHandler eventHandler = new EventHandler(EventRunner.getMainEventRunner());

    /**
     * onStart.
     *
     * @param intent intent
     */
    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);
        super.setMainRoute(MainAbilitySlice.class.getName());
        this.items = createItems();
        setUIContent(ResourceTable.Layout_ability_main);
        initViews();
    }

    /**
     * To create the Items.
     *
     * @return List
     */
    private static List<String> createItems() {
        List<String> itemsLocal = new LinkedList<>();
        for (int i = 0; i < NUMBER_OF_ITEMS; i++) {
            String prefix = i < 10 ? "0" : "";
            itemsLocal.add("Item #".concat(prefix).concat(String.valueOf(i)));
        }
        return itemsLocal;
    }

    /**
     * IntiViews.
     */
    public void initViews() {
        list = (ListContainer) findComponentById(ResourceTable.Id_list_view);
        mProgressBar = (RoundProgressBar) findComponentById(ResourceTable.Id_progressBar);
        initListView();

    }

    /**
     * IntiView in List formate.
     */
    private void initListView() {
        mAdapter = new Adapter(this, items.subList(page, MAX_ITEMS_PER_REQUEST));
        list.setItemProvider(mAdapter);
        list.setScrolledListener(createInfiniteScrollListener());
    }

    /**
     * To create the createInfiniteScrollListener.
     *
     * @return InfiniteScrollListener
     */
    private InfiniteScrollListener createInfiniteScrollListener() {
        LogUtil.info(LABEL, "createInfiniteScrollListener");
        return new InfiniteScrollListener(MAX_ITEMS_PER_REQUEST, list) {
            @Override public void onScrolledToEnd(final int firstVisibleItemPosition) {
                LogUtil.info(LABEL, "onScrolledToEnd");
                simulateLoading();
                int start = ++page * MAX_ITEMS_PER_REQUEST;
                final boolean allItemsLoaded = start >= items.size();
                LogUtil.info(LABEL, "allItemsLoaded" + allItemsLoaded);
                if (allItemsLoaded) {
                    mProgressBar.setVisibility(Component.HIDE);
                } else {
                    int end = start + MAX_ITEMS_PER_REQUEST;
                    final List<String> itemsLocal = getItemsToBeLoaded(start, end);
                    mAdapter.addItems(itemsLocal);
                    refreshView(list, mAdapter, firstVisibleItemPosition);
                }
            }
        };
    }

    /**
     * To get which Items to be loaded.
     *
     * @param start start
     * @param end end
     * @return List
     */
    private List<String> getItemsToBeLoaded(int start, int end) {
        List<String> newItems = items.subList(start, end);
        final List<String> oldItems = ((Adapter) list.getItemProvider()).getItems();
        final List<String> itemsLocal = new LinkedList<>();
        itemsLocal.addAll(oldItems);
        itemsLocal.addAll(newItems);
        return itemsLocal;
    }

    /**
     * SimulateLoading.
     */
    private void simulateLoading() {
        LogUtil.info(LABEL, "simulateLoading");
        mProgressBar.setVisibility(Component.VISIBLE);
        eventHandler.removeAllEvent();
        eventHandler.postTask(() -> {
            mProgressBar.setVisibility(Component.HIDE);
        }, SIMULATED_LOADING_TIME_IN_MS);
    }
}