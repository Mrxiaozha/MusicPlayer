package com.example.musicplayer;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.List;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.ViewHolder> {
    private List<MusicFile> musicList;
    private OnItemClickListener listener;
    private int currentPlayingPosition = -1;
    private Context context;

    public interface OnItemClickListener {
        void onItemClick(MusicFile musicFile);
    }

    public MusicAdapter(List<MusicFile> musicList, OnItemClickListener listener) {
        this.musicList = musicList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_music, parent, false);
        context = parent.getContext();
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MusicFile music = musicList.get(position);
        holder.tvIndex.setText(String.format("%d.", position + 1));
        
        String fileName = music.getTitle();
        if (fileName.toLowerCase().endsWith(".mp3")) {
            fileName = fileName.substring(0, fileName.length() - 4);
        }
        holder.tvMusicName.setText(fileName);

        if (position == currentPlayingPosition) {
            holder.tvIndex.setTextColor(Color.parseColor("#FFEB3B"));
            holder.tvMusicName.setTextColor(Color.parseColor("#FFEB3B"));
            holder.tvMusicName.setTypeface(null, Typeface.BOLD);
        } else {
            holder.tvIndex.setTextColor(Color.WHITE);
            holder.tvMusicName.setTextColor(Color.WHITE);
            holder.tvMusicName.setTypeface(null, Typeface.NORMAL);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(music);
            }
        });
    }

    public void setCurrentPlayingPosition(int position) {
        this.currentPlayingPosition = position;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return musicList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIndex;
        TextView tvMusicName;

        ViewHolder(View view) {
            super(view);
            tvIndex = view.findViewById(R.id.tvIndex);
            tvMusicName = view.findViewById(R.id.tvMusicName);
        }
    }
} 