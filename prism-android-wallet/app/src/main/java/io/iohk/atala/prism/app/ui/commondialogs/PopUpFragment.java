package io.iohk.atala.prism.app.ui.commondialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.iohk.cvp.R;
import io.iohk.atala.prism.app.utils.IntentDataConstants;
import io.iohk.atala.prism.app.ui.Navigator;

public class PopUpFragment extends DialogFragment {

    @BindView(R.id.error_description_text_view)
    TextView errorMessageTextView;

    @BindView(R.id.error)
    TextView errorTextView;

    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        FragmentActivity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View convertView = activity.getLayoutInflater().
                inflate(R.layout.error_popup, null, false);
        builder.setView(convertView);

        ButterKnife.bind(this, convertView);

        Bundle params = getArguments();

        if (params.getBoolean(IntentDataConstants.POP_UP_IS_ERROR)) {
            errorMessageTextView.setText(params.getString(IntentDataConstants.ERROR_MSG_DESCRIPTION_KEY));
            errorMessageTextView.setClickable(false);
            errorMessageTextView.setFocusable(false);
            errorTextView.setVisibility(View.VISIBLE);
        }

        return builder.create();
    }

    @OnClick(R.id.error_description_text_view)
    public void onSettingsLinkClick() {
        Navigator navigator = new Navigator();
        navigator.showAppPermissionSettings(getActivity());
    }

    @OnClick(R.id.btn_ok)
    public void onOkClick() {
        this.dismiss();
    }
}
