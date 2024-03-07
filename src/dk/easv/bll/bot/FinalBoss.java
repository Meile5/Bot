package dk.easv.bll.bot;

import dk.easv.bll.field.IField;
import dk.easv.bll.game.GameState;
import dk.easv.bll.game.IGameState;
import dk.easv.bll.move.IMove;

import java.sql.SQLOutput;
import java.util.*;

import static dk.easv.bll.field.IField.AVAILABLE_FIELD;
import static dk.easv.bll.game.GameManager.isWin;

public class FinalBoss implements IBot {
    final int moveTimeMs = 1000;
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
        return calculateWinningMove(state);
    }

    private IMove calculateWinningMove(IGameState state){
        int maxDepth = 7;

        List<IMove> availableMoves = state.getField().getAvailableMoves();

        IMove bestMove = null;
        int bestScore = Integer.MIN_VALUE;

        for (IMove move : availableMoves) {
            // Clone the current state
            GameSimulator simulator = createSimulator(state);
            simulator.updateGame(move);

            // Evaluate the move using minimax
            int score = simulator.minimax(simulator, maxDepth, Integer.MIN_VALUE, Integer.MAX_VALUE, true, availableMoves);

            // If the score is better than the current best, update the best move
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
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
        return BOT_NAME;
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
        private int currentPlayer = 0; //player0 == 0 && player1 == 1
        private int player;
        private int opponent;
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
                    macroBoard[macroX][macroY].equals(AVAILABLE_FIELD)) {

                String[][] board = getCurrentState().getField().getBoard();

                if (isWin(board, move, "" + currentPlayer))
                    macroBoard[macroX][macroY] = currentPlayer + "";
                else if (isTie(board, move))
                    macroBoard[macroX][macroY] = "TIE";

                //Check macro win
                if (isWin(macroBoard,new Move(macroX, macroY), "" + currentPlayer))
                    gameOver = GameOverState.Win;
                else if (isTie(macroBoard,new Move(macroX, macroY)))
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
                    if (board[i][k].equals(AVAILABLE_FIELD) ||
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
                    if (macroBoard[i][k].equals(AVAILABLE_FIELD))
                        macroBoard[i][k] = IField.EMPTY_FIELD;
                }

            int xTrans = move.getX() % 3;
            int yTrans = move.getY() % 3;

            if (macroBoard[xTrans][yTrans].equals(IField.EMPTY_FIELD))
                macroBoard[xTrans][yTrans] = AVAILABLE_FIELD;
            else {
                // Field is already won, set all fields not won to avail.
                for (int i = 0; i < macroBoard.length; i++)
                    for (int k = 0; k < macroBoard[i].length; k++) {
                        if (macroBoard[i][k].equals(IField.EMPTY_FIELD))
                            macroBoard[i][k] = AVAILABLE_FIELD;
                    }
            }
        }

        private int minimax(GameSimulator simulator, int depth, int alpha, int beta, boolean isMaximizing, List<IMove> availableMoves){

            int evaluationOfPosition = 0;

            String[][] board = simulator.getCurrentState().getField().getBoard();

            if (depth == 0 || simulator.getGameOver() != GameOverState.Active) {
                // If at max depth or game over, evaluate the board state
                evaluationOfPosition += evaluate(simulator, availableMoves);
                return evaluationOfPosition;
            }

            int evaluation = evaluate(simulator, availableMoves);

            if (isMaximizing) {
                int bestMax = Integer.MIN_VALUE;
                for (IMove move : availableMoves) {
                    // Current state clone
                    GameSimulator newSimulator = createSimulator(simulator.getCurrentState());
                    newSimulator.updateGame(move);

                    // Pass a sublist of availableMoves to the recursive call
                    List<IMove> nextOpponentAvailableMoves = newSimulator.getCurrentState().getField().getAvailableMoves();

                    int eval = minimax(newSimulator, depth - 1, alpha, beta, false, nextOpponentAvailableMoves);
                    bestMax = Math.max(bestMax, eval);
                    alpha = Math.max(alpha, eval);
                    if (beta <= alpha)
                        break;
                }
                return bestMax + evaluation;
            } else {
                int bestMin = Integer.MAX_VALUE;
                for (IMove move : availableMoves) {
                    // Current state clone
                    GameSimulator newSimulator = createSimulator(simulator.getCurrentState());
                    newSimulator.updateGame(move);

                    // Pass a sublist of availableMoves to the recursive call
                    List<IMove> nextAvailableMoves = newSimulator.getCurrentState().getField().getAvailableMoves();

                    int eval = minimax(newSimulator, depth - 1, alpha, beta, true, nextAvailableMoves);
                    bestMin = Math.min(bestMin, eval);
                    beta = Math.min(beta, eval);
                    if (beta <= alpha)
                        break;
                }
                return bestMin - evaluation;
            }
        }


        private int evaluate(GameSimulator simulator, List<IMove> availableMoves) {
            List<IMove> moves = availableMoves;
            if(!moves.isEmpty()) {
                int localX = moves.get(0).getX() % 3;
                int localY = moves.get(0).getY() % 3;
                int startX = moves.get(0).getX() - (localX);
                int startY = moves.get(0).getY() - (localY);

                int x = moves.get(0).getX();
                int y = moves.get(0).getY();

                //do for loop with the board and the current available moves, one by one

                String[][] board = simulator.getCurrentState().getField().getBoard();
                int score = 0;


                for (int row = startX; row < startX + 2; row++) {
                    score += evaluateLine(board[row][startY], board[row][startY + 1], board[row][startY + 2]);
                }


                for (int col = startY; col < startY + 3; col++) {
                    score += evaluateLine(board[startX][col], board[startX + 1][col], board[startX + 2][col]);
                }


                score += evaluateLine(board[startX][startY], board[startX + 1][startY + 1], board[startX + 2][startY + 2]);
                score += evaluateLine(board[startX][startY + 2], board[startX + 1][startY + 1], board[startX + 2][startY]);

                return score;
            }
            return 0;
        }

        private int evaluateLine(String cell1, String cell2, String cell3) {
            int score = 0;

            // Count the number of X's and O's in the line
            int playerCount = 0;
            int opponentCount = 0;
            for (String cell : new String[]{cell1, cell2, cell3}) {
                if (cell.equals(Integer.toString(player))) {
                    playerCount++;
                } else if (cell.equals(Integer.toString(opponent))) {
                    opponentCount++;
                }
            }

            // Assign scores based on the presence of player's and opponent's marks in the line
            if (playerCount == 3) {
                score += 1000;
                return score; // Player wins the line
            } else if (opponentCount == 3) {
                score -= 1000; // Opponent wins the line
                return score;
            } else if (playerCount == 2 && opponentCount == 0) {
                score += 100; // Two in a row for player
                return score;
            } else if (opponentCount == 2 && playerCount == 0) {
                score -= 100; // Two in a row for opponent, consider blocking
                return score;
            } else if (playerCount == 1 && opponentCount == 0) {
                score += 10; // One in a row for player
                return score;
            } else if (opponentCount == 1 && playerCount == 0) {
                score -= 10; // One in a row for opponent
                return score;
            } else if (playerCount == 0 && opponentCount == 1) {
                score -= 5; // Opponent has one in a row, consider blocking
                return score;
            }
            return score;
        }
    }
}