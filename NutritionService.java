import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class NutritionService {
    private final InMemoryStore store; NutritionService(InMemoryStore s){store=s;}
    void logNutrition(User u, String item, int grams, int kcal){ store.addNutrition(u.email, new NutritionEntry(item, grams, kcal)); }
    int dailyCalories(User u, LocalDate date){ return store.getNutrition(u.email).stream().filter(n -> n.loggedAt.toLocalDate().equals(date)).mapToInt(n -> n.calories).sum(); }
    List<NutritionEntry> listToday(User u){ LocalDate t=LocalDate.now(); return store.getNutrition(u.email).stream().filter(n -> n.loggedAt.toLocalDate().equals(t)).collect(Collectors.toList()); }
}