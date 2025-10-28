import java.awt.*;
import java.awt.event.ItemEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

public class Main {

    // ======== THEME ========
    static class Theme {
        static final Color RED = new Color(220, 53, 69);
        static final Color RED_DARK = new Color(200, 40, 55);
        static final Color YELLOW = new Color(255, 193, 7);
        static final Color BG = new Color(250, 250, 250);
        static final Color CARD = new Color(255, 255, 255);
        static final Color TEXT = new Color(33, 37, 41);
        static final Color SUBTLE = new Color(108, 117, 125);
        static final Font BUTTON_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 13);
        static final int RADIUS = 18;
    }


    // ======== UTIL ========
    private static LocalDate startOfThisWeek() { // unused now
        LocalDate today = LocalDate.now();
        return today.minusDays((today.getDayOfWeek().getValue() + 6) % 7);
    }

    // ======== MAIN ========
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        InMemoryStore store = new InMemoryStore();
        SwingUtilities.invokeLater(() -> new FitnessFrame(store).setVisible(true));
    }
}