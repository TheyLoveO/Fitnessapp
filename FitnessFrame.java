import java.util.Comparator;
import java.util.List;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class FitnessFrame extends JFrame {
    static {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignore) {}
    }

    final InMemoryStore store;
    final AuthService auth;
    final WorkoutService workoutSvc;
    final NutritionService nutritionSvc;
    final GoalService goalSvc = new GoalService();

    User currentUser;

    // Header
    JLabel welcomeLbl = new JLabel("Welcome!");
    JTextField emailField = new JTextField(20);
    JTextField nameField = new JTextField(16);
    JButton loginBtn = new RoundedButton("Sign In / Create");

    // Nutrition (tab 1) + Daily Goal
    JComboBox<Food> foodPicker = new JComboBox<>(Food.getDefaultFoods().toArray(new Food[0]));
    JSpinner gramsSpinner = new JSpinner(new SpinnerNumberModel(100, 1, 2000, 10));
    JSpinner caloriesSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 5000, 10));
    JButton addFoodBtn = new RoundedButton("Add Food");
    JLabel foodSavedLbl = new JLabel(" ");
    DefaultTableModel todayNutritionModel = new DefaultTableModel(new Object[]{"Time", "Food", "Grams", "Calories"}, 0);
    JTable todayNutritionTable = new JTable(todayNutritionModel);

    // NEW: Daily Burn Goal controls (first page)
    JSpinner burnGoalSpinner = new JSpinner(new SpinnerNumberModel(500, 0, 20000, 50));
    JButton setBurnGoalBtn = new RoundedButton("Set Burn Goal");

    // Workout entry (tab 2)
    JComboBox<String> workoutType = new JComboBox<>(new String[]{"Run", "Walk", "Cycle", "Swim", "Lift"});
    JComboBox<String> bodyPart = new JComboBox<>(new String[]{"-", "Chest", "Back", "Legs", "Shoulders", "Arms", "Core", "Full Body"});

    JSpinner startHour = new JSpinner(new SpinnerNumberModel(LocalTime.now().getHour(), 0, 23, 1));
    JSpinner startMin  = new JSpinner(new SpinnerNumberModel(LocalTime.now().getMinute(), 0, 59, 1));
    JSpinner workoutMins = new JSpinner(new SpinnerNumberModel(30, 1, 600, 1));
    JSpinner workoutKcal = new JSpinner(new SpinnerNumberModel(300, 0, 5000, 10));

    // Strength panel
    JPanel strengthPanel = new JPanel(new GridBagLayout());
    JTextField exNameField = new JTextField(18);
    JSpinner setsSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 50, 1));
    JSpinner repsSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 200, 1));

    // Cardio panel
    JPanel cardioPanel = new JPanel(new GridBagLayout());
    JComboBox<String> distanceUnit = new JComboBox<>(new String[]{"Miles", "Steps"});
    JSpinner milesSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.0, 1000.0, 0.1));
    JSpinner stepsSpinner = new JSpinner(new SpinnerNumberModel(1000, 0, 200000, 100));

    JButton saveWorkoutBtn = new RoundedButton("Log Workout");
    JLabel workoutSavedLbl = new JLabel(" ");

    // Saved Workouts (tab 3)
    DefaultListModel<LocalDate> daysListModel = new DefaultListModel<>();
    JList<LocalDate> daysList = new JList<>(daysListModel);
    DefaultTableModel savedWorkoutsModel = new DefaultTableModel(
            new Object[]{"Time", "Type", "Body Part / Detail", "Minutes", "Calories"}, 0);
    JTable savedWorkoutsTable = new JTable(savedWorkoutsModel);
    JTextArea savedWorkoutDetail = new JTextArea(8, 40);

    // Progress (tab 4)
    JLabel eatenTodayLbl = new JLabel("Eaten today: 0");
    JLabel burnedTodayLbl = new JLabel("Burned today: 0");
    JLabel burnGoalLbl = new JLabel("Burn goal: -");
    JButton refreshProgressBtn = new RoundedButton("Refresh");
    DefaultTableModel progressNutritionModel = new DefaultTableModel(new Object[]{"Time", "Food", "Grams", "Calories"}, 0);
    JTable progressNutritionTable = new JTable(progressNutritionModel);
    DefaultTableModel progressWorkoutsModel = new DefaultTableModel(
            new Object[]{"Date", "Start", "Type", "Detail", "Minutes", "Calories"}, 0);
    JTable progressWorkoutsTable = new JTable(progressWorkoutsModel);

    private JTabbedPane tabs;

    FitnessFrame(InMemoryStore store) {
        super("Fitness App (Swing)");
        this.store = store;
        this.auth = new AuthService(store);
        this.workoutSvc = new WorkoutService(store);
        this.nutritionSvc = new NutritionService(store);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1120, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.BG);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTabs(), BorderLayout.CENTER);

        wireActions();
        styleGlobals();

        updateCaloriesFromFood();
        toggleWorkoutPanels(); // initialize correct panel
    }

    private void styleGlobals() {
        welcomeLbl.setFont(welcomeLbl.getFont().deriveFont(Font.BOLD, 14f));
        welcomeLbl.setForeground(Theme.RED_DARK);

        JTableHeader hn = todayNutritionTable.getTableHeader();
        hn.setBackground(Theme.YELLOW); hn.setForeground(Theme.TEXT); hn.setFont(hn.getFont().deriveFont(Font.BOLD));
        todayNutritionTable.setRowHeight(22);

        JTableHeader pn = progressNutritionTable.getTableHeader();
        pn.setBackground(Theme.YELLOW); pn.setForeground(Theme.TEXT); pn.setFont(pn.getFont().deriveFont(Font.BOLD));
        progressNutritionTable.setRowHeight(22);

        JTableHeader hw = progressWorkoutsTable.getTableHeader();
        hw.setBackground(Theme.RED); hw.setForeground(Theme.TEXT); hw.setFont(hw.getFont().deriveFont(Font.BOLD));
        progressWorkoutsTable.setRowHeight(22);

        // Saved Workouts table: widths (header is hidden)
        savedWorkoutsTable.setRowHeight(22);
        savedWorkoutsTable.getColumnModel().getColumn(0).setPreferredWidth(70);
        savedWorkoutsTable.getColumnModel().getColumn(1).setPreferredWidth(70);
        savedWorkoutsTable.getColumnModel().getColumn(2).setPreferredWidth(240);
        savedWorkoutsTable.getColumnModel().getColumn(3).setPreferredWidth(70);
        savedWorkoutsTable.getColumnModel().getColumn(4).setPreferredWidth(80);

        daysList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel l = new JLabel(value.toString());
            l.setOpaque(true);
            l.setBorder(new EmptyBorder(4,8,4,8));
            l.setBackground(isSelected ? new Color(255,236,179) : Color.WHITE);
            l.setForeground(Theme.TEXT);
            return l;
        });
        savedWorkoutDetail.setEditable(false);
        savedWorkoutDetail.setLineWrap(true);
        savedWorkoutDetail.setWrapStyleWord(true);
    
        installEnhancedLook();
}

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        p.setBackground(Theme.CARD);

        JPanel loginPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        loginPanel.setOpaque(false);
        loginPanel.add(new JLabel("Email:"));
        loginPanel.add(emailField);
        loginPanel.add(new JLabel("Name:"));
        loginPanel.add(nameField);
        loginPanel.add(loginBtn);

        p.add(loginPanel, BorderLayout.WEST);
        p.add(welcomeLbl, BorderLayout.EAST);
        return p;
    }

    private JTabbedPane buildTabs() {
        tabs = new JTabbedPane();
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.setBackground(Theme.CARD);
        tabs.setBorder(new EmptyBorder(6, 6, 6, 6));

        tabs.addTab("Nutrition", buildNutritionTab());
        tabs.addTab("Workouts", buildWorkoutTab());
        tabs.addTab("Saved Workouts", buildSavedWorkoutsTab());
        tabs.addTab("Progress", buildProgressTab());

        return tabs;
    }

    private JPanel buildNutritionTab() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.CARD);
        p.setBorder(new EmptyBorder(12,12,12,12));

        // --- Daily Goal panel (first page) ---
        JPanel goalPanel = new JPanel(new GridBagLayout());
        goalPanel.setOpaque(true);
        goalPanel.setBackground(Theme.CARD);
        TitledBorder gb = new TitledBorder("Daily Goal");
        gb.setTitleColor(Theme.TEXT);
        goalPanel.setBorder(gb);

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6,6,6,6);
        g.fill = GridBagConstraints.HORIZONTAL;
        int gy = 0;
        g.gridx = 0; g.gridy = gy; goalPanel.add(new JLabel("Calories to burn today (kcal):"), g);
        g.gridx = 1; g.gridy = gy++; goalPanel.add(burnGoalSpinner, g);
        g.gridx = 0; g.gridy = gy; g.gridwidth = 2; goalPanel.add(setBurnGoalBtn, g);

        // --- Food form ---
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.fill = GridBagConstraints.HORIZONTAL;

        int y=0;
        c.gridx=0; c.gridy=y; form.add(new JLabel("Food:"), c);
        c.gridx=1; c.gridy=y++; form.add(foodPicker, c);

        c.gridx=0; c.gridy=y; form.add(new JLabel("Grams:"), c);
        c.gridx=1; c.gridy=y++; form.add(gramsSpinner, c);

        c.gridx=0; c.gridy=y; form.add(new JLabel("Calories (auto):"), c);
        c.gridx=1; c.gridy=y++; form.add(caloriesSpinner, c);

        c.gridx=0; c.gridy=y; c.gridwidth=2; form.add(addFoodBtn, c);
        c.gridy=y+1; form.add(colorizeInfo(foodSavedLbl), c);

        p.add(goalPanel, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.setBorder(new TitledBorder("Today's Nutrition"));
        center.add(new JScrollPane(todayNutritionTable), BorderLayout.CENTER);
        p.add(center, BorderLayout.CENTER);

        JPanel below = new JPanel(new BorderLayout());
        below.setOpaque(false);
        below.add(form, BorderLayout.NORTH);
        p.add(below, BorderLayout.SOUTH);

        return p;
    }

    private JPanel buildWorkoutTab() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.CARD);
        p.setBorder(new EmptyBorder(12,12,12,12));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.fill = GridBagConstraints.HORIZONTAL;

        int y=0;
        c.gridx=0; c.gridy=y; form.add(new JLabel("Type:"), c);
        c.gridx=1; c.gridy=y++; form.add(workoutType, c);

        c.gridx=0; c.gridy=y; form.add(new JLabel("Body Part (if Lift):"), c);
        c.gridx=1; c.gridy=y++; form.add(bodyPart, c);

        c.gridx=0; c.gridy=y; form.add(new JLabel("Start (HH:MM):"), c);
        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        timePanel.setOpaque(false);
        timePanel.add(startHour);
        timePanel.add(new JLabel(":"));
        timePanel.add(startMin);
        c.gridx=1; c.gridy=y++; form.add(timePanel, c);

        c.gridx=0; c.gridy=y; form.add(new JLabel("Duration (min):"), c);
        c.gridx=1; c.gridy=y++; form.add(workoutMins, c);

        c.gridx=0; c.gridy=y; form.add(new JLabel("Estimated Calories (kcal):"), c);
        c.gridx=1; c.gridy=y++; form.add(workoutKcal, c);

        // Strength Qs
        strengthPanel.setOpaque(false);
        GridBagConstraints s = new GridBagConstraints();
        s.insets = new Insets(6,6,6,6);
        s.fill = GridBagConstraints.HORIZONTAL;
        int sy=0;
        s.gridx=0; s.gridy=sy; strengthPanel.add(new JLabel("What exercise?"), s);
        s.gridx=1; s.gridy=sy++; strengthPanel.add(exNameField, s);
        s.gridx=0; s.gridy=sy; strengthPanel.add(new JLabel("How many sets?"), s);
        s.gridx=1; s.gridy=sy++; strengthPanel.add(setsSpinner, s);
        s.gridx=0; s.gridy=sy; strengthPanel.add(new JLabel("How many reps?"), s);
        s.gridx=1; s.gridy=sy++; strengthPanel.add(repsSpinner, s);

        // Cardio Qs
        cardioPanel.setOpaque(false);
        GridBagConstraints k = new GridBagConstraints();
        k.insets = new Insets(6,6,6,6);
        k.fill = GridBagConstraints.HORIZONTAL;
        int ky=0;
        k.gridx=0; k.gridy=ky; cardioPanel.add(new JLabel("Distance unit:"), k);
        k.gridx=1; k.gridy=ky++; cardioPanel.add(distanceUnit, k);

        k.gridx=0; k.gridy=ky; cardioPanel.add(new JLabel("How many miles/steps?"), k);
        JPanel distPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        distPanel.setOpaque(false);
        distPanel.add(milesSpinner);
        distPanel.add(stepsSpinner);
        k.gridx=1; k.gridy=ky++; cardioPanel.add(distPanel, k);

        // Add them (we'll toggle visibility)
        c.gridx=0; c.gridy=y; c.gridwidth=2; form.add(strengthPanel, c);
        c.gridy=y+1; form.add(cardioPanel, c);

        c.gridy=y+2; form.add(saveWorkoutBtn, c);
        c.gridy=y+3; form.add(colorizeInfo(workoutSavedLbl), c);

        p.add(form, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildSavedWorkoutsTab() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.CARD);
        p.setBorder(new EmptyBorder(12,12,12,12));

        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(true);
        left.setBackground(Theme.CARD);
        TitledBorder leftBorder = new TitledBorder("Days");
        leftBorder.setTitleColor(Theme.TEXT);
        left.setBorder(leftBorder);
        JScrollPane daysScroll = new JScrollPane(daysList);
        daysScroll.getViewport().setBackground(Color.WHITE);
        left.add(daysScroll, BorderLayout.CENTER);

        JPanel right = new JPanel();
        right.setOpaque(true);
        right.setBackground(Theme.CARD);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setOpaque(true);
        tablePanel.setBackground(Theme.CARD);
        TitledBorder tableBorder = new TitledBorder("Workouts");
        tableBorder.setTitleColor(Theme.TEXT);
        tablePanel.setBorder(tableBorder);

        // ---- ALWAYS-VISIBLE LEGEND ----
        JPanel legend = new JPanel(new GridLayout(1,5));
        legend.setBorder(new EmptyBorder(4,8,4,8));
        legend.setBackground(new Color(245,245,245));
        legend.add(makeLegendLabel("Time"));
        legend.add(makeLegendLabel("Type"));
        legend.add(makeLegendLabel("Body Part / Detail"));
        legend.add(makeLegendLabel("Minutes"));
        legend.add(makeLegendLabel("Calories"));
        tablePanel.add(legend, BorderLayout.NORTH);

        JScrollPane tableScroll = new JScrollPane(savedWorkoutsTable);
        tableScroll.getViewport().setBackground(Color.WHITE);

        // ---- HIDE TABLE HEADER (keep legend only) ----
        savedWorkoutsTable.setTableHeader(null);
        tableScroll.setColumnHeaderView(null);

        tablePanel.add(tableScroll, BorderLayout.CENTER);

        JPanel detailPanel = new JPanel(new BorderLayout());
        detailPanel.setOpaque(true);
        detailPanel.setBackground(Theme.CARD);
        TitledBorder detailBorder = new TitledBorder("Selected Workout Details");
        detailBorder.setTitleColor(Theme.TEXT);
        detailPanel.setBorder(detailBorder);

        JScrollPane detailScroll = new JScrollPane(savedWorkoutDetail);
        detailScroll.getViewport().setBackground(Color.WHITE);
        detailPanel.add(detailScroll, BorderLayout.CENTER);

        right.add(tablePanel);
        right.add(Box.createVerticalStrut(8));
        right.add(detailPanel);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.25);
        p.add(split, BorderLayout.CENTER);

        daysList.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) populateSavedWorkoutsForDay(daysList.getSelectedValue()); });
        savedWorkoutsTable.getSelectionModel().addListSelectionListener(e -> {
            int row = savedWorkoutsTable.getSelectedRow();
            if (row >= 0) showSavedWorkoutDetailForRow(row);
        });

        return p;
    }

    private JLabel makeLegendLabel(String text){
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        l.setForeground(Theme.TEXT);
        return l;
    }

    private JPanel buildProgressTab() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.CARD);
        p.setBorder(new EmptyBorder(12,12,12,12));

        JPanel top = new JPanel(new GridBagLayout());
        top.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.fill = GridBagConstraints.HORIZONTAL;

        eatenTodayLbl.setForeground(Theme.TEXT);
        burnedTodayLbl.setForeground(Theme.TEXT);
        burnGoalLbl.setForeground(Theme.SUBTLE);

        int y=0;
        c.gridx=0; c.gridy=y; top.add(eatenTodayLbl, c);
        c.gridx=1; c.gridy=y++; top.add(burnedTodayLbl, c);
        c.gridx=0; c.gridy=y; c.gridwidth=2; top.add(burnGoalLbl, c);
        c.gridx=0; c.gridy=y+1; c.gridwidth=2; top.add(refreshProgressBtn, c);

        p.add(top, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JPanel nutPanel = new JPanel(new BorderLayout());
        nutPanel.setOpaque(true);
        nutPanel.setBackground(Theme.CARD);
        TitledBorder nutBorder = new TitledBorder("Today's Nutrition");
        nutBorder.setTitleColor(Theme.TEXT);
        nutPanel.setBorder(nutBorder);
        JScrollPane progNutScroll = new JScrollPane(progressNutritionTable);
        progNutScroll.getViewport().setBackground(Color.WHITE);
        nutPanel.add(progNutScroll, BorderLayout.CENTER);

        JPanel wkPanel = new JPanel(new BorderLayout());
        wkPanel.setOpaque(true);
        wkPanel.setBackground(Theme.CARD);
        TitledBorder wkBorder = new TitledBorder("Today's Workouts");
        wkBorder.setTitleColor(Theme.TEXT);
        wkPanel.setBorder(wkBorder);
        JScrollPane progWkScroll = new JScrollPane(progressWorkoutsTable);
        progWkScroll.getViewport().setBackground(Color.WHITE);
        wkPanel.add(progWkScroll, BorderLayout.CENTER);

        center.add(nutPanel);
        center.add(Box.createVerticalStrut(10));
        center.add(wkPanel);
        p.add(center, BorderLayout.CENTER);

        return p;
    }

    private JLabel colorizeInfo(JLabel lbl) { lbl.setForeground(Theme.RED_DARK); return lbl; }

    // ======== WIRES ========
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
            refreshAllProgress();
            refreshDaysList();
        });

        setBurnGoalBtn.addActionListener(e -> {
            if (!ensureUser()) return;
            int goal = (Integer) burnGoalSpinner.getValue();
            goalSvc.setDailyBurn(currentUser, goal);
            JOptionPane.showMessageDialog(this, "Daily burn goal set to " + goal + " kcal.", "Goal Updated", JOptionPane.INFORMATION_MESSAGE);
            refreshAllProgress();
        });

        gramsSpinner.addChangeListener(e -> updateCaloriesFromFood());
        foodPicker.addItemListener(e -> { if (e.getStateChange() == ItemEvent.SELECTED) updateCaloriesFromFood(); });

        addFoodBtn.addActionListener(e -> {
            if (!ensureUser()) return;
            Food f = (Food) foodPicker.getSelectedItem();
            int grams = (Integer) gramsSpinner.getValue();
            int kcal = (Integer) caloriesSpinner.getValue();
            if (f == null) return;
            nutritionSvc.logNutrition(currentUser, f.name, grams, kcal);
            foodSavedLbl.setText("Added: " + f.name + " (" + grams + "g, " + kcal + " kcal)");
            todayNutritionModel.addRow(new Object[]{ LocalTime.now().withSecond(0).withNano(0).toString(), f.name, grams, kcal });
            refreshAllProgress();
        });

        workoutType.addItemListener(e -> { if (e.getStateChange() == ItemEvent.SELECTED) toggleWorkoutPanels(); });
        distanceUnit.addItemListener(e -> toggleDistanceEditors());

        saveWorkoutBtn.addActionListener(e -> {
            if (!ensureUser()) return;

            String type = String.valueOf(workoutType.getSelectedItem());
            int mins = (Integer) workoutMins.getValue();
            int kcal = (Integer) workoutKcal.getValue();
            int h = (Integer) startHour.getValue();
            int m = (Integer) startMin.getValue();
            LocalDateTime start = LocalDateTime.of(LocalDate.now(), LocalTime.of(h, m));

            Workout w;
            if (isCardio(type)) {
                String unit = String.valueOf(distanceUnit.getSelectedItem());
                Double miles = unit.equals("Miles") ? ((Number) milesSpinner.getValue()).doubleValue() : null;
                Integer steps = unit.equals("Steps") ? ((Number) stepsSpinner.getValue()).intValue() : null;
                w = Workout.cardio(type, start, unit, miles, steps, mins, kcal);
            } else {
                String part = String.valueOf(bodyPart.getSelectedItem());
                String ex = exNameField.getText().trim();
                int sets = (Integer) setsSpinner.getValue();
                int reps = (Integer) repsSpinner.getValue();
                if (ex.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please enter an exercise name.", "Missing Info", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                w = Workout.strength(type, part.equals("-") ? "" : part, start, ex, sets, reps, mins, kcal);
            }

            workoutSvc.logWorkout(currentUser, w);
            workoutSavedLbl.setText("Workout saved.");
            exNameField.setText("");
            refreshAllProgress();
            refreshDaysList();
            selectDay(LocalDate.now());
        });

        refreshProgressBtn.addActionListener(e -> {
            refreshAllProgress();
            refreshDaysList();
        });
    }

    private void toggleWorkoutPanels() {
        String type = String.valueOf(workoutType.getSelectedItem());
        boolean cardio = isCardio(type);
        strengthPanel.setVisible(!cardio);
        bodyPart.setEnabled(!cardio);
        cardioPanel.setVisible(cardio);
        toggleDistanceEditors();
        strengthPanel.revalidate(); strengthPanel.repaint();
        cardioPanel.revalidate(); cardioPanel.repaint();
    }

    private void toggleDistanceEditors() {
        String unit = String.valueOf(distanceUnit.getSelectedItem());
        boolean miles = unit.equals("Miles");
        milesSpinner.setVisible(miles);
        stepsSpinner.setVisible(!miles);
        cardioPanel.revalidate(); cardioPanel.repaint();
    }

    private boolean isCardio(String type) {
        return type.equals("Run") || type.equals("Walk") || type.equals("Cycle") || type.equals("Swim");
    }

    // ======== REFRESH ========
    private void refreshAllProgress() {
        if (currentUser == null) return;

        int eatenToday = nutritionSvc.dailyCalories(currentUser, LocalDate.now());
        eatenTodayLbl.setText("Eaten today: " + eatenToday + " kcal");

        int burnedToday = workoutSvc.listWorkouts(currentUser).stream()
                .filter(w -> w.startedAt.toLocalDate().equals(LocalDate.now()))
                .mapToInt(w -> w.estimatedCalories)
                .sum();
        burnedTodayLbl.setText("Burned today: " + burnedToday + " kcal");

        if (currentUser.goal.dailyBurnTarget != null) {
            int remain = Math.max(currentUser.goal.dailyBurnTarget - burnedToday, 0);
            burnGoalLbl.setText("Burn goal: " + currentUser.goal.dailyBurnTarget + " kcal (Remaining: " + remain + ")");
        } else {
            burnGoalLbl.setText("Burn goal: -");
        }

        // progress nutrition table
        progressNutritionModel.setRowCount(0);
        for (NutritionEntry n : nutritionSvc.listToday(currentUser)) {
            progressNutritionModel.addRow(new Object[]{
                    n.loggedAt.toLocalTime().withSecond(0).withNano(0).toString(), n.itemName, n.grams, n.calories
            });
        }

        // today's workouts table
        progressWorkoutsModel.setRowCount(0);
        for (Workout w : workoutSvc.listWorkouts(currentUser).stream()
                .filter(w -> w.startedAt.toLocalDate().equals(LocalDate.now())).toList()) {
            progressWorkoutsModel.addRow(new Object[]{
                    w.startedAt.toLocalDate().toString(),
                    w.startedAt.toLocalTime().withNano(0).toString(),
                    w.type,
                    summarizeDetail(w),
                    w.durationMin, w.estimatedCalories
            });
        }
    }

    private void refreshDaysList() {
        if (currentUser == null) return;
        daysListModel.clear();
        List<LocalDate> dates = workoutSvc.listWorkouts(currentUser).stream()
                .map(w -> w.startedAt.toLocalDate())
                .distinct().sorted(Comparator.reverseOrder()).toList();
        for (LocalDate d : dates) daysListModel.addElement(d);
        if (!dates.isEmpty() && daysList.getSelectedValue() == null) daysList.setSelectedIndex(0);
    }

    private void selectDay(LocalDate date) {
        for (int i = 0; i < daysListModel.size(); i++) if (daysListModel.get(i).equals(date)) { daysList.setSelectedIndex(i); return; }
    }

    private void populateSavedWorkoutsForDay(LocalDate d) {
        savedWorkoutsModel.setRowCount(0);
        savedWorkoutDetail.setText("");
        if (currentUser == null || d == null) return;
        List<Workout> list = workoutSvc.listWorkouts(currentUser).stream()
                .filter(w -> w.startedAt.toLocalDate().equals(d))
                .sorted(Comparator.comparing(w -> w.startedAt))
                .toList();
        for (Workout w : list) {
            savedWorkoutsModel.addRow(new Object[]{
                    w.startedAt.toLocalTime().withNano(0).toString(),
                    w.type,
                    summarizeDetail(w),
                    w.durationMin, w.estimatedCalories
            });
        }
    }

    private void showSavedWorkoutDetailForRow(int row) {
        if (currentUser == null) return;
        LocalDate day = daysList.getSelectedValue();
        if (day == null) return;
        String at = String.valueOf(savedWorkoutsModel.getValueAt(row, 0));
        List<Workout> list = workoutSvc.listWorkouts(currentUser).stream()
                .filter(w -> w.startedAt.toLocalDate().equals(day)
                        && w.startedAt.toLocalTime().withNano(0).toString().equals(at))
                .sorted(Comparator.comparing(w -> w.startedAt)).toList();
        if (list.isEmpty()) { savedWorkoutDetail.setText(""); return; }
        Workout w = list.get(0);
        StringBuilder sb = new StringBuilder();
        sb.append("Start: ").append(w.startedAt.toLocalTime().withNano(0))
                .append("\nType: ").append(w.type)
                .append("\nMinutes: ").append(w.durationMin)
                .append("\nCalories: ").append(w.estimatedCalories);
        if (isCardio(w.type)) {
            sb.append("\nDistance: ").append(w.distanceUnit.equals("Miles") ? (w.distanceValue + " miles")
                    : (w.steps + " steps"));
        } else {
            sb.append("\nBody Part: ").append((w.bodyPart == null || w.bodyPart.isEmpty()) ? "-" : w.bodyPart)
                    .append("\nExercise: ").append(w.exerciseName)
                    .append("\nSets x Reps: ").append(w.sets).append(" x ").append(w.reps);
        }
        savedWorkoutDetail.setText(sb.toString());
        savedWorkoutDetail.setCaretPosition(0);
    }

    private String summarizeDetail(Workout w) {
        if (isCardio(w.type)) {
            return w.distanceUnit.equals("Miles")
                    ? (String.format("%.2f miles", w.distanceValue == null ? 0.0 : w.distanceValue))
                    : (w.steps == null ? "0 steps" : (w.steps + " steps"));
        }
        String bp = (w.bodyPart == null || w.bodyPart.isEmpty()) ? "-" : w.bodyPart;
        return bp + " — " + (w.exerciseName == null ? "-" : w.exerciseName) + " (" + w.sets + "x" + w.reps + ")";
    }

    // ======== HELPERS ========
    private void updateCaloriesFromFood() {
        Food f = (Food) foodPicker.getSelectedItem();
        int grams = (Integer) gramsSpinner.getValue();
        if (f != null) {
            int kcal = Math.round(f.kcalPer100g * grams / 100f);
            caloriesSpinner.setValue(kcal);
        }
    }

    private boolean ensureUser() {
        if (currentUser == null) {
            JOptionPane.showMessageDialog(this, "Please sign in first.", "Not Signed In", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }


    /** Apply extra modern visuals without changing behavior. */
    private void installEnhancedLook() {
        // Soften overall background
        getContentPane().setBackground(new Color(248,250,252));

        // Buttons: unify font, padding, rounded border, hover cursor
        for (Component c : getContentPane().getComponents()) {
            applyRecursive(c);
        }

        // Zebra striping for tables
        installZebra(todayNutritionTable);
        installZebra(progressNutritionTable);
        installZebra(savedWorkoutsTable);

        // Slightly larger tab font if present
        for (Window w : Window.getWindows()) {
            for (Component c : w.getComponents()) {
                if (c instanceof JTabbedPane tabs) {
                    tabs.setFont(tabs.getFont().deriveFont(Font.BOLD, tabs.getFont().getSize2D() + 1f));
                    tabs.setBorder(new EmptyBorder(6,6,0,6));
                }
            }
        }
    }

    private void applyRecursive(Component c) {
        if (c instanceof AbstractButton b) {
            b.setFont(b.getFont().deriveFont(Font.BOLD));
            b.setFocusPainted(false);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            if (!(b.getBorder() instanceof RoundedBorder)) {
                b.setBorder(new RoundedBorder(14));
            }
            if (b.getBackground() == null || b.getBackground() instanceof javax.swing.plaf.UIResource) {
                b.setBackground(new Color(59,130,246)); // blue-500
                b.setForeground(Color.WHITE);
            }
            b.setOpaque(true);
            b.setRolloverEnabled(true);
            b.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                    b.setBackground(new Color(37,99,235)); // darker on hover
                }
                @Override public void mouseExited(java.awt.event.MouseEvent e) {
                    b.setBackground(new Color(59,130,246));
                }
            });
        } else if (c instanceof JPanel p) {
            if (!p.isOpaque()) p.setOpaque(true);
            if (p.getBackground() == null || p.getBackground() instanceof javax.swing.plaf.UIResource) {
                p.setBackground(new Color(255,255,255));
            }
            if (p.getBorder() == null) {
                p.setBorder(new EmptyBorder(6,6,6,6));
            }
            for (Component child : p.getComponents()) applyRecursive(child);
        } else if (c instanceof JScrollPane sp) {
            sp.setBorder(new LineBorder(new Color(229,231,235)));
            if (sp.getViewport() != null && sp.getViewport().getView() != null) {
                applyRecursive(sp.getViewport().getView());
            }
        } else if (c instanceof JComponent jc) {
            if (jc.getBorder() == null) {
                jc.setBorder(new EmptyBorder(4,6,4,6));
            }
        }
    }

    private void installZebra(JTable table) {
        if (table == null) return;
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setRowHeight(Math.max(24, table.getRowHeight()));
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component comp = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    comp.setBackground((row % 2 == 0) ? new Color(249,250,251) : Color.WHITE);
                }
                if (comp instanceof JComponent jc) {
                    jc.setBorder(new EmptyBorder(6,8,6,8));
                }
                return comp;
            }
        });
        JTableHeader header = table.getTableHeader();
        if (header != null) {
            header.setOpaque(true);
            header.setBackground(new Color(230, 243, 255));
            header.setForeground(new Color(17, 24, 39));
            header.setFont(header.getFont().deriveFont(Font.BOLD));
        }
    }

    /** Simple rounded border that keeps button shapes smooth. */
    static class RoundedBorder implements Border {
        private final int radius;
        RoundedBorder(int radius) { this.radius = radius; }
        @Override public Insets getBorderInsets(Component c) { return new Insets(radius/2, radius, radius/2, radius); }
        @Override public boolean isBorderOpaque() { return false; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(229,231,235));
            g2.drawRoundRect(x, y, width-1, height-1, radius, radius);
            g2.dispose();
        }
    }

}

