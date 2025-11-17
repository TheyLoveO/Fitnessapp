import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents an entry in the nutrition list.
 */
public class NutritionEntry {
    final UUID id = UUID.randomUUID();
    final LocalDateTime loggedAt = LocalDateTime.now();
    String itemName; int grams; int calories;
    /* Creates an instance of NutritionEntry with the given parameters */
    NutritionEntry(String itemName, int grams, int calories) { 
        this.itemName = itemName; 
        this.grams = grams; 
        this.calories = calories;
    }
}