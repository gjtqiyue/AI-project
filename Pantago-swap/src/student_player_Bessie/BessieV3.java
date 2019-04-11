package student_player_Bessie;

import boardgame.Board;
import boardgame.Move;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import pentago_swap.PentagoPlayer;
import pentago_swap.PentagoBoardState;
import pentago_swap.PentagoBoardState.Piece;
import pentago_swap.PentagoBoardState.Quadrant;
//import student_player_MonteCarlo.StudentPlayer.Node;
//import student_player_MonteCarlo.StudentPlayer.SearchTree;
import pentago_swap.PentagoMove;


public class BessieV3 extends PentagoPlayer {
	
	public static final int WIN_VALUE = Integer.MAX_VALUE-1;
	public static final int LOSE_VALUE = Integer.MIN_VALUE+1;
	public static final int FIRST_MOVE_TIMELIMIT = 29950;
	public static final int MOVE_TIMELIMIT = 1950;
	public static long max_prun = 0;
	public static long min_prun = 0;
	public static long start_time;
	public static PentagoBoardState cur_board;
    public Move lastPlayerMove;

   
    public BessieV3() {
        super("260708568");
    }
    
    public class State{
    	HashMap<Direction, LinkedList<int[]>> four = new HashMap<>();
    	HashMap<Direction, LinkedList<int[]>> three = new HashMap<>();
    	HashMap<Direction, LinkedList<int[]>> two = new HashMap<>();
    	HashMap<Direction, LinkedList<int[]>> one = new HashMap<>();
    	
    	public State() {
    		four.put(Direction.Vertical, new LinkedList<>());
    		four.put(Direction.Horizontal, new LinkedList<>());
    		four.put(Direction.RightD, new LinkedList<>());
    		four.put(Direction.LeftD, new LinkedList<>());
    		three.put(Direction.Vertical, new LinkedList<>());
    		three.put(Direction.Horizontal, new LinkedList<>());
    		three.put(Direction.RightD, new LinkedList<>());
    		three.put(Direction.LeftD, new LinkedList<>());
    		two.put(Direction.Vertical, new LinkedList<>());
    		two.put(Direction.Horizontal, new LinkedList<>());
    		two.put(Direction.RightD, new LinkedList<>());
    		two.put(Direction.LeftD, new LinkedList<>());
    		one.put(Direction.Vertical, new LinkedList<>());
    		one.put(Direction.Horizontal, new LinkedList<>());
    		one.put(Direction.RightD, new LinkedList<>());
    		one.put(Direction.LeftD, new LinkedList<>());
    	}
    }

  
    public enum Direction{
    	LeftD(2), RightD(3), Vertical(0), Horizontal(1);
    	
    	private final int value;
    	private static Map map = new HashMap<>();
    	private Direction(int value) {
    	    this.value = value;
    	}

    	static {
            for (Direction dir : Direction.values()) {
                map.put(dir.value, dir);
            }
        }

        public static Direction valueOf(int pageType) {
            return (Direction) map.get(pageType);
        }

        public int getValue() {
            return value;
        }
    }
  
    public class MoveValue {
    	public double value;
    	public PentagoMove move;
    	public MoveValue(double v, PentagoMove m) {
    		value = v;
    		move = m;
    	}
    }
    
    public class StateEval{
    	public PentagoMove move;
    	public PentagoBoardState board;
    	public double value;
    	
    	public StateEval (PentagoMove m, PentagoBoardState b) {
    		move = m;
    		board = b;
    		value = evaluateBoardState(b);
    	}
    }
    
	// Evaluation step
	public Piece MyPiece(int firstPlayer) {
    	return player_id == firstPlayer ? Piece.WHITE : Piece.BLACK;
    }
    
    public Piece OpponentPiece(int firstPlayer) {
    	return player_id == firstPlayer ? Piece.BLACK : Piece.WHITE;
    }
    
