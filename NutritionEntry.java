import java.time.LocalDateTime;
import java.util.UUID;

public class NutritionEntry {
    final UUID id = UUID.randomUUID();
    final LocalDateTime loggedAt = LocalDateTime.now();
    String itemName; int grams; int calories;
    NutritionEntry(String itemName, int grams, int calories) { this.itemName = itemName; this.grams = grams; this.calories = calories; }
}