/*
 * Copyright 2016 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.view;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.yuriy.openradio.R;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class FileDialog extends ListActivity {

    /**
     *
     */
    private static final String ITEM_KEY = "key";

    /**
     *
     */
    private static final String ITEM_IMAGE = "image";

    /**
     *
     */
    private static final String ROOT_SYMBOL = "/";

    /**
     *
     */
    private static final String RESULT_PATH = "RESULT_PATH";

    private final List<String> mPath = new ArrayList<>();
    private TextView mPathView;
    private Button mSelectButton;
    private InputMethodManager mInputManager;
    private String mParentPath;
    private String mCurrentPath = ROOT_SYMBOL;
    private final String[] formatFilter = new String[]{".jpeg", ".jpg", ".png", ".bmp"};
    private File mSelectedFile;
    private final Map<String, Integer> mLastPositions = new HashMap<>();

    /**
     * @param context
     * @return
     */
    public static Intent makeIntentToOpenFile(final Context context) {
        return new Intent(context, FileDialog.class);
    }

    /**
     *
     * @param intent
     * @return
     */
    public static String getFilePath(final Intent intent) {
        if (intent == null) {
            return null;
        }
        return intent.getStringExtra(FileDialog.RESULT_PATH);
    }

    /**
     *
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        setResult(RESULT_CANCELED, intent);

        setContentView(R.layout.file_dialog_main);
        mPathView = (TextView) findViewById(R.id.path);

        mInputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        mSelectButton = (Button) findViewById(R.id.file_dialog_select_btn);
        mSelectButton.setEnabled(false);
        mSelectButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(final View view) {
                if (mSelectedFile == null) {
                    return;
                }
                intent.putExtra(RESULT_PATH, mSelectedFile.getPath());
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        getDir("/sdcard");
    }

    private void getDir(final String dirPath) {

        final boolean useAutoSelection = dirPath.length() < mCurrentPath.length();
        final Integer position = mLastPositions.get(mParentPath);

        getDirImpl(dirPath);

        if (position != null && useAutoSelection) {
            getListView().setSelection(position);
        }
    }

    /**
     *
     */
    private void getDirImpl(final String dirPath) {

        mCurrentPath = dirPath;

        mPath.clear();
        final List<Map<String, Object>> list = new ArrayList<>();

        File currentFile = new File(mCurrentPath);
        File[] files = currentFile.listFiles();
        if (files == null) {
            mCurrentPath = ROOT_SYMBOL;
            currentFile = new File(mCurrentPath);
            files = currentFile.listFiles();
        }
        mPathView.setText(getText(R.string.location_label) + ": " + mCurrentPath);

        if (!mCurrentPath.equals(ROOT_SYMBOL)) {
            addItem(ROOT_SYMBOL, R.drawable.folder, list);
            mPath.add(ROOT_SYMBOL);

            addItem("../", R.drawable.folder, list);
            mPath.add(currentFile.getParent());
            mParentPath = currentFile.getParent();
        }

        final TreeMap<String, String> dirsMap = new TreeMap<>();
        final TreeMap<String, String> dirsPathMap = new TreeMap<>();
        final TreeMap<String, String> filesMap = new TreeMap<>();
        final TreeMap<String, String> filesPathMap = new TreeMap<>();
        for (final File file : files) {
            final String name = file.getName();
            if (file.isDirectory()) {
                dirsMap.put(name, name);
                dirsPathMap.put(name, file.getPath());
            } else {
                for (final String filter : formatFilter) {
                    if (name.toLowerCase().endsWith(filter.toLowerCase())) {
                        filesMap.put(name, name);
                        filesPathMap.put(name, file.getPath());
                        break;
                    }
                }
            }
        }
        mPath.addAll(dirsPathMap.tailMap("").values());
        mPath.addAll(filesPathMap.tailMap("").values());

        final SimpleAdapter fileList = new SimpleAdapter(
                this, list, R.layout.file_dialog_row,
                new String[]{ITEM_KEY, ITEM_IMAGE},
                new int[]{R.id.file_dialog_row_text, R.id.file_dialog_row_image}
        );

        for (final String dir : dirsMap.tailMap("").values()) {
            addItem(dir, R.drawable.folder, list);
        }

        for (final String file : filesMap.tailMap("").values()) {
            addItem(file, R.drawable.file, list);
        }

        fileList.notifyDataSetChanged();

        setListAdapter(fileList);
    }

    private static void addItem(final String fileName, final int imageId,
                                final List<Map<String, Object>> list) {
        final Map<String, Object> item = new HashMap<>();
        item.put(ITEM_KEY, fileName);
        item.put(ITEM_IMAGE, imageId);
        list.add(item);
    }

    /**
     *
     */
    @Override
    protected void onListItemClick(final ListView listView, final View view, final int position,
                                   final long id) {

        final File file = new File(mPath.get(position));

        mInputManager.hideSoftInputFromWindow(view.getWindowToken(), 0);

        if (!file.isDirectory()) {
            mSelectedFile = file;
            view.setSelected(true);
            mSelectButton.setEnabled(true);

            return;
        }

        mSelectButton.setEnabled(false);
        if (!file.canRead()) {
            new AlertDialog.Builder(this).setIcon(R.drawable.icon)
                    .setTitle("[" + file.getName() + "] " + getText(R.string.cant_read_folder))
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(final DialogInterface dialog, final int which) {

                        }
                    }).show();
        }

        mLastPositions.put(mCurrentPath, position);
        getDir(mPath.get(position));
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        if (keyCode != KeyEvent.KEYCODE_BACK) {
            return super.onKeyDown(keyCode, event);
        }

        mSelectButton.setEnabled(false);

        if (!mCurrentPath.equals(ROOT_SYMBOL)) {
            getDir(mParentPath);
        } else {
            return super.onKeyDown(keyCode, event);
        }

        return true;
    }
}
