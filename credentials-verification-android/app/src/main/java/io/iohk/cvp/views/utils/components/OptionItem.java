package io.iohk.cvp.views.utils.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.iohk.cvp.R;

public class OptionItem extends ConstraintLayout {

  @BindView(R.id.primary_info)
  public TextView textView;

  @BindView(R.id.more_info)
  public TextView secondaryTextView;

  @BindView(R.id.logo)
  public ImageView logoImageView;

  @BindView(R.id.action_image)
  public ImageView actionImageView;

  public OptionItem(@NonNull Context context) {
    super(context);
  }

  public OptionItem(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(attrs, 0);
  }

  public OptionItem(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(attrs, defStyle);
  }

  private void init(AttributeSet attrs, int defStyle) {
    inflate(getContext(), R.layout.option_item, this);
    ButterKnife.bind(this);

    if (attrs != null) {
      // Load attributes
      final TypedArray a = getContext().obtainStyledAttributes(
          attrs, R.styleable.OptionItem, defStyle, 0);
      try {
        textView.setText(a.getString(
            R.styleable.OptionItem_primaryText));

        secondaryTextView.setText(a.getString(
            R.styleable.OptionItem_secondaryText));

        actionImageView.setImageDrawable(a.getDrawable(
            R.styleable.OptionItem_actionImage));

        logoImageView.setImageDrawable(a.getDrawable(
            R.styleable.OptionItem_logoImage));
      } finally {
        a.recycle();
      }
    }
  }
}
