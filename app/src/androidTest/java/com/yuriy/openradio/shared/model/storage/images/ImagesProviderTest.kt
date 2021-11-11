/*
 * Copyright 2021 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.shared.model.storage.images

import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
class ImagesProviderTest {

    /**
     * Note - it requires internet connection.
     */
    @Test
    fun testLoadAndDeleteImage() {
        val id = "test1234"
        val url = "https://mmo.aiircdn.com/460/5fb3bb6151ee4.png"
        val uri = ImagesStore.buildImageUri(id, url)
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val latch = CountDownLatch(1)
        runBlocking {
            CoroutineScope(Dispatchers.IO).launch {
                for (i in 1..500) {
                    val monitor = Object()
                    val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {

                        override fun onChange(selfChange: Boolean, uri: Uri?) {
                            super.onChange(selfChange, uri)
                            synchronized(monitor) {
                                monitor.notifyAll()
                            }
                        }
                    }
                    context.contentResolver.registerContentObserver(uri, true, observer)
                    // First call will trigger download.
                    context.contentResolver.openFile(uri, OPEN_FILE_MODE, null)
                    // Wait for image be downloaded.
                    synchronized(monitor) {
                        monitor.wait()
                    }
                    context.contentResolver.unregisterContentObserver(observer)
                    // Second call will get image downloaded.
                    val result = context.contentResolver.openFile(uri, OPEN_FILE_MODE, null)
                    MatcherAssert.assertThat(result, CoreMatchers.notNullValue())
                    MatcherAssert.assertThat(result!!.fileDescriptor.valid(), CoreMatchers.`is`(true))
                    val numOfDeletedItems = context.contentResolver.delete(uri, Bundle())
                    MatcherAssert.assertThat(numOfDeletedItems, CoreMatchers.`is`(1))
                }
                latch.countDown()
            }
        }
        latch.await()
    }

    companion object {

        private const val OPEN_FILE_MODE = "rw"
    }
}
