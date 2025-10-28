import java.time.LocalDate;
import java.util.List;

public class WorkoutService {
    private final InMemoryStore store; 
    WorkoutService(InMemoryStore s) {
        store=s;
    }
    void logWorkout(User u, Workout w) { 
        store.addWorkout(u.email, w); 
    }
    List<Workout> listWorkouts(User u) { 
        return store.getWorkouts(u.email); 
    
    }
    long countWorkoutsInWeek(User u, LocalDate weekStart) { // (not used anymore; kept for reference)
        LocalDate weekEnd = weekStart.plusDays(6);
        return listWorkouts(u).stream().filter(w -> {
            LocalDate d = w.startedAt.toLocalDate();
            return !d.isBefore(weekStart) && !d.isAfter(weekEnd);
        }).count();
    }
}