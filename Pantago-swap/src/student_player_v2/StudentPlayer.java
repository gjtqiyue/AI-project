package student_player_v2;

import boardgame.Board;
import boardgame.Move;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import java.lang.*;
import java.sql.Time;

import pentago_swap.PentagoPlayer;
import pentago_swap.PentagoBoardState;
import pentago_swap.PentagoBoardState.Piece;
import pentago_swap.PentagoBoardState.Quadrant;
import student_player_MonteCarlo.StudentPlayer.Node;
import student_player_MonteCarlo.StudentPlayer.SearchTree;
import pentago_swap.PentagoMove;

/** A player file submitted by a student. */
public class StudentPlayer extends PentagoPlayer {
	
	public static final int WIN_VALUE = Integer.MAX_VALUE-1;
	public static final int LOSE_VALUE = Integer.MIN_VALUE+1;
	public static final int FIRST_MOVE_TIMELIMIT = 29950;
	public static final int MOVE_TIMELIMIT = 1950;
	public static long max_prun = 0;
	public static long min_prun = 0;
	public static long start_time;

    /**
     * You must modify this constructor to return your student number. This is
     * important, because this is what the code that runs the competition uses to
     * associate you with your agent. The constructor should do nothing else.
     */
    public StudentPlayer() {
        super("260728557");
    }
    
    private static PentagoBoardState cur_board;
    public Move lastPlayerMove;
    
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
    
    public class Node {
    	public Piece piece;
    	public int xPos;
    	public int yPos;
    	public boolean isSinglePiece;
    	// 0 is count
    	// 1 is blocked by wall, 0 means no block ends, 1 means blocked at 1 side, 2 means both blocked
    	// 2 is blocked by enemy
    	// 3 is if there is potential 2 or 3 in a row at matching coordinate
    	// 4 is visited or not
    	public int[] verticalStatus = {1,0,0,0,0};
    	public int[] horizontalStatus = {1,0,0,0,0};;
    	public int[] leftDiagonalStatus = {1,0,0,0,0};;
    	public int[] rightDiagonalStatus = {1,0,0,0,0};;
    	
    	public Node(int x, int y, Piece b) {
    		piece = b;
    		xPos = x;
    		yPos = y;
    		isSinglePiece = true;
    	}
    	
    	public int[] getStatus(int index) {
    		switch(index) {
    			case 1:
    				return verticalStatus;
    			case 2:
    				return horizontalStatus;
    			case 3:
    				return leftDiagonalStatus;
    			case 4:
    				return rightDiagonalStatus;
    			default:
    				break;
    		}
    		return null;
    	}
    }
    
    private Piece MyPiece(int firstPlayer) {
    	return player_id == firstPlayer ? Piece.WHITE : Piece.BLACK;
    }
    
    private Piece OpponentPiece(int firstPlayer) {
    	return player_id == firstPlayer ? Piece.BLACK : Piece.WHITE;
    }
    
    public double evaluateBoardState(PentagoBoardState boardState) {
    	
    	Piece myPiece = MyPiece(boardState.firstPlayer());
    	Piece opponentPiece = OpponentPiece(boardState.firstPlayer());
    	Node[][] board = new Node[6][6];
    	double score = 0;
		
		if (boardState.getWinner() == player_id) {
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
			for (int i=0; i<boardState.BOARD_SIZE; i++) {
				for (int j=0; j<boardState.BOARD_SIZE; j++) {
					board[i][j] = new Node(i, j, boardState.getPieceAt(i,j));
				}
			}	
			double playerScore = evaluatePlayerState(board, myPiece);
			//System.out.println("Player move eval:" + playerScore);
			double opponentScore = evaluatePlayerState(board, opponentPiece);
			//System.out.println("Oppo move eval:" + opponentScore);
			score = playerScore - opponentScore;
			//System.out.println("final score " + score);
			return score;	
		}
	}
    
