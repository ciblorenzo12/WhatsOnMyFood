package com.ciblorenzo.whatsonmyfood.ui;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import androidx.core.view.ViewCompat;
import com.ciblorenzo.whatsonmyfood.R;

public class ExpandableSection extends LinearLayout {
    private boolean expanded;
    public ExpandableSection(Context c, AttributeSet a){super(c,a);setOrientation(VERTICAL);}
    @Override protected void onFinishInflate(){super.onFinishInflate(); if(getChildCount()<2)return;
        View heading=getChildAt(0); heading.setOnClickListener(v->{expanded=!expanded;apply();}); apply();
    }
    private void apply(){if(getChildCount()<2)return;getChildAt(1).setVisibility(expanded?VISIBLE:GONE);
        ViewCompat.setStateDescription(getChildAt(0),getContext().getString(expanded?R.string.ui_expanded:R.string.ui_collapsed));
        if(expanded)com.ciblorenzo.whatsonmyfood.utils.GlassMotion.enter(getChildAt(1));
    }
    @Override protected Parcelable onSaveInstanceState(){Bundle b=new Bundle();b.putParcelable("parent",super.onSaveInstanceState());b.putBoolean("expanded",expanded);return b;}
    @Override protected void onRestoreInstanceState(Parcelable state){if(state instanceof Bundle){Bundle b=(Bundle)state;super.onRestoreInstanceState(b.getParcelable("parent"));expanded=b.getBoolean("expanded");apply();}else super.onRestoreInstanceState(state);}
}
