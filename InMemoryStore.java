import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores relevent information in memory for the application.
 */
public class InMemoryStore {
    final Map<String, User> usersByEmail = new HashMap<>();
    final Map<String, List<Workout>> workoutsByEmail = new HashMap<>();
    final Map<String, List<NutritionEntry>> nutritionByEmail = new HashMap<>();
    
    /**
     * If the given email & name combination is not present, it is inserted into the hashmap. The new (or old) user is returned
     * @return the user with the given name & email
     * @param email the email of the user
     * @param name the name of the user
     */
    User getOrCreateUser(String email, String name) { 
        return usersByEmail.computeIfAbsent(email, e -> new User(email, name)); 
    }
    
    /**
     * Adds a workout to a corresponding email's workouts.
     * @param email the email the workout will be added to
     * @param w the workout added to the user's workouts
     */
    void addWorkout(String email, Workout w) { 
        workoutsByEmail.computeIfAbsent(email, k -> new ArrayList<>()).add(w); 
    }

    /**
     * @param email the email the workout will be added to
     * @param w the workout added to the user's workouts
     */
    void addNutrition(String email, NutritionEntry n) { 
        nutritionByEmail.computeIfAbsent(email, k -> new ArrayList<>()).add(n); 
    }

    /**
     * @param email the email the workouts will be retrieved from
     */
    List<Workout> getWorkouts(String email) { 
        return workoutsByEmail.getOrDefault(email, Collections.emptyList()); 
    }

    /**
     * @param email the email the nutrition will be retrieved from
     */
    List<NutritionEntry> getNutrition(String email) { 
        return nutritionByEmail.getOrDefault(email, Collections.emptyList()); 
    }
}