import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Minimal Fitness App with a simple Swing UI.
 * Single-file for easy compile/run:
 *   javac Main.java
 *   java Main
 */
public class Main {

    // ======== DOMAIN MODELS ========
    static class User {
        final String email;
        String name;
        Goal goal = new Goal();
        User(String email, String name) { this.email = email; this.name = name; }
    }

    static class Goal {
        Integer dailyCaloriesTarget;   // e.g., 2200 kcal
        Integer weeklyWorkoutsTarget;  // e.g., 4 per week
        @Override public String toString() {
            return "Goal{dailyCaloriesTarget=" + dailyCaloriesTarget +
                    ", weeklyWorkoutsTarget=" + weeklyWorkoutsTarget + "}";
        }
    }

    static class Workout {
        final UUID id = UUID.randomUUID();
        final LocalDateTime startedAt = LocalDateTime.now();
        String type;            // "Run", "Lift", "Cycle"
        int durationMin;        // minutes
        int estimatedCalories;  // kcal
        Workout(String type, int durationMin, int estimatedCalories) {
            this.type = type; this.durationMin = durationMin; this.estimatedCalories = estimatedCalories;
        }
        @Override public String toString() {
            return String.format("[%s] %s - %d min - %d kcal",
                    startedAt.toLocalDate(), type, durationMin, estimatedCalories);
        }
    }

    static class NutritionEntry {
        final UUID id = UUID.randomUUID();
        final LocalDateTime loggedAt = LocalDateTime.now();
        String itemName;
        int calories;   // kcal
        int protein;    // g
        int carbs;      // g
        int fats;       // g
        NutritionEntry(String itemName, int calories, int protein, int carbs, int fats) {
            this.itemName = itemName; this.calories = calories; this.protein = protein; this.carbs = carbs; this.fats = fats;
        }
        @Override public String toString() {
            return String.format("[%s] %s - %d kcal (P:%dg C:%dg F:%dg)",
                    loggedAt.toLocalDate(), itemName, calories, protein, carbs, fats);
        }
    }

    // ======== IN-MEMORY STORE ========
    static class InMemoryStore {
        final Map<String, User> usersByEmail = new HashMap<>();
        final Map<String, List<Workout>> workoutsByEmail = new HashMap<>();
        final Map<String, List<NutritionEntry>> nutritionByEmail = new HashMap<>();
        User getOrCreateUser(String email, String name) {
            return usersByEmail.computeIfAbsent(email, e -> new User(email, name));
        }
        void addWorkout(String email, Workout w) {
            workoutsByEmail.computeIfAbsent(email, k -> new ArrayList<>()).add(w);
        }
        void addNutrition(String email, NutritionEntry n) {
            nutritionByEmail.computeIfAbsent(email, k -> new ArrayList<>()).add(n);
        }
        List<Workout> getWorkouts(String email) {
            return workoutsByEmail.getOrDefault(email, Collections.emptyList());
        }
        List<NutritionEntry> getNutrition(String email) {
            return nutritionByEmail.getOrDefault(email, Collections.emptyList());
        }
    }

    // ======== SERVICES ========
    static class AuthService {
        private final InMemoryStore store;
        AuthService(InMemoryStore store) { this.store = store; }
        User signInOrSignUp(String email, String name) {
            return store.getOrCreateUser(email, name);
        }
    }

    static class WorkoutService {
        private final InMemoryStore store;
        WorkoutService(InMemoryStore store) { this.store = store; }
        void logWorkout(User user, String type, int minutes, int kcal) {
            store.addWorkout(user.email, new Workout(type, minutes, kcal));
        }
        List<Workout> listWorkouts(User user) {
            return store.getWorkouts(user.email);
        }
        long countWorkoutsInWeek(User user, LocalDate weekStart) {
            LocalDate weekEnd = weekStart.plusDays(6);
            return listWorkouts(user).stream()
                    .filter(w -> {
                        LocalDate d = w.startedAt.toLocalDate();
                        return !d.isBefore(weekStart) && !d.isAfter(weekEnd);
                    }).count();
        }
    }

    static class NutritionService {
        private final InMemoryStore store;
        NutritionService(InMemoryStore store) { this.store = store; }
        void logNutrition(User user, String item, int kcal, int p, int c, int f) {
            store.addNutrition(user.email, new NutritionEntry(item, kcal, p, c, f));
        }
        int dailyCalories(User user, LocalDate date) {
            return store.getNutrition(user.email).stream()
                    .filter(n -> n.loggedAt.toLocalDate().equals(date))
                    .mapToInt(n -> n.calories).sum();
        }
        List<NutritionEntry> listToday(User user) {
            LocalDate today = LocalDate.now();
            return store.getNutrition(user.email).stream()
                    .filter(n -> n.loggedAt.toLocalDate().equals(today))
                    .collect(Collectors.toList());
        }
    }

