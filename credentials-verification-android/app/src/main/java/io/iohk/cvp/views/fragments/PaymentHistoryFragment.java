package io.iohk.cvp.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import io.iohk.cvp.R;
import io.iohk.cvp.utils.DateUtils;
import io.iohk.cvp.viewmodel.PaymentViewModel;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.utils.adapters.PaymentsHistoryRecyclerViewAdapter;
import java.util.Objects;
import javax.inject.Inject;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@NoArgsConstructor
public class PaymentHistoryFragment extends CvpFragment<PaymentViewModel> {

  private ViewModelProvider.Factory factory;

  @BindView(R.id.payments_list)
  public RecyclerView recyclerView;

  @Inject
  PaymentHistoryFragment(ViewModelProvider.Factory factory) {
    this.factory = factory;
  }

  @Inject
  Navigator navigator;

  private PaymentsHistoryRecyclerViewAdapter adapter = new PaymentsHistoryRecyclerViewAdapter(
      new DateUtils(getContext()));

  @Override
  protected int getViewId() {
    return R.layout.fragment_payment_history;
  }

  @Override
  protected AppBarConfigurator getAppBarConfigurator() {
    return supportActionBar -> {
      setHasOptionsMenu(true);
      supportActionBar.setHomeButtonEnabled(true);
      supportActionBar.setDisplayHomeAsUpEnabled(true);
      supportActionBar.setTitle(R.string.payment_history);
    };
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      Objects.requireNonNull(getActivity()).onBackPressed();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);
    LinearLayoutManager linearLayoutManagerCredentials = new LinearLayoutManager(getContext());

    recyclerView.setLayoutManager(linearLayoutManagerCredentials);
    recyclerView.setAdapter(adapter);

    viewModel.getPayments().observe(this, payments -> {
      adapter.setPayments(payments);
      adapter.notifyDataSetChanged();
    });
    return view;
  }

  @Override
  public PaymentViewModel getViewModel() {
    return ViewModelProviders.of(this, factory).get(PaymentViewModel.class);
  }
}
