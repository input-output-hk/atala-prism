package io.iohk.atala.prism.app.views.fragments;

import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import javax.inject.Inject;

import butterknife.BindView;
import io.iohk.cvp.R;
import io.iohk.atala.prism.app.data.local.db.model.Credential;
import io.iohk.atala.prism.app.utils.CredentialParse;
import io.iohk.atala.prism.app.utils.IntentDataConstants;
import io.iohk.atala.prism.app.viewmodel.CredentialsViewModel;
import io.iohk.atala.prism.app.viewmodel.dtos.CredentialDto;
import io.iohk.atala.prism.app.views.fragments.utils.AppBarConfigurator;
import io.iohk.atala.prism.app.views.fragments.utils.StackedAppBar;
import lombok.NoArgsConstructor;
import lombok.Setter;

// TODO This needs its own ViewModel
@Setter
@NoArgsConstructor
public class CredentialDetailFragment extends CvpFragment<CredentialsViewModel> implements DeleteCredentialDialogFragment.OnDeleteCredential {

    @Inject
    ViewModelProvider.Factory factory;

    private static final int DELETE_ALL_CONNECTIONS_REQUEST_CODE = 22;

    private Credential credential;

    private CredentialDto credentialDto;

    private Boolean credentialIsNew;

    @BindView(R.id.web_view)
    WebView webView;

    @Override
    protected int getViewId() {
        return R.layout.fragment_credential_detail;
    }

    @Override
    protected AppBarConfigurator getAppBarConfigurator() {
        setHasOptionsMenu(true);

        if (credentialDto != null) {
            return new StackedAppBar(credentialDto.getTitle());
        }
        return new StackedAppBar(R.string.education);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem shareCredentialMenuItem;
        if (!credentialIsNew) {
            shareCredentialMenuItem = menu.findItem(R.id.action_share_credential);
            shareCredentialMenuItem.setVisible(true);
        }
        MenuItem deleteCredentialMenuItem = menu.findItem(R.id.action_delete_credential);
        deleteCredentialMenuItem.setVisible(true);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_share_credential:
                getShareFragment().show(requireActivity().getSupportFragmentManager(), null);
                return true;
            case android.R.id.home:
                requireActivity().onBackPressed();
                return true;
            case R.id.action_delete_credential:
                navigator.showDialogFragment(
                        requireActivity().getSupportFragmentManager(), getDeleteCredentialFragment(), null);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private ShareCredentialDialogFragment getShareFragment() {
        ShareCredentialDialogFragment fragment = new ShareCredentialDialogFragment();
        Bundle args = new Bundle();
        args.putString(IntentDataConstants.CREDENTIAL_DATA_KEY, credential.credentialDocument);
        args.putString(IntentDataConstants.CREDENTIAL_TYPE_KEY, credential.credentialType);
        args.putByteArray(IntentDataConstants.CREDENTIAL_ENCODED_KEY, credential.credentialEncoded.toByteArray());
        fragment.setArguments(args);

        return fragment;
    }

    private DeleteCredentialDialogFragment getDeleteCredentialFragment() {
        DeleteCredentialDialogFragment fragment = new DeleteCredentialDialogFragment();
        fragment.setTargetFragment(this, DELETE_ALL_CONNECTIONS_REQUEST_CODE);
        Bundle args = new Bundle();
        args.putString(IntentDataConstants.CREDENTIAL_TYPE_KEY, credential.credentialType);
        args.putString(IntentDataConstants.CREDENTIAL_ID_KEY, credential.credentialId);
        args.putString(IntentDataConstants.CREDENTIAL_DATA_KEY, credential.credentialDocument);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        fillData(credential);
        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getViewModel().setCredentialViewed(credential);
    }

    private void fillData(Credential credential) {
        credentialDto = CredentialParse.parse(credential.credentialType, credential.credentialDocument);
        String credentialHtmlView = credentialDto.getView().getHtml();
        String encodedHtml = Base64.encodeToString(credentialHtmlView.getBytes(), Base64.NO_PADDING);
        webView.loadData(encodedHtml, "text/html", "base64");
    }

    @Override
    public CredentialsViewModel getViewModel() {
        CredentialsViewModel viewModel = ViewModelProviders.of(this, factory)
                .get(CredentialsViewModel.class);
        return viewModel;
    }

    @Override
    public void credentialDeleted() {
        getFragmentManager().popBackStack();
    }

    public void setCredential(Credential credential) {
        this.credential = credential;
    }

    public void setCredentialIsNew(Boolean credentialIsNew) {
        this.credentialIsNew = credentialIsNew;
    }
}