    static class GoalService {
        void setDailyCalories(User u, Integer kcal) { u.goal.dailyCaloriesTarget = kcal; }
        void setWeeklyWorkouts(User u, Integer count) { u.goal.weeklyWorkoutsTarget = count; }
    }

    // ======== SWING APP ========
    static class FitnessFrame extends JFrame {
        final InMemoryStore store;
        final AuthService auth;
        final WorkoutService workoutSvc;
        final NutritionService nutritionSvc;
        final GoalService goalSvc = new GoalService();

        User currentUser;

        // UI fields used across tabs
        JLabel welcomeLbl = new JLabel("Welcome!");
        JTextField emailField = new JTextField(20);
        JTextField nameField = new JTextField(16);
        JButton loginBtn = new JButton("Sign In / Create");

        // Goals tab
        JSpinner dailyCalSpinner = new JSpinner(new SpinnerNumberModel(2200, 0, 10000, 50));
        JSpinner weeklyWorkoutsSpinner = new JSpinner(new SpinnerNumberModel(3, 0, 21, 1));
        JButton saveGoalsBtn = new JButton("Save Goals");
        JLabel goalsSavedLbl = new JLabel(" ");

        // Log Workout tab
        JComboBox<String> workoutType = new JComboBox<>(new String[]{"Run", "Lift", "Cycle", "Swim", "Walk"});
        JSpinner workoutMins = new JSpinner(new SpinnerNumberModel(30, 1, 600, 1));
        JSpinner workoutKcal = new JSpinner(new SpinnerNumberModel(300, 0, 5000, 10));
        JButton saveWorkoutBtn = new JButton("Log Workout");
        JLabel workoutSavedLbl = new JLabel(" ");

        // Log Nutrition tab
        JTextField foodName = new JTextField(16);
        JSpinner foodKcal = new JSpinner(new SpinnerNumberModel(500, 0, 5000, 10));
        JSpinner foodP = new JSpinner(new SpinnerNumberModel(30, 0, 300, 1));
        JSpinner foodC = new JSpinner(new SpinnerNumberModel(50, 0, 600, 1));
        JSpinner foodF = new JSpinner(new SpinnerNumberModel(20, 0, 300, 1));
        JButton saveFoodBtn = new JButton("Log Food");
        JLabel foodSavedLbl = new JLabel(" ");

        // Progress tab
        JLabel todayCalLbl = new JLabel("Calories today: 0");
        JLabel dailyTargetLbl = new JLabel("Daily target: -");
        JLabel weekWorkoutsLbl = new JLabel("Workouts this week: 0");
        JLabel weeklyTargetLbl = new JLabel("Weekly target: -");
        JButton refreshProgressBtn = new JButton("Refresh");

        // Workouts tab
        DefaultTableModel workoutsModel = new DefaultTableModel(new Object[]{"Date", "Type", "Minutes", "Kcal"}, 0);
        JTable workoutsTable = new JTable(workoutsModel);
        JButton refreshWorkoutsBtn = new JButton("Refresh");

        FitnessFrame(InMemoryStore store) {
            super("Fitness App (Swing)");
            this.store = store;
            this.auth = new AuthService(store);
            this.workoutSvc = new WorkoutService(store);
            this.nutritionSvc = new NutritionService(store);

            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(850, 520);
            setLocationRelativeTo(null);
            setLayout(new BorderLayout());

            add(buildHeader(), BorderLayout.NORTH);
            add(buildTabs(), BorderLayout.CENTER);

            wireActions();
        }

        private JPanel buildHeader() {
            JPanel p = new JPanel(new BorderLayout());
            p.setBorder(new EmptyBorder(10, 10, 10, 10));
            // Login panel
            JPanel loginPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            loginPanel.add(new JLabel("Email:"));
            loginPanel.add(emailField);
            loginPanel.add(new JLabel("Name:"));
            loginPanel.add(nameField);
            loginPanel.add(loginBtn);
            p.add(loginPanel, BorderLayout.WEST);

            welcomeLbl.setFont(welcomeLbl.getFont().deriveFont(Font.BOLD, 14f));
            p.add(welcomeLbl, BorderLayout.EAST);
            return p;
        }

