package io.iohk.cvp.views.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.SupportErrorDialogFragment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import io.iohk.cvp.R;
import io.iohk.cvp.core.exception.ErrorCode;
import io.iohk.cvp.core.exception.SharedPrefencesDataNotFoundException;
import io.iohk.cvp.grpc.AsyncTaskResult;
import io.iohk.cvp.utils.CredentialParse;
import io.iohk.cvp.viewmodel.ConnectionsListablesViewModel;
import io.iohk.cvp.viewmodel.dtos.ConnectionListable;
import io.iohk.cvp.viewmodel.dtos.CredentialDto;
import io.iohk.cvp.views.Preferences;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.utils.adapters.ShareCredentialRecyclerViewAdapter;
import io.iohk.cvp.views.utils.dialogs.SuccessDialog;
import io.iohk.prism.protos.ConnectionInfo;
import io.iohk.prism.protos.Credential;
import io.iohk.prism.protos.ParticipantInfo;
import lombok.NoArgsConstructor;

import static io.iohk.cvp.utils.IntentDataConstants.CREDENTIAL_DATA_KEY;

@NoArgsConstructor
public class ShareCredentialDialogFragment extends CvpFragment<ConnectionsListablesViewModel> {

    private ViewModelProvider.Factory factory;

    @BindView(R.id.background)
    public ConstraintLayout background;

    @BindView(R.id.verifier_recycler_view)
    public RecyclerView recyclerView;

    @BindView(R.id.share_button)
    public Button button;

    private ShareCredentialRecyclerViewAdapter adapter;

    private Set<String> selectedVerifiers = new HashSet<>();

    private LiveData<AsyncTaskResult<Boolean>> liveData;

    private CredentialDto credential;


    @Inject
    ShareCredentialDialogFragment(ViewModelProvider.Factory factory) {
        this.factory = factory;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = super.onCreateView(inflater, container, savedInstanceState);
        try {
            adapter = new ShareCredentialRecyclerViewAdapter(this);
            recyclerView.setLayoutManager(
                    new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
            recyclerView.setAdapter(adapter);

            credential = CredentialParse.parse(Credential.parseFrom(getArguments().getByteArray(CREDENTIAL_DATA_KEY)));

            LiveData<AsyncTaskResult<List<ConnectionListable>>> liveData = viewModel
                    .getConnections(this.getUserIds());

            if (!liveData.hasActiveObservers()) {
                liveData.observe(this, response -> {

                    if (response.getError() != null) {
                        getNavigator().showPopUp(getFragmentManager(), getResources().getString(
                                R.string.server_error_message));
                        return;
                    }

                    List<ConnectionListable> connections = new ArrayList<>();
                    connections.addAll(response.getResult().stream()
                            .filter(conn -> {
                                    return !conn.getDid().substring(conn.getDid().lastIndexOf(":"))
                                            .equals(credential.getIssuer().getId().substring(credential.getIssuer().getId().lastIndexOf(":")));
                            }).collect(Collectors.toList()));

                    adapter.addConnections(connections);
                    adapter.notifyDataSetChanged();
                });
            }
        }catch (Exception e){
            Crashlytics.logException(e);
        }
        return view;
    }

    @OnClick(R.id.background)
    void onBackgroundClick() {
        Objects.requireNonNull(getActivity()).onBackPressed();
    }

    @Override
    public ConnectionsListablesViewModel getViewModel() {
        ConnectionsListablesViewModel viewModel = ViewModelProviders.of(this, factory)
                .get(ConnectionsListablesViewModel.class);
        viewModel.setContext(getContext());
        return viewModel;
    }

    @Override
    protected AppBarConfigurator getAppBarConfigurator() {
        return null;
    }

    @Override
    protected int getViewId() {
        return R.layout.component_share_credential_dialog;
    }

    public void updateButtonState() {
        button.setEnabled(adapter.areConnectionsSelected());
    }

    @OnClick(R.id.share_button)
    public void onShareClick() {
        Preferences prefs = new Preferences(getContext());
        selectedVerifiers.forEach(connectionId -> {
            try {
                String userId = prefs.getUserIdByConnection(connectionId).orElseThrow(() ->
                        new SharedPrefencesDataNotFoundException(
                                "Couldn't find user id for connection id " + connectionId,
                                ErrorCode.USER_ID_NOT_FOUND));

                liveData = viewModel.sendMessage(userId, connectionId,
                        Credential.parseFrom(getArguments().getByteArray(CREDENTIAL_DATA_KEY)).toByteString());

                if (!liveData.hasActiveObservers()) {
                    liveData.observe(this, response -> {
                        FragmentManager fm = getFragmentManager();
                        if (response.getError() != null) {
                            SupportErrorDialogFragment.newInstance(new Dialog(getContext()))
                                    .show(fm, "");
                            getNavigator().showPopUp(getFragmentManager(), getResources().getString(
                                    R.string.server_error_message));
                            return;
                        } else {
                            SuccessDialog.newInstance(this, R.string.server_share_successfully)
                                    .show(getFragmentManager(), "dialog");
                        }
                    });
                }

            } catch (Exception e) {
                Crashlytics.logException(e);
            }
        });
        onBackgroundClick();
    }

    public void updateSelectedVerifiers(String connectionId, Boolean isSelected) {
        if (isSelected) {
            selectedVerifiers.add(connectionId);
        } else {
            selectedVerifiers.remove(connectionId);
        }
    }
}