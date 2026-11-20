package group18.model;

/**
 * The four categories of telemetry recorded by the plugin.
 *
 * <p>The constant name is written to the JSON export as the {@code eventType} of each event, so a reader of the
 * file can tell what kind of event an entry is without knowing any Java class names.</p>
 */
public enum EventType {

    /** A project, class or method was created, opened, modified, closed or deleted. */
    LIFECYCLE,

    /** Typing, pasting, deleting and idle time measured over one sampling interval. */
    EDITOR_INTERACTION,

    /** One compilation requested by the user, with any compiler errors it produced. */
    COMPILATION,

    /** One method or constructor call made from BlueJ, with its outcome and console output. */
    EXECUTION
}
