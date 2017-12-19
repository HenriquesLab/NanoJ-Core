package nanoj.core.java.tools;

import java.awt.*;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 01/06/15
 * Time: 09:53
 */
public class Colors {

    private static int counter = 0;
    private static int selected = 0;

    public static Color[] COLORS =
            new Color[] {
                    Color.RED, Color.GREEN, Color.BLUE, Color.MAGENTA,
                    Color.CYAN, Color.ORANGE, Color.YELLOW, Color.PINK,};
    public static String[] COLORNAMES =
            new String[] {"red", "green", "blue", "magenta", "cyan", "orange", "yellow", "pink"};

    public static Color getNextColor() {
        Color c = COLORS[counter];
        selected = counter;
        if (counter == COLORS.length-1) counter = 0;
        else counter++;
        return c;
    }

    public static String getSelectedColorName() {
        return COLORNAMES[selected];
    }
}