    public double evaluatePlayerState(Piece player, State state) {
    	double score = 0;
    	double baseScore = 1;
    	double centerFactor = 1;
    	double chainFactor = 1;
    	double swapFactor = 1;
    	//boolean center = false;
    	
    	for(int i = 0; i < 4; i++) {
    		Direction d = Direction.valueOf(i);
    		LinkedList<int[]> list = state.four.get(d);
    		
    		if (list.size() > 0) {
    	    	for (int[] record : list) {
    	    		if (record[1] >= 2) {chainFactor = 4;}	//all blocked by wall
    	    		else if (record[2] == 2) {chainFactor = 100;}	//all blocked by player
    	    		else if (record[1] == 1 && record[2] == 1) { 	//one wall one oppo
    	    			// check if we can swap it
    	    			chainFactor = 12;
    	    			int k = checkPotentialSwapKill(record[3], record[4], d, 3, state);
    	    			if(k > 1) {
    	    				swapFactor = k;
    	    			}
    	    			else
    	    				swapFactor = 1;
    	    		} 
    	    		else {chainFactor = 1000;}
    				//System.out.println("four in a row:" + chainFactor * centerFactor * swapFactor * baseScore);
    	    		score += chainFactor * centerFactor * swapFactor * baseScore;
    	    	}
    		}
    		
    		list = state.three.get(d);
    		if(list.size() > 0) {
    			for (int[] record : list) {
    				chainFactor = 5;
    				
    				if(isInCenter(record[3],record[4],d)) centerFactor = 3;
    				else centerFactor = 1;
    				
    				if(record[1] > 1) chainFactor++;    // blocked by walls, good
    				if(record[2] == 0) chainFactor += 2;
    				if(record[2] == 1 && record[1] == 1) chainFactor -= 8;
    				// one side is blocked by player but the other side is not
    				if(record[2] == 1 && record[1] == 0) chainFactor = 4;
        				
    				//check for instant swap kill
    				int k = checkPotentialSwapKill(record[3], record[4], d, 3, state);
    				if (k > 1)
    					swapFactor = k;
    				else
    					swapFactor = 1;
    				
    				score += chainFactor * centerFactor * swapFactor * baseScore;
    			}
    			
    		}
    		
    		list = state.two.get(d);
    		if (list.size() > 0) {
    			for (int[] record : list) {
    				if (isInCenter(record[3],record[4], d)) centerFactor = 2;
    				else centerFactor = 1;
    				
    				if (record[2] > 0)	//player
    					chainFactor = 0.1;
    				else if (record[1] == 2)//wall
    					chainFactor = 2.5;
    				else chainFactor = 3;
    	
    				int k = checkPotentialSwapKill(record[3], record[4], d, 2, state);
    				if (k > 1) swapFactor = k;
    				else swapFactor = 1;
    				score += chainFactor * centerFactor * swapFactor * baseScore;
    			}
    		}
    		
    		list = state.one.get(d);
    		if (list.size() > 0) {
    			for (int[] record : list) {
    				chainFactor = 1;
    				int k = checkPotentialSwapKill(record[3], record[4], d, 1, state);
    				if (k > 1) swapFactor = k;
    				else swapFactor = 1;
    				score += chainFactor * centerFactor * swapFactor * baseScore * 0.3;
    			}
    		}
    		
    	}
    	return score;
    }
    
    public double evaluateBoardState(PentagoBoardState boardState) {
    	Piece myPiece = MyPiece(boardState.firstPlayer());
    	Piece opponentPiece = OpponentPiece(boardState.firstPlayer());
    	double score = 0;
    	
    	if (boardState.getWinner() == player_id && boardState.getWinner() == Board.DRAW) {
    		State myState = new State();
			State oppState = new State();
			myState = processState(boardState, myPiece, opponentPiece, myState);
			oppState = processState(boardState, opponentPiece, myPiece, oppState);
			double playerScore = evaluatePlayerState(myPiece, myState);
			double opponentScore = evaluatePlayerState(opponentPiece, oppState);
			return playerScore - opponentScore;
		}
    	else if (boardState.getWinner() == player_id) {
			return WIN_VALUE;
		}
		else if (boardState.getWinner() == 1 - player_id) {
			return LOSE_VALUE;
		}
		else if (boardState.getWinner() == Board.DRAW) {
			return 0;
		}
		else {
			// evaluate the state
			// create the board with necessary info
			State myState = new State();
			State oppState = new State();
			myState = processState(boardState, myPiece, opponentPiece, myState);
			oppState = processState(boardState, opponentPiece, myPiece, oppState);
			
			double playerScore = evaluatePlayerState(myPiece, myState);
			//System.out.println("player score " + playerScore + "id " + myPiece.name() + "player_id " + player_id);
			double opponentScore = evaluatePlayerState(opponentPiece, oppState);
			//System.out.println("oppo score " + opponentScore + "id " + opponentPiece.name() + "player_id " + player_id);
			score = playerScore - opponentScore;
			return score;
			
		}
	}
   
   
    public static boolean isLegal(int x, int y) {
    	if((x < 6 && x >= 0) && (y < 6 && y >= 0))
    		return true; 
   		else 
   			return false;
   }
    
