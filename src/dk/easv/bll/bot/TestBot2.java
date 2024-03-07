package dk.easv.bll.bot;

import dk.easv.bll.field.IField;
import dk.easv.bll.game.GameState;
import dk.easv.bll.game.IGameState;
import dk.easv.bll.move.IMove;

import java.util.List;
import java.util.Objects;

public class TestBot2 implements IBot {
    final int moveTimeMs = 1000;
    private String BOT_NAME = getClass().getSimpleName();
    private long timerStartMillis = 0;

    private String timerMsg = "";
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
        //startTimer("Minimax move");

        return calculateWinningMove(state, moveTimeMs);
    }

    // Plays single games until it wins and returns the first move for that. If iterations reached with no clear win, just return random valid move
    private IMove calculateWinningMove(IGameState state, int maxTimeMs) {
        long startTime = System.currentTimeMillis();
        int depth = 3; // Set the maximum depth for the search tree
        // grazina move findBestMove
        IMove bestMove = null;
        GameSimulator simulator = createSimulator(state);
        bestMove = simulator.findBestMove(simulator, depth);

        long endTime = System.currentTimeMillis();
        //stopTimer();
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

       // @Override
       // public boolean equals(Object o) {
       //     if (this == o) return true;
       //     if (o == null || getClass() != o.getClass()) return false;
       //     ExampleSneakyBot.Move move = (ExampleSneakyBot.Move) o;
       //     return x == move.x && y == move.y;
       // }
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

        //--------------------------------------

        public int miniMax(GameSimulator board, int depth, boolean isMax) {
            int boardVal = evaluateBoard(board, depth);

            // Terminal node (win/lose/draw) or max depth reached.
            if (Math.abs(boardVal) > 0 || depth == 0
                    || board.getCurrentState().getField().getAvailableMoves().isEmpty()) {
                return boardVal;
            }

            // Maximising player, find the maximum attainable value.
            if (isMax) {
                int highestVal = Integer.MIN_VALUE;
                List<IMove> moves = board.getCurrentState().getField().getAvailableMoves();
                for (IMove move : moves) {
                    GameSimulator newSimulator = createSimulator(board.getCurrentState());
                    newSimulator.updateGame(move);
                    highestVal = Math.max(highestVal, miniMax(newSimulator,
                            depth - 1, false));
                }
                return highestVal;
            }

            else {
                int lowestVal = Integer.MAX_VALUE;
                List<IMove> moves = board.getCurrentState().getField().getAvailableMoves();
                for (IMove move : moves) {
                    GameSimulator newSimulator = createSimulator(board.getCurrentState());
                    newSimulator.updateGame(move);
                    lowestVal = Math.min(lowestVal, miniMax(newSimulator, depth - 1, true));

                }
                return lowestVal;
            }
        }
        /*public int miniMax(GameSimulator board, int depth, int alpha, int beta, boolean isMax) {
            int boardVal = evaluateBoard(board, depth);

            // Terminal node (win/lose/draw) or max depth reached.
            if (Math.abs(boardVal) > 0 || depth == 0
                    || board.getCurrentState().getField().getAvailableMoves().isEmpty()) {
                return boardVal;
            }

            // Maximising player, find the maximum attainable value.
            if (isMax) {
                int highestVal = Integer.MIN_VALUE;
                List<IMove> moves = board.getCurrentState().getField().getAvailableMoves();
                for (IMove move : moves) {
                    GameSimulator newSimulator = createSimulator(board.getCurrentState());
                    newSimulator.updateGame(move);
                    int val = miniMax(newSimulator, depth - 1, alpha, beta, false);
                    highestVal = Math.max(highestVal, val);
                    alpha = Math.max(alpha, highestVal);
                    if (beta <= alpha) {
                        break; // Beta cutoff
                    }
                }
                return highestVal;
            } else {
                int lowestVal = Integer.MAX_VALUE;
                List<IMove> moves = board.getCurrentState().getField().getAvailableMoves();
                for (IMove move : moves) {
                    GameSimulator newSimulator = createSimulator(board.getCurrentState());
                    newSimulator.updateGame(move);
                    int val = miniMax(newSimulator, depth - 1, alpha, beta, true);
                    lowestVal = Math.min(lowestVal, val);
                    beta = Math.min(beta, lowestVal);
                    if (beta <= alpha) {
                        break; // Alpha cutoff
                    }
                }
                return lowestVal;
            }
        }*/

        /**
         * Evaluate the given board from the perspective of the X player, return
         * 10 if a winning board configuration is found, -10 for a losing one and 0
         * for a draw, weight the value of a win/loss/draw according to how many
         * moves it would take to realise it using the depth of the game tree the
         * board configuration is at.
         * @param board Board to evaluate
         * @param depth depth of the game tree the board configuration is at
         * @return value of the board
         */
        private int evaluateBoard(GameSimulator board, int depth) {
            String [] rowSum = new String[3];
            //int rowSum = 0;
            int bWidth = 3; //small board width
            String[] Playerwin = {""+ player, ""+ player, "" + player};
            String[] Opponentwin = {""+ opponent, ""+ opponent, "" + opponent};
            String[][] macroBoard = board.getCurrentState().getField().getBoard();
            //Getting current board
            List<IMove> moves = board.getCurrentState().getField().getAvailableMoves();
            if (!moves.isEmpty()) {
                int localX = moves.get(0).getX() % 3;
                int localY = moves.get(0).getY() % 3;
                int startX = moves.get(0).getX() - (localX);
                int startY = moves.get(0).getY() - (localY);

                int playerCount = 0;
                int opponentCount = 0;

                // Check rows for winner.
                for (int row = startX; row < startX + bWidth; row++) {
                    for (int col = startY; col < startY+ bWidth; col++) {
                        if(macroBoard[row][col].equals(Integer.toString(player))){
                            playerCount++;
                        }
                        else if(macroBoard[row][col].equals(Integer.toString(opponent))){
                            opponentCount++;
                        }
                    }
                    if (playerCount == 3) {
                        return 10 + depth;
                    } else if (opponentCount == 3) {
                        return -10 - depth;
                    }
                    opponentCount = 0;
                    playerCount = 0;
                }

                // Check columns for winner.
                opponentCount = 0;
                playerCount = 0;
                for (int col = startY; col < startY+ bWidth; col++) {
                    for (int row = startX; row < startX+bWidth; row++) {
                        if(macroBoard[row][col].equals(Integer.toString(player))){
                            playerCount++;
                        }
                        else if(macroBoard[row][col].equals(Integer.toString(opponent))){
                            opponentCount++;
                        }
                    }
                    if (playerCount == 3) {
                        return 10 + depth;
                    } else if (opponentCount == 3) {
                        return -10 - depth;
                    }
                    opponentCount = 0;
                    playerCount = 0;
                }

                // Check diagonals for winner.
                // Top-left to bottom-right diagonal.
                opponentCount = 0;
                playerCount = 0;
                for (int i = 0; i < bWidth; i++) {
                    if(macroBoard[startX+i][startY+i].equals(Integer.toString(player))){
                        playerCount++;
                    }
                    else if(macroBoard[startX+i][startY+i].equals(Integer.toString(opponent))){
                        opponentCount++;
                    }
                }
                if (playerCount == 3) {
                    return 10 + depth;
                } else if (opponentCount == 3) {
                    return -10 - depth;
                }

                // Top-right to bottom-left diagonal.
                opponentCount = 0;
                playerCount = 0;
                int indexMax = bWidth - 1;
                for (int i = 0; i <= indexMax; i++) {
                    if(macroBoard[startX+i][startY+indexMax-i].equals(Integer.toString(player))){
                        playerCount++;
                    }
                    else if(macroBoard[startX+i][startY+indexMax-i].equals(Integer.toString(opponent))){
                        opponentCount++;
                    }
                }
                if (playerCount == 3) {
                    return 10 + depth;
                } else if (opponentCount == 3) {
                    return -10 - depth;
                }
            }
            return 0;
        }


        private int evaluate(GameSimulator simulator) {
            List<IMove> moves = simulator.getCurrentState().getField().getAvailableMoves();
            if (!moves.isEmpty()) {
                int localX = moves.get(0).getX() % 3;
                int localY = moves.get(0).getY() % 3;
                int startX = moves.get(0).getX() - (localX);
                int startY = moves.get(0).getY() - (localY);
                String[][] board = simulator.getCurrentState().getField().getBoard();

                int score = 0;

                // Evaluate rows
                for (int row = startX; row < startX + 3; row++) {
                    score += evaluateLine(board[row][startY], board[row][startY + 1], board[row][startY + 2]);
                }

                // Evaluate columns
                for (int col = startY; col < startY + 3; col++) {
                    score += evaluateLine(board[startX][col], board[startX + 1][col], board[startX + 2][col]);
                }

                // Evaluate diagonals
                score += evaluateLine(board[startX][startY], board[startX + 1][startY + 1], board[startX + 2][startY + 2]);
                score += evaluateLine(board[startX][startY + 2], board[startX + 1][startY + 1], board[startX + 2][startY]);

                // Add score based on the number of winning positions on the board
                score += evaluateWinningPositions(board,startX,startY);

                return score;
            }
            return 0;
        }

        private int evaluateWinningPositions(String[][] board,int startX,int startY) {
            // Count the number of winning positions on the board
            int winningPositions = 0;

            // Check rows
            for (int row = startX; row < startX + 3; row++) {
                if (board[row][startY].equals(board[row][startY+1]) && board[row][startY+1].equals(board[row][startY+2])) {
                    if (!board[row][startY].equals(IField.EMPTY_FIELD)) {
                        winningPositions++;
                    }
                }
            }

            // Check columns
            for (int col = startY; col <startY+ 3; col++) {
                if (board[startX][col].equals(board[startX+1][col]) && board[startX+1][col].equals(board[startX+2][col])) {
                    if (!board[startX][col].equals(IField.EMPTY_FIELD)) {
                        winningPositions++;
                    }
                }
            }

            // Check diagonals
            if (board[startX][startY].equals(board[startX+1][startY+1]) && board[startX+1][startY+1].equals(board[startX+2][startY+2])) {
                if (!board[startX][startY].equals(IField.EMPTY_FIELD)) {
                    winningPositions++;
                }
            }
            if (board[startX][startY+2].equals(board[startX+1][startY+1]) && board[startX+1][startY+1].equals(board[startX+2][startY])) {
                if (!board[startX][startY+2].equals(IField.EMPTY_FIELD)) {
                    winningPositions++;
                }
            }

            // Assign scores based on the number of winning positions
            int score = 0;
            if (winningPositions > 0) {
                score += winningPositions * 10000;
            }

            return score;
        }

        private int evaluateLine2(String cell1, String cell2, String cell3) {
            int score = 0;
            int emptyCount =0;

            // Count the number of X's and O's in the line
            int playerCount = 0;
            int opponentCount = 0;
            for (String cell : new String[]{cell1, cell2, cell3}) {
                if (cell.equals(Integer.toString(player))) {
                    playerCount++;
                } else if (cell.equals(Integer.toString(opponent))) {
                    opponentCount++;
                }
                else{
                    emptyCount++;
                }

            }

            // Assign scores based on the presence of player's and opponent's marks in the line
            if (playerCount == 3) {
                score += 10000; // Player wins the line
            } else if (opponentCount == 3) {
                score -= 10000; // Opponent wins the line
            } else if (playerCount == 2 && opponentCount == 0) {
                score += 1000; // Two in a row for player
            } else if (opponentCount == 2 && playerCount == 0) {
                score -= 1000; // Two in a row for opponent, consider blocking
            } else if (playerCount == 1 && opponentCount == 0) {
                score += 100; // One in a row for player
            } else if (opponentCount == 1 && playerCount == 0) {
                score -= 100; // One in a row for opponent
            } else if (playerCount == 0 && opponentCount == 1) {
                score -= 10; // Opponent has one in a row, consider blocking
                // Prioritize blocking the opponent's potential winning moves
                if (emptyCount == 1) {
                    // Opponent has one empty cell in a winning line (Threat Level 1)
                    score -= 1000; // Block immediately
                } else if (emptyCount == 2) {
                    // Opponent has two empty cells in a winning line (Threat Level 2)
                    score -= 500; // Consider blocking, but less urgent
                }
            }


            return score;


        }

        private int evaluateLine(String cell1, String cell2, String cell3) {
            int score = 0;
            int emptyCount = 0;

            // Count the number of X's and O's in the line
            int playerCount = 0;
            int opponentCount = 0;
            for (String cell : new String[]{cell1, cell2, cell3}) {
                if (cell.equals(Integer.toString(player))) {
                    playerCount++;
                } else if (cell.equals(Integer.toString(opponent))) {
                    opponentCount++;
                } else {
                    emptyCount++;
                }
            }

            // Winning scenarios for player
            if (playerCount == 3) {
                score += 10000; // Player wins the line
            } else if (playerCount == 2 && emptyCount == 1) {
                score += 1000; // Two in a row for player with one empty cell
            } else if (playerCount == 1 && emptyCount == 2) {
                score += 100; // One in a row for player with two empty cells
            }

            // Blocking scenarios for opponent
            if (opponentCount == 2 && emptyCount == 1) {
                score -= 2000; // Two in a row for opponent with one empty cell, urgent block
            } else if (opponentCount == 1 && emptyCount == 2) {
                score -= 500; // One in a row for opponent with two empty cells, consider blocking
            }

            return score;
        }


        private Move findBestMove(GameSimulator simulator,int depth)
        {
            player = currentPlayer;
            opponent = (player == 0) ? 1 : 0;

            int bestVal = Integer.MIN_VALUE;
            Move bestMove = new Move();
            bestMove.x = -1;
            bestMove.y = -1;
            List<IMove> moves = simulator.getCurrentState().getField().getAvailableMoves();
            // Traverse all cells, evaluate minimax function for all empty cells. And return the cell
            // with optimal value.
            for (IMove move : moves) {
                // Current state clone
                GameSimulator newSimulator = createSimulator(simulator.getCurrentState());
                // Make the move
                newSimulator.updateGame(move);

                // compute evaluation function for this move.
                int moveVal = miniMax(newSimulator, depth,  false);

                // If the value of the current move is more than the best value, then update best

                if (moveVal > bestVal) {
                    bestMove.x = move.getX();
                    bestMove.y = move.getY();
                    bestVal = moveVal;
                }
            }
            System.out.println("best score from bestMove" + bestVal);
            System.out.println("best move from bestMove" + bestMove);
            return bestMove;
        }




    }

   /* private boolean isWinningMove(GameSimulator simulator, int player) {
        String[][] board = simulator.getCurrentState().getField().getBoard();
        // Check rows, columns, and diagonals for winning configuration
        for (int i = 0; i < 3; i++) {
            if (board[i][0].equals(board[i][1]) && board[i][1].equals(board[i][2]) && board[i][0].equals(Integer.toString(player))) {
                return true; // Row i is filled with player's mark
            }
            if (board[0][i].equals(board[1][i]) && board[1][i].equals(board[2][i]) && board[0][i].equals(Integer.toString(player))) {
                return true; // Column i is filled with player's mark
            }
        }
        if (board[0][0].equals(board[1][1]) && board[1][1].equals(board[2][2]) && board[0][0].equals(Integer.toString(player))) {
            return true; // Diagonal from top-left to bottom-right is filled with player's mark
        }
        if (board[0][2].equals(board[1][1]) && board[1][1].equals(board[2][0]) && board[0][2].equals(Integer.toString(player))) {
            return true; // Diagonal from top-right to bottom-left is filled with player's mark
        }
        return false; // No winning configuration found
    }



    //To change


    // Helper method to evaluate each mini-board
    private int evaluateMiniBoard(GameSimulator simulator, int startRow, int startCol, char player) {
        int score = 0;
        char opponent = (player == 'X') ? 'O' : 'X';

        // Evaluate rows
        for (int row = startRow; row < startRow + 3; row++) {
            score += assignScores(simulator, row, startCol, row, startCol + 2, player, opponent);
        }

        // Evaluate columns
        for (int col = startCol; col < startCol + 3; col++) {
            score += assignScores(simulator, startRow, col, startRow + 2, col, player, opponent);
        }

        // Evaluate diagonals
        score += assignScores(simulator, startRow, startCol, startRow + 2, startCol + 2, player, opponent);
        score += assignScores(simulator, startRow, startCol + 2, startRow + 2, startCol, player, opponent);

        return score;
    }

    // Helper method to evaluate a line (row, column, or diagonal) within a mini-board
    private int assignScores(GameSimulator simulator, int startRow, int startCol, int endRow, int endCol, char player, char opponent) {
        int score = 0;
        int playerCount = 0;
        int opponentCount = 0;

        // Calculate line (rows, columns, and diagonals)
        for (int i = startRow; i <= endRow; i++) {
            for (int j = startCol; j <= endCol; j++) {
                char cell = simulator.getCurrentState().getField().getBoard()[i][j].charAt(0); // Convert String to char
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
                miniBoard[i][j] = simulator.getCurrentState().getField().getBoard()[startRow + i][startCol + j].charAt(0); // Convert String to char
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

        return '.'; // No winner found

        // Check rows, columns, and diagonals for a winner


    }*/


}