package student_player_zero;

import pentago_swap.PentagoBoardState;
import pentago_swap.PentagoBoardState.Piece;
public class MyTools{
	public static class State{
		double two;
		double threetwo;
		double threethree;
		double fourone;
		double fourtwo;
		public double[] getNum(){
			double[] result = {two, threetwo, threethree, fourone, fourtwo};
			return result;
			}
		public void increase(int input, int avail, double value){
			if(input ==2){
				two+=value;
			}
			else if(input ==3){
				if(avail ==2){
					threetwo+=value;
				}
				else if(avail ==3){
					threethree+=value;
				}
			}
			else if(input ==4){
				if(avail ==2){
					fourtwo+=value;
				}
				else if(avail ==1){
					fourone+=value;
				}
			}
			else{
				return;
			}
		}
	}
	public static double[][] checkState(PentagoBoardState currentBoard, int player){
		State whiteState = new State();
		State blackState = new State();
		State[] states = {whiteState, blackState};
		for(int i=0; i<6; i++){
			checkHorizontal(i, currentBoard, whiteState, blackState);
			checkVertical(i, currentBoard, whiteState, blackState);
		}
		checkDiagRight(0,0,currentBoard, whiteState, blackState, false);
		checkDiagRight(0,1,currentBoard, whiteState, blackState, true);
		checkDiagRight(1,0,currentBoard, whiteState, blackState, true);
		checkDiagLeft(0,5,currentBoard, whiteState, blackState, false);
		checkDiagLeft(0,4,currentBoard, whiteState, blackState, true);
		checkDiagLeft(1,4,currentBoard, whiteState, blackState, true);
		double[][] result = {whiteState.getNum(), blackState.getNum()};
		return  result;
	}
	
	public static void checkHorizontal(int rowNum, PentagoBoardState currentBoard, State whiteState, State blackState){
		int white = 0;
		int avail = 0;
		int black = 0;
		for(int i=0; i<6; i++){
			Piece currentPiece = currentBoard.getPieceAt(rowNum, i);
			if(currentPiece == Piece.WHITE){
				white++;
			}
			else if(currentPiece == Piece.BLACK){
				black++;
			}
			else{
				avail++;
			}
		}
		if(white == 0){
			blackState.increase(black, avail, 1);
		}
		else if(black ==0){
			whiteState.increase(white, avail, 1);
		}
		else{
			if(white + avail>= 5){
				whiteState.increase(white, avail, 1);
			}
			else if(black + avail >= 5){
				blackState.increase(black, avail, 1);
			}
		}
	}
	
	public static void checkVertical(int colNum, PentagoBoardState currentBoard, State whiteState, State blackState){
		int white = 0;
		int avail = 0;
		int black = 0;
		for(int i=0; i<6; i++){
			Piece currentPiece = currentBoard.getPieceAt(i, colNum);
			if(currentPiece == Piece.WHITE){
				white++;
			}
			else if(currentPiece == Piece.BLACK){
				black++;
			}
			else{
				avail++;
			}
		}
		if(white == 0){
			blackState.increase(black, avail, 1);
		}
		else if(black ==0){
			whiteState.increase(white, avail, 1);
		}
		else{
			if(white + avail>= 5){
				whiteState.increase(white, avail, 1);
			}
			else if(black + avail >= 5){
				blackState.increase(black, avail, 1);
			}
			else{
				//later
			}
			}
		}
	
	public static void checkDiagRight(int rowNum, int colNum, PentagoBoardState currentBoard, State whiteState, State blackState,Boolean initial){
		int white = 0;
		int black = 0;
		int avail = 0;
		for(int i= 0; i<6; i++){
				Piece currentPiece = currentBoard.getPieceAt(rowNum, colNum);
				if(currentPiece == Piece.WHITE){
					white++;
				}
				else if(currentPiece == Piece.BLACK){
					black++;
				}
				else{
					avail++;
				}
				
				if(rowNum<5 && colNum<5){
					rowNum++;
					colNum++;
				}
				else{
					break;
				}
		}
		if(initial){
			if(white== 0){
				blackState.increase(black+1, avail, 2);
			}
			else if(black ==0){
				whiteState.increase(white+1, avail, 2);
			}
			else{
				if(white + avail>=5){
					whiteState.increase(white, avail, 2);
				}
				else if(black + avail >=5){
					blackState.increase(black, avail, 2);
				}
				else{
					//later
				}
			}
		}
		else{
		if(white== 0){
			blackState.increase(black, avail, 2);
		}
		else if(black ==0){
			whiteState.increase(white, avail, 2);
		}
		else{
			if(white + avail>=5){
				whiteState.increase(white, avail, 2);
			}
			else if(black + avail >=5){
				blackState.increase(black, avail, 2);
			}
			else{
				//later
			}
		}
		}
	}
	
	public static void checkDiagLeft(int rowNum, int colNum, PentagoBoardState currentBoard, State whiteState, State blackState,Boolean initial){
		int white = 0;
		int black = 0;
		int avail = 0;
		for(int i= 0; i<6; i++){
				Piece currentPiece = currentBoard.getPieceAt(rowNum, colNum);
				if(currentPiece == Piece.WHITE){
					white++;
				}
				else if(currentPiece == Piece.BLACK){
					black++;
				}
				else{
					avail++;
				}
				if(rowNum<5 && colNum<5){
					rowNum++;
					colNum++;
				}
				else{
					break;
				}
			}
		//System.out.println("white" + white);
		//System.out.println("black" + black);
		//System.out.println("avail" + avail);
		if(initial){
			if(white== 0){
				blackState.increase(black+1, avail, 2);
			}
			else if(black ==0){
				whiteState.increase(white+1, avail, 2);
			}
			else{
				if(white + avail>=5){
					whiteState.increase(white, avail, 2);
				}
				else if(black + avail >=5){
					blackState.increase(black, avail, 2);
				}
				else{
					//later
				}
			}
		}
		else{
		if(white== 0){
			blackState.increase(black, avail, 2);
		}
		else if(black ==0){
			whiteState.increase(white, avail, 2);
		}
		else{
			if(white + avail>=5){
				whiteState.increase(white, avail, 2);
			}
			else if(black + avail >=5){
				blackState.increase(black, avail, 2);
			}
			else{
				//later
			}
		}
	}
	}
}

