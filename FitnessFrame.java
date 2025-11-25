import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

/**
 * This class is responsible for the user interface of the application. It also contains most of the logic related to user actions.
 */
public class FitnessFrame extends JFrame {
    static {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    //setting the look and feel to Nimbus: @https://docs.oracle.com/javase/tutorial/uiswing/lookandfeel/nimbus.html
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


    // Daily Burn Goal controls (first page)
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

    /**
     * Creates an instance of FitnessFrame, calling all relevant functions for UI and actions
     * @param store the memory storage object
     */
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
        toggleWorkoutPanels();
    }

    /**
     * A GUI function - this prepares many visual aspects of the app
     */
    private void styleGlobals() {
        welcomeLbl.setFont(welcomeLbl.getFont().deriveFont(Font.BOLD, 14f));
        welcomeLbl.setForeground(Theme.TEXT);

        JTableHeader hn = todayNutritionTable.getTableHeader();//fixed color
        hn.setBackground(Theme.YELLOW); hn.setForeground(Theme.BG); hn.setFont(hn.getFont().deriveFont(Font.BOLD));
        todayNutritionTable.setRowHeight(22);

        JTableHeader pn = progressNutritionTable.getTableHeader();
        pn.setBackground(Theme.YELLOW); pn.setForeground(Theme.BG); pn.setFont(pn.getFont().deriveFont(Font.BOLD));
        progressNutritionTable.setRowHeight(22);

        JTableHeader hw = progressWorkoutsTable.getTableHeader();
        hw.setBackground(Theme.RED); hw.setForeground(Theme.BG); hw.setFont(hw.getFont().deriveFont(Font.BOLD));
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

        //installEnhancedLook();
    }

    /**
     * Instantiates the JPanel object that covers the top of the GUI - the header for inputting login information.
     */
    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        p.setBackground(Theme.CARD);

        JPanel loginPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        loginPanel.setOpaque(true);
        loginPanel.setBackground(Theme.CARD);

        JLabel emailLbl = new JLabel("Email:");
        emailLbl.setForeground(Theme.TEXT);
        emailLbl.setOpaque(false);
        JLabel nameLbl = new JLabel("Name:");
        nameLbl.setForeground(Theme.TEXT);
        nameLbl.setOpaque(false);

        emailField.setOpaque(true);
        emailField.setBackground(Theme.CARD);
        emailField.setForeground(Theme.TEXT);
        emailField.setCaretColor(Theme.TEXT);
        emailField.setBorder(new javax.swing.border.LineBorder(new Color(90,95,105)));

        nameField.setOpaque(true);
        nameField.setBackground(Theme.CARD);
        nameField.setForeground(Theme.TEXT);
        nameField.setCaretColor(Theme.TEXT);
        nameField.setBorder(new javax.swing.border.LineBorder(new Color(90,95,105)));

        loginPanel.add(emailLbl);
        loginPanel.add(emailField);
        loginPanel.add(nameLbl);
        loginPanel.add(nameField);
        loginPanel.add(loginBtn);

