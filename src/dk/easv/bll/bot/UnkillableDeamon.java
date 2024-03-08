package dk.easv.bll.bot;

import dk.easv.bll.field.IField;
import dk.easv.bll.game.GameState;
import dk.easv.bll.game.IGameState;
import dk.easv.bll.move.IMove;

import java.util.List;
import java.util.Objects;

public class UnkillableDeamon implements IBot {
    final int moveTimeMs = 900;
    private String BOT_NAME = getClass().getSimpleName();



    private GameSimulator createSimulator(IGameState state) {
        GameSimulator simulator = new GameSimulator(new GameState());
        simulator.setGameOver(GameOverState.Active);
        simulator.setCurrentPlayer(state.getMoveNumber() % 2);
        simulator.getCurrentState().setRoundNumber(state.getRoundNumber());
        simulator.getCurrentState().setMoveNumber(state.getMoveNumber());
        simulator.getCurrentState().getField().setBoard(state.getField().getBoard());
        simulator.getCurrentState().getField().setMacroboard(state.getField().getMacroboard());
        return simulator;
    }


    @Override
    public IMove doMove(IGameState state) {
        long startTime = System.currentTimeMillis();
        IMove move = calculateWinningMove(state, moveTimeMs);
        long endTime = System.currentTimeMillis();
        long totalTimeTaken = endTime - startTime;
        //System.out.println("Total time taken for the move: " + totalTimeTaken + "ms");
        return move;
    }

    private IMove calculateWinningMove(IGameState state, int maxTimeMs) {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + maxTimeMs;
        int depth = 1; // Start with a depth of 1

        IMove bestMove = null;
        GameSimulator simulator = createSimulator(state);

        // Iteratively increase the depth until time runs out or the maximum time is reached
        while (System.currentTimeMillis() < endTime && System.currentTimeMillis() - startTime < maxTimeMs) {
            IMove currentBestMove = simulator.findBestMove(simulator, depth);
            if (currentBestMove != null) {
                bestMove = currentBestMove;
            }
            depth++; // Increase the depth for the next iteration
        }

        long totalTimeTaken = System.currentTimeMillis() - startTime;
        //System.out.println("Total time taken: " + totalTimeTaken + "ms");

        // Return the best move found within the time limit
        return bestMove;
    }




    /*
        The code below is a simulator for simulation of gameplay. This is needed for AI.

        It is put here to make the Bot independent of the GameManager and its subclasses/enums

        Now this class is only dependent on a few interfaces: IMove, IField, and IGameState

        You could say it is self-contained. The drawback is that if the game rules change, the simulator must be
        changed accordingly, making the code redundant.

     */

    @Override
    public String getBotName() {
        return "Unkillable Deamon";
    }

    public enum GameOverState {
        Active,
        Win,
        Tie
    }

    public class Move implements IMove {
        int x = 0;
        int y = 0;

        public Move(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public Move() {

        }

        @Override
        public int getX() {
            return x;
        }

        @Override
        public int getY() {
            return y;
        }


        @Override
        public String toString() {
            return "(" + x + "," + y + ")";
        }

       @Override
       public boolean equals(Object o) {
           if (this == o) return true;
           if (o == null || getClass() != o.getClass()) return false;
           Move move = (Move) o;
           return x == move.x && y == move.y;
       }
//
        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }



     class GameSimulator {
        private final IGameState currentState;
        private  int currentPlayer = 0; //player0 == 0 && player1 == 1
        private  int player;
        private  int opponent;
        private volatile GameOverState gameOver = GameOverState.Active;

        public void setGameOver(GameOverState state) {
            gameOver = state;
        }

        public GameOverState getGameOver() {
            return gameOver;
        }

        public void setCurrentPlayer(int player) {
            currentPlayer = player;
        }

        public IGameState getCurrentState() {
            return currentState;
        }

        public GameSimulator(IGameState currentState) {
            this.currentState = currentState;
        }

        public Boolean updateGame(IMove move) {
            if (!verifyMoveLegality(move))
                return false;

            updateBoard(move);
            currentPlayer = (currentPlayer + 1) % 2;

            return true;
        }

        private Boolean verifyMoveLegality(IMove move) {
            IField field = currentState.getField();
            boolean isValid = field.isInActiveMicroboard(move.getX(), move.getY());

            if (isValid && (move.getX() < 0 || 9 <= move.getX())) isValid = false;
            if (isValid && (move.getY() < 0 || 9 <= move.getY())) isValid = false;

            if (isValid && !field.getBoard()[move.getX()][move.getY()].equals(IField.EMPTY_FIELD))
                isValid = false;

            return isValid;
        }

