package com.example.musicplayer;

import android.content.Context;
import android.util.Log;
import android.os.Environment;

import java.util.ArrayList;
import java.util.List;
import java.io.File;

public class MusicScanner {
    private static final String TAG = "MusicScanner";

    public static List<MusicFile> scanMusic(Context context) {
        List<MusicFile> musicFiles = new ArrayList<>();
        
        Log.d(TAG, "Starting music scan...");
        
        // 使用 Environment 获取外部存储的 Music 目录
        File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        Log.d(TAG, "Scanning directory: " + musicDir.getAbsolutePath());
        
        if (!musicDir.exists() || !musicDir.isDirectory()) {
            Log.e(TAG, "Music directory does not exist or is not a directory");
            // 尝试创建目录
            if (!musicDir.mkdirs()) {
                Log.e(TAG, "Failed to create music directory");
            }
            return musicFiles;
        }
        
        // 获取所有 .mp3 和 .flac 文件
        File[] files = musicDir.listFiles((dir, name) -> {
            String lowercaseName = name.toLowerCase();
            return lowercaseName.endsWith(".mp3") || lowercaseName.endsWith(".flac");
        });
        
        if (files != null) {
            Log.d(TAG, "Found " + files.length + " files");
            for (File file : files) {
                Log.d(TAG, "Found music file: " + file.getPath());
                MusicFile musicFile = new MusicFile(file.getPath());
                musicFiles.add(musicFile);
            }
            Log.d(TAG, "Scan complete, found " + musicFiles.size() + " music files");
        } else {
            Log.e(TAG, "Failed to list files in music directory");
        }
        
        return musicFiles;
    }
} 