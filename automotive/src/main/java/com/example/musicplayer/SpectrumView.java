package com.example.musicplayer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.media.audiofx.Visualizer;
import android.util.Log;

public class SpectrumView extends View {
    private Paint paint;
    private Visualizer visualizer;
    private byte[] fftData;
    private float[] points;
    private LinearGradient gradient;

    public SpectrumView(Context context) {
        super(context);
        init();
    }

    public SpectrumView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setStrokeWidth(4f);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        fftData = null;
    }

    public void setVisualizer(int audioSessionId) {
        Log.d("SpectrumView", "Setting visualizer with session ID: " + audioSessionId);
        if (visualizer != null) {
            visualizer.release();
        }

        try {
            visualizer = new Visualizer(audioSessionId);
            Log.d("SpectrumView", "Visualizer created successfully");
            visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
            visualizer.setDataCaptureListener(
                new Visualizer.OnDataCaptureListener() {
                    @Override
                    public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
                    }

                    @Override
                    public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                        Log.d("SpectrumView", "FFT data received, length: " + (fft != null ? fft.length : 0));
                        fftData = fft;
                        invalidate();
                    }
                }, Visualizer.getMaxCaptureRate() / 2, false, true);

            visualizer.setEnabled(true);
            Log.d("SpectrumView", "Visualizer enabled");
        } catch (Exception e) {
            Log.e("SpectrumView", "Error setting up visualizer", e);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        gradient = new LinearGradient(0, h, 0, 0,
            new int[]{
                getResources().getColor(R.color.spectrum_start, null),
                getResources().getColor(R.color.spectrum_middle, null),
                getResources().getColor(R.color.spectrum_end, null)
            },
            null, Shader.TileMode.CLAMP);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (fftData == null) return;

        paint.setShader(gradient);
        
        int width = getWidth();
        int height = getHeight();
        int barCount = 32;  // 减少频谱条数，使每个条更宽
        int barWidth = (width - (barCount - 1) * 4) / barCount;
        int barSpacing = 8;  // 增加间距
        
        for (int i = 0; i < barCount; i++) {
            if (i >= fftData.length) break;
            
            // 使用对数计算来增强视觉效果
            int magnitude = Math.abs(fftData[i]);
            float scaledHeight = (float) (Math.log10(magnitude + 1) * height / 1.5f);  // 增加高度
            // 添加最小高度
            scaledHeight = Math.max(scaledHeight, 10);  // 增加最小高度
            
            float left = i * (barWidth + barSpacing);
            float right = left + barWidth;
            float top = height - scaledHeight;
            float bottom = height;
            
            // 绘制倒影效果
            canvas.drawRect(left, 0, right, scaledHeight, paint);
            canvas.drawRect(left, top, right, bottom, paint);
        }
        
        // 强制持续刷新
        invalidate();
    }

    public void release() {
        if (visualizer != null) {
            visualizer.release();
            visualizer = null;
        }
    }
} 