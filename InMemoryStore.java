import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class InMemoryStore {
    final Map<String, User> usersByEmail = new HashMap<>();
    final Map<String, List<Workout>> workoutsByEmail = new HashMap<>();
    final Map<String, List<NutritionEntry>> nutritionByEmail = new HashMap<>();
    User getOrCreateUser(String email, String name) { return usersByEmail.computeIfAbsent(email, e -> new User(email, name)); }
    void addWorkout(String email, Workout w) { workoutsByEmail.computeIfAbsent(email, k -> new ArrayList<>()).add(w); }
    void addNutrition(String email, NutritionEntry n) { nutritionByEmail.computeIfAbsent(email, k -> new ArrayList<>()).add(n); }
    List<Workout> getWorkouts(String email) { return workoutsByEmail.getOrDefault(email, Collections.emptyList()); }
    List<NutritionEntry> getNutrition(String email) { return nutritionByEmail.getOrDefault(email, Collections.emptyList()); }
}