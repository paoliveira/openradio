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
import android.view.View
import android.widget.TextView
import com.yuriy.openradio.R
import com.yuriy.openradio.shared.vo.Country

class CountriesArrayAdapter(context: Context, private val items: Array<Country>)
    : BaseArrayAdapter<Country, CountriesArrayAdapter.ViewHolderImp>(context, items) {

    override fun setSpinnerItemLayout(): Int {
        return R.layout.countries_spinner_item
    }

    override fun createViewHolder(view: View?): ViewHolderImp {
        return ViewHolderImp(view)
    }

    override fun getView(viewHolder: ViewHolderImp, position: Int) {
        val model = items[position]
        viewHolder.textView?.text = model.name
    }

    class ViewHolderImp(view: View?) {
        val textView: TextView? = view?.findViewById(R.id.countries_spinner_name_view)
    }
}
