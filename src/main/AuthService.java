package main;

/**
 * The authentication service for the application.
 */
public class AuthService { 
    private final InMemoryStore store; 
    AuthService(InMemoryStore s){
        store=s;
    } 
    
    User signInOrSignUp(String e,String n) {
        return store.getOrCreateUser(e,n);
    } 
}