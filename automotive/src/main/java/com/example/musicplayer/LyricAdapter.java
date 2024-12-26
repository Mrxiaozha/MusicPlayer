package com.example.musicplayer;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class LyricAdapter extends RecyclerView.Adapter<LyricAdapter.ViewHolder> {
    private List<LyricLine> lyrics = new ArrayList<>();
    private int currentLine = -1;
    private Context context;
    private OnLyricLongClickListener longClickListener;

    public interface OnLyricLongClickListener {
        void onLyricLongClick();
    }

    public LyricAdapter(Context context) {
        this.context = context;
    }

    public void setOnLyricLongClickListener(OnLyricLongClickListener listener) {
        this.longClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lyric, parent, false);
        view.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onLyricLongClick();
            }
            return true;
        });
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tvLyric.setText(lyrics.get(position).text);
        holder.tvLyric.setTextSize(20);
        holder.tvLyric.setAlpha(1.0f);
        Log.d("LyricAdapter", "Position: " + position + ", CurrentLine: " + currentLine);
    }

    @Override
    public int getItemCount() {
        return lyrics.size();
    }

    public void setLyrics(List<LyricLine> lyrics) {
        this.lyrics = lyrics;
        // 添加2行空白在开头，1行空白在结尾，让当前行显示在第二行
        for (int i = 0; i < 2; i++) {
            this.lyrics.add(0, new LyricLine(0, ""));
        }
        this.lyrics.add(new LyricLine(0, ""));
        notifyDataSetChanged();
    }

    public void setCurrentLine(int position) {
        int oldPosition = currentLine;
        currentLine = position + 2; // 因为添加了2行空白
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvLyric;

        ViewHolder(View view) {
            super(view);
            tvLyric = (TextView) view;
        }
    }
} 