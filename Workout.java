import java.time.LocalDateTime;
import java.util.UUID;

/*
 * Represents a workout the user has inputted.
 */
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

    /**
     * Creates an instance of Workout representing a strength activity (non cardio)
     * @param type the type of workout
     * @param bodyPart the body part being exercised
     * @param start the start time
     * @param exName the name of the exercise
     * @param sets the number of sets
     * @param reps the number of reps
     * @param minutes the elapsed time of the workout in minutes
     * @param kcal the kilocalories burned
     * @return
     */
    static Workout strength(String type, String bodyPart, LocalDateTime start, String exName, int sets, int reps, int minutes, int kcal) {
        Workout w = new Workout();
        w.type = type; w.bodyPart = bodyPart; w.startedAt = start;
        w.exerciseName = exName; w.sets = sets; w.reps = reps;
        w.durationMin = minutes; w.estimatedCalories = kcal;
        return w;
    }
    
    /**
     * Creates an instance of Workout representing a cardio activity
     * @param type the type of workout
     * @param start the start time
     * @param unit the distance unit
     * @param miles the number of miles
     * @param steps the number of steps
     * @param minutes the elapsed time of the workout in minutes
     * @param kcal the kilocalories burned
     * @return
     */
    static Workout cardio(String type, LocalDateTime start, String unit, Double miles, Integer steps, int minutes, int kcal) {
        Workout w = new Workout();
        w.type = type; w.startedAt = start;
        w.distanceUnit = unit; w.distanceValue = miles; w.steps = steps;
        w.durationMin = minutes; w.estimatedCalories = kcal;
        return w;
    }
}