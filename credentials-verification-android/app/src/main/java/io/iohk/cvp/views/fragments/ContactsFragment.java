package io.iohk.cvp.views.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import io.iohk.cvp.R;
import io.iohk.cvp.data.local.db.model.Contact;
import io.iohk.cvp.grpc.AsyncTaskResult;
import io.iohk.cvp.utils.ActivityUtils;
import io.iohk.cvp.viewmodel.ConnectionsActivityViewModel;
import io.iohk.cvp.viewmodel.dtos.CredentialsToShare;
import io.iohk.cvp.views.activities.MainActivity;
import io.iohk.cvp.views.fragments.utils.ActionBarUtils;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.fragments.utils.RootAppBar;
import io.iohk.cvp.views.utils.adapters.ConnectionTabsAdapter;
import io.iohk.cvp.views.utils.adapters.ContactsRecyclerViewAdapter;
import lombok.Setter;

import static io.iohk.cvp.views.fragments.ShareProofRequestDialogFragment.SHARE_PROOF_REQUEST_DIALOG;

@Setter
public class ContactsFragment extends CvpFragment<ConnectionsActivityViewModel> {

    @BindView(R.id.loading)
    RelativeLayout loading;

    @Inject
    ViewModelProvider.Factory factory;

    @BindView(R.id.connections_list_tabs)
    TabLayout tabs;

    @BindView(R.id.connections_list_view_pager)
    ViewPager viewPager;

    @Inject
    ConnectionsListFragment connectionsListFragment;
    private CvpDialogFragment dialogFragment;


    @Inject
    public ContactsFragment() {
    }

    @Override
    public ConnectionsActivityViewModel getViewModel() {
        ConnectionsActivityViewModel viewModel = ViewModelProviders.of(this, factory)
                .get(ConnectionsActivityViewModel.class);
        viewModel.setContext(getContext());
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
        View view = super.onCreateView(inflater, container, savedInstanceState);

        connectionsListFragment.setAdapter(new ContactsRecyclerViewAdapter());

        ConnectionTabsAdapter adapter = new ConnectionTabsAdapter(
                getChildFragmentManager(), 1, connectionsListFragment);

        viewPager.setAdapter(adapter);

        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabs));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        viewPager.setCurrentItem(0);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        connectionsListFragment.clearConnecitons();
        getViewModel().getAllMessages();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        registerTokenInfoObserver();
        initObservers();
    }

    private void initObservers() {

        MutableLiveData<AsyncTaskResult<List<Contact>>> contactsLiveData = getViewModel().getContactsLiveData();
        contactsLiveData.observe(getViewLifecycleOwner(), connectionListResponse -> {
            if (connectionListResponse.getError() != null) {
                hideLoading();
                this.showGenericError();
                return;
            }
            List<Contact> contactsList = connectionListResponse.getResult();
            if (contactsList != null)
                connectionsListFragment.addConnections(contactsList);
        });
        MutableLiveData<AsyncTaskResult<List<CredentialsToShare>>> credentialsToShareLiveData = getViewModel().getCredentialsToShareLiveData();
        credentialsToShareLiveData.observe(getViewLifecycleOwner(), connectionListResponse -> {
            hideLoading();
            if (connectionListResponse.getError() != null) {
                this.showGenericError();
                return;
            }
            List<CredentialsToShare> connectionList = connectionListResponse.getResult();

            if (connectionList != null && !connectionList.isEmpty()) {
                dialogFragment = ShareProofRequestDialogFragment.newInstance(connectionList.get(0));
                getNavigator().showDialogFragment(getFragmentManager(), dialogFragment, SHARE_PROOF_REQUEST_DIALOG);
                viewModel.clearProofRequestToShow();
            }
        });
    }

    private void registerTokenInfoObserver() {
        ActivityUtils.registerObserver((MainActivity) getActivity(),
                viewModel);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        ActionBarUtils.menuItemClicked(navigator, item, this);
        // If we got here, the user's action was not recognized.
        // Invoke the superclass to handle it.
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected int getViewId() {
        return R.layout.fragment_connections;
    }

    @Override
    protected AppBarConfigurator getAppBarConfigurator() {
        return new RootAppBar(R.string.contacts);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ActivityUtils.onQrcodeResult(requestCode, resultCode, viewModel, data);
        if (resultCode == Activity.RESULT_OK) {
            showLoading();
        }
    }
}