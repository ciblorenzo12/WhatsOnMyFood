package com.ciblorenzo.whatsonmyfood;

import android.content.Context;
import android.content.res.Configuration;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.util.Locale;
import static org.junit.Assert.*;

/** Inflates production layouts without contacting services or changing the user's database. */
public class ModernUiLayoutTest {
    private Context context(int width,boolean dark,String language){
        Context base=InstrumentationRegistry.getInstrumentation().getTargetContext();
        Configuration config=new Configuration(base.getResources().getConfiguration());
        config.screenWidthDp=width;config.screenHeightDp=800;config.smallestScreenWidthDp=Math.min(width,800);
        config.fontScale=1.3f;config.setLocale(new Locale(language));
        config.uiMode=(config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)|(dark?Configuration.UI_MODE_NIGHT_YES:Configuration.UI_MODE_NIGHT_NO);
        return new ContextThemeWrapper(base.createConfigurationContext(config),R.style.Theme_MyApplication);
    }
    @Test public void productionLayoutsInflateAtCompactAndExpandedWidthsInBothThemes(){
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            for(int width:new int[]{360,1000})for(boolean dark:new boolean[]{false,true})for(String language:new String[]{"en","es"}){
                Context c=context(width,dark,language);float d=c.getResources().getDisplayMetrics().density;
                for(int layout:new int[]{R.layout.activity_main,R.layout.activity_pantry,R.layout.activity_additive_database,R.layout.activity_profile,R.layout.activity_product_details,R.layout.fragment_product_details}){
                    FrameLayout parent=new FrameLayout(c);
                    LayoutInflater inflater=LayoutInflater.from(c).cloneInContext(c);
                    inflater.setFactory2(new LayoutInflater.Factory2() {
                        public View onCreateView(String name,Context context,android.util.AttributeSet attrs){return onCreateView(null,name,context,attrs);}
                        public View onCreateView(View p,String name,Context context,android.util.AttributeSet attrs){
                            if("Button".equals(name))return new com.google.android.material.button.MaterialButton(context,attrs);
                            if("TextView".equals(name))return new com.google.android.material.textview.MaterialTextView(context,attrs);
                            return null;
                        }
                    });
                    View v=inflater.inflate(layout,parent,false);parent.addView(v);
                    parent.measure(View.MeasureSpec.makeMeasureSpec(Math.round(width*d),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(Math.round(800*d),View.MeasureSpec.EXACTLY));
                    parent.layout(0,0,parent.getMeasuredWidth(),parent.getMeasuredHeight());
                    assertTrue("Layout must measure at width "+width,v.getMeasuredWidth()>0);
                    if(layout==R.layout.activity_additive_database)assertEquals(width>=840,v.findViewById(R.id.ui_ingredient_detail)!=null);
                    if(layout==R.layout.activity_main){
                        assertTrue(v.findViewById(R.id.action_scan).getMeasuredHeight()>=48*d);
                        assertTrue(v.findViewById(R.id.action_ingredients).getMeasuredHeight()>=48*d);
                        assertEquals(v.findViewById(R.id.action_pantry).getTop(),v.findViewById(R.id.action_history).getTop());
                    }
                    if(layout==R.layout.activity_pantry && width==360)assertTrue("Keep the compact product list usable",v.findViewById(R.id.pantry_recycler_view).getMeasuredHeight()>=220*d);
                    if(layout==R.layout.fragment_product_details){
                        View ingredients=v.findViewById(R.id.ingredients_card);
                        assertEquals(View.GONE,ingredients.getVisibility());
                        v.findViewById(R.id.ingredients_label).performClick();
                        assertEquals(View.VISIBLE,ingredients.getVisibility());
                    }
                    if("true".equals(InstrumentationRegistry.getArguments().getString("uiScreenshots"))) {
                        try {
                            android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(parent.getMeasuredWidth(),parent.getMeasuredHeight(),android.graphics.Bitmap.Config.ARGB_8888);
                            parent.draw(new android.graphics.Canvas(bitmap));
                            java.io.File dir=c.getExternalFilesDir("ui-review");
                            java.io.File file=new java.io.File(dir,c.getResources().getResourceEntryName(layout)+"-"+width+"-"+language+"-"+(dark?"dark":"light")+".png");
                            try(java.io.FileOutputStream stream=new java.io.FileOutputStream(file)){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,stream);}
                            bitmap.recycle();
                        }catch(java.io.IOException error){throw new AssertionError(error);}
                    }
                }
            }
        });
    }
}
