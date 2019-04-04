package student_player_v3;

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
        super("xxx");
        
        //initialize the map
        MyTools.initMap();
    }
    
    public PentagoBoardState lastBoardState;
    public PentagoBoardState secondLastBoardState;
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
    		value = MyTools.evaluateBoardState(b, player_id);
    	}
    }

    
    public MoveValue MiniMax(int depth, int maxDepth, double alpha, double beta, PentagoBoardState boardState, PentagoMove pre_move) {
    	
    	boolean isMyTurn = boardState.getTurnPlayer() == player_id ? true : false;
    	//check if at the max
    	//System.out.println("depth: " + depth);
    	if (boardState.gameOver()) return new MoveValue(0, null);
    	if (depth == maxDepth) return new MoveValue(MyTools.evaluateBoardState(boardState, player_id), null);
    	
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
    	start_time = System.currentTimeMillis();
    	final LinkedList<PentagoMove> candidateMoves = new LinkedList<PentagoMove>();
    	final int time_out = boardState.getTurnNumber() == 0 ? FIRST_MOVE_TIMELIMIT : MOVE_TIMELIMIT;
    	
    	Move myMove = null;
    	if (boardState.getTurnNumber() < 2) {
    		for (int i=0; i<centerCoord.length; i++) {
    			if (boardState.getPieceAt(centerCoord[i][0], centerCoord[i][1]) == Piece.EMPTY) {
    				myMove = new PentagoMove(centerCoord[i][0], centerCoord[i][1], Quadrant.TL, Quadrant.TR, player_id);
    			}
    		}
    	}
//    	else if (boardState.getTurnNumber() > 15) {
//    		myMove = MonteCarloSearch(boardState);
//    	}
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
								optimal = MiniMax(0, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, boardState, candidateMoves.get(1));
						        System.out.println("Value of the move: " + optimal.value);
						        System.out.println("max " + max_prun + ", min " + min_prun);
						        System.out.println("depth " + depth);
							}
							// the first time we set the optimal as the second
					        if (depth == 2) {
					        	candidateMoves.add(optimal.move);
					        	candidateMoves.add(optimal.move);
					        }
					        else {	// else we put the first place to the second place
					        	candidateMoves.set(1, candidateMoves.get(0));
					        	candidateMoves.set(0, optimal.move);
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
    			myMove = candidateMoves.get(0);
			}
    		catch (InterruptedException | ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		finally {
    			executor.shutdownNow();
    		}
    		
    		myMove = candidateMoves.get(0);
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