package io.iohk.cvp.views.utils.adapters;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import io.iohk.cvp.io.connector.ConnectionInfo;
import java.util.List;

public abstract class ConnectionsRecyclerViewAdapter<V extends ViewHolder> extends
    RecyclerView.Adapter<V> {

  public abstract void setConnections(List<ConnectionInfo> connections);

}