    private double evaluatePlayerState(Node[][] board, Piece target) {
    	
    	ArrayList<Node> targetPieces = new ArrayList<Node>();
    	
    	// update information on each node on the board
    	for (int i=0; i<board.length; i++) {
			for (int j=0; j<board[0].length; j++) {
				
				if (board[i][j].piece == target) {
					targetPieces.add(board[i][j]);
			    	// only look at top three and left
					// top left
					if (inRange(i-1) && inRange(j-1)) {
						int cur_i = i;
						int cur_j = j;
						// if the next one is blocked
						if (inRange(cur_i+1) && inRange(cur_j+1)) {
							if (board[cur_i + 1][cur_j + 1].piece != target && board[cur_i + 1][cur_j + 1].piece != Piece.EMPTY) {
								board[cur_i][cur_j].leftDiagonalStatus[1]++;
							}
						}
						else {
							board[cur_i][cur_j].leftDiagonalStatus[2]++;
						}
						// if top left has the same color
						Node curNode = board[cur_i][cur_j];
						Node lastNode = board[cur_i - 1][cur_j - 1];
						if (lastNode.piece == target) {
							int times = lastNode.leftDiagonalStatus[0];
							for (int k=1; k<times+1; k++) {
								// go through the node in the line
								board[cur_i-k][cur_j-k].leftDiagonalStatus[0]++;	//update count
								board[cur_i-k][cur_j-k].leftDiagonalStatus[1]+=curNode.leftDiagonalStatus[1];	//update block ends
								board[cur_i-k][cur_j-k].isSinglePiece = false;
							}
							// update current node
							curNode.leftDiagonalStatus[0] = lastNode.leftDiagonalStatus[0];
							curNode.leftDiagonalStatus[1] = lastNode.leftDiagonalStatus[1];
							curNode.isSinglePiece = lastNode.isSinglePiece;
						}
						else if (board[cur_i - 1][cur_j - 1].piece != Piece.EMPTY) {
							// get blocked again
							board[cur_i][cur_j].leftDiagonalStatus[1]++;
						}
					}
					else {
						//blocked by board
						board[i][j].leftDiagonalStatus[2]++;
					}
					// top
					if (inRange(i-1)) {
						int cur_i = i;
						int cur_j = j;
						// if the next one is blocked
						if (inRange(cur_i + 1)) {	//blocked by me
							if (board[cur_i + 1][cur_j].piece != target && board[cur_i + 1][cur_j].piece != Piece.EMPTY) {
								board[cur_i][cur_j].verticalStatus[1]++;
							}
						}
						else {	//blocked by board
							board[cur_i][cur_j].verticalStatus[2]++;
						}
						// if top left has the same color
						Node curNode = board[cur_i][cur_j];
						Node lastNode = board[cur_i - 1][cur_j];
						if (lastNode.piece == target) {
							int times = lastNode.verticalStatus[0];
							for (int k=1; k<times+1; k++) {
								// go through the node in the line
								board[cur_i-k][cur_j].verticalStatus[0]++;	//update count
								board[cur_i-k][cur_j].verticalStatus[1]+=curNode.verticalStatus[1];	//update block ends
								board[cur_i-k][cur_j].isSinglePiece = false;
							}
							// update current node
							curNode.verticalStatus[0] = lastNode.verticalStatus[0];
							curNode.verticalStatus[1] = lastNode.verticalStatus[1];
							curNode.isSinglePiece = lastNode.isSinglePiece;
						}
						else if (board[cur_i - 1][cur_j].piece != Piece.EMPTY) {
							// get blocked again
							board[cur_i][cur_j].verticalStatus[1]++;
						}
					}
					else {
						//blocked by board
						board[i][j].verticalStatus[2]++;
					}
					// top right
					if (inRange(i-1) && inRange(j+1)) {
						
						int cur_i = i;
						int cur_j = j;
						// if the next one is blocked
						if (inRange(cur_i + 1) && inRange(cur_j - 1)) {
							if (board[cur_i + 1][cur_j - 1].piece != target && board[cur_i + 1][cur_j - 1].piece != Piece.EMPTY) {
								board[cur_i][cur_j].rightDiagonalStatus[1]++;
							}
						}
						else {
							board[cur_i][cur_j].rightDiagonalStatus[2]++;
						}
						// if top left has the same color
						Node curNode = board[cur_i][cur_j];
						Node lastNode = board[cur_i - 1][cur_j + 1];
						if (lastNode.piece == target) {
							int times = lastNode.rightDiagonalStatus[0];
							for (int k=1; k<times+1; k++) {
								// go through the node in the line
								board[cur_i-k][cur_j+k].rightDiagonalStatus[0]++;	//update count
								board[cur_i-k][cur_j+k].rightDiagonalStatus[1]+=curNode.rightDiagonalStatus[1];	//update block ends
								board[cur_i-k][cur_j+k].isSinglePiece = false;
							}
							// update current node
							curNode.rightDiagonalStatus[0] = lastNode.rightDiagonalStatus[0];
							curNode.rightDiagonalStatus[1] = lastNode.rightDiagonalStatus[1];
							curNode.isSinglePiece = lastNode.isSinglePiece;
						}
						else if (board[cur_i - 1][cur_j + 1].piece != Piece.EMPTY) {
							// get blocked again
							board[cur_i][cur_j].rightDiagonalStatus[1]++;
						}
					}
					else {
						//blocked by board
						board[i][j].rightDiagonalStatus[2]++;
					}
					// left
					if (inRange(j-1)) {
						int cur_i = i;
						int cur_j = j;
						// if the next one is blocked
						if (inRange(cur_j + 1)) {	//blocked by me
							if (board[cur_i][cur_j + 1].piece != target && board[cur_i][cur_j + 1].piece != Piece.EMPTY) {
								board[cur_i][cur_j].horizontalStatus[1]++;
							}
						}
						else {	//blocked by board
							board[cur_i][cur_j].horizontalStatus[2]++;
						}
						// if top left has the same color
						Node curNode = board[cur_i][cur_j];
						Node lastNode = board[cur_i][cur_j - 1];
						if (lastNode.piece == target) {
							int times = lastNode.horizontalStatus[0];
							for (int k=1; k<times+1; k++) {
								// go through the node in the line
								board[cur_i][cur_j-k].horizontalStatus[0]++;	//update count
								board[cur_i][cur_j-k].horizontalStatus[1]+=curNode.horizontalStatus[1];	//update block ends
								board[cur_i][cur_j-k].isSinglePiece = false;
							}
							// update current node
							curNode.horizontalStatus[0] = lastNode.horizontalStatus[0];
							curNode.horizontalStatus[1] = lastNode.horizontalStatus[1];
							curNode.isSinglePiece = lastNode.isSinglePiece;
						}
						else if (board[cur_i][cur_j - 1].piece != Piece.EMPTY) {
							// get blocked again
							board[cur_i][cur_j].horizontalStatus[1]++;
						}
					}
					else {
						//blocked by board
						board[i][j].horizontalStatus[2]++;
					}
				}
			}
    	}
    	
    	double score = 0;
    	
    	for (Node n : targetPieces) {
    		for (int i=0; i<4; i++) {
    			Direction dir = Direction.valueOf(i);
    			int[] status = n.getStatus(i+1);
    			score+=evaluatePiece(n, board, dir, status, target);
    		}
    	}
    	
//    	System.out.println("Student Board State:");
//    	StringBuilder sb = new StringBuilder();
//    	for (int i=0; i<6; i++) {
//    		sb.append("| ");
//    		for (int j=0; j<6; j++) {
//    			Node n = board[i][j];
//    			sb.append(" " + n.piece.toString() + ":{" + n.verticalStatus[0] + "," + n.verticalStatus[1] + "," + n.verticalStatus[2] + "},");
//    			sb.append("{" + n.horizontalStatus[0] + "," + n.horizontalStatus[1] + "," + n.horizontalStatus[2] + "},");
//    			sb.append("{" + n.leftDiagonalStatus[0] + "," + n.leftDiagonalStatus[1] + "," + n.leftDiagonalStatus[2] + "},");
//    			sb.append("{" + n.rightDiagonalStatus[0] + "," + n.rightDiagonalStatus[1] + "," + n.rightDiagonalStatus[2] + "},");
//    			sb.append(" | ");
//    		}
//    		sb.append("\n");
//    	}
//    	System.out.println(sb.toString());
//    	System.out.println(score);
    	return score;
    }
    
