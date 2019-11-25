import java.net.*;
import java.io.*;

public class Connection {
    static final int STATE_INITIAL = 0;
    static final int STATE_SEATED = 1;
    static final int STATE_PLAYING = 2;
    static final int STATE_DONE = 3;
    int state = STATE_INITIAL;

    static final int PLAYER_WHITE = 1;
    static final int PLAYER_BLACK = 2;
    static final int OBSERVER = 3;
    int who = 0;

    private Socket socket;
    private Messages msg;

    private void observe_state() {
	if (who != OBSERVER)
	    return;
	msg.print_board();
    }

    public Connection(ServerSocket ss,
		      boolean white_ok,
		      boolean black_ok,
		      int nobservers)
      throws IOException {
	socket = ss.accept();
	msg = new Messages(socket.getInputStream(), socket.getOutputStream());
	msg.resp_greeting();
	who = msg.req_side();
	if (who == -1) {
	    socket.close();
	    throw new IOException("botched initial handshake");
	}
	if (who == PLAYER_WHITE) {
	    if (!white_ok) {
		msg.response("191 Other player holds requested side");
		socket.close();
		throw new IOException("second try for white player");
	    }
	    if (Gthd.time_controls) {
		msg.response("101 " +
			 Gthd.secs(Gthd.white_msecs) +
			 " " +
			 Gthd.secs(Gthd.black_msecs) +
			 " Request accepted with time controls (you / opp)");
		return;
	    }
	    msg.response("100 Request accepted");
	    return;
	}
	if (who == PLAYER_BLACK) {
	    if (!black_ok) {
		msg.response("191 Other player holds requested side");
		socket.close();
		throw new IOException("second try for black player");
	    }
	    if (Gthd.time_controls) {
		msg.response("101 " +
			 Gthd.secs(Gthd.black_msecs) +
			 " " +
			 Gthd.secs(Gthd.white_msecs) +
			 " Request accepted with time controls (you / opp)");
		return;
	    }
	    msg.response("100 Request accepted");
	    return;
	}
	if (who == OBSERVER) {
	    // XXX this limit is illegal
	    if (nobservers >= Gthd.max_observers) {
		msg.response("193 Cannot observe");
		socket.close();
		throw new IOException("too many observers");
	    }
	    if (Gthd.time_controls) {
		msg.response("101 " +
			 Gthd.secs(Gthd.white_msecs) +
			 " " +
			 Gthd.secs(Gthd.black_msecs) +
			 " Request accepted with time controls");
		return;
	    }
	    msg.response("100 Request accepted");
	    return;
	}
	msg.response("999 Internal error");
	socket.close();
	throw new Error("internal error: funny who");
    }

    public void start()
      throws IOException {
	switch (who) {
	case PLAYER_WHITE:
	    msg.response("351 You will play white");
	    return;
	case PLAYER_BLACK:
	    msg.response("352 You will play black");
	    return;
	case OBSERVER:
	    msg.response("353 You will observe");
	    return;
	}
	msg.response("999 internal error");
	socket.close();
	throw new Error("internal error: funny who");
    }

    public Move get_move(int serial, int to_move)
      throws IOException {
	while (true) {
	   Move m = msg.req_move(serial, to_move);
	   if (m != null)
	       return m;
	}
    }

    public void legal_move(Move m) {
	msg.response("200 Move " +
		     m.name() +
		     " accepted, continue playing");
    }

    public void legal_move_tc(Move m, int time) {
	msg.response("207 " + time + " secs remaining after move " +
		     m.name() +
		     " accepted, continue playing");
    }

    public void final_move(int to_move, int winner, Move m) {
	String result_code, result_desc;
	if (to_move == winner) {
	    result_code = "201";
	    result_desc = "You win";
	} else if(winner == Connection.OBSERVER) {
	    result_code = "203";
	    result_desc = "You draw";
	} else {
	    result_code = "202";
	    result_desc = "You lose";
	}
	msg.response(result_code +
		     " Move " +
		     m.name() +
		     " accepted, " +
		     result_desc);
    }

    public void flag_fell() {
	msg.response("202 Your time expires, and you lose");
    }

    public void illegal_move(Move m) {
	msg.response("291 Illegal move " + m.name());
    }

    public void move(int serial, int to_move, Move m) {
	String result_code, ellipses, result_desc;
	if (to_move == Connection.PLAYER_BLACK) {
	    if (m.isPass())
		result_code = "315";
	    else
		result_code = "311";
	    ellipses = "";
	    result_desc = "black";
	} else {
	    if (m.isPass())
		result_code = "316";
	    else
		result_code = "312";
	    ellipses = " ...";
	    result_desc = "white";
	}
	msg.response(result_code +
		     " " +
		     serial +
		     ellipses +
		     " " +
		     m.name() +
		     " is " + 
		     result_desc +
		     " move, game continues");
	observe_state();
    }

    public void move_tc(int serial, int to_move, Move m, int time) {
	String result_code, ellipses, result_desc;
	if (to_move == Connection.PLAYER_BLACK) {
	    if (m.isPass())
		result_code = "317";
	    else
		result_code = "313";
	    ellipses = "";
	    result_desc = "black";
	} else {
	    if (m.isPass())
		result_code = "318";
	    else
		result_code = "314";
	    ellipses = " ...";
	    result_desc = "white";
	}
	msg.response(result_code +
		     " " +
		     serial +
		     ellipses +
		     " " +
		     m.name() +
		     " " +
		     time +
		     " (secs) is " + 
		     result_desc +
		     " move, game continues");
	observe_state();
    }

    public void stop(int serial, int to_move, int winner, Move m) {
	String result_code, ellipses, result_desc;
	if (to_move == Connection.PLAYER_BLACK) {
	    ellipses = "";
	    result_desc = "black ";
	    switch (winner) {
	    case Connection.PLAYER_BLACK:
		result_code = "321";
		result_desc = result_desc + "wins";
		break;
	    case Connection.PLAYER_WHITE:
		result_code = "322";
		result_desc = result_desc + "loses";
		break;
	    case Connection.OBSERVER:
		result_code = "325";
		result_desc = result_desc + "draws";
		break;
	    default:
		throw new Error("internal error: funny winner");
	    }
	} else {
	    ellipses = " ...";
	    result_desc = "white ";
	    switch (winner) {
	    case Connection.PLAYER_WHITE:
		result_code = "323";
		result_desc = result_desc + "wins";
		break;
	    case Connection.PLAYER_BLACK:
		result_code = "324";
		result_desc = result_desc + "loses";
		break;
	    case Connection.OBSERVER:
		result_code = "326";
		result_desc = result_desc + "draws";
		break;
	    default:
		throw new Error("internal error: funny winner");
	    }
	}
	msg.response(result_code +
		     " " +
		     serial +
		     ellipses +
		     " " +
		     m.name() +
		     " and " + 
		     result_desc);
	observe_state();
	try {
	    socket.close();
	} catch(IOException e) {
	    // do nothing
	}
    }

    public void stop_flag(int serial, int winner) {
	if (winner == Connection.PLAYER_BLACK) {
	    msg.response("361 Black wins by White time expiring");
	} else {
	    msg.response("362 White wins by Black time expiring");
	}
	observe_state();
	try {
	    socket.close();
	} catch(IOException e) {
	    // do nothing
	}
    }
}
