package io.iohk.cvp.views.utils.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.iohk.cvp.R;
import io.iohk.cvp.utils.DateUtils;
import io.iohk.cvp.viewmodel.PaymentViewModel.Payment;
import io.iohk.cvp.views.utils.components.PaymentState;
import java.util.ArrayList;
import java.util.List;
import lombok.Setter;

public class PaymentsHistoryRecyclerViewAdapter extends
    RecyclerView.Adapter<PaymentsHistoryRecyclerViewAdapter.ViewHolder> {

  @Setter
  private List<Payment> payments = new ArrayList<>();

  private final DateUtils dateUtils;

  public PaymentsHistoryRecyclerViewAdapter(DateUtils dateUtils) {
    this.dateUtils = dateUtils;
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View v = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.row_historic_payment, parent, false);
    return new ViewHolder(v);
  }

  @Override
  public void onBindViewHolder(PaymentsHistoryRecyclerViewAdapter.ViewHolder holder, int position) {
    // TODO unmock this when we are sure about which class's info are we going to show here
    Payment payment = payments.get(position);
    holder.textViewDate.setText(dateUtils.format(payment.getDate()));
    holder.textViewTitle.setText(payment.getTitle());
    holder.textViewAmount.setText(payment.getCurrencySymbol() + payment.getAmount());
    holder.paymentState.setState(payment.getState());
  }

  @Override
  public int getItemCount() {
    return payments.size();
  }

  static class ViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.text_view_date)
    TextView textViewDate;

    @BindView(R.id.text_view_title)
    TextView textViewTitle;

    @BindView(R.id.text_view_amount)
    TextView textViewAmount;

    @BindView(R.id.text_view_state)
    PaymentState paymentState;

    ViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }

  }
}

