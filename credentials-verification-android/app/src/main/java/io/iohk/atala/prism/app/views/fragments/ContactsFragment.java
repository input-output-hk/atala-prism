package io.iohk.atala.prism.app.views.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;

import javax.inject.Inject;

import io.iohk.atala.prism.app.neo.common.OnSelectItem;
import io.iohk.cvp.R;
import io.iohk.atala.prism.app.data.local.db.model.Contact;
import io.iohk.cvp.databinding.FragmentContactsBinding;
import io.iohk.atala.prism.app.utils.ActivitiesRequestCodes;
import io.iohk.atala.prism.app.utils.IntentDataConstants;
import io.iohk.atala.prism.app.viewmodel.ContactsViewModel;
import io.iohk.atala.prism.app.viewmodel.ContactsViewModelFactory;
import io.iohk.atala.prism.app.views.fragments.utils.ActionBarUtils;
import io.iohk.atala.prism.app.views.fragments.utils.AppBarConfigurator;
import io.iohk.atala.prism.app.views.fragments.utils.RootAppBar;
import io.iohk.atala.prism.app.views.utils.adapters.ContactsRecyclerViewAdapter;

public class ContactsFragment extends CvpFragment<ContactsViewModel> implements OnSelectItem<Contact> {

    @Inject
    ContactsViewModelFactory factory;

    private ContactsRecyclerViewAdapter adapter;

    private FragmentContactsBinding binding;

    private ContactsViewModel viewModel;


    @Override
    public ContactsViewModel getViewModel() {
        ContactsViewModel viewModel = ViewModelProviders.of(this, factory)
                .get(ContactsViewModel.class);
        return viewModel;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        ActionBarUtils.setupMenu(menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        viewModel = getViewModel();
        binding = DataBindingUtil.inflate(inflater, getViewId(), container, false);
        binding.setLifecycleOwner(this);
        binding.setViewModel(viewModel);
        binding.setScanQrBtnOnClick(v -> scanQr());
        configureRecyclerView();
        initObservers();
        return binding.getRoot();
    }

    private void configureRecyclerView() {
        adapter = new ContactsRecyclerViewAdapter(this);
        binding.contactsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.contactsRecyclerView.setAdapter(adapter);
    }

    private void initObservers() {
        viewModel.getContacts().observe(getViewLifecycleOwner(), contacts -> {
            adapter.clear();
            adapter.addAll(contacts);
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO This has to be redone implementing the navigation library
        ActionBarUtils.menuItemClicked(navigator, item, this);
        // If we got here, the user's action was not recognized.
        // Invoke the superclass to handle it.
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected int getViewId() {
        return R.layout.fragment_contacts;
    }

    @Override
    protected AppBarConfigurator getAppBarConfigurator() {
        return new RootAppBar(R.string.contacts);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ActivitiesRequestCodes.QR_SCANNER_REQUEST_ACTIVITY) {
            if (resultCode == Activity.RESULT_OK && data.hasExtra(IntentDataConstants.QR_RESULT)) {
                final String token = data.getStringExtra(IntentDataConstants.QR_RESULT);
                // TODO momentary solution, the use of "safeArgs" will be implemented
                AcceptConnectionDialogFragment dialog = AcceptConnectionDialogFragment.Companion.build(token);
                dialog.show(requireActivity().getSupportFragmentManager(), null);
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onSelect(Contact item) {
        ContactDetailFragment fragment = ContactDetailFragment.Companion.build(item.id);
        navigator.showFragmentOnTop(requireActivity().getSupportFragmentManager(), fragment);
    }
}