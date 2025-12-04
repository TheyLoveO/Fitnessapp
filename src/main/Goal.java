package main;


/**
 * Represents a goal with a daily burn target.
 */
public class Goal {
    Integer dailyBurnTarget;
    
     /**
     * Converts the Goal object to a descriptive string.
     * @return the string derived from the object
     */
    @Override public String toString() {
        return "Goal{dailyBurnTarget=" + dailyBurnTarget + "}";
    }
}
