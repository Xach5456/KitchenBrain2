package com.example.kitchenbrain.data;

import com.example.kitchenbrain.model.FoodProduct;
import java.util.ArrayList;
import java.util.List;

/**
 * Comprehensive database of food products/ingredients
 * Contains 100+ products across multiple categories
 */
public class ProductDatabase {
    
    // Category constants
    public static final String CATEGORY_VEGETABLES = "Vegetables";
    public static final String CATEGORY_FRUITS = "Fruits";
    public static final String CATEGORY_MEAT = "Meat & Poultry";
    public static final String CATEGORY_SEAFOOD = "Fish & Seafood";
    public static final String CATEGORY_DAIRY = "Dairy Products";
    public static final String CATEGORY_GRAINS = "Grains & Pasta";
    public static final String CATEGORY_SPICES = "Spices & Herbs";
    public static final String CATEGORY_OTHER = "Other Ingredients";
    
    /**
     * Get all available products
     */
    public static List<FoodProduct> getAllProducts() {
        List<FoodProduct> products = new ArrayList<>();
        
        // Add all categories
        products.addAll(getVegetables());
        products.addAll(getFruits());
        products.addAll(getMeatAndPoultry());
        products.addAll(getFishAndSeafood());
        products.addAll(getDairyProducts());
        products.addAll(getGrainsAndPasta());
        products.addAll(getSpicesAndHerbs());
        products.addAll(getOtherIngredients());
        
        return products;
    }
    
    /**
     * Get products by category
     */
    public static List<FoodProduct> getProductsByCategory(String category) {
        switch (category) {
            case CATEGORY_VEGETABLES:
                return getVegetables();
            case CATEGORY_FRUITS:
                return getFruits();
            case CATEGORY_MEAT:
                return getMeatAndPoultry();
            case CATEGORY_SEAFOOD:
                return getFishAndSeafood();
            case CATEGORY_DAIRY:
                return getDairyProducts();
            case CATEGORY_GRAINS:
                return getGrainsAndPasta();
            case CATEGORY_SPICES:
                return getSpicesAndHerbs();
            case CATEGORY_OTHER:
                return getOtherIngredients();
            default:
                return new ArrayList<>();
        }
    }
    
    /**
     * Search products by query
     */
    public static List<FoodProduct> searchProducts(String query) {
        List<FoodProduct> allProducts = getAllProducts();
        List<FoodProduct> results = new ArrayList<>();
        
        for (FoodProduct product : allProducts) {
            if (product.matchesQuery(query)) {
                results.add(product);
            }
        }
        
        return results;
    }
    
    // ===== VEGETABLES (20 products) =====
    public static List<FoodProduct> getVegetables() {
        List<FoodProduct> vegetables = new ArrayList<>();
        vegetables.add(new FoodProduct("veg_001", "Tomato", CATEGORY_VEGETABLES, "🍅", "tomatoes", "cherry tomato"));
        vegetables.add(new FoodProduct("veg_002", "Potato", CATEGORY_VEGETABLES, "🥔", "potatoes", "spud"));
        vegetables.add(new FoodProduct("veg_003", "Onion", CATEGORY_VEGETABLES, "🧅", "onions", "red onion", "green onion"));
        vegetables.add(new FoodProduct("veg_004", "Carrot", CATEGORY_VEGETABLES, "🥕", "carrots"));
        vegetables.add(new FoodProduct("veg_005", "Broccoli", CATEGORY_VEGETABLES, "🥦", "broccoli florets"));
        vegetables.add(new FoodProduct("veg_006", "Cucumber", CATEGORY_VEGETABLES, "🥒", "cucumbers", "gherkin"));
        vegetables.add(new FoodProduct("veg_007", "Bell Pepper", CATEGORY_VEGETABLES, "🫑", "pepper", "peppers", "capsicum", "sweet pepper"));
        vegetables.add(new FoodProduct("veg_008", "Eggplant", CATEGORY_VEGETABLES, "🍆", "aubergine", "brinjal"));
        vegetables.add(new FoodProduct("veg_009", "Corn", CATEGORY_VEGETABLES, "🌽", "maize", "corn kernels", "sweet corn"));
        vegetables.add(new FoodProduct("veg_010", "Lettuce", CATEGORY_VEGETABLES, "🥬", "lettuce leaves", "iceberg", "romaine"));
        vegetables.add(new FoodProduct("veg_011", "Spinach", CATEGORY_VEGETABLES, "🍃", "spinach leaves", "baby spinach"));
        vegetables.add(new FoodProduct("veg_012", "Garlic", CATEGORY_VEGETABLES, "🧄", "garlic cloves", "garlic bulb"));
        vegetables.add(new FoodProduct("veg_013", "Ginger", CATEGORY_VEGETABLES, "🫚", "ginger root", "fresh ginger"));
        vegetables.add(new FoodProduct("veg_014", "Mushroom", CATEGORY_VEGETABLES, "🍄", "mushrooms", "button mushroom", "portobello"));
        vegetables.add(new FoodProduct("veg_015", "Zucchini", CATEGORY_VEGETABLES, "🥒", "courgette", "summer squash"));
        vegetables.add(new FoodProduct("veg_016", "Celery", CATEGORY_VEGETABLES, "🌿", "celery stalks", "celery sticks"));
        vegetables.add(new FoodProduct("veg_017", "Asparagus", CATEGORY_VEGETABLES, "🎋", "asparagus spears"));
        vegetables.add(new FoodProduct("veg_018", "Cabbage", CATEGORY_VEGETABLES, "🥬", "cabbage", "green cabbage", "red cabbage"));
        vegetables.add(new FoodProduct("veg_019", "Cauliflower", CATEGORY_VEGETABLES, "🥦", "cauliflower florets"));
        vegetables.add(new FoodProduct("veg_020", "Pumpkin", CATEGORY_VEGETABLES, "🎃", "pumpkin flesh", "butternut squash"));
        return vegetables;
    }
    
