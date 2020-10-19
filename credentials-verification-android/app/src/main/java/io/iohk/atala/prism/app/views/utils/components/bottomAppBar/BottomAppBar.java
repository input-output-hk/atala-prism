package io.iohk.atala.prism.app.views.utils.components.bottomAppBar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageButton;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import butterknife.BindColor;
import butterknife.BindDrawable;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.iohk.cvp.R;
import lombok.Setter;

public class BottomAppBar extends CoordinatorLayout {

    @BindColor(R.color.colorPrimary)
    ColorStateList colorRed;

    @BindColor(R.color.black)
    ColorStateList colorBlack;

    @BindView(R.id.my_credentials_menu_item)
    ImageButton myCredentialItem;

    @BindDrawable(R.drawable.ic_credentials_grey)
    Drawable icCredentialsGrey;

    @BindDrawable(R.drawable.ic_credentials_red)
    Drawable icCredentialsRed;

    @BindView(R.id.contacts_menu_item)
    ImageButton contactsMenuItem;

    @BindDrawable(R.drawable.ic_profile_grey)
    Drawable icProfileGrey;

    @BindDrawable(R.drawable.ic_profile_red)
    Drawable icProfileRed;

    @BindDrawable(R.drawable.ic_contacts_grey)
    Drawable icContactGrey;

    @BindDrawable(R.drawable.ic_contacts_red)
    Drawable icContactRed;

    @BindView(R.id.profile_menu_item)
    ImageButton profileMenuItem;

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
        listener.onNavigation(option);
    }

    public void setItemColors(BottomAppBarOption optionSelected) {
        myCredentialItem
                .setImageDrawable(BottomAppBarOption.CREDENTIAL.equals(optionSelected) ? icCredentialsRed : icCredentialsGrey);
        contactsMenuItem.setImageDrawable(
                BottomAppBarOption.CONTACTS.equals(optionSelected) ? icContactRed : icContactGrey);
        profileMenuItem.setImageDrawable(
                BottomAppBarOption.PROFILE.equals(optionSelected) ? icProfileRed : icProfileGrey);
        settingsMenuItem.setImageDrawable(
                BottomAppBarOption.SETTINGS.equals(optionSelected) ? icSettingsRed : icSettingsGrey);
    }

    @OnClick(R.id.my_credentials_menu_item)
    public void onMyCredentialsClick() {
        onItemSelected(BottomAppBarOption.CREDENTIAL);
    }

    @OnClick(R.id.contacts_menu_item)
    public void onContactsClick() {
        onItemSelected(BottomAppBarOption.CONTACTS);
    }

    @OnClick(R.id.profile_menu_item)
    public void onProfileClick() {
        onItemSelected(BottomAppBarOption.PROFILE);
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
    }
}
