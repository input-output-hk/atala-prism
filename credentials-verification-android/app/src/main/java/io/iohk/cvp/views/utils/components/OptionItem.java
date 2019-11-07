package io.iohk.cvp.views.utils.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
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
      final TypedArray attrArray = getContext().obtainStyledAttributes(
          attrs, R.styleable.OptionItem, defStyle, 0);
      try {
        textView.setText(attrArray.getString(
            R.styleable.OptionItem_primaryText));

        setSecondaryText(attrArray);

        actionImageView.setImageDrawable(attrArray.getDrawable(
            R.styleable.OptionItem_actionImage));

        logoImageView.setImageDrawable(attrArray.getDrawable(
            R.styleable.OptionItem_logoImage));
      } finally {
        attrArray.recycle();
      }
    }
  }

  private void setSecondaryText(TypedArray attrArray) {
    String secondaryTextValue = attrArray.getString(
        R.styleable.OptionItem_secondaryText);

    if (secondaryTextValue != null) {
      secondaryTextView.setText(secondaryTextValue);
    } else {
      secondaryTextView.setVisibility(GONE);
      ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
          0, LayoutParams.WRAP_CONTENT);

      int marginSides = (int) TypedValue.applyDimension(
          TypedValue.COMPLEX_UNIT_DIP,
          16,
          getResources().getDisplayMetrics()
      );

      params.setMargins(marginSides, 0, marginSides, 0);
      params.topToTop = LayoutParams.PARENT_ID;
      params.bottomToBottom = LayoutParams.PARENT_ID;
      params.startToEnd = R.id.card_logo_container;
      textView.setLayoutParams(params);
    }
  }
}
