package com.ciblorenzo.whatsonmyfood.ui;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import com.ciblorenzo.whatsonmyfood.R;

/** Static placeholders avoid motion distraction and respect reduced-motion preferences. */
public class LoadingSkeleton extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    public LoadingSkeleton(Context c,AttributeSet a){super(c,a);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    @Override protected void onDraw(Canvas c){super.onDraw(c);paint.setColor(getContext().getColor(R.color.divider));
        float d=getResources().getDisplayMetrics().density,w=getWidth();
        c.drawRoundRect(0,0,w,72*d,16*d,16*d,paint);
        c.drawRoundRect(0,88*d,w*.72f,104*d,8*d,8*d,paint);
        c.drawRoundRect(0,116*d,w*.9f,132*d,8*d,8*d,paint);
    }
}
