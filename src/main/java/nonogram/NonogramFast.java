package nonogram;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class NonogramFast {
    private static final BufferedReader reader;

    private static int columns;
    private static int rows;
    private static long columnsMask;
    private static long rowsMask;

    private static int[][] columnClues;
    private static int[][] rowClues;

    private static final List<String> solutions = new ArrayList<>();

    static {
        try {
            reader = new BufferedReader(new FileReader("nonogram-my.in"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void main(String[] args) throws Exception {
        var header = reader.readLine().trim().split("\\s+");
        columns = Integer.parseInt(header[0]);
        rows = Integer.parseInt(header[1]);
        assert columns < 63 && rows < 63;
        columnsMask = (1L << columns) - 1;
        rowsMask = (1L << rows) - 1;

        parseClues();

        State initial = new State(rows, columns);
        for (int r = 0; r < rows; r++) initial.rowCandidates[r] = generateCandidates(rowClues[r], columns);
        for (int c = 0; c < columns; c++) initial.colCandidates[c] = generateCandidates(columnClues[c], rows);

        solveRec(initial);
        writeSolutions();
    }

    private static void parseClues() throws Exception {
        rowClues = new int[rows][];
        for (int r = 0; r < rows; r++) rowClues[r] = parseClueLine();
        columnClues = new int[columns][];
        for (int c = 0; c < columns; c++) columnClues[c] = parseClueLine();
    }

    private static int[] parseClueLine() throws IOException {
        var line = reader.readLine();
        if (line == null || line.isBlank()) return new int[0];
        var tokens = line.trim().split("\\s+");
        var clues = new int[tokens.length];
        for (int i = 0; i < tokens.length; i++) clues[i] = Integer.parseInt(tokens[i]);
        return clues;
    }

    // ------------------------------------------------------------------
    // everything that needs to be cloned when we branch on a decision
    // ------------------------------------------------------------------
    private static final class State {
        long[] knownFilled;   // knownFilled[r]: bits = columns known filled in row r
        long[] knownBlank;    // knownBlank[r]:  bits = columns known blank  in row r
        List<Long>[] rowCandidates; // surviving placements per row, given what's known
        List<Long>[] colCandidates; // surviving placements per column

        @SuppressWarnings("unchecked")
        State(int rows, int columns) {
            knownFilled = new long[rows];
            knownBlank = new long[rows];
            rowCandidates = new List[rows];
            colCandidates = new List[columns];
        }

        @SuppressWarnings("unchecked")
        State copy() {
            State s = new State(rows, columns);
            s.knownFilled = knownFilled.clone();
            s.knownBlank = knownBlank.clone();
            for (int r = 0; r < rows; r++) s.rowCandidates[r] = new ArrayList<>(rowCandidates[r]);
            for (int c = 0; c < columns; c++) s.colCandidates[c] = new ArrayList<>(colCandidates[c]);
            return s;
        }
    }

    private static void solveRec(State state) {
        // "presolve/propagate": push every row/column candidate list to a fixpoint
        if (!propagateAll(state)) return; // contradiction -> bound: prune this branch

        // d = the most-constrained still-ambiguous line (fewest remaining candidates > 1)
        int bestSize = Integer.MAX_VALUE;
        boolean bestIsRow = true;
        int bestIndex = -1;
        for (int r = 0; r < rows; r++) {
            int size = state.rowCandidates[r].size();
            if (size > 1 && size < bestSize) { bestSize = size; bestIsRow = true; bestIndex = r; }
        }
        for (int c = 0; c < columns; c++) {
            int size = state.colCandidates[c].size();
            if (size > 1 && size < bestSize) { bestSize = size; bestIsRow = false; bestIndex = c; }
        }

        if (bestIndex == -1) {
            // if d does not exist: every line is fully resolved -> a complete assignment
            if (validNonogram(state)) solutions.add(boardToString(state));
            return; // (we don't stop the whole search here -- see note below)
        }

        // for each possible decision D (a candidate for the chosen line)
        List<Long> options = bestIsRow ? state.rowCandidates[bestIndex] : state.colCandidates[bestIndex];
        for (long candidate : options) {
            State branch = state.copy();               // S' = apply D to a private copy of the state
            if (bestIsRow) branch.rowCandidates[bestIndex] = new ArrayList<>(List.of(candidate));
            else branch.colCandidates[bestIndex] = new ArrayList<>(List.of(candidate));
            solveRec(branch);                           // recurse
        }
    }

    // propagate to a fixpoint; returns false the moment any line becomes unsatisfiable
    private static boolean propagateAll(State state) {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int r = 0; r < rows; r++) {
                List<Long> cand = state.rowCandidates[r];
                if (!filterLine(cand, state.knownFilled[r], state.knownBlank[r], columnsMask)) return false;
                long forcedOnes = commonOnes(cand, columnsMask) & ~state.knownFilled[r];
                long forcedZeros = commonZeros(cand, columnsMask) & ~state.knownBlank[r];
                if (forcedOnes != 0 || forcedZeros != 0) {
                    state.knownFilled[r] |= forcedOnes;
                    state.knownBlank[r] |= forcedZeros;
                    changed = true;
                }
            }
            for (int c = 0; c < columns; c++) {
                long colFilled = extractColumnBits(state.knownFilled, c);
                long colBlank = extractColumnBits(state.knownBlank, c);
                List<Long> cand = state.colCandidates[c];
                if (!filterLine(cand, colFilled, colBlank, rowsMask)) return false;
                long forcedOnes = commonOnes(cand, rowsMask) & ~colFilled;
                long forcedZeros = commonZeros(cand, rowsMask) & ~colBlank;
                if (forcedOnes != 0 || forcedZeros != 0) {
                    for (int r = 0; r < rows; r++) {
                        if (((forcedOnes >> r) & 1) != 0) state.knownFilled[r] |= (1L << c);
                        if (((forcedZeros >> r) & 1) != 0) state.knownBlank[r] |= (1L << c);
                    }
                    changed = true;
                }
            }
        }
        return true;
    }

    private static long extractColumnBits(long[] rowsArray, int c) {
        long bits = 0;
        for (int r = 0; r < rows; r++) if (((rowsArray[r] >> c) & 1L) != 0) bits |= (1L << r);
        return bits;
    }

    private static boolean filterLine(List<Long> candidates, long knownFilled, long knownBlank, long mask) {
        candidates.removeIf(cand -> (cand & knownBlank) != 0 || (~cand & knownFilled & mask) != 0);
        return !candidates.isEmpty();
    }

    private static long commonOnes(List<Long> candidates, long mask) {
        long common = mask;
        for (long c : candidates) common &= c;
        return common;
    }

    private static long commonZeros(List<Long> candidates, long mask) {
        long common = mask;
        for (long c : candidates) common &= (~c & mask);
        return common;
    }

    private static boolean validNonogram(State state) {
        for (int r = 0; r < rows; r++)
            if (!lineMatchesClue(state.knownFilled[r], columns, rowClues[r])) return false;
        for (int c = 0; c < columns; c++)
            if (!lineMatchesClue(extractColumnBits(state.knownFilled, c), rows, columnClues[c])) return false;
        return true;
    }

    private static boolean lineMatchesClue(long lineBits, int length, int[] clue) {
        int clueIndex = 0, runLength = 0;
        for (int i = 0; i < length; i++) {
            boolean filled = ((lineBits >> i) & 1L) != 0;
            if (filled) {
                runLength++;
            } else if (runLength > 0) {
                if (clueIndex >= clue.length || clue[clueIndex] != runLength) return false;
                clueIndex++;
                runLength = 0;
            }
        }
        if (runLength > 0) {
            if (clueIndex >= clue.length || clue[clueIndex] != runLength) return false;
            clueIndex++;
        }
        return clueIndex == clue.length;
    }

    private static List<Long> generateCandidates(int[] clue, int length) {
        List<Long> result = new ArrayList<>();
        generateCandidatesRec(clue, 0, length, 0, 0L, result);
        return result;
    }

    private static void generateCandidatesRec(int[] clue, int clueIdx, int length,
                                              int pos, long acc, List<Long> result) {
        if (clueIdx == clue.length) {
            result.add(acc);
            return;
        }
        int block = clue[clueIdx];
        int remaining = 0;
        for (int i = clueIdx; i < clue.length; i++) remaining += clue[i];
        remaining += (clue.length - clueIdx - 1);
        for (int start = pos; start + remaining <= length; start++) {
            long mask = ((1L << block) - 1) << start;
            generateCandidatesRec(clue, clueIdx + 1, length, start + block + 1, acc | mask, result);
        }
    }

    private static String boardToString(State state) {
        StringBuilder sb = new StringBuilder(rows * columns);
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < columns; c++)
                sb.append(((state.knownFilled[r] >> c) & 1L) != 0 ? '#' : '.');
        return sb.toString();
    }

    private static void writeSolutions() throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter("nonogram-my.out"))) {
            for (String solution : solutions) writer.println(solution);
        }
    }
}