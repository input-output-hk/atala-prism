package io.iohk.cvp.views.utils.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import java.util.Objects;

import io.iohk.cvp.R;

public class SuccessDialog extends DialogFragment {

  private Fragment context;

  public static SuccessDialog newInstance(Fragment context, int description) {
    SuccessDialog frag = new SuccessDialog();
    Bundle args = new Bundle();
    args.putInt("description", description);
    frag.context = context;
    frag.setArguments(args);
    return frag;
  }


  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    int description = getArguments().getInt("description");

    // Use the Builder class for convenient dialog construction
    AlertDialog.Builder builder = new AlertDialog.Builder(Objects.requireNonNull(getActivity()));

    LayoutInflater inflater = context.getLayoutInflater();
    View dialogView = inflater.inflate(R.layout.success_popup, null);

    TextView descriptionTextView = dialogView.findViewById(R.id.description_text_view);
    descriptionTextView.setText(description);

    Button btnOk = dialogView.findViewById(R.id.btn_ok);
    btnOk.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        SuccessDialog.this.dismiss();
      }
    });

    builder.setView(dialogView);
    return builder.create();
  }
}
