import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/*
 * Represents an entry in the nutrition list.
 */
public class NutritionService {
    private final InMemoryStore store;
    NutritionService(InMemoryStore s) {
        store = s;
    }

    /**
     * Logs a nutrition object created with the given parameters to the nutrition service's memory store,
     * using the current date and time.
     */
    void logNutrition(User u, String item, int grams, int kcal) {
        store.addNutrition(u.email, new NutritionEntry(item, grams, kcal));
    }

    /**
     * Logs a nutrition entry for a specific day (used by the day-cycling UI).
     */
    void logNutritionForDate(User u, String item, int grams, int kcal, LocalDate date) {
        LocalDateTime ts = LocalDateTime.of(date, LocalTime.now());
        store.addNutrition(u.email, new NutritionEntry(item, grams, kcal, ts));
    }

    /**
     * Gives the daily calorie count for a given date and user.
     */
    int dailyCalories(User u, LocalDate date) {
        return store.getNutrition(u.email).stream()
                .filter(n -> n.loggedAt.toLocalDate().equals(date))
                .mapToInt(n -> n.calories)
                .sum();
    }

    /**
     * Gives a list of nutrition objects from a user for the current day (system clock).
     */
    List<NutritionEntry> listToday(User u) {
        LocalDate t = LocalDate.now();
        return listForDate(u, t);
    }

    /**
     * Gives a list of nutrition objects from a user for a specific day.
     */
    List<NutritionEntry> listForDate(User u, LocalDate date) {
        return store.getNutrition(u.email).stream()
                .filter(n -> n.loggedAt.toLocalDate().equals(date))
                .collect(Collectors.toList());
    }


    /**
     * Returns all nutrition entries for a user between the given dates (inclusive).
     */
    List<NutritionEntry> listForRange(User u, LocalDate start, LocalDate end) {
        return store.getNutrition(u.email).stream()
                .filter(n -> {
                    LocalDate d = n.loggedAt.toLocalDate();
                    return !d.isBefore(start) && !d.isAfter(end);
                })
                .collect(Collectors.toList());
    }

}