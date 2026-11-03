import java.awt.Point;
import java.util.Random;

public class GameModel {
    public enum Direction {
        UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0);

        final int dx, dy;

        Direction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }

        boolean isOpposite(Direction o) {
            return dx == -o.dx && dy == -o.dy;
        }
    }

    public static final int INITIAL_DELAY_MS = 150;
    public static final int DELAY_STEP_MS = 15;
    public static final int MIN_DELAY_MS = 50;
    public static final int APPLES_PER_SPEEDUP = 5;

    private final int cols, rows;
    private final Random random;
    private final java.util.Deque<Point> snake = new java.util.ArrayDeque<>();
    private Direction direction;
    private Direction pending;
    private Point apple;
    private int score;
    private boolean gameOver;

    public GameModel(int cols, int rows, Random random) {
        this.cols = cols;
        this.rows = rows;
        this.random = random;
        reset();
    }

    public void reset() {
        snake.clear();
        int y = rows / 2;
        for (int i = 0; i < 3; i++) {
            snake.addLast(new Point(cols / 2 - i, y));
        }
        direction = Direction.RIGHT;
        pending = Direction.RIGHT;
        score = 0;
        gameOver = false;
        placeApple();
    }

    /** Queues a turn; reversing into the snake's own body is ignored. */
    public void setDirection(Direction d) {
        if (!d.isOpposite(direction)) {
            pending = d;
        }
    }

    public void tick() {
        if (gameOver) {
            return;
        }
        direction = pending;
        Point head = snake.peekFirst();
        Point next = new Point(head.x + direction.dx, head.y + direction.dy);
        boolean eating = next.equals(apple);

        if (next.x < 0 || next.y < 0 || next.x >= cols || next.y >= rows) {
            gameOver = true;
            return;
        }
        // The tail moves away this tick unless the snake is growing.
        Point tail = snake.peekLast();
        for (Point p : snake) {
            if (p.equals(next) && (eating || p != tail)) {
                gameOver = true;
                return;
            }
        }

        snake.addFirst(next);
        if (eating) {
            score++;
            placeApple();
        } else {
            snake.removeLast();
        }
    }

    private void placeApple() {
        if (snake.size() >= cols * rows) {
            apple = null;
            gameOver = true;
            return;
        }
        Point p;
        do {
            p = new Point(random.nextInt(cols), random.nextInt(rows));
        } while (snake.contains(p));
        apple = p;
    }

    public int delayMs() {
        int d = INITIAL_DELAY_MS - DELAY_STEP_MS * (score / APPLES_PER_SPEEDUP);
        return Math.max(MIN_DELAY_MS, d);
    }

    public java.util.Deque<Point> getSnake() { return snake; }
    public Point getApple() { return apple; }
    public int getScore() { return score; }
    public boolean isGameOver() { return gameOver; }
    public Direction getDirection() { return direction; }

    /** Test hooks. */
    void setApple(Point p) { apple = p; }
    void setScore(int s) { score = s; }
}