    public static boolean isLegal(int x) {
    	return (x >= 0 && x < 6);
    }
    
  //TODO: total of 4 different directions to check
    public State processState(PentagoBoardState boardState, Piece player, Piece oppo, State state)
    {
    	processStateInOneDir(boardState,player, oppo,1,1,state); //leftD
    	processStateInOneDir(boardState,player, oppo,1,-1,state); //rightD
    	processStateInOneDir(boardState,player, oppo,0,1,state); //horizontal
    	processStateInOneDir(boardState,player, oppo,1,0,state); //vertical
    	
    	return state;
    }

    public State processStateInOneDir(PentagoBoardState boardState, Piece player, Piece oppo, int dirX, int dirY, State state) {
    	Piece curColour = player;
    	Piece oppColour = oppo;
    	int pathLength, numWallsHit, numOppBlock, emptySpaces;
 
    	for(int i = 0; i < 6; i++) {
    		for(int j = 0; j < 6; j++) {
    			//reset all counters
    			pathLength = 1;
            	numWallsHit = 0;
            	numOppBlock = 0;
            	emptySpaces = 0;
    			if(boardState.getPieceAt(i, j) == Piece.EMPTY) continue;
    			if(boardState.getPieceAt(i, j) == oppColour) continue;
    			int row = i; //initial row coord
				int col = j; //initial col coord
				int prev_row = i - dirX;
				int prev_col = j - dirY;
				
    			if(isLegal(prev_row) && isLegal(prev_col)){
    				//part of a longer path
    				if(boardState.getPieceAt(prev_row, prev_col) == curColour) {
    					continue;
    				}
    				else if (boardState.getPieceAt(prev_row, prev_col) == oppColour) {
    					numOppBlock++;
    				}
    				else {
	    				while(isLegal(prev_row, prev_col) && boardState.getPieceAt(prev_row, prev_col) == Piece.EMPTY) {
	    					emptySpaces++;
	    					prev_row -= dirX;
	    					prev_col -= dirY;
	    				}
    				}
    			}
    			//hit wall at the other end
    			else{
    				numWallsHit++;
    			}
    			
    			int cur_row = i + dirX;
    			int cur_col = j + dirY;
    				
    			//if the next piece in the direction is not blocked 
    			while(isLegal(cur_row) && isLegal(cur_col) && boardState.getPieceAt(cur_row, cur_col) == curColour)
   				{
   					pathLength++;
   					cur_row += dirX;
   					cur_col += dirY;    				
   				}
    				
    			//hit wall in the same direction
        		if(!isLegal(cur_row,cur_col)) {
        			numWallsHit++;
        		}
        		//opponent block
        		else if(isLegal(cur_row,cur_col) && boardState.getPieceAt(cur_row,cur_col) == oppColour) numOppBlock++;
        		//hit empty space
        		else {
        			while(isLegal(cur_row) && isLegal(cur_col) && boardState.getPieceAt(cur_row, cur_col) == Piece.EMPTY) {
        				emptySpaces++;
        				cur_row += dirX;
        				cur_col += dirY;
        			}
        		}
    			checkState(state, pathLength, emptySpaces, numWallsHit, numOppBlock, row, col, dirX, dirY);
    		}
    	}
    	return state;
    }
    
