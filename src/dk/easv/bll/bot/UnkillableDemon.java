package dk.easv.bll.bot;

import dk.easv.bll.field.IField;
import dk.easv.bll.game.GameState;
import dk.easv.bll.game.IGameState;
import dk.easv.bll.move.IMove;
import java.util.List;
import java.util.Objects;
import java.util.Random;


public class UnkillableDemon implements IBot {
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
            return calculateWinningMove(state, moveTimeMs);
        }
        // Plays single games until it wins and returns the first move for that. If iterations reached with no clear win, just return random valid move
        private IMove calculateWinningMove(IGameState state, int maxTimeMs){
            long startTime = System.currentTimeMillis();
            int depth = 5; // Set the maximum depth for the search tree

            IMove bestMove = null;
            int bestScore = Integer.MIN_VALUE;

            List<IMove> moves = state.getField().getAvailableMoves();
            for (IMove move : moves) {
                GameSimulator simulator = createSimulator(state);
                simulator.updateGame(move);
                int score = simulator.minimax(simulator, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
                long moveEndTime = System.currentTimeMillis(); // Record end time for each move
                long moveTime = moveEndTime - startTime; // Calculate time taken for this move
                System.out.println("Move evaluation time: " + moveTime + " milliseconds");

                if (score > bestScore) {
                    bestScore = score;
                    bestMove = move;
                }
            }

            long endTime = System.currentTimeMillis();
            if (endTime - startTime >= maxTimeMs) {
                // Return the best move found within the time limit
                return bestMove;
            } else {
                // If time runs out, return a random move
                if(bestMove != null){
                    return bestMove;
                }
                Random rand = new Random();
                return moves.get(rand.nextInt(moves.size()));
            }
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

            @Override
            public int hashCode() {
                return Objects.hash(x, y);
            }
        }

        class GameSimulator {
            private final IGameState currentState;
            private int currentPlayer = 0; //player0 == 0 && player1 == 1
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

            private int minimax(GameSimulator simulator, int depth,int alpha, int beta, boolean isMaximizingPlayer) {
                if (depth == 0 || simulator.getGameOver() != GameOverState.Active) {
                    // If at max depth or game over, evaluate the board state
                    //return evaluate(simulator);
                    return 0;
                }
                List<IMove> moves = simulator.getCurrentState().getField().getAvailableMoves();
                if (isMaximizingPlayer) {
                    int bestScore = Integer.MIN_VALUE;
                    for (IMove move : moves) {
                        GameSimulator newSimulator = createSimulator(simulator.getCurrentState());
                        newSimulator.updateGame(move);
                        int score = minimax(newSimulator, depth -1, alpha, beta, false);
                        bestScore = Math.max(bestScore, score);
                        alpha = Math.max(alpha, score);
                        // Beta cut-off: if beta is less than or equal to alpha, stop searching further
                        if (beta <= alpha)
                            break; // Beta cut-off
                    }
                    return bestScore;
                } else {
                    int bestScore = Integer.MAX_VALUE;
                    for (IMove move : moves) {
                        GameSimulator newSimulator = createSimulator(simulator.getCurrentState());
                        newSimulator.updateGame(move);
                        int score = minimax(newSimulator, depth - 1, alpha, beta,true);
                        bestScore = Math.min(bestScore, score);
                        beta = Math.min(beta, score);
                        // Alpha cut-off: if beta is less than or equal to alpha, stop searching further
                        if (beta <= alpha)
                            break; // Alpha cut-off
                    }
                    return bestScore;
                }
            }


        }

   /* private int evaluate(GameSimulator simulator) {
        int score = 0;

        // Check each mini-board
        for (int miniRow = 0; miniRow < 3; miniRow++) {
            for (int miniCol = 0; miniCol < 3; miniCol++) {
                char winner = getMiniBoardWinner(simulator, miniRow * 3, miniCol * 3);
                if (winner == 'X') {
                    score += 100; // Favorable score for maximizing player (X) winning mini-board
                } else if (winner == 'O') {
                    score -= 100; // Unfavorable score for minimizing player (O) winning mini-board
                } else {
                    // Check for potential threats and opportunities in each mini-board
                    score += evaluateMiniBoard(simulator, miniRow * 3, miniCol * 3, 'X');
                    score -= evaluateMiniBoard(simulator, miniRow * 3, miniCol * 3, 'O');
                }
            }
        }

        return score;
    }

    // Helper method to evaluate each mini-board
    private int evaluateMiniBoard(GameSimulator simulator, int startRow, int startCol, char player) {
        int score = 0;
        char opponent = (player == 'X') ? 'O' : 'X';

        // Evaluate rows
        for (int row = startRow; row < startRow + 3; row++) {
            score += evaluateLine(simulator, row, startCol, row, startCol + 2, player, opponent);
        }

        // Evaluate columns
        for (int col = startCol; col < startCol + 3; col++) {
            score += evaluateLine(simulator, startRow, col, startRow + 2, col, player, opponent);
        }

        // Evaluate diagonals
        score += evaluateLine(simulator, startRow, startCol, startRow + 2, startCol + 2, player, opponent);
        score += evaluateLine(simulator, startRow, startCol + 2, startRow + 2, startCol, player, opponent);

        return score;
    }

    // Helper method to evaluate a line (row, column, or diagonal) within a mini-board
    private int evaluateLine(GameSimulator simulator, int row1, int col1, int row2, int col2, char player, char opponent) {
        int score = 0;
        int playerCount = 0;
        int opponentCount = 0;

        for (int row = row1; row <= row2; row++) {
            for (int col = col1; col <= col2; col++) {
                char cell = simulator.getCurrentState().getField().get(row, col);
                if (cell == player) {
                    playerCount++;
                } else if (cell == opponent) {
                    opponentCount++;
                }
            }
        }

        // Assign scores based on the presence of player's and opponent's marks in the line
        if (playerCount == 3) {
            score += 1000; // Player wins the line
        } else if (opponentCount == 3) {
            score -= 1000; // Opponent wins the line
        } else if (playerCount == 2 && opponentCount == 0) {
            score += 100; // Two in a row for player
        } else if (opponentCount == 2 && playerCount == 0) {
            score -= 100; // Two in a row for opponent, consider blocking
        } else if (playerCount == 1 && opponentCount == 0) {
            score += 10; // One in a row for player
        } else if (opponentCount == 1 && playerCount == 0) {
            score -= 10; // One in a row for opponent
        } else if (playerCount == 0 && opponentCount == 1) {
            score -= 5; // Opponent has one in a row, consider blocking
        }

        return score;
    }

    // Helper method to determine the winner of a mini-board
    private char getMiniBoardWinner(GameSimulator simulator, int startRow, int startCol) {
        char[][] miniBoard = new char[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                miniBoard[i][j] = simulator.getCurrentState().getField().get(startRow + i, startCol + j);
            }
        }

        // Check rows
        for (int row = 0; row < 3; row++) {
            if (miniBoard[row][0] == miniBoard[row][1] && miniBoard[row][1] == miniBoard[row][2]) {
                if (miniBoard[row][0] != '.') {
                    return miniBoard[row][0];
                }
            }
        }

        // Check columns
        for (int col = 0; col < 3; col++) {
            if (miniBoard[0][col] == miniBoard[1][col] && miniBoard[1][col] == miniBoard[2][col]) {
                if (miniBoard[0][col] != '.') {
                    return miniBoard[0][col];
                }
            }
        }

        // Check diagonals
        if (miniBoard[0][0] == miniBoard[1][1] && miniBoard[1][1] == miniBoard[2][2]) {
            if (miniBoard[0][0] != '.') {
                return miniBoard[0][0];
            }
        }
        if (miniBoard[0][2] == miniBoard[1][1] && miniBoard[1][1] == miniBoard[2][0]) {
            if (miniBoard[0][2] != '.') {
                return miniBoard[0][2];
            }
        }

        return '.';
    }*/




}



