package main;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Interfaces with the memory store object - contains mostly setter & getter functions.
 */
public class WorkoutService {
    private final InMemoryStore store; 
    WorkoutService(InMemoryStore s) {
        store=s;
    }

    /**
     * Logs a workout to the memory store object.
     * @param u the user
     * @param w the workout
     */
    void logWorkout(User u, Workout w, Database d) {

        store.addWorkout(u.email, w);
        d.writeWorkouts(store.workoutsByEmail);
    }

    /**
     * Gives a list of workouts from the user.
     * @param u the user
     * @return a list of workouts from the user
     */
    List<Workout> listWorkouts(User u) { 
        return store.getWorkouts(u.email); 
    
    }
    /**
     * (this function is deprecated)
     */
    long countWorkoutsInWeek(User u, LocalDate weekStart) { // (not used anymore; kept for reference)
        LocalDate weekEnd = weekStart.plusDays(6);
        return listWorkouts(u).stream().filter(w -> {
            LocalDate d = LocalDateTime.parse(w.startedAt).toLocalDate();
            return !d.isBefore(weekStart) && !d.isAfter(weekEnd);
        }).count();
    }
}