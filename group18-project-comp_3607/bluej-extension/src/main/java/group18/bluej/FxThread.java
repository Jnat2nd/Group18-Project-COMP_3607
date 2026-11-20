package group18.bluej;

// Source that helped: https://www.w3schools.com/java/java_threads.asp

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.util.Duration;

/** Runs code on the JavaFX application thread, which BlueJ's extension API and JavaFX controls require. */
final class FxThread {

    private FxThread() {
    }

    /**
     * Runs a task on the JavaFX thread: immediately if already on it, otherwise as soon as possible.
     *
     * @param task the task
     */
    static void run(Runnable task) {
        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }
    }

    /**
     * Runs a task on the JavaFX thread after a delay, without blocking it in the meantime.
     *
     * @param millis the delay in milliseconds
     * @param task   the task
     */
    static void runAfter(long millis, Runnable task) {
        run(() -> {
            PauseTransition pause = new PauseTransition(Duration.millis(millis));
            pause.setOnFinished(event -> task.run());
            pause.play();
        });
    }
}