    // ===== FRUITS (18 products) =====
    public static List<FoodProduct> getFruits() {
        List<FoodProduct> fruits = new ArrayList<>();
        fruits.add(new FoodProduct("fruit_001", "Apple", CATEGORY_FRUITS, "🍎", "apples", "red apple", "green apple"));
        fruits.add(new FoodProduct("fruit_002", "Banana", CATEGORY_FRUITS, "🍌", "bananas"));
        fruits.add(new FoodProduct("fruit_003", "Orange", CATEGORY_FRUITS, "🍊", "oranges", "mandarin", "tangerine"));
        fruits.add(new FoodProduct("fruit_004", "Strawberry", CATEGORY_FRUITS, "🍓", "strawberries"));
        fruits.add(new FoodProduct("fruit_005", "Lemon", CATEGORY_FRUITS, "🍋", "lemons", "lemon juice"));
        fruits.add(new FoodProduct("fruit_006", "Mango", CATEGORY_FRUITS, "🥭", "mangoes", "mango flesh"));
        fruits.add(new FoodProduct("fruit_007", "Pineapple", CATEGORY_FRUITS, "🍍", "pineapple chunks"));
        fruits.add(new FoodProduct("fruit_008", "Grape", CATEGORY_FRUITS, "🍇", "grapes", "red grapes", "green grapes"));
        fruits.add(new FoodProduct("fruit_009", "Watermelon", CATEGORY_FRUITS, "🍉", "watermelon flesh"));
        fruits.add(new FoodProduct("fruit_010", "Peach", CATEGORY_FRUITS, "🍑", "peaches"));
        fruits.add(new FoodProduct("fruit_011", "Pear", CATEGORY_FRUITS, "🍐", "pears"));
        fruits.add(new FoodProduct("fruit_012", "Cherry", CATEGORY_FRUITS, "🍒", "cherries"));
        fruits.add(new FoodProduct("fruit_013", "Blueberry", CATEGORY_FRUITS, "🔵", "blueberries"));
        fruits.add(new FoodProduct("fruit_014", "Raspberry", CATEGORY_FRUITS, "🍇", "raspberries"));
        fruits.add(new FoodProduct("fruit_015", "Kiwi", CATEGORY_FRUITS, "🥝", "kiwifruit", "kiwi fruit"));
        fruits.add(new FoodProduct("fruit_016", "Avocado", CATEGORY_FRUITS, "🥑", "avocados", "avocado flesh"));
        fruits.add(new FoodProduct("fruit_017", "Coconut", CATEGORY_FRUITS, "🥥", "coconut milk", "coconut cream"));
        fruits.add(new FoodProduct("fruit_018", "Papaya", CATEGORY_FRUITS, "🟠", "papaya", "pawpaw"));
        return fruits;
    }
    
