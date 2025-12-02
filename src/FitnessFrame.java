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
    Pedometer pedometer = new Pedometer();

    // Header
    JLabel welcomeLbl = new JLabel("Welcome!");
    JTextField emailField = new JTextField(20);
    JTextField nameField = new JTextField(16);
    JButton loginBtn = new RoundedButton("Sign In / Create", pedometer);

    // Nutrition (tab 1) + Daily Goal
    JComboBox<Food> foodPicker = new JComboBox<>(Food.getDefaultFoods().toArray(new Food[0]));
    JSpinner gramsSpinner = new JSpinner(new SpinnerNumberModel(100, 1, 2000, 10));
    JSpinner caloriesSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 5000, 10));
    JButton addFoodBtn = new RoundedButton("Add Food", pedometer);
    JLabel foodSavedLbl = new JLabel(" ");
    DefaultTableModel todayNutritionModel = new DefaultTableModel(new Object[]{"Time", "Food", "Grams", "Calories"}, 0);
    JTable todayNutritionTable = new JTable(todayNutritionModel);


    // Daily Burn Goal controls (first page)
    JSpinner burnGoalSpinner = new JSpinner(new SpinnerNumberModel(500, 0, 20000, 50));
    JButton setBurnGoalBtn = new RoundedButton("Set Burn Goal", pedometer);

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

    JButton saveWorkoutBtn = new RoundedButton("Log Workout", pedometer);
    JLabel workoutSavedLbl = new JLabel(" ");

    // Saved Workouts (tab 3)
    DefaultListModel<LocalDate> daysListModel = new DefaultListModel<>();
    JList<LocalDate> daysList = new JList<>(daysListModel);
    DefaultTableModel savedWorkoutsModel = new DefaultTableModel(
            new Object[]{"Time", "Type", "Body Part / Detail", "Minutes", "Calories"}, 0);
    JTable savedWorkoutsTable = new JTable(savedWorkoutsModel);
    JTextArea savedWorkoutDetail = new JTextArea(8, 40);
    JTextArea workoutNotesArea = new JTextArea(4, 40);
    JButton saveNotesBtn = new RoundedButton("Save Notes", pedometer);
    Workout selectedWorkout = null;


    // Progress (tab 4)
    JLabel eatenTodayLbl = new JLabel("Eaten today: 0 kcal");
    JLabel burnedTodayLbl = new JLabel("Burned today: 0 kcal");
    JLabel burnGoalLbl = new JLabel("Burn goal: -");
    JLabel eatenWeekLbl = new JLabel("Last 7 days eaten: 0 kcal");
    JLabel burnedWeekLbl = new JLabel("Last 7 days burned: 0 kcal");
    JLabel stepsTodayLbl = new JLabel("Steps today: 0");
    JLabel stepsTotalLbl = new JLabel("Total steps: 0");
    JButton refreshProgressBtn = new RoundedButton("Refresh Progress", pedometer);

    // Weekly nutrition list
    DefaultTableModel weekNutritionModel = new DefaultTableModel(
            new Object[]{"Date", "Time", "Food", "Grams", "Calories"}, 0);
    JTable weekNutritionTable = new JTable(weekNutritionModel);
    JLabel weekRangeLbl = new JLabel("This week: -");

    // BMI calculator controls
    JSpinner bmiHeightInSpinner = new JSpinner(new SpinnerNumberModel(68, 36, 96, 1));
    JSpinner bmiWeightLbSpinner = new JSpinner(new SpinnerNumberModel(180.0, 50.0, 600.0, 0.5));
    JButton calcBmiBtn = new RoundedButton("Calculate BMI", pedometer);
    JLabel bmiResultLbl = new JLabel("BMI: -");

    // Pedometer tab
    int pedometerSteps = 0;
    JLabel pedometerStepsLbl = new JLabel("Steps: 0");
    JButton pedometerPresser = new RoundedButton("", pedometer);

    // Day cycle
    LocalDate currentDay = LocalDate.now();
    JLabel currentDayLbl = new JLabel();
    JButton nextDayBtn = new RoundedButton("Next Day", pedometer);
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

        addKeyListener(pedometer);
        setFocusable(false);

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

        // Right-aligned panel showing the current user's name
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightPanel.setOpaque(true);
        rightPanel.setBackground(Theme.CARD);
        rightPanel.add(welcomeLbl);

        p.add(rightPanel, BorderLayout.EAST);
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
        tabs.addTab("Pedometer", buildPedometerTab());

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

        JPanel dayHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        dayHeader.setOpaque(false);
        dayHeader.add(currentDayLbl);
        dayHeader.add(nextDayBtn);
        center.add(dayHeader, BorderLayout.NORTH);

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

        savedWorkoutDetail.setEditable(false);
        savedWorkoutDetail.setLineWrap(true);
        savedWorkoutDetail.setWrapStyleWord(true);
        JScrollPane detailScroll = new JScrollPane(savedWorkoutDetail);
        detailScroll.getViewport().setBackground(Color.WHITE);
        detailPanel.add(detailScroll, BorderLayout.CENTER);

        // Notes section for this workout
        JPanel notesPanel = new JPanel(new BorderLayout());
        notesPanel.setOpaque(false);
        JLabel notesLbl = new JLabel("Notes about this workout:");
        notesLbl.setForeground(Theme.TEXT);
        notesPanel.add(notesLbl, BorderLayout.NORTH);

        workoutNotesArea.setLineWrap(true);
        workoutNotesArea.setWrapStyleWord(true);
        JScrollPane notesScroll = new JScrollPane(workoutNotesArea);
        notesScroll.getViewport().setBackground(Color.WHITE);
        notesPanel.add(notesScroll, BorderLayout.CENTER);

        JPanel notesButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        notesButtons.setOpaque(false);
        notesButtons.add(saveNotesBtn);
        notesPanel.add(notesButtons, BorderLayout.SOUTH);

        detailPanel.add(notesPanel, BorderLayout.SOUTH);

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
        eatenWeekLbl.setForeground(Theme.TEXT);
        burnedWeekLbl.setForeground(Theme.TEXT);
        burnGoalLbl.setForeground(Theme.SUBTLE);
        stepsTodayLbl.setForeground(Theme.TEXT);
        stepsTotalLbl.setForeground(Theme.TEXT);
        weekRangeLbl.setForeground(Theme.TEXT);
        bmiResultLbl.setForeground(Theme.TEXT);

        int y=0;
        c.gridx=0; c.gridy=y; top.add(eatenTodayLbl, c);
        c.gridx=1; c.gridy=y++; top.add(burnedTodayLbl, c);

        c.gridx=0; c.gridy=y; top.add(stepsTodayLbl, c);
        c.gridx=1; c.gridy=y++; top.add(stepsTotalLbl, c);

        c.gridx=0; c.gridy=y; top.add(eatenWeekLbl, c);
        c.gridx=1; c.gridy=y++; top.add(burnedWeekLbl, c);

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
        center.add(Box.createVerticalStrut(10));

        // Weekly Nutrition panel
        JPanel weekPanel = new JPanel(new BorderLayout());
        weekPanel.setOpaque(true);
        weekPanel.setBackground(Theme.CARD);
        TitledBorder weekBorder = new TitledBorder("Weekly Nutrition (Last 7 Days)");
        weekBorder.setTitleColor(Theme.TEXT);
        weekPanel.setBorder(weekBorder);

        weekRangeLbl.setBorder(new EmptyBorder(4,8,4,8));
        weekPanel.add(weekRangeLbl, BorderLayout.NORTH);

        JScrollPane weekScroll = new JScrollPane(weekNutritionTable);
        weekScroll.getViewport().setBackground(Color.WHITE);
        weekPanel.add(weekScroll, BorderLayout.CENTER);

        center.add(weekPanel);
        center.add(Box.createVerticalStrut(10));

        // BMI calculator panel
        JPanel bmiPanel = new JPanel(new GridBagLayout());
        bmiPanel.setOpaque(true);
        bmiPanel.setBackground(Theme.CARD);
        TitledBorder bmiBorder = new TitledBorder("BMI Calculator");
        bmiBorder.setTitleColor(Theme.TEXT);
        bmiPanel.setBorder(bmiBorder);

        GridBagConstraints b = new GridBagConstraints();
        b.insets = new Insets(4,4,4,4);
        b.fill = GridBagConstraints.HORIZONTAL;

        JLabel hLbl = new JLabel("Height (inches):");
        JLabel wLbl = new JLabel("Weight (lb):");
        hLbl.setForeground(Theme.TEXT);
        wLbl.setForeground(Theme.TEXT);

        int by = 0;
        b.gridx = 0; b.gridy = by; bmiPanel.add(hLbl, b);
        b.gridx = 1; b.gridy = by; bmiPanel.add(bmiHeightInSpinner, b);
        by++;
        b.gridx = 0; b.gridy = by; bmiPanel.add(wLbl, b);
        b.gridx = 1; b.gridy = by; bmiPanel.add(bmiWeightLbSpinner, b);
        by++;
        b.gridx = 0; b.gridy = by; bmiPanel.add(calcBmiBtn, b);
        b.gridx = 1; b.gridy = by; bmiPanel.add(bmiResultLbl, b);

        center.add(bmiPanel);

        p.add(center, BorderLayout.CENTER);

        return p;
    }


    /**
     * Creates the Pedometer tab. This shows the step counts based on logged workouts.
     */
    private JPanel buildPedometerTab() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.CARD);
        p.setBorder(new EmptyBorder(12,12,12,12));

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Pedometer (from logged workouts)");
        title.setForeground(Theme.TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        center.add(title);
        center.add(Box.createVerticalStrut(16));
        
        center.add(pedometerPresser);

        pedometerStepsLbl.setForeground(Theme.TEXT);
        pedometerStepsLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        pedometerStepsLbl.setFont(pedometerStepsLbl.getFont().deriveFont(Font.BOLD, 24f));
        center.add(pedometerStepsLbl);

        center.add(Box.createVerticalStrut(8));

        JLabel hint = new JLabel("Steps are calculated from cardio workouts logged with unit = Steps.");
        hint.setForeground(Theme.SUBTLE);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(hint);

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

            // log against the current in-app day
            nutritionSvc.logNutritionForDate(currentUser, f.name, grams, kcal, currentDay);
            foodSavedLbl.setText("Added: " + f.name + " (" + grams + "g, " + kcal + " kcal)");
            refreshAllProgress();
        });

        pedometerPresser.addActionListener(e -> {
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
            LocalDateTime start = LocalDateTime.of(currentDay, LocalTime.of(h, m));

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
            selectDay(currentDay);
        });

        //when the user presses the refresh progress button
        refreshProgressBtn.addActionListener(e -> {
            refreshAllProgress();
            refreshDaysList();
        });

        // save notes for the selected workout
        saveNotesBtn.addActionListener(e -> {
            if (selectedWorkout == null) {
                JOptionPane.showMessageDialog(this, "Select a workout first.", "No Workout Selected", JOptionPane.WARNING_MESSAGE);
                return;
            }
            selectedWorkout.notes = workoutNotesArea.getText().trim();
            JOptionPane.showMessageDialog(this, "Notes saved for this workout.", "Notes Saved", JOptionPane.INFORMATION_MESSAGE);
        });

        // BMI calculator action
        calcBmiBtn.addActionListener(e -> {
            double hIn = ((Number) bmiHeightInSpinner.getValue()).doubleValue();
            double wLb = ((Number) bmiWeightLbSpinner.getValue()).doubleValue();
            if (hIn <= 0 || wLb <= 0) {
                JOptionPane.showMessageDialog(this, "Height and weight must be positive.", "BMI", JOptionPane.WARNING_MESSAGE);
                return;
            }
            double bmi = (wLb / (hIn * hIn)) * 703.0;
            String category;
            if (bmi < 18.5) category = "Underweight";
            else if (bmi < 25.0) category = "Normal";
            else if (bmi < 30.0) category = "Overweight";
            else category = "Obese";
            bmiResultLbl.setText(String.format("BMI: %.1f (%s)", bmi, category));
        });

        //when the user advances to the next day
        nextDayBtn.addActionListener(e -> {
            if (!ensureUser()) return;
            currentDay = currentDay.plusDays(1);
            refreshAllProgress();
            refreshDaysList();
            selectDay(currentDay);
        });
    }

    /**
     * Called from LoginFrame to log a user in and update the header and tabs.
     */
    public void autoLogin(String email, String name) {
        currentUser = auth.signInOrSignUp(email, name);
        welcomeLbl.setText("Welcome, " + currentUser.name + "!");
        currentDay = LocalDate.now();
        updateCurrentDayLabel();
        refreshAllProgress();
        refreshDaysList();
        if (tabs != null) {
            tabs.setSelectedIndex(0); // go to Nutrition tab
        }
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

        int eatenToday = nutritionSvc.dailyCalories(currentUser, currentDay);
        eatenTodayLbl.setText("Eaten today: " + eatenToday + " kcal");

        int burnedToday = workoutSvc.listWorkouts(currentUser).stream()
                .filter(w -> w.startedAt.toLocalDate().equals(currentDay))
                .mapToInt(w -> w.estimatedCalories)
                .sum();
        burnedTodayLbl.setText("Burned today: " + burnedToday + " kcal");

        // steps based on cardio workouts that use steps
        // int stepsToday = workoutSvc.listWorkouts(currentUser).stream()
        //         .filter(w -> w.startedAt.toLocalDate().equals(currentDay)
        //                 && isCardio(w.type)
        //                 && "Steps".equals(w.distanceUnit)
        //                 && w.steps != null)
        //         .mapToInt(w -> w.steps)
        //         .sum();
        // stepsTodayLbl.setText("Steps today: " + stepsToday);

        // int totalSteps = workoutSvc.listWorkouts(currentUser).stream()
        //         .filter(w -> isCardio(w.type)
        //                 && "Steps".equals(w.distanceUnit)
        //                 && w.steps != null)
        //         .mapToInt(w -> w.steps)
        //         .sum();
        stepsTotalLbl.setText("Total steps: " + pedometer.getSteps());
        pedometerStepsLbl.setText("Steps: " + pedometer.getSteps());

        // last 7 days (including currentDay)
        LocalDate weekEnd = currentDay;
        LocalDate weekStart = currentDay.minusDays(6);

        int eatenWeek = store.getNutrition(currentUser.email).stream()
                .filter(n -> {
                    LocalDate d = n.loggedAt.toLocalDate();
                    return !d.isBefore(weekStart) && !d.isAfter(weekEnd);
                })
                .mapToInt(n -> n.calories)
                .sum();
        eatenWeekLbl.setText("Last 7 days eaten: " + eatenWeek + " kcal");

        int burnedWeek = workoutSvc.listWorkouts(currentUser).stream()
                .filter(w -> {
                    LocalDate d = w.startedAt.toLocalDate();
                    return !d.isBefore(weekStart) && !d.isAfter(weekEnd);
                })
                .mapToInt(w -> w.estimatedCalories)
                .sum();
        burnedWeekLbl.setText("Last 7 days burned: " + burnedWeek + " kcal");

        // weekly nutrition table (last 7 days)
        weekNutritionModel.setRowCount(0);
        for (NutritionEntry n : nutritionSvc.listForRange(currentUser, weekStart, weekEnd)) {
            weekNutritionModel.addRow(new Object[]{
                    n.loggedAt.toLocalDate().toString(),
                    n.loggedAt.toLocalTime().withSecond(0).withNano(0).toString(),
                    n.itemName,
                    n.grams,
                    n.calories
            });
        }
        weekRangeLbl.setText("This week: " + weekStart + " to " + weekEnd + "");

        if (currentUser.goal.dailyBurnTarget != null) {
            int remain = Math.max(currentUser.goal.dailyBurnTarget - burnedToday, 0);
            burnGoalLbl.setText("Burn goal: " + currentUser.goal.dailyBurnTarget + " kcal (Remaining: " + remain + ")");
        } else {
            burnGoalLbl.setText("Burn goal: -");
        }

        // progress nutrition table (for current day)
        progressNutritionModel.setRowCount(0);
        for (NutritionEntry n : nutritionSvc.listForDate(currentUser, currentDay)) {
            progressNutritionModel.addRow(new Object[]{
                    n.loggedAt.toLocalTime().withSecond(0).withNano(0).toString(), n.itemName, n.grams, n.calories
            });
        }

        // today's workouts table (for current day)
        progressWorkoutsModel.setRowCount(0);
        for (Workout w : workoutSvc.listWorkouts(currentUser).stream()
                .filter(w -> w.startedAt.toLocalDate().equals(currentDay)).toList()) {
            progressWorkoutsModel.addRow(new Object[]{
                    w.startedAt.toLocalDate().toString(),
                    w.startedAt.toLocalTime().withNano(0).toString(),
                    w.type,
                    summarizeDetail(w),
                    w.durationMin, w.estimatedCalories
            });
        }

        // keep the nutrition tab's table in sync with the active day
        refreshTodayNutritionTable();
        updateCurrentDayLabel();
    }

    /**
     * Updates the GUI day list to reflect the current state of the program.
     */
    private void refreshDaysList() {
        if (currentUser == null) return;
        daysListModel.clear();
        List<LocalDate> dates = workoutSvc.listWorkouts(currentUser).stream()
                .map(w -> w.startedAt.toLocalDate())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
        for (LocalDate d : dates) {
            daysListModel.addElement(d);
        }
        if (!dates.isEmpty() && daysList.getSelectedValue() == null) {
            daysList.setSelectedIndex(0);
        }
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
    
    /**
     * Rebuilds the 'Today's Nutrition' table on the Nutrition tab for the current day.
     */
    private void refreshTodayNutritionTable() {
        todayNutritionModel.setRowCount(0);
        if (currentUser == null) return;
        for (NutritionEntry n : nutritionSvc.listForDate(currentUser, currentDay)) {
            todayNutritionModel.addRow(new Object[]{
                    n.loggedAt.toLocalTime().withSecond(0).withNano(0).toString(),
                    n.itemName,
                    n.grams,
                    n.calories
            });
        }
    }

    /**
     * Updates the small day label used next to the Next Day button.
     */
    private void updateCurrentDayLabel() {
        currentDayLbl.setText("Day: " + currentDay.toString());
    }

    private void populateSavedWorkoutsForDay(LocalDate d) {
        savedWorkoutsModel.setRowCount(0);
        savedWorkoutDetail.setText("");
        workoutNotesArea.setText("");
        selectedWorkout = null;
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
        if (list.isEmpty()) {
            savedWorkoutDetail.setText("");
            workoutNotesArea.setText("");
            selectedWorkout = null;
            return;
        }
        Workout w = list.get(row);
        selectedWorkout = w;

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

        workoutNotesArea.setText(w.notes == null ? "" : w.notes);
        workoutNotesArea.setCaretPosition(0);
    }
//nates a string containing details relevant to a workout object.
    // * @param w the given workout object
     //* @return A string describing the details of the workout object
     //*/
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
