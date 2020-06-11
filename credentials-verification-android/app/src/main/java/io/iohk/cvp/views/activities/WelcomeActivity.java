package io.iohk.cvp.views.activities;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.lifecycle.ViewModel;
import androidx.viewpager2.widget.ViewPager2;

import butterknife.BindColor;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.OnClick;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.analytics.FirebaseAnalytics;

import io.iohk.cvp.R;
import io.iohk.cvp.utils.FirebaseAnalyticsEvents;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.utils.adapters.TutorialScrolleableAdapter;

import java.util.Objects;
import javax.inject.Inject;

public class WelcomeActivity extends CvpActivity {

  @Inject
  Navigator navigator;

  @BindView(R.id.welcome_view)
  View welcomeView;

  @BindView(R.id.vp_pager)
  ViewPager2 vpPager;

  @BindView(R.id.steps_counter)
  View stepsCounter;

  @BindView(R.id.step1_text)
  TextView step1Text;

  @BindView(R.id.step1_dot)
  View step1Dot;

  @BindView(R.id.step2_text)
  TextView step2Text;

  @BindView(R.id.step2_dot)
  View step2Dot;

  @BindView(R.id.step3_dot)
  View step3Dot;

  @BindView(R.id.view_pager_button)
  Button viewPagerButton;

  @BindView(R.id.restore_account_btn)
  Button restoreAccountBtn;

  @BindView(R.id.get_started_button)
  Button getStartedButton;

  @BindView(R.id.tabDots)
  TabLayout tabLayout;

  @BindColor(R.color.black)
  ColorStateList blackColor;

  @BindColor(R.color.grey_2)
  ColorStateList mediumGrayColor;

  @BindString(R.string.continue_string)
  String continueString;

  @BindString(R.string.get_started)
  String getStartedString;

  @BindString(R.string.create_account)
  String createAccountString;

  private FirebaseAnalytics mFirebaseAnalytics;
  private TutorialScrolleableAdapter vpAdapter;

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    Objects.requireNonNull(getSupportActionBar()).hide();
    mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

    vpAdapter = new TutorialScrolleableAdapter(this);
    vpPager.setAdapter(vpAdapter);


    new TabLayoutMediator(tabLayout, vpPager, (tab, position) -> {
      vpPager.setCurrentItem(tab.getPosition(), true);
    }).attach();


    vpPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
      @Override
      public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        super.onPageScrolled(position, positionOffset, positionOffsetPixels);
      }

      @Override
      public void onPageSelected(int position) {
        super.onPageSelected(position);
      }

      @Override
      public void onPageScrollStateChanged(int state) {
        super.onPageScrollStateChanged(state);
      }
    });
  }

  @Override
  public void onBackPressed() {
    if (vpPager.getCurrentItem() > 0) {
      vpPager.setCurrentItem(vpPager.getCurrentItem() - 1);
    } else if(vpPager.getVisibility() == View.VISIBLE) {
      vpPager.setVisibility(View.GONE);
      tabLayout.setVisibility(View.GONE);
      welcomeView.setVisibility(View.VISIBLE);
      getStartedButton.setVisibility(View.VISIBLE);
      viewPagerButton.setVisibility(View.GONE);
    } else {
      super.onBackPressed();
    }
  }

  @OnClick(R.id.view_pager_button)
  public void onClickViewPager() {
    if(vpPager.getCurrentItem() == vpAdapter.getItemCount()-1) {
      mFirebaseAnalytics.logEvent(FirebaseAnalyticsEvents.CREATE_ACCOUNT,null);
      // TODO this should take you to the wallet setup
      navigator.showTermsAndConditions(this);
    } else {
      vpPager.setCurrentItem(vpPager.getCurrentItem() + 1);
    }
  }

  @OnClick(R.id.get_started_button)
  public void onClickGetStarted() {
    vpPager.setVisibility(View.VISIBLE);
    tabLayout.setVisibility(View.VISIBLE);
    welcomeView.setVisibility(View.GONE);
    getStartedButton.setVisibility(View.GONE);
    viewPagerButton.setVisibility(View.VISIBLE);
  }

  @Override
  protected Navigator getNavigator() {
    return this.navigator;
  }

  @Override
  protected int getView() {
    return R.layout.welcome_activity;
  }

  @Override
  protected int getTitleValue() {
    return R.string.empty_title;
  }

  @Override
  public ViewModel getViewModel() {
    return null;
  }
}