        private void updateBoard(IMove move) {
            String[][] board = currentState.getField().getBoard();
            board[move.getX()][move.getY()] = currentPlayer + "";
            currentState.setMoveNumber(currentState.getMoveNumber() + 1);
            if (currentState.getMoveNumber() % 2 == 0) {
                currentState.setRoundNumber(currentState.getRoundNumber() + 1);
            }
            checkAndUpdateIfWin(move);
            updateMacroboard(move);

        }

        private void checkAndUpdateIfWin(IMove move) {
            String[][] macroBoard = currentState.getField().getMacroboard();
            int macroX = move.getX() / 3;
            int macroY = move.getY() / 3;

            if (macroBoard[macroX][macroY].equals(IField.EMPTY_FIELD) ||
                    macroBoard[macroX][macroY].equals(IField.AVAILABLE_FIELD)) {

                String[][] board = getCurrentState().getField().getBoard();

                if (isWin(board, move, "" + currentPlayer))
                    macroBoard[macroX][macroY] = currentPlayer + "";
                else if (isTie(board, move))
                    macroBoard[macroX][macroY] = "TIE";

                //Check macro win
                if (isWin(macroBoard, new Move(macroX, macroY), "" + currentPlayer))
                    gameOver = GameOverState.Win;
                else if (isTie(macroBoard, new Move(macroX, macroY)))
                    gameOver = GameOverState.Tie;
            }

        }

        private boolean isTie(String[][] board, IMove move) {
            int localX = move.getX() % 3;
            int localY = move.getY() % 3;
            int startX = move.getX() - (localX);
            int startY = move.getY() - (localY);

            for (int i = startX; i < startX + 3; i++) {
                for (int k = startY; k < startY + 3; k++) {
                    if (board[i][k].equals(IField.AVAILABLE_FIELD) ||
                            board[i][k].equals(IField.EMPTY_FIELD))
                        return false;
                }
            }
            return true;
        }


        public boolean isWin(String[][] board, IMove move, String currentPlayer) {
            int localX = move.getX() % 3;
            int localY = move.getY() % 3;
            int startX = move.getX() - (localX);
            int startY = move.getY() - (localY);

            //check col
            for (int i = startY; i < startY + 3; i++) {
                if (!board[move.getX()][i].equals(currentPlayer))
                    break;
                if (i == startY + 3 - 1) return true;
            }

            //check row
            for (int i = startX; i < startX + 3; i++) {
                if (!board[i][move.getY()].equals(currentPlayer))
                    break;
                if (i == startX + 3 - 1) return true;
            }

            //check diagonal
            if (localX == localY) {
                //we're on a diagonal
                int y = startY;
                for (int i = startX; i < startX + 3; i++) {
                    if (!board[i][y++].equals(currentPlayer))
                        break;
                    if (i == startX + 3 - 1) return true;
                }
            }

            //check anti diagonal
            if (localX + localY == 3 - 1) {
                int less = 0;
                for (int i = startX; i < startX + 3; i++) {
                    if (!board[i][(startY + 2) - less++].equals(currentPlayer))
                        break;
                    if (i == startX + 3 - 1) return true;
                }
            }
            return false;
        }

        private void updateMacroboard(IMove move) {
            String[][] macroBoard = currentState.getField().getMacroboard();
            for (int i = 0; i < macroBoard.length; i++)
                for (int k = 0; k < macroBoard[i].length; k++) {
                    if (macroBoard[i][k].equals(IField.AVAILABLE_FIELD))
                        macroBoard[i][k] = IField.EMPTY_FIELD;
                }

            int xTrans = move.getX() % 3;
            int yTrans = move.getY() % 3;

            if (macroBoard[xTrans][yTrans].equals(IField.EMPTY_FIELD))
                macroBoard[xTrans][yTrans] = IField.AVAILABLE_FIELD;
            else {
                // Field is already won, set all fields not won to avail.
                for (int i = 0; i < macroBoard.length; i++)
                    for (int k = 0; k < macroBoard[i].length; k++) {
                        if (macroBoard[i][k].equals(IField.EMPTY_FIELD))
                            macroBoard[i][k] = IField.AVAILABLE_FIELD;
                    }
            }
        }

