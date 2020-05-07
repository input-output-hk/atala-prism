package io.iohk.cvp.views.utils.adapters;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import butterknife.BindDrawable;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.iohk.cvp.R;
import io.iohk.cvp.utils.ImageUtils;
import io.iohk.cvp.viewmodel.dtos.ConnectionListable;
import io.iohk.cvp.views.fragments.ShareCredentialDialogFragment;

import java.util.ArrayList;
import java.util.List;

import lombok.Setter;

public class ShareCredentialRecyclerViewAdapter extends
        RecyclerView.Adapter<ShareCredentialRecyclerViewAdapter.ViewHolder> {

    private final float displayDensity;
    private final ShareCredentialDialogFragment fragment;
    @Setter
    private List<ConnectionListable> connections = new ArrayList<>();

    public ShareCredentialRecyclerViewAdapter(ShareCredentialDialogFragment fragment) {
        this.displayDensity = fragment.getContext().getResources().getDisplayMetrics().density;
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_share_with_verifier, parent, false);
        return new ViewHolder(v, displayDensity, fragment);
    }

    @Override
    public void onBindViewHolder(ShareCredentialRecyclerViewAdapter.ViewHolder holder, int position) {
        holder.setConnectionInfo(connections.get(position));
    }

    @Override
    public int getItemCount() {
        return connections.size();
    }


    public Boolean areConnectionsSelected() {
        return connections.stream().anyMatch(ConnectionListable::getIsSelected);
    }

    public void addConnections(List<ConnectionListable> newConnections) {
        connections.addAll(newConnections);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final float displayDensity;
        private final ShareCredentialDialogFragment fragment;
        @BindView(R.id.verifier_name)
        TextView verifierName;
        @BindView(R.id.verifier_logo)
        ImageView verifierLogo;
        @BindView(R.id.checkShare)
        CheckBox checkShare;
        @BindView(R.id.layout_share_with_verifier)
        View background;
        @BindDrawable(R.drawable.rounded_corner_white)
        Drawable backgroudNotSelected;
        @BindDrawable(R.drawable.rounded_corner_white_bg_red_sroke)
        Drawable backgroudSelected;
        ConnectionListable connectionInfo;

        ViewHolder(final View itemView, final float displayDensity,
                   ShareCredentialDialogFragment fragment) {
            super(itemView);
            this.displayDensity = displayDensity;
            this.fragment = fragment;
            ButterKnife.bind(this, itemView);
        }

        void setConnectionInfo(ConnectionListable connectionInfo) {
            this.connectionInfo = connectionInfo;
            verifierName.setText(connectionInfo.getIssuerName());
            if (connectionInfo.getVerifierLogo().length > 0) {
                verifierLogo.setImageBitmap(
                        ImageUtils.getBitmapFromByteArray(connectionInfo.getVerifierLogo()));
            }
            setBackground();
        }

        @OnClick(R.id.layout_share_with_verifier)
        void onClick() {
            connectionInfo.setIsSelected(!connectionInfo.getIsSelected());
            setBackground();
            checkShare.setChecked(!connectionInfo.getIsSelected());
            fragment.updateButtonState();
            fragment.updateSelectedVerifiers(connectionInfo.getConnectionId(),
                    connectionInfo.getIsSelected());
        }

        void setBackground() {
            boolean isSelected = connectionInfo != null && connectionInfo.getIsSelected();
            this.background.setBackground(isSelected ? backgroudSelected : backgroudNotSelected);
            this.background.setElevation((isSelected ? 2 : 0) * displayDensity);
        }
    }
}

