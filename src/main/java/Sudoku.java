import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class Sudoku {
    static final BufferedReader reader;

    private static final int fieldSize = 9;

    private static final int[][] field = new int[9][9];
    private static int amountOfSolutions = 0;

    private static int nextBestRowIndex;
    private static int nextBestColIndex;

    static {
        try {
            reader = new BufferedReader(new FileReader("sudoku.in"));
        } catch (IOException _e) {
            throw new RuntimeException(_e);
        }
    }

    public static void main(String[] args) throws Exception {
        try (reader; var out = new PrintWriter("sudoku.out")) {
            for (int i = 0; i < fieldSize; i++) {
                var line = reader.readLine();
                for (int j = 0; j < fieldSize; j++) {
                    var c = line.charAt(j);
                    if ('.' != c) {
                        field[i][j] = c - '0';
                    }
                }
            }

            countSolutions();

            out.println(amountOfSolutions);
        }
    }

    private static void countSolutions() {
        if (validSolution()) {
            amountOfSolutions++;
            return;
        }

        findBestIndexes();
        int bestRowIndex = nextBestRowIndex;
        int bestColIndex = nextBestColIndex;

        // FIX 2: try EVERY candidate (not just the first one). If there is no
        // candidate, the loop body never runs and this branch is a dead end.
        var available = availableNumbers(bestRowIndex, bestColIndex);
        for (int i = 0; i < available.length; i++) {
            if (available[i]) {
                field[bestRowIndex][bestColIndex] = i + 1;
                countSolutions();
                undo(bestRowIndex, bestColIndex);
            }
        }
    }

    // was apply(): now only computes the candidates, the placing is done in countSolutions()
    private static boolean[] availableNumbers(int bestRowIndex, int bestColIndex) {
        boolean[] available = new boolean[]{true,true,true,true,true,true,true,true,true};
        for (int i = 0; i < fieldSize; i++) {
            var n = field[i][bestColIndex];
            if (n != 0) {
                available[n - 1] = false;
            }
            n = field[bestRowIndex][i];
            if (n != 0) {
                available[n - 1] = false;
            }
        }

        int row = (bestRowIndex / 3) * 3;
        int col = (bestColIndex / 3) * 3;
        for (int i = row; i < row + 3; i++) {
            for (int j = col; j < col + 3; j++) {
                var n = field[i][j];
                if (n != 0) {
                    available[n - 1] = false;
                }
            }
        }

        return available;
    }

    private static void undo(int bestRowIndex, int bestColIndex) {
        field[bestRowIndex][bestColIndex] = 0;
    }

    private static void findBestIndexes() {
        int minEmpty = 10;
        int bestIndex = 10;
        int bestType = 0;
        for (int i = 0; i < fieldSize; i++) {
            int rowCount = 9;
            int colCount = 9;

            for (int j = 0; j < fieldSize; j++) {
                if (field[i][j] != 0) {
                    rowCount--;
                }
                if (field[j][i] != 0) {
                    colCount--;
                }
            }
            // FIX 1: ignore rows/columns that are already full (count 0),
            // otherwise they "win" and no empty cell is found in them.
            if (rowCount > 0 && rowCount < minEmpty) {
                minEmpty = rowCount;
                bestIndex = i;
                bestType = 1;
            }
            if (colCount > 0 && colCount < minEmpty) {
                minEmpty = colCount;
                bestIndex = i;
                bestType = 2;
            }
        }

        for (int g = 0; g < fieldSize; g++) {
            int boxRow = (g / 3) * 3;
            int boxCol = (g % 3) * 3;
            int boxCount = 9;

            for (int i = boxRow; i < boxRow + 3; i++) {
                for (int j = boxCol; j < boxCol + 3; j++) {
                    if (field[i][j] != 0) {
                        boxCount--;
                    }
                }
            }
            if (boxCount > 0 && boxCount < minEmpty) {
                minEmpty = boxCount;
                bestIndex = g;
                bestType = 3;
            }
        }

        if (bestType == 1) {
            for (int i = 0; i < fieldSize; i++) {
                if (field[bestIndex][i] == 0) {
                    nextBestRowIndex = bestIndex;
                    nextBestColIndex = i;
                }
            }
        } else if (bestType == 2) {
            for (int i = 0; i < fieldSize; i++) {
                if (field[i][bestIndex] == 0) {
                    nextBestRowIndex = i;
                    nextBestColIndex = bestIndex;
                }
            }
        } else if (bestType == 3) {
            // NEW: pick an empty cell from the chosen box
            int boxRow = (bestIndex / 3) * 3;
            int boxCol = (bestIndex % 3) * 3;
            for (int i = boxRow; i < boxRow + 3; i++) {
                for (int j = boxCol; j < boxCol + 3; j++) {
                    if (field[i][j] == 0) {
                        nextBestRowIndex = i;
                        nextBestColIndex = j;
                    }
                }
            }
        }
    }

    private static boolean validSolution() {
        for (int i = 0; i < fieldSize; i++) {
            for (int j = 0; j < fieldSize; j++) {
                if (field[j][i] == 0) {
                    return false;
                }
            }
        }
        return true;
    }
}