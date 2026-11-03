# copilot-starter
A template repository for building an application using Copilot.

## Snake (Java/Swing)

Single-player snake. Steer with the arrow keys; the score is shown in the top-right. The snake speeds up every 5 apples eaten. After game over, press Enter to restart.

```sh
javac -d target src/*.java       # build
java -cp target SnakeGame        # play
java -cp target GameModelTest    # run game-logic tests
```