         private String[][] getBoard(GameSimulator simulator,int startX,int startY){


             String[][] BigBoard = simulator.getCurrentState().getField().getBoard();

             String[][] board = new String[3][3];

             board[0][0] = BigBoard[startX][startY];
             board[0][1] = BigBoard[startX][startY +1];
             board[0][2] = BigBoard[startX][startY +2];

             board[1][0] = BigBoard[startX+1][startY];
             board[1][1] = BigBoard[startX+1][startY +1];
             board[1][2] = BigBoard[startX+1][startY +2];

             board[2][0] = BigBoard[startX+2][startY];
             board[2][1] = BigBoard[startX+2][startY +1];
             board[2][2] = BigBoard[startX+2][startY +2];

             return board;
         }


        private Move findBestMove(GameSimulator simulator,int depth)
        {
            player = currentPlayer;
            opponent = (player == 0) ? 1 : 0;

            Move bestMove = new Move();
            bestMove.x = -1;
            bestMove.y = -1;
            List<IMove> moves = simulator.getCurrentState().getField().getAvailableMoves();
            int localX = moves.get(0).getX() % 3;
            int localY = moves.get(0).getY() % 3;
            int startX = moves.get(0).getX() - (localX);
            int startY = moves.get(0).getY() - (localY);

            String[][] board = getBoard(simulator,startX, startY);
            Move returnedMove = findMove(board);
            bestMove.x = returnedMove.x + startX;
            bestMove.y = returnedMove.y + startY;

            return bestMove;
        }

         private Boolean isMovesLeft(String board[][])
         {
             for (int i = 0; i < 3; i++)
                 for (int j = 0; j < 3; j++)
                     if (board[i][j].equals("."))
                         return true;
             return false;
         }


         private Move findMove(String board[][]) {
             int bestVal = Integer.MIN_VALUE;
             int alpha = Integer.MIN_VALUE;
             int beta = Integer.MAX_VALUE;
             Move bestMove = new Move();
             bestMove.x = -1;
             bestMove.y = -1;

             // Traverse all cells, evaluate minimax function
             // for all empty cells. And return the cell
             // with optimal value.
             for (int i = 0; i < 3; i++) {
                 for (int j = 0; j < 3; j++) {
                     // Check if cell is empty
                     if (board[i][j].equals(".")) {
                         // Make the move
                         board[i][j] = (Integer.toString(player));

                         // compute evaluation function for this
                         // move with alpha-beta pruning.
                         int moveVal = minimax(board, 6, alpha, beta, false);

                         // Undo the move
                         board[i][j] = (".");

                         // If the value of the current move is
                         // more than the best value, then update
                         if (moveVal > bestVal) {
                             bestMove.x = i;
                             bestMove.y = j;
                             bestVal = moveVal;
                         }
                         // Update alpha
                         alpha = Math.max(alpha, bestVal);
                     }
                 }
             }
             return bestMove;
         }

         private int evaluate(String[][] board) {
             int score = 0;

             // Check rows for potential wins/losses and blocking moves
             for (int i = 0; i < 3; i++) {
                 if (board[i][0].equals(board[i][1]) && board[i][1].equals(board[i][2])) {
                     if (board[i][0].equals(Integer.toString(player))) score += 100;
                     else if (board[i][0].equals(Integer.toString(opponent))) score -= 100;
                 } else if (board[i][0].equals(Integer.toString(opponent)) && board[i][1].equals(Integer.toString(opponent)) && board[i][2].equals(".")) {
                     score -= 50; // Blocking opponent's potential win
                 }
             }

             // Check columns for potential wins/losses and blocking moves
             for (int i = 0; i < 3; i++) {
                 if (board[0][i].equals(board[1][i]) && board[1][i].equals(board[2][i])) {
                     if (board[0][i].equals(Integer.toString(player)) || board[1][i].equals(Integer.toString(player)) || board[2][i].equals(Integer.toString(player))) score += 100;
                     else if (board[0][i].equals(Integer.toString(opponent))) score -= 100;
                 } else if (board[0][i].equals(Integer.toString(opponent)) && board[1][i].equals(Integer.toString(opponent)) && board[2][i].equals(".")) {
                     score -= 50; // Blocking opponent's potential win
                 }
             }

             // Check diagonals for potential wins/losses and blocking moves
             if (board[0][0].equals(board[1][1]) && board[1][1].equals(board[2][2])) {
                 if (board[0][0].equals(Integer.toString(player))) score += 100;
                 else if (board[0][0].equals(Integer.toString(opponent))) score -= 100;
             } else if (board[0][0].equals(Integer.toString(opponent)) && board[1][1].equals(Integer.toString(opponent)) && board[2][2].equals(".")) {
                 score -= 50; // Blocking opponent's potential win
             }

             if (board[0][2].equals(board[1][1]) && board[1][1].equals(board[2][0])) {
                 if (board[0][2].equals(Integer.toString(player))) score += 100;
                 else if (board[0][2].equals(Integer.toString(opponent))) score -= 100;
             } else if (board[0][2].equals(Integer.toString(opponent)) && board[1][1].equals(Integer.toString(opponent)) && board[2][0].equals(".")) {
                 score -= 50; // Blocking opponent's potential win
             }

             // Check for potential forks for the player
             if ((board[0][0].equals(Integer.toString(player)) && board[2][2].equals(Integer.toString(player))) ||
                     (board[0][2].equals(Integer.toString(player)) && board[2][0].equals(Integer.toString(player)))) {
                 score += 40;
             }

             // Check for potential forks for the opponent
             if ((board[0][0].equals(Integer.toString(opponent)) && board[2][2].equals(Integer.toString(opponent))) ||
                     (board[0][2].equals(Integer.toString(opponent)) && board[2][0].equals(Integer.toString(opponent)))) {
                 score -= 40;
             }

             // Check for winning moves
             if (isWinningMove(board, player)) {
                 score += 1000; // Very high score for immediate win
             }

             // Check for blocking opponent's winning moves
             if (isWinningMove(board, opponent)) {
                 score -= 1000; // Very high penalty for allowing opponent's immediate win
             }
             return score;
         }

