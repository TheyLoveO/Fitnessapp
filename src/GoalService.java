/**
 * Used for updating a user's goals.
 */
public class GoalService { 
    /**
     * Sets a daily burn target for a given user.
     * @param u the user who will have their burn target updates
     * @param k the burn target in kilocalories
     */
    void setDailyBurn(User u, Integer k) {
        u.goal.dailyBurnTarget=k;
    } 
}