    private double evaluatePiece(Node n, Node[][] board, Direction dir, int[] status, Piece target) {
    	double score = 0;
    	double baseScore = 1;
    	double centerFactor = 1;
    	double chainFactor = 1;
    	double swapFactor = 1;
    	
    	if (numInARow(n, 4, dir)) {
    		if (status[2] == 2) {chainFactor = 4;}	//all blocked by wall
    		else if (status[1] == 2) {chainFactor = 7;}	//all blocked by player
    		else if (status[1] == 1 && status[2] == 1) { 
    			// check if we can swap it
    			chainFactor = 8;
    			int x = checkPotentialSwapKill(n, dir, board, 3, target);
    			if (x > 1)
    				swapFactor = x;
    			else
    				swapFactor = 1;
    		} 
    		else {chainFactor = 10;}
			//System.out.println("four in a row:" + chainFactor * centerFactor * swapFactor * baseScore);
			//markNodes(n.xPos, n.yPos, 4, board, dir);
		}
		else if (numInARow(n, 3, dir)) {
			chainFactor = 5;
			
			// is in the center?
			if (isInTheCenter(n, dir)) {
	    		centerFactor = 2; // a temporary factor
			}
			else {
				centerFactor = 1;
			}
			
			if (status[2] > 0) {
				chainFactor = 6;
			}
			else if (status[1] > 2) {
				chainFactor = 4.5;
			}
			else {
				chainFactor = 5;
			}
			// check for instant swap kill
			int x = checkPotentialSwapKill(n, dir, board, 3, target);
			if (x > 1)
				swapFactor = x;
			else
				swapFactor = 1;
			//System.out.println("three in a row:" + chainFactor * centerFactor * swapFactor * baseScore);
			//markNodes(n.xPos, n.yPos, 3, board, dir);
		}
		else if (numInARow(n, 2, dir)) {
			if (isInTheCenter(n, dir)) {
				centerFactor = 2;
			}
			else {
				centerFactor = 1;
			}
			
			if (status[1] > 0) {	//player
				chainFactor = 2;
			}
			else if (status[2] == 2) {	//wall
				chainFactor = 2.5;
			}
			else {
				chainFactor = 3;
			}
			
			int x = checkPotentialSwapKill(n, dir, board, 2, target);
			if (x > 1)
				swapFactor = x;
			else
				swapFactor = 1;
			//System.out.println("two in a row:" + chainFactor * centerFactor * swapFactor * baseScore);
			//markNodes(n.xPos, n.yPos, 2, board, dir);
		}
		else if (numInARow(n, 1, dir)) {
			if (isInTheCenter(n, dir)) {
				centerFactor = 2;
			}
			else {
				centerFactor = 1;
			}
			//System.out.println("one in a row:" + chainFactor * centerFactor * swapFactor * baseScore);
			//markNodes(n.xPos, n.yPos, 1, board, dir);
		}
    	
    	// check for potential instant kill
    	
    	score = chainFactor * centerFactor * swapFactor * baseScore;
    	return score;
    }
    
