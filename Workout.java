import java.time.LocalDateTime;
import java.util.UUID;

public class Workout {
    final UUID id = UUID.randomUUID();
    LocalDateTime startedAt = LocalDateTime.now();
    String type;            // Run, Lift, Cycle, Walk, Swim
    String bodyPart;        // for Lift/strength
    // Strength
    String exerciseName;
    Integer sets;
    Integer reps;
    // Cardio
    String distanceUnit;    // "Miles" or "Steps"
    Double distanceValue;
    Integer steps;
    // Shared
    int durationMin;
    int estimatedCalories;

    static Workout strength(String type, String bodyPart, LocalDateTime start, String exName, int sets, int reps, int minutes, int kcal) {
        Workout w = new Workout();
        w.type = type; w.bodyPart = bodyPart; w.startedAt = start;
        w.exerciseName = exName; w.sets = sets; w.reps = reps;
        w.durationMin = minutes; w.estimatedCalories = kcal;
        return w;
    }
    
    static Workout cardio(String type, LocalDateTime start, String unit, Double miles, Integer steps, int minutes, int kcal) {
        Workout w = new Workout();
        w.type = type; w.startedAt = start;
        w.distanceUnit = unit; w.distanceValue = miles; w.steps = steps;
        w.durationMin = minutes; w.estimatedCalories = kcal;
        return w;
    }
}