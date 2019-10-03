package io.iohk.cvp.views.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.iohk.cvp.R;
import io.iohk.cvp.views.Navigator;

public class PopUpFragment extends DialogFragment {

  @NonNull
  @Override
  public Dialog onCreateDialog(final Bundle savedInstanceState) {
    FragmentActivity activity = getActivity();
    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
    View convertView = activity.getLayoutInflater().
        inflate(R.layout.permission_denied_popup, null, false);
    builder.setView(convertView);

    ButterKnife.bind(this, convertView);

    return builder.create();
  }

  @OnClick(R.id.permissionDeniedText)
  public void onSettingsLinkClick() {
    Navigator navigator = new Navigator();
    navigator.showAppPermissionSettings(getActivity());
  }

}
