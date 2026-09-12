package com.ciblorenzo.whatsonmyfood.ui;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.*;
import androidx.core.view.ViewCompat;
import com.google.android.material.button.MaterialButton;
import com.ciblorenzo.whatsonmyfood.*;
import com.ciblorenzo.whatsonmyfood.utils.LinkHandler;

public class IngredientDetailsView extends ScrollView {
    private final LinearLayout body;
    public IngredientDetailsView(Context context,AttributeSet attrs){super(context,attrs);
        body=new LinearLayout(context);body.setOrientation(LinearLayout.VERTICAL);int p=dp(24);body.setPadding(p,p,p,p);addView(body);setBackgroundResource(R.drawable.glass_panel);
        text(context.getString(R.string.ui_select_ingredient),16,false);
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private void text(String value,int size,boolean heading){
        TextView view=new TextView(getContext());view.setText(value);view.setTextSize(size);view.setTextColor(getContext().getColor(heading?R.color.text_primary:R.color.text_secondary));
        if(heading){view.setTypeface(null,android.graphics.Typeface.BOLD);ViewCompat.setAccessibilityHeading(view,true);}
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(heading?24:8);body.addView(view,p);
    }
    private void section(int title,String content){text(getContext().getString(title),16,true);text(content==null||content.trim().isEmpty()?getContext().getString(R.string.ui_not_provided):content,16,false);}
    public void bind(AdditiveEntry entry){body.removeAllViews();if(entry==null){text(getContext().getString(R.string.ui_select_ingredient),16,false);return;}
        text(entry.name,24,true);text(entry.aliases,14,false);
        if((entry.sourceTitle+" "+entry.note).toLowerCase(java.util.Locale.ROOT).contains("bitwise"))text(getContext().getString(R.string.ui_ai_context),14,false);
        section(R.string.ui_what_is,entry.category);
        section(R.string.ui_what_does,entry.function);
        section(R.string.ui_where_found,entry.explanation);
        section(R.string.ui_why_matters,entry.note);
        section(R.string.ui_evidence,entry.sourceTitle);
        section(R.string.ui_regulatory,null);
        MaterialButton source=new MaterialButton(getContext(),null,com.google.android.material.R.attr.borderlessButtonStyle);
        source.setText(R.string.ui_sources);source.setMinHeight(dp(48));source.setEnabled(entry.sourceUrl!=null&&!entry.sourceUrl.trim().isEmpty());
        source.setOnClickListener(v->LinkHandler.openLink(getContext(),entry.sourceUrl,entry.sourceTitle,entry.function));body.addView(source);
        scrollTo(0,0);
    }
}
