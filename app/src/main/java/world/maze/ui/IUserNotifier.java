package world.maze.ui;

/**
 * Created by Greg Stein on 11/3/2018.
 */

public interface IUserNotifier {
    void displayError(String s, boolean exit);

    void displayTragicError(String title, String message);
}
