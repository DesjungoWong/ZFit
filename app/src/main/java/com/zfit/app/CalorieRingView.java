package com.zfit.app;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CalorieRingView extends View {

    private Paint trackPaint;
    private Paint progressPaint;
    private Paint textPaint;
    private Paint labelPaint;
    private Paint subLabelPaint;

    private int progress = 0;
    private int max = 2000;
    private int ringColor = 0xFFA100FF;
    private int trackColor = 0x12FFFFFF;
    private float strokeWidthPx;

    private String centerText = "0";
    private String centerLabel = "KCAL TODAY";
    private String subLabel = "";

    private RectF ovalRect = new RectF();

    public CalorieRingView(Context context) {
        super(context);
        init(context, null);
    }

    public CalorieRingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CalorieRingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        float density = context.getResources().getDisplayMetrics().density;
        strokeWidthPx = 18 * density;

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CalorieRingView);
            progress = a.getInt(R.styleable.CalorieRingView_ringProgress, 0);
            max = a.getInt(R.styleable.CalorieRingView_ringMax, 2000);
            ringColor = a.getColor(R.styleable.CalorieRingView_ringColor, 0xFFA100FF);
            trackColor = a.getColor(R.styleable.CalorieRingView_trackColor, 0x12FFFFFF);
            strokeWidthPx = a.getDimension(R.styleable.CalorieRingView_ringStrokeWidth, strokeWidthPx);
            a.recycle();
        }

        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setColor(trackColor);
        trackPaint.setStrokeWidth(strokeWidthPx);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setColor(ringColor);
        progressPaint.setStrokeWidth(strokeWidthPx);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        float density2 = context.getResources().getDisplayMetrics().density;

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFFF4F4F6);
        textPaint.setTextSize(42 * density2);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(0xFF9A9AA3);
        labelPaint.setTextSize(10 * density2);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setLetterSpacing(0.12f);

        subLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subLabelPaint.setColor(0xFF5C5C66);
        subLabelPaint.setTextSize(11 * density2);
        subLabelPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setProgress(int consumed, int goal) {
        this.progress = consumed;
        this.max = goal > 0 ? goal : 2000;
        this.centerText = String.valueOf(consumed);
        int remaining = goal - consumed;
        this.subLabel = remaining >= 0 ? (remaining + " remaining") : (Math.abs(remaining) + " over");
        invalidate();
    }

    public void setCenterLabel(String label) {
        this.centerLabel = label;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float padding = strokeWidthPx / 2f + 4;
        float size = Math.min(w, h);
        float left = (w - size) / 2f + padding;
        float top = (h - size) / 2f + padding;
        float right = (w + size) / 2f - padding;
        float bottom = (h + size) / 2f - padding;
        ovalRect.set(left, top, right, bottom);

        // Track
        canvas.drawArc(ovalRect, -90, 360, false, trackPaint);

        // Progress arc
        float sweep = max > 0 ? Math.min((float) progress / max, 1f) * 360f : 0f;
        if (sweep > 0) {
            canvas.drawArc(ovalRect, -90, sweep, false, progressPaint);
        }

        // Center text
        float cx = w / 2f;
        float cy = h / 2f;
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textH = fm.descent - fm.ascent;
        float labelH = labelPaint.getFontMetrics().descent - labelPaint.getFontMetrics().ascent;
        float subH = subLabelPaint.getFontMetrics().descent - subLabelPaint.getFontMetrics().ascent;
        float totalH = textH + 4 + labelH + 4 + subH;
        float startY = cy - totalH / 2f - fm.ascent;

        canvas.drawText(centerText, cx, startY, textPaint);
        canvas.drawText(centerLabel, cx, startY + textH + 4 - labelPaint.getFontMetrics().ascent, labelPaint);
        canvas.drawText(subLabel, cx, startY + textH + 4 + labelH + 4 - subLabelPaint.getFontMetrics().ascent, subLabelPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = (int)(220 * getContext().getResources().getDisplayMetrics().density);
        int w = resolveSize(size, widthMeasureSpec);
        int h = resolveSize(size, heightMeasureSpec);
        int sq = Math.min(w, h);
        setMeasuredDimension(sq, sq);
    }
}
