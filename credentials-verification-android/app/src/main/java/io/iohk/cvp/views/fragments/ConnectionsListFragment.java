package io.iohk.cvp.views.fragments;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import io.iohk.cvp.R;
import io.iohk.cvp.io.connector.ConnectionInfo;
import io.iohk.cvp.viewmodel.ConnectionsActivityViewModel;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.fragments.utils.RootAppBar;
import io.iohk.cvp.views.utils.adapters.ConnectionsRecyclerViewAdapter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@NoArgsConstructor
public class ConnectionsListFragment extends CvpFragment {

  private ViewModelProvider.Factory factory;

  @BindView(R.id.universities_list)
  RecyclerView recyclerView;

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

    //TODO use view model and make grpc call to fetch real connections info
    List<ConnectionInfo> lista = new ArrayList<>();
    lista.add(ConnectionInfo.getDefaultInstance());
    lista.add(ConnectionInfo.getDefaultInstance());
    lista.add(ConnectionInfo.getDefaultInstance());

    adapter.setConnections(lista);
    recyclerView.setAdapter(adapter);
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
    // TODO implemenet this when view model is defined after grpc server is available
    /* viewModel = ViewModelProviders.of(this, factory).get(ConnectionsActivityViewModel.class);
    return viewModel; */
    return null;
  }
}
