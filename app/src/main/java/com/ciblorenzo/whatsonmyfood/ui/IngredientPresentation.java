package com.ciblorenzo.whatsonmyfood.ui;
import java.util.Locale;
public final class IngredientPresentation {
    private IngredientPresentation(){}
    public static String categoryKey(String value){
        String c=value==null?"":value.toLowerCase(Locale.ROOT);
        if(c.contains("preserv")||c.contains("conserv"))return "preservatives";
        if(c.contains("color")||c.contains("colour"))return "colorants";
        if(c.contains("emuls"))return "emulsifiers";
        if(c.contains("sweet")||c.contains("edulcor"))return "sweeteners";
        return "other";
    }
}
