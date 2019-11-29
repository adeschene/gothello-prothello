import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Gothello game client for Gthd server.
 *
 * @author Bart Massey
 * @version $Revision: 2.1 $
 */
public class GthClient {
    /**
     * Player is nobody.
     */
    public final static int WHO_NONE = 0;
    /**
     * Player is white.
     */
    public final static int WHO_WHITE = 1;
    /**
     * Player is black.
     */
    public final static int WHO_BLACK = 2;
    /**
     * Player is undefined.
     */
    public final static int WHO_OTHER = 3;

    /**
     * Game will continue.
     */
    public final static int STATE_CONTINUE = 0;
    /**
     * Game over.
     */
    public final static int STATE_DONE = 1;

    /**
     * Which side this client is playing.
     */
    public int who = WHO_NONE;
    /**
     * If done, player which won.
     */
    public int winner = WHO_NONE;
    /**
     * Move returned by get_move() method as side-effect.
     */
    public Move move;

    /**
     * True if playing under time controls.
     */
    public boolean time_controls = false;
    /**
     * Number of seconds white player has at start of game,
     * if playing under time controls.
     */
    public int white_time_control;
    /**
     * Number of seconds black player has at start of game,
     * if playing under time controls.
     */
    public int black_time_control;
    /**
     * Number of seconds the client has currently remaining,
     * if playing under time controls.
     */
    public int my_time;
    /**
     * Number of seconds the client's opponent has currently remaining,
     * if playing under time controls.
     */
    public int opp_time;

    private final static String client_version = "0.9";
    private Socket sock = null;
    private BufferedReader fsock_in = null;
    private PrintStream fsock_out = null;
    private final static int server_base = 29068;

    private String msg_txt;
    private int msg_code;
    private int serial = 0;

    private void get_msg()
      throws IOException {
	String buf = fsock_in.readLine();
	if (buf == null)
	    throw new IOException("read failed");
	int len = buf.length();
	if (len < 4)
	    throw new IOException("short read");
	if (!Character.isDigit(buf.charAt(0)) ||
	    !Character.isDigit(buf.charAt(1)) ||
	    !Character.isDigit(buf.charAt(2)) ||
	    buf.charAt(3) != ' ')
	    throw new IOException("ill-formatted response code");
	msg_txt = buf.substring(4);
	msg_code = Integer.parseInt(buf.substring(0, 3));
    }

    private int opponent(int w) {
	if (w != WHO_WHITE && w != WHO_BLACK)
	    throw new Error("internal error: funny who");
	if (w == WHO_WHITE)
	    return WHO_BLACK;
	return WHO_WHITE;
    }

    private Move parse_move()
      throws IOException {
	Move m;
	try {
	    StringTokenizer toks = new StringTokenizer(msg_txt);

	    int nserial = Integer.parseInt(toks.nextToken());
	    if (serial != nserial)
		throw new IOException("synchronization lost: expected " +
				  serial +
				  " got " +
				  nserial);
	    switch(msg_code) {
	    case 312:
	    case 314:
	    case 316:
	    case 318:
	    case 323:
	    case 324:
	    case 326:
		String ellipses = toks.nextToken();
		if(!ellipses.equals("..."))
		    throw new IOException("expected ellipsis, got " + ellipses);
	    }
	    String ms = toks.nextToken();
	    m = new Move(ms);
	    switch (msg_code) {
	    case 313:
	    case 314:
	    case 317:
	    case 318:
		int tc = Integer.parseInt(toks.nextToken());
		int whose_move = WHO_BLACK;
		if (msg_code == 314 || msg_code == 318)
		    whose_move = WHO_WHITE;
		if (whose_move == who)
		    my_time = tc;
		else
		    opp_time = tc;
	    }
	} catch(NoSuchElementException e) {
	    throw new IOException("missing argument in opponent move");
	}
	return m;
    }

    private void get_time_controls()
      throws IOException
    {
	int i, j;
	for (i = 0; i < msg_txt.length(); i++)
	    if (Character.isDigit(msg_txt.charAt(i)))
		break;
	if (i >= msg_txt.length())
	    throw new IOException("cannot find time controls in message text");
	j = i;
	while(Character.isDigit(msg_txt.charAt(j)))
	    j++;
	white_time_control = Integer.parseInt(msg_txt.substring(i, j));
	i = j;
	while(!Character.isDigit(msg_txt.charAt(i)))
	    i++;
	j = i;
	while(Character.isDigit(msg_txt.charAt(j)))
	    j++;
	black_time_control = Integer.parseInt(msg_txt.substring(i, j));
    }

    private int get_time()
      throws IOException
    {
	int i, j;
	for (i = 0; i < msg_txt.length(); i++)
	    if (Character.isDigit(msg_txt.charAt(i)))
		break;
	if (i >= msg_txt.length())
	    throw new IOException("cannot find time in message text");
	j = i;
	while(Character.isDigit(msg_txt.charAt(j)))
	    j++;
	return Integer.parseInt(msg_txt.substring(i, j));
    }

    private void close()
      throws IOException {
	if (sock == null)
	    return;
	try {
	    fsock_out.close();
	    fsock_in.close();
	    sock.close();
	} finally {
	    fsock_out = null;
	    fsock_in = null;
	    sock = null;
	}
    }
    
    private String zeropad(int n) {
	if (n > 99)
	    return "" + n;
	if (n > 9)
	    return "0" + n;
	return "00" + n;
    }
    
    private void flushout()
      throws IOException {
	fsock_out.print("\r");
	fsock_out.flush();
    }

