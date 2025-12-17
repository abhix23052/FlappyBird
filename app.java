import javax.swing.*;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Flappy Bird");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            FlappyBird game = new FlappyBird();
            frame.add(game);
            frame.pack();
            frame.setLocationRelativeTo(null); // Center on screen
            frame.setResizable(false);
            frame.setVisible(true);
        });
    }
}
