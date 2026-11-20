package group18.bluej;

import java.util.List;

/** Reads what a project's terminal window currently shows. */
interface ConsoleTextSource {

    /**
     * Reads the terminal of a project.
     *
     * @param projectName the project whose terminal to read
     * @return the full text of each terminal pane (standard output first, then standard error), or an empty list
     *         if the terminal is not open or cannot be read
     */
    List<String> readPanes(String projectName);
}
