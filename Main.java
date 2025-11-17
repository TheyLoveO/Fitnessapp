import javax.swing.*;

public class Main {

    // ======== MAIN ========
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        InMemoryStore store = new InMemoryStore();
        SwingUtilities.invokeLater(() -> new FitnessFrame(store).setVisible(true));
    }
}