package io.iohk.atala.prism.app.ui.main.settings;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dagger.android.support.DaggerDialogFragment;
import io.iohk.atala.prism.app.neo.common.extensions.FragmentExtensionsKt;
import io.iohk.cvp.R;

public class DeleteAllConnectionsDialogFragment extends DaggerDialogFragment {

    private static final float DELETE_ALL_CONNECTIONS_DIALOG_WIDTH = 350;
    private static final float DELETE_ALL_CONNECTIONS_DIALOG_HEIGHT = 300;

    static final String REQUEST_DELETE_DATA = "REQUEST_DELETE_DATA";

    @OnClick(R.id.cancel_button)
    void cancel() {
        this.dismiss();
    }

    @OnClick(R.id.reset_button)
    void resetData() {
        Bundle result = new Bundle();
        result.putInt(FragmentExtensionsKt.getKEY_RESULT(this), Activity.RESULT_OK);
        getParentFragmentManager().setFragmentResult(REQUEST_DELETE_DATA, result);
        this.dismiss();
    }

    @Override
    public void onResume() {
        super.onResume();
        Window window = getDialog().getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        float factor = getContext().getResources().getDisplayMetrics().density;
        params.width = (int) (DELETE_ALL_CONNECTIONS_DIALOG_WIDTH * factor);
        params.height = (int) (DELETE_ALL_CONNECTIONS_DIALOG_HEIGHT * factor);
        window.setAttributes(params);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }
        View view = inflater.inflate(R.layout.fragment_delete_all_connections_dialog, container, false);
        ButterKnife.bind(this, view);
        return view;
    }
}