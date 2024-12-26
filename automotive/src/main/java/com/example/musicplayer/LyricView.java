package com.example.musicplayer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class LyricView extends View {
    private Paint paint;
    private List<String> lyrics = new ArrayList<>();
    private int currentLine = 0;

    public LyricView(Context context) {
        super(context);
        init();
    }

    public LyricView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTextSize(40);
        paint.setTextAlign(Paint.Align.CENTER);
    }

    public void loadLyric(String musicPath) {
        lyrics.clear();
        String lrcPath = musicPath.substring(0, musicPath.lastIndexOf(".")) + ".lrc";
        File lrcFile = new File(lrcPath);
        
        if (lrcFile.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(lrcFile));
                String line;
                while ((line = reader.readLine()) != null) {
                    // 简单处理，移除时间标签
                    line = line.replaceAll("\\[.*?\\]", "").trim();
                    if (!line.isEmpty()) {
                        lyrics.add(line);
                    }
                }
                reader.close();
                setVisibility(VISIBLE);
            } catch (Exception e) {
                e.printStackTrace();
                setVisibility(GONE);
            }
        } else {
            setVisibility(GONE);
        }
        invalidate();
    }

    public void setCurrentLine(int line) {
        currentLine = Math.min(line, lyrics.size() - 1);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (lyrics.isEmpty()) return;

        int centerY = getHeight() / 2;
        int centerX = getWidth() / 2;

        // 绘制当前行和周围的几行
        for (int i = -2; i <= 2; i++) {
            int index = currentLine + i;
            if (index >= 0 && index < lyrics.size()) {
                paint.setColor(i == 0 ? 
                    getResources().getColor(R.color.primary, null) : 
                    getResources().getColor(R.color.text_secondary, null));
                paint.setAlpha(i == 0 ? 255 : 128);
                canvas.drawText(lyrics.get(index), centerX, centerY + i * 60, paint);
            }
        }
    }
} 