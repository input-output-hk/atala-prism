package io.iohk.cvp.views.utils.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import io.iohk.cvp.R;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ProfileTabsAdapter extends PagerAdapter {

  private Context context;

  @Override
  public int getCount() {
    return 2;
  }

  @NonNull
  @Override
  public Object instantiateItem(@NonNull ViewGroup collection, int position) {
    LayoutInflater inflater = LayoutInflater.from(context);
    ViewGroup layout = (ViewGroup) inflater
        .inflate(
            position == 0 ? R.layout.fragment_profile_personal_view : R.layout.fragment_profile_qr_view,
            collection,
            false
        );
    collection.addView(layout);
    return layout;
  }

  @Override
  public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
    return view == object;
  }
}

