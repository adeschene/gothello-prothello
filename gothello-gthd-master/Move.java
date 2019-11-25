/**
 * Move for Gthd, GthClient.
 *
 * @author Bart Massey
 * @version $Revision: 2.2 $
 */
public class Move {
    /**
     * Square coordinates in the range 0-4, 0-4,
     * where the first coordinate is the column (x) value,
     * and the second is the row (y) value.
     * XXX Coord -1, -1 is a pass.
     */
    public int x, y;

    /**
     * Returns true iff this move is a pass.
     */
    public boolean isPass() {
	return x == -1 && y == -1;
    }

    /**
     * Create a move object from a description of the
     * form "coord" where "coord" is an algebraic
     * square number or the string "pass".
     *
     * @param desc Move description.
     */
    public Move(String desc) {
	if (desc.equals("pass")) {
	    x = -1;
	    y = -1;
	    return;
	}
	if (desc.length() != 2)
	    throw new IllegalArgumentException("bad move format");
	x = move_letter(desc.charAt(0));
	y = move_digit(desc.charAt(1));
    }

    /**
     * Create a move object from a coordinate pair in internal form.
     *
     * @param x Move coordinate.
     * @param y Move coordinate.
     */
    public Move(int x, int y) {
	this.x = x;
	this.y = y;
    }

    /**
     * Create a move object which is a pass.
     */
    public Move() {
	this.x = -1;
	this.y = -1;
    }

    private static int move_letter(char ch) {
	switch (ch) {
	case 'a': return 0;
	case 'b': return 1;
	case 'c': return 2;
	case 'd': return 3;
	case 'e': return 4;
	}
	throw new IllegalArgumentException("bad move letter");
    }

    private static int move_digit(char ch) {
	switch (ch) {
	case '1': return 0;
	case '2': return 1;
	case '3': return 2;
	case '4': return 3;
	case '5': return 4;
	}
	throw new IllegalArgumentException("bad move digit");
    }

    private static String square_name(int x, int y) {
	if (!(y >= 0 && y <= 4))
	    throw new IllegalArgumentException("bad y coordinate in square");
	switch(x + 1) {
	case 1: return "a" + (y + 1);
	case 2: return "b" + (y + 1);
	case 3: return "c" + (y + 1);
	case 4: return "d" + (y + 1);
	case 5: return "e" + (y + 1);
	}
	throw new IllegalArgumentException("bad x coordinate in square");
    }

    /**
     * Get a description of the move object.
     *
     * @return String of the form "coord-coord" where
     *         the coords are the starting and ending
     *         move coordinates.
     */
    public String name() {
	if (isPass())
	    return "pass";
	return square_name(x, y);
    }
}
