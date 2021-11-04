/*
 * Copyright 2017 The "Open Radio" Project. Author: Chernyshov Yuriy
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
package com.yuriy.openradio.shared.view.dialog

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.FragmentManager
import com.yuriy.openradio.shared.R
import com.yuriy.openradio.shared.broadcast.LocalAbstractReceiver
import com.yuriy.openradio.shared.broadcast.RSAddValidatedReceiver
import com.yuriy.openradio.shared.broadcast.RSAddValidatedReceiverListener
import com.yuriy.openradio.shared.permission.PermissionChecker
import com.yuriy.openradio.shared.service.LocationService
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.AppUtils
import com.yuriy.openradio.shared.utils.ImageFilePath
import com.yuriy.openradio.shared.utils.IntentUtils
import com.yuriy.openradio.shared.view.BaseDialogFragment
import com.yuriy.openradio.shared.view.SafeToast.showAnyThread
import com.yuriy.openradio.shared.vo.RadioStationToAdd

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 *
 * Base dialog to use by Edit and Add dialogs.
 */
abstract class BaseAddEditStationDialog : BaseDialogFragment() {

    protected lateinit var mNameEdit: EditText
    protected lateinit var mUrlEdit: EditText
    protected lateinit var mCountriesSpinner: Spinner
    protected lateinit var mGenresSpinner: Spinner
    protected lateinit var mAddToFavCheckView: CheckBox