    //coord: initial coordinates for the path
    public  void checkState(State s, int pathLength, int empty, int walls, int opponents, 
    											int coordX, int coordY, int dirX, int dirY) 
    {
    	Direction d = getDirection(dirX, dirY);
    	switch(pathLength) {
    		case (1):
    			s.one.get(d).add(new int[] {empty,walls,opponents,coordX,coordY});
    			break;
    		case(2):
    			s.two.get(d).add(new int[] {empty,walls,opponents,coordX,coordY});
    			break;
    		case(3):
    			s.three.get(d).add(new int[] {empty,walls,opponents,coordX,coordY});
    			break;
    		case(4):
    			s.four.get(d).add(new int[] {empty,walls,opponents,coordX,coordY});
    			break;
    	}
    	
    }
	
    
    public static int[][] diagonalFirstCheckList = { {1,1},{1,4},{4,1},{4,4} };
    public static int[][] leftDiagonalSecondCheckList = { {1,0},{0,1},{1,3},{0,4},{4,0},{3,1},{4,3},{3,4} };
    public static int[][] verticalLeftCheckList = {{1,0},{1,3},{4,0},{4,3}};
    public static int[][] verticalMiddleCheckList = {{1,1},{1,4},{4,1},{4,4}};
    public static int[][] verticalRightCheckList = {{1,2},{1,5},{4,2},{4,5}};
    public static int[][] horizontalTopCheckList = {{0,1},{0,4},{3,1},{3,4}};
    public static int[][] horizontalMiddleCheckList = {{1,1},{1,4},{4,1},{4,4}};
    public static int[][] horizontalBottomCheckList = {{2,1},{2,4},{5,1},{5,4}};
    
    private static int checkPotentialSwapKill(int x, int y, Direction d, int num, State state){
    	int count = 0;
    	switch (d) {
    		case Vertical:
    			if (y == 0 || y == 3) count = helperCheckingSwapping(state, verticalLeftCheckList, num, Direction.Vertical);
    			else if (y == 1 || y == 4) count = helperCheckingSwapping(state, verticalMiddleCheckList, num, Direction.Vertical);
    			else if (y == 2 || y == 5) count = helperCheckingSwapping(state, verticalRightCheckList, num, Direction.Vertical);
    			return count * 10 + 1;
    		case Horizontal:
    			if (x == 0 || x == 3) count = helperCheckingSwapping(state, horizontalTopCheckList, num, Direction.Horizontal);
    			else if (x == 1 || x == 4) count = helperCheckingSwapping(state, horizontalMiddleCheckList, num, Direction.Horizontal);
    			else if (x == 2 || x == 5) count = helperCheckingSwapping(state, horizontalBottomCheckList, num, Direction.Horizontal);
    			return count * 10 + 1;
    		case LeftD:
    			count = helperCheckingSwapping(state,diagonalFirstCheckList, num, Direction.LeftD);
    			return count * 10 + 1;
    		case RightD:	
    			count = helperCheckingSwapping(state,diagonalFirstCheckList, num, Direction.RightD);
    			return count * 10 + 1;
    		default:
    			break;
    	}
    	return 0;
    }
    
    private static int helperCheckingSwapping(State s, int[][] checkList, int num, Direction d) {
    	int matches = 1;
    	for(int i = 0; i < checkList.length; i++) {
    		int x = checkList[i][0];
    		int y = checkList[i][1];
    		
    		switch(num) {
    			case 1:
    				if(containCoord(s,3,d,x,y)) matches += 2;
    				if(containCoord(s,2,d,x,y)) matches += 1;
    				break;
    			case 2:
    				if(containCoord(s,2,d,x,y)) matches += 2;
    				if(containCoord(s,1,d,x,y)) matches += 1;
    				if(checkOppBlock(s,2,d,x,y)) matches -= 1;
    				break;
    			case 3:
    				if(containCoord(s,2,d,x,y) || containCoord(s,3,d,x,y) || containCoord(s,4,d,x,y)) matches += 2;
    				if(containCoord(s,1,d,x,y)) matches += 1;
    				break;
    		}
    	}
    	
    	return matches;
    }
    
