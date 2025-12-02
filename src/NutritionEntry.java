import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents an entry in the nutrition list.
 */
public class NutritionEntry {
    final UUID id = UUID.randomUUID();
    LocalDateTime loggedAt;
    String itemName;
    int grams;
    int calories;

    /* Creates an instance of NutritionEntry with the current date/time */
    NutritionEntry(String itemName, int grams, int calories) {
        this(itemName, grams, calories, LocalDateTime.now());
    }

    /* Creates an instance of NutritionEntry with an explicit timestamp */
    NutritionEntry(String itemName, int grams, int calories, LocalDateTime loggedAt) {
        this.itemName = itemName;
        this.grams = grams;
        this.calories = calories;
        this.loggedAt = loggedAt;
    }
}
