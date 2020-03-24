package io.iohk.cvp.views.utils.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.iohk.cvp.R;
import io.iohk.cvp.utils.DateUtils;
import io.iohk.cvp.viewmodel.PaymentViewModel;
import io.iohk.cvp.views.utils.components.PaymentState;
import io.iohk.prism.protos.Payment;

public class PaymentsHistoryRecyclerViewAdapter extends
    RecyclerView.Adapter<PaymentsHistoryRecyclerViewAdapter.ViewHolder> {

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
    Payment payment = payments.get(position);
    holder.textViewDate.setText(dateUtils.format(payment.getCreatedOn()));
    holder.textViewAmount.setText(String.format("USD %s", getFormattedAmount(payment.getAmount())));
    holder.paymentState
        .setState(payment.getStatus().equals("CHARGED") ? PaymentViewModel.PaymentState.CHARGED
            : PaymentViewModel.PaymentState.FAILED);
  }

  private String getFormattedAmount(String amount) {
    return new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP).toString();
  }

  public void addPayments(List<Payment> newPayments) {
    this.payments.addAll(newPayments);
    notifyDataSetChanged();
  }

  public void clearPayments() {
    this.payments.clear();
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

