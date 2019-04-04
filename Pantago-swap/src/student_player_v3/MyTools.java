package student_player_v3;

import boardgame.Board;
import boardgame.Move;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.lang.*;

import pentago_swap.PentagoPlayer;
import pentago_swap.PentagoBoardState;
import pentago_swap.PentagoBoardState.Piece;
import student_player_MonteCarlo.StudentPlayer.Node;
import student_player_v3.StudentPlayer.Direction;
import pentago_swap.PentagoMove;

public class MyTools {
	
	public static final int WIN_VALUE = Integer.MAX_VALUE-1;
	public static final int LOSE_VALUE = Integer.MIN_VALUE+1;
 
    static final int TR      = 1;   // 00000001
    static final int TL      = 2;   // 00000010
    static final int BL      = 4;   // 00000100
    static final int BR      = 8;   // 00001000
    static final int TOP     = 16;  // 00010000
    static final int BOTTOM  = 32;  // 00100000
    static final int LEFT    = 64;  // 01000000
    static final int RIGHT   = 128; // 10000000
    
    
    public static class Node{
    	public int x;
    	public int y;
    	
    	//uses bits to represent neighbours
    	int neighbours = 0;
    	
    	public Node(int x, int y) {
    		this.x = x;
    		this.y = y;
    	}
    	
    	public void reset() {
    		neighbours = 0;
    	
    	}

    	public void add(int dirX, int dirY) {
    		if(dirX == 0 && dirY == 1)        neighbours |= RIGHT;
    		else if (dirX == 0 && dirY == -1) neighbours |= LEFT;
    		else if (dirX == 1 && dirY == 0)  neighbours |= BOTTOM;
    		else if (dirX == -1 && dirY == 0) neighbours |= TOP;
    		else if (dirX == 1 && dirY == 1)  neighbours |= BR;
    		else if (dirX == 1 && dirY == -1) neighbours |= BL;
    		else if (dirX == -1 && dirY == 1) neighbours |= TR;
    		else if (dirX == -1 && dirY == -1)neighbours |= TL;
    	}
    }
    
    public static class State{
    	LinkedList<int[]> four = new LinkedList<>();
    	LinkedList<int[]> three = new LinkedList<>();
    	LinkedList<int[]> two = new LinkedList<>();
    }
    
    
    public static Node[][] map = new Node[6][6];

    
    public static boolean isLegal(int x, int y) {
   	 if (x >= 6 || x < 0 || y < 0 || y >= 6) { return false; }
   	 return true;
   }
    
  //TODO: total of 4 different directions to check
    public static State processState(PentagoBoardState boardState, int player, State state)
    {
    	processStateInOneDir(boardState,player,0,1,state); //right
    	processStateInOneDir(boardState,player,1,0,state); //left
    	processStateInOneDir(boardState,player,1,1,state); //br
    	processStateInOneDir(boardState,player,1,-1,state); //bl
    	return state;
    }

    
    
    public static State processStateInOneDir(PentagoBoardState boardState, int player, int dirX, int dirY, State state) {
    	Piece curColour = player == 0 ? Piece.WHITE : Piece.BLACK;
    	Piece oppColour = 1 - player == 0 ? Piece.WHITE : Piece.BLACK;
    	//r_op, c_op : the opposite site coordinate
    	//r,c : current coordinate
    	int r, c, r_op, c_op;
    	int pathLength, numWallsHit, numOppBlock, emptySpaces;
    	Node cur, parent;
    	
    	for(int i = 0; i < 6; i++) {
    		r = i + dirX;
    		r_op = i - dirX;
    		pathLength = 0;
        	numWallsHit = 0;
        	numOppBlock = 0;
        	emptySpaces = 0;
        	
    		for(int j = 0; j < 6; j++) {
    			c = j + dirY;
    			c_op = j - dirY;
    			cur = map[r][c];
    			
    			while(isLegal(r,c) && boardState.getPieceAt(r, c) == curColour) {
    				//if in a path of longer chain, break;
    				if(isLegal(r_op,c_op) && boardState.getPieceAt(r_op, c_op) == curColour) break;
    				pathLength++;
    				r += dirX;
    				c += dirY;
    				parent = cur;
    				parent.add(dirX, dirY); //we have a child in this direction
    				cur = map[r][c];
        			cur.reset();
        			cur.add(-dirX, -dirY);  //we have a parent in the opposition direction
    			}
    			
    			//hit wall in the same direction
    			if(!isLegal(r,c)) numWallsHit++;
    			//opponent block
    			else if(boardState.getPieceAt(r, c) == oppColour) numOppBlock++;
    			//hit empty space
    			else {
    				while(isLegal(r,c) && boardState.getPieceAt(r, c) == Piece.EMPTY) {
    					emptySpaces++;
    					r += dirX;
    					c += dirY;
    				}
    			}
    			
    			//hit wall at the other end 
    			if(!isLegal(r_op, c_op)) numWallsHit++;
    			//opponent block
    			else if(boardState.getPieceAt(r_op, c_op) == oppColour) numOppBlock++;
    			//hit empty space
    			else {
    				while(isLegal(r_op, c_op) && boardState.getPieceAt(r_op, c_op) == Piece.EMPTY) {
    					emptySpaces++;
    					r_op -= dirX;
    					c_op -= dirY;
    				}
    			}
    			checkState(state,pathLength,emptySpaces, numWallsHit, numOppBlock,i,j,dirX,dirY);
    		}
    	}
    	return state;
    }
    
