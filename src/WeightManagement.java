/**
 * (this class is currently in unused - in development)
 */
public class WeightManagement {

    //Some way for the user to put in their weight and what their weight goals are.
    int currentWeight;
    int goalWeight;
    int weightDifference;

    static void goal(int currentWeight, int goalWeight, int weightDifference) {
        WeightManagement w = new WeightManagement();
        w.currentWeight = currentWeight; w.goalWeight = goalWeight; w.weightDifference = weightDifference;
        weightDifference = currentWeight - goalWeight;
        //return weightDifference;
    }
}
