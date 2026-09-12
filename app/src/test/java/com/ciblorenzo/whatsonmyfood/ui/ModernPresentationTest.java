package com.ciblorenzo.whatsonmyfood.ui;
import com.ciblorenzo.whatsonmyfood.Product;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class ModernPresentationTest {
    private Product product(String name,Integer score){return new Product(name,name,"Brand",null,null,null,null,null,null,null,null,null,null,score);}
    @Test public void filtersNeverMutateTheExportOrPersistenceList(){
        Product good=product("Oats",90),review=product("Snack",20),unknown=product("Unknown score",null);
        List<Product> source=new ArrayList<>(Arrays.asList(good,review,unknown));
        List<Product> filtered=PantryPresentation.filter(source,"","review");
        assertEquals(Collections.singletonList(review),filtered);
        filtered.clear();assertEquals(3,source.size());assertSame(good,source.get(0));
    }
    @Test public void unknownIsNotLabeledGoodAndSearchComposesWithFilter(){
        Product good=product("OATS",90),unknown=product("Oats unscored",null);
        assertEquals(Collections.singletonList(good),PantryPresentation.filter(Arrays.asList(good,unknown),"oats","good"));
        assertTrue(PantryPresentation.filter(Arrays.asList(good,unknown),"milk","all").isEmpty());
    }
    @Test public void ingredientCategoriesHandleEnglishSpanishAndUnknown(){
        assertEquals("preservatives",IngredientPresentation.categoryKey("Conservante"));
        assertEquals("colorants",IngredientPresentation.categoryKey("Food colouring"));
        assertEquals("emulsifiers",IngredientPresentation.categoryKey("Emulsionante"));
        assertEquals("sweeteners",IngredientPresentation.categoryKey("Edulcorante"));
        assertEquals("other",IngredientPresentation.categoryKey(null));
    }
}