        private JTabbedPane buildTabs() {
            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Goals", buildGoalsTab());
            tabs.addTab("Log Workout", buildWorkoutTab());
            tabs.addTab("Log Nutrition", buildNutritionTab());
            tabs.addTab("Progress", buildProgressTab());
            tabs.addTab("Workouts", buildWorkoutsTab());
            return tabs;
        }

        private JPanel buildGoalsTab() {
            JPanel p = new JPanel(new GridBagLayout());
            p.setBorder(new EmptyBorder(16,16,16,16));
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(8,8,8,8);
            c.fill = GridBagConstraints.HORIZONTAL;

            int y=0;
            c.gridx=0; c.gridy=y; p.add(new JLabel("Daily Calories Target (kcal):"), c);
            c.gridx=1; c.gridy=y++; p.add(dailyCalSpinner, c);
            c.gridx=0; c.gridy=y; p.add(new JLabel("Weekly Workouts Target:"), c);
            c.gridx=1; c.gridy=y++; p.add(weeklyWorkoutsSpinner, c);
            c.gridx=0; c.gridy=y; c.gridwidth=2; p.add(saveGoalsBtn, c);
            c.gridy=y+1; p.add(goalsSavedLbl, c);
            return p;
        }

        private JPanel buildWorkoutTab() {
            JPanel p = new JPanel(new GridBagLayout());
            p.setBorder(new EmptyBorder(16,16,16,16));
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(8,8,8,8);
            c.fill = GridBagConstraints.HORIZONTAL;

            int y=0;
            c.gridx=0; c.gridy=y; p.add(new JLabel("Type:"), c);
            c.gridx=1; c.gridy=y++; p.add(workoutType, c);
            c.gridx=0; c.gridy=y; p.add(new JLabel("Duration (min):"), c);
            c.gridx=1; c.gridy=y++; p.add(workoutMins, c);
            c.gridx=0; c.gridy=y; p.add(new JLabel("Estimated Calories (kcal):"), c);
            c.gridx=1; c.gridy=y++; p.add(workoutKcal, c);
            c.gridx=0; c.gridy=y; c.gridwidth=2; p.add(saveWorkoutBtn, c);
            c.gridy=y+1; p.add(workoutSavedLbl, c);
            return p;
        }

        private JPanel buildNutritionTab() {
            JPanel p = new JPanel(new GridBagLayout());
            p.setBorder(new EmptyBorder(16,16,16,16));
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(8,8,8,8);
            c.fill = GridBagConstraints.HORIZONTAL;

            int y=0;
            c.gridx=0; c.gridy=y; p.add(new JLabel("Food Name:"), c);
            c.gridx=1; c.gridy=y++; p.add(foodName, c);
            c.gridx=0; c.gridy=y; p.add(new JLabel("Calories (kcal):"), c);
            c.gridx=1; c.gridy=y++; p.add(foodKcal, c);
            c.gridx=0; c.gridy=y; p.add(new JLabel("Protein (g):"), c);
            c.gridx=1; c.gridy=y++; p.add(foodP, c);
            c.gridx=0; c.gridy=y; p.add(new JLabel("Carbs (g):"), c);
            c.gridx=1; c.gridy=y++; p.add(foodC, c);
            c.gridx=0; c.gridy=y; p.add(new JLabel("Fats (g):"), c);
            c.gridx=1; c.gridy=y++; p.add(foodF, c);
            c.gridx=0; c.gridy=y; c.gridwidth=2; p.add(saveFoodBtn, c);
            c.gridy=y+1; p.add(foodSavedLbl, c);
            return p;
        }

        private JPanel buildProgressTab() {
            JPanel p = new JPanel(new GridBagLayout());
            p.setBorder(new EmptyBorder(16,16,16,16));
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(8,8,8,8);
            c.fill = GridBagConstraints.HORIZONTAL;

            int y=0;
            c.gridx=0; c.gridy=y; p.add(todayCalLbl, c);
            c.gridx=1; c.gridy=y++; p.add(dailyTargetLbl, c);
            c.gridx=0; c.gridy=y; p.add(weekWorkoutsLbl, c);
            c.gridx=1; c.gridy=y++; p.add(weeklyTargetLbl, c);
            c.gridx=0; c.gridy=y; c.gridwidth=2; p.add(refreshProgressBtn, c);
            return p;
        }

        private JPanel buildWorkoutsTab() {
            JPanel p = new JPanel(new BorderLayout());
            p.setBorder(new EmptyBorder(8,8,8,8));
            JScrollPane scroll = new JScrollPane(workoutsTable);
            p.add(scroll, BorderLayout.CENTER);
            JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            south.add(refreshWorkoutsBtn);
            p.add(south, BorderLayout.SOUTH);
            return p;
        }

