package io.iohk.cvp.views.utils.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.iohk.cvp.R;
import io.iohk.cvp.views.activities.CvpActivity;
import io.iohk.cvp.views.activities.TermsAndConditionsActivity;
import io.iohk.cvp.views.utils.dialogs.ConditionsDialog;
import lombok.Setter;

public class CheckboxWithDescription extends ConstraintLayout {

  public interface CheckboxStateListener {
    void stateChanged(Boolean isClicked);
  }

  public interface CheckboxLinkListener {
    void linkClicked();
  }

  @BindView(R.id.checkbox)
  public CheckBox checkBox;

  @BindView(R.id.text_view)
  public TextView textView;

  @BindView(R.id.link_text_view)
  public TextView linkTextView;

  private CheckboxStateListener statelistener;
  private CheckboxLinkListener clicklistener;

  public CheckboxWithDescription(Context context) {
    super(context);
    init(null, 0);
  }

  public CheckboxWithDescription(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(attrs, 0);
  }

  public CheckboxWithDescription(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(attrs, defStyle);
  }

  private void init(AttributeSet attrs, int defStyle) {
    inflate(getContext(), R.layout.component_checkbox_with_description, this);
    ButterKnife.bind(this);

    if (attrs != null) {
      // Load attributes
      final TypedArray a = getContext().obtainStyledAttributes(
        attrs, R.styleable.CheckboxWithDescription, defStyle, 0);
      try {
        textView.setText(a.getString(
          R.styleable.CheckboxWithDescription_text));
        linkTextView.setText(a.getString(
          R.styleable.CheckboxWithDescription_link_text));
      } finally {
        a.recycle();
      }
    }
  }

  public void setListeners(CheckboxStateListener statelistener, CheckboxLinkListener clicklistener) {
    this.statelistener = statelistener;
    this.clicklistener = clicklistener;
  }

  @OnClick(R.id.checkbox)
  public void onChecked(CheckBox checkBox) {
    statelistener.stateChanged(checkBox.isChecked());
  }


  @OnClick(R.id.link_text_view)
  public void onTextClicked() {
    clicklistener.linkClicked();
  }
}