    public static int[][] diagonalFirstCheckList = { {1,1},{1,4},{4,1},{4,4} };
    
    public static int[][] verticalLeftCheckList = {{1,0},{1,3},{4,0},{4,3}};
    public static int[][] verticalMiddleCheckList = {{1,1},{1,4},{4,1},{4,4}};
    public static int[][] verticalRightCheckList = {{1,2},{1,5},{4,2},{4,5}};
    
    public static int[][] horizontalTopCheckList = {{0,1},{0,4},{3,1},{3,4}};
    public static int[][] horizontalMiddleCheckList = {{1,1},{1,4},{4,1},{4,4}};
    public static int[][] horizontalBottomCheckList = {{2,1},{2,4},{5,1},{5,4}};
    
    public static int[][] leftDiagonalSecondCheckList = { {1,0},{0,1},{1,3},{0,4},{4,0},{3,1},{4,3},{3,4} };
    
    private int countMatching(int[][] list, int statusIdx, int num, Piece target, Node[][] board) {
    	int count = 0;
    	for (int i=0; i<list.length; i++) {
			int _x = list[i][0];
			int _y = list[i][1];
			if (board[_x][_y].getStatus(statusIdx)[0] >= (3-num) && board[_x][_y].piece == target) {	// 3 in a row we have defend for minimum 1, 2 to 2
				count+=1;
			}
			else if (board[_x][_y].getStatus(statusIdx)[0] >= (3-num) && board[_x][_y].getStatus(statusIdx)[1] > 0 && board[_x][_y].piece == target) {
				count+=0.5;
			}
		}
    	return count;
    }
    
