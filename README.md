# copilot-starter
A template repository for building an application using Copilot.

## Snake

A single-player Snake game built with Java and Swing.

- Arrow keys steer the snake; the score is shown in the top-right.
- The snake speeds up every 5 apples eaten.
- Hitting a wall or yourself ends the game; press Enter to restart.

```sh
javac -d target src/snake/*.java
java -cp target snake.SnakeGame
```
