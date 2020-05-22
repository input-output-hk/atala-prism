package io.iohk.cvp.utils;

import android.app.Activity;
import android.content.Intent;

import androidx.fragment.app.FragmentManager;

import io.iohk.cvp.R;
import io.iohk.cvp.viewmodel.ConnectionsActivityViewModel;
import io.iohk.cvp.viewmodel.NewConnectionsViewModel;
import io.iohk.cvp.views.activities.MainActivity;
import io.iohk.cvp.views.fragments.AcceptConnectionDialogFragment;
import io.iohk.cvp.views.fragments.CvpFragment;
import io.iohk.prism.protos.ConnectionInfo;

import java.util.List;
import java.util.Objects;

public class ActivityUtils {

    public static void onQrcodeResult(int requestCode, int resultCode, MainActivity activity,
                                      NewConnectionsViewModel viewModel, Intent data, CvpFragment fragment,
                                      List<ConnectionInfo> issuerConnections) {

        if (requestCode == ActivitiesRequestCodes.QR_SCANNER_REQUEST_ACTIVITY
                && resultCode == Activity.RESULT_OK) {
            String token = data.getStringExtra(IntentDataConstants.QR_RESULT);

            viewModel.getConnectionTokenInfo(token)
                    .observe(fragment, response -> {
                                FragmentManager fragmentManager = Objects.requireNonNull(activity)
                                        .getSupportFragmentManager();
                                if (response.getError() != null) {
                                    fragment.getNavigator().showPopUp(fragmentManager, fragment.getResources().getString(
                                            R.string.server_error_message));
                                    return;
                                }

                                boolean isAcceptedConnection = issuerConnections.stream()
                                        .anyMatch(connection -> connection.getParticipantInfo().getIssuer().getDID().equals(response.getResult().getIssuer().getDID()));

                                String title, buttonDescription;
                                if(isAcceptedConnection){
                                    title =  activity.getResources().getString(R.string.connection_re_acept_title);
                                    buttonDescription =  activity.getResources().getString(R.string.connection_re_acept_button);
                                }else{
                                    title = activity.getResources().getString(R.string.connection_acept_title);
                                    buttonDescription = activity.getResources().getString(R.string.connection_acept_button);
                                }
                                
                                fragment.getNavigator().showDialogFragment(fragmentManager,
                                        AcceptConnectionDialogFragment.newInstance(title, buttonDescription, token,
                                                response.getResult()),
                                        "ACCEPT_CONNECTION_DIALOG_FRAGMENT");
                            }
                    );
        }
    }

}
