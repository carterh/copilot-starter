import java.awt.Point;
import java.util.Random;

public class GameModelTest {
    private static int failures = 0;

    private static void check(boolean cond, String name) {
        System.out.println((cond ? "PASS " : "FAIL ") + name);
        if (!cond) {
            failures++;
        }
    }

    private static GameModel fresh() {
        GameModel m = new GameModel(10, 10, new Random(1));
        m.setApple(new Point(0, 0));
        return m;
    }

    public static void main(String[] args) {
        GameModel m = fresh();
        Point head = m.getSnake().peekFirst();
        m.tick();
        check(m.getSnake().peekFirst().equals(new Point(head.x + 1, head.y)) && m.getSnake().size() == 3,
                "moves right without growing");

        m = fresh();
        m.setDirection(GameModel.Direction.LEFT);
        m.tick();
        check(m.getDirection() == GameModel.Direction.RIGHT, "reversal ignored");

        m = fresh();
        Point h = m.getSnake().peekFirst();
        m.setApple(new Point(h.x + 1, h.y));
        m.tick();
        check(m.getScore() == 1 && m.getSnake().size() == 4, "eating grows and scores");

        m = fresh();
        for (int i = 0; i < 20 && !m.isGameOver(); i++) {
            m.tick();
        }
        check(m.isGameOver(), "wall collision ends game");

        // Snake coiled so that turning left runs the head into its own body.
        m = fresh();
        m.getSnake().clear();
        int[][] body = {{5, 5}, {5, 6}, {4, 6}, {4, 5}, {4, 4}, {5, 4}, {6, 4}};
        for (int[] b : body) {
            m.getSnake().addLast(new Point(b[0], b[1]));
        }
        m.setDirection(GameModel.Direction.UP);
        m.tick();
        check(m.isGameOver(), "self collision ends game");

        m = fresh();
        m.reset();
        check(!m.isGameOver() && m.getScore() == 0 && m.getSnake().size() == 3, "reset restores state");

        m = fresh();
        check(m.delayMs() == 150, "initial delay 150");
        m.setScore(4);
        check(m.delayMs() == 150, "no speedup at 4 apples");
        m.setScore(5);
        check(m.delayMs() == 135, "speedup at 5 apples");
        m.setScore(9);
        check(m.delayMs() == 135, "no extra speedup at 9 apples");
        m.setScore(10);
        check(m.delayMs() == 120, "speedup at 10 apples");
        m.setScore(100);
        check(m.delayMs() == 50, "delay floors at 50");

        if (failures > 0) {
            System.out.println(failures + " failure(s)");
            System.exit(1);
        }
        System.out.println("All tests passed");
    }
}
