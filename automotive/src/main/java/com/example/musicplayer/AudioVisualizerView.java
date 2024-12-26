package com.example.musicplayer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.media.audiofx.Visualizer;

public class AudioVisualizerView extends View {
    private Paint paint;
    private Visualizer visualizer;
    private byte[] bytes;
    private float[] points;
    private LinearGradient gradient;
    private float amplitude = 1.0f;

    public AudioVisualizerView(Context context) {
        super(context);
        init();
    }

    public AudioVisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setStrokeWidth(2f);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        bytes = null;
        points = null;
    }

    public void setVisualizer(int audioSessionId) {
        if (visualizer != null) {
            visualizer.release();
        }

        visualizer = new Visualizer(audioSessionId);
        visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
        visualizer.setDataCaptureListener(
            new Visualizer.OnDataCaptureListener() {
                @Override
                public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
                    bytes = waveform;
                    invalidate();
                }

                @Override
                public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                    // 不使用FFT数据
                }
            }, Visualizer.getMaxCaptureRate() / 2, true, false);

        visualizer.setEnabled(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        gradient = new LinearGradient(0, 0, 0, h,
            new int[]{
                Color.parseColor("#336699FF"),  // 淡蓝色
                Color.parseColor("#3300FF99"),  // 淡绿色
                Color.parseColor("#33FF6699"),  // 淡粉色
                Color.parseColor("#33FF9966")   // 淡橙色
            },
            new float[]{0f, 0.33f, 0.66f, 1f},
            Shader.TileMode.MIRROR);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bytes == null) {
            return;
        }

        paint.setShader(gradient);
        paint.setAlpha(180);  // 设置整体透明度

        if (points == null || points.length < bytes.length * 4) {
            points = new float[bytes.length * 4];
        }

        int width = getWidth();
        int height = getHeight();
        float strokeWidth = (float) width / bytes.length;
        paint.setStrokeWidth(strokeWidth);

        for (int i = 0; i < bytes.length - 1; i++) {
            float scaledHeight = ((byte) (bytes[i] + 128)) * (height / 4) / 128.0f * amplitude;
            float nextScaledHeight = ((byte) (bytes[i + 1] + 128)) * (height / 4) / 128.0f * amplitude;

            float startX = i * strokeWidth;
            float startY = height/2 + scaledHeight;
            float endX = (i + 1) * strokeWidth;
            float endY = height/2 + nextScaledHeight;
            
            // 上半部分
            points[i * 4] = startX;
            points[i * 4 + 1] = startY;
            points[i * 4 + 2] = endX;
            points[i * 4 + 3] = endY;

            // 下半部分（镜像）
            canvas.drawLine(startX, height/2 - scaledHeight, endX, height/2 - nextScaledHeight, paint);
        }

        canvas.drawLines(points, paint);
    }

    public void release() {
        if (visualizer != null) {
            visualizer.release();
            visualizer = null;
        }
    }

    public void setAmplitude(float amplitude) {
        this.amplitude = amplitude;
    }
} 