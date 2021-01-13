package io.iohk.atala.prism.app.ui.utils.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import io.iohk.cvp.R;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class SeedPhraseAdapter extends BaseAdapter {

    @Getter
    @Setter
    private List<String> seedPhrase = new ArrayList<>();

    @Override
    public int getCount() {
        return seedPhrase.size();
    }

    @Override
    public Object getItem(int position) {
        return seedPhrase.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView != null ? convertView
                : LayoutInflater.from(parent.getContext()).inflate(R.layout.row_seed, parent, false);
        TextView textView = v.findViewById(R.id.text_view_seed);
        String seedToShow = (position + 1) + ". " + seedPhrase.get(position);
        textView.setText(seedToShow);
        return v;
    }
}

