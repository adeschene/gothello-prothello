import java.io.*;
import java.net.*;

public class Gthd {
    public static final String name = "Gothello";
    public static final String version = "0.9";
    public static final int server_base = 29068;
    public static final int max_servers = 10;
    public static final int max_observers = 10;

    public static Board board;

    static Connection white_conn = null;
    static Connection black_conn = null;
    static Connection observer_conn[] = new Connection[max_observers];
    static int nobservers = 0;

    public static boolean time_controls = false;
    public static long white_msecs = 0;
    public static long black_msecs = 0;
	
    private static long timestamp = 0;
	
    private static void start_clock() {
	timestamp = System.currentTimeMillis();
    }

    private static long elapsed_clock() {
	return System.currentTimeMillis() - timestamp;
    }
    
    public static int secs(long msecs) {
	return (int)(msecs / 1000);
    }

    public static void main(String args[])
      throws IOException {
	// ready the server
	if (args.length < 1 || args.length > 3 )
	    throw new IllegalArgumentException(
	      "usage: Gthd server_number [secs [secs_black]]");
	int snum = Integer.parseInt(args[0]);
	if (!(snum >= 0 && snum <= max_servers - 1))
	    throw new
	     IllegalArgumentException("server number should be in 0.." +
				      (max_servers - 1));
	if (args.length > 1) {
	    white_msecs = Integer.parseInt(args[1]) * 1000;
	    black_msecs = white_msecs;
	    time_controls = true;
	}
	if (args.length > 2)
	    black_msecs = Integer.parseInt(args[2]) * 1000;
	ServerSocket s = new ServerSocket(server_base + snum);
	// marshal the clients
	while (white_conn == null || black_conn == null) {
	    Connection c;
	    try {
		c = new Connection(s,
				   white_conn == null,
				   black_conn == null,
				   nobservers);
	    } catch (IOException e) {
		System.out.println("Failed connection attempt");
		continue;
	    }
	    switch (c.who) {
	    case Connection.PLAYER_WHITE:
		System.out.println("White player connected");
		white_conn = c;
		break;
	    case Connection.PLAYER_BLACK:
		System.out.println("Black player connected");
		black_conn = c;
	        break;
	    case Connection.OBSERVER:
		System.out.println("Observer " +
				   nobservers +
				   " connected");
		observer_conn[nobservers++] = c;
		break;
	    default:
		throw new Error("internal error: funny who");
	    }
	}
	s.close();
	// tell the clients we're set
	for (int i = 0; i < nobservers; i++)
	    observer_conn[i].start();
	white_conn.start();
	black_conn.start();
	// play the game
	board = new Board();
	while (true) {
	    System.out.println();
	    board.print(System.out);
	    Connection move_conn;
	    if (board.to_move == Connection.PLAYER_WHITE)
		move_conn = white_conn;
	    else
		move_conn = black_conn;
	    int to_move = board.to_move;
	    int serial = board.serial;
	    start_clock();
	    Move m = move_conn.get_move(serial, to_move);
	    int winner = 0;
	    long move_time = 0;
	    if (time_controls) {
		boolean flag_fell = false;
		if (to_move == Connection.PLAYER_WHITE) {
		    white_msecs -= elapsed_clock();
		    move_time = white_msecs;
		    if (white_msecs < 0) {
			flag_fell = true;
			winner = Connection.PLAYER_BLACK;
		    }
		} else {
		    black_msecs -= elapsed_clock();
		    move_time = black_msecs;
		    if (black_msecs < 0) {
			flag_fell = true;
			winner = Connection.PLAYER_WHITE;
		    }
		}
		if (flag_fell) {
		    if (winner == Connection.PLAYER_WHITE)
			System.out.print("White");
		    else if (winner == Connection.PLAYER_BLACK)
			System.out.print("Black");
		    else
			throw new Error("bogus winner");
		    System.out.println(" player wins on time.");
		    move_conn.flag_fell();
		    board.game_state = Board.GAME_OVER;
		    white_conn.stop_flag(serial, winner);
		    black_conn.stop_flag(serial, winner);
		    for (int i = 0; i < nobservers; i++)
			observer_conn[i].stop_flag(serial, winner);
		    return;
		}
	    }
	    int status = board.try_move(m);
	    switch(status) {
	    case Board.ILLEGAL_MOVE:
		move_conn.illegal_move(m);
		continue;
	    case Board.GAME_OVER:
		board.print(System.out);
		winner = board.referee();
		if (winner == Board.OBSERVER) {
		    System.out.println("Game drawn.");
		} else {
		    switch(winner) {
		    case Board.PLAYER_BLACK:
			System.out.print("Black");
			break;
		    case Board.PLAYER_WHITE:
			System.out.print("White");
			break;
		    }
		    System.out.println(" wins.");
		}
		move_conn.final_move(to_move, winner, m);
		white_conn.stop(serial, to_move, winner, m);
		black_conn.stop(serial, to_move, winner, m);
		for (int i = 0; i < nobservers; i++)
		    observer_conn[i].stop(serial, to_move, winner, m);
		return;
	    case Board.CONTINUE:
		if (time_controls) {
		    int ms = secs(move_time);
		    move_conn.legal_move_tc(m, ms);
		    white_conn.move_tc(serial, to_move, m, ms);
		    black_conn.move_tc(serial, to_move, m, ms);
		    for (int i = 0; i < nobservers; i++)
			observer_conn[i].move_tc(serial, to_move, m, ms);
		} else {
		    move_conn.legal_move(m);
		    white_conn.move(serial, to_move, m);
		    black_conn.move(serial, to_move, m);
		    for (int i = 0; i < nobservers; i++)
			observer_conn[i].move(serial, to_move, m);
		}
		continue;
	    }
	    throw new Error("internal error: funny who");
	}
    }
}