    /**
     * Text view for Image Url.
     */
    private lateinit var mImageLocalUrlEdit: EditText
    private lateinit var mProgressView: ProgressBar
    private lateinit var mGenresAdapter: ArrayAdapter<CharSequence>
    private lateinit var mCountriesAdapter: ArrayAdapter<String>
    private lateinit var mRsAddValidatedReceiver: LocalAbstractReceiver

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_add_edit_station, container, false)
        val layoutParams = FrameLayout.LayoutParams(
            (AppUtils.getShortestScreenSize(requireActivity()) * 0.8).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        mRsAddValidatedReceiver = RSAddValidatedReceiver(
            object : RSAddValidatedReceiverListener {
                override fun onSuccess(message: String) {
                    mProgressView.visibility = View.INVISIBLE
                    showAnyThread(context, message)
                    dialog!!.dismiss()
                }

                override fun onFailure(reason: String) {
                    mProgressView.visibility = View.INVISIBLE
                    showAnyThread(context, reason)
                }
            }
        )
        mRsAddValidatedReceiver.register(context)
        val root = view.findViewById<LinearLayout>(R.id.add_edit_station_dialog_root)
        root.layoutParams = layoutParams
        val homePageEdit = view.findViewById<EditText>(R.id.add_edit_station_home_page_edit)
        mNameEdit = view.findViewById(R.id.add_edit_station_name_edit)
        mUrlEdit = view.findViewById(R.id.add_edit_station_stream_url_edit)
        mImageLocalUrlEdit = view.findViewById(R.id.add_edit_station_image_url_edit)
        val imageWebUrlEdit = view.findViewById<EditText>(R.id.add_edit_station_web_image_url_edit)
        mProgressView = view.findViewById(R.id.add_edit_station_dialog_progress_bar_view)
        val countries = ArrayList(LocationService.COUNTRY_CODE_TO_NAME.values)
        countries.sort()
        mCountriesSpinner = view.findViewById(R.id.add_edit_station_country_spin)
        // Create an ArrayAdapter using the string array and a default spinner layout
        mCountriesAdapter = ArrayAdapter(
            requireActivity(),
            android.R.layout.simple_spinner_item,
            countries
        )
        // Specify the layout to use when the list of choices appears
        mCountriesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // Apply the mCountriesAdapter to the spinner
        mCountriesSpinner.adapter = mCountriesAdapter
        mGenresSpinner = view.findViewById(R.id.add_station_genre_spin)
        // Create an ArrayAdapter using the string array and a default spinner layout
        mGenresAdapter = ArrayAdapter(
            requireActivity(),
            android.R.layout.simple_spinner_item,
            ArrayList<CharSequence>(AppUtils.predefinedCategories())
        )
        // Specify the layout to use when the list of choices appears
        mGenresAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // Apply the mCountriesAdapter to the spinner
        mGenresSpinner.adapter = mGenresAdapter

        val launcher = IntentUtils.registerForActivityResultIntrl(
            this, ::onActivityResultCallback
        )
        val imageUrlBtn = view.findViewById<Button>(R.id.add_edit_station_image_browse_btn)
        imageUrlBtn.setOnClickListener {
            val galleryIntent = Intent()
            galleryIntent.type = "image/*"
            galleryIntent.action = Intent.ACTION_GET_CONTENT

            // Chooser of filesystem options.
            val chooserIntent = Intent.createChooser(galleryIntent, "Select Image")
            launcher.launch(chooserIntent)
        }
        mAddToFavCheckView = view.findViewById(R.id.add_to_fav_check_view)
        val addToSrvrCheckView = view.findViewById<CheckBox>(R.id.add_to_srvr_check_view)
        addToSrvrCheckView.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            toggleWebImageView(view, isChecked)
        }
        val addOrEditBtn = view.findViewById<Button>(R.id.add_edit_station_dialog_add_btn_view)
        addOrEditBtn.setOnClickListener {
            mProgressView.visibility = View.VISIBLE
            processInputInternal(
                mNameEdit.text.toString(),
                mUrlEdit.text.toString(),
                mImageLocalUrlEdit.text.toString(),
                imageWebUrlEdit.text.toString(),
                homePageEdit.text.toString(),
                mGenresSpinner.selectedItem.toString(),
                mCountriesSpinner.selectedItem.toString(),
                mAddToFavCheckView.isChecked,
                addToSrvrCheckView.isChecked
            )
        }
        val cancelBtn = view.findViewById<Button>(R.id.add_edit_station_dialog_cancel_btn_view)
        cancelBtn.setOnClickListener { dialog!!.dismiss() }
        mProgressView.visibility = View.INVISIBLE
        return view
    }

    override fun onResume() {
        super.onResume()
        val context: Activity? = activity
        if (!PermissionChecker.isExternalStorageGranted(context!!)) {
            PermissionChecker.requestExternalStoragePermission(
                context, requireView().findViewById(R.id.dialog_add_edit_root_layout), 1234
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mProgressView.visibility = View.INVISIBLE
        mRsAddValidatedReceiver.unregister(context)
    }

    private fun onActivityResultCallback(data: Intent?) {
        val selectedImageUri = data?.data
        if (selectedImageUri == null) {
            AppLogger.e("Can not process image path, image uri is null")
            return
        }
        val ctx = context
        if (ctx == null) {
            AppLogger.e("Can not process image path, context is null")
            return
        }
        //MEDIA GALLERY
        val selectedImagePath = ImageFilePath.getPath(ctx, selectedImageUri)
        AppLogger.d("Image Path:$selectedImagePath")
        if (selectedImagePath != null) {
            mImageLocalUrlEdit.setText(selectedImagePath)
        } else {
            showAnyThread(ctx, ctx.getString(R.string.can_not_open_file))
        }
    }

    /**
     * Abstraction to handle action once input is processed.
     *
     * @param radioStationToAdd Data to add as radio station.
     */
    protected abstract fun processInput(radioStationToAdd: RadioStationToAdd)

    /**
     * Return position of country in drop down list.
     *
     * @param country Country of the Radio Station.
     * @return Position of country.
     */
    fun getCountryPosition(country: String?): Int {
        return mCountriesAdapter.getPosition(country)
    }

    /**
     * Return position of genre in drop down list.
     *
     * @param genre Genre of the Radio Station.
     * @return Position of Genre.
     */
    fun getGenrePosition(genre: String?): Int {
        return mGenresAdapter.getPosition(genre)
    }

    /**
     * Processing provided input to perform appropriate actions on the data: add or edit Radio Station.
     *
     * @param name          Name of the Radio Station.
     * @param url           Url of the Stream associated with Radio Station.
     * @param imageLocalUrl Local Url of the Image associated with Radio Station.
     * @param imageWebUrl   Web Url of the Image associated with Radio Station.
     * @param homePage      Web Url of Radio Station's home page.
     * @param genre         Genre of the Radio Station.
     * @param country       Country of the Radio Station.
     * @param addToFav      Whether or not add radio station to favorites.
     * @param addToServer   Whether or not add radio station to the server.
     */
    private fun processInputInternal(
        name: String, url: String, imageLocalUrl: String,
        imageWebUrl: String, homePage: String, genre: String,
        country: String, addToFav: Boolean, addToServer: Boolean
    ) {
        val rsToAdd = RadioStationToAdd(
            name, url, imageLocalUrl, imageWebUrl, homePage, genre, country, addToFav, addToServer
        )
        processInput(rsToAdd)
    }

    private fun toggleWebImageView(view: View?, enabled: Boolean) {
        if (view == null) {
            return
        }
        val label = view.findViewById<TextView>(R.id.add_edit_station_web_image_url_label)
        val edit = view.findViewById<EditText>(R.id.add_edit_station_web_image_url_edit)
        label.isEnabled = enabled
        edit.isEnabled = enabled
    }

    companion object {

        @JvmStatic
        fun findDialog(fragmentManager: FragmentManager?): BaseAddEditStationDialog? {
            if (fragmentManager == null) {
                return null
            }
            var fragment = fragmentManager.findFragmentByTag(AddStationDialog.DIALOG_TAG)
            return if (fragment is AddStationDialog) {
                fragment
            } else {
                fragment = fragmentManager.findFragmentByTag(EditStationDialog.DIALOG_TAG)
                if (fragment is EditStationDialog) {
                    return fragment
                }
                return null
            }
        }
    }
}
