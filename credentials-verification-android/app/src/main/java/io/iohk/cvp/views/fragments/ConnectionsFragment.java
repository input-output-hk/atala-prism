package io.iohk.cvp.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import com.google.android.material.tabs.TabLayout;
import io.iohk.cvp.R;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.fragments.utils.RootAppBar;
import io.iohk.cvp.views.utils.adapters.ConnectionTabsAdapter;
import io.iohk.cvp.views.utils.adapters.EmployersRecyclerViewAdapter;
import io.iohk.cvp.views.utils.adapters.UniversitiesRecyclerViewAdapter;
import java.util.Objects;
import javax.inject.Inject;
import lombok.Setter;

@Setter
public class ConnectionsFragment extends CvpFragment {

  @Inject
  public ConnectionsFragment() {
  }

  @Inject
  Navigator navigator;

  @BindView(R.id.connections_list_tabs)
  TabLayout tabs;

  @BindView(R.id.connections_list_view_pager)
  ViewPager viewPager;

  @Inject
  ConnectionsListFragment universitiesListFragment;

  @Inject
  ConnectionsListFragment employersListFragment;

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
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.action_new_connection) {
      navigator.showFragment(
          Objects.requireNonNull(getActivity()).getSupportFragmentManager(),
          new FirstConnectionFragment());
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
  public ViewModel getViewModel() {
    return null;
  }

  @Override
  protected AppBarConfigurator getAppBarConfigurator() {
    return new RootAppBar(R.string.connections_activity_title);
  }
}
