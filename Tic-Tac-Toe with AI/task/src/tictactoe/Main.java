package tictactoe;

import javax.swing.plaf.synth.SynthMenuBarUI;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

class Coordinate {
    int row;
    int col;

    public Coordinate(int row, int col) {
        this.row = row;
        this.col = col;
    }

    @Override
    public String toString() {
        return "Coordinate{" +
                row +
                ", " + col +
                '}';
    }
}

abstract class Player {
    abstract Coordinate makeMove(Board board);
}

class User extends Player {
    private final Scanner sc = new Scanner(System.in);

    Coordinate getNextMove(Board board) {
        int cartesianRow, cartesianCol;
        while (true) {
            System.out.print("Enter the coordinates: ");
            String input = sc.nextLine().trim();
            String[] inputArr = input.split("\\s");
            if (inputArr.length != 2) {
                System.out.println("You should enter numbers!");
                continue;
            }
            else {
                try {
                    cartesianCol = Integer.parseInt(inputArr[0]);
                    cartesianRow = Integer.parseInt(inputArr[1]);
                } catch (NumberFormatException e) {
                    System.out.println("You should enter numbers!");
                    continue;
                }
            }
            if (cartesianRow < 1 || cartesianRow > 3 || cartesianCol < 1 || cartesianCol > 3) {
                System.out.println("Coordinates should be from 1 to 3!");
            }
            else if (board.spaceNotAvailableWithCartesianCoord(cartesianRow, cartesianCol))
                System.out.println("This cell is occupied! Choose" + " another one!");
            else break;
        }
        return new Coordinate(board.SIZE - cartesianRow, cartesianCol - 1);
    }

    @Override
    Coordinate makeMove(Board board) {
        // ask user for move
        return getNextMove(board);
    }
}

class AI extends Player {
    private final String level;
    private final SelectionAlg ai;

    public AI(String level) {
        this.level = level;
        this.ai = SelectionAlgFactory.getAlg(level);
    }

    @Override
    Coordinate makeMove(Board board) {
        System.out.printf("Making move level \"%s\"\n", level);
        // use SelectionAlg to select coordinate
        return ai.selectMove(board);

    }
}

class SelectionAlgFactory {
    public static SelectionAlg getAlg(String level) {
        switch (level) {
            case "medium":
                return new MediumLevelAlg();
            case "hard":
                return new HardLevelAlg();
            default: // "easy"
                return new EasyLevelAlg();
        }
    }
}

abstract class SelectionAlg {
    public static final int[] ROW_UNI_DIR = new int[] { -1, -1, -1, 0, 0, 1, 1, 1 };
    public static final int[] COL_UNI_DIR = new int[] { -1, 0, 1, -1, 1, -1, 0, 1 };
    public static final int[][] ROW_BI_DIR = new int[][] {{0, 0},{-1, 1}};
    public static final int[][] COL_BI_DIR = new int[][] {{-1, 1},{0, 0}};