    private static boolean containCoord(State s, int pathLength, Direction d, int x, int y) {
    	int[] coords = getCoord(d);
    	int dirX = coords[0];
    	int dirY = coords[1];
    	
    	LinkedList<int[]> l;
    	switch(pathLength) {
    		case 1:
    			l = s.one.get(d);
    			for(int[] a : l) {
    				if(a[3]==x && a[4]==y) return true;
    			}
    			break;
    		case 2:
    			l = s.two.get(d);
    			for(int[] a : l) {
    				if(a[3]==x && a[4]==y || a[3]==x+dirX && a[4]==y+dirY)
    					return true;
    			}
    			break;
    		case 3:
    			l = s.three.get(d);
    			for(int[] a : l) {
    				if(a[3]==x && a[4]==y || a[3]==x+dirX && a[4]==y+dirY || a[3]==x+2*dirX && a[4]==y+2*dirY)
    					return true;
    			}
    			break;
    		case 4:
    			l = s.four.get(d);
    			for(int[] a : l) {
    				if(a[3]==x && a[4]==y || a[3]==x+dirX && a[4]==y+dirY || a[3]==x+2*dirX && a[4]==y+2*dirY || a[3]==x+3*dirX && a[4]==y+3*dirY)
    					return true;
    			}
    			break;
    	}
    	return false;
    }
    
    private static boolean checkOppBlock(State s, int pathLength, Direction d, int x, int y) {
    	int[] coords = getCoord(d);
    	int dirX = coords[0];
    	int dirY = coords[1];
    	
    	LinkedList<int[]> l;
    	//switch(pathLength) {
		//case 2:
			l = s.two.get(d);
			for(int[] a : l) {
				if(a[3]==x && a[4]==y || a[3]==x+dirX && a[4]==y+dirY && a[2] > 0)
					return true;
			}
		//	break;
		//case 3:
			l = s.three.get(d);
			for(int[] a : l) {
				if(a[3]==x && a[4]==y || a[3]==x+dirX && a[4]==y+dirY || a[3]==x+2*dirX && a[4]==y+2*dirY && a[2] > 0)
					return true;
			}
		//	break;
    	//}
			l = s.four.get(d);
			for(int[] a : l) {
				if(a[3]==x && a[4]==y || a[3]==x+dirX && a[4]==y+dirY || a[3]==x+2*dirX && a[4]==y+2*dirY || a[3]==x+3*dirX && a[4]==y+3*dirY && a[2] > 0)
					return true;
			}
    	return false;
    }
    
    private static int[] getCoord(Direction d) {
    	switch(d){
    		case Vertical:
    			return new int[]{1,0};
    		case Horizontal:
    			return new int[]{0,1};
    		case LeftD:
    			return new int[]{1,1};
    		case RightD:
    			return new int[]{1,-1};
    		default:
    			return null;
    	}
    }
    
    private static Direction getDirection(int dirX, int dirY) {
    	if(dirX == 0 && dirY == 1)        return Direction.Horizontal;
		else if (dirX == 0 && dirY == -1) return Direction.Horizontal;
		else if (dirX == 1 && dirY == 0)  return Direction.Vertical;
		else if (dirX == -1 && dirY == 0) return Direction.Vertical;
		else if (dirX == 1 && dirY == 1)  return Direction.LeftD;
		else if (dirX == -1 && dirY == -1)return Direction.LeftD;
		else if (dirX == 1 && dirY == -1) return Direction.RightD;
		else if (dirX == -1 && dirY == 1) return Direction.RightD;
		
		else return null;
    }

