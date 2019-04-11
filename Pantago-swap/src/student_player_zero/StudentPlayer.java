package student_player_zero;


import boardgame.Move;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import boardgame.Board;
import boardgame.BoardState;
import pentago_swap.PentagoPlayer;
import pentago_swap.PentagoBoardState;
import pentago_swap.PentagoBoardState.Piece;
import pentago_swap.PentagoBoardState.Quadrant;
import pentago_swap.PentagoMove;

/** A player file submitted by a student. */
public class StudentPlayer extends PentagoPlayer {

    /**
     * You must modify this constructor to return your student number. This is
     * important, because this is what the code that runs the competition uses to
     * associate you with your agent. The constructor should do nothing else.
     */
    public StudentPlayer() {
        super("260716696");
    }
    //public double max = Integer.MAX_VALUE;
    //public double min = Integer.MIN_VALUE;
    public static final int WIN_VALUE = Integer.MAX_VALUE-1;
	public static final int LOSE_VALUE = Integer.MIN_VALUE+1;
	public static long max_prun = 0;
	public static long min_prun = 0;
	public static long start_time;	
	int ourplayer = this.player_id;
	
	public class MoveValue {
	    	public double value;
	    	public PentagoMove move;
	    	public MoveValue(double v, PentagoMove m) {
	    		value = v;
	    		move = m;
	    	}
	}

    //evaluate function
    private double evaluate(PentagoBoardState boardState){
    	double result = 0;
    	//case when we already win
    	if(boardState.getWinner() == player_id){
    		return WIN_VALUE;
    	}
    	//case the opponent win
    	else if(boardState.getWinner() == 1-player_id){
    		return LOSE_VALUE;
    	}
    	//case in a draw
    	else if(boardState.getWinner() == Board.DRAW){
    		return 0.0;
    	}
    	
    	int turnPlayer = boardState.getTurnPlayer();
    	
    	//changed here
    	double[][] totalResult = MyTools.checkState(boardState, 1-turnPlayer);
    	double[] whiteResult = totalResult[0];
    	double[] blackResult = totalResult[1];
    	//System.out.println(Arrays.toString(oppoResult));
    	double finalscore = 0;
    			finalscore = evaluationfunction(whiteResult, blackResult, turnPlayer, boardState);
    	return finalscore;
    }
    private double evaluationfunction(double[] whitescore,double[] blackscore, int turn, BoardState boardState){
    	double evaluatedscore =0.0;
    	//if we move first
    	if(ourplayer == 0){
    		evaluatedscore = whitescore[4]*10 + whitescore[2]*5 + whitescore[3]*3 + whitescore[1]*0.5 + whitescore[0]*1;
    		evaluatedscore = evaluatedscore - 0.8*(blackscore[4]*10+ blackscore[2]*5 + blackscore[3]*3 + blackscore[1]*0.5 + blackscore[0]*1);
    	}
    	//if we move last
    	else{
    		evaluatedscore = whitescore[4]*10 + whitescore[2]*5 + whitescore[3]*3 + whitescore[1]*0.5 + whitescore[0]*1;
    		evaluatedscore = (blackscore[4]*10 + blackscore[2]*5 + blackscore[3]*3 + blackscore[1]*0.5 + blackscore[0]*1) - 0.9*evaluatedscore;
 
    	}
    	return evaluatedscore;
    }
    
    
 public MoveValue MiniMax(int depth, int maxDepth, double alpha, double beta, PentagoBoardState boardState, PentagoMove pre_move) {
    	
    	boolean isMyTurn = boardState.getTurnPlayer() == player_id ? true : false;

    	if (depth == maxDepth) return new MoveValue(evaluate(boardState), null);
    	
    	
    	ArrayList<PentagoMove> options = boardState.getAllLegalMoves();
    	
    	//System.out.println("Turn:" + boardState.getTurnNumber() + ", options: " + options.size());

    	MoveValue bestValue = null;
    	//minimax with alpha beta pruning
    	if (isMyTurn) {	//max player
    		//sort the moves based on the evaluation function (max first)
    		
    		//max
    		for (PentagoMove move : options) {
    			//apply this move and get a new boardstate
    			PentagoBoardState newState = (PentagoBoardState)boardState.clone();
    			newState.processMove(move);
    			
    			if (newState.getWinner() == player_id) {
    				return new MoveValue(WIN_VALUE, move);
    			}
    			
    			MoveValue result = MiniMax(depth+1, maxDepth, alpha, beta, newState, move);
    			
    			// if the best value so far is null we assign the first return value to it
    			if (bestValue == null || (result.value > bestValue.value)) {
    				bestValue = result;
    				bestValue.move = move;
    			}
    			
    			//alpha = Math.max(alpha, result.value);
    			
    			if (alpha < result.value) {
    				//update the best move
    				alpha = result.value;
    				bestValue = result;
    			}
    			
    			if (alpha >= beta) {	// pruning
    				max_prun++;
    				bestValue.value = beta;
    				bestValue.move = null;
    				return bestValue;
    			}
    		}
    		return bestValue;
    	}
    	else {
    		//sort the moves based on the evaluation function (min first)
    		//min
    		for (PentagoMove move : options) {
    			//apply this move and get a new boardstate
    			PentagoBoardState newState = (PentagoBoardState)boardState.clone();
    			newState.processMove(move);
    			
    			if (newState.getWinner() == 1 - player_id) {
    				return new MoveValue(LOSE_VALUE, move);
    			}
    			MoveValue result = MiniMax(depth+1, maxDepth, alpha, beta, newState, move);    			
    			
    			if (bestValue == null || (result.value < bestValue.value)) {
    				bestValue = result;
    				bestValue.move = move;
    			}
    			
    			if (beta > result.value) {
    				//update the best move
    				beta = result.value;
    				bestValue = result;
    			}
    			
    			if (alpha >= beta) {
    				min_prun++;
    				bestValue.value = alpha;
    				bestValue.move = null;
    				return bestValue;
    			}

    		}
    		return bestValue;
    	}
 }
    
