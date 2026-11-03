import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.util.Random;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

public class GamePanel extends JPanel {
    private static final int CELL = 25;
    private static final int COLS = 24;
    private static final int ROWS = 24;

    private final GameModel model = new GameModel(COLS, ROWS, new Random());
    private final Timer timer;

    public GamePanel() {
        setPreferredSize(new Dimension(COLS * CELL, ROWS * CELL));
        setBackground(Color.BLACK);
        setFocusable(true);

        bind("UP", GameModel.Direction.UP);
        bind("DOWN", GameModel.Direction.DOWN);
        bind("LEFT", GameModel.Direction.LEFT);
        bind("RIGHT", GameModel.Direction.RIGHT);

        timer = new Timer(model.delayMs(), e -> {
            Timer t = (Timer) e.getSource();
            model.tick();
            t.setDelay(model.delayMs());
            if (model.isGameOver()) {
                t.stop();
            }
            repaint();
        });

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ENTER"), "restart");
        getActionMap().put("restart", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (model.isGameOver()) {
                    model.reset();
                    timer.setDelay(model.delayMs());
                    timer.start();
                    repaint();
                }
            }
        });
        timer.start();
    }

    private void bind(String key, GameModel.Direction dir) {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), key);
        getActionMap().put(key, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                model.setDirection(dir);
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Point apple = model.getApple();
        if (apple != null) {
            g.setColor(Color.RED);
            g.fillOval(apple.x * CELL + 2, apple.y * CELL + 2, CELL - 4, CELL - 4);
        }

        boolean head = true;
        for (Point p : model.getSnake()) {
            g.setColor(head ? new Color(120, 255, 120) : new Color(40, 180, 40));
            g.fillRoundRect(p.x * CELL + 1, p.y * CELL + 1, CELL - 2, CELL - 2, 8, 8);
            head = false;
        }

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        g.setColor(Color.WHITE);
        String score = "Score: " + model.getScore();
        FontMetrics fm = g.getFontMetrics();
        g.drawString(score, getWidth() - fm.stringWidth(score) - 10, fm.getAscent() + 8);

        if (model.isGameOver()) {
            g.setColor(new Color(0, 0, 0, 160));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 36));
            center(g, "Game Over", getHeight() / 2 - 10);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
            center(g, "Press Enter to restart", getHeight() / 2 + 25);
        }
    }

    private void center(Graphics2D g, String s, int y) {
        g.drawString(s, (getWidth() - g.getFontMetrics().stringWidth(s)) / 2, y);
    }
}