    /**
     * Construct a game client, connected to the specified
     * server and ready to play.
     * @param side Should be either WHO_WHITE or WHO_BLACK.
     *             Side the client will play.
     * @param host Hostname of the server.
     * @param server Server number of server on host.
     * @throws IOException Unable to connect to specified server
     *                     as specified side.
     */
    public GthClient(int side, String host, int server)
      throws IOException {
	InetAddress addr = InetAddress.getByName(host);
	sock = new Socket(addr, server_base + server);
	InputStream instream = sock.getInputStream();
	fsock_in = new BufferedReader(new InputStreamReader(instream));
	OutputStream outstream = sock.getOutputStream();
	fsock_out = new PrintStream(new BufferedOutputStream(outstream));

	get_msg();
	if (msg_code != 0)
	    throw new IOException("illegal greeeting " + zeropad(msg_code));
	fsock_out.print(client_version + " player");
	if (side == WHO_WHITE)
	    fsock_out.print(" white");
	else
	    fsock_out.print(" black");
	flushout();
	get_msg();
	if (msg_code != 100 && msg_code != 101)
	    throw new IOException("side failure " + zeropad(msg_code));
	if (msg_code == 101) {
	    time_controls = true;
	    get_time_controls();
	    if (side == WHO_WHITE) {
		my_time = white_time_control;
		opp_time = black_time_control;
	    } else {
		opp_time = black_time_control;
		my_time = white_time_control;
	    }
	}
	get_msg();
	if ((msg_code != 351 && side == WHO_WHITE) ||
	    (msg_code != 352 && side == WHO_BLACK))
	    throw new IOException("side failure " + zeropad(msg_code));
	who = side;
    }

    /**
     * Make a move on the server.  The server must be expecting
     * a move (that is, it must be this client's turn), and
     * the move must be legal.
     * @param m What move to make.
     * @returns Will be either STATE_CONTINUE or STATE_DONE.
     * @throws IOException Move is illegal or communication failed.
     */
    public int make_move(Move m)
      throws IOException {
	String ellipses = "";
  
	if (who == WHO_NONE)
	    throw new IOException("not initialized");
	if (winner != WHO_NONE)
	    throw new IOException("game over");
	if (who == WHO_BLACK)
	    serial++;
	if (who == WHO_WHITE)
	    ellipses = " ...";
	fsock_out.print(serial + ellipses + " " + m.name());
	flushout();
	get_msg();
	switch(msg_code) {
	case 201:
	    winner = who;
	    break;
	case 202:
	    winner = opponent(who);
	    break;
	case 203:
	    winner = WHO_OTHER;
	    break;
	}
	if (winner != WHO_NONE) {
	    close();
	    return STATE_DONE;
	}
	if (msg_code != 200 && msg_code != 207)
	    throw new IOException("bad result code " + zeropad(msg_code));
	if (msg_code == 207)
	  my_time = get_time();
	get_msg();
	if (!(msg_code >= 311 || msg_code <= 318))
	    throw new IOException("bad status code " + zeropad(msg_code));
	return STATE_CONTINUE;
    }

    /**
     * Get a move from the server.  The server must be expecting
     * a move from the opponent (that is, it must be this client's
     * opponent's turn).  The <tt>move</tt> field will indicate
     * the returned move.
     * @returns Will be either STATE_CONTINUE or STATE_DONE.
     * @throws IOException Move is illegal or communication failed.
     */
    public int get_move()
      throws IOException {
	if (who == WHO_NONE)
	    throw new IOException("not initialized");
	if (winner != WHO_NONE)
	    throw new IOException("game over");
	if (who == WHO_WHITE)
	    serial++;
	get_msg();
	if (!(msg_code >= 311 && msg_code <= 326 ||
	    msg_code == 361 || msg_code == 362))
	    throw new IOException("bad status code " + zeropad(msg_code));
	if ((who == WHO_WHITE &&
	     (msg_code == 312 || msg_code == 314 ||
	      msg_code == 316 || msg_code == 318 ||
	      msg_code == 323 ||
	      msg_code == 324 || msg_code == 326)) ||
	    (who == WHO_BLACK &&
	     (msg_code == 311 || msg_code == 313 ||
      	      msg_code == 315 || msg_code == 317 ||
	      msg_code == 321 ||
	      msg_code == 322 || msg_code == 325)))
	    throw new IOException("status code " +
				  zeropad(msg_code) +
				  " from wrong side");
	switch(who) {
	case WHO_WHITE:
	    switch(msg_code) {
	    case 311:
	    case 313:
	    case 315:
	    case 317:
		move = parse_move();
		return STATE_CONTINUE;
	    case 321:
		move = parse_move();
		winner = WHO_BLACK;
		return STATE_DONE;
	    case 361:
		winner = WHO_BLACK;
		return STATE_DONE;
	    case 322:
		move = parse_move();
		winner = WHO_WHITE;
		return STATE_DONE;
	    case 362:
		winner = WHO_WHITE;
		return STATE_DONE;
	    case 325:
		move = parse_move();
		winner = WHO_OTHER;
		return STATE_DONE;
	    }
	    break;
	case WHO_BLACK:
	    switch(msg_code) {
	    case 312:
	    case 314:
	    case 316:
	    case 318:
		move = parse_move();
		return STATE_CONTINUE;
	    case 323:
		move = parse_move();
		winner = WHO_WHITE;
		return STATE_DONE;
	    case 362:
		winner = WHO_WHITE;
		return STATE_DONE;
	    case 324:
		move = parse_move();
		winner = WHO_BLACK;
		return STATE_DONE;
	    case 361:
		winner = WHO_BLACK;
		return STATE_DONE;
	    case 326:
		move = parse_move();
		winner = WHO_OTHER;
		return STATE_DONE;
	    }
	    break;
	}
	throw new IOException("unknown status code " + zeropad(msg_code));
    }

    /**
     * Internal method.
     */
    protected void finalize()
	throws Throwable {
	close();
    }

}
