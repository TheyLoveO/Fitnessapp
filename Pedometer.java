
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class Pedometer implements KeyListener {

    public double steps;

    @Override
    public void keyPressed(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (keyCode == KeyEvent.VK_SPACE)
        {
            steps++;
            System.out.println(steps);
        }
    }

    @Override
    public void keyReleased(KeyEvent event) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
        
    }
}