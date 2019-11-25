import java.lang.*;
import java.io.*;

class TerminationException extends Exception {}

public class Grossthello {
    GthClient client;
    WorkBoard board = new WorkBoard();
    int depth;
    
    public Grossthello(GthClient client, int depth)
	{
		this.client = client;
		this.depth = depth;
    }

    private void make_my_move() throws TerminationException
	{
		board.bestMove(depth);
		String ellipses = "... ";
		if (board.to_move == Board.PLAYER_BLACK)
			ellipses = "";
		System.out.println(
				"me:  " + board.serial + ". " + ellipses + board.best_move.name()
		);
		int result = board.try_move(board.best_move);
		if (result == board.ILLEGAL_MOVE)
	    	throw new Error("attempted illegal move");
		int state;
		try {
	    	state = client.make_move(board.best_move);
		} catch(IOException e) {
	    	e.printStackTrace(System.out);
	    	throw new Error("move refused by referee");
		}
		if (state == client.STATE_CONTINUE && result != board.CONTINUE)
	    	throw new Error("Client erroneously expected game over");
		if (state == client.STATE_DONE) {
	    	if (result != board.GAME_OVER)
				System.out.println("Game over unexpectedly");
	    	throw new TerminationException();
		}
    }

    private void get_opp_move() throws TerminationException
	{
		int state;
		try {
	    	state = client.get_move();
		} catch(IOException e) {
	    	e.printStackTrace(System.out);
	    	throw new Error("couldn't get move from referee");
		}
		if (state == client.STATE_DONE)
	    	throw new TerminationException();
		String ellipses = "... ";
		if (board.to_move == Board.PLAYER_BLACK)
	    	ellipses = "";
		System.out.println(
				"opp: " + board.serial + ". " + ellipses + client.move.name()
		);
		int result = board.try_move(client.move);
		if (result == board.ILLEGAL_MOVE)
	    	throw new Error("received apparently illegal move");
    }

    public void play()
	{
		try {
	    	while (true)
	    	{
				if (client.who == client.WHO_BLACK)
		    		make_my_move();
				else
		    		get_opp_move();
				if (client.who == client.WHO_WHITE)
		    		make_my_move();
				else
		    		get_opp_move();
	    	}
		} catch(TerminationException e) {
	    	System.out.print("Game ends with ");
	    	switch (client.winner)
			{
	    		case GthClient.WHO_WHITE:
					System.out.println("white win");
					break;
	    		case GthClient.WHO_BLACK:
					System.out.println("black win");
					break;
	    		case GthClient.WHO_NONE:
					System.out.println("draw");
					break;
	    	}
		}
    }

    public static void main(String args[]) throws IOException
	{
		// Set up game
		if (args.length != 4)
	    	throw new IllegalArgumentException(
	       		"usage: black|white hostname server-number depth"
			);
		int side;
		if (args[0].equals("black"))
	    	side = GthClient.WHO_BLACK;
		else if (args[0].equals("white"))
	    	side = GthClient.WHO_WHITE;
		else
	    	throw new IllegalArgumentException("unknown side");
		String host = args[1];
		int server = Integer.parseInt(args[2]);
		GthClient client = new GthClient(side, host, server);
		int depth = Integer.parseInt(args[3]);
		Grossthello game = new Grossthello(client, depth);

		// Start game
		game.play();
    }
}
