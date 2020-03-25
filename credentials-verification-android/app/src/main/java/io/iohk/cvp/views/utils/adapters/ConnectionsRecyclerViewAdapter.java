package io.iohk.cvp.views.utils.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import java.util.ArrayList;
import java.util.List;

import io.iohk.prism.protos.ConnectionInfo;
import lombok.Setter;

@Setter
public abstract class ConnectionsRecyclerViewAdapter<V extends ViewHolder> extends
    RecyclerView.Adapter<V> {

  protected List<ConnectionInfo> connections = new ArrayList<>();

  @Override
  public int getItemCount() {
    return connections.size();
  }

  @NonNull
  @Override
  public V onCreateViewHolder(ViewGroup parent, int viewType) {
    View v = LayoutInflater.from(parent.getContext()).inflate(getLayoutId(), parent, false);
    return createViewHolder(v);
  }

  protected abstract int getLayoutId();

  protected abstract V createViewHolder(View view);

  @Override
  public void onBindViewHolder(@NonNull V holder, int position) {
    onBindViewHolder(holder, connections.get(position));
  }

  protected abstract void onBindViewHolder(V holder, ConnectionInfo connectionInfo);

  public void addConnections(List<ConnectionInfo> newConnections) {
    connections.addAll(newConnections);
  }
}
