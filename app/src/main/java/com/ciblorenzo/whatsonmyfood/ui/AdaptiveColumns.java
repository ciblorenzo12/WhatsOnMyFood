package com.ciblorenzo.whatsonmyfood.ui;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

/** Two content sections stack on phones and share space in expanded windows. */
public class AdaptiveColumns extends LinearLayout {
    public AdaptiveColumns(Context context, AttributeSet attrs) { super(context, attrs); }
    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        boolean expanded = MeasureSpec.getSize(widthSpec) / getResources().getDisplayMetrics().density >= 760;
        setOrientation(expanded ? HORIZONTAL : VERTICAL);
        for (int i=0; i<getChildCount(); i++) {
            View child=getChildAt(i);
            LayoutParams p=(LayoutParams)child.getLayoutParams();
            p.width=expanded ? 0 : LayoutParams.MATCH_PARENT;
            p.weight=expanded ? 1 : 0;
            p.setMarginStart(expanded && i>0 ? Math.round(24*getResources().getDisplayMetrics().density) : 0);
            p.resolveLayoutDirection(getLayoutDirection());
        }
        super.onMeasure(widthSpec,heightSpec);
    }
}
