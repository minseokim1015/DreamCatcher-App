package com.smartapp.dreamcatcher.data;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Color;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smartapp.dreamcatcher.R;

import java.util.ArrayList;
import java.util.List;

public class BTListAdapter extends RecyclerView.Adapter<BTListAdapter.ViewHolder> {
    private OnItemClickListener onClickListener;
    private int selectedIndex = RecyclerView.NO_POSITION;
    private List<BluetoothDevice> data;

    public interface OnItemClickListener {
        void onItemClick(View v, int pos, BluetoothDevice item);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public View parent;
        public TextView tvName;
        public ViewHolder(View parent) {
            super(parent);
            tvName = parent.findViewById(R.id.list_item_bt_text);
            this.parent = parent;
            parent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int pos = getAdapterPosition();
                    int oldPosition = selectedIndex;
                    selectedIndex = pos;
                    if (pos != RecyclerView.NO_POSITION) {
                        notifyItemChanged(pos);
                    } else {
                        notifyItemChanged(oldPosition);
                    }
                    if (pos != RecyclerView.NO_POSITION && onClickListener != null) {
                        onClickListener.onItemClick(view, pos, data.get(pos));
                    }
                }
            });
        }
    }

    public BTListAdapter(ArrayList<BluetoothDevice> data) {
        this.data = data;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BluetoothDevice item = data.get(position);
        holder.tvName.setText(item.getName());
        if (selectedIndex != -1 && position == selectedIndex) {
            holder.parent.setBackgroundResource(R.color.colorAccent);
        } else {
            holder.parent.setBackgroundResource(R.color.transparent);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.list_item_bt, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void setOnClickListener(OnItemClickListener listener) {
        this.onClickListener = listener;
    }

    public void clearSelection() {
        selectedIndex = RecyclerView.NO_POSITION;
    }
}
