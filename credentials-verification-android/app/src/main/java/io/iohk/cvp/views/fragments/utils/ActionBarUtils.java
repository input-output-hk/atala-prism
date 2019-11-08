package io.iohk.cvp.views.fragments.utils;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import androidx.appcompat.app.ActionBar;
import java.util.Objects;

public class ActionBarUtils {

  public static void setTextColor(ActionBar actBar, int color) {
    String title = Objects.requireNonNull(actBar.getTitle()).toString();
    Spannable spannablerTitle = new SpannableString(title);
    spannablerTitle.setSpan(new ForegroundColorSpan(color), 0, spannablerTitle.length(),
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    actBar.setTitle(spannablerTitle);
  }
}
