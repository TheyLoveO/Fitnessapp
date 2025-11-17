import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/*
 * Represents an entry in the nutrition list.
 */
public class NutritionService {
    private final InMemoryStore store; 
    NutritionService(InMemoryStore s) {
        store=s;
    }

    /**
     * Logs a nutrition object created with the given parameters to the nutrition service's memory store.
     * @param u the user
     * @param item the name of the food
     * @param grams the amount of grams of the food
     * @param kcal the amount of kilocalories of the food
     */

    void logNutrition(User u, String item, int grams, int kcal) { 
        store.addNutrition(u.email, new NutritionEntry(item, grams, kcal)); 
    }

    /**
     * Gives the daily calorie count for a given date and user
     * @param u the user
     * @param date the date
     * @return an amount in kilocalories
     */
    int dailyCalories(User u, LocalDate date) { 
        return store.getNutrition(u.email).stream().filter(n -> n.loggedAt.toLocalDate().equals(date)).mapToInt(n -> n.calories).sum(); 
    }

    /**
     * Gives a list of nutrition objects from a user for the current day.
     * @param u the user
     * @return a list of nutrition entries
     */
    List<NutritionEntry> listToday(User u) { 
        LocalDate t=LocalDate.now();
        return store.getNutrition(u.email).stream().filter(n -> n.loggedAt.toLocalDate().equals(t)).collect(Collectors.toList()); 
    }
}