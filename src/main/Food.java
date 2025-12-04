package main;
import java.util.List;


/**
 * Represents a given food.
 */
public class Food {
    final String name;
    final int kcalPer100g;

    /**
     * Creates an instance of the Food class.
     * @param name the name of the fod
     * @param kcalPer100g how many kilocalories per 100 grams
     * @return the new instance of Food
     */
    Food(String name, int kcalPer100g) { 
        this.name = name; 
        this.kcalPer100g = kcalPer100g; 
    }
    /**
     * Converts the Food object to a descriptive string.
     * @return the string derived from the object
     */
    @Override public String toString() { 
        return name + " (" + kcalPer100g + " kcal/100g)";
    }

    /**
     * Gives a list of the default foods that can be chosen from within the application.
     * @return the complete list of foods
     */
    public static List<Food> getDefaultFoods() {
        return List.of(
            new Food("Chicken Breast (cooked)", 165),
            new Food("White Rice (cooked)", 130),
            new Food("Oatmeal (dry)", 370),
            new Food("Egg", 143),
            new Food("Banana", 89),
            new Food("Apple", 52),
            new Food("Peanut Butter", 588),
            new Food("Broccoli", 34),
            new Food("Greek Yogurt (plain)", 59),
            new Food("Olive Oil", 884)
        );
    }
}
