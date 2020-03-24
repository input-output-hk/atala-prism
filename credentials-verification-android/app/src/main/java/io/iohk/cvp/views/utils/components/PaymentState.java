package io.iohk.cvp.views.utils.components;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.TextView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import butterknife.BindColor;
import butterknife.BindDrawable;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.iohk.cvp.R;
import io.iohk.cvp.viewmodel.PaymentViewModel;

public class PaymentState extends CoordinatorLayout {

  @BindView(R.id.text_view)
  TextView textView;

  @BindColor(R.color.colorPrimary)
  ColorStateList colorRed;
  @BindColor(R.color.green)
  ColorStateList colorGreen;

  @BindDrawable(R.drawable.rounded_red_stroke)
  Drawable strokeRed;
  @BindDrawable(R.drawable.rounded_green_stroke)
  Drawable strokeGreen;

  public void setState(PaymentViewModel.PaymentState state) {
    textView.setText(getStateText(state));
    textView.setBackground(getStateBackground(state));
    textView.setTextColor(getStateColor(state));
  }

  private ColorStateList getStateColor(PaymentViewModel.PaymentState state) {
    if (state.equals(PaymentViewModel.PaymentState.CHARGED)) {
      return colorGreen;
    }

    return colorRed;
  }

  private Drawable getStateBackground(PaymentViewModel.PaymentState state) {
    if (state.equals(PaymentViewModel.PaymentState.CHARGED)) {
      return strokeGreen;
    }
    return strokeRed;
  }

  private int getStateText(PaymentViewModel.PaymentState state) {
    if (state.equals(PaymentViewModel.PaymentState.CHARGED)) {
      return R.string.charged;
    }

    return R.string.failed;
  }

  public PaymentState(Context context) {
    super(context);
    init();
  }

  public PaymentState(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public PaymentState(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init();
  }

  private void init() {
    inflate(getContext(), R.layout.componenet_payment_state, this);
    ButterKnife.bind(this);
  }
}
