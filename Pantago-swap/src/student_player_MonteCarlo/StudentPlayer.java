package student_player_MonteCarlo;

import boardgame.Board;
import boardgame.Move;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.lang.*;
import java.sql.Time;

import pentago_swap.PentagoPlayer;
import pentago_swap.PentagoBoardState;
import pentago_swap.PentagoBoardState.Piece;
import pentago_swap.PentagoBoardState.Quadrant;
import student_player_v1.StudentPlayer.MCTNode;
import pentago_swap.PentagoMove;

/** A player file submitted by a student. */
public class StudentPlayer extends PentagoPlayer {
	public static final double UCT_FACTOR = 1.414;
	public static final int MOVE_TIMELIMIT = 1850;
	public static final int FIRST_MOVE_TIMELIMIT = 29950;
	public static long max_prun = 0;
	public static long min_prun = 0;
	public static long start_time;
	public static double epsilon = 1e-6;
	Random r = new Random();
	int depth;
	
	static int[][] centerCoord = { {1,1},{1,4},{4,1},{4,4} };

    /**
     * You must modify this constructor to return your student number. This is
     * important, because this is what the code that runs the competition uses to
     * associate you with your agent. The constructor should do nothing else.
     */
    public StudentPlayer() {
        super("260728557");
    }
    
    public class SearchTree {
    	private Node root;
    	
    	public SearchTree(Node r) {
    		root = r;
    	}
    	public SearchTree() { 
    		root = null;
    	}
    	
    	public Node root() {
    		return root;
    	}
    	
    	public void setRoot(Node r) {
    		root = r;
    	}
    }
    
    public class Node {
    	public int turnPlayer;
    	public double total;
    	public double win;
    	public PentagoBoardState state;
    	public PentagoMove action;
    	public Node parent;
    	public ArrayList<Node> children;
    	public boolean visited;
    	
    	public Node(Node par, int turnPlayer, PentagoBoardState board, PentagoMove move) {
    		total = 0;
    		win = 0;
    		parent = par;
    		state = board;
    		action = move;
    		visited = false;
    		children = new ArrayList<Node>();
    	}
    	
    	public double getWinRate() {
    		if (total == 0) return win/(total+epsilon);
    		else return win/ total;
    	}
    	
    	public double getUCTInChildren(Node node) {
    		try {
    			System.out.println("UCT" + node.getWinRate() + " " + total + " " + (node.total+epsilon) + " ");
    			return node.getWinRate() + UCT_FACTOR * Math.sqrt((Math.log(total)/(node.total + epsilon)));
    			
    			//else return node.getWinRate() + UCT_FACTOR * Math.sqrt((Math.log(total)/(node.total + epsilon)));
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    		return -1;
    	}
    	
		public Node getBestNode() {
			// randomly return a node for now
    		Node ranEntry = (Node)children.get(new Random().nextInt(children.size()));
    		return ranEntry;
    	}
    	
    	// find the one with the most visited number
    	public Node getBestEntry(){
    		double maxRate = -1;
//    		Node bestEntry = null;
//        	for (Node m : children) {
//        		double rate = m.getWinRate();
//        		if (rate > maxRate) {
//        			maxRate = rate;
//        			bestEntry = m;
//        		}
//        	}
    		Node bestEntry = null;
        	for (Node m : children) {
        		double rate = m.total;
        		if (rate > maxRate) {
        			maxRate = rate;
        			bestEntry = m;
        		}
        	}
        	return bestEntry;
    	}
    }
    
   
    // apply UCT until reach the bottom
    public Node SelectPromisingNode(Node rootNode) {
    	Node node = rootNode;
        while (node.children.size() != 0) {
            node = FindBestNodeWithUCT(node);
            depth++;
        }
        return node;
    }
    
    public Node FindBestNodeWithUCT(Node node) {
    	double maxUCT = -1;
    	Node maxNode = null;
    	for (Node m : node.children) {
    		double uct = node.getUCTInChildren(m);
    		if (uct > maxUCT) {
    			maxUCT = uct;
    			maxNode = m;
    		}
    	}
    	System.out.println("max UCT " + maxUCT + " win/total " + maxNode.win + "/" + maxNode.total);
    	return maxNode;
    }
    
    public void expandNode(Node parent) {
    	int turn_player = 0;
    	ArrayList<PentagoMove> possibleMoves = parent.state.getAllLegalMoves();
    	for (PentagoMove m : possibleMoves) {
    		PentagoBoardState newState = (PentagoBoardState)parent.state.clone();
    		newState.processMove(m);
    		Node child = new Node(parent, newState.getTurnPlayer(), newState, m);
    		turn_player = newState.getTurnPlayer();
    		parent.children.add(child);
    	}
    	System.out.println("Expand node: "+ parent.children.size() + " for player " +  turn_player);
    }
    
    public int simulateRandomPlay(Node n) {
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
    
    public void updateNodes(Node leaf, int res) {
    	System.out.println("backtracking, result" + res);
    	Node cur = leaf;
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
    		tree.setRoot(new Node(null, player_id, (PentagoBoardState)board.clone(), null));
    	}
    	
    	int count = 0;
    	while (System.currentTimeMillis() - start_time < MOVE_TIMELIMIT) {
    		depth = 0;
	    	// selection
	    	Node nodeToExpand = SelectPromisingNode(tree.root());
	    	System.out.println("depth " + depth);
	    	// expansion / rollout
	    	if (nodeToExpand.state.getWinner() == Board.NOBODY) {
	    		expandNode(nodeToExpand);// fully expanded
	    	}
	    	Node nodeToSearch;
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
    	Node entryToSelect = tree.root().getBestEntry();
    	
    	System.out.println("WinRate:" + entryToSelect.win + "/" + entryToSelect.total);
    	tree.setRoot(entryToSelect);
    	return entryToSelect.action;
    }
    
    

    /**
     * This is the primary method that you need to implement. The ``boardState``
     * object contains the current state of the game, which your agent must use to
     * make decisions.
     */
    public Move chooseMove(PentagoBoardState boardState) {
        // You probably will make separate functions in MyTools.
        // For example, maybe you'll need to load some pre-processed best opening
        // strategies...
    	start_time = System.currentTimeMillis();
    	
    	Move myMove = null;
    	if (boardState.getTurnNumber() < 2) {
    		for (int i=0; i<centerCoord.length; i++) {
    			if (boardState.getPieceAt(centerCoord[i][0], centerCoord[i][1]) == Piece.EMPTY) {
    				myMove = new PentagoMove(centerCoord[i][0], centerCoord[i][1], Quadrant.TL, Quadrant.TR, player_id);
    			}
    		}
    	}
    	else {
		    myMove = MonteCarloSearch(boardState);  
    	}
    	
    	long endTime = System.currentTimeMillis();
    	long elapseTime = endTime - start_time;
    	System.out.println("Time: " + elapseTime + "\n");

        // Return your move to be processed by the server.
        return myMove;
    }
}