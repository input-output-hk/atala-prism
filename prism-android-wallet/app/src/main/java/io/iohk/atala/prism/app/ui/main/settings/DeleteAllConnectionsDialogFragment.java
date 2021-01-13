package io.iohk.atala.prism.app.ui.main.settings;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import butterknife.OnClick;
import io.iohk.atala.prism.app.ui.CvpDialogFragment;
import io.iohk.cvp.R;

public class DeleteAllConnectionsDialogFragment extends CvpDialogFragment {

    private static final float DELETE_ALL_CONNECTIONS_DIALOG_WIDTH = 350;
    private static final float DELETE_ALL_CONNECTIONS_DIALOG_HEIGHT = 300;

    public DeleteAllConnectionsDialogFragment() {
        // Required empty public constructor
    }

    public static DeleteAllConnectionsDialogFragment newInstance() {
        DeleteAllConnectionsDialogFragment fragment = new DeleteAllConnectionsDialogFragment();
        return fragment;
    }

    @OnClick(R.id.cancel_button)
    void cancel() {
        this.dismiss();
    }

    @OnClick(R.id.reset_button)
    void resetData() {
        Fragment targetFragment = getTargetFragment();
        if (targetFragment != null) {
            targetFragment.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, null);
        }
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
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected int getViewId() {
        return R.layout.fragment_delete_all_connections_dialog;
    }

    @Override
    public ViewModel getViewModel() {
        return null;
    }
}