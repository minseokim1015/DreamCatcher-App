package com.smartapp.dreamcatcher.data;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smartapp.dreamcatcher.R;
import java.util.ArrayList;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.ViewHolder> {
    private ArrayList<Music> data;
    private OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onItemClick(View v, int pos, Music item) ;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvTitle, tvDescription;

        public ViewHolder(final View v) {
            super(v);
            tvTitle = v.findViewById(R.id.list_item_music_title);
            tvDescription = v.findViewById(R.id.list_item_music_desc);

            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int pos = getAdapterPosition() ;
                    if (pos != RecyclerView.NO_POSITION) {
                        Music item = data.get(pos);
                        if (mListener != null) {
                            mListener.onItemClick(v, pos, item) ;
                        }
                    }
                }
            });
        }
    }

    public MusicAdapter(ArrayList<Music> data) {
        this.data = data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.list_item_music, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Music item = data.get(position);
        holder.tvTitle.setText(item.title);
        holder.tvDescription.setText(item.description);
    }

    @Override
    public int getItemCount() {
        return this.data.size();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mListener = listener ;
    }
}
