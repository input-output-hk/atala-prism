package io.iohk.cvp.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.crashlytics.android.Crashlytics;

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import butterknife.BindView;
import io.iohk.cvp.R;
import io.iohk.cvp.data.local.db.model.Credential;
import io.iohk.cvp.grpc.AsyncTaskResult;
import io.iohk.cvp.viewmodel.CredentialsViewModel;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.fragments.utils.RootAppBar;
import io.iohk.cvp.views.utils.adapters.CredentialClickListener;
import io.iohk.cvp.views.utils.adapters.NewCredentialsRecyclerViewAdapter;
import io.iohk.prism.protos.ReceivedMessage;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@NoArgsConstructor
public class MyCredentialsFragment extends CvpFragment<CredentialsViewModel>  implements CredentialClickListener {

    @BindView(R.id.credentials_list)
    RecyclerView credentialsRecyclerView;

    @BindView(R.id.fragment_layout)
    FrameLayout fragmentLayout;

    @BindView(R.id.no_credentials_container)
    LinearLayout noCredentialsContainer;

    @BindView(R.id.loading)
    RelativeLayout loading;

    @Inject
    CredentialDetailFragment credentialFragment;
    @Inject
    ViewModelProvider.Factory factory;
    private MutableLiveData<AsyncTaskResult<List<ReceivedMessage>>> liveData;
    private NewCredentialsRecyclerViewAdapter credentialsAdapter;

    @Override
    protected int getViewId() {
        return R.layout.fragment_home;
    }

    @Override
    protected AppBarConfigurator getAppBarConfigurator() {
        return new RootAppBar(R.string.home_title);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = super.onCreateView(inflater, container, savedInstanceState);
        credentialsAdapter = new NewCredentialsRecyclerViewAdapter(R.layout.row_credential, this, false, getContext());
        credentialsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        credentialsRecyclerView.setAdapter(credentialsAdapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        listMyCredentials();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initObservers();
    }

    private void initObservers() {
        viewModel.getCredentialLiveData().observe(getViewLifecycleOwner(), result -> {
            try {
                if (result.getError() != null) {
                    getNavigator().showPopUp(getFragmentManager(), getResources().getString(
                            R.string.server_error_message));
                    return;
                }
                List<Credential> credentialList = result.getResult();
                if(credentialList != null) {
                    credentialsAdapter.addMesseges(credentialList);
                    if (credentialList.isEmpty()) {
                        credentialsRecyclerView.setVisibility(View.GONE);
                        noCredentialsContainer.setVisibility(View.VISIBLE);
                    }
                }
            } catch (Exception e) {
                Crashlytics.logException(e);
            } finally {
                loading.setVisibility(View.GONE);
            }
        });
    }

    public void listMyCredentials() {
        getViewModel().getAllCredentials();
    }

    @Override
    public CredentialsViewModel getViewModel() {
        CredentialsViewModel viewModel = ViewModelProviders.of(this, factory)
                .get(CredentialsViewModel.class);
        viewModel.setContext(getContext());
        return viewModel;
    }

    @Override
    public void onCredentialClickListener(Boolean isNew, Credential credential) {
        credentialFragment.setCredential(credential);
        credentialFragment.setCredentialIsNew(isNew);

        navigator.showFragmentOnTop(
                Objects.requireNonNull(getActivity()).getSupportFragmentManager(), credentialFragment);
    }
}