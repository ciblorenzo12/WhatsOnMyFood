package com.ciblorenzo.whatsonmyfood;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.ciblorenzo.whatsonmyfood.AdditiveEntry.HealthStatus;

public final class AdditiveDatabase {
    private static final String FDA_TYPES = "https://www.fda.gov/food/food-additives-and-gras-ingredients-information-consumers/types-food-ingredients";
    private static final String FDA_SWEETENERS = "https://www.fda.gov/food/food-additives-petitions/high-intensity-sweeteners";
    private static final String EFSA_ADDITIVES = "https://www.efsa.europa.eu/en/topics/topic/food-additives";
    private static final String WHO_ADDITIVES = "https://www.who.int/news-room/fact-sheets/detail/food-additives";

    private AdditiveDatabase() {
    }

    public static List<AdditiveEntry> entries() {
        List<AdditiveEntry> entries = new ArrayList<>();
        entries.add(new AdditiveEntry(
                "Sodium benzoate",
                "Preservative",
                "Benzoate, E211",
                "Helps slow growth of yeast, mold, and bacteria in acidic foods and drinks.",
                "Often used in sodas, juices, sauces, pickles, and condiments to extend shelf life.",
                "Usually a processing signal. People sensitive to preservatives may prefer products without it.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.NOT_RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Potassium sorbate",
                "Preservative",
                "Sorbate, E202",
                "Helps prevent mold and yeast growth.",
                "Common in cheese, baked goods, dried fruit, dips, and beverages.",
                "A common shelf-life additive; not a nutrient and usually not needed in fresh foods.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.MODERATE
        ));
        entries.add(new AdditiveEntry(
                "Calcium propionate",
                "Preservative",
                "Propionate, E282",
                "Helps prevent mold growth, especially in bread and baked goods.",
                "Used to keep packaged breads, tortillas, and pastries from spoiling quickly.",
                "Useful for shelf life, but frequent presence can point to highly packaged bread products.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.MODERATE
        ));
        entries.add(new AdditiveEntry(
                "Sodium nitrite",
                "Curing preservative",
                "Nitrite, E250",
                "Helps preserve cured meats and maintain cured meat color and flavor.",
                "Common in bacon, ham, hot dogs, deli meats, and sausages.",
                "Processed meats with nitrites are best treated as occasional foods.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.NOT_RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "BHA / BHT",
                "Antioxidant preservative",
                "Butylated hydroxyanisole, butylated hydroxytoluene, E320, E321",
                "Helps slow rancidity in fats and oils.",
                "May appear in cereals, snack foods, gum, shortenings, and packaged foods with added fats.",
                "A strong processing signal. Choosing fresher foods or simpler ingredient lists avoids it.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.NOT_RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Citric acid",
                "Acidulant",
                "E330",
                "Adds tartness and helps control acidity.",
                "Used in drinks, candy, sauces, canned foods, and fruit products.",
                "Common and usually used for flavor or pH control; it does not mean the food contains citrus fruit.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Ascorbic acid",
                "Antioxidant",
                "Vitamin C, E300",
                "Helps slow browning and oxidation; can also add vitamin C.",
                "Common in fruit products, beverages, cured meats, and bakery products.",
                "Often a lower-concern additive compared with synthetic colors or preservatives.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Lecithin",
                "Emulsifier",
                "Soy lecithin, sunflower lecithin, E322",
                "Helps oil and water mix and keeps texture smooth.",
                "Common in chocolate, dressings, baked goods, spreads, and protein products.",
                "Usually used in small amounts for texture; check soy source if avoiding soy.",
                "EFSA Food Additives",
                EFSA_ADDITIVES,
                HealthStatus.RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Mono- and diglycerides",
                "Emulsifier",
                "E471",
                "Helps blend ingredients, improve softness, and prevent separation.",
                "Common in bread, cakes, ice cream, whipped toppings, and margarine.",
                "A texture additive often found in ultra-processed foods.",
                "EFSA Food Additives",
                EFSA_ADDITIVES,
                HealthStatus.MODERATE
        ));
        entries.add(new AdditiveEntry(
                "Xanthan gum",
                "Thickener / stabilizer",
                "E415",
                "Thickens foods and keeps sauces or drinks evenly mixed.",
                "Common in dressings, gluten-free baked goods, sauces, beverages, and dairy alternatives.",
                "Usually used for texture. Multiple gums together can signal heavy formulation.",
                "EFSA Food Additives",
                EFSA_ADDITIVES,
                HealthStatus.RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Carrageenan",
                "Thickener / stabilizer",
                "E407",
                "Thickens and stabilizes liquids and creamy foods.",
                "Common in dairy alternatives, desserts, whipped toppings, and processed meats.",
                "Some people avoid it for digestive comfort, especially when many texture additives appear together.",
                "EFSA Food Additives",
                EFSA_ADDITIVES,
                HealthStatus.NOT_RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Polysorbate 80",
                "Emulsifier",
                "E433",
                "Helps keep ingredients mixed and improves creamy texture.",
                "Common in ice cream, sauces, dressings, and some baked goods.",
                "A processing marker. Prefer shorter ingredient lists when this appears with several other emulsifiers.",
                "EFSA Food Additives",
                EFSA_ADDITIVES,
                HealthStatus.NOT_RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Red 40",
                "Color additive",
                "Allura Red, FD&C Red No. 40, E129",
                "Adds red color.",
                "Common in candy, drinks, cereals, desserts, and colorful snacks.",
                "Adds color only, not nutrition. FDA-certified color additives must be listed by name on labels.",
                "FDA Color Additives",
                "https://www.fda.gov/food/color-additives-information-consumers/color-additives-foods",
                HealthStatus.NOT_RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Yellow 5",
                "Color additive",
                "Tartrazine, FD&C Yellow No. 5, E102",
                "Adds yellow color.",
                "Common in drinks, candy, chips, cereals, baked goods, and desserts.",
                "Adds appearance, not nutrition. Some shoppers choose to limit synthetic colors.",
                "FDA Color Additives",
                "https://www.fda.gov/food/color-additives-information-consumers/color-additives-foods",
                HealthStatus.NOT_RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Caramel color",
                "Color additive",
                "Caramel, E150",
                "Adds brown color.",
                "Common in colas, sauces, gravies, baked goods, and candies.",
                "Used mostly for appearance. It does not mean the food contains caramel candy.",
                "FDA Color Additives",
                "https://www.fda.gov/food/color-additives-information-consumers/color-additives-foods",
                HealthStatus.MODERATE
        ));
        entries.add(new AdditiveEntry(
                "Sucralose",
                "High-intensity sweetener",
                "Splenda, E955",
                "Adds sweetness with little or no sugar.",
                "Common in diet drinks, protein products, sugar-free desserts, and tabletop sweeteners.",
                "FDA identifies high-intensity sweeteners by name on ingredient labels; some people prefer unsweetened foods.",
                "FDA High-Intensity Sweeteners",
                FDA_SWEETENERS,
                HealthStatus.NOT_RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Aspartame",
                "High-intensity sweetener",
                "E951",
                "Adds sweetness with little or no sugar.",
                "Common in diet sodas, drink mixes, gum, dairy products, and sugar-free foods.",
                "People with phenylketonuria need to avoid phenylalanine from aspartame.",
                "FDA High-Intensity Sweeteners",
                FDA_SWEETENERS,
                HealthStatus.NOT_RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Acesulfame potassium",
                "High-intensity sweetener",
                "Ace-K, acesulfame-K, E950",
                "Adds sweetness and is often blended with other sweeteners.",
                "Common in diet drinks, protein products, gum, candies, and desserts.",
                "Often appears with sucralose or aspartame in highly formulated products.",
                "FDA High-Intensity Sweeteners",
                FDA_SWEETENERS,
                HealthStatus.NOT_RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "High fructose corn syrup",
                "Sweetener",
                "HFCS, corn syrup",
                "Adds sweetness and helps texture, browning, and moisture.",
                "Common in sodas, sweet drinks, candy, sauces, baked goods, and packaged snacks.",
                "A strong added-sugar signal, especially when it appears near the top of the ingredient list.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.NOT_RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Natural flavors",
                "Flavoring",
                "Natural flavor, flavor",
                "Adds or restores flavor.",
                "Common in drinks, snacks, desserts, protein products, and packaged foods.",
                "The phrase can cover complex flavor mixtures; it does not tell you the full recipe.",
                "FDA Food Additives and GRAS Ingredients",
                "https://www.fda.gov/food/food-ingredients-packaging/food-additives-and-gras-ingredients-information-consumers",
                HealthStatus.NOT_RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Artificial flavors",
                "Flavoring",
                "Artificial flavor",
                "Adds flavor using synthetic chemicals.",
                "Used in almost every category of processed food to mimic natural tastes cheaply.",
                "Synthetic chemicals used to create flavor; often a sign of highly engineered food.",
                "FDA Food Additives and GRAS Ingredients",
                "https://www.fda.gov/food/food-ingredients-packaging/food-additives-and-gras-ingredients-information-consumers",
                HealthStatus.NOT_RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Monosodium glutamate",
                "Flavor enhancer",
                "MSG, E621",
                "Boosts savory or umami flavor.",
                "Common in soups, seasonings, chips, sauces, and prepared meals.",
                "Used for flavor intensity. It is not the same as salt, but it does add sodium.",
                "WHO Food Additives",
                WHO_ADDITIVES,
                HealthStatus.NOT_RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Enriched flour",
                "Refined grain ingredient",
                "Wheat flour, white flour, bleached flour",
                "Forms the base of many baked foods after bran and germ are removed; some vitamins and iron are added back.",
                "Common in bread, crackers, cookies, cereals, pasta, and pastries.",
                "Usually less fiber-rich than whole grain flour. Look for whole grain as a first ingredient.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.MODERATE
        ));
        entries.add(new AdditiveEntry(
                "Pectin",
                "Gelling agent / thickener",
                "E440",
                "Helps jams, jellies, fruit fillings, and gummies gel or thicken.",
                "Common in fruit spreads, yogurt fruit bases, candies, and dessert fillings.",
                "Pectin can occur naturally in fruit; on labels it usually signals texture control.",
                "EFSA Food Additives",
                EFSA_ADDITIVES,
                HealthStatus.RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Guar gum",
                "Thickener / stabilizer",
                "E412",
                "Thickens liquids and helps prevent ingredient separation.",
                "Common in ice cream, sauces, dressings, gluten-free foods, and dairy alternatives.",
                "Often fine in small amounts, but several gums together can indicate heavy formulation.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Gellan gum",
                "Stabilizer / gelling agent",
                "E418",
                "Helps suspend ingredients and create a smooth, consistent texture.",
                "Common in plant milks, drinks with added nutrients, grains, and desserts.",
                "Often used to keep added minerals, protein, or cocoa from settling.",
                "EFSA Food Additives",
                EFSA_ADDITIVES,
                HealthStatus.RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Cellulose gum",
                "Thickener / stabilizer",
                "Carboxymethylcellulose, CMC, E466",
                "Thickens foods and helps prevent ice crystals or separation.",
                "Common in ice cream, sauces, dressings, gluten-free baked goods, and reduced-fat foods.",
                "A texture additive. Multiple emulsifiers and gums together are a stronger ultra-processing signal.",
                "EFSA Food Additives",
                EFSA_ADDITIVES,
                HealthStatus.MODERATE
        ));
        entries.add(new AdditiveEntry(
                "Modified food starch",
                "Thickener / texturizer",
                "Modified corn starch, modified tapioca starch",
                "Thickens, stabilizes, or changes texture after processing.",
                "Common in soups, sauces, puddings, fillings, frozen meals, and dressings.",
                "Not the same as whole-food starch; usually used to engineer texture or stability.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.MODERATE
        ));
        entries.add(new AdditiveEntry(
                "Maltodextrin",
                "Bulking agent / carbohydrate",
                "Corn maltodextrin, tapioca maltodextrin",
                "Adds bulk, carries flavors, improves texture, or adds quick-digesting carbohydrate.",
                "Common in snacks, drink powders, seasoning blends, protein products, and processed foods.",
                "Can behave like a refined carbohydrate. Watch for it in products marketed as low sugar.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.NOT_RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Dextrose",
                "Added sugar",
                "Glucose",
                "Adds sweetness, browning, and quick-digesting carbohydrate.",
                "Common in cured meats, baked goods, snacks, candies, and seasoning blends.",
                "An added sugar even when it does not say sugar in the name.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.NOT_RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Invert sugar",
                "Added sugar",
                "Invert syrup",
                "Adds sweetness and helps keep foods moist or smooth.",
                "Common in candies, baked goods, frostings, syrups, and desserts.",
                "An added sugar. If it appears near the top, the product is likely more dessert-like.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.NOT_RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Agave syrup",
                "Added sugar",
                "Agave nectar",
                "Adds sweetness.",
                "Common in bars, drinks, desserts, granola, and natural-branded sweet foods.",
                "Still an added sugar despite natural branding.",
                "WHO Healthy Diet",
                "https://www.who.int/news-room/fact-sheets/detail/healthy-diet",
                HealthStatus.MODERATE
        ));
        entries.add(new AdditiveEntry(
                "Brown rice syrup",
                "Added sugar",
                "Rice syrup",
                "Adds sweetness and sticky texture.",
                "Common in granola bars, cereals, snacks, and natural-branded sweets.",
                "An added sugar. It can make products seem less sugary because the word sugar may not appear.",
                "WHO Healthy Diet",
                "https://www.who.int/news-room/fact-sheets/detail/healthy-diet",
                HealthStatus.MODERATE
        ));
        entries.add(new AdditiveEntry(
                "Sorbitol",
                "Sugar alcohol",
                "E420",
                "Adds sweetness and helps retain moisture.",
                "Common in sugar-free candy, gum, baked goods, and some diet foods.",
                "Sugar alcohols can cause digestive discomfort for some people, especially in larger amounts.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.MODERATE
        ));
        entries.add(new AdditiveEntry(
                "Erythritol",
                "Sugar alcohol",
                "E968",
                "Adds sweetness with fewer calories than sugar.",
                "Common in keto foods, sugar-free chocolate, protein bars, and tabletop sweeteners.",
                "May be easier to tolerate than some sugar alcohols, but can still bother sensitive digestion.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.MODERATE
        ));
        entries.add(new AdditiveEntry(
                "Stevia extract",
                "High-intensity sweetener",
                "Steviol glycosides, Reb A, E960",
                "Adds sweetness with little or no sugar.",
                "Common in reduced-sugar drinks, yogurt, protein products, and tabletop sweeteners.",
                "Plant-derived does not mean whole-food; it is still a concentrated sweetener.",
                "FDA High-Intensity Sweeteners",
                FDA_SWEETENERS,
                HealthStatus.RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Monk fruit extract",
                "High-intensity sweetener",
                "Luo han guo, mogrosides",
                "Adds sweetness with little or no sugar.",
                "Common in keto products, protein bars, drinks, and tabletop sweetener blends.",
                "Often blended with erythritol or other carriers; check the full ingredient list.",
                "FDA High-Intensity Sweeteners",
                FDA_SWEETENERS,
                HealthStatus.RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Sodium phosphate",
                "Stabilizer / emulsifying salt",
                "Phosphate, disodium phosphate, trisodium phosphate",
                "Helps control acidity, retain moisture, or keep processed cheese smooth.",
                "Common in processed cheese, deli meats, baked goods, and some beverages.",
                "A processing marker. People watching phosphorus intake may need extra caution.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.NOT_RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Calcium disodium EDTA",
                "Preservative / sequestrant",
                "EDTA",
                "Helps protect flavor and color by binding trace metals that speed spoilage.",
                "Common in dressings, sauces, canned beans, mayo, and pickled products.",
                "Usually used to keep packaged foods stable on shelves.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.NOT_RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Sodium metabisulfite",
                "Preservative / antioxidant",
                "Sulfites, E223",
                "Helps prevent browning and microbial spoilage.",
                "Common in dried fruit, wine, shrimp, potatoes, and some packaged foods.",
                "Sulfites can trigger reactions in sensitive people and must be declared in many cases.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.NOT_RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Annatto",
                "Color additive",
                "E160b",
                "Adds yellow to orange color.",
                "Common in cheese, butter, snacks, cereals, and baked goods.",
                "A color additive from plant sources; still mainly used for appearance.",
                "FDA Color Additives",
                "https://www.fda.gov/food/color-additives-information-consumers/color-additives-foods",
                HealthStatus.RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Titanium dioxide",
                "Color additive",
                "E171",
                "Adds white color and opacity.",
                "Historically used in candies, icings, chewing gum, and decorative foods.",
                "Regulatory treatment differs by region. If avoiding cosmetic additives, this is one to notice.",
                "EFSA Food Additives",
                EFSA_ADDITIVES,
                HealthStatus.NOT_RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Silicon dioxide",
                "Anti-caking agent",
                "Silica, E551",
                "Helps powders flow and prevents clumping.",
                "Common in seasoning blends, powdered drink mixes, salt, supplements, and baking mixes.",
                "Usually a small functional additive for powdered foods.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.MODERATE
        ));
        entries.add(new AdditiveEntry(
                "Calcium carbonate",
                "Mineral / anti-caking / leavening aid",
                "E170",
                "Can add calcium, reduce acidity, prevent caking, or support leavening.",
                "Common in plant milks, cereals, baked goods, supplements, and powders.",
                "May be used for fortification or texture; context matters.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Sodium bicarbonate",
                "Leavening agent",
                "Baking soda, E500",
                "Helps baked goods rise by releasing carbon dioxide.",
                "Common in breads, cakes, cookies, crackers, and pancake mixes.",
                "A normal baking ingredient, but it adds sodium.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Monocalcium phosphate",
                "Leavening acid",
                "E341",
                "Reacts with baking soda to help doughs and batters rise.",
                "Common in baking powder, pancake mixes, biscuits, and packaged baked goods.",
                "Mostly a baking function; less concerning than preservatives, colors, or added sugars.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Lactic acid",
                "Acidulant",
                "E270",
                "Adds tartness and helps control acidity.",
                "Common in pickles, olives, cheese, sourdough products, drinks, and sauces.",
                "Can be made by fermentation; usually a flavor and pH-control ingredient.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Tocopherols",
                "Antioxidant",
                "Mixed tocopherols, vitamin E, E306",
                "Helps protect oils and fats from rancidity.",
                "Common in cereals, snacks, oils, nut butters, and packaged foods with added fats.",
                "Often used to preserve freshness; generally a lower-concern additive.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Rosemary extract",
                "Antioxidant",
                "Rosemary oleoresin",
                "Helps slow oxidation and rancidity in fats and oils.",
                "Common in chips, crackers, meats, oils, and natural-branded packaged foods.",
                "Used for preservation, not necessarily for rosemary flavor.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Potassium metabisulfite",
                "Preservative / antioxidant",
                "Sulfites, E224",
                "Helps prevent browning and spoilage.",
                "Common in wine, dried fruit, bottled lemon juice, shrimp, and some potato products.",
                "Sulfites can affect sensitive people. This is worth noticing if someone has asthma or sulfite sensitivity.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.NOT_RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Sodium erythorbate",
                "Curing accelerator / antioxidant",
                "E316",
                "Helps preserve color and speed curing reactions in processed meats.",
                "Common in bacon, ham, hot dogs, deli meat, and sausages.",
                "Usually appears with nitrites in processed meats, so treat it as part of the processed-meat signal.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.NOT_RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "TBHQ",
                "Antioxidant preservative",
                "Tertiary butylhydroquinone, E319",
                "Helps slow rancidity in fats and oils.",
                "Common in fried snacks, crackers, microwave popcorn, instant noodles, and fast-food style oils.",
                "A strong processing marker, especially in salty snacks and fried packaged foods.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.NOT_RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Locust bean gum",
                "Thickener / stabilizer",
                "Carob bean gum, E410",
                "Adds thickness and improves creamy texture.",
                "Common in ice cream, dairy alternatives, cream cheese, sauces, and desserts.",
                "Usually a texture additive. Several gums together point to a more engineered texture.",
                "EFSA Food Additives",
                EFSA_ADDITIVES,
                HealthStatus.RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Agar",
                "Gelling agent",
                "Agar-agar, E406",
                "Forms gels and helps foods hold shape.",
                "Common in desserts, jellies, vegan gummies, fillings, and some dairy alternatives.",
                "A seaweed-derived gelling ingredient, mainly used for texture.",
                "EFSA Food Additives",
                EFSA_ADDITIVES,
                HealthStatus.RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Corn syrup",
                "Added sugar",
                "Glucose syrup",
                "Adds sweetness, body, shine, and moisture.",
                "Common in candy, cereal bars, sauces, baked goods, frosting, and desserts.",
                "An added sugar even when it is not called table sugar.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.NOT_RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Xylitol",
                "Sugar alcohol",
                "E967",
                "Adds sweetness with fewer calories than sugar.",
                "Common in sugar-free gum, mints, candy, oral-care products, and some keto foods.",
                "Can cause digestive discomfort in larger amounts. Keep products with xylitol away from dogs.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.MODERATE
        ));
        entries.add(new AdditiveEntry(
                "Blue 1",
                "Color additive",
                "Brilliant Blue FCF, FD&C Blue No. 1, E133",
                "Adds blue color.",
                "Common in candy, drinks, cereals, frostings, desserts, and colorful snacks.",
                "Adds appearance, not nutrition. Synthetic colors are easy to limit by choosing less colorful packaged foods.",
                "FDA Color Additives",
                "https://www.fda.gov/food/color-additives-information-consumers/color-additives-foods",
                HealthStatus.NOT_RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Beta carotene",
                "Color additive / provitamin A",
                "E160a",
                "Adds yellow to orange color and can contribute provitamin A.",
                "Common in margarine, cheese products, drinks, desserts, and fortified foods.",
                "Often lower concern than synthetic dyes, but still may be added mainly for appearance.",
                "FDA Color Additives",
                "https://www.fda.gov/food/color-additives-information-consumers/color-additives-foods",
                HealthStatus.RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Sodium acid pyrophosphate",
                "Leavening acid / phosphate",
                "SAPP, E450",
                "Reacts with baking soda to help baked foods rise and can help maintain color in potatoes.",
                "Common in baking mixes, biscuits, pancakes, frozen potatoes, and processed potato products.",
                "A functional phosphate additive. It can add to total phosphate exposure in highly processed diets.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.NOT_RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Malic acid",
                "Acidulant",
                "E296",
                "Adds sharp, fruity tartness and helps control acidity.",
                "Common in sour candy, drinks, fruit snacks, jams, and desserts.",
                "Mostly a flavor and acidity ingredient; frequent use often appears in candy-like foods.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Phosphoric acid",
                "Acidulant",
                "E338",
                "Adds tangy acidity and helps control pH.",
                "Common in cola, dark sodas, processed cheese, and some packaged drinks.",
                "A marker for highly processed drinks when it appears in sweetened beverages.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.NOT_RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Soy protein isolate",
                "Protein ingredient",
                "Soy isolate, isolated soy protein",
                "Adds concentrated protein and changes texture or moisture retention.",
                "Common in protein bars, meat alternatives, shakes, cereals, and processed meats.",
                "Not automatically unhealthy, but it is a concentrated processed ingredient rather than a whole soy food.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.MODERATE
        ));
        entries.add(new AdditiveEntry(
                "Whey protein isolate",
                "Protein ingredient",
                "Whey isolate",
                "Adds concentrated dairy protein.",
                "Common in protein powders, bars, shakes, yogurt products, and high-protein snacks.",
                "Useful for protein, but flavored products often pair it with sweeteners, gums, and flavors.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.MODERATE
        ));
        entries.add(new AdditiveEntry(
                "Palm oil",
                "Added fat",
                "Palm kernel oil",
                "Adds fat, texture, and shelf stability.",
                "Common in cookies, crackers, spreads, instant noodles, pastries, and snack foods.",
                "A saturated-fat source and common ultra-processed food ingredient. Check nutrition facts for saturated fat.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.NOT_RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Canola oil",
                "Added oil",
                "Rapeseed oil",
                "Adds fat, moisture, and cooking performance.",
                "Common in dressings, chips, sauces, baked goods, spreads, and frozen foods.",
                "An added oil. Health impact depends on amount, processing, and the full food pattern.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.MODERATE
        ));
        entries.add(new AdditiveEntry(
                "Soybean oil",
                "Added oil",
                "Vegetable oil",
                "Adds fat, moisture, and frying or baking performance.",
                "Common in dressings, mayonnaise, chips, crackers, sauces, and frozen meals.",
                "Often appears in processed foods. Check overall fat quality and ingredient simplicity.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.MODERATE
        ));
        entries.add(new AdditiveEntry(
                "Partially hydrogenated oil",
                "Trans fat source",
                "PHO, partially hydrogenated soybean oil",
                "Was used to improve texture and shelf life by creating more solid fats.",
                "May appear on old labels, imported foods, bakery shortenings, frostings, and some fried foods.",
                "A red-flag ingredient because it can indicate artificial trans fat. Avoid when found.",
                "FDA Trans Fat",
                "https://www.fda.gov/food/food-autitives-petitions/trans-fat",
                HealthStatus.NOT_RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Spices",
                "Flavoring",
                "Spice blend",
                "Adds flavor from dried plant ingredients.",
                "Common in sauces, soups, meats, snacks, seasonings, and prepared meals.",
                "This label can hide the exact blend, but it is usually less vague than natural flavors.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.RECOMMENDED
        ));
        entries.add(new AdditiveEntry(
                "Yeast extract",
                "Flavor enhancer",
                "Autolyzed yeast extract",
                "Adds savory, umami flavor.",
                "Common in soups, chips, crackers, sauces, bouillon, meat alternatives, and frozen meals.",
                "A flavor booster often used to make salty packaged foods taste richer.",
                "WHO Food Additives",
                WHO_ADDITIVES,
                HealthStatus.MODERATE
        ));

        entries.add(new AdditiveEntry(
                "Sodium nitrate",
                "Preservative / Color fixative",
                "Nitrate, E251",
                "Helps preserve processed meats and keep them looking pink.",
                "Common in cured meats, bacon, deli slices, and sausages.",
                "Similar to nitrites; processed meat consumption is linked to health risks when frequent.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.NOT_RECOMMENDED
        ));

        entries.add(new AdditiveEntry(
                "Potassium nitrate",
                "Preservative / Color fixative",
                "Saltpeter, E252",
                "Used as a preservative and colorant in cured meats.",
                "Found in some deli meats, sausages, and preserved cheeses.",
                "Another marker for cured and processed meat products.",
                "EFSA Food Additives",
                EFSA_ADDITIVES,
                HealthStatus.NOT_RECOMMENDED
        ));

        entries.add(new AdditiveEntry(
                "Propyl gallate",
                "Antioxidant preservative",
                "E310",
                "Prevents fats and oils from spoiling or becoming rancid.",
                "Often in meat products, microwave popcorn, vegetable oils, and potato flakes.",
                "A synthetic antioxidant used to extend shelf life in fat-heavy processed foods.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.NOT_RECOMMENDED
        ));

        entries.add(new AdditiveEntry(
                "Sodium stearoyl lactylate",
                "Emulsifier / Dough strengthener",
                "SSL, E481",
                "Helps strengthen dough, improve volume, and keep texture soft.",
                "Common in commercial breads, buns, pancakes, and waffles.",
                "A texture engineer common in factory-made bakery items.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.MODERATE
        ));

        entries.add(new AdditiveEntry(
                "DATEM",
                "Emulsifier",
                "Diacetyl Tartaric Acid Esters of Mono- and Diglycerides, E472e",
                "Helps strengthen bread dough and improves crumb structure.",
                "Found in packaged breads, frozen doughs, and crackers.",
                "A technical processing aid typical of commercial baking.",
                "EFSA Food Additives",
                EFSA_ADDITIVES,
                HealthStatus.MODERATE
        ));

        entries.add(new AdditiveEntry(
                "Arabic gum",
                "Thickener / Stabilizer",
                "Acacia gum, E414",
                "Adds body, stabilizes emulsions, and encapsulates flavors.",
                "Common in sodas, candies, frostings, and snack foods.",
                "A natural plant fiber used for technical consistency in processed foods.",
                "EFSA Food Additives",
                EFSA_ADDITIVES,
                HealthStatus.RECOMMENDED
        ));

        entries.add(new AdditiveEntry(
                "Tara gum",
                "Thickener / Stabilizer",
                "E417",
                "Provides smooth texture and improves moisture retention.",
                "Used in dairy products, ice cream, baked goods, and sauces.",
                "Often used as an alternative to Guar or Xanthan gum for texture.",
                "EFSA Food Additives",
                EFSA_ADDITIVES,
                HealthStatus.RECOMMENDED
        ));

        entries.add(new AdditiveEntry(
                "Yellow 6",
                "Color additive",
                "Sunset Yellow FCF, FD&C Yellow No. 6, E110",
                "Adds orange-yellow color.",
                "Common in cheese snacks, cereals, beverages, candy, and desserts.",
                "Purely for appearance; some organizations recommend limiting synthetic dyes.",
                "FDA Color Additives",
                "https://www.fda.gov/food/color-additives-information-consumers/color-additives-foods",
                HealthStatus.NOT_RECOMMENDED
        ));

        entries.add(new AdditiveEntry(
                "Red 3",
                "Color additive",
                "Erythrosine, FD&C Red No. 3, E127",
                "Adds cherry-pink color.",
                "Found in maraschino cherries, canned fruits, candy, and baked goods.",
                "A synthetic dye under increasing scrutiny by health advocacy groups.",
                "FDA Color Additives",
                "https://www.fda.gov/food/color-additives-information-consumers/color-additives-foods",
                HealthStatus.NOT_RECOMMENDED
        ));

        entries.add(new AdditiveEntry(
                "Maltitol",
                "Sugar alcohol / Sweetener",
                "Maltitol syrup, E965",
                "Adds sweetness with fewer calories and less impact on blood sugar than table sugar.",
                "Common in sugar-free candy, chocolate, protein bars, and 'diet' treats.",
                "Can cause significant digestive upset (bloating, laxative effect) if eaten in large amounts.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.MODERATE
        ));

        entries.add(new AdditiveEntry(
                "Isomalt",
                "Sugar alcohol / Sweetener",
                "E953",
                "Adds sweetness and technical properties like shine to candies.",
                "Found in hard candies, cough drops, and sugar-free decorations.",
                "Low glycemic index but can cause gas and bloating in sensitive people.",
                "EFSA Sweeteners",
                "https://www.efsa.europa.eu/en/topics/topic/sweeteners",
                HealthStatus.MODERATE
        ));

        entries.add(new AdditiveEntry(
                "Allulose",
                "Low-calorie sweetener",
                "Rare sugar",
                "Tastes like sugar but is mostly not metabolized, adding very few calories.",
                "Common in keto bars, sugar-free syrups, ice creams, and low-carb snacks.",
                "A newer sweetener often used to reduce total sugar on the label.",
                "FDA High-Intensity Sweeteners",
                FDA_SWEETENERS,
                HealthStatus.RECOMMENDED
        ));

        entries.add(new AdditiveEntry(
                "Sunflower oil",
                "Added oil",
                "Sunflower seed oil",
                "Adds fat and is used for frying or baking.",
                "Common in chips, snacks, crackers, and sauces.",
                "Rich in omega-6 fats; health impact depends on the overall diet balance.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.MODERATE
        ));

        entries.add(new AdditiveEntry(
                "Coconut oil",
                "Added fat",
                "Coconut fat",
                "Adds saturated fat and a specific texture or flavor.",
                "Common in vegan baked goods, non-dairy creamers, and snack bars.",
                "High in saturated fat. Use in moderation within a balanced diet.",
                "WHO Healthy Diet",
                "https://www.who.int/news-room/fact-sheets/detail/healthy-diet",
                HealthStatus.MODERATE
        ));

        entries.add(new AdditiveEntry(
                "Disodium guanylate",
                "Flavor enhancer",
                "E627",
                "Works with MSG to intensify savory flavors.",
                "Often in savory snacks, instant noodles, and processed soups.",
                "A signal of highly flavored, high-sodium savory products.",
                "FDA Food Additives and GRAS Ingredients",
                "https://www.fda.gov/food/food-ingredients-packaging/food-additives-and-gras-ingredients-information-consumers",
                HealthStatus.NOT_RECOMMENDED
        ));

        entries.add(new AdditiveEntry(
                "Disodium inosinate",
                "Flavor enhancer",
                "E631",
                "Works together with MSG and Disodium Guanylate to create umami.",
                "Common in ramen, chips, canned stews, and seasoned snacks.",
                "Indicates a product heavily engineered for taste intensity.",
                "FDA Food Additives and GRAS Ingredients",
                "https://www.fda.gov/food/food-ingredients-packaging/food-additives-and-gras-ingredients-information-consumers",
                HealthStatus.NOT_RECOMMENDED
        ));

        entries.add(new AdditiveEntry(
                "Hydrolyzed vegetable protein",
                "Flavor enhancer",
                "HVP, hydrolyzed soy protein",
                "Adds savory, meaty flavor through chemically broken-down proteins.",
                "Common in bouillons, gravies, meat products, and salty snacks.",
                "Contains naturally occurring glutamates (like MSG) to boost flavor.",
                "FDA Food Additives and GRAS Ingredients",
                "https://www.fda.gov/food/food-ingredients-packaging/food-additives-and-gras-ingredients-information-consumers",
                HealthStatus.NOT_RECOMMENDED
        ));

        entries.add(new AdditiveEntry(
                "Carnauba wax",
                "Glazing agent",
                "E903",
                "Adds a shiny protective coating to foods.",
                "Common in hard candies, gummies, sprinkles, and fruit coatings.",
                "Used to make food look shiny and prevent sticking; not a nutrient.",
                "EFSA Food Additives",
                EFSA_ADDITIVES,
                HealthStatus.RECOMMENDED
        ));

        entries.add(new AdditiveEntry(
                "Propylene glycol",
                "Humectant / Solvent",
                "E1520",
                "Maintains moisture and carries flavors or colors in liquid foods.",
                "Found in some salad dressings, drink mixes, frostings, and fast foods.",
                "A functional processing aid; presence usually indicates a highly formulated product.",
                "FDA Food Additives and GRAS Ingredients",
                "https://www.fda.gov/food/food-ingredients-packaging/food-additives-and-gras-ingredients-information-consumers",
                HealthStatus.NOT_RECOMMENDED
        ));

        entries.add(new AdditiveEntry(
                "Glycerin",
                "Humectant / Sweetener",
                "Glycerol, E422",
                "Helps keep foods moist and can add a slight sweetness.",
                "Common in protein bars, candies, and soft-baked goods.",
                "Keeps processed snacks from drying out or becoming hard.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.MODERATE
        ));

        entries.add(new AdditiveEntry(
                "Blue 2",
                "Color additive",
                "Indigotine, E132",
                "Adds dark blue color.",
                "Common in baked goods, cereals, snack foods, ice cream, and candy.",
                "Synthetic color; used for appearance, not nutrition.",
                "FDA Color Additives",
                "https://www.fda.gov/food/color-additives-information-consumers/color-additives-foods",
                HealthStatus.NOT_RECOMMENDED
        ));

        entries.add(new AdditiveEntry(
                "Green 3",
                "Color additive",
                "Fast Green FCF, E143",
                "Adds turquoise-green color.",
                "Found in candies, beverages, desserts, and ice cream.",
                "Another synthetic dye used purely for visual appeal.",
                "FDA Color Additives",
                "https://www.fda.gov/food/color-additives-information-consumers/color-additives-foods",
                HealthStatus.NOT_RECOMMENDED
        ));

        entries.add(new AdditiveEntry(
                "Maltitol syrup",
                "Sugar alcohol / Sweetener",
                "Lycasin",
                "Adds sweetness and bulk with fewer calories than sugar.",
                "Common in 'sugar-free' syrups, gummy candies, and chocolate coatings.",
                "Like maltitol powder, it can cause digestive upset in many people.",
                "FDA Types of Food Ingredients",
                FDA_TYPES,
                HealthStatus.MODERATE
        ));

        entries.add(new AdditiveEntry(
                "Caffeine",
                "Stimulant / Flavoring",
                "Added caffeine",
                "Adds a bitter flavor and provides a stimulant effect.",
                "Common in sodas, energy drinks, and some snack bars.",
                "Effect varies by person; check the label for total caffeine content.",
                "FDA Food Additives and GRAS Ingredients",
                "https://www.fda.gov/food/food-ingredients-packaging/food-additives-and-gras-ingredients-information-consumers",
                HealthStatus.MODERATE
        ));

        entries.add(new AdditiveEntry(
                "Quinine",
                "Flavoring",
                "Quinine hydrochloride",
                "Adds a characteristic bitter flavor.",
                "Primarily found in tonic water and some bitter lemon drinks.",
                "Used for flavor; limit intake if sensitive to quinine.",
                "FDA Food Additives and GRAS Ingredients",
                "https://www.fda.gov/food/food-ingredients-packaging/food-additives-and-gras-ingredients-information-consumers",
                HealthStatus.MODERATE
        ));

        return Collections.unmodifiableList(entries);
    }
}
