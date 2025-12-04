package main;


import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;

/**
 * Simple sign-in screen shown when the app first opens.
 * User types email/username + password, presses Enter on password,
 * and we open the main FitnessFrame on the Nutrition tab.
 */
public class LoginFrame extends JFrame {

    private final InMemoryStore store;

    private JTextField userField;
    private JPasswordField passwordField;

    public LoginFrame(InMemoryStore store) {
        super("Fitness App - Sign In");
        this.store = store;
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 240);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Theme.BG);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 10, 10, 10);
        c.fill = GridBagConstraints.HORIZONTAL;

        // Title
        JLabel title = new JLabel("Sign in to continue");
        title.setForeground(Theme.TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        panel.add(title, c);

        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;

        // Email / username label
        JLabel userLbl = new JLabel("Email or username:");
        userLbl.setForeground(Theme.TEXT);
        c.gridx = 0;
        c.gridy = 1;
        panel.add(userLbl, c);

        // Email / username field
        userField = new JTextField(20);
        userField.setBackground(Theme.CARD);
        userField.setForeground(Theme.TEXT);
        userField.setCaretColor(Theme.TEXT);
        userField.setBorder(new javax.swing.border.LineBorder(new Color(90, 95, 105)));
        c.gridx = 1;
        panel.add(userField, c);

        // Password label
        JLabel passLbl = new JLabel("Password:");
        passLbl.setForeground(Theme.TEXT);
        c.gridx = 0;
        c.gridy = 2;
        panel.add(passLbl, c);

        // Password field
        passwordField = new JPasswordField(20);
        passwordField.setBackground(Theme.CARD);
        passwordField.setForeground(Theme.TEXT);
        passwordField.setCaretColor(Theme.TEXT);
        passwordField.setBorder(new javax.swing.border.LineBorder(new Color(90, 95, 105)));
        c.gridx = 1;
        panel.add(passwordField, c);

        // Sign in button
        JButton signInBtn = new RoundedButton("Sign In", null);
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        panel.add(signInBtn, c);

        // Pressing the button OR pressing Enter on password does the same thing
        ActionListener doLogin = e -> attemptLogin();
        signInBtn.addActionListener(doLogin);
        passwordField.addActionListener(doLogin); // "Enter" on password

        setContentPane(panel);
    }

    private void attemptLogin() {
        String userText = userField.getText().trim();

        if (userText.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Enter your email or username.",
                    "Missing Info",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        // In this simple version we don't store/check the password.
        String email = userText;
        String name  = userText; // treat it as both for now

        // Open the main app window and automatically log this user in
        FitnessFrame app = new FitnessFrame(store);
        app.autoLogin(email, name);      // sets current user + welcome + progress, selects Nutrition tab
        app.setVisible(true);            // show main window
        dispose();                       // close the login screen
    }
}
