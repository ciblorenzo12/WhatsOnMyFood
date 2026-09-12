package com.ciblorenzo.whatsonmyfood.ui;
import com.ciblorenzo.whatsonmyfood.Product;
import com.ciblorenzo.whatsonmyfood.PantryRiskScorer;
import java.util.*;

/** Filtering changes presentation only; uses the existing pantry risk calculation. */
public final class PantryPresentation {
    private PantryPresentation() {}
    public static boolean needsReview(Product p) {
        return p!=null && PantryRiskScorer.scoreProducts(Collections.singletonList(p)).get(0).combinedRisk>=70;
    }
    public static int reviewCount(List<Product> items){int count=0;if(items!=null)for(Product p:items)if(needsReview(p))count++;return count;}
    public static List<Product> filter(List<Product> source,String query,String filter){
        List<Product> result=new ArrayList<>();if(source==null)return result;
        String q=query==null?"":query.trim().toLowerCase(Locale.ROOT);
        for(Product p:source){
            if(p==null)continue;
            String text=(Objects.toString(p.productName,"")+" "+Objects.toString(p.brands,"")+" "+Objects.toString(p.barcode,"")).toLowerCase(Locale.ROOT);
            if(!text.contains(q))continue;
            if("review".equals(filter)&&!needsReview(p))continue;
            if("good".equals(filter)&&(p.healthScore==null||p.healthScore<70||needsReview(p)))continue;
            result.add(p);
        }
        return result;
    }
}