    public static void checkState(State s, int pathLength, int empty, int walls, int opponents, 
    											int coordX, int coordY, int dirX, int dirY) 
    {
    	switch(pathLength) {
    		case(2):
    			s.two.add(new int[] {empty,walls,opponents,coordX,coordY,dirX,dirY});
    		case(3):
    			s.three.add(new int[] {empty,walls,opponents,coordX,coordY,dirX,dirY});
    		case(4):
    			s.four.add(new int[] {empty,walls,opponents,coordX,coordY,dirX,dirY});
    	}
    	
    }

	public static void initMap() {
		// TODO Auto-generated method stub
		for(int i = 0; i < 6; i++) {
    		for(int j = 0; j < 6; j++) {
    			map[i][j] = new Node(i,j);
    		}
		}
	}
	
	
	// Evaluation step
	private static Piece MyPiece(int player) {
    	return player == 0 ? Piece.WHITE : Piece.BLACK;
    }
    
    private static Piece OpponentPiece(int player) {
    	return 1 - player == 0 ? Piece.BLACK : Piece.WHITE;
    }
    
    public static double evaluateBoardState(PentagoBoardState boardState, int player) {
    	
    	Piece myPiece = MyPiece(boardState.firstPlayer());
    	Piece opponentPiece = OpponentPiece(boardState.firstPlayer());
    	double score = 0;
		
		if (boardState.getWinner() == player) {
			return WIN_VALUE;
		}
		else if (boardState.getWinner() == 1 - player) {
			return LOSE_VALUE;
		}
		else if (boardState.getWinner() == Board.DRAW) {
			return 0;
		}
		else {
			// evaluate the state
			// create the board with necessary info
			State state = new State();
			state = processState(boardState, player, state);
			
			double playerScore = evaluatePlayerState(MyTools.map, myPiece, state);
			double opponentScore = evaluatePlayerState(MyTools.map, opponentPiece, state);
			
			score = playerScore - opponentScore;
			//System.out.println("final score " + score);
			return score;
			
		}
	}
    
//    private Node UpdateNodesInALine(int cur_i, int cur_j, int xDir, int yDir, Node node, Node[][] board, Piece opponent) {
////    	if (!(inRange(cur_i+xDir) && inRange(cur_j+yDir))) {
////    		return;
////    	}
////    	else if (board[cur_i + xDir][cur_j + yDir].piece != opponent && board[cur_i + xDir][cur_j + yDir],piece != Piece.EMPTY) {
////    		// blocked 
////    	}
//    	
//    	if (xDir == -1 && yDir == -1) {
//	    	Node nextNode = board[cur_i + xDir][cur_j + yDir];
//	    	if (nextNode.leftDiagonalStatus[1] == 1) {
//	    		
//	    	}
//	    	if (nextNode.piece == opponent) {
//				node.isSinglePiece = false;
//			}
//    	}
//    }
    
    private static double evaluatePlayerState(Node[][] board, Piece target, State state) {
    	
    	double score = 0;
    	
    	double baseScore = 1;
    	double centerFactor = 1;
    	double chainFactor = 1;
    	double swapFactor = 1;
    	boolean center = false;
    	
    	if (state.four.size() > 0) {
	    	for (int[] record : state.four) {
	    		if (record[1] >= 2) {chainFactor = 4;}	//all blocked by wall
	    		else if (record[2] >= 2) {chainFactor = 7;}	//all blocked by player
	    		else if (record[1] == 1 && record[2] == 1) { 	//one wall one oppo
	    			// check if we can swap it
	    			chainFactor = 8;
	    			if (checkPotentialSwapKill(record[3], record[4], record[5], record[6], board, state))
	    				swapFactor = 2;
	    			else
	    				swapFactor = 1;
	    		} 
	    		else {chainFactor = 10;}
				//System.out.println("four in a row:" + chainFactor * centerFactor * swapFactor * baseScore);
	    		score += chainFactor * centerFactor * swapFactor * baseScore;
	    	}
		}
		if (state.three.size() > 0) {
			for (int[] record : state.four) {
				chainFactor = 5;
				// is in the center?
				if (isInTheCenter(board[record[3]][record[4]], record[5], record[6])) {
		    		centerFactor = 2; // a temporary factor
				}
				else {
					centerFactor = 1;
				}
				
				if (record[1] >= 2) {
					chainFactor = 6;
				}
				else if (record[2] >= 2) {
					chainFactor = 4.5;
				}
				else {
					chainFactor = 5;
				}
				// check for instant swap kill
				if (checkPotentialSwapKill(record[3], record[4], record[5], record[6], board, state))
    				swapFactor = 2;
    			else
    				swapFactor = 1;
				//System.out.println("three in a row:" + chainFactor * centerFactor * swapFactor * baseScore);
				
				score += chainFactor * centerFactor * swapFactor * baseScore;
			}
		}
		if (state.two.size() > 0) {
			for (int[] record : state.four) {
				if (isInTheCenter(board[record[3]][record[4]], record[5], record[6])) {
					centerFactor = 2;
				}
				else {
					centerFactor = 1;
				}
				
				if (record[2] >= 2) {	//player
					chainFactor = 2;
				}
				else if (record[1] >= 2) {	//wall
					chainFactor = 2.5;
				}
				else {
					chainFactor = 3;
				}
				
				if (checkPotentialSwapKill(record[3], record[4], record[5], record[6], board, state))
    				swapFactor = 2;
    			else
    				swapFactor = 1;
				//System.out.println("two in a row:" + chainFactor * centerFactor * swapFactor * baseScore);
				
				score += chainFactor * centerFactor * swapFactor * baseScore;
			}
		}
		else {
			chainFactor = 1;
			centerFactor = 1;
//			if (isInTheCenter(n, dir)) {
//				centerFactor = 2;
//			}
//			else {
//				centerFactor = 1;
//			}
			//System.out.println("one in a row:" + chainFactor * centerFactor * swapFactor * baseScore);
			score += chainFactor * centerFactor * swapFactor * baseScore;
		}
    	
    	// check for potential instant kill
    	
    	
    	return score;

    	
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
    }
    
