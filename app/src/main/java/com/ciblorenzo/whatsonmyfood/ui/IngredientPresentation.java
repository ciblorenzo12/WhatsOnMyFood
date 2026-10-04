package com.ciblorenzo.whatsonmyfood.ui;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;
import java.util.Locale;
public final class IngredientPresentation {
    private IngredientPresentation(){}
    public static final int ALLERGEN_HIGHLIGHT_COLOR = 0x66FFEB3B;

    public static boolean isAllergenNotice(AnalysisResult result) {
        if (result == null || result.getLevel() != AnalysisResult.WarningLevel.INFO
                || result.getMessage() == null) return false;
        return result.getMessage().startsWith("Allergen statement:")
                || result.getMessage().startsWith("Allergen advisory:");
    }
    public static String categoryKey(String value){
        String c=value==null?"":value.toLowerCase(Locale.ROOT);
        if(c.contains("preserv")||c.contains("conserv"))return "preservatives";
        if(c.contains("color")||c.contains("colour"))return "colorants";
        if(c.contains("emuls"))return "emulsifiers";
        if(c.contains("sweet")||c.contains("edulcor"))return "sweeteners";
        return "other";
    }
}
