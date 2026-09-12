package com.ciblorenzo.whatsonmyfood.ui;
import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ProgressBar;
import com.google.android.material.button.MaterialButton;
import com.ciblorenzo.whatsonmyfood.R;

/** One presentation for empty, error, offline and loading states; callers supply resources. */
public class ContentStateView extends LinearLayout {
    private final TextView message;
    private final ProgressBar progress;
    private final MaterialButton action;
    public ContentStateView(Context c, AttributeSet attrs) {
        super(c,attrs); setOrientation(VERTICAL); setGravity(Gravity.CENTER);
        int padding=Math.round(16*getResources().getDisplayMetrics().density); setPadding(padding,padding,padding,padding);
        setBackgroundResource(R.drawable.glass_panel);
        progress=new ProgressBar(c); addView(progress,new LayoutParams(padding,padding)); progress.setVisibility(GONE);
        message=new TextView(c); message.setTextColor(c.getColor(R.color.text_secondary)); message.setGravity(Gravity.CENTER); message.setTextSize(14);
        addView(message,new LayoutParams(-1,-2));
        action=new MaterialButton(c,null,com.google.android.material.R.attr.borderlessButtonStyle); addView(action); action.setVisibility(GONE);
        setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);
    }
    public void show(int text, boolean loading) { setVisibility(VISIBLE); message.setText(text); progress.setVisibility(loading?VISIBLE:GONE); action.setVisibility(GONE); }
    public void action(int text, OnClickListener listener) { action.setText(text); action.setVisibility(VISIBLE); action.setOnClickListener(listener); }
}
