package com.yuriy.openradio.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.yuriy.openradio.R;
import com.yuriy.openradio.utils.IntentsHelper;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 5/3/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */

/**
 * {@link UseLocationDialog} is a Dialog to inform user that there is a profit of using Location
 * service of the device.
 */
public class UseLocationDialog extends BaseDialogFragment {

    /**
     * Tag string to use in the debugging messages.
     */
    private static final String LOG_TAG = UseLocationDialog.class.getSimpleName();

    /**
     * The tag for this fragment, as per {@link android.app.FragmentTransaction#add}.
     */
    public final static String DIALOG_TAG = LOG_TAG + "Tag";

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final View view = getInflater().inflate(R.layout.use_location_dialog,
                (ViewGroup) getActivity().findViewById(R.id.use_location_dialog_root));

        final Button enableLocationServiceBtn
                = (Button) view.findViewById(R.id.uld_enable_location_service_btn_view);
        enableLocationServiceBtn.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        startActivityForResult(
                                IntentsHelper.makeOpenLocationSettingsIntent(),
                                IntentsHelper.REQUEST_CODE_LOCATION_SETTINGS);
                    }
                }
        );

        final AlertDialog.Builder builder = createAlertDialogWithOkButton(getActivity());
        builder.setTitle(getActivity().getString(R.string.location_service));
        builder.setView(view);
        return builder.create();
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IntentsHelper.REQUEST_CODE_LOCATION_SETTINGS) {
            ((MainActivity) getActivity()).processLocationCallback();
        }
    }
}
