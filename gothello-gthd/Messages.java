import java.io.*;

public class Messages {
    private InputStream instream;
    private OutputStream outstream;
    private BufferedInputStream reader;
    private PrintStream writer;
    
    public Messages(InputStream instream, OutputStream outstream) {
	this.instream = instream;
	this.reader = new BufferedInputStream(instream);
	this.outstream = outstream;
	this.writer = new PrintStream(new BufferedOutputStream(outstream));
    }
    
    // get next (ws separated) identifier in request string
    public String get_id(StringChars req) {
	if (req == null)
	    return null;
	char ch;
	while(req.length() > 0 && ((ch = req.charAt(0)) == ' ' || ch == '\t'))
	    req.deleteFirstChar();
	int n = req.length();
	if (n == 0)
	    return null;
	int i;
	for (i = 0; i < n; i++) {
	    ch = req.charAt(i);
	    if (ch == ' ' || ch == '\t')
		break;
	}
	String result = req.substring(0, i);
	req.deleteFirstChars(i);
	return result;
    }

    private static final boolean request_debug = false;

    public StringChars request()
      throws IOException {
	if (request_debug)
	    System.err.println("entering request()");
	// cr-terminated ASCII on instream
	StringBuffer result = new StringBuffer();
	int chi;
	boolean garbage;
	do {
	    chi = reader.read();
	    if (chi == -1)
		return null;
	    char ch = (char) chi;
	    garbage = (ch == '\r') || (ch == '\n');
	    if (request_debug && garbage)
		System.err.println("discarding garbage");
	} while (garbage);
	while(chi != -1) {
	    char ch = (char) chi;
	    if (ch == '\r')
		break;
	    result.append(ch);
	    chi = reader.read();
	}
	if (request_debug)
	    System.err.println("leaving request(): result " +
			       result);
	return new StringChars(result);
    }

    public void response(String m) {
	// crlf-terminated ASCII on outstream
	writer.print(m + "\r\n");
	writer.flush();
    }

    public void resp_greeting() {
	// not yet
	if (false && Gthd.time_controls) {
	    response("001 " +
		     Gthd.version +
		     " " +
		     Gthd.secs(Gthd.white_msecs) +
		     " " +
		     Gthd.secs(Gthd.black_msecs) +
		     " version white-secs black-secs " +
		     Gthd.name +
		     " server says hello!");
	    return;
	}
	response("000 " +
		 Gthd.version +
		 " version " +
		 Gthd.name +
		 " server says hello!");
    }

    public int req_side() {
	StringChars req;
	try {
	    req = request();
	} catch(IOException e) {
	    return -1;
	}
	String version = get_id(req);
	if (version == null || !version.equals(Gthd.version)) {
	    response("198 Illegal version number (expected " +
		     Gthd.version +
		     " got " +
		     version +
		     ")");
	    return -1;
	}
	String player = get_id(req);
	if (player != null && player.equals("observer"))
	    return Connection.OBSERVER;
	if (player == null || !player.equals("player")) {
	    response("199 Request not understood");
	    return -1;
	}
	String which_player = get_id(req);
	if (which_player != null && which_player.equals("white"))
	    return Connection.PLAYER_WHITE;
	if (which_player != null && which_player.equals("black"))
	    return Connection.PLAYER_BLACK;
        response("199 Request not understood");
	return -1;
    }

    public Move req_move(int serial, int to_move)
      throws IOException {
	StringChars req = request();
	if (req == null)
	    throw new IOException("failed read for player " +
				  to_move);
	String rserial = get_id(req);
	if (rserial == null || !rserial.equals(serial + "")) {
	    response("299 Illegal serial number (got " +
		     rserial +
		     " expected " +
		     serial +
		     ")");
	    return null;
	}
	if (to_move == Connection.PLAYER_WHITE) {
	    String ellipsis = get_id(req);
	    if (ellipsis == null || !ellipsis.equals("...")) {
		response("299 Ellipses (...) expected before move");
		return null;
	    }
	}
	String move = get_id(req);
	try {
	    Move m = new Move(move);
	    return m;
	} catch (IllegalArgumentException e) {
	    System.out.println("Illegal move " + move + ": " + e.getMessage());
	    response("199 Request not understood");
	}
	return null;
    }

    public void print_board() {
	Gthd.board.print(writer);
    }
}
