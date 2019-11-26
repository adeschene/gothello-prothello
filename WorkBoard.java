import java.util.Vector;
import java.lang.Math;

public class WorkBoard extends Board
{
	static final int INF = 5 * 5 + 1; // Greater magnitude than any possible score
    Move bestMoveFound = null;

    public WorkBoard() {}

    public WorkBoard(WorkBoard w) { super(w); }

    int heval()
	{
		int score = 0; // Initial score

		// Score = Number of stones on both sides
		for (int i = 0; i < 5; i++)
	    	for (int j = 0; j < 5; j++)
				if (square[i][j] == checker_of(to_move))
		    		score++; // For each stone on player's side, increment score
	        	else if (square[i][j] == checker_of(opponent(to_move)))
		    		score--; // For each stone on opponent's side, decrement score
		return score;
    }

    // Minimax algorithm with alpha-beta pruning
    int pruningMinimax(int depth, int alpha, int beta, boolean maxingPlayer)
	{
		Vector<Move> moves = genMoves();
		if (depth <= 0 || moves.isEmpty())
	    	return heval();
		if (maxingPlayer)
		{
			int score = -INF;
			for (Move nextMove : moves)
			{
				WorkBoard nextBoard = new WorkBoard(this);
				int status = nextBoard.try_move(bestMoveFound);
				if (status == ILLEGAL_MOVE)
					throw new Error("unexpectedly illegal move");
				if (status == GAME_OVER)
					throw new Error("unexpectedly game over");
				int newScore = pruningMinimax(depth - 1, alpha, beta, false);
				score = Math.max(score, newScore);
				alpha = Math.max(alpha, newScore);
				if (beta <= alpha)
					break;
			}
			return score;
		}
		else // Minimizing player
		{
			int score = INF;
			for (Move nextMove : moves)
			{
				WorkBoard nextBoard = new WorkBoard(this);
				int status = nextBoard.try_move(bestMoveFound);
				if (status == ILLEGAL_MOVE)
					throw new Error("unexpectedly illegal move");
				if (status == GAME_OVER)
					throw new Error("unexpectedly game over");
				int newScore = pruningMinimax(depth - 1, alpha, beta, false);
				score = Math.min(score, newScore);
				beta = Math.min(beta, newScore);
				if (beta <= alpha)
					break;
			}
			return score;
		}
	}

    void bestMove(int depth)
	{
		int score = pruningMinimax(depth, -INF, INF, true);
    }
}
