package com.ciblorenzo.whatsonmyfood.ui;
import android.view.View;
import android.widget.LinearLayout;
import com.ciblorenzo.whatsonmyfood.R;

public final class ProductPresentation {
    private ProductPresentation() {}
    public static void explain(View root, com.ciblorenzo.whatsonmyfood.analysis.ProductAnalysisReport report) {
        if (root == null) return;
        android.widget.TextView text = root.findViewById(R.id.ui_score_calculation);
        if (text != null) text.setText(report == null ? root.getContext().getString(R.string.ui_score_context) : report.getScoreExplanation());
    }
    public static void score(View root,Integer value){
        if(root==null)return; ScoreRing ring=root.findViewById(R.id.ui_product_score);
        if(ring!=null)ring.setScore(value);
    }
    public static void drivers(View root, com.ciblorenzo.whatsonmyfood.ProductWithDetails data) {
        if(root==null||data==null||data.product==null)return;
        android.content.Context c=root.getContext();
        android.widget.TextView sugar=root.findViewById(R.id.ui_driver_sugar),sodium=root.findViewById(R.id.ui_driver_sodium),processing=root.findViewById(R.id.ui_driver_processing),additives=root.findViewById(R.id.ui_driver_additives);
        if(sugar==null)return;
        String unknown=c.getString(R.string.ui_data_unavailable);
        sugar.setText(c.getString(R.string.ui_added_sugar)+"\n"+(data.nutriments==null||data.nutriments.addedSugars==null?unknown:c.getString(R.string.ui_grams_per100,data.nutriments.addedSugars)));
        sodium.setText(c.getString(R.string.ui_sodium)+"\n"+(data.nutriments==null||data.nutriments.sodium==null?unknown:c.getString(R.string.ui_grams_per100,data.nutriments.sodium)));
        processing.setText(c.getString(R.string.ui_processing)+"\n"+(data.product.novaGroup==null?unknown:c.getString(R.string.ui_nova_group,data.product.novaGroup)));
        java.util.List<com.ciblorenzo.whatsonmyfood.AdditiveEntry> matches=new java.util.ArrayList<>();
        StringBuilder input=new StringBuilder();
        if(data.ingredients!=null)for(com.ciblorenzo.whatsonmyfood.Ingredient i:data.ingredients)if(i!=null&&i.text!=null)input.append(i.text).append(" ");
        String label=input.toString().toLowerCase(java.util.Locale.ROOT);
        for(com.ciblorenzo.whatsonmyfood.AdditiveEntry entry:com.ciblorenzo.whatsonmyfood.AdditiveDatabase.entries()) {
            if(java.util.regex.Pattern.compile("(?<![\\p{L}\\p{N}])"+java.util.regex.Pattern.quote(entry.name.toLowerCase(java.util.Locale.ROOT))+"(?![\\p{L}\\p{N}])").matcher(label).find())matches.add(entry);
        }
        additives.setText(c.getString(R.string.ui_additives)+"\n"+c.getString(R.string.ui_catalog_count,matches.size()));
        android.widget.LinearLayout list=root.findViewById(R.id.ui_additive_matches);
        list.removeAllViews();
        android.widget.TextView context=new android.widget.TextView(c);context.setText(R.string.ui_catalog_context);context.setTextColor(c.getColor(R.color.text_secondary));list.addView(context);
        for(com.ciblorenzo.whatsonmyfood.AdditiveEntry entry:matches){
            com.google.android.material.button.MaterialButton button=new com.google.android.material.button.MaterialButton(c,null,com.google.android.material.R.attr.borderlessButtonStyle);
            button.setText(entry.name);button.setOnClickListener(v->{IngredientDetailsView detail=new IngredientDetailsView(c,null);detail.bind(entry);new com.google.android.material.dialog.MaterialAlertDialogBuilder(c).setView(detail).setPositiveButton(R.string.ui_close,null).show();});list.addView(button);
        }
    }
    public static void adapt(View root) {
        if(root==null || !root.getResources().getBoolean(R.bool.ui_two_pane))return;
        LinearLayout details=root.findViewById(R.id.details_layout);
        if(details==null || details.getTag(R.id.ui_product_score)!=null)return;
        LinearLayout left=new LinearLayout(root.getContext()),right=new LinearLayout(root.getContext()),row=new LinearLayout(root.getContext());
        left.setOrientation(LinearLayout.VERTICAL);right.setOrientation(LinearLayout.VERTICAL);row.setOrientation(LinearLayout.HORIZONTAL);
        boolean second=false;
        while(details.getChildCount()>0){View child=details.getChildAt(0);details.removeViewAt(0);
            if(child.getId()==R.id.ai_summary_container)second=true;
            (second?right:left).addView(child);
        }
        row.addView(left,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,-2,1);rp.setMarginStart(Math.round(24*root.getResources().getDisplayMetrics().density));row.addView(right,rp);
        details.addView(row,new LinearLayout.LayoutParams(-1,-2));details.setTag(R.id.ui_product_score,true);
    }
}