    protected boolean uniDirectionalCheck(Board board, char symbol, Coordinate source) {
        int rr, rrr, cc, ccc;
        for (int k = 0; k < 8; k++) {
            rr = source.row + ROW_UNI_DIR[k];
            cc = source.col + COL_UNI_DIR[k];
            if (isInbound(board, rr, cc) && board.dataArr[rr][cc] == symbol) {
                rrr = rr + ROW_UNI_DIR[k];
                ccc = cc + COL_UNI_DIR[k];
                if (isInbound(board, rrr, ccc) && board.dataArr[rrr][ccc] == symbol) {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean biDirectionalCheck(Board board, char symbol, Coordinate source) {
        if (source.row == 1 || source.col == 1) {
            int row, col;
            for (int i = 0; i < 2; i++) {
                row = source.row + ROW_BI_DIR[i][0];
                col = source.col + COL_BI_DIR[i][0];
                if (isInbound(board,row, col) && board.dataArr[row][col] == symbol) {
                    row = source.row + ROW_BI_DIR[i][1];
                    col = source.col + COL_BI_DIR[i][1];
                    if (isInbound(board, row, col) && board.dataArr[row][col] == symbol) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    abstract Coordinate selectMove(Board board);

    ArrayList<Coordinate> findEmptySpaces(Board board) {
        ArrayList<Coordinate> result = new ArrayList<>();
        for (int i = 0; i < board.SIZE; i++) {
            for (int j = 0; j < board.SIZE; j++) {
                if (board.spaceAvailableWithMatrixCoord(i, j)) {
                    result.add(new Coordinate(i, j));
                }
            }
        }
        return result;
    }

    int[] pickRandomCoord() {
        int[] result = new int[2];
        result[0] = ThreadLocalRandom.current().nextInt(3);
        result[1] = ThreadLocalRandom.current().nextInt(3);
        return result;
    }

    protected char getOpponentSymbol(char currentSymbol) {
        return currentSymbol == 'X' ? 'O' : 'X';
    }

    protected boolean isInbound(Board board, int row, int col) {
        return (row >= 0) && (row < board.SIZE)
                && (col >= 0) && (col < board.SIZE);
    }
}

class EasyLevelAlg extends SelectionAlg {

    @Override
    Coordinate selectMove(Board board) {
        // randomly select a space
        int cartesianRow, cartesianCol;
        do {
            int[] randoms = pickRandomCoord();
            cartesianRow = randoms[0]+1;
            cartesianCol = randoms[1]+1;
        } while (board.spaceNotAvailableWithCartesianCoord(cartesianRow, cartesianCol));
        return new Coordinate(board.SIZE - cartesianRow, cartesianCol - 1);
    }
}

class MediumLevelAlg extends EasyLevelAlg {

    Coordinate findWinningMove(Board board, char symbol) {
        ArrayList<Coordinate> emptySpaces = findEmptySpaces(board);
        for (Coordinate cor : emptySpaces) {
            Coordinate winningMove = findWinningMoveFromEmptySpace(board, symbol, cor);
            if (winningMove != null) return winningMove;
        }

        return null;
    }

    private Coordinate findWinningMoveFromEmptySpace(Board board, char symbol, Coordinate source) {
        if (uniDirectionalCheck(board, symbol, source)) return source;
        if (biDirectionalCheck(board, symbol, source)) return source;
        return null;
    }

    @Override
    Coordinate selectMove(Board board) {
        Coordinate move = findWinningMove(board, board.getCurrentSymbol());
        if (move != null) {
            return move;
        }
        move = findDefendMove(board, board.getCurrentSymbol());
        return move == null ? super.selectMove(board) : move;
    }

    protected Coordinate findDefendMove(Board board, char currentSymbol) {
        return findWinningMove(board, getOpponentSymbol(currentSymbol));
    }

}

class HardLevelAlg extends MediumLevelAlg {
    static final Map<Character, Character> flippedSymbol = Map.of('X','O','O','X');
    @Override
    Coordinate selectMove(Board board) {
        return getBestMove(board);
    }

    Coordinate getBestMove(Board board) {
        ArrayList<Coordinate> candidateMoves = findEmptySpaces(board);
        Coordinate bestMove = null;
        int highestScore = Integer.MIN_VALUE;

        for (int i = 0; i < candidateMoves.size(); i++) {
            int currentScore = minimaxAlg(board, candidateMoves.get(i), board.getCurrentSymbol(), true);
            if (currentScore > highestScore) {
                highestScore = currentScore;
                bestMove = candidateMoves.get(i);
            }
        }

        return bestMove;
    }

    private int minimaxAlg(Board board, Coordinate move, char symbolToWin, boolean isOpponentTurn) {
        char lastTurnSymbol = isOpponentTurn ? symbolToWin : flippedSymbol.get(symbolToWin);
        int score;

        // make the move
        String oldBoard = board.convertToDataStr();
        Board newBoard = new Board(board.SIZE, updatedDataStr(move, oldBoard, lastTurnSymbol));

        // check if game is won
        newBoard.checkState();
        if (newBoard.isGameOver()) {
            if (newBoard.currentState.contains(String.valueOf(symbolToWin))) {
                score = 1;
            }
            else if (newBoard.currentState.contains("Draw")) {
                score = 0;
            }
            else {
                score = -1;
            }
            return score;
        }

        // Get available moves
        ArrayList<Coordinate> availableMoves = findEmptySpaces(newBoard);

        // calculate scores
        if (isOpponentTurn) {
            ArrayList<Integer> listOfScores = new ArrayList<>();
            score = Integer.MAX_VALUE;
            listOfScores.add(score);
            for (Coordinate nextMove : availableMoves) {
                listOfScores.add(minimaxAlg(newBoard, nextMove, symbolToWin, false));
            }
            score = Collections.min(listOfScores);
        }
        else {
            ArrayList<Integer> listOfScores = new ArrayList<>();
            score = Integer.MIN_VALUE;
            listOfScores.add(score);
            for (Coordinate nextMove : availableMoves) {
                listOfScores.add(minimaxAlg(newBoard, nextMove, symbolToWin, true));
            }
            score = Collections.max(listOfScores);
        }
        return score;
    }

    private String updatedDataStr(Coordinate move, String dataStr, char symbol) {
        int row = move.row;
        int col = move.col;
        return dataStr.substring(0, row*3+col) + symbol + dataStr.substring(row*3+col+1);
    }

}

class Board {
    final int SIZE;
    String dataStr;
    char[][] dataArr;
    String currentState = "Game not finished";
    int currentTurn = 0;
    boolean gameOver = false;

    public Board(int size, String field) {
        SIZE = size;
        dataStr = field;
        fillDataArr();
        calculateCurrentTurn();
    }

    public String convertToDataStr() {
        StringBuilder sb = new StringBuilder();
        for (char[] row : dataArr) {
            for (char col : row) {
                sb.append(col);
            }
        }
        return sb.toString();
    }

    public int getCurrentTurn() {
        return currentTurn;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    private void calculateCurrentTurn() {
        for (char c : dataStr.toCharArray()) {
            if (c != '_') currentTurn++;
        }
    }

    private void fillDataArr() {
        dataArr =  new char[SIZE][SIZE];
        for (int i = 0; i < dataStr.length(); i++ ) {
            char c = dataStr.charAt(i);
            dataArr[(i / 3)][(i % 3)] = c;
        }
    }

    void printBoard() {
        System.out.print("---------\n");
        for (char[] row : dataArr) {
            System.out.print("| ");
            for (char col : row) {
                if (col == '_') col = ' ';
                System.out.printf("%s ", col);
            }
            System.out.print("|\n");
        }
        System.out.print("---------\n");
    }

    private boolean isFullBoard() {
        return currentTurn == 9;
    }

    public void printState() {
        System.out.println(currentState);
    }

    private boolean checkDiagonal() {
        char left = dataArr[0][0];
        char right = dataArr[0][SIZE-1];
        boolean leftDiagonal = true, rightDiagonal = true;
        for (int d = 0; d < SIZE; d++) {
            if (leftDiagonal && (dataArr[d][d] == '_' || dataArr[d][d] != left)) leftDiagonal = false;
            if (rightDiagonal
                    && (dataArr[d][SIZE-1-d] == '_'
                    || dataArr[d][SIZE-1-d] != right))
                rightDiagonal = false;
        }
        if (leftDiagonal) currentState = (left + " wins");
        else if (rightDiagonal) currentState = (right+ " wins");
        return leftDiagonal || rightDiagonal;
    }

    private boolean checkCol() {
        for (int j = 0; j < SIZE; j++) {
            char check = dataArr[0][j];
            if (check != '_' && dataArr[1][j] == check && dataArr[2][j] == check) {
                currentState = (check + " wins");
                return true;
            }
        }
        return false;
    }

    private boolean checkRow() {
        for (int i = 0; i < SIZE; i++) {
            char check = dataArr[i][0];
            if (check != '_' && dataArr[i][1] == check && dataArr[i][2] == check) {
                currentState = (check + " wins");
                return true;
            }
        }
        return false;
    }

    void checkState() {
        if (!(checkRow() || checkCol() || checkDiagonal())) {
            if (isFullBoard()) {
                currentState = "Draw";
                gameOver = true;
            }
        }
        else gameOver = true;
    }

    public void applyMove(Coordinate move) {
        updateBoard(move);
        checkState();
        printBoard();
    }

    public boolean spaceNotAvailableWithCartesianCoord(int row, int col) {
        // return true if space is empty
        // origin: lower left
        return dataArr[SIZE - row][col - 1] != '_';
    }

    public boolean spaceAvailableWithMatrixCoord(int row, int col) {
        // origin: upper left
        return dataArr[row][col] == '_';
    }

    private void updateBoard(Coordinate move) {
        dataArr[move.row][move.col] = getCurrentSymbol();
        currentTurn++;
    }

    public char getCurrentSymbol() {
        return (currentTurn % 2 == 0) ? 'X' : 'O';
    }
}

public class Main {
    static final String emptyBoard = "_________";

    private static void playGame(Board board, Player p1, Player p2) {
        while (!board.isGameOver()) {
            Coordinate nextMove;
            if (board.getCurrentTurn() % 2 == 0) {
                nextMove = p1.makeMove(board);
            }
            else {
                nextMove = p2.makeMove(board);
            }
            board.applyMove(nextMove);
        }
        board.printState();
    }

    private static void parseFromStdIn() {
        Scanner sc = new Scanner(System.in);
        String[] playerLevels = new String[2];
        while (true) {
            System.out.print("Input command: ");
            String[] commands = sc.nextLine().trim().split("\\s");
            if (commands.length == 3 && commands[0].equals("start")) {
                System.arraycopy(commands, 1, playerLevels, 0, 2);
            }
            else if (commands.length == 1 && commands[0].equals("exit")) {
                break;
            }
            else {
                System.out.println("Bad parameters!");
                continue;
            }

            Board board = initBoard();
            Player[] players = initPlayers(playerLevels);
            playGame(board, players[0], players[1]);
        }
    }

    private static Player[] initPlayers(String[] levels) {
        Player[] players = new Player[2];
        for (int i = 0; i < levels.length; i++) {
            if (levels[i].equals("user"))
                players[i] = new User();
            else
                players[i] = new AI(levels[i]);
        }
        return players;
    }

    private static Board initBoard() {
        Board board = new Board(3, emptyBoard);
        board.printBoard();
        return board;
    }

    public static void main(String[] args) {
        parseFromStdIn();
    }

}
