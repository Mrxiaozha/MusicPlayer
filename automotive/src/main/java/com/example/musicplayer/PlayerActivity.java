package com.example.musicplayer;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;
import android.view.WindowManager;
import android.widget.ImageView;
import android.view.KeyEvent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.content.Context;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.DividerItemDecoration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class PlayerActivity extends AppCompatActivity {
    private static final String TAG = "PlayerActivity";
    private MediaPlayer mediaPlayer;
    private MaterialButton btnPlayPause;
    private MaterialButton btnNext;
    private MaterialButton btnPrevious;
    private MaterialButton btnPlayMode;
    private MaterialButton btnPlaylist;
    private Slider seekBar;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;
    private TextView tvSongName;
    private List<LyricLine> lyricLines = new ArrayList<>();
    private int currentLyricIndex = 0;
    
    private boolean isPlaying = false;
    private boolean isRandomMode = false;
    private Handler handler = new Handler();
    private Handler uiHandler = new Handler(Looper.getMainLooper());
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private static final int REQUEST_PICK_MUSIC = 1;
    private static final int PERMISSION_REQUEST_CODE = 123;

    private List<MusicFile> playlist = new ArrayList<>();
    private int currentTrackIndex = -1;

    private static final String PREFS_NAME = "MusicPlayerPrefs";
    private static final String KEY_CURRENT_SONG = "currentSong";
    private static final String KEY_SONG_POSITION = "songPosition";
    private static final String KEY_IS_PLAYING = "isPlaying";
    private static final String KEY_PLAY_MODE = "playMode";
    private static final String KEY_PLAYLIST = "playlist";
    private static final String KEY_CURRENT_INDEX = "currentIndex";
    private static final String KEY_AUTO_START = "auto_start";
    private boolean autoStart = true;

    private BottomSheetDialog playlistDialog;

    private RecyclerView lyricRecyclerView;
    private LyricAdapter lyricAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_player_material);
            Log.d(TAG, "Setting content view");
            
            // 读取自动启动设置
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            autoStart = prefs.getBoolean(KEY_AUTO_START, true);
            
            initializeViews();
            setupListeners();
            startProgressUpdate();
            
            // 检查并请求权限
            checkAndRequestPermissions();
            
            // 恢复上次的播放状态
            restorePlaybackState();
            
            // 如果开启了自动启动，且有音乐文件但没有在播放，自动开始播放
            if (autoStart && mediaPlayer != null && !isPlaying && currentTrackIndex != -1) {
                mediaPlayer.start();
                isPlaying = true;
                btnPlayPause.setIcon(getDrawable(R.drawable.ic_pause));
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: ", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从后台恢复时，如果有音乐文件但没有在播放，自动开始播放
        if (mediaPlayer != null && !isPlaying && currentTrackIndex != -1) {
            mediaPlayer.start();
            isPlaying = true;
            btnPlayPause.setIcon(getDrawable(R.drawable.ic_pause));
        }
    }

    private void initializeViews() {
        try {
            Log.d(TAG, "Initializing views");
            mediaPlayer = new MediaPlayer();
            btnPlayPause = findViewById(R.id.btnPlayPause);
            btnNext = findViewById(R.id.btnNext);
            btnPrevious = findViewById(R.id.btnPrevious);
            btnPlayMode = findViewById(R.id.btnPlayMode);
            btnPlaylist = findViewById(R.id.btnPlaylist);
            seekBar = findViewById(R.id.seekBar);
            tvCurrentTime = findViewById(R.id.tvCurrentTime);
            tvTotalTime = findViewById(R.id.tvTotalTime);
            tvSongName = findViewById(R.id.tvSongName);
            lyricRecyclerView = findViewById(R.id.lyricRecyclerView);
            lyricAdapter = new LyricAdapter(this);
            lyricAdapter.setOnLyricLongClickListener(() -> {
                if (currentTrackIndex >= 0 && currentTrackIndex < playlist.size()) {
                    showLyricOptionsDialog(playlist.get(currentTrackIndex).getPath());
                }
            });
            lyricRecyclerView.setAdapter(lyricAdapter);
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            lyricRecyclerView.setLayoutManager(layoutManager);
            
            // 初始化默认显示
            tvSongName.setText("暂无音乐播放");
            lyricLines.clear();
            lyricLines.add(new LyricLine(0, "暂无音乐播放"));
            lyricAdapter.setLyrics(lyricLines);
            lyricRecyclerView.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Log.e(TAG, "Error in initializeViews: ", e);
        }
    }

    private void setupListeners() {
        try {
            Log.d(TAG, "Setting up listeners");
            seekBar.addOnChangeListener(new Slider.OnChangeListener() {
                @Override
                public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                    if (fromUser && mediaPlayer != null) {
                        mediaPlayer.seekTo((int) value);
                        updateTimeLabels();
                    }
                }
            });

            btnPlayPause.setOnClickListener(v -> {
                if (mediaPlayer != null) {
                    if (isPlaying) {
                        mediaPlayer.pause();
                        btnPlayPause.setIcon(getDrawable(R.drawable.ic_play));
                        savePlaybackState();
                        // 用户手动暂停时，关闭自动启动
                        autoStart = false;
                        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                        editor.putBoolean(KEY_AUTO_START, false);
                        editor.apply();
                        Toast.makeText(this, "已关闭自动播放", Toast.LENGTH_SHORT).show();
                    } else {
                        mediaPlayer.start();
                        btnPlayPause.setIcon(getDrawable(R.drawable.ic_pause));
                        // 用户手动播放时，重新开启自动启动
                        autoStart = true;
                        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                        editor.putBoolean(KEY_AUTO_START, true);
                        editor.apply();
                    }
                    isPlaying = !isPlaying;
                }
            });

            btnNext.setOnClickListener(v -> {
                Log.d(TAG, "Next button clicked");
                if (isRandomMode) {
                    playRandomSong();
                } else {
                    playNextSong();
                }
            });

            btnPrevious.setOnClickListener(v -> {
                Log.d(TAG, "Previous button clicked");
                playPreviousSong();
            });

            btnPlayMode.setOnClickListener(v -> {
                isRandomMode = !isRandomMode;
                updatePlayModeButton();
                Toast.makeText(this, 
                    isRandomMode ? "已切换到随机播放" : "已切换到列表循环", 
                    Toast.LENGTH_SHORT).show();
            });

            btnPlaylist.setOnClickListener(v -> {
                String permission = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU ?
                    Manifest.permission.READ_MEDIA_AUDIO :
                    Manifest.permission.READ_EXTERNAL_STORAGE;

                if (ContextCompat.checkSelfPermission(this, permission)
                        == PackageManager.PERMISSION_GRANTED) {
                    showPlaylistBottomSheet();
                } else {
                    ActivityCompat.requestPermissions(this,
                        new String[]{permission},
                        PERMISSION_REQUEST_CODE);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in setupListeners", e);
        }
    }

    @Override
    protected void onDestroy() {
        savePlaybackState();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
            // 恢复默认显示
            tvSongName.setText("暂无音乐播放");
            lyricLines.clear();
            lyricLines.add(new LyricLine(0, "暂无音乐播放"));
            lyricAdapter.setLyrics(lyricLines);
        }
        handler.removeCallbacksAndMessages(null);
        scheduler.shutdown();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_MUSIC && resultCode == RESULT_OK && data != null) {
            String musicPath = data.getStringExtra("musicPath");
            playlist = (List<MusicFile>) data.getSerializableExtra("playlist");
            currentTrackIndex = data.getIntExtra("currentIndex", 0);
            playMusic(musicPath);
            updatePlayModeButton();
        }
    }

    private void playMusic(String path) {
        try {
            Log.d(TAG, "Starting to play music: " + path);
            if (mediaPlayer != null) {
                mediaPlayer.reset();
            } else {
                mediaPlayer = new MediaPlayer();
            }
            
            // 检查文件是否存在
            File musicFile = new File(path);
            if (!musicFile.exists()) {
                throw new Exception("音乐文件不存在");
            }
            
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            mediaPlayer.start();
            
            isPlaying = true;
            btnPlayPause.setText("");
            btnPlayPause.setIcon(getDrawable(R.drawable.ic_pause));
            
            // 设置歌曲名称（添加序号）
            String fileName = new File(path).getName();
            String title = fileName.substring(0, fileName.lastIndexOf("."));
            tvSongName.setText(String.format("%d. %s", currentTrackIndex + 1, title));
            
            // 更新进度条
            seekBar.setValueTo(mediaPlayer.getDuration());
            seekBar.setValueFrom(0);
            updateTimeLabels();

            // 设置播放完成监听器
            mediaPlayer.setOnCompletionListener(mp -> {
                if (isRandomMode) {
                    playRandomSong();
                } else {
                    playNextSong();
                }
            });

            // 保存当前播放状态
            savePlaybackState();

            // 加载歌词
            loadLyrics(path);

        } catch (Exception e) {
            Log.e(TAG, "Error playing music: " + path + ", Error: " + e.getMessage(), e);
            String errorMsg = e.getMessage();
            if (path.toLowerCase().endsWith(".flac") && 
                errorMsg != null && errorMsg.contains("Unsupported")) {
                Toast.makeText(this, "此设备不支持播放 FLAC 格式", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "播放失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateTimeLabels() {
        if (mediaPlayer != null) {
            tvCurrentTime.setText(formatTime(mediaPlayer.getCurrentPosition()));
            tvTotalTime.setText(formatTime(mediaPlayer.getDuration()));
        }
    }

    private String formatTime(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60)) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void startProgressUpdate() {
        Runnable updateTask = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && isPlaying) {
                    uiHandler.post(() -> {
                        seekBar.setValue(mediaPlayer.getCurrentPosition());
                        updateTimeLabels();
                        updateLyricDisplay();
                        savePlaybackState();
                    });
                }
            }
        };
        scheduler.scheduleAtFixedRate(updateTask, 0, 50, TimeUnit.MILLISECONDS);
        scheduler.scheduleAtFixedRate(() -> {
            if (mediaPlayer != null && isPlaying) {
                savePlaybackState();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void playNextSong() {
        if (playlist == null || playlist.isEmpty()) {
            Toast.makeText(this, "播放列表为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!playlist.isEmpty()) {
            currentTrackIndex = (currentTrackIndex + 1) % playlist.size();
            MusicFile nextSong = playlist.get(currentTrackIndex);
            playMusic(nextSong.getPath());
        }
    }

    private void playPreviousSong() {
        if (playlist == null || playlist.isEmpty()) {
            Toast.makeText(this, "播放列表为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!playlist.isEmpty()) {
            currentTrackIndex = (currentTrackIndex - 1 + playlist.size()) % playlist.size();
            MusicFile previousSong = playlist.get(currentTrackIndex);
            playMusic(previousSong.getPath());
        }
    }

    private void playRandomSong() {
        if (playlist == null || playlist.isEmpty()) {
            Toast.makeText(this, "播放列表为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!playlist.isEmpty()) {
            Random random = new Random();
            int newIndex;
            do {
                newIndex = random.nextInt(playlist.size());
            } while (newIndex == currentTrackIndex && playlist.size() > 1);
            
            currentTrackIndex = newIndex;
            MusicFile randomSong = playlist.get(currentTrackIndex);
            playMusic(randomSong.getPath());
        }
    }

    private void checkAndRequestPermissions() {
        Log.d(TAG, "Checking permissions");
        // 需要的所有权限
        List<String> permissions = new ArrayList<>();
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO);
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        
        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        
        if (!allGranted) {
            Log.d(TAG, "Requesting permissions");
            ActivityCompat.requestPermissions(this,
                permissions.toArray(new String[0]),
                PERMISSION_REQUEST_CODE);
        } else {
            Log.d(TAG, "All permissions granted");
            refreshLocalMusicList();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                Log.d(TAG, "All permissions granted, starting scan");
                refreshLocalMusicList();
            } else {
                Log.d(TAG, "Some permissions denied");
                Toast.makeText(this, "需要存储权限才能扫描音乐文件", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void savePlaybackState() {
        try {
            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
            if (mediaPlayer != null && currentTrackIndex >= 0 && !playlist.isEmpty()) {
                editor.putString(KEY_CURRENT_SONG, playlist.get(currentTrackIndex).getPath());
                editor.putInt(KEY_SONG_POSITION, mediaPlayer.getCurrentPosition());
                editor.putBoolean(KEY_IS_PLAYING, isPlaying);
                editor.putBoolean(KEY_PLAY_MODE, isRandomMode);
                StringBuilder playlistStr = new StringBuilder();
                for (MusicFile file : playlist) {
                    playlistStr.append(file.getPath()).append(",");
                }
                editor.putString(KEY_PLAYLIST, playlistStr.toString());
                editor.putInt(KEY_CURRENT_INDEX, currentTrackIndex);
            }
            editor.commit();
        } catch (Exception e) {
            Log.e(TAG, "Error saving playback state", e);
        }
    }

    private void restorePlaybackState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String lastSongPath = prefs.getString(KEY_CURRENT_SONG, null);
        int lastPosition = prefs.getInt(KEY_SONG_POSITION, 0);
        isRandomMode = prefs.getBoolean(KEY_PLAY_MODE, false);
        currentTrackIndex = prefs.getInt(KEY_CURRENT_INDEX, -1);

        if (lastSongPath != null) {
            try {
                mediaPlayer.setDataSource(lastSongPath);
                mediaPlayer.prepare();
                mediaPlayer.seekTo(lastPosition);
                // 总是开始播放
                mediaPlayer.start();
                isPlaying = true;
                btnPlayPause.setIcon(getDrawable(R.drawable.ic_pause));

                // 设置歌曲名称
                String fileName = new File(lastSongPath).getName();
                String title = fileName.substring(0, fileName.lastIndexOf("."));
                tvSongName.setText(String.format("%d. %s", currentTrackIndex + 1, title));

                // 更新进度条
                seekBar.setValueTo(mediaPlayer.getDuration());
                seekBar.setValueFrom(0);
                seekBar.setValue(lastPosition);
                updateTimeLabels();

                // 加载歌词
                loadLyrics(lastSongPath);
            } catch (Exception e) {
                Log.e(TAG, "Error restoring playback state", e);
            }
        }

        updatePlayModeButton();
    }

    @Override
    protected void onPause() {
        super.onPause();
        savePlaybackState();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.player_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_scan) {
            // 检查权限并扫描音乐文件
            String permission = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_AUDIO :
                Manifest.permission.READ_EXTERNAL_STORAGE;

            if (ContextCompat.checkSelfPermission(this, permission)
                    == PackageManager.PERMISSION_GRANTED) {
                startActivityForResult(new Intent(this, PlaylistActivity.class), REQUEST_PICK_MUSIC);
                Toast.makeText(this, "正在扫描音乐文件...", Toast.LENGTH_SHORT).show();
            } else {
                ActivityCompat.requestPermissions(this,
                    new String[]{permission},
                    PERMISSION_REQUEST_CODE);
            }
            return true;
        } else if (id == R.id.action_exit) {
            // 显示退出确认对话框
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("退出确认")
                .setMessage("确定要退出程序吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    // 保存播放状态
                    savePlaybackState();
                    // 停止播放
                    if (mediaPlayer != null) {
                        if (mediaPlayer.isPlaying()) {
                            mediaPlayer.stop();
                        }
                        mediaPlayer.release();
                        mediaPlayer = null;
                    }
                    // 结束所有活动
                    finishAffinity();
                })
                .setNegativeButton("取消", null)
                .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updatePlayModeButton() {
        if (isRandomMode) {
            btnPlayMode.setText("随机播放");
            btnPlayMode.setIcon(getDrawable(R.drawable.ic_shuffle));
        } else {
            btnPlayMode.setText("列表循环");
            btnPlayMode.setIcon(getDrawable(R.drawable.ic_repeat));
        }
    }

    private void showPlaylistBottomSheet() {
        try {
            playlistDialog = new BottomSheetDialog(this);
            View view = getLayoutInflater().inflate(R.layout.bottom_sheet_playlist, null);
            RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            
            // 设置完全展开
            playlistDialog.setOnShowListener(dialog -> {
                BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialog;
                FrameLayout bottomSheet = bottomSheetDialog.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet);
                if (bottomSheet != null) {
                    BottomSheetBehavior<FrameLayout> behavior = 
                        BottomSheetBehavior.from(bottomSheet);
                    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    behavior.setDraggable(false);
                }
            });
            
            // 设置适配器
            MusicAdapter adapter = new MusicAdapter(playlist, musicFile -> {
                int index = playlist.indexOf(musicFile);
                if (index != -1) {
                    currentTrackIndex = index;
                    playMusic(musicFile.getPath());
                }
                playlistDialog.dismiss();
            });
            
            // 设置当前播放项
            adapter.setCurrentPlayingPosition(currentTrackIndex);
            recyclerView.setAdapter(adapter);
            
            // 定位到当前播放的歌曲
            if (currentTrackIndex != -1) {
                recyclerView.post(() -> {
                    recyclerView.scrollToPosition(currentTrackIndex);
                });
            }
            
            playlistDialog.setContentView(view);
            playlistDialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing playlist dialog: ", e);
            Toast.makeText(this, "显示播放列表失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshLocalMusicList() {
        Log.d(TAG, "Starting refreshLocalMusicList");
        // 显示加载提示
        Toast.makeText(this, "正在扫描本地音乐...", Toast.LENGTH_SHORT).show();
        
        // 在后台线程中扫描音乐文件
        new Thread(() -> {
            Log.d(TAG, "Starting music scan thread");
            List<MusicFile> newPlaylist = MusicScanner.scanMusic(this);
            Log.d(TAG, "Scan complete, found " + (newPlaylist != null ? newPlaylist.size() : 0) + " files");
            
            // 在主线程中更新UI
            runOnUiThread(() -> {
                if (newPlaylist == null || newPlaylist.isEmpty()) {
                    Log.d(TAG, "No music files found");
                    Toast.makeText(this, "未找到音乐文件", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                Log.d(TAG, "Updating playlist with " + newPlaylist.size() + " files");
                playlist.clear();
                playlist.addAll(newPlaylist);
                
                // 检查播放列表是否真的更新了
                Log.d(TAG, "Current playlist size after update: " + playlist.size());
                for (MusicFile file : playlist) {
                    Log.d(TAG, "Playlist item: " + file.getTitle());
                }
                
                // 重新显示播放列表对话框
                showPlaylistBottomSheet();
                
                Toast.makeText(this, "已更新播放列表", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void downloadAndSaveLyrics(String musicPath) {
        try {
            // 获取完整文件名（不包含后缀）
            String fileName = new File(musicPath).getName();
            String songName = fileName;
            if (songName.toLowerCase().endsWith(".mp3")) {
                songName = songName.substring(0, songName.length() - 4);
            }
            
            // 构建请求URL，使用URLEncoder处理特殊字符
            String urlStr = "https://api.lrc.cx/lyrics?title=" + 
                java.net.URLEncoder.encode(songName, "UTF-8")
                    .replace("+", "%20"); // 将空格编码为%20不是+
            
            Log.d(TAG, "Requesting lyrics URL: " + urlStr);
            
            // 在后台线程中下载歌词
            new Thread(() -> {
                try {
                    java.net.URL url = new java.net.URL(urlStr);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                    
                    // 读取响应
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line).append("\n");
                    }
                    reader.close();
                    
                    // 检查响应是否为空
                    if (response.length() == 0) {
                        throw new Exception("No lyrics found");
                    }
                    
                    // 保存歌词到文件
                    String lrcPath = musicPath.substring(0, musicPath.lastIndexOf(".")) + ".lrc";
                    java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(
                        new java.io.FileOutputStream(lrcPath), "GBK");
                    writer.write(response.toString());
                    writer.close();
                    
                    // 在主线程中重新加载歌词
                    runOnUiThread(() -> {
                        loadLyrics(musicPath);
                        Toast.makeText(this, "已下载歌词", Toast.LENGTH_SHORT).show();
                    });
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error downloading lyrics: ", e);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "未找到歌词", Toast.LENGTH_SHORT).show();
                        lyricLines.clear();
                        lyricLines.add(new LyricLine(0, "未找到歌词"));
                        lyricAdapter.setLyrics(lyricLines);
                    });
                }
            }).start();
            
        } catch (Exception e) {
            Log.e(TAG, "Error preparing lyrics download: ", e);
        }
    }

    private void loadLyrics(String musicPath) {
        lyricLines.clear();
        Log.d(TAG, "Loading lyrics for: " + musicPath);
        String lrcPath = musicPath.substring(0, musicPath.lastIndexOf(".")) + ".lrc";
        Log.d(TAG, "Looking for LRC file: " + lrcPath);
        File lrcFile = new File(lrcPath);
        
        if (lrcFile.exists()) {
            Log.d(TAG, "LRC file found!");
            try {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                        new FileInputStream(lrcFile), "GBK"
                    )
                );
                String line;
                while ((line = reader.readLine()) != null) {
                    Log.d(TAG, "Read line: " + line);
                    // 解析所有时间标签，包括元数据
                    if (line.startsWith("[")) {
                        String timeStr = line.substring(1, line.indexOf("]"));
                        String text = line.substring(line.indexOf("]") + 1).trim();
                        
                        // 检查是否是时间标签
                        if (timeStr.matches("\\d{2}:\\d{2}\\.\\d{3}")) {
                            long time = parseTime(timeStr);
                            if (!text.isEmpty()) {
                                lyricLines.add(new LyricLine(time, text));
                                Log.d(TAG, "Added lyric line: " + text + " at time: " + time);
                            }
                        }
                    }
                }
                reader.close();
                // 按时间排序歌词
                Collections.sort(lyricLines, (a, b) -> Long.compare(a.time, b.time));
                if (!lyricLines.isEmpty()) {
                    Log.d(TAG, "Setting first lyric: " + lyricLines.get(0).text);
                    lyricAdapter.setLyrics(lyricLines);
                    lyricRecyclerView.setVisibility(View.VISIBLE);
                    // 立即显示第一行歌词
                    currentLyricIndex = 0;
                    lyricAdapter.setCurrentLine(0);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error reading LRC file: ", e);
                e.printStackTrace();
                lyricLines.clear();
                lyricLines.add(new LyricLine(0, "该歌曲暂未找到歌词"));
                lyricAdapter.setLyrics(lyricLines);
            }
        } else {
            Log.d(TAG, "No LRC file found");
            // 尝试从网络下载歌词
            downloadAndSaveLyrics(musicPath);
            // 显示临时提示
            lyricLines.clear();
            lyricLines.add(new LyricLine(0, "正在搜索歌词..."));
            lyricAdapter.setLyrics(lyricLines);
        }
    }

    private long parseTime(String timeStr) {
        // 解析 mm:ss.xxx 格式的时间
        String[] parts = timeStr.split(":");
        String[] secondParts = parts[1].split("\\.");
        int minutes = Integer.parseInt(parts[0]);
        int seconds = Integer.parseInt(secondParts[0]);
        // 确保毫秒部分是3位数
        String msStr = secondParts[1];
        while (msStr.length() < 3) msStr += "0";
        int milliseconds = Integer.parseInt(msStr);
        Log.d(TAG, String.format("Parsing time: %s -> %02d:%02d.%03d -> %dms",
            timeStr, minutes, seconds, milliseconds,
            minutes * 60000 + seconds * 1000 + milliseconds));
        return minutes * 60000 + seconds * 1000 + milliseconds;
    }

    private void updateLyricDisplay() {
        if (lyricLines.isEmpty()) return;
        
        long currentTime = mediaPlayer.getCurrentPosition();
        
        int newIndex = -1;
        for (int j = 0; j < lyricLines.size(); j++) {
            long currentLyricTime = lyricLines.get(j).time;
            long nextLyricTime = (j < lyricLines.size() - 1) ? 
                lyricLines.get(j + 1).time : Long.MAX_VALUE;
            
            if (currentTime >= currentLyricTime && currentTime < nextLyricTime) {
                newIndex = j;
                break;
            }
        }

        if (newIndex >= 0 && newIndex < lyricLines.size() && newIndex != currentLyricIndex) {
            currentLyricIndex = newIndex;
            // 保持当前行在中间位置
            lyricRecyclerView.smoothScrollToPosition(Math.min(currentLyricIndex + 2, lyricLines.size() - 1));
        }
    }

    private void scanMusicFiles(File directory) {
        // 使用 MusicScanner 来扫描音乐文件
        playlist.clear();
        playlist.addAll(MusicScanner.scanMusic(this));
    }

    @Override
    protected void onStop() {
        super.onStop();
        savePlaybackState();
        if (mediaPlayer != null && isPlaying) {
            mediaPlayer.pause();
            isPlaying = false;
            btnPlayPause.setIcon(getDrawable(R.drawable.ic_play));
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                // 下一曲
                if (isRandomMode) {
                    playRandomSong();
                } else {
                    playNextSong();
                }
                return true;
                
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                // 上一曲
                playPreviousSong();
                return true;
                
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                // 播放/暂停
                if (mediaPlayer != null) {
                    if (isPlaying) {
                        mediaPlayer.pause();
                        btnPlayPause.setIcon(getDrawable(R.drawable.ic_play));
                        savePlaybackState();
                    } else {
                        mediaPlayer.start();
                        btnPlayPause.setIcon(getDrawable(R.drawable.ic_pause));
                    }
                    isPlaying = !isPlaying;
                }
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onStart() {
        super.onStart();
        // 注册音频焦点监听
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        AudioAttributes playbackAttributes = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build();
            
        AudioFocusRequest focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(playbackAttributes)
            .setOnAudioFocusChangeListener(focusChange -> {
                switch (focusChange) {
                    case AudioManager.AUDIOFOCUS_LOSS:
                        // 长时间失去音频焦点，暂停播放
                        if (mediaPlayer != null && isPlaying) {
                            mediaPlayer.pause();
                            btnPlayPause.setIcon(getDrawable(R.drawable.ic_play));
                            isPlaying = false;
                        }
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        // 暂时失去音频焦点，暂停播放
                        if (mediaPlayer != null && isPlaying) {
                            mediaPlayer.pause();
                            btnPlayPause.setIcon(getDrawable(R.drawable.ic_play));
                            isPlaying = false;
                        }
                        break;
                    case AudioManager.AUDIOFOCUS_GAIN:
                        // 重新获得音频焦点，恢复播放
                        if (mediaPlayer != null && !isPlaying) {
                            mediaPlayer.start();
                            btnPlayPause.setIcon(getDrawable(R.drawable.ic_pause));
                            isPlaying = true;
                        }
                        break;
                }
            })
            .build();
            
        audioManager.requestAudioFocus(focusRequest);
    }

    private void showLyricOptionsDialog(String musicPath) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("歌词选项")
            .setItems(new String[]{"重新获取歌词"}, (dialog, which) -> {
                if (which == 0) {
                    // 删除现有歌词文件
                    String lrcPath = musicPath.substring(0, musicPath.lastIndexOf(".")) + ".lrc";
                    File lrcFile = new File(lrcPath);
                    if (lrcFile.exists()) {
                        lrcFile.delete();
                    }
                    // 重新下载歌词
                    downloadAndSaveLyrics(musicPath);
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
} 