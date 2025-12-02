
import javax.swing.*;

public class Main {

    // ======== MAIN ========
    public static void main(String[] args) {
        try { 
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); 
        } catch (Exception ignored) {}

        InMemoryStore store = new InMemoryStore();

        // Start with the login window instead of the main app
        SwingUtilities.invokeLater(() -> new LoginFrame(store).setVisible(true));
    }
}
