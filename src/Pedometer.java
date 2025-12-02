import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class Pedometer implements KeyListener {

    private double steps;

    @Override
    public void keyPressed(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (keyCode == KeyEvent.VK_SPACE)
        {
            steps++;
        }
    }

    @Override
    public void keyReleased(KeyEvent event) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
        
    }

    public double getSteps() {
        return steps;
    }

    public void setSteps(double new_steps) {
        steps = new_steps;
    } 
}