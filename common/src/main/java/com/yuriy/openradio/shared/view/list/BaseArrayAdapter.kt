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

package com.yuriy.openradio.shared.view.list

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.annotation.LayoutRes

abstract class BaseArrayAdapter<T, VH>(context: Context, private val items: Array<T>) : BaseAdapter() {

    private var inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getView(position: Int, view: View?, viewGroup: ViewGroup?): View {
        var viewClone = view
        val viewHolder: VH

        if (viewClone == null) {
            viewClone = inflater.inflate(setSpinnerItemLayout(), viewGroup, false)
            viewHolder = createViewHolder(viewClone)
            viewClone?.tag = viewHolder
        } else {
            viewHolder = viewClone.tag as VH
        }
        getView(viewHolder, position)

        return viewClone!!
    }

    override fun getItem(position: Int): T {
        return items[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return items.size
    }

    @LayoutRes
    abstract fun setSpinnerItemLayout(): Int

    abstract fun getView(viewHolder: VH, position: Int)

    abstract fun createViewHolder(view: View?): VH
}
