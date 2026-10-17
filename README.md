# copilot-starter

A single-player Snake game built with Java Swing.

## Requirements

- Java Development Kit (JDK) 8 or later

## Build and run

From the repository root, compile the source into `target/` and start the game:

```sh
mkdir -p target
javac -d target src/SnakeGame.java
java -cp target SnakeGame
```

## Controls and rules

- Use the arrow keys to steer the snake.
- Eat apples to increase your score. The snake speeds up every five apples.
- Hitting the wall or the snake ends the game.
- Press **R** after game over to start a new game.
