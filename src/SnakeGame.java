import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.util.ArrayDeque;
import java.util.concurrent.ThreadLocalRandom;
import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public final class SnakeGame {
    private SnakeGame() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Snake");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new GamePanel());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.setVisible(true);
        });
    }

    private static final class GamePanel extends JPanel {
        private static final long serialVersionUID = 1L;

        private static final int CELL_SIZE = 24;
        private static final int COLUMNS = 25;
        private static final int ROWS = 25;
        private static final int BOARD_X = 20;
        private static final int BOARD_Y = 72;
        private static final int INITIAL_DELAY = 140;
        private static final int MINIMUM_DELAY = 1;
        private static final int SPEEDUP_PER_LEVEL = 12;

        private static final Color BACKGROUND = new Color(19, 25, 22);
        private static final Color BOARD_BACKGROUND = new Color(27, 36, 31);
        private static final Color GRID_COLOR = new Color(34, 44, 38);
        private static final Color SNAKE_COLOR = new Color(91, 190, 91);
        private static final Color HEAD_COLOR = new Color(139, 224, 112);
        private static final Color APPLE_COLOR = new Color(228, 77, 68);
        private static final Color TEXT_COLOR = new Color(236, 241, 236);
        private static final Color MUTED_TEXT_COLOR = new Color(165, 178, 168);

        private final ArrayDeque<Point> snake = new ArrayDeque<>();
        private final Timer timer;
        private Point apple;
        private Direction direction;
        private Direction nextDirection;
        private int applesEaten;
        private boolean gameOver;
        private boolean boardFilled;

        GamePanel() {
            setPreferredSize(new Dimension(
                    BOARD_X * 2 + COLUMNS * CELL_SIZE,
                    BOARD_Y + ROWS * CELL_SIZE + 20));
            setBackground(BACKGROUND);
            setFocusable(true);
            bindKeys();
            timer = new Timer(INITIAL_DELAY, event -> advance());
            startNewGame();
        }

        private void bindKeys() {
            bindDirection("UP", Direction.UP);
            bindDirection("DOWN", Direction.DOWN);
            bindDirection("LEFT", Direction.LEFT);
            bindDirection("RIGHT", Direction.RIGHT);
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("R"), "restart");
            getActionMap().put("restart", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    if (gameOver) {
                        startNewGame();
                    }
                }
            });
        }

        private void bindDirection(String key, Direction requested) {
            String actionName = "move-" + key;
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), actionName);
            getActionMap().put(actionName, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    if (!gameOver && requested != direction.opposite()) {
                        nextDirection = requested;
                    }
                }
            });
        }

        private void startNewGame() {
            snake.clear();
            int middleX = COLUMNS / 2;
            int middleY = ROWS / 2;
            snake.addFirst(new Point(middleX, middleY));
            snake.addLast(new Point(middleX - 1, middleY));
            snake.addLast(new Point(middleX - 2, middleY));
            direction = Direction.RIGHT;
            nextDirection = Direction.RIGHT;
            applesEaten = 0;
            gameOver = false;
            boardFilled = false;
            timer.setDelay(INITIAL_DELAY);
            placeApple();
            timer.start();
            repaint();
        }

        private void advance() {
            if (gameOver) {
                return;
            }

            direction = nextDirection;
            Point head = snake.peekFirst();
            Point nextHead = new Point(head.x + direction.dx, head.y + direction.dy);
            boolean eating = nextHead.equals(apple);
            Point tail = snake.peekLast();

            if (nextHead.x < 0 || nextHead.x >= COLUMNS
                    || nextHead.y < 0 || nextHead.y >= ROWS
                    || (snake.contains(nextHead) && (eating || !nextHead.equals(tail)))) {
                endGame(false);
                return;
            }

            snake.addFirst(nextHead);
            if (eating) {
                applesEaten++;
                updateSpeed();
                if (snake.size() == COLUMNS * ROWS) {
                    endGame(true);
                    return;
                }
                placeApple();
            } else {
                snake.removeLast();
            }

            repaint();
        }

        private void updateSpeed() {
            int levels = applesEaten / 5;
            int delay = Math.max(MINIMUM_DELAY, INITIAL_DELAY - levels * SPEEDUP_PER_LEVEL);
            timer.setDelay(delay);
        }

        private void placeApple() {
            int availableCells = COLUMNS * ROWS - snake.size();
            if (availableCells <= 0) {
                apple = null;
                return;
            }

            int selected = ThreadLocalRandom.current().nextInt(availableCells);
            for (int y = 0; y < ROWS; y++) {
                for (int x = 0; x < COLUMNS; x++) {
                    Point candidate = new Point(x, y);
                    if (!snake.contains(candidate) && selected-- == 0) {
                        apple = candidate;
                        return;
                    }
                }
            }
        }

        private void endGame(boolean filledBoard) {
            gameOver = true;
            boardFilled = filledBoard;
            timer.stop();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            drawHeader(g);
            drawBoard(g);
            drawSnake(g);
            drawApple(g);
            if (gameOver) {
                drawGameOver(g);
            }
            g.dispose();
        }

        private void drawHeader(Graphics2D g) {
            g.setColor(TEXT_COLOR);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 25));
            g.drawString("SNAKE", BOARD_X, 43);

            String score = "SCORE  " + applesEaten;
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
            FontMetrics metrics = g.getFontMetrics();
            int scoreX = getWidth() - BOARD_X - metrics.stringWidth(score);
            g.setColor(TEXT_COLOR);
            g.drawString(score, scoreX, 43);
        }

        private void drawBoard(Graphics2D g) {
            int width = COLUMNS * CELL_SIZE;
            int height = ROWS * CELL_SIZE;
            g.setColor(BOARD_BACKGROUND);
            g.fillRoundRect(BOARD_X, BOARD_Y, width, height, 8, 8);
            g.setColor(GRID_COLOR);
            for (int x = 1; x < COLUMNS; x++) {
                int lineX = BOARD_X + x * CELL_SIZE;
                g.drawLine(lineX, BOARD_Y, lineX, BOARD_Y + height);
            }
            for (int y = 1; y < ROWS; y++) {
                int lineY = BOARD_Y + y * CELL_SIZE;
                g.drawLine(BOARD_X, lineY, BOARD_X + width, lineY);
            }
        }

        private void drawSnake(Graphics2D g) {
            boolean head = true;
            for (Point segment : snake) {
                int inset = 3;
                g.setColor(head ? HEAD_COLOR : SNAKE_COLOR);
                g.fillRoundRect(
                        BOARD_X + segment.x * CELL_SIZE + inset,
                        BOARD_Y + segment.y * CELL_SIZE + inset,
                        CELL_SIZE - inset * 2,
                        CELL_SIZE - inset * 2,
                        8,
                        8);
                head = false;
            }
        }

        private void drawApple(Graphics2D g) {
            if (apple == null) {
                return;
            }
            int inset = 5;
            g.setColor(APPLE_COLOR);
            g.fillOval(
                    BOARD_X + apple.x * CELL_SIZE + inset,
                    BOARD_Y + apple.y * CELL_SIZE + inset,
                    CELL_SIZE - inset * 2,
                    CELL_SIZE - inset * 2);
        }

        private void drawGameOver(Graphics2D g) {
            int boardWidth = COLUMNS * CELL_SIZE;
            int boardHeight = ROWS * CELL_SIZE;
            g.setColor(new Color(8, 12, 10, 205));
            g.fillRoundRect(BOARD_X, BOARD_Y, boardWidth, boardHeight, 8, 8);

            String title = boardFilled ? "BOARD CLEARED!" : "GAME OVER";
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 31));
            drawCentered(g, title, BOARD_Y + boardHeight / 2 - 24, TEXT_COLOR);

            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
            drawCentered(g, "Final score: " + applesEaten, BOARD_Y + boardHeight / 2 + 12, TEXT_COLOR);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
            drawCentered(g, "Press R to play again", BOARD_Y + boardHeight / 2 + 47, MUTED_TEXT_COLOR);
        }

        private void drawCentered(Graphics2D g, String text, int baseline, Color color) {
            FontMetrics metrics = g.getFontMetrics();
            int x = BOARD_X + (COLUMNS * CELL_SIZE - metrics.stringWidth(text)) / 2;
            g.setColor(color);
            g.drawString(text, x, baseline);
        }
    }

    private enum Direction {
        UP(0, -1),
        DOWN(0, 1),
        LEFT(-1, 0),
        RIGHT(1, 0);

        private final int dx;
        private final int dy;

        Direction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }

        private Direction opposite() {
            switch (this) {
                case UP:
                    return DOWN;
                case DOWN:
                    return UP;
                case LEFT:
                    return RIGHT;
                case RIGHT:
                    return LEFT;
                default:
                    throw new IllegalStateException("Unknown direction: " + this);
            }
        }
    }

    private static final class Point implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        private final int x;
        private final int y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Point)) {
                return false;
            }
            Point point = (Point) other;
            return x == point.x && y == point.y;
        }

        @Override
        public int hashCode() {
            return 31 * x + y;
        }
    }
}
