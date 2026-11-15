package group18.plugins;
// Sources:
//  https://www.bluej.org/doc/extensionsAPI2/bluej/extensions2/event/InvocationFinishedEvent.html
//  https://www.bluej.org/doc/extensionsAPI2/bluej/extensions2/event/InvocationFinishedEvent.EventType.html


import bluej.extensions2.event.InvocationFinishedEvent;
import bluej.extensions2.event.InvocationFinishedListener;
import group18.listener.BlueJEventRecorder;

import java.util.Objects;

// Adapter: a finished method call or constructor call in BlueJ becomes an ExecutionEvent. //
class InvocationEventAdapter implements InvocationFinishedListener {

    
    private static final long CONSOLE_SETTLE_MS = 400;

    private final BlueJEventRecorder recorder;

    InvocationEventAdapter(BlueJEventRecorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public void invocationFinished(InvocationFinishedEvent event) {
        String className = event.getClassName();
        String exit = event.getEventType() == null ? null : event.getEventType().name();
        String result = Objects.toString(event.getResult(), null);
        FxThread.runAfter(CONSOLE_SETTLE_MS,
                () -> recorder.executionFinished(className, exit, result, BlueJTerminalReader.read()));
    }
}
