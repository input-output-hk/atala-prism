package io.iohk.atala.prism.app.ui.main.credentials;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import javax.inject.Inject;

import io.iohk.atala.prism.app.ui.CvpFragment;
import io.iohk.cvp.R;
import io.iohk.atala.prism.app.data.local.db.model.Credential;
import io.iohk.atala.prism.app.utils.IntentDataConstants;
import io.iohk.atala.prism.app.ui.utils.AppBarConfigurator;
import io.iohk.atala.prism.app.ui.utils.StackedAppBar;
import io.iohk.cvp.databinding.FragmentCredentialDetailBinding;
import lombok.NoArgsConstructor;
import lombok.Setter;

// TODO This needs its own ViewModel
@Setter
@NoArgsConstructor
public class CredentialDetailFragment extends CvpFragment<CredentialDetailViewModel> {

    @Inject
    ViewModelProvider.Factory factory;

    private static final int REQUEST_DELETE_CREDENTIAL = 22;

    private Credential credential;

    private FragmentCredentialDetailBinding binding;

    @Override
    protected int getViewId() {
        return R.layout.fragment_credential_detail;
    }

    @Override
    protected AppBarConfigurator getAppBarConfigurator() {
        setHasOptionsMenu(true);

        if (credential != null) {
            return new StackedAppBar(CredentialUtil.getNameResource(credential.credentialType));
        }
        return new StackedAppBar(R.string.education);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_credential_history).setVisible(true);
        menu.findItem(R.id.action_share_credential).setVisible(true);
        menu.findItem(R.id.action_delete_credential).setVisible(true);
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
                getDeleteCredentialFragment().show(requireActivity().getSupportFragmentManager(), null);
                return true;
            case R.id.action_credential_history:
                CredentialHistoryFragment fragment = CredentialHistoryFragment.Companion.build(credential.credentialId);
                navigator.showFragmentOnTop(
                        requireActivity().getSupportFragmentManager(), fragment);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private ShareCredentialDialogFragment getShareFragment() {
        ShareCredentialDialogFragment fragment = new ShareCredentialDialogFragment();
        Bundle args = new Bundle();
        args.putString(IntentDataConstants.CREDENTIAL_ID_KEY, credential.credentialId);
        fragment.setArguments(args);
        return fragment;
    }

    private DeleteCredentialDialogFragment getDeleteCredentialFragment() {
        DeleteCredentialDialogFragment dialog = DeleteCredentialDialogFragment.Companion.build(credential.credentialId);
        dialog.setTargetFragment(this, REQUEST_DELETE_CREDENTIAL);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, getViewId(), container, false);
        binding.setLifecycleOwner(this);
        fillData(credential);
        setObservers();
        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getViewModel().setCredentialViewed(credential);
    }

    private void fillData(Credential credential) {
        String credentialHtmlView = CredentialUtil.getHtml(credential);
        String encodedHtml = Base64.encodeToString(credentialHtmlView.getBytes(), Base64.NO_PADDING);
        binding.webView.loadData(encodedHtml, "text/html", "base64");
    }

    private void setObservers() {
        viewModel.getCustomDateFormat().observe(getViewLifecycleOwner(), customFormat -> {
            if (customFormat != null && credential != null) {
                String formattedDate = customFormat.getDateFormat().format(credential.dateReceived);
                binding.setFormattedIssuedDate(getString(R.string.received, formattedDate));
            }
        });
    }

    @Override
    public CredentialDetailViewModel getViewModel() {
        CredentialDetailViewModel viewModel = ViewModelProviders.of(this, factory)
                .get(CredentialDetailViewModel.class);
        return viewModel;
    }

    public void setCredential(Credential credential) {
        this.credential = credential;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_DELETE_CREDENTIAL && resultCode == Activity.RESULT_OK) {
            requireActivity().onBackPressed();
        }
    }
}