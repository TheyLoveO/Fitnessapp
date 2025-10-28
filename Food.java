import java.util.List;

public class Food {
    final String name;
    final int kcalPer100g;
    Food(String name, int kcalPer100g) { this.name = name; this.kcalPer100g = kcalPer100g; }
    @Override public String toString() { return name + " (" + kcalPer100g + " kcal/100g)"; }

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
