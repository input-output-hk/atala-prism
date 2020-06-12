package io.iohk.cvp.utils;

import android.app.Activity;
import android.content.Intent;

import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LiveData;

import io.iohk.cvp.R;
import io.iohk.cvp.grpc.AsyncTaskResult;
import io.iohk.cvp.grpc.ParticipantInfoResponse;
import io.iohk.cvp.viewmodel.ConnectionsActivityViewModel;
import io.iohk.cvp.viewmodel.CredentialsViewModel;
import io.iohk.cvp.viewmodel.NewConnectionsViewModel;
import io.iohk.cvp.views.activities.MainActivity;
import io.iohk.cvp.views.fragments.AcceptConnectionDialogFragment;
import io.iohk.cvp.views.fragments.CvpFragment;
import io.iohk.cvp.views.fragments.HomeFragment;
import io.iohk.prism.protos.ConnectionInfo;
import io.iohk.prism.protos.ParticipantInfo;

import java.util.List;
import java.util.Objects;

public class ActivityUtils {

    public static void onQrcodeResult(int requestCode, int resultCode, NewConnectionsViewModel viewModel, Intent data) {
        if (requestCode == ActivitiesRequestCodes.QR_SCANNER_REQUEST_ACTIVITY
                && resultCode == Activity.RESULT_OK) {
            final String token = data.getStringExtra(IntentDataConstants.QR_RESULT);
            viewModel.getConnectionTokenInfo(token);
        }
    }

    public static void registerObserver(MainActivity activity, NewConnectionsViewModel viewModel, List<ConnectionInfo> issuerConnections) {
        LiveData<AsyncTaskResult<ParticipantInfoResponse>> liveData = viewModel.getConnectionTokenInfoLiveData();
        if(!liveData.hasActiveObservers()){
            liveData.observe(activity, response -> {
                        FragmentManager fragmentManager = Objects.requireNonNull(activity)
                                .getSupportFragmentManager();
                        if (response.getError() != null) {
                            activity.getNavigator().showPopUp(fragmentManager, activity.getResources().getString(
                                    R.string.server_error_message));
                            return;
                        }

                        boolean isAcceptedConnection = issuerConnections.stream()
                                .anyMatch(connection -> connection.getParticipantInfo().getIssuer().getDID().equals(response.getResult().getParticipantInfo().getIssuer().getDID()));

                        String title, buttonDescription;
                        if(isAcceptedConnection){
                            title =  activity.getResources().getString(R.string.connection_re_acept_title);
                            buttonDescription =  activity.getResources().getString(R.string.connection_re_acept_button);
                        }else{
                            title = activity.getResources().getString(R.string.connection_acept_title);
                            buttonDescription = activity.getResources().getString(R.string.connection_acept_button);
                        }

                        activity.getNavigator().showDialogFragment(fragmentManager,
                                AcceptConnectionDialogFragment.newInstance(title, buttonDescription, response.getResult().getToken(),
                                        response.getResult().getParticipantInfo()),
                                "ACCEPT_CONNECTION_DIALOG_FRAGMENT");
                    }
            );
        }
    }
}