         // Helper function to check if a player has a winning move
         private boolean isWinningMove(String[][] board, int player) {
             // Check rows, columns, and diagonals for a winning move
             for (int i = 0; i < 3; i++) {
                 if ((board[i][0].equals(Integer.toString(player))) && board[i][1].equals(Integer.toString(player)) && board[i][2].equals(Integer.toString(player))||
                         (board[0][i].equals(Integer.toString(player)) && board[1][i].equals(Integer.toString(player)) && board[2][i].equals(Integer.toString(player)))) {
                     return true;
                 }
             }
             // Check diagonals
             if ((board[0][0].equals(Integer.toString(player)) && board[1][1].equals(Integer.toString(player)) && board[2][2].equals(Integer.toString(player))) ||
                     (board[0][2].equals(Integer.toString(player))&& board[1][1].equals(Integer.toString(player)) && board[2][0].equals(Integer.toString(player)))) {
                 return true;
             }
             return false;
         }


         private int minimax(String board[][], int depth, int alpha, int beta, boolean isMax) {
             int score = evaluate(board);

             // If Maximizer has won the game
             // return his/her evaluated score
             if (score >= 100)
                 return score;
//
             //// If Minimizer has won the game
             //// return his/her evaluated score
             if (score <= -100)
                 return score;
//
             //// If there are no more moves and
             //// no winner then it is a tie
             if (isMovesLeft(board) == false)
                 return 0;
//
             // If this maximizer's move
             if (isMax) {
                 int best = Integer.MIN_VALUE;

                 // Traverse all cells
                 for (int i = 0; i < 3; i++) {
                     for (int j = 0; j < 3; j++) {
                         // Check if cell is empty
                         if (board[i][j].equals(".")) {
                             // Make the move
                             board[i][j] = (Integer.toString(player));

                             // Call minimax recursively and choose
                             // the maximum value
                             best = Math.max(best, minimax(board, depth - 1, alpha, beta, !isMax));

                             // Undo the move
                             board[i][j] = ".";

                             // Update alpha
                             alpha = Math.max(alpha, best);

                             // Alpha-beta pruning
                             if (beta <= alpha)
                                 break;
                         }
                     }
                 }
                 return best;
             }

             // If this minimizer's move
             else {
                 int best = Integer.MAX_VALUE;

                 // Traverse all cells
                 for (int i = 0; i < 3; i++) {
                     for (int j = 0; j < 3; j++) {
                         // Check if cell is empty
                         if (board[i][j].equals(".")) {
                             // Make the move
                             board[i][j] = (Integer.toString(opponent));

                             // Call minimax recursively and choose
                             // the minimum value
                             best = Math.min(best, minimax(board, depth - 1, alpha, beta, !isMax));

                             // Undo the move
                             board[i][j] = (".");

                             // Update beta
                             beta = Math.min(beta, best);

                             // Alpha-beta pruning
                             if (beta <= alpha)
                                 break;
                         }
                     }
                 }
                 return best;
             }
         }
     }


}
