# InfiniteScroll
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=applibgroup_SpannableTextView&metric=alert_status)](https://sonarcloud.io/dashboard?id=applibgroup_SpannableTextView)
[![Build](https://github.com/applibgroup/SpannableTextView/actions/workflows/main.yml/badge.svg)](https://github.com/applibgroup/SpannableTextView/actions/workflows/main.yml)
 
## Introduction
 
###### Infinite Scroll (Endless Scrolling) for ListContainer in HMOS.

## Source

###### The code in this repository was inspired from [pwittchen/InfiniteScroll-v0.0.3](https://github.com/pwittchen/InfiniteScroll). We are very thankful to pwittchen.

## Screenshot

  ![Continuous, Discrete, Custom Java layout](images/list1.png)
  ![Continuous, Discrete, Custom Java layout](images/list2.png)
  ![Continuous, Discrete, Custom Java layout](images/list3.png)

## Installation

In order to use the following library, add the following line to your **root** gradle file:

1.For using InfiniteScroll module in sample app, include the source code and add the below dependencies in entry/build.gradle to generate hap/support.har.

```
dependencies{
    implementation fileTree(dir: 'libs', include: ['*.jar', '*.har'])
    implementation project(path: ':infinitescroll')
    testImplementation 'junit:junit:4.13'
}
```
2.For using InfiniteScroll in separate application using har file, add the har file in the entry/libs folder and add the dependencies in entry/build.gradle file.

```
dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar','*.har'])
    testImplementation 'junit:junit:4.13'
}
```
​
## Usage

Create necessary fields in your `MainAbility`:

```java
private ListContainer list;
```

Create new `InfiniteScrollListener`:

```java
private InfiniteScrollListener createInfiniteScrollListener() {
  return new InfiniteScrollListener(maxItemsPerRequest) {
    @Override public void onScrolledToEnd(final int firstVisibleItemPosition) {
      // load your items here
      // logic of loading items will be different depending on your specific use case

      // when new items are loaded, combine old and new items, pass them to your adapter
      // and call refreshView(...) method from InfiniteScrollListener class to refresh RecyclerView
      refreshView(recyclerView, new MyAdapter(items), firstVisibleItemPosition);
    }
  }
}
```

Initialize `ListContainer` in your `MainAbility

```java
@Override public void onStart(Intent intent) {
    list = (ListContainer) findComponentById(ResourceTable.Id_list_view);
    initListView();
    }
private void initListView() {
     mAdapter = new Adapter(this, items.subList(page, MAX_ITEMS_PER_REQUEST));
     list.setItemProvider(mAdapter);
     list.setScrolledListener(createInfiniteScrollListener());
    }
```

If you want to display loading progress, you should add additional view for it, show it while loading starts and hide it when loading is finished. Check exemplary app in this repository to see concrete solution.

That's it!
## Support & extension

Currently there is a limitation in display of list items, where the items are repeated. This can be improved by effective use of appropriate collection framework and refresh of Adapter (BaseItemProvider)

### License
```
Copyright 2016 Piotr Wittchen

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
