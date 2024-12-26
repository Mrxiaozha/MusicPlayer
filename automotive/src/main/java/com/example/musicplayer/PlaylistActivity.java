package com.example.musicplayer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PlaylistActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private List<MusicFile> musicFiles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadMusicFiles();

        MusicAdapter adapter = new MusicAdapter(musicFiles, musicFile -> {
            // 发送播放意图回到PlayerActivity
            Intent resultIntent = new Intent();
            resultIntent.putExtra("musicPath", musicFile.getPath());
            resultIntent.putExtra("playlist", new ArrayList<>(musicFiles));
            resultIntent.putExtra("currentIndex", musicFiles.indexOf(musicFile));
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        recyclerView.setAdapter(adapter);
    }

    private void loadMusicFiles() {
        try {
            Log.d("PlaylistActivity", "Loading music files...");
            File musicDir = new File(Environment.getExternalStorageDirectory(), "Music");
            Log.d("PlaylistActivity", "Music directory path: " + musicDir.getAbsolutePath());
            
            if (musicDir.exists()) {
                Log.d("PlaylistActivity", "Music directory exists");
                scanMusicFiles(musicDir);
            } else {
                Log.e("PlaylistActivity", "Music directory does not exist");
            }
        } catch (Exception e) {
            Log.e("PlaylistActivity", "Error loading music files", e);
        }
    }

    private void scanMusicFiles(File directory) {
        // 使用 MusicScanner 来扫描音乐文件
        musicFiles.clear();
        musicFiles.addAll(MusicScanner.scanMusic(this));
    }
} 