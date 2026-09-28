package nonogram;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Nonogram {
    private static final BufferedReader reader;

    private static List<String> validNonograms = new ArrayList<>();
    private static long[] nonogram;
    private static int columns;
    private static int rows;

    private static int[][] columnClues;
    private static int[][] rowClues;

    private static List<Long>[] rowPlacements;


    static {
        try{
            reader = new BufferedReader(new FileReader("nonogram.in"));
        }
        catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void main(String[] args) throws Exception {
        parseHeader();
        parseClues();

        solve();

        writeToFile();
    }

    private static void solve() {
        nonogram = new long[rows];
        solveRec(0);
    }

    // what is the bound of this problem?
    private static void solveRec(int row) {
        if (row == rows) {
            validNonograms.add(nonogramToString());
            return;
        }
        for (long candidate : rowPlacements(row)) {
            if (validPlacement(row, candidate)) {
                fill(row, candidate);
                solveRec(row + 1);
                unfill(row);
            }
        }


        //optional: presolve/propagate

        //d = find something that must be decided
        //if d does not exist:
        // return isValidSolution

        //for each possible decision D of d
        //  S' = apply D to state
        //  if (solveRec(S'))
        //    return true;
        //return false
    }

    private static List<Long> rowPlacements(int row) {
        List<Long> result = new ArrayList<>();
        generatePlacements(rowClues[row], 0, columns, 0, 0L, result);
        return result;
    }

    private static void generatePlacements(int[] clue, int clueIdx, int length,
                                           int pos, long acc, List<Long> result) {
        if (clueIdx == clue.length) {
            result.add(acc);
            return;
        }
        int block = clue[clueIdx];
        int remaining = 0;
        for (int i = clueIdx; i < clue.length; i++) remaining += clue[i];
        remaining += (clue.length - clueIdx - 1); // mandatory 1-cell gaps between the rest

        for (int start = pos; start + remaining <= length; start++) {
            long mask = ((1L << block) - 1) << start;
            generatePlacements(clue, clueIdx + 1, length, start + block + 1, acc | mask, result);
        }
    }

    private static void fill(int row, long candidate) {
        nonogram[row] = candidate;
    }

    private static void unfill(int row) {
        nonogram[row] = 0L;
    }

    private static boolean validPlacement(int row, long candidate) {
        for (int c = 0; c < columns; c++) {
            long colBits = 0;
            for (int r = 0; r <= row; r++) {
                long rowBits = (r == row) ? candidate : nonogram[r];
                if (((rowBits >> c) & 1L) != 0) {
                    colBits |= (1L << r);
                }
            }
            if (!columnPrefixValid(colBits, row + 1, columnClues[c])) {
                return false;
            }
        }
        return true;
    }

    // true if the filled cells in colBits (rows 0..rowsPlaced-1) could still
// eventually match clue -- i.e. every *closed* run so far matches exactly,
// and any run still open (touching rowsPlaced-1) hasn't overshot yet
    private static boolean columnPrefixValid(long colBits, int rowsPlaced, int[] clue) {
        int blockIdx = 0;
        int i = 0;
        while (i < rowsPlaced) {
            if (((colBits >> i) & 1L) == 0) {
                i++;
                continue;
            }
            int start = i;
            while (i < rowsPlaced && ((colBits >> i) & 1L) != 0) i++;
            int runLength = i - start;
            boolean closed = i < rowsPlaced;

            if (blockIdx >= clue.length) return false;
            if (closed) {
                if (runLength != clue[blockIdx]) return false;
                blockIdx++;
            } else {
                if (runLength > clue[blockIdx]) return false;
            }
        }
        return true;
    }

    private static void parseHeader() throws Exception{
        var header = reader.readLine().trim().split("\\s+");
        columns = Integer.parseInt(header[0]);
        rows = Integer.parseInt(header[1]);
        assert columns < 63 && rows < 63;
    }

    private static void parseClues() throws Exception {
        rowClues = new int[rows][];
        for (int r = 0; r < rows; r++) {
            rowClues[r] = parseClueLine();
        }

        columnClues = new int[columns][];
        for (int c = 0; c < columns; c++) {
            columnClues[c] = parseClueLine();
        }
    }

    private static int[] parseClueLine() throws IOException {
        var line = reader.readLine();
        if (line == null || line.isBlank()) {
            return new int[0];
        }

        var tokens = line.trim().split("\\s+");
        var clues = new int[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            clues[i] = Integer.parseInt(tokens[i]);
        }
        return clues;
    }


    private static void writeToFile() throws Exception{
        try (var writer = new PrintWriter(new FileWriter("nonogram-my.out"))) {
            for (var solution : validNonograms) {
                writer.println(solution);
            }
        }
    }

    private static String nonogramToString() {
        StringBuilder sb = new StringBuilder(rows * columns);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                sb.append(((nonogram[r] >> c) & 1L) != 0 ? '#' : '.');
            }
        }
        return sb.toString();
    }
}