    private int checkPotentialSwapKill(Node n, Direction d, Node[][] board, int num, Piece target){
    	int count = 0;
    	int blocked = 0;
    	switch (d) {
    	case Vertical:
    		if (n.yPos == 0 || n.yPos == 3) count = countMatching(verticalLeftCheckList, 1, num, target, board);
    		else if (n.yPos == 1 || n.yPos == 4) count = countMatching(verticalMiddleCheckList, 1, num, target, board);
    		else if (n.yPos == 2 || n.yPos == 5) count = countMatching(verticalRightCheckList, 1, num, target, board);
    		return count;
    	case Horizontal:
    		if (n.xPos == 0 || n.xPos == 3) count = countMatching(horizontalTopCheckList, 2, num, target, board);
    		else if (n.xPos == 1 || n.xPos == 4) count = countMatching(horizontalMiddleCheckList, 2, num, target, board);
    		else if (n.xPos == 2 || n.xPos == 5) count = countMatching(horizontalBottomCheckList, 2, num, target, board);
    		return count;
    	case LeftD:
    		count = countMatching(diagonalFirstCheckList, 3, num, target, board);
    		return count;
    	case RightD:
    		count = countMatching(diagonalFirstCheckList, 4, num, target, board);
    		return count;
    	default:
    		break;
    	}
    	return 0;
    }
    
//    private void markNodes(int x, int y, int count, Node[][] board, Direction d) {
//    	switch (d) {
//    	case Vertical:
//    		for (int i=0; i<count; i++) {
//    			board[x+i][y].verticalStatus[3] = 1;
//    		}
//    		break;
//    	case Horizontal:
//    		for (int i=0; i<count; i++) {
//    			board[x][y+i].horizontalStatus[3] = 1;
//    		}
//    		break;
//    	case LeftD:
//    		for (int i=0; i<count; i++) {
//    			board[x+i][y+i].leftDiagonalStatus[3] = 1;
//    		}
//    		break;
//    	case RightD:
//    		for (int i=0; i<count; i++) {
//    			board[x+i][y-i].rightDiagonalStatus[3] = 1;
//    		}
//    		break;
//    	default:
//    		break;
//    	}
//    }
    
