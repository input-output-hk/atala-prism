package io.iohk.atala.prism.app.views.utils.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.iohk.cvp.R;

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

    private CheckboxStateListener stateListener;
    private CheckboxLinkListener clickListener;

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
                setParameterText(a, textView, R.styleable.CheckboxWithDescription_text);
                setParameterText(a, linkTextView, R.styleable.CheckboxWithDescription_link_text);
            } finally {
                a.recycle();
            }
        }
    }

    private void setParameterText(TypedArray a, TextView textView, int attr) {
        String text = a.getString(attr);
        if (text != null) {
            textView.setVisibility(VISIBLE);
            textView.setText(text);
        } else {
            textView.setVisibility(INVISIBLE);
        }
    }

    public void setListeners(CheckboxStateListener stateListener, CheckboxLinkListener clickListener) {
        this.stateListener = stateListener;
        this.clickListener = clickListener;
    }

    @OnClick(R.id.checkbox)
    public void onChecked(CheckBox checkBox) {
        stateListener.stateChanged(checkBox.isChecked());
    }


    @OnClick(R.id.link_text_view)
    public void onTextClicked() {
        clickListener.linkClicked();
    }
}
