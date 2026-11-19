package group18.bluej;

// Source that helped: https://www.w3schools.com/java/java_threads.asp

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.util.Duration;

final class FxThread {

    private FxThread() {
    }


    static void run(Runnable task) {
        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }
    }

    static void runAfter(long millis, Runnable task) {
        run(() -> {
            PauseTransition pause = new PauseTransition(Duration.millis(millis));
            pause.setOnFinished(event -> task.run());
            pause.play();
        });
    }
}
