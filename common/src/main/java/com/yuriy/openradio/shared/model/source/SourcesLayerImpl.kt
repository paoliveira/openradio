/*
 * Copyright 2022 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.shared.model.source

import android.content.Context
import com.yuriy.openradio.R
import com.yuriy.openradio.shared.model.storage.SourceStorage
import com.yuriy.openradio.shared.utils.AppLogger
import java.lang.ref.WeakReference

class SourcesLayerImpl(context: Context) : SourcesLayer {

    private val mSources: Set<Source>
    private val mPersistence = SourceStorage(WeakReference(context))

    init {
        mSources = constructSources(context)
    }

    override fun getAllSources(): Set<Source> {
        return HashSet(mSources)
    }

    override fun getActiveSource(): Source {
        val idx = mPersistence.getIntValue(KEY_IDX, 0)
        val source = mSources.elementAt(idx)
        AppLogger.d("Get active source $source")
        return source
    }

    override fun setActiveSource(source: Source) {
        AppLogger.d("Set active source $source")
        mPersistence.putIntValue(KEY_IDX, source.idx)
    }

    private fun constructSources(context: Context): Set<Source> {
        val names = context.resources.getStringArray(R.array.sources_names)
        val ids = context.resources.getIntArray(R.array.sources_ids)
        val set = HashSet<Source>(names.size)
        // Names and Ids (and whatever may be associated) are of the same length.
        for ((i, name) in names.withIndex()) {
            set.add(Source(ids[i], name))
        }
        return set
    }

    companion object {

        private const val KEY_IDX = "ActiveSrcIdx"
    }
}