    private static boolean isInCenter(int x, int y, Direction d) {
    	switch (d) {
    		case Vertical:
    			return (y == 1 || y == 4);
    		case Horizontal:
    			return (x == 1 || x == 4);
    		case LeftD:
    			return ((x == y) || (x - y == 3) || (y - x == 3));
    		case RightD:
    			return ((x == 5 - y) || (x + y == 2) || (x + y == 8));
    		default:
    			return false;
    	}
	}

    
    public MoveValue MiniMax(int depth, int maxDepth, double alpha, double beta, PentagoBoardState boardState, PentagoMove pre_move) {
    	
    	boolean isMyTurn = boardState.getTurnPlayer() == player_id ? true : false;
    	//check if at the max
    	//System.out.println("depth: " + depth);
    
    	if (depth == maxDepth) return new MoveValue(evaluateBoardState(boardState), null);
    	
    	if (boardState.getWinner() == player_id) {
			return new MoveValue(WIN_VALUE, null);
		}
		else if (boardState.getWinner() == 1 - player_id) {
			return new MoveValue(LOSE_VALUE, null);
		}
    	
    	if (boardState.gameOver()) return new MoveValue(0, null);
    	
    	// we want to sort all the move with its evaluation before doing a-b pruning
    	PriorityQueue<StateEval> queue;
    	Comparator<StateEval> comparator;
    	if (pre_move != null && depth == 0) {
    		comparator = new Comparator<StateEval>() {
    			// it is a priority queue, but we want the biggest on the top 
				@Override
				public int compare(StateEval o1, StateEval o2) {
					//neg if o1 < o2, pos if o1 > o2
					if (o2.move == pre_move) return LOSE_VALUE+1;
					else if (o1.move == pre_move) return WIN_VALUE-1;
					else return (int)(o2.value - o1.value);
				}
    		};
    	}
    	else if (isMyTurn) {
    		comparator = new Comparator<StateEval>() {
    			// it is a priority queue, but we want the biggest on the top 
				@Override
				public int compare(StateEval o1, StateEval o2) {
					//neg if o1 < o2, pos if o1 > o2
					return (int)(o2.value - o1.value);
				}
    		};
    	}
    	else {
    		comparator = new Comparator<StateEval>() {
    			// it is a priority queue, but we want the biggest on the top 
				@Override
				public int compare(StateEval o1, StateEval o2) {
					//neg if o1 < o2, pos if o1 > o2
					return (int)(o1.value - o2.value);
				}
    		};
    	}
    	
    	ArrayList<PentagoMove> options = boardState.getAllLegalMoves();
    	
    	//System.out.println("Turn:" + boardState.getTurnNumber() + ", options: " + options.size());

    	MoveValue bestValue = null;
    	//minimax with alpha beta pruning
    	if (isMyTurn) {	//max player
    		//sort the moves based on the evaluation function (max first)
    		queue = new PriorityQueue<StateEval> (10, comparator);
    		//max
    		//add to the queue and then process the queue instead
    		for (PentagoMove move : options) {
    			//apply this move and get a new boardstate
    			PentagoBoardState newState = (PentagoBoardState)boardState.clone();
    			newState.processMove(move);
    			queue.add(new StateEval(move, newState));
    		}
    		int count = 0;
    		while (!queue.isEmpty()) {
    			
    			StateEval s = queue.remove();
    			//System.out.println("Max step " + count +  ": " + s.value );
    			count++;
    			PentagoBoardState newState = s.board;
    			PentagoMove move = s.move;
    			
    			if (newState.getWinner() == player_id) {
    				return new MoveValue(WIN_VALUE, move);
    			}
    			
    			MoveValue result = MiniMax(depth+1, maxDepth, alpha, beta, newState, null);
    			
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
    		//sort the moves based on the evaluation function (max first)
    		queue = new PriorityQueue<StateEval> (10, comparator);
    		//max
    		//add to the queue and then process the queue instead
    		for (PentagoMove move : options) {
    			//apply this move and get a new boardstate
    			PentagoBoardState newState = (PentagoBoardState)boardState.clone();
    			newState.processMove(move);
    			queue.add(new StateEval(move, newState));
    		}
    		int count = 0;
    		while (!queue.isEmpty()) {
    			//apply this move and get a new boardstate
    			StateEval s = queue.remove();
    			//System.out.println("Min step " + count +  ": " + s.value );
    			count++;
    			PentagoBoardState newState = s.board;
    			PentagoMove move = s.move;
    			
    			if (newState.getWinner() == 1 - player_id) {
    				return new MoveValue(LOSE_VALUE, move);
    			}
    			
    			MoveValue result = MiniMax(depth+1, maxDepth, alpha, beta, newState, null);    			
    			
    			if (bestValue == null || (result.value < bestValue.value)) {
    				bestValue = result;
    				bestValue.move = move;
    			}
    			
    			//alpha = Math.max(alpha, result.value);
    			
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

    /**
     * This is the primary method that you need to implement. The ``boardState``
     * object contains the current state of the game, which your agent must use to
     * make decisions.
     */
    public Move chooseMove(PentagoBoardState boardState) {
    	
    	/*
    	ArrayList<PentagoMove> moves = boardState.getAllLegalMoves();
    	PentagoBoardState newState = (PentagoBoardState)boardState.clone();
		newState.processMove(moves.get(0));
    	evaluateBoardState(newState);
        return moves.get(11);
        */
        
    	
        // You probably will make separate functions in MyTools.
        // For example, maybe you'll need to load some pre-processed best opening
        // strategies...
    	cur_board = boardState;
    	start_time = System.currentTimeMillis();
    	final LinkedList<MoveValue> candidateMoves = new LinkedList<MoveValue>();
    	final int time_out = boardState.getTurnNumber() == 0 ? FIRST_MOVE_TIMELIMIT : MOVE_TIMELIMIT;
    	
    	Move myMove = null;
    	if (boardState.getTurnNumber() < 2) {
    		for (int i=0; i<centerCoord.length; i++) {
    			if (boardState.getPieceAt(centerCoord[i][0], centerCoord[i][1]) == Piece.EMPTY) {
    				myMove = new PentagoMove(centerCoord[i][0], centerCoord[i][1], Quadrant.TL, Quadrant.TR, player_id);
    			}
    		}
    	}
//    	
    	
    	else {
    		Callable<Object> IterativeDeepening = new Callable<Object> () {

				@Override
				public Object call() throws Exception {
						int depth = 2;
						MoveValue optimal;
						while (!Thread.currentThread().isInterrupted()) {
							if (depth == 2) {
								optimal = MiniMax(0, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, boardState, null);
						        System.out.println("Value of the move: " + optimal.value);
						        System.out.println("max " + max_prun + ", min " + min_prun);
						        System.out.println("depth " + depth);
							}
							else {
								optimal = MiniMax(0, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, boardState, candidateMoves.get(1).move);
						        System.out.println("Value of the move: " + optimal.value);
						        System.out.println("max " + max_prun + ", min " + min_prun);
						        System.out.println("depth " + depth);
							}
							//the first time we set the optimal as the second
					        if (depth == 2) {
					        	candidateMoves.add(optimal);
					        	candidateMoves.add(optimal);
					        }
					        else {	// else we put the first place to the second place
					        	candidateMoves.set(1, candidateMoves.get(0));
					        	candidateMoves.set(0, optimal);
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
    	}
 
    	long endTime = System.currentTimeMillis();
    	long elapseTime = endTime - start_time;
    	System.out.println("Time: " + elapseTime + "\n");

        // Return your move to be processed by the server.
        return myMove;
    }
    
    // monte carlo search
    public static final double UCT_FACTOR = 1.414;
	public static double epsilon = 1e-6;
	Random r = new Random();
	int depth;
    
    public class SearchTree {
    	private MCTNode root;
    	
    	public SearchTree(MCTNode r) {
    		root = r;
    	}
    	public SearchTree() { 
    		root = null;
    	}
    	
    	public MCTNode root() {
    		return root;
    	}
    	
    	public void setRoot(MCTNode r) {
    		root = r;
    	}
    }
    
    public class MCTNode {
    	public int turnPlayer;
    	public double total;
    	public double win;
    	public PentagoBoardState state;
    	public PentagoMove action;
    	public MCTNode parent;
    	public ArrayList<MCTNode> children;
    	public boolean visited;
    	
    	public MCTNode(MCTNode par, int turnPlayer, PentagoBoardState board, PentagoMove move) {
    		total = 0;
    		win = 0;
    		parent = par;
    		state = board;
    		action = move;
    		visited = false;
    		children = new ArrayList<MCTNode>();
    	}
    	
    	public double getWinRate() {
    		if (total == 0) return win/(total+epsilon);
    		else return win/ total;
    	}
    	
    	public double getUCTInChildren(MCTNode node) {
    		try {
    			//System.out.println("UCT" + node.getWinRate() + " " + total + " " + (node.total+epsilon) + " ");
    			return node.getWinRate() + UCT_FACTOR * Math.sqrt((Math.log(total)/(node.total + epsilon))) + r.nextDouble()*epsilon;
    			//else return node.getWinRate() + UCT_FACTOR * Math.sqrt((Math.log(total)/(node.total + epsilon)));
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    		return -1;
    	}
    	
		public MCTNode getBestNode() {
			// randomly return a node for now
			MCTNode ranEntry = (MCTNode)children.get(new Random().nextInt(children.size()));
    		return ranEntry;
    	}
    	
    	// find the one with the most visited number
    	public MCTNode getBestEntry(){
    		double maxRate = -1;
    		MCTNode bestEntry = null;
        	for (MCTNode m : children) {
        		double rate = m.getWinRate();
        		if (rate > maxRate) {
        			maxRate = rate;
        			bestEntry = m;
        		}
        	}
        	return bestEntry;
    	}
    }
    
   
    // apply UCT until reach the bottom
    public MCTNode SelectPromisingNode(MCTNode rootNode) {
    	MCTNode node = rootNode;
        while (node.children.size() != 0) {
            node = FindBestNodeWithUCT(node);
            depth++;
        }
        return node;
    }
    
    public MCTNode FindBestNodeWithUCT(MCTNode node) {
    	double maxUCT = -1;
    	MCTNode maxNode = null;
    	for (MCTNode m : node.children) {
    		double uct = node.getUCTInChildren(m);
    		if (uct > maxUCT) {
    			maxUCT = uct;
    			maxNode = m;
    		}
    	}
    	System.out.println("max UCT " + maxUCT + " win/total " + maxNode.win + "/" + maxNode.total);
    	return maxNode;
    }
    
    public void expandNode(MCTNode parent) {
    	int turn_player = 0;
    	ArrayList<PentagoMove> possibleMoves = parent.state.getAllLegalMoves();
    	for (PentagoMove m : possibleMoves) {
    		PentagoBoardState newState = (PentagoBoardState)parent.state.clone();
    		newState.processMove(m);
    		MCTNode child = new MCTNode(parent, newState.getTurnPlayer(), newState, m);
    		turn_player = newState.getTurnPlayer();
    		parent.children.add(child);
    	}
    	System.out.println("Expand node: "+ parent.children.size() + " for player " +  turn_player);
    }
    
    public int simulateRandomPlay(MCTNode n) {
    	PentagoBoardState curState = n.state;
    	do {
    		PentagoMove m = (PentagoMove)curState.getRandomMove();
    		PentagoBoardState newState = (PentagoBoardState)curState.clone();
    		newState.processMove(m);
    		curState = newState;
    	} while (curState.getWinner() == Board.NOBODY);
    	
    	if (curState.getWinner() == player_id) {
			return 1;
		}
		else {
			return 0;
		}
    }
    
    public void updateNodes(MCTNode leaf, int res) {
    	System.out.println("backtracking, result" + res);
    	MCTNode cur = leaf;
    	while (cur != null) {
    		cur.visited = true;
    		cur.win+=res;
    		cur.total++;
    		System.out.println(cur.win + " " + cur.total + " " + cur.getWinRate());
    		cur = cur.parent;
    	}
    }
    
    
    public PentagoMove MonteCarloSearch(PentagoBoardState board) {
    	SearchTree tree = new SearchTree();
    	
    	if (tree.root() == null) {
    		tree.setRoot(new MCTNode(null, player_id, (PentagoBoardState)board.clone(), null));
    	}
    	
    	int count = 0;
    	while (System.currentTimeMillis() - start_time < MOVE_TIMELIMIT) {
    		depth = 0;
	    	// selection
    		MCTNode nodeToExpand = SelectPromisingNode(tree.root());
	    	System.out.println("depth " + depth);
	    	// expansion / rollout
	    	if (nodeToExpand.state.getWinner() == Board.NOBODY) {
	    		expandNode(nodeToExpand);// fully expanded
	    	}
	    	MCTNode nodeToSearch;
	    	if (nodeToExpand.children.size() > 0) {
	    		nodeToSearch = nodeToExpand.getBestNode();
	    		depth++;
	    		System.out.println("second depth " + depth);
	    	}
	    	else {
	    		nodeToSearch = nodeToExpand;
	    	}
	    	// choose one to explore and play randomly
	    	int result = simulateRandomPlay(nodeToSearch);
	    	// update
	    	updateNodes(nodeToSearch, result);
	    	System.out.println(System.currentTimeMillis() - start_time);
	    	count++;
    	}
    	
    	System.out.println("Simulation time: " + count);
    	MCTNode entryToSelect = tree.root().getBestEntry();
    	
    	System.out.println("WinRate:" + entryToSelect.getWinRate());
    	tree.setRoot(entryToSelect);
    	return entryToSelect.action;
    	
    }
    
    
}