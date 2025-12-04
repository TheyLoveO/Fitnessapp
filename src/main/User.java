package main;

/**
 * Represents a user of the application.
 */
public class User {
    final String email;
    String name;
    Goal goal = new Goal();
    User(String email, String name) { this.email = email; this.name = name; }
}