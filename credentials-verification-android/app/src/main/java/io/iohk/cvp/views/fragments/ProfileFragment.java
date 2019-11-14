package io.iohk.cvp.views.fragments;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import com.google.android.material.textfield.TextInputEditText;
import io.iohk.cvp.R;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.fragments.utils.ActionBarUtils;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.fragments.utils.RootAppBar;
import io.iohk.cvp.views.utils.adapters.ProfileTabsAdapter;
import javax.inject.Inject;
import lombok.Setter;

@Setter
public class ProfileFragment extends CvpFragment {

  @Inject
  public ProfileFragment() {
  }

  @Inject
  Navigator navigator;

  @BindView(R.id.profile_tabs)
  TabLayout tabs;

  @BindView(R.id.profile_view_pager)
  ViewPager viewPager;

  @Override
  protected int getViewId() {
    return R.layout.fragment_profile;
  }

  @Override
  public ViewModel getViewModel() {
    return null;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);

    ProfileTabsAdapter adapter = new ProfileTabsAdapter(getContext());

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
    if (item.getItemId() == R.id.action_edit_profile) {
      // TODO: the inputs used to show the profile data should be enabled here
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void switchState(TextInputEditText editText) {
    editText.setEnabled(!editText.isEnabled());
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    MenuItem editProfileMenuItem;
    editProfileMenuItem = menu.findItem(R.id.action_edit_profile);
    editProfileMenuItem.setVisible(true);
  }

  @Override
  protected AppBarConfigurator getAppBarConfigurator() {
    return new RootAppBar(R.string.profile, Color.WHITE);
  }
}
