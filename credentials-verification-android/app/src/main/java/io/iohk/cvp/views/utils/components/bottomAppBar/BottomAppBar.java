package io.iohk.cvp.views.utils.components.bottomAppBar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageButton;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import butterknife.BindColor;
import butterknife.BindDrawable;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.iohk.cvp.R;
import lombok.Setter;

public class BottomAppBar extends CoordinatorLayout {

  @BindView(R.id.fab)
  FloatingActionButton fab;

  @BindColor(R.color.colorPrimary)
  ColorStateList colorRed;

  @BindColor(R.color.black)
  ColorStateList colorBlack;

  @BindView(R.id.home_menu_item)
  ImageButton homeMenuItem;

  @BindDrawable(R.drawable.ic_home_grey)
  Drawable icHomeGrey;

  @BindDrawable(R.drawable.ic_home_red)
  Drawable icHomeRed;

  @BindView(R.id.profile_menu_item)
  ImageButton profileMenuItem;

  @BindDrawable(R.drawable.ic_profile_grey)
  Drawable icProfileGrey;

  @BindDrawable(R.drawable.ic_profile_red)
  Drawable icProfileRed;

  @BindView(R.id.wallet_menu_item)
  ImageButton walletMenuItem;

  @BindDrawable(R.drawable.ic_wallet_grey)
  Drawable icWalletGrey;

  @BindDrawable(R.drawable.ic_wallet_red)
  Drawable icWalletRed;

  @BindView(R.id.settings_menu_item)
  ImageButton settingsMenuItem;

  @BindDrawable(R.drawable.ic_settings_grey)
  Drawable icSettingsGrey;

  @BindDrawable(R.drawable.ic_settings_red)
  Drawable icSettingsRed;

  @BindView(R.id.bottom_appbar_container)
  CoordinatorLayout bottomAppBarContainer;

  @Setter
  private BottomAppBarListener listener;

  public void onItemSelected(BottomAppBarOption option) {
    setItemColors(option);
    listener.onNavigation(option, null);
  }

  private void setItemColors(BottomAppBarOption optionSelected) {
    fab.setBackgroundTintList(BottomAppBarOption.CONNECTIONS.equals(optionSelected) ? colorRed : colorBlack);
    homeMenuItem.setImageDrawable(BottomAppBarOption.HOME.equals(optionSelected) ? icHomeRed : icHomeGrey);
    profileMenuItem.setImageDrawable(BottomAppBarOption.PROFILE.equals(optionSelected) ? icProfileRed : icProfileGrey);
    walletMenuItem.setImageDrawable(BottomAppBarOption.WALLET.equals(optionSelected) ? icWalletRed : icWalletGrey);
    settingsMenuItem.setImageDrawable(BottomAppBarOption.SETTINGS.equals(optionSelected) ? icSettingsRed : icSettingsGrey);
  }

  @OnClick(R.id.fab)
  public void onFabClick() {
    onItemSelected(BottomAppBarOption.CONNECTIONS);
  }

  @OnClick(R.id.home_menu_item)
  public void onHomeClick() {
    onItemSelected(BottomAppBarOption.HOME);
  }

  @OnClick(R.id.profile_menu_item)
  public void onProfileClick() {
    onItemSelected(BottomAppBarOption.PROFILE);
  }

  @OnClick(R.id.wallet_menu_item)
  public void onWalletClick() {
    onItemSelected(BottomAppBarOption.WALLET);
  }

  @OnClick(R.id.settings_menu_item)
  public void onSettingsClick() {
    onItemSelected(BottomAppBarOption.SETTINGS);
  }

  public BottomAppBar(Context context) {
    super(context);
    init();
  }

  public BottomAppBar(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public BottomAppBar(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init();
  }

  private void init() {
    inflate(getContext(), R.layout.bottom_appbar, this);
    ButterKnife.bind(this);
    fab.setBackgroundTintList(colorRed);
  }
}
