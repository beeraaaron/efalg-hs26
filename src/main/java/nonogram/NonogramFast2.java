package nonogram;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class NonogramFast2 {
    private static final BufferedReader reader;

    private static int columns;
    private static int rows;
    private static long columnsMask;
    private static long rowsMask;

    private static int[][] columnClues;
    private static int[][] rowClues;

    // reversible candidate storage: only arr[0..size) is "active"
    private static long[][] rowCandidateArr;
    private static int[] rowCandidateSize;
    private static long[][] colCandidateArr;
    private static int[] colCandidateSize;

    private static long[] knownFilled;
    private static long[] knownBlank;

    // undo log: one entry per state mutation, undone in LIFO order
    private static final class TrailEntry {
        static final int ROW_SIZE = 0, COL_SIZE = 1, ROW_FILLED = 2, ROW_BLANK = 3;
        final int kind, index;
        final long oldValue;
        TrailEntry(int kind, int index, long oldValue) {
            this.kind = kind; this.index = index; this.oldValue = oldValue;
        }
    }
    private static final List<TrailEntry> trail = new ArrayList<>();

    private static final List<String> solutions = new ArrayList<>();

    static {
        try {
            reader = new BufferedReader(new FileReader("nonogram-my.in"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        var header = reader.readLine().trim().split("\\s+");
        columns = Integer.parseInt(header[0]);
        rows = Integer.parseInt(header[1]);
        assert columns < 63 && rows < 63;
        columnsMask = (1L << columns) - 1;
        rowsMask = (1L << rows) - 1;

        parseClues();

        knownFilled = new long[rows];
        knownBlank = new long[rows];
        rowCandidateArr = new long[rows][];
        rowCandidateSize = new int[rows];
        colCandidateArr = new long[columns][];
        colCandidateSize = new int[columns];

        for (int r = 0; r < rows; r++) {
            long[] arr = toArray(generateCandidates(rowClues[r], columns));
            rowCandidateArr[r] = arr;
            rowCandidateSize[r] = arr.length;
        }
        for (int c = 0; c < columns; c++) {
            long[] arr = toArray(generateCandidates(columnClues[c], rows));
            colCandidateArr[c] = arr;
            colCandidateSize[c] = arr.length;
        }

        solveRec();
        writeSolutions();
    }

    private static long[] toArray(List<Long> list) {
        long[] arr = new long[list.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = list.get(i);
        return arr;
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
    // trail: record + undo
    // ------------------------------------------------------------------
    private static int trailMark() {
        return trail.size();
    }

    private static void undoTo(int mark) {
        for (int i = trail.size() - 1; i >= mark; i--) {
            TrailEntry e = trail.get(i);
            switch (e.kind) {
                case TrailEntry.ROW_SIZE -> rowCandidateSize[e.index] = (int) e.oldValue;
                case TrailEntry.COL_SIZE -> colCandidateSize[e.index] = (int) e.oldValue;
                case TrailEntry.ROW_FILLED -> knownFilled[e.index] = e.oldValue;
                case TrailEntry.ROW_BLANK -> knownBlank[e.index] = e.oldValue;
            }
        }
        while (trail.size() > mark) trail.remove(trail.size() - 1);
    }

    private static void setKnownFilled(int r, long newValue) {
        trail.add(new TrailEntry(TrailEntry.ROW_FILLED, r, knownFilled[r]));
        knownFilled[r] = newValue;
    }

    private static void setKnownBlank(int r, long newValue) {
        trail.add(new TrailEntry(TrailEntry.ROW_BLANK, r, knownBlank[r]));
        knownBlank[r] = newValue;
    }

    // ------------------------------------------------------------------
    // search
    // ------------------------------------------------------------------
    private static void solveRec() {
        int mark = trailMark();
        if (!propagateAll()) {
            undoTo(mark); // contradiction -> bound: prune
            return;
        }

        int bestSize = Integer.MAX_VALUE;
        boolean bestIsRow = true;
        int bestIndex = -1;
        for (int r = 0; r < rows; r++) {
            int size = rowCandidateSize[r];
            if (size > 1 && size < bestSize) { bestSize = size; bestIsRow = true; bestIndex = r; }
        }
        for (int c = 0; c < columns; c++) {
            int size = colCandidateSize[c];
            if (size > 1 && size < bestSize) { bestSize = size; bestIsRow = false; bestIndex = c; }
        }

        if (bestIndex == -1) {
            if (validNonogram()) solutions.add(boardToString());
            undoTo(mark);
            return;
        }

        long[] arr = bestIsRow ? rowCandidateArr[bestIndex] : colCandidateArr[bestIndex];
        int size = bestIsRow ? rowCandidateSize[bestIndex] : colCandidateSize[bestIndex];
        long[] options = new long[size]; // snapshot values -- arr's order may shift between branches
        System.arraycopy(arr, 0, options, 0, size);

        for (long candidate : options) {
            int branchMark = trailMark();
            forceCandidate(bestIsRow, bestIndex, candidate);
            solveRec();
            undoTo(branchMark);
        }

        undoTo(mark);
    }

    // reduces a line's active window to just `candidate` (O(size) to locate it, O(1) to force)
    private static void forceCandidate(boolean isRow, int index, long candidate) {
        long[] arr = isRow ? rowCandidateArr[index] : colCandidateArr[index];
        int size = isRow ? rowCandidateSize[index] : colCandidateSize[index];

        int pos = 0;
        while (arr[pos] != candidate) pos++;
        if (pos != 0) {
            long tmp = arr[0]; arr[0] = arr[pos]; arr[pos] = tmp; // reordering only, no trail needed
        }
        recordSizeChange(isRow, index, size);
        if (isRow) rowCandidateSize[index] = 1; else colCandidateSize[index] = 1;
    }

    private static void recordSizeChange(boolean isRow, int index, int oldSize) {
        trail.add(new TrailEntry(isRow ? TrailEntry.ROW_SIZE : TrailEntry.COL_SIZE, index, oldSize));
    }

    private static boolean propagateAll() {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int r = 0; r < rows; r++) {
                if (!filterLine(true, r, knownFilled[r], knownBlank[r], columnsMask)) return false;
                long[] arr = rowCandidateArr[r];
                int size = rowCandidateSize[r];
                long forcedOnes = commonOnes(arr, size, columnsMask) & ~knownFilled[r];
                long forcedZeros = commonZeros(arr, size, columnsMask) & ~knownBlank[r];
                if (forcedOnes != 0) { setKnownFilled(r, knownFilled[r] | forcedOnes); changed = true; }
                if (forcedZeros != 0) { setKnownBlank(r, knownBlank[r] | forcedZeros); changed = true; }
            }
            for (int c = 0; c < columns; c++) {
                long colFilled = extractColumnBits(knownFilled, c);
                long colBlank = extractColumnBits(knownBlank, c);
                if (!filterLine(false, c, colFilled, colBlank, rowsMask)) return false;
                long[] arr = colCandidateArr[c];
                int size = colCandidateSize[c];
                long forcedOnes = commonOnes(arr, size, rowsMask) & ~colFilled;
                long forcedZeros = commonZeros(arr, size, rowsMask) & ~colBlank;
                if (forcedOnes != 0 || forcedZeros != 0) {
                    for (int r = 0; r < rows; r++) {
                        if (((forcedOnes >> r) & 1) != 0) setKnownFilled(r, knownFilled[r] | (1L << c));
                        if (((forcedZeros >> r) & 1) != 0) setKnownBlank(r, knownBlank[r] | (1L << c));
                    }
                    changed = true;
                }
            }
        }
        return true;
    }

    // swap-removes invalid candidates from the active window; one trail entry for the whole pass
    private static boolean filterLine(boolean isRow, int index, long knownFilledBits, long knownBlankBits, long mask) {
        long[] arr = isRow ? rowCandidateArr[index] : colCandidateArr[index];
        int size = isRow ? rowCandidateSize[index] : colCandidateSize[index];
        int oldSize = size;

        int i = 0;
        while (i < size) {
            long cand = arr[i];
            boolean invalid = (cand & knownBlankBits) != 0 || (~cand & knownFilledBits & mask) != 0;
            if (invalid) {
                size--;
                long tmp = arr[i]; arr[i] = arr[size]; arr[size] = tmp;
            } else {
                i++;
            }
        }

        if (size != oldSize) {
            recordSizeChange(isRow, index, oldSize);
            if (isRow) rowCandidateSize[index] = size; else colCandidateSize[index] = size;
        }
        return size > 0;
    }

    private static long commonOnes(long[] arr, int size, long mask) {
        long common = mask;
        for (int i = 0; i < size; i++) {
            common &= arr[i];
        }
        return common;
    }

    private static long commonZeros(long[] arr, int size, long mask) {
        long common = mask;
        for (int i = 0; i < size; i++) {
            common &= (~arr[i] & mask);
        }
        return common;
    }

    private static long extractColumnBits(long[] rowsArray, int c) {
        long bits = 0;
        for (int r = 0; r < rows; r++) {
            if (((rowsArray[r] >> c) & 1L) != 0) {
                bits |= (1L << r);
            }
        }
        return bits;
    }

    private static boolean validNonogram() {
        for (int r = 0; r < rows; r++)
            if (!lineMatchesClue(knownFilled[r], columns, rowClues[r])) return false;
        for (int c = 0; c < columns; c++)
            if (!lineMatchesClue(extractColumnBits(knownFilled, c), rows, columnClues[c])) return false;
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

    private static void generateCandidatesRec(int[] clue, int clueIndex, int length,
                                              int pos, long acc, List<Long> result) {
        if (clueIndex == clue.length) {
            result.add(acc);
            return;
        }
        int block = clue[clueIndex];
        int remaining = 0;
        for (int i = clueIndex; i < clue.length; i++) {
            remaining += clue[i];
        }
        remaining += (clue.length - clueIndex - 1);
        for (int start = pos; start + remaining <= length; start++) {
            long mask = ((1L << block) - 1) << start;
            generateCandidatesRec(clue, clueIndex + 1, length, start + block + 1, acc | mask, result);
        }
    }

    private static String boardToString() {
        var sb = new StringBuilder(rows * columns);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                sb.append(((knownFilled[r] >> c) & 1L) != 0 ? '#' : '.');
            }
        }
        return sb.toString();
    }

    private static void writeSolutions() throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter("nonogram-my.out"))) {
            for (String solution : solutions) {
                writer.println(solution);
            }
        }
    }
}