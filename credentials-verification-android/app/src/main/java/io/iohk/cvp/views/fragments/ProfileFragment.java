package io.iohk.cvp.views.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;
import butterknife.BindView;
import com.google.android.material.textfield.TextInputEditText;
import io.iohk.cvp.R;
import io.iohk.cvp.views.Preferences;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.fragments.utils.RootAppBar;
import javax.inject.Inject;
import lombok.Setter;

@Setter
public class ProfileFragment extends CvpFragment {

  private Preferences preferences;
  private boolean isEditing;


  @Inject
  public ProfileFragment() {
  }

  @BindView(R.id.edit_text_name)
  TextInputEditText textInputEditTextName;

  @BindView(R.id.edit_text_country)
  TextInputEditText textInputEditTextCountry;

  @BindView(R.id.edit_text_birth_date)
  TextInputEditText textInputEditTextBirthDate;

  @BindView(R.id.edit_text_email)
  TextInputEditText textInputEditTextEmail;

  @BindView(R.id.text_view_email)
  TextView textViewEmail;

  @BindView(R.id.text_view_name)
  TextView textViewName;

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
    this.preferences = new Preferences(getContext());

    String name = preferences.getString(Preferences.USER_PROFILE_NAME);
    textInputEditTextName.setText(name);
    textViewName.setText(name);

    textInputEditTextCountry.setText(preferences.getString(Preferences.USER_PROFILE_COUNTRY));
    textInputEditTextBirthDate.setText(preferences.getString(Preferences.USER_PROFILE_BIRTH_DATE));

    String email = preferences.getString(Preferences.USER_PROFILE_EMAIL);
    textInputEditTextEmail.setText(email);
    textViewEmail.setText(email);
    return view;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.action_edit_profile) {
      this.isEditing = !this.isEditing;
      this.setActionBar();
      switchState(textInputEditTextName);
      switchState(textInputEditTextCountry);
      switchState(textInputEditTextBirthDate);
      switchState(textInputEditTextEmail);

      if (this.isEditing) {
        item.setIcon(getActivity().getDrawable(R.drawable.ic_check));
      } else {
        item.setIcon(getActivity().getDrawable(R.drawable.ic_edit));
        saveUserProfile();
      }
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void switchState(TextInputEditText editText) {
    editText.setEnabled(!editText.isEnabled());
  }

  private void saveUserProfile() {
    String name = textInputEditTextName.getText().toString();
    String country = textInputEditTextCountry.getText().toString();
    String birthDate = textInputEditTextBirthDate.getText().toString();
    String email = textInputEditTextEmail.getText().toString();

    preferences.saveUserProfile(name, country, birthDate, email);
    textViewEmail.setText(email);
    textViewName.setText(name);
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
    return new RootAppBar(isEditing ? R.string.edit_profile : R.string.profile, Color.WHITE);
  }
}
