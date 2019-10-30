package io.iohk.cvp.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.lifecycle.ViewModel;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import javax.inject.Inject;

import butterknife.BindView;
import io.iohk.cvp.R;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.utils.adapters.ConnectionTabsAdapter;
import io.iohk.cvp.views.utils.adapters.EmployersRecyclerViewAdapter;
import io.iohk.cvp.views.utils.adapters.UniversitiesRecyclerViewAdapter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@NoArgsConstructor
public class ConnectionsFragment extends CvpFragment {

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
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
  protected int getViewId() {
    return R.layout.fragment_connections;
  }

  @Override
  public ViewModel getViewModel() {
    return null;
  }
}
