package com.ciblorenzo.whatsonmyfood.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

/** Shared window-width-aware shell. It does not use physical display dimensions. */
public class ResponsiveContentContainer extends FrameLayout {
    private final LinearLayout row;
    private final LinearLayout body;
    private final FrameLayout content;
    private int maxWidthDp = 1120;

    public ResponsiveContentContainer(Context context) {
        super(context);
        row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        body = new LinearLayout(context);
        body.setOrientation(LinearLayout.VERTICAL);
        content = new FrameLayout(context);
        body.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        row.addView(body, new LinearLayout.LayoutParams(0, -1, 1));
        addView(row, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER_HORIZONTAL));
    }

    public FrameLayout content() { return content; }
    public void setMaxWidthDp(int value) { maxWidthDp = value; requestLayout(); }
    public void setNavigation(View navigation, boolean rail) {
        if (rail) row.addView(navigation, 0, new LinearLayout.LayoutParams(dp(88), -1));
        else body.addView(navigation, new LinearLayout.LayoutParams(-1, -2));
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = Math.min(MeasureSpec.getSize(widthSpec), dp(maxWidthDp));
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), heightSpec);
    }
}
