package pentago_swap;

import boardgame.Move;
import pentago_swap.PentagoBoardState.Quadrant;


/**
 * @author mgrenander
 */
public class PentagoMove extends Move {
    private int playerId;
    private int xMove;
    private int yMove;
    private Quadrant aSwap;
    private Quadrant bSwap;
    private boolean fromBoard;

    public PentagoMove(PentagoCoord coord, Quadrant aSwap, Quadrant bSwap, int playerId) {
        this(coord.getX(), coord.getY(), aSwap, bSwap, playerId);
    }

    public PentagoMove(int x, int y, Quadrant aSwap, Quadrant bSwap, int playerId) {
        this.playerId = playerId;
        this.xMove = x;
        this.yMove = y;
        this.aSwap = aSwap;
        this.bSwap = bSwap;
        this.fromBoard = false;
    }

    public PentagoMove(String formatString) {
        String[] components = formatString.split(" ");
        try {
            this.xMove = Integer.parseInt(components[0]);
            this.yMove = Integer.parseInt(components[1]);
            this.aSwap = Quadrant.valueOf(components[2]);
            this.bSwap = Quadrant.valueOf(components[3]);
            this.playerId = Integer.parseInt(components[4]);
            this.fromBoard = false;
        } catch(IllegalArgumentException e) {
            throw new IllegalArgumentException("Received an uninterpretable string format for a PentagoMove.");
        }
    }

    // Getters
    public PentagoCoord getMoveCoord() { return new PentagoCoord(this.xMove, this.yMove); }
    public Quadrant getASwap() { return this.aSwap; }
    public Quadrant getBSwap() { return this.bSwap; }

    // Fetch player's name
    public String getPlayerName(int player) {
        if (playerId != PentagoBoardState.BLACK && playerId != PentagoBoardState.WHITE) {
            return "Illegal";
        }
        return player == PentagoBoardState.WHITE ? "White" : "Black";
    }

    // Fetch the current player name
    public String getPlayerName() {
        return getPlayerName(this.playerId);
    }

    // Server methods
    @Override
    public int getPlayerID() { return this.playerId; }

    @Override
    public void setPlayerID(int playerId) { this.playerId = playerId; }

    @Override
    public void setFromBoard(boolean fromBoard) { this.fromBoard = fromBoard; }

    @Override
    public boolean doLog() { return true; }

    @Override
    public String toPrettyString() {
        return String.format("Player %d, Move: (%d, %d), Swap: (%s, %s)", playerId, xMove, yMove, aSwap, bSwap);
    }

    @Override
    public String toTransportable() {
        return String.format("%d %d %s %s %d", xMove, yMove, aSwap, bSwap, playerId);
    }
}
