package io.iohk.cvp.views.activities;

import android.os.Bundle;
import androidx.lifecycle.ViewModel;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import com.google.android.material.tabs.TabLayout;
import io.iohk.cvp.R;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.fragments.ConnectionsListFragment;
import io.iohk.cvp.views.utils.adapters.ConnectionTabsAdapter;
import io.iohk.cvp.views.utils.adapters.EmployersRecyclerViewAdapter;
import io.iohk.cvp.views.utils.adapters.UniversitiesRecyclerViewAdapter;
import javax.inject.Inject;

public class ConnectionsListActivity extends CvpActivity {

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
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    universitiesListFragment.setAdapter(new UniversitiesRecyclerViewAdapter());
    employersListFragment.setAdapter(new EmployersRecyclerViewAdapter());

    ConnectionTabsAdapter adapter = new ConnectionTabsAdapter(
        getSupportFragmentManager(), tabs.getTabCount(), universitiesListFragment,
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

  }

  @Override
  protected Navigator getNavigator() {
    return null;
  }

  @Override
  protected int getView() {
    return R.layout.connections_list_activity;
  }

  @Override
  protected int getTitleValue() {
    return R.string.connections_activity_title;
  }

  @Override
  public ViewModel getViewModel() {
    return null;
  }
}