    private boolean isInTheCenter(Node n, Direction d) {
    	if (n.isSinglePiece) {
    		return ((n.xPos == 1 && n.yPos == 1) || 
    				(n.xPos == 4 && n.yPos == 1) || 
    				(n.xPos == 1 && n.yPos == 4) || 
    				(n.xPos == 4 && n.yPos == 4));
    	}
    	else {
    	switch (d) {
	    	case Vertical:
	    		return (n.yPos == 1 || n.yPos == 4);
	    	case Horizontal:
	    		return (n.xPos == 1 || n.xPos == 4);
	    	case LeftD:
	    		return ((n.xPos == n.yPos) || (n.xPos - n.yPos == 3) || (n.yPos - n.xPos == 3));
	    	case RightD:
	    		return ((n.xPos == 5 - n.yPos) || (n.xPos + n.yPos == 2) || (n.xPos + n.yPos == 8));
	    	default:
	    		return false;
	    	}
    	}
    }
    
    private boolean numInARow(Node n, int num, Direction d) {
    	switch (d) {
    	case Vertical:
    		return (n.verticalStatus[0] == num);
    	case Horizontal:
    		return (n.horizontalStatus[0] == num);
    	case LeftD:
    		return (n.leftDiagonalStatus[0] == num);
    	case RightD:
    		return (n.rightDiagonalStatus[0] == num);
    	default:
    		return false;
    	}
    }
    
    private boolean inRange(int c) {
    	return (c >= 0 && c < 6);
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

    
    public MoveValue MiniMax(int depth, int maxDepth, double alpha, double beta, PentagoBoardState boardState, PentagoMove pre_move) {
    	
    	boolean isMyTurn = boardState.getTurnPlayer() == player_id ? true : false;
    	//check if at the max
    	//System.out.println("depth: " + depth);
    	
    	if (depth == maxDepth) return new MoveValue(evaluateBoardState(boardState), null);
    	
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
        // You probably will make separate functions in MyTools.
        // For example, maybe you'll need to load some pre-processed best opening
        // strategies...
    	cur_board = boardState;
    	start_time = System.currentTimeMillis();
    	final LinkedList<MoveValue> candidateMoves = new LinkedList<MoveValue>();
    	final int time_out = boardState.getTurnNumber() == 0 ? FIRST_MOVE_TIMELIMIT : MOVE_TIMELIMIT;
    	
    	System.out.println("Current board evaluation: " + evaluateBoardState(boardState));
    	
    	Move myMove = null;
    	
    	if (boardState.getTurnNumber() < 2) {
    		for (int i=0; i<centerCoord.length; i++) {
    			if (boardState.getPieceAt(centerCoord[i][0], centerCoord[i][1]) == Piece.EMPTY) {
    				myMove = new PentagoMove(centerCoord[i][0], centerCoord[i][1], Quadrant.TL, Quadrant.TR, player_id);
    			}
    		}
    	}
//    	else if (boardState.getTurnNumber() < 5) {
//    		myMove = MonteCarloSearch(boardState);
//    	}
    	else {
//    		MoveValue optimal = MiniMax(0, 2, Integer.MIN_VALUE, Integer.MAX_VALUE, boardState, null);
//	        System.out.println("Value of the move: " + optimal.value);
//	        System.out.println("max " + max_prun + ", min " + min_prun);
//	        System.out.println("depth " + depth);
//	        myMove = optimal.move;
    		Callable<Object> IterativeDeepening = new Callable<Object> () {

				@Override
				public Object call() throws Exception {
						int depth = 2;
						MoveValue optimal;
						while (!Thread.currentThread().isInterrupted()) {
							if (depth == 2) {
								optimal = MiniMax(0, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, boardState, null);
								System.out.println("Value of the move move: " + optimal.value);
				    	        System.out.println("max " + max_prun + ", min " + min_prun);
				    	        System.out.println("depth " + depth);
							}
							else {
								optimal = MiniMax(0, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, boardState, candidateMoves.get(1).move);
								System.out.println("Value of the move move: " + optimal.value);
				    	        System.out.println("max " + max_prun + ", min " + min_prun);
				    	        System.out.println("depth " + depth);
							}
							// the first time we set the optimal as the second
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
			System.out.println("Value of the final move move: " + candidateMoves.get(0).value);
	        
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