    public static int[][] diagonalFirstCheckList = { {1,1},{1,4},{4,1},{4,4} };
    
    public static int[][] verticalLeftCheckList = {{1,0},{1,3},{4,0},{4,3}};
    public static int[][] verticalMiddleCheckList = {{1,1},{1,4},{4,1},{4,4}};
    public static int[][] verticalRightCheckList = {{1,2},{1,5},{4,2},{4,5}};
    
    public static int[][] horizontalTopCheckList = {{0,1},{0,4},{3,1},{3,4}};
    public static int[][] horizontalMiddleCheckList = {{1,1},{1,4},{4,1},{4,4}};
    public static int[][] horizontalBottomCheckList = {{2,1},{2,4},{5,1},{5,4}};
    
    public static int[][] leftDiagonalSecondCheckList = { {1,0},{0,1},{1,3},{0,4},{4,0},{3,1},{4,3},{3,4} };
    //public static int[][] rightDiagonalCheckList = { {0,2},{0,5},{3,0},{3,5} };
    //public static int[][] rightDiagonalCheckList = { {0,2},{0,5},{3,0},{3,5} };
    
    private static boolean checkPotentialSwapKill(int x, int y, int dx, int dy, Node[][] board, State state){
    	Direction d = getDirection(dx, dy);
    	switch (d) {
    	case Vertical:
    		
    		break;
    	case Horizontal:
    		
    		break;
    	case LeftD:
    		int count = 0;
    		for (int i=0; i<diagonalFirstCheckList.length; i++) {
    			int _x = diagonalFirstCheckList[i][0];
    			int _y = diagonalFirstCheckList[i][1];
    			//if (diagonalFirstCheckList[_x][_y] )
    		}
    		break;
    	case RightD:
   
    		break;
    	default:
    		break;
    	}
    	return false;
    }
    
    private static boolean isSinglePiece(Node n) {
    	return false;
    }
    
    private static Direction getDirection(int dirX, int dirY) {
    	if(dirX == 0 && dirY == 1)        return Direction.Horizontal;
		else if (dirX == 0 && dirY == -1) return Direction.Horizontal;
		else if (dirX == 1 && dirY == 0)  return Direction.Vertical;
		else if (dirX == -1 && dirY == 0) return Direction.Vertical;
		else if (dirX == 1 && dirY == 1)  return Direction.LeftD;
		else if (dirX == 1 && dirY == -1) return Direction.RightD;
		else if (dirX == -1 && dirY == 1) return Direction.RightD;
		else if (dirX == -1 && dirY == -1)return Direction.LeftD;
		else return null;
    }
    
    private static boolean isInTheCenter(Node n, int dirX, int dirY) {
    	Direction d = getDirection(dirX, dirY);
    	if (isSinglePiece(n)) {
    		return ((n.x == 1 && n.y == 1) || 
    				(n.x == 4 && n.y == 1) || 
    				(n.x == 1 && n.y == 4) || 
    				(n.x == 4 && n.y == 4));
    	}
    	else {
    	switch (d) {
	    	case Vertical:
	    		return (n.y == 1 || n.y == 4);
	    	case Horizontal:
	    		return (n.x == 1 || n.x == 4);
	    	case LeftD:
	    		return ((n.x == n.y) || (n.x - n.y == 3) || (n.y - n.x == 3));
	    	case RightD:
	    		return ((n.x == 5 - n.y) || (n.x + n.y == 2) || (n.x + n.y == 8));
	    	default:
	    		return false;
	    	}
    	}
    }
    
    private boolean inRange(int c) {
    	return (c >= 0 && c < 6);
    }

}