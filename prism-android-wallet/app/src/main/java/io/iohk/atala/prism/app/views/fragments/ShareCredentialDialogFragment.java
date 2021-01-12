package io.iohk.atala.prism.app.views.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import javax.inject.Inject;

import dagger.android.support.DaggerDialogFragment;
import io.iohk.cvp.R;
import io.iohk.atala.prism.app.data.local.db.model.Contact;
import io.iohk.cvp.databinding.NeoDialogShareCredentialBinding;
import io.iohk.atala.prism.app.neo.common.OnSelectItem;
import io.iohk.atala.prism.app.neo.common.extensions.FragmentActivityExtensionsKt;
import io.iohk.atala.prism.app.neo.common.model.CheckableData;
import io.iohk.atala.prism.app.viewmodel.ShareCredentialDialogViewModel;
import io.iohk.atala.prism.app.viewmodel.ShareCredentialDialogViewModelFactory;
import io.iohk.atala.prism.app.views.utils.adapters.CheckableContactRecyclerViewAdapter;
import io.iohk.atala.prism.app.views.utils.dialogs.SuccessDialog;
import lombok.NoArgsConstructor;

import static io.iohk.atala.prism.app.utils.IntentDataConstants.CREDENTIAL_ID_KEY;

@NoArgsConstructor
public class ShareCredentialDialogFragment extends DaggerDialogFragment implements OnSelectItem<CheckableData<Contact>> {

    @Inject
    ShareCredentialDialogViewModelFactory factory;

    private ShareCredentialDialogViewModel viewModel;

    private CheckableContactRecyclerViewAdapter adapter;

    private NeoDialogShareCredentialBinding binding;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new BottomSheetDialog(getContext(), getTheme());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = getViewModel();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.neo_dialog_share_credential, container, false);
        binding.setLifecycleOwner(this);
        binding.setViewModel(viewModel);
        configureRecyclerView();
        try {
            String credentialId = getArguments().getString(CREDENTIAL_ID_KEY);
            viewModel.fetchData(credentialId);
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initObservers();
    }

    private void initObservers() {
        // Update the contact list
        viewModel.getFilteredContacts().observe(getViewLifecycleOwner(), checkableContacts -> {
            adapter.clear();
            adapter.addAll(checkableContacts);
            adapter.notifyDataSetChanged();
        });

        // Handle ViewModel errors
        viewModel.getError().observe(getViewLifecycleOwner(), errorEvent -> {
            ShareCredentialDialogViewModel.ErrorType errorType = errorEvent.getContentIfNotHandled();
            if (errorType != null) {
                switch (errorType) {
                    case CantLoadContactsError:
                        FragmentActivityExtensionsKt.showErrorDialog(requireActivity(), R.string.error_loading_contacts);
                        dismiss();
                        break;
                    case CantShareCredentialError:
                        FragmentActivityExtensionsKt.showErrorDialog(requireActivity(), R.string.server_error_message);
                        break;
                }
            }
        });

        // Handles when credential is being sent (this locks the UI with a loading dialog)
        viewModel.getCredentialSharingIsInProcess().observe(getViewLifecycleOwner(), isInSharingProcess -> {
            if (isInSharingProcess) {
                FragmentActivityExtensionsKt.showBlockUILoading(requireActivity());
            } else {
                FragmentActivityExtensionsKt.hideBlockUILoading(requireActivity());
            }
        });

        // Handles when the credential has been sent successfully
        viewModel.getCredentialHasBeenShared().observe(getViewLifecycleOwner(), completeEvent -> {
            if (completeEvent.getContentIfNotHandled() != null) {
                SuccessDialog.newInstance(this, R.string.server_share_successfully).show(requireActivity().getSupportFragmentManager(), "dialog");
                dismiss();
            }
        });
    }

    private ShareCredentialDialogViewModel getViewModel() {
        ShareCredentialDialogViewModel viewModel = ViewModelProviders.of(this, factory)
                .get(ShareCredentialDialogViewModel.class);
        return viewModel;
    }

    private void configureRecyclerView() {
        adapter = new CheckableContactRecyclerViewAdapter(this);
        binding.verifierRecyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        binding.verifierRecyclerView.setAdapter(adapter);
    }

    // Handles when a contact is selected or deselected
    @Override
    public void onSelect(CheckableData<Contact> contactCheckableData) {
        viewModel.selectDeselectContact(contactCheckableData);
    }
}