    public static int[][] centerCoord = { {1,1},{1,4},{4,1},{4,4} };
    public static int[][] centerCoordDiag = {{2, 2}, {2, 3}, {3, 2}, {3, 3}};
    
    /**
     * This is the primary method that you need to implement. The ``boardState``
     * object contains the current state of the game, which your agent must use to
     * make decisions.
     */
    private static PentagoBoardState cur_board;
    public static final int FIRST_MOVE_TIMELIMIT = 29950;
	public static final int MOVE_TIMELIMIT = 1950;
    public Move chooseMove(PentagoBoardState boardState) {
        // You probably will make separate functions in MyTools.
        // For example, maybe you'll need to load some pre-processed best opening
        // strategies...
    	cur_board = boardState;
    	start_time = System.currentTimeMillis();
    	final LinkedList<MoveValue> candidateMoves = new LinkedList<MoveValue>();
    	final int time_out = boardState.getTurnNumber() == 0 ? FIRST_MOVE_TIMELIMIT : MOVE_TIMELIMIT;
    	
    	//System.out.println("Current board evaluation: " + evaluateBoardState(boardState));
    	
    	Move myMove = null;
    	Piece ourColour = this.player_id == 0 ? Piece.WHITE : Piece.BLACK;
    	System.out.println("Turn: " + boardState.getTurnNumber());
    	System.out.println(ourColour);
    	if (boardState.getTurnNumber() < 3 && ourColour ==Piece.WHITE) {
    		for (int i=0; i<centerCoord.length; i++) {
    			if (boardState.getPieceAt(centerCoord[i][0], centerCoord[i][1]) == Piece.EMPTY) {
    				myMove = new PentagoMove(centerCoord[i][0], centerCoord[i][1], Quadrant.TL, Quadrant.TR, player_id);
    			}
    		}
    		int count = 0;
    		for (int i=0; i<centerCoord.length; i++) {
    			if (boardState.getPieceAt(centerCoordDiag[i][0], centerCoordDiag[i][1]) == Piece.EMPTY) {
    				count++;
    			}
    		}
    		
        	if (count==4 &&boardState.getTurnNumber() ==2 && ourColour==Piece.WHITE){
        		System.out.println("reached here!");
        		int first=-1;
        		int second=-1;
        		for (int i=0; i<centerCoord.length; i++) {
        			if (boardState.getPieceAt(centerCoord[i][0], centerCoord[i][1]) == ourColour ) {
        					if(first ==-1){
        						first = i;
        					}
        					else{
        						second =i;
        					}
        			}
        		}
        		if(first ==0){
        			if(second==1){
        				myMove = new PentagoMove(centerCoordDiag[first][0], centerCoordDiag[first][1], Quadrant.TR, Quadrant.BR, player_id);
        			}
        			else if(second ==2){
        				myMove = new PentagoMove(centerCoordDiag[first][0], centerCoordDiag[first][1], Quadrant.BL, Quadrant.BR, player_id);
        			}
        			else{
        				myMove = new PentagoMove(centerCoordDiag[first][0], centerCoordDiag[first][1], Quadrant.TR, Quadrant.BL, player_id);
        			}
        		}
        		else if(first ==1){
        			if(second ==2){
        				myMove = new PentagoMove(centerCoordDiag[first][0], centerCoordDiag[first][1], Quadrant.TL, Quadrant.BR, player_id);
        			}
        			else{
        				myMove = new PentagoMove(centerCoordDiag[first][0], centerCoordDiag[first][1], Quadrant.BL, Quadrant.BR, player_id);
        			}
        		}
        		else{
        		myMove = new PentagoMove(centerCoordDiag[first][0], centerCoordDiag[first][1], Quadrant.TR, Quadrant.BR, player_id);
        		}
        	}
        	
    		

    	}
    		
    	else {
    		if (boardState.getTurnNumber() < 2){
    			for (int i=0; i<centerCoord.length; i++) {
        			if (boardState.getPieceAt(centerCoord[i][0], centerCoord[i][1]) == Piece.EMPTY) {
        				myMove = new PentagoMove(centerCoord[i][0], centerCoord[i][1], Quadrant.TL, Quadrant.TR, player_id);
        			}
        		}
    		}
    		if(myMove == null){
    		Callable<Object> IterativeDeepening = new Callable<Object> () {

				@Override
				public Object call() throws Exception {
						int depth = 2;
						MoveValue optimal;
						while (!Thread.currentThread().isInterrupted()) {
							if (depth == 2) {
								optimal = MiniMax(0, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, boardState, null);
							}
							else {
								optimal = MiniMax(0, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, boardState, candidateMoves.get(1).move);
							}
							// the first time we set the optimal as the second
					        if (depth == 2) {
					        	candidateMoves.add(optimal);
					        	candidateMoves.add(optimal);
					        }
					        else {	// else we put the first place to the second place
					        	//if(optimal.value > candidateMoves.get(0).value){
					        		candidateMoves.set(1, candidateMoves.get(0));
					        		candidateMoves.set(0, optimal);
					        	//}
					        	System.out.println("zero depth" + depth);
					        }
					        depth++;
					}
					return 0;
				}
    		
    		};
    		
    		ExecutorService executor = Executors.newSingleThreadExecutor();
    		
    		final Future<Object> futureEvent = executor.submit(IterativeDeepening);
    		try {
				futureEvent.get(time_out, TimeUnit.MILLISECONDS);
			} 
    		catch (TimeoutException e) {
				// time out stop the thread and return a value
    			if (candidateMoves.size() > 0) {
    				myMove = candidateMoves.get(0).move;
	    			System.out.println("Value of the final move move: " + candidateMoves.get(0).value);
    			}
			}
    		catch (InterruptedException | ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		finally {
    			executor.shutdownNow();
    		}
    		
    		myMove = candidateMoves.get(0).move;
			System.out.println("Value of the final move move: " + candidateMoves.get(0).value);
	        
    	}
    	}
    	
 
    	long endTime = System.currentTimeMillis();
    	long elapseTime = endTime - start_time;
    	System.out.println("Time: " + elapseTime + "\n");

        // Return your move to be processed by the server.
    	int turnPlayer = player_id;
    	double[][] totalResult = MyTools.checkState(boardState, 1-turnPlayer);
    	System.out.println("whitescore: ");
    	System.out.println(Arrays.toString(totalResult[0])); 
    	System.out.println("blackscore: ");
    	System.out.println(Arrays.toString(totalResult[1]));
        return myMove;
    }


}