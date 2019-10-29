package io.iohk.cvp.views.utils.components.seed;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import butterknife.BindColor;
import butterknife.BindDrawable;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.iohk.cvp.R;
import io.iohk.cvp.views.utils.components.bottomAppBar.BottomAppBarListener;
import io.iohk.cvp.views.utils.components.bottomAppBar.BottomAppBarOption;
import lombok.Setter;

public class Seed extends CoordinatorLayout {

  @BindView(R.id.text_view_seed)
  TextView textViewSeed;

  public Seed(Context context) {
    super(context);
    init();
  }

  public Seed(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public Seed(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init();
  }

  private void init() {
    inflate(getContext(), R.layout.seed, this);
    ButterKnife.bind(this);
  }

  public void setText(String text) {
    textViewSeed.setText(text);
  }
}
