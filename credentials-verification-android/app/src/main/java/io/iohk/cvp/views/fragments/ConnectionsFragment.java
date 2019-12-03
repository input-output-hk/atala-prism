package io.iohk.cvp.views.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import com.google.android.material.tabs.TabLayout;
import io.iohk.cvp.R;
import io.iohk.cvp.io.connector.ConnectionInfo;
import io.iohk.cvp.utils.ActivityUtils;
import io.iohk.cvp.viewmodel.ConnectionsActivityViewModel;
import io.iohk.cvp.views.activities.MainActivity;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.fragments.utils.RootAppBar;
import io.iohk.cvp.views.utils.adapters.ConnectionTabsAdapter;
import io.iohk.cvp.views.utils.adapters.EmployersRecyclerViewAdapter;
import io.iohk.cvp.views.utils.adapters.UniversitiesRecyclerViewAdapter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.Setter;

@Setter
public class ConnectionsFragment extends CvpFragment<ConnectionsActivityViewModel> {

  @Inject
  ViewModelProvider.Factory factory;

  @BindView(R.id.connections_list_tabs)
  TabLayout tabs;

  @BindView(R.id.connections_list_view_pager)
  ViewPager viewPager;

  @Inject
  ConnectionsListFragment universitiesListFragment;

  @Inject
  ConnectionsListFragment employersListFragment;

  @Inject
  public ConnectionsFragment() {
  }

  @Override
  public ConnectionsActivityViewModel getViewModel() {
    return ViewModelProviders.of(this, factory).get(ConnectionsActivityViewModel.class);
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    MenuItem paymentHistoryMenuItem;
    paymentHistoryMenuItem = menu.findItem(R.id.action_new_connection);
    paymentHistoryMenuItem.setVisible(true);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);

    universitiesListFragment.setAdapter(new UniversitiesRecyclerViewAdapter());
    employersListFragment.setAdapter(new EmployersRecyclerViewAdapter());

    ConnectionTabsAdapter adapter = new ConnectionTabsAdapter(
        getChildFragmentManager(), tabs.getTabCount(), universitiesListFragment,
        employersListFragment);

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
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    universitiesListFragment.clearConnecitons();
    employersListFragment.clearConnecitons();
    listConnections(this.getUserIds());
  }

  public void listConnections(Set<String> userIds) {
    LiveData<List<ConnectionInfo>> connectionsData = viewModel.getConnections(userIds);

    if (!connectionsData.hasActiveObservers()) {
      connectionsData.observe(this, connections -> {
        List<ConnectionInfo> issuerConnections = connections.stream()
            .filter(conn -> conn.getParticipantInfo().getIssuer() != null).collect(
                Collectors.toList());
        universitiesListFragment.addConnections(issuerConnections);

        // FIXME connections should have something like getVerifier
          /* <ConnectionInfo> verifiersConnections = connections.stream()
              .filter(conn -> conn.getParticipantInfo().getVerifier() != null).collect(
                  Collectors.toList());
          employersListFragment.addConnections(verifiersConnections); */
      });
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.action_new_connection) {
      navigator.showQrScanner(this);
      return true;
    }
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
    return new RootAppBar(R.string.connections_activity_title);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    ActivityUtils
        .onQrcodeResult(requestCode, resultCode, (MainActivity) getActivity(),
            viewModel, data, this);
  }
}
