package snake;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;
import javax.swing.JPanel;
import javax.swing.Timer;

public class GamePanel extends JPanel {
    static final int CELLS = 20;
    static final int CELL_SIZE = 30;
    static final int SIZE = CELLS * CELL_SIZE;
    static final int START_DELAY_MS = 150;
    static final int DELAY_STEP_MS = 10;
    static final int MIN_DELAY_MS = 50;
    static final int APPLES_PER_SPEEDUP = 5;

    private final Random random = new Random();
    private final Timer timer;
    private final Deque<Point> snake = new ArrayDeque<>();
    private Point apple;
    private int dx, dy;
    private int pendingDx, pendingDy;
    private int score;
    private boolean gameOver;

    public GamePanel() {
        setPreferredSize(new Dimension(SIZE, SIZE));
        setBackground(Color.BLACK);
        setFocusable(true);
        timer = new Timer(START_DELAY_MS, e -> tick());
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKey(e.getKeyCode());
            }
        });
        reset();
    }

    private void reset() {
        snake.clear();
        int mid = CELLS / 2;
        for (int i = 0; i < 3; i++) {
            snake.addLast(new Point(mid - i, mid));
        }
        dx = pendingDx = 1;
        dy = pendingDy = 0;
        score = 0;
        gameOver = false;
        placeApple();
        timer.setDelay(START_DELAY_MS);
        timer.restart();
        repaint();
    }

    private void placeApple() {
        Point p;
        do {
            p = new Point(random.nextInt(CELLS), random.nextInt(CELLS));
        } while (snake.contains(p));
        apple = p;
    }

    private void handleKey(int key) {
        if (gameOver) {
            if (key == KeyEvent.VK_ENTER) {
                reset();
            }
            return;
        }
        int ndx = pendingDx, ndy = pendingDy;
        switch (key) {
            case KeyEvent.VK_UP -> { ndx = 0; ndy = -1; }
            case KeyEvent.VK_DOWN -> { ndx = 0; ndy = 1; }
            case KeyEvent.VK_LEFT -> { ndx = -1; ndy = 0; }
            case KeyEvent.VK_RIGHT -> { ndx = 1; ndy = 0; }
            default -> { return; }
        }
        // Compare with the direction last moved, so rapid key presses can't reverse the snake.
        if (ndx != -dx || ndy != -dy) {
            pendingDx = ndx;
            pendingDy = ndy;
        }
    }

    private void tick() {
        dx = pendingDx;
        dy = pendingDy;
        Point head = snake.peekFirst();
        Point next = new Point(head.x + dx, head.y + dy);
        boolean eating = next.equals(apple);

        if (next.x < 0 || next.y < 0 || next.x >= CELLS || next.y >= CELLS) {
            endGame();
            return;
        }
        // The tail moves away this tick unless we are growing, so it is not a collision.
        Point tail = snake.peekLast();
        if (snake.contains(next) && (eating || !next.equals(tail))) {
            endGame();
            return;
        }

        snake.addFirst(next);
        if (eating) {
            score++;
            if (score % APPLES_PER_SPEEDUP == 0) {
                timer.setDelay(Math.max(MIN_DELAY_MS, timer.getDelay() - DELAY_STEP_MS));
            }
            if (snake.size() == CELLS * CELLS) {
                endGame();
                return;
            }
            placeApple();
        } else {
            snake.removeLast();
        }
        repaint();
    }

    private void endGame() {
        gameOver = true;
        timer.stop();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(Color.RED);
        g2.fillOval(apple.x * CELL_SIZE + 2, apple.y * CELL_SIZE + 2, CELL_SIZE - 4, CELL_SIZE - 4);

        boolean head = true;
        for (Point p : snake) {
            g2.setColor(head ? new Color(120, 255, 120) : new Color(40, 180, 40));
            g2.fillRoundRect(p.x * CELL_SIZE + 1, p.y * CELL_SIZE + 1, CELL_SIZE - 2, CELL_SIZE - 2, 8, 8);
            head = false;
        }

        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        g2.setColor(Color.WHITE);
        String text = "Score: " + score;
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, SIZE - fm.stringWidth(text) - 10, fm.getAscent() + 8);

        if (gameOver) {
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 40));
            fm = g2.getFontMetrics();
            String over = "Game Over";
            g2.drawString(over, (SIZE - fm.stringWidth(over)) / 2, SIZE / 2);
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
            fm = g2.getFontMetrics();
            String hint = "Press Enter to restart";
            g2.drawString(hint, (SIZE - fm.stringWidth(hint)) / 2, SIZE / 2 + 35);
        }
    }
}
