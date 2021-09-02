/*
 * Copyright (C) 2020-21 Application Library Engineering Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.pwittchen.infinitescroll.app;

import ohos.aafwk.ability.delegation.AbilityDelegatorRegistry;
import ohos.agp.components.ListContainer;
import ohos.app.Context;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.manipulation.Ordering;

import com.github.pwittchen.infinitescroll.library.InfiniteScrollListener;
import com.github.pwittchen.infinitescroll.library.Preconditions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * ExampleOhosTest.
 */
public class ExampleOhosTest {

    private String message = "maxitemsperrequest shouldn't be zero";
    @Test
    public void testBundleName() {
        final String actualBundleName = AbilityDelegatorRegistry.getArguments().getTestBundleName();
        assertEquals("com.github.pwittchen.infinitescroll.app", actualBundleName);
    }

}