    // ===== MEAT & POULTRY (12 products) =====
    public static List<FoodProduct> getMeatAndPoultry() {
        List<FoodProduct> meats = new ArrayList<>();
        meats.add(new FoodProduct("meat_001", "Chicken", CATEGORY_MEAT, "🍗", "chicken breast", "chicken thighs", "chicken wings", "whole chicken"));
        meats.add(new FoodProduct("meat_002", "Beef", CATEGORY_MEAT, "🥩", "steak", "ground beef", "minced beef", "beef steak"));
        meats.add(new FoodProduct("meat_003", "Pork", CATEGORY_MEAT, "🥓", "pork chops", "bacon", "ham", "pork belly"));
        meats.add(new FoodProduct("meat_004", "Turkey", CATEGORY_MEAT, "🦃", "turkey breast", "ground turkey"));
        meats.add(new FoodProduct("meat_005", "Lamb", CATEGORY_MEAT, "🐑", "lamb chops", "ground lamb", "leg of lamb"));
        meats.add(new FoodProduct("meat_006", "Sausage", CATEGORY_MEAT, "🌭", "sausages", "bratwurst", "chorizo"));
        meats.add(new FoodProduct("meat_007", "Meatballs", CATEGORY_MEAT, "🍖", "beef meatballs", "pork meatballs"));
        meats.add(new FoodProduct("meat_008", "Chicken Liver", CATEGORY_MEAT, "🫀", "liver", "chicken livers"));
        meats.add(new FoodProduct("meat_009", "Ribs", CATEGORY_MEAT, "🍖", "pork ribs", "beef ribs", "BBQ ribs"));
        meats.add(new FoodProduct("meat_010", "Duck", CATEGORY_MEAT, "🦆", "duck breast", "whole duck"));
        meats.add(new FoodProduct("meat_011", "Veal", CATEGORY_MEAT, "🐂", "veal cutlets", "veal scallops"));
        meats.add(new FoodProduct("meat_012", "Venison", CATEGORY_MEAT, "🦌", "deer meat", "venison steak"));
        return meats;
    }
    
    // ===== FISH & SEAFOOD (12 products) =====
    public static List<FoodProduct> getFishAndSeafood() {
        List<FoodProduct> seafood = new ArrayList<>();
        seafood.add(new FoodProduct("fish_001", "Salmon", CATEGORY_SEAFOOD, "🐟", "salmon fillet", "smoked salmon"));
        seafood.add(new FoodProduct("fish_002", "Tuna", CATEGORY_SEAFOOD, "🐟", "tuna steak", "canned tuna", "tuna fillet"));
        seafood.add(new FoodProduct("fish_003", "Shrimp", CATEGORY_SEAFOOD, "🍤", "prawns", "king prawns", "tiger prawns"));
        seafood.add(new FoodProduct("fish_004", "Cod", CATEGORY_SEAFOOD, "🐟", "cod fillet", "cod steak"));
        seafood.add(new FoodProduct("fish_005", "Crab", CATEGORY_SEAFOOD, "🦀", "crab meat", "blue crab", "dungeness crab"));
        seafood.add(new FoodProduct("fish_006", "Lobster", CATEGORY_SEAFOOD, "🦞", "lobster tail", "lobster meat"));
        seafood.add(new FoodProduct("fish_007", "Squid", CATEGORY_SEAFOOD, "🦑", "calamari", "squid rings"));
        seafood.add(new FoodProduct("fish_008", "Oyster", CATEGORY_SEAFOOD, "🦪", "oysters", "fresh oysters"));
        seafood.add(new FoodProduct("fish_009", "Mussels", CATEGORY_SEAFOOD, "🦪", "blue mussels", "green mussels"));
        seafood.add(new FoodProduct("fish_010", "Scallops", CATEGORY_SEAFOOD, "🐚", "sea scallops", "bay scallops"));
        seafood.add(new FoodProduct("fish_011", "Tilapia", CATEGORY_SEAFOOD, "🐠", "tilapia fillet"));
        seafood.add(new FoodProduct("fish_012", "Sardines", CATEGORY_SEAFOOD, "🐟", "canned sardines", "fresh sardines"));
        return seafood;
    }
    
