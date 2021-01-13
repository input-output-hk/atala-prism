package io.iohk.atala.prism.app.ui.commondialogs;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;

import java.util.Objects;

import butterknife.BindView;
import butterknife.OnClick;
import io.iohk.atala.prism.app.ui.CvpDialogFragment;
import io.iohk.cvp.R;
import io.iohk.atala.prism.app.utils.IntentDataConstants;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AddQrCodeDialogFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AddQrCodeDialogFragment extends CvpDialogFragment {

    public AddQrCodeDialogFragment() {
        // Required empty public constructor
    }

    @BindView(R.id.body)
    EditText body;

    public static AddQrCodeDialogFragment newInstance() {
        return new AddQrCodeDialogFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public ViewModel getViewModel() {
        return null;
    }

    @Override
    protected int getViewId() {
        return R.layout.fragment_add_qr_code_dialog;
    }

    @OnClick(R.id.confirm_button)
    public void onClickConfirmButton() {
        Intent intent = new Intent().putExtra(IntentDataConstants.QR_RESULT, body.getText().toString());
        Objects.requireNonNull(getTargetFragment()).onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
        dismiss();
    }

    @OnClick(R.id.cancel_button)
    public void onClickCancelButton() {
        this.dismiss();
    }
}
