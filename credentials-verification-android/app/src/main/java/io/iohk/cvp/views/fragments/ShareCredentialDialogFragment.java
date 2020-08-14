package io.iohk.cvp.views.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.SupportErrorDialogFragment;
import com.google.protobuf.ByteString;

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
import io.iohk.cvp.utils.CredentialParse;
import io.iohk.cvp.viewmodel.ConnectionsListablesViewModel;
import io.iohk.cvp.viewmodel.dtos.ConnectionListable;
import io.iohk.cvp.viewmodel.dtos.CredentialDto;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.interfaces.SelectedVerifiersUpdateable;
import io.iohk.cvp.views.utils.adapters.ShareCredentialRecyclerViewAdapter;
import io.iohk.cvp.views.utils.dialogs.SuccessDialog;
import lombok.NoArgsConstructor;

import static io.iohk.cvp.utils.IntentDataConstants.CREDENTIAL_DATA_KEY;
import static io.iohk.cvp.utils.IntentDataConstants.CREDENTIAL_ENCODED_KEY;
import static io.iohk.cvp.utils.IntentDataConstants.CREDENTIAL_TYPE_KEY;

@NoArgsConstructor
public class ShareCredentialDialogFragment extends CvpFragment<ConnectionsListablesViewModel> implements SelectedVerifiersUpdateable {

    @Inject
    ViewModelProvider.Factory factory;

    @BindView(R.id.background)
    public ConstraintLayout background;

    @BindView(R.id.verifier_recycler_view)
    public RecyclerView recyclerView;

    @BindView(R.id.share_button)
    public Button button;

    private ShareCredentialRecyclerViewAdapter adapter;

    private Set<ConnectionListable> selectedVerifiers = new HashSet<>();

    private CredentialDto credential;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = super.onCreateView(inflater, container, savedInstanceState);
        try {
            adapter = new ShareCredentialRecyclerViewAdapter(this, getResources().getDisplayMetrics().density);
            recyclerView.setLayoutManager(
                    new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
            recyclerView.setAdapter(adapter);

            credential = CredentialParse.parse(getArguments().getString(CREDENTIAL_TYPE_KEY), getArguments().getString(CREDENTIAL_DATA_KEY));
        } catch (Exception e) {
            Crashlytics.logException(e);
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initObservers();
    }

    private void initObservers() {
        viewModel.getMessageSentLiveData().observe(getViewLifecycleOwner(), response -> {
            hideLoading();
            FragmentManager fm = getFragmentManager();
            if (response.getError() != null) {
                SupportErrorDialogFragment.newInstance(new Dialog(getContext()))
                        .show(fm, "");
                getNavigator().showPopUp(getFragmentManager(), getResources().getString(
                        R.string.server_error_message));
            } else {
                if(response.getResult()) {
                    SuccessDialog.newInstance(this, R.string.server_share_successfully)
                            .show(getActivity().getSupportFragmentManager(), "dialog");
                    getFragmentManager().popBackStack();
                    viewModel.shouldNotShowSuccessDialog();
                }
            }
        });

        viewModel.allConnectionsLiveData().observe(getViewLifecycleOwner(), response -> {
            hideLoading();
            if (response.getError() != null) {
                getNavigator().showPopUp(getFragmentManager(), getResources().getString(
                        R.string.server_error_message));
                return;
            }
            if(response.getResult() != null) {
                List<ConnectionListable> connections = new ArrayList<>(response.getResult().stream()
                        .filter(conn -> !conn.did.substring(conn.did.lastIndexOf(":"))
                                .equals(credential.getIssuer().getId().substring(credential.getIssuer().getId().lastIndexOf(":"))))
                        .map(ConnectionListable::new).collect(Collectors.toList()));

                adapter.addConnections(connections);
                adapter.notifyDataSetChanged();
            }
        });
        viewModel.getAllConnections();
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
        showLoading();
        viewModel.sendMessageToMultipleConnections(selectedVerifiers, ByteString.copyFrom(getArguments().getByteArray(CREDENTIAL_ENCODED_KEY)));
    }

    public void updateSelectedVerifiers(ConnectionListable connection, Boolean isSelected) {
        if (isSelected) {
            selectedVerifiers.add(connection);
        } else {
            selectedVerifiers.remove(connection);
        }
    }
}