    // ===== DAIRY PRODUCTS (12 products) =====
    public static List<FoodProduct> getDairyProducts() {
        List<FoodProduct> dairy = new ArrayList<>();
        dairy.add(new FoodProduct("dairy_001", "Milk", CATEGORY_DAIRY, "🥛", "whole milk", "skim milk", "almond milk", "soy milk"));
        dairy.add(new FoodProduct("dairy_002", "Cheese", CATEGORY_DAIRY, "🧀", "cheddar cheese", "mozzarella", "parmesan", "swiss cheese"));
        dairy.add(new FoodProduct("dairy_003", "Butter", CATEGORY_DAIRY, "🧈", "unsalted butter", "salted butter"));
        dairy.add(new FoodProduct("dairy_004", "Yogurt", CATEGORY_DAIRY, "🥛", "greek yogurt", "plain yogurt", "vanilla yogurt"));
        dairy.add(new FoodProduct("dairy_005", "Cream", CATEGORY_DAIRY, "🥛", "heavy cream", "whipping cream", "sour cream"));
        dairy.add(new FoodProduct("dairy_006", "Cottage Cheese", CATEGORY_DAIRY, "🥛", "low fat cottage cheese"));
        dairy.add(new FoodProduct("dairy_007", "Cream Cheese", CATEGORY_DAIRY, "🧀", "philadelphia cream cheese"));
        dairy.add(new FoodProduct("dairy_008", "Mozzarella", CATEGORY_DAIRY, "🧀", "fresh mozzarella", "buffalo mozzarella"));
        dairy.add(new FoodProduct("dairy_009", "Parmesan", CATEGORY_DAIRY, "🧀", "parmesan cheese", "parmigiano-reggiano"));
        dairy.add(new FoodProduct("dairy_010", "Feta Cheese", CATEGORY_DAIRY, "🧀", "feta", "greek feta"));
        dairy.add(new FoodProduct("dairy_011", "Ricotta", CATEGORY_DAIRY, "🧀", "ricotta cheese"));
        dairy.add(new FoodProduct("dairy_012", "Ice Cream", CATEGORY_DAIRY, "🍦", "vanilla ice cream", "chocolate ice cream"));
        return dairy;
    }
    
    // ===== GRAINS & PASTA (14 products) =====
    public static List<FoodProduct> getGrainsAndPasta() {
        List<FoodProduct> grains = new ArrayList<>();
        grains.add(new FoodProduct("grain_001", "Rice", CATEGORY_GRAINS, "🍚", "white rice", "brown rice", "jasmine rice", "basmati rice"));
        grains.add(new FoodProduct("grain_002", "Pasta", CATEGORY_GRAINS, "🍝", "spaghetti", "penne", "fusilli", "macaroni"));
        grains.add(new FoodProduct("grain_003", "Bread", CATEGORY_GRAINS, "🍞", "white bread", "whole wheat bread", "sourdough"));
        grains.add(new FoodProduct("grain_004", "Oats", CATEGORY_GRAINS, "🌾", "oatmeal", "rolled oats", "instant oats"));
        grains.add(new FoodProduct("grain_005", "Flour", CATEGORY_GRAINS, "🌾", "all-purpose flour", "wheat flour", "bread flour"));
        grains.add(new FoodProduct("grain_006", "Noodles", CATEGORY_GRAINS, "🍜", "egg noodles", "rice noodles", "ramen noodles"));
        grains.add(new FoodProduct("grain_007", "Quinoa", CATEGORY_GRAINS, "🌾", "white quinoa", "red quinoa"));
        grains.add(new FoodProduct("grain_008", "Barley", CATEGORY_GRAINS, "🌾", "pearl barley"));
        grains.add(new FoodProduct("grain_009", "Couscous", CATEGORY_GRAINS, "🌾", "moroccan couscous", "Israeli couscous"));
        grains.add(new FoodProduct("grain_010", "Tortilla", CATEGORY_GRAINS, "🌮", "flour tortillas", "corn tortillas"));
        grains.add(new FoodProduct("grain_011", "Pizza Dough", CATEGORY_GRAINS, "🍕", "pizza base", "pizza crust"));
        grains.add(new FoodProduct("grain_012", "Lasagna Sheets", CATEGORY_GRAINS, "🍝", "lasagna pasta"));
        grains.add(new FoodProduct("grain_013", "Semolina", CATEGORY_GRAINS, "🌾", "semolina flour"));
        grains.add(new FoodProduct("grain_014", "Cornmeal", CATEGORY_GRAINS, "🌽", "polenta", "corn grits"));
        return grains;
    }
    
    // ===== SPICES & HERBS (15 products) =====
    public static List<FoodProduct> getSpicesAndHerbs() {
        List<FoodProduct> spices = new ArrayList<>();
        spices.add(new FoodProduct("spice_001", "Salt", CATEGORY_SPICES, "🧂", "table salt", "sea salt", "kosher salt"));
        spices.add(new FoodProduct("spice_002", "Black Pepper", CATEGORY_SPICES, "⚫", "pepper", "ground pepper", "peppercorns"));
        spices.add(new FoodProduct("spice_003", "Paprika", CATEGORY_SPICES, "🌶️", "sweet paprika", "smoked paprika", "hot paprika"));
        spices.add(new FoodProduct("spice_004", "Basil", CATEGORY_SPICES, "🌿", "fresh basil", "dried basil", "thai basil"));
        spices.add(new FoodProduct("spice_005", "Oregano", CATEGORY_SPICES, "🌿", "dried oregano", "fresh oregano"));
        spices.add(new FoodProduct("spice_006", "Thyme", CATEGORY_SPICES, "🌿", "fresh thyme", "dried thyme"));
        spices.add(new FoodProduct("spice_007", "Rosemary", CATEGORY_SPICES, "🌿", "fresh rosemary", "dried rosemary"));
        spices.add(new FoodProduct("spice_008", "Cumin", CATEGORY_SPICES, "🌶️", "ground cumin", "cumin seeds"));
        spices.add(new FoodProduct("spice_009", "Cinnamon", CATEGORY_SPICES, "🍂", "ground cinnamon", "cinnamon sticks"));
        spices.add(new FoodProduct("spice_010", "Turmeric", CATEGORY_SPICES, "🌶️", "ground turmeric", "fresh turmeric"));
        spices.add(new FoodProduct("spice_011", "Chili Powder", CATEGORY_SPICES, "🌶️", "chili flakes", "cayenne pepper"));
        spices.add(new FoodProduct("spice_012", "Nutmeg", CATEGORY_SPICES, "🌰", "ground nutmeg", "whole nutmeg"));
        spices.add(new FoodProduct("spice_013", "Bay Leaves", CATEGORY_SPICES, "🍂", "bay leaf", "dried bay leaves"));
        spices.add(new FoodProduct("spice_014", "Parsley", CATEGORY_SPICES, "🌿", "fresh parsley", "dried parsley"));
        spices.add(new FoodProduct("spice_015", "Mint", CATEGORY_SPICES, "🌿", "fresh mint", "dried mint", "peppermint"));
        return spices;
    }
    
    // ===== OTHER INGREDIENTS (12 products) =====
    public static List<FoodProduct> getOtherIngredients() {
        List<FoodProduct> other = new ArrayList<>();
        other.add(new FoodProduct("other_001", "Eggs", CATEGORY_OTHER, "🥚", "eggs", "egg whites", "egg yolks"));
        other.add(new FoodProduct("other_002", "Sugar", CATEGORY_OTHER, "🍬", "white sugar", "brown sugar", "powdered sugar"));
        other.add(new FoodProduct("other_003", "Honey", CATEGORY_OTHER, "🍯", "pure honey", "raw honey"));
        other.add(new FoodProduct("other_004", "Olive Oil", CATEGORY_OTHER, "🫒", "extra virgin olive oil", "olive oil"));
        other.add(new FoodProduct("other_005", "Vegetable Oil", CATEGORY_OTHER, "🌻", "canola oil", "sunflower oil"));
        other.add(new FoodProduct("other_006", "Vinegar", CATEGORY_OTHER, "🍶", "white vinegar", "apple cider vinegar", "balsamic vinegar"));
        other.add(new FoodProduct("other_007", "Soy Sauce", CATEGORY_OTHER, "🫙", "light soy sauce", "dark soy sauce"));
        other.add(new FoodProduct("other_008", "Tomato Paste", CATEGORY_OTHER, "🍅", "tomato puree", "tomato concentrate"));
        other.add(new FoodProduct("other_009", "Peanut Butter", CATEGORY_OTHER, "🥜", "creamy peanut butter", "chunky peanut butter"));
        other.add(new FoodProduct("other_010", "Almonds", CATEGORY_OTHER, "🌰", "almonds", "sliced almonds", "ground almonds"));
        other.add(new FoodProduct("other_011", "Walnuts", CATEGORY_OTHER, "🌰", "walnuts", "chopped walnuts"));
        other.add(new FoodProduct("other_012", "Chocolate", CATEGORY_OTHER, "🍫", "dark chocolate", "milk chocolate", "cocoa powder"));
        return other;
    }
}
