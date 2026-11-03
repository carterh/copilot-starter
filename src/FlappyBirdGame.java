import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class FlappyBirdGame {
    private FlappyBirdGame() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Flappy Bird");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.setContentPane(new GamePanel());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    @SuppressWarnings("serial")
    private static final class GamePanel extends JPanel {
        private static final int WIDTH = 800;
        private static final int HEIGHT = 600;
        private static final int GROUND_HEIGHT = 76;
        private static final int BIRD_SIZE = 34;
        private static final int PIPE_WIDTH = 76;
        private static final int PIPE_GAP = 174;
        private static final int PIPE_SPACING = 290;
        private static final double GRAVITY = 0.42;
        private static final double FLAP_VELOCITY = -8.2;
        private static final double PIPE_SPEED = 3.2;

        private final Timer timer;
        private final List<Pipe> pipes = new ArrayList<>();
        private final Random random = new Random();
        private double birdX;
        private double birdY;
        private double birdVelocity;
        private int score;
        private GameState state = GameState.READY;

        GamePanel() {
            setPreferredSize(new java.awt.Dimension(WIDTH, HEIGHT));
            setFocusable(true);
            bindKey("UP", "flap-up", this::flap);
            bindKey("SPACE", "flap-space", this::flap);
            bindKey("LEFT", "move-left", () -> moveBird(-24));
            bindKey("RIGHT", "move-right", () -> moveBird(24));
            bindKey("DOWN", "dive", () -> {
                if (state == GameState.PLAYING) {
                    birdVelocity = Math.max(birdVelocity, 5.5);
                }
            });
            bindKey("ENTER", "restart-enter", this::startOrRestart);
            bindKey("R", "restart-r", this::startOrRestart);
            resetGame();
            timer = new Timer(16, this::updateGame);
            timer.start();
        }

        private void bindKey(String key, String name, Runnable action) {
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), name);
            getActionMap().put(name, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    action.run();
                }
            });
        }

        private void flap() {
            if (state == GameState.READY || state == GameState.GAME_OVER) {
                resetGame();
                state = GameState.PLAYING;
            }
            if (state == GameState.PLAYING) {
                birdVelocity = FLAP_VELOCITY;
            }
        }

        private void startOrRestart() {
            if (state != GameState.PLAYING) {
                resetGame();
                state = GameState.PLAYING;
                birdVelocity = FLAP_VELOCITY;
            }
        }

        private void resetGame() {
            birdX = 180;
            birdY = HEIGHT * 0.43;
            birdVelocity = 0;
            score = 0;
            pipes.clear();
            pipes.add(new Pipe(WIDTH + 80, randomGapTop()));
            pipes.add(new Pipe(WIDTH + 80 + PIPE_SPACING, randomGapTop()));
        }

        private double randomGapTop() {
            int min = 72;
            int max = HEIGHT - GROUND_HEIGHT - PIPE_GAP - 72;
            return min + random.nextInt(max - min + 1);
        }

        private void moveBird(int amount) {
            if (state == GameState.PLAYING) {
                birdX = Math.max(50, Math.min(WIDTH - 120, birdX + amount));
            }
        }

        private void updateGame(ActionEvent event) {
            if (state == GameState.PLAYING) {
                birdVelocity = Math.min(10, birdVelocity + GRAVITY);
                birdY += birdVelocity;

                for (Pipe pipe : pipes) {
                    pipe.x -= PIPE_SPEED;
                    if (!pipe.scored && pipe.x + PIPE_WIDTH < birdX) {
                        pipe.scored = true;
                        score++;
                    }
                }

                Iterator<Pipe> iterator = pipes.iterator();
                while (iterator.hasNext()) {
                    if (iterator.next().x + PIPE_WIDTH < 0) {
                        iterator.remove();
                    }
                }
                if (pipes.isEmpty() || pipes.get(pipes.size() - 1).x < WIDTH - PIPE_SPACING) {
                    pipes.add(new Pipe(WIDTH + 10, randomGapTop()));
                }
                checkCollisions();
            }
            repaint();
        }

        private void checkCollisions() {
            if (birdY < 0 || birdY + BIRD_SIZE > HEIGHT - GROUND_HEIGHT) {
                state = GameState.GAME_OVER;
                return;
            }

            Ellipse2D.Double bird = new Ellipse2D.Double(
                    birdX + 3, birdY + 3, BIRD_SIZE - 6, BIRD_SIZE - 6);
            for (Pipe pipe : pipes) {
                java.awt.geom.Rectangle2D.Double topPipe =
                        new java.awt.geom.Rectangle2D.Double(pipe.x, 0, PIPE_WIDTH, pipe.gapTop);
                double lowerY = pipe.gapTop + PIPE_GAP;
                java.awt.geom.Rectangle2D.Double bottomPipe = new java.awt.geom.Rectangle2D.Double(
                        pipe.x, lowerY, PIPE_WIDTH, HEIGHT - GROUND_HEIGHT - lowerY);
                if (bird.intersects(topPipe) || bird.intersects(bottomPipe)) {
                    state = GameState.GAME_OVER;
                    return;
                }
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            drawBackground(g);
            drawPipes(g);
            drawGround(g);
            drawBird(g);
            drawScore(g);
            if (state != GameState.PLAYING) {
                drawMessage(g);
            }
            g.dispose();
        }

        private void drawBackground(Graphics2D g) {
            g.setPaint(new GradientPaint(0, 0, new Color(90, 196, 238),
                    0, HEIGHT, new Color(207, 240, 246)));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            g.setColor(new Color(255, 255, 255, 160));
            drawCloud(g, 84, 105);
            drawCloud(g, 510, 154);
            drawCloud(g, 660, 82);
            g.setColor(new Color(116, 202, 139, 120));
            g.fillOval(-90, 350, 370, 180);
            g.fillOval(210, 365, 430, 180);
            g.fillOval(550, 345, 360, 190);
        }

        private void drawCloud(Graphics2D g, int x, int y) {
            g.fillOval(x, y + 12, 72, 30);
            g.fillOval(x + 18, y, 42, 44);
            g.fillOval(x + 42, y + 8, 48, 34);
        }

        private void drawPipes(Graphics2D g) {
            for (Pipe pipe : pipes) {
                int gapBottom = (int) (pipe.gapTop + PIPE_GAP);
                drawPipe(g, (int) pipe.x, 0, PIPE_WIDTH, (int) pipe.gapTop, true);
                drawPipe(g, (int) pipe.x, gapBottom, PIPE_WIDTH,
                        HEIGHT - GROUND_HEIGHT - gapBottom, false);
            }
        }

        private void drawPipe(Graphics2D g, int x, int y, int width, int height, boolean top) {
            if (height <= 0) {
                return;
            }
            g.setPaint(new GradientPaint(x, y, new Color(69, 190, 79),
                    x + width, y, new Color(34, 139, 58)));
            g.fillRect(x, y, width, height);
            g.setColor(new Color(27, 111, 47));
            g.setStroke(new BasicStroke(3));
            g.drawRect(x + 1, y + 1, width - 2, height - 2);
            g.setColor(new Color(139, 225, 118, 190));
            g.fillRect(x + 10, y, 9, height);
            int capHeight = 24;
            int capY = top ? y + height - capHeight : y;
            g.setPaint(new GradientPaint(x - 5, capY, new Color(91, 207, 91),
                    x + width, capY, new Color(39, 151, 59)));
            g.fillRoundRect(x - 5, capY, width + 10, capHeight, 7, 7);
            g.setColor(new Color(27, 111, 47));
            g.drawRoundRect(x - 5, capY, width + 10, capHeight, 7, 7);
        }

        private void drawGround(Graphics2D g) {
            int groundY = HEIGHT - GROUND_HEIGHT;
            g.setColor(new Color(218, 190, 112));
            g.fillRect(0, groundY, WIDTH, GROUND_HEIGHT);
            g.setColor(new Color(105, 190, 75));
            g.fillRect(0, groundY, WIDTH, 15);
            g.setColor(new Color(76, 158, 61));
            g.fillRect(0, groundY + 13, WIDTH, 5);
            g.setColor(new Color(192, 162, 94));
            for (int x = -20; x < WIDTH; x += 38) {
                g.drawLine(x, groundY + 29, x + 17, groundY + 29);
                g.drawLine(x + 12, groundY + 48, x + 29, groundY + 48);
            }
        }

        private void drawBird(Graphics2D g) {
            int x = (int) birdX;
            int y = (int) birdY;
            g.setColor(new Color(225, 143, 36, 90));
            g.fillOval(x - 4, y + 5, BIRD_SIZE + 9, BIRD_SIZE - 2);
            g.setColor(new Color(255, 199, 49));
            g.fillOval(x, y, BIRD_SIZE, BIRD_SIZE);
            g.setColor(new Color(226, 151, 25));
            g.setStroke(new BasicStroke(2));
            g.drawOval(x, y, BIRD_SIZE, BIRD_SIZE);
            g.setColor(new Color(247, 158, 44));
            g.fillOval(x + 3, y + 17, 18, 12);
            g.setColor(Color.WHITE);
            g.fillOval(x + 19, y + 6, 12, 14);
            g.setColor(new Color(41, 46, 52));
            g.fillOval(x + 25, y + 10, 5, 6);
            g.setColor(new Color(255, 137, 40));
            g.fillRoundRect(x + 26, y + 19, 17, 8, 5, 5);
            g.setColor(new Color(224, 97, 30));
            g.drawRoundRect(x + 26, y + 19, 17, 8, 5, 5);
        }

        private void drawScore(Graphics2D g) {
            String text = Integer.toString(score);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 44));
            FontMetrics metrics = g.getFontMetrics();
            int x = (WIDTH - metrics.stringWidth(text)) / 2;
            g.setColor(new Color(40, 54, 59, 90));
            g.drawString(text, x + 2, 67);
            g.setColor(Color.WHITE);
            g.drawString(text, x, 64);
        }

        private void drawMessage(Graphics2D g) {
            String title = state == GameState.READY ? "FLAPPY BIRD" : "GAME OVER";
            String subtitle = state == GameState.READY
                    ? "Press UP or SPACE to fly"
                    : "Press ENTER or R to play again";
            int boxWidth = 390;
            int boxHeight = 132;
            int boxX = (WIDTH - boxWidth) / 2;
            int boxY = 205;
            g.setColor(new Color(28, 48, 54, 175));
            g.fill(new RoundRectangle2D.Double(boxX, boxY, boxWidth, boxHeight, 22, 22));
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
            drawCentered(g, title, WIDTH / 2, boxY + 49);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 17));
            drawCentered(g, subtitle, WIDTH / 2, boxY + 88);
            if (state == GameState.READY) {
                g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
                drawCentered(g, "LEFT / RIGHT to steer  -  DOWN to dive",
                        WIDTH / 2, boxY + 113);
            }
        }

        private void drawCentered(Graphics2D g, String text, int centerX, int baselineY) {
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(text, centerX - metrics.stringWidth(text) / 2, baselineY);
        }
    }

    private static final class Pipe {
        double x;
        final double gapTop;
        boolean scored;

        Pipe(double x, double gapTop) {
            this.x = x;
            this.gapTop = gapTop;
        }
    }

    private enum GameState {
        READY,
        PLAYING,
        GAME_OVER
    }
}
