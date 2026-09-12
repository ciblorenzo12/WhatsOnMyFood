package com.ciblorenzo.whatsonmyfood.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import com.ciblorenzo.whatsonmyfood.R;

/** Draws only an existing score; null is an explicit unknown state. */
public class ScoreRing extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Integer score;
    public ScoreRing(Context context, AttributeSet attrs) { super(context, attrs); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES); }
    public void setScore(Integer value) {
        score = value == null || value < 0 || value > 100 ? null : value;
        setContentDescription(score == null ? getContext().getString(R.string.ui_score_unknown)
                : getContext().getString(R.string.ui_score_out_of, score));
        invalidate();
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float size = Math.min(getWidth(), getHeight()), stroke = size * .065f;
        float left = (getWidth() - size) / 2f + stroke, top = (getHeight() - size) / 2f + stroke;
        RectF bounds = new RectF(left, top, left + size - stroke * 2, top + size - stroke * 2);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(stroke); paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(getContext().getColor(R.color.divider)); canvas.drawArc(bounds, 0, 360, false, paint);
        paint.setColor(getContext().getColor(R.color.colorPrimary));
        if (score != null) canvas.drawArc(bounds, -90, score * 3.6f, false, paint);
        paint.setStyle(Paint.Style.FILL); paint.setColor(getContext().getColor(R.color.text_primary));
        paint.setTextAlign(Paint.Align.CENTER); paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setTextSize(size * .26f);
        canvas.drawText(score == null ? "—" : String.valueOf(score), getWidth()/2f, getHeight()/2f + size*.04f, paint);
        paint.setTextSize(size * .11f); paint.setTypeface(android.graphics.Typeface.DEFAULT);
        canvas.drawText(getContext().getString(R.string.ui_score_denominator), getWidth()/2f, getHeight()/2f + size*.22f, paint);
    }
}
