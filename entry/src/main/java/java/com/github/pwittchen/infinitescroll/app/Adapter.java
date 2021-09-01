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

import ohos.agp.components.*;
import ohos.app.Context;
import java.util.List;

/**
 * AdapterClass.
 */
public class Adapter extends BaseItemProvider {

    /**
     * Context Object.
     */
    private Context mContext;

    /**
     * List Object to hold ListItems.
     */
    private List<String> items;

    /**
     * mIndex.
     */
    private int mIndex;

    /**
     * Adapter constructor.
     *
     * @param context context
     * @param list    list
     */
    public Adapter(Context context, List<String> list) {
        super();

        mContext = context;
        this.items = list;
    }

    /**
     * To add items.
     *
     * @param list list
     */
    public void addItems(List<String> list) {
        this.items = list;
    }

    /**
     * To get listItems.
     *
     * @return List
     */
    public List<String> getItems() {
        return items;
    }

    /**
     * To get Items.
     *
     * @param index index
     * @return Object
     */
    @Override
    public Object getItem(int index) {
        return items.get(index);
    }

    /**
     * To get ItemId.
     *
     * @param index index
     * @return long
     */
    @Override
    public long getItemId(int index) {
        return index;
    }

    /**
     * To get the count.
     *
     * @return int
     */
    @Override
    public int getCount() {
        return items.size();
    }

    /**
     * To get component.
     *
     * @param index              index
     * @param component          component
     * @param componentContainer componentContainer
     * @return Component
     */
    @Override
    public Component getComponent(int index, Component component, ComponentContainer componentContainer) {
        Component view = component;
        this.mIndex = index;
        if (view == null) {
            ComponentContainer rootlayout = (ComponentContainer) LayoutScatter.getInstance(mContext).
                    parse(ResourceTable.Layout_row_item, null, false);
            Text rowText = (Text) rootlayout.findComponentById(ResourceTable.Id_row_text);
            rowText.setText(items.get(index));
            view = rootlayout;
        }
        return view;
    }
}