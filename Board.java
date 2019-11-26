import java.io.*;
import java.util.Vector;

public class Board {
    public static final int GAME_OVER = 1;
    public static final int CONTINUE = 0;
    public static final int ILLEGAL_MOVE = -1;
    static final int PLAYER_WHITE = 1;
    static final int PLAYER_BLACK = 2;
    static final int OBSERVER = 3;
    public int game_state = CONTINUE;

    public int to_move = PLAYER_BLACK;
    public int serial = 1;

    // rules-specific game state 
    Move previous_move = null;
    int square[][] = new int[5][5];
    static final int WHITE_CHECKER = PLAYER_WHITE;
    static final int BLACK_CHECKER = PLAYER_BLACK;

    public Board() {
	for (int i = 0; i < 5; i++)
	    for (int j = 0; j < 5; j++)
		square[i][j] = 0;
    }

    public Board(Board b) {
	previous_move = b.previous_move;
	for (int i = 0; i < 5; i++)
	    for (int j = 0; j < 5; j++)
		square[i][j] = b.square[i][j];

	to_move = b.to_move;
	game_state = b.game_state;
	serial = b.serial;
    }

    public void print(PrintStream s) {
	if (Gthd.time_controls)
	    s.print("381 ");
	else
	    s.print("380 ");
	s.print(serial);
	s.print(" ");
	if (Gthd.time_controls) {
	    s.print(Gthd.secs(Gthd.black_msecs));
	    s.print(" ");
	    s.print(Gthd.secs(Gthd.white_msecs));
	    s.print(" ");
	}
	if (game_state == GAME_OVER)
	    s.print("*");
	else if (to_move == PLAYER_WHITE)
	    s.print("w");
	else
	    s.print("b");
	s.print("\r\n");
	s.flush();
	s.print("382\r\n");
	for (int j = 4; j >= 0; --j) {
	    for (int i = 0; i < 5; i++)
		switch (square[i][j]) {
		case 0:  s.print("."); break;
		case BLACK_CHECKER:  s.print("b"); break;
		case WHITE_CHECKER:  s.print("w"); break;
		default: s.print("?");
		}
	    s.print("\r\n");
	}
	s.flush();
    }

    static final int opponent(int player) {
	if (player == PLAYER_WHITE)
	    return PLAYER_BLACK;
	if (player == PLAYER_BLACK)
	    return PLAYER_WHITE;
	throw new Error("internal error: bad player");
    }

    // XXX see declaration of BLACK_CHECKER, WHITE_CHECKER
    static final int checker_of(int player) {
	return player;
    }
    static final int owner_of(int checker) {
	return checker;
    }

    static final boolean[][] scratch_board() {
	boolean[][] scratch = new boolean[5][5];
	for (int i = 0; i < 5; i++)
	    for (int j = 0; j < 5; j++)
		scratch[i][j] = false;
	return scratch;
    }

    void flood(boolean[][] scratch, int color, int x, int y) {
	/* off board */
	if (!(x >= 0 && x <= 4 && y >= 0 && y <= 4))
	    return;
	/* already done */
	if (scratch[x][y])
	    return;
	/* wrong color */
	if (square[x][y] != color)
	    return;
	/* ok */
	scratch[x][y] = true;
	flood(scratch, color, x - 1, y);
	flood(scratch, color, x + 1, y);
	flood(scratch, color, x, y - 1);
	flood(scratch, color, x, y + 1);
    }

    static final boolean group_border(boolean[][] scratch, int x, int y) {
	if (scratch[x][y])
	    return false;
	if (x > 0 && scratch[x - 1][y])
	    return true;
	if (x < 4 && scratch[x + 1][y])
	    return true;
	if (y > 0 && scratch[x][y - 1])
	    return true;
	if (y < 4 && scratch[x][y + 1])
	    return true;
	return false;
    }
    
    int liberties(int x, int y) {
	boolean[][] scratch = scratch_board();
	flood(scratch, square[x][y], x, y);
	int n = 0;
	for (int i = 0; i < 5; i++)
	    for (int j = 0; j < 5; j++)
		if (square[i][j] == 0 && group_border(scratch, i, j))
		    n++;
	return n;
    }
    
    boolean move_ok(Move m) {
	if (m.isPass())
	    return true;
	if (square[m.x][m.y] != 0)
	    return false;
	square[m.x][m.y] = to_move;
	int n = liberties(m.x, m.y);
	square[m.x][m.y] = 0;
	if (n == 0)
	    return false;
	return true;
    }

    Vector<Move> genMoves() {
	Vector<Move> result = new Vector<Move>();
	for (int i = 0; i < 5; i++)
	    for (int j = 0; j < 5; j++)
		if (square[i][j] == 0) {
		    Move m = new Move(i, j);
		    if (move_ok(m))
			result.add(m);
		}
	return result;
    }

    private boolean has_moves() {
	Vector m = genMoves();
	return m.size() > 0;
    }

    void capture(int x, int y) {
	if (liberties(x, y) > 0)
	    return;
	/* XXX this duplicates a lot of work, but
	   who cares?  This is just for the referee */
	boolean[][] scratch = scratch_board();
	flood(scratch, square[x][y], x, y);
	for (int i = 0; i < 5; i++)
	    for (int j = 0; j < 5; j++)
		if (scratch[i][j])
		    square[i][j] = to_move;
    }

    void do_captures(Move m) {
	if (m.x > 0 && square[m.x - 1][m.y] == opponent(to_move))
	    capture(m.x - 1, m.y);
	if (m.x < 4 && square[m.x + 1][m.y] == opponent(to_move))
	    capture(m.x + 1, m.y);
	if (m.y > 0 && square[m.x][m.y - 1] == opponent(to_move))
	    capture(m.x, m.y - 1);
	if (m.y < 4 && square[m.x][m.y + 1] == opponent(to_move))
	    capture(m.x, m.y + 1);
    }
    
    public void makeMove(Move m) {
	previous_move = m;
	if (m.isPass())
	    return;
	square[m.x][m.y] = to_move;
	do_captures(m);
    }

    private static final boolean debug_try_move = false;

    public int try_move(Move m) {
	if (debug_try_move)
	    System.err.println("entering try_move()");
	if (game_state != CONTINUE) {
	    if (debug_try_move)
		System.err.println("leaving try_move(): move after game over");
	    return ILLEGAL_MOVE;
	}
	if (m.isPass() && previous_move != null && previous_move.isPass()) {
	    game_state = GAME_OVER;
	    if (debug_try_move)
		System.err.println("leaving try_move(): game over");
	    return GAME_OVER;
	}
	if (!move_ok(m)) {
	    if (debug_try_move)
		System.err.println("leaving try_move(): illegal move");
	    return ILLEGAL_MOVE;
	}
	if (debug_try_move)
	    System.err.println("move ok");

	makeMove(m);

	to_move = opponent(to_move);
	if (to_move == PLAYER_BLACK)
	    serial++;
	if (debug_try_move)
	    System.err.println("leaving try_move(): continue game");
	return CONTINUE;
    }

    public int referee() {
	if (game_state != GAME_OVER)
	    throw new Error("internal error: referee unfinished game");
	int nblack = 0;
	int nwhite = 0;
	for (int i = 0; i < 5; i++)
	    for (int j = 0; j < 5; j++)
		switch (square[i][j]) {
		case BLACK_CHECKER:  nblack++; break;
		case WHITE_CHECKER:  nwhite++; break;
		}
	if (nblack > nwhite)
	    return PLAYER_BLACK;
	if (nwhite > nblack)
	    return PLAYER_WHITE;
	return OBSERVER;
    }
}