        private void wireActions() {
            loginBtn.addActionListener(e -> {
                String email = emailField.getText().trim();
                String name = nameField.getText().trim();
                if (email.isEmpty() || name.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Enter email and name.", "Missing Info", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                currentUser = auth.signInOrSignUp(email, name);
                welcomeLbl.setText("Welcome, " + currentUser.name + "!");
                goalsSavedLbl.setText(" ");
                workoutSavedLbl.setText(" ");
                foodSavedLbl.setText(" ");
                refreshProgress();
                refreshWorkouts();
            });

            saveGoalsBtn.addActionListener(e -> {
                if (!ensureUser()) return;
                Integer daily = (Integer) dailyCalSpinner.getValue();
                Integer weekly = (Integer) weeklyWorkoutsSpinner.getValue();
                goalSvc.setDailyCalories(currentUser, daily);
                goalSvc.setWeeklyWorkouts(currentUser, weekly);
                goalsSavedLbl.setText("Saved: " + currentUser.goal);
            });

            saveWorkoutBtn.addActionListener(e -> {
                if (!ensureUser()) return;
                String type = String.valueOf(workoutType.getSelectedItem());
                int mins = (Integer) workoutMins.getValue();
                int kcal = (Integer) workoutKcal.getValue();
                workoutSvc.logWorkout(currentUser, type, mins, kcal);
                workoutSavedLbl.setText("Workout saved.");
                refreshProgress();
                refreshWorkouts();
            });

            saveFoodBtn.addActionListener(e -> {
                if (!ensureUser()) return;
                String item = foodName.getText().trim();
                if (item.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Enter a food name.", "Missing Info", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                int kcal = (Integer) foodKcal.getValue();
                int p = (Integer) foodP.getValue();
                int c = (Integer) foodC.getValue();
                int f = (Integer) foodF.getValue();
                nutritionSvc.logNutrition(currentUser, item, kcal, p, c, f);
                foodSavedLbl.setText("Food entry saved.");
                foodName.setText("");
                refreshProgress();
            });

            refreshProgressBtn.addActionListener(e -> refreshProgress());
            refreshWorkoutsBtn.addActionListener(e -> refreshWorkouts());
        }

        private boolean ensureUser() {
            if (currentUser == null) {
                JOptionPane.showMessageDialog(this, "Please sign in first.", "Not Signed In", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            return true;
        }

        private void refreshProgress() {
            if (currentUser == null) return;
            int calToday = nutritionSvc.dailyCalories(currentUser, LocalDate.now());
            todayCalLbl.setText("Calories today: " + calToday);
            if (currentUser.goal.dailyCaloriesTarget != null) {
                int remain = currentUser.goal.dailyCaloriesTarget - calToday;
                dailyTargetLbl.setText("Daily target: " + currentUser.goal.dailyCaloriesTarget + " (Remaining: " + remain + ")");
            } else {
                dailyTargetLbl.setText("Daily target: -");
            }
            long wkCount = workoutSvc.countWorkoutsInWeek(currentUser, startOfThisWeek());
            weekWorkoutsLbl.setText("Workouts this week: " + wkCount);
            if (currentUser.goal.weeklyWorkoutsTarget != null) {
                long left = Math.max(currentUser.goal.weeklyWorkoutsTarget - wkCount, 0);
                weeklyTargetLbl.setText("Weekly target: " + currentUser.goal.weeklyWorkoutsTarget + " (Left: " + left + ")");
            } else {
                weeklyTargetLbl.setText("Weekly target: -");
            }
        }

        private void refreshWorkouts() {
            if (currentUser == null) return;
            workoutsModel.setRowCount(0);
            for (Workout w : workoutSvc.listWorkouts(currentUser)) {
                workoutsModel.addRow(new Object[]{
                        w.startedAt.toLocalDate().toString(), w.type, w.durationMin, w.estimatedCalories
                });
            }
        }
    }

    // ======== UTIL ========
    private static LocalDate startOfThisWeek() {
        LocalDate today = LocalDate.now();
        // Monday as start of week (ISO-8601): Monday = 1 ... Sunday = 7
        return today.minusDays((today.getDayOfWeek().getValue() + 6) % 7);
    }

    // ======== MAIN ========
    public static void main(String[] args) {
        // Simple look & feel for nicer default UI (optional)
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}

        InMemoryStore store = new InMemoryStore();

        SwingUtilities.invokeLater(() -> {
            FitnessFrame frame = new FitnessFrame(store);
            frame.setVisible(true);
        });
    }
}
