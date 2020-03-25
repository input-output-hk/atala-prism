package io.iohk.cvp.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import butterknife.BindView;
import io.iohk.cvp.R;
import io.iohk.cvp.grpc.AsyncTaskResult;
import io.iohk.cvp.utils.DateUtils;
import io.iohk.cvp.viewmodel.PaymentViewModel;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.Preferences;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.fragments.utils.StackedAppBar;
import io.iohk.cvp.views.utils.adapters.PaymentsHistoryRecyclerViewAdapter;
import io.iohk.prism.protos.Payment;
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

  LiveData<AsyncTaskResult<List<Payment>>> liveData;

  private PaymentsHistoryRecyclerViewAdapter adapter = new PaymentsHistoryRecyclerViewAdapter(
      new DateUtils(getContext()));

  @Override
  protected int getViewId() {
    return R.layout.fragment_payment_history;
  }

  @Override
  protected AppBarConfigurator getAppBarConfigurator() {
    setHasOptionsMenu(true);
    return new StackedAppBar(R.string.payment_history);
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

    Preferences prefs = new Preferences(getContext());

    liveData = viewModel.getPayments(prefs.getUserIds());

    if (!liveData.hasActiveObservers()) {
      liveData.observe(this, response -> {
        if (response.getError() != null) {
          getNavigator().showPopUp(getFragmentManager(), getResources().getString(
              R.string.server_error_message));
          return;
        }
        adapter.addPayments(response.getResult());
      });
    }
    return view;
  }

  @Override
  public PaymentViewModel getViewModel() {
    PaymentViewModel viewModel = ViewModelProviders.of(this, factory).get(PaymentViewModel.class);
    viewModel.setContext(getContext());
    return viewModel;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    viewModel.clearPayments();
    adapter.clearPayments();
    liveData.removeObservers(this);
  }
}