        p.add(welcomeLbl, BorderLayout.WEST);
        p.add(loginPanel, BorderLayout.EAST);
        return p;
    }

    /**
     * Instantiates the JTabbedPane object that contains each tab in the application.
     */
    private JTabbedPane buildTabs() {
        tabs = new JTabbedPane();
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.setBackground(Color.WHITE);
        tabs.setBorder(new EmptyBorder(6, 6, 6, 6));

        tabs.addTab("Nutrition", buildNutritionTab());
        tabs.addTab("Workouts", buildWorkoutTab());
        tabs.addTab("Saved Workouts", buildSavedWorkoutsTab());
        tabs.addTab("Progress", buildProgressTab());

        return tabs;
    }

    /**
     * Creates the nutrition tab's JPanel object.
     */
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
        JLabel burn = new JLabel("Calories to burn today (kcal):");//Changed to make text visible
        burn.setForeground(Theme.TEXT);
        g.gridx = 0; g.gridy = gy; goalPanel.add(burn, g);
        g.gridx = 1; g.gridy = gy++; goalPanel.add(burnGoalSpinner, g);
        g.gridx = 0; g.gridy = gy; g.gridwidth = 2; goalPanel.add(setBurnGoalBtn, g);

        // --- Food form ---
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.fill = GridBagConstraints.HORIZONTAL;

        int y=0;
        JLabel foodLabel1 = new JLabel("Food:");//these three changed to increase readability
        foodLabel1.setForeground(Theme.TEXT);
        c.gridx=0; c.gridy=y; form.add(foodLabel1, c);
        c.gridx=1; c.gridy=y++; form.add(foodPicker, c);

        JLabel gramsLabel1 = new JLabel("Grams:");
        gramsLabel1.setForeground(Theme.TEXT);
        c.gridx=0; c.gridy=y; form.add(gramsLabel1, c);
        c.gridx=1; c.gridy=y++; form.add(gramsSpinner, c);

        JLabel caloriesAuto = new JLabel("Calories (auto):");
        caloriesAuto.setForeground((Theme.TEXT));
        c.gridx=0; c.gridy=y; form.add(caloriesAuto, c);
        c.gridx=1; c.gridy=y++; form.add(caloriesSpinner, c);

        c.gridx=0; c.gridy=y; c.gridwidth=2; form.add(addFoodBtn, c);
        c.gridy=y+1; form.add(colorizeInfo(foodSavedLbl), c);

        p.add(goalPanel, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        TitledBorder todaysNutrition = new TitledBorder("Today's Nutrition");//changed to make text visible
        todaysNutrition.setTitleColor(Theme.TEXT);
        center.setBorder(todaysNutrition);
        center.add(new JScrollPane(todayNutritionTable), BorderLayout.CENTER);
        p.add(center, BorderLayout.CENTER);

        JPanel below = new JPanel(new BorderLayout());
        below.setOpaque(false);
        below.add(form, BorderLayout.NORTH);
        p.add(below, BorderLayout.SOUTH);

        return p;
    }

    /**
     * Creates the workout tab's JPanel object.
     */
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
        JLabel type1 = new JLabel("Type:");
        type1.setForeground(Theme.TEXT);
        c.gridx=0; c.gridy=y; form.add(type1, c);
        c.gridx=1; c.gridy=y++; form.add(workoutType, c);

        JLabel bpifLift = new JLabel("Body Part (if Lift):");
        bpifLift.setForeground(Theme.TEXT);
        c.gridx=0; c.gridy=y; form.add(bpifLift, c);
        c.gridx=1; c.gridy=y++; form.add(bodyPart, c);

        JLabel startTime = new JLabel("Star (HH:MM):");
        startTime.setForeground(Theme.TEXT);
        c.gridx=0; c.gridy=y; form.add(startTime, c);
        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        timePanel.setOpaque(false);
        timePanel.add(startHour);
        timePanel.add(new JLabel(":"));
        timePanel.add(startMin);
        c.gridx=1; c.gridy=y++; form.add(timePanel, c);

        JLabel durationMin = new JLabel("Duration (min):");
        durationMin.setForeground(Theme.TEXT);
        c.gridx=0; c.gridy=y; form.add(durationMin, c);
        c.gridx=1; c.gridy=y++; form.add(workoutMins, c);

        JLabel estCal = new JLabel("Estimated Calories (kcal):");
        estCal.setForeground(Theme.TEXT);
        c.gridx=0; c.gridy=y; form.add(estCal, c);
        c.gridx=1; c.gridy=y++; form.add(workoutKcal, c);

        // Strength Qs
        strengthPanel.setOpaque(false);
        GridBagConstraints s = new GridBagConstraints();
        s.insets = new Insets(6,6,6,6);
        s.fill = GridBagConstraints.HORIZONTAL;
        int sy=0;
        JLabel whatEx = new JLabel("What exercise?");//fixed color
        whatEx.setForeground(Theme.TEXT);
        JLabel howManys = new JLabel("How many sets?");
        howManys.setForeground(Theme.TEXT);
        JLabel howManyr = new JLabel("How many reps?");
        howManyr.setForeground(Theme.TEXT);
        s.gridx=0; s.gridy=sy; strengthPanel.add(whatEx, s);
        s.gridx=1; s.gridy=sy++; strengthPanel.add(exNameField, s);
        s.gridx=0; s.gridy=sy; strengthPanel.add(howManys, s);
        s.gridx=1; s.gridy=sy++; strengthPanel.add(setsSpinner, s);
        s.gridx=0; s.gridy=sy; strengthPanel.add(howManyr, s);
        s.gridx=1; s.gridy=sy++; strengthPanel.add(repsSpinner, s);

        // Cardio Qs
        cardioPanel.setOpaque(false);
        GridBagConstraints k = new GridBagConstraints();
        k.insets = new Insets(6,6,6,6);
        k.fill = GridBagConstraints.HORIZONTAL;
        int ky=0;
        JLabel distUnits = new JLabel("Distance unit:");//fixed color
        distUnits.setForeground(Theme.TEXT);
        k.gridx=0; k.gridy=ky; cardioPanel.add(distUnits, k);
        k.gridx=1; k.gridy=ky++; cardioPanel.add(distanceUnit, k);

        JLabel howManym = new JLabel("How many miles/steps?");
        howManym.setForeground(Theme.TEXT);
        k.gridx=0; k.gridy=ky; cardioPanel.add(howManym, k);
        JPanel distPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        distPanel.setOpaque(false);
        distPanel.add(milesSpinner);
        distPanel.add(stepsSpinner);
        k.gridx=1; k.gridy=ky++; cardioPanel.add(distPanel, k);

        c.gridx=0; c.gridy=y; c.gridwidth=2; form.add(strengthPanel, c);
        c.gridy=y+1; form.add(cardioPanel, c);

        c.gridy=y+2; form.add(saveWorkoutBtn, c);
        c.gridy=y+3; form.add(colorizeInfo(workoutSavedLbl), c);

        p.add(form, BorderLayout.CENTER);
        return p;
    }

    /**
     * Creates the saved workout tab's JPanel object.
     */
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

    /**
     * Creates a legend label - this is used primarily in the saved workouts tab
     */
    private JLabel makeLegendLabel(String text){
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        l.setForeground(Theme.BG);
        return l;
    }

    /**
     * Creates the progress tab's JPanel object.
     */
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

    /**
     * Sets the foreground color to red dark for a given JLabel.
     * @param lbl the JLabel object
     */
    private JLabel colorizeInfo(JLabel lbl) {
        lbl.setForeground(Theme.RED_DARK);
        return lbl;
    }

    /**
     * This function creates various action listeners for the objects in the FitnessFrame class. Some of the basic logic for how to update
     * the application when these actions are called is also included within the function.
     */
    private void wireActions() {
        //when the user inputs login information
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

        //when the user inputs burn goal information
        setBurnGoalBtn.addActionListener(e -> {
            if (!ensureUser()) return;
            int goal = (Integer) burnGoalSpinner.getValue();
            goalSvc.setDailyBurn(currentUser, goal);
            JOptionPane.showMessageDialog(this, "Daily burn goal set to " + goal + " kcal.", "Goal Updated", JOptionPane.INFORMATION_MESSAGE);
            refreshAllProgress();
        });

        gramsSpinner.addChangeListener(e -> updateCaloriesFromFood());
        foodPicker.addItemListener(e -> { if (e.getStateChange() == ItemEvent.SELECTED) updateCaloriesFromFood(); });

        //when the user inputs consumed food information
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

        //when the user saves a workout
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

        //when the user presses the refresh progress button
        refreshProgressBtn.addActionListener(e -> {
            refreshAllProgress();
            refreshDaysList();
        });
    }

    /**
     * Basic GUI setters for visibilty.
     */
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

    /**
     * Basic GUI setters for visibilty.
     */
    private void toggleDistanceEditors() {
        String unit = String.valueOf(distanceUnit.getSelectedItem());
        boolean miles = unit.equals("Miles");
        milesSpinner.setVisible(miles);
        stepsSpinner.setVisible(!miles);
        cardioPanel.revalidate(); cardioPanel.repaint();
    }

    /**
     * Returns a boolean value corresponding to if a workout is cardio.
     * @param type the name of the workout
     * @return a boolean value - true if the workout is cardio, false otherwise
     */
    private boolean isCardio(String type) {
        return type.equals("Run") || type.equals("Walk") || type.equals("Cycle") || type.equals("Swim");
    }

    /**
     * Updates several GUI components related to the user's progress to reflect the current state, should be called after an action updates values in memory
     */
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


    /**
     * Updates the GUI day list to reflect the current state of the program.
     */
    private void refreshDaysList() {
        if (currentUser == null) return;
        daysListModel.clear();
        List<LocalDate> dates = workoutSvc.listWorkouts(currentUser).stream()
                .map(w -> w.startedAt.toLocalDate())
                .distinct().sorted(Comparator.reverseOrder()).toList();
        for (LocalDate d : dates) daysListModel.addElement(d);
        if (!dates.isEmpty() && daysList.getSelectedValue() == null) daysList.setSelectedIndex(0);
    }

    /**
     * Sets the daysList object to select a day with the given date
     * @param date a LocalDate object representing a point in time
     */
    private void selectDay(LocalDate date) {
        for (int i = 0; i < daysListModel.size(); i++)  {
            if (daysListModel.get(i).equals(date)) {
                daysList.setSelectedIndex(i);
                return;
            }
        }
    }

    /**
     * Updates the savedWorkoutsModel GUI object to be populated with only workouts from a given day.
     * @param d the day to populate the saved workouts object with
     */
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

    /**
     * A GUI function for displaying information relevant to a workout. This modifies the savedWorkoutDetail JTextArea object.
     * @param row a value corresponding to an index in the WorkoutService object. This is how the function gets access to the workout to display information for.
     */
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
        Workout w = list.get(row);//Changed the "0" to "row" to fix bug. If this broke something else change it back and I'll try something else.
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

    /**
     * Concatenates a string containing details relevant to a workout object.
     * @param w the given workout object
     * @return A string describing the details of the workout object
     */
    private String summarizeDetail(Workout w) {
        if (isCardio(w.type)) {
            return w.distanceUnit.equals("Miles")
                    ? (String.format("%.2f miles", w.distanceValue == null ? 0.0 : w.distanceValue))
                    : (w.steps == null ? "0 steps" : (w.steps + " steps"));
        }
        String bp = (w.bodyPart == null || w.bodyPart.isEmpty()) ? "-" : w.bodyPart;
        return bp + " — " + (w.exerciseName == null ? "-" : w.exerciseName) + " (" + w.sets + "x" + w.reps + ")";
    }

    /**
     * Updates the calorie count from the selected food (using the foodPicker object).
     */
    private void updateCaloriesFromFood() {
        Food f = (Food) foodPicker.getSelectedItem();
        int grams = (Integer) gramsSpinner.getValue();
        if (f != null) {
            int kcal = Math.round(f.kcalPer100g * grams / 100f);
            caloriesSpinner.setValue(kcal);
        }
    }

    /**
     * This function is called before several actions in the program to ensure that the user is logged in. It returns a boolean value indicating
     * this, and also displays a message telling the user to sign in.
     * @return a boolean value indicating if the user is logged in: true if they are, false if they are not.
     */
    private boolean ensureUser() {
        if (currentUser == null) {
            JOptionPane.showMessageDialog(this, "Please sign in first.", "Not Signed In", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }
}
