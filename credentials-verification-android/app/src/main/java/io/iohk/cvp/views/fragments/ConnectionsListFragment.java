package io.iohk.cvp.views.fragments;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import io.iohk.cvp.R;
import io.iohk.cvp.viewmodel.ConnectionsActivityViewModel;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.fragments.utils.RootAppBar;
import io.iohk.cvp.views.utils.adapters.ConnectionsRecyclerViewAdapter;
import java.util.ArrayList;
import javax.inject.Inject;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@NoArgsConstructor
public class ConnectionsListFragment extends CvpFragment<ConnectionsActivityViewModel> {

  @BindView(R.id.universities_list)
  RecyclerView recyclerView;
  private ViewModelProvider.Factory factory;
  private ConnectionsRecyclerViewAdapter adapter;

  @Inject
  public ConnectionsListFragment(ViewModelProvider.Factory factory) {
    this.factory = factory;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return super.onCreateView(inflater, container, savedInstanceState);
  }

  @Override
  public void onViewCreated(@NonNull View view,
      Bundle savedInstanceState) {
    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

    adapter.setConnections(new ArrayList<>());
    recyclerView.setAdapter(adapter);

    viewModel.getConnections().observe(this, connections -> {
      adapter.setConnections(connections);
      adapter.notifyDataSetChanged();
    });
  }

  @Override
  protected int getViewId() {
    return R.layout.universities_list;
  }

  @Override
  protected AppBarConfigurator getAppBarConfigurator() {
    return new RootAppBar(R.string.connections_activity_title);
  }

  @Override
  public ConnectionsActivityViewModel getViewModel() {
    viewModel = ViewModelProviders.of(this, factory).get(ConnectionsActivityViewModel.class);
    return viewModel;
  }
}
