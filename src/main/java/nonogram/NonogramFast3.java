package nonogram;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NonogramFast3 {
    private static final String inputFilename = "nonogram.in";
    private static final String outputFilename = "nonogram-my.out";
    private static final BufferedReader reader;

    private static int columnAmount;
    private static int rowAmount;
    private static long columnsMask;
    private static long rowsMask;

    private static int[][] columnClues;
    private static int[][] rowClues;

    private static long[][] rowCandidates;
    private static int[] rowCandidateSize;
    private static long[][] columnCandidates;
    private static int[] columnCandidateSize;

    private static long[] knownFilled;
    private static long[] knownBlank;

    private static final int KIND_ROW_SIZE = 0, KIND_COL_SIZE = 1, KIND_ROW_FILLED = 2, KIND_ROW_BLANK = 3;
    private static int[] trailKind = new int[64];
    private static int[] trailIndex = new int[64];
    private static long[] trailValue = new long[64];
    private static int trailSize = 0;

    private static final List<String> solutions = new ArrayList<>();

    static {
        try {
            reader = new BufferedReader(new FileReader(inputFilename));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        parseHeaderClues();
        parseClues();

        initializeCandidates();

        solveRec();
        writeSolutionsToFile();
    }

    private static void solveRec() {
        int mark = trailMark();
        if (!propagateAll()) {
            undoTo(mark);
            return;
        }

        boolean bestLineIsRow = true;
        int bestSize = Integer.MAX_VALUE;
        int bestIndex = -1;
        for (int r = 0; r < rowAmount; r++) {
            int size = rowCandidateSize[r];
            if (size > 1 && size < bestSize) {
                bestSize = size;
                bestIndex = r;
            }
        }
        for (int c = 0; c < columnAmount; c++) {
            int size = columnCandidateSize[c];
            if (size > 1 && size < bestSize) {
                bestLineIsRow = false;
                bestSize = size;
                bestIndex = c;
            }
        }

        if (bestIndex == -1) {
            // at this point we must have a valid nonogram:
            // -> that's an invariant kept through the whole algo
            solutions.add(boardToString());
            undoTo(mark);
            return;
        }

        int candidateSize = bestLineIsRow ? rowCandidateSize[bestIndex] : columnCandidateSize[bestIndex];
        for (int i = 0; i < candidateSize; i++) {
            int branchMark = trailMark();
            forceCandidate(bestLineIsRow, bestIndex, i);
            solveRec();
            undoTo(branchMark);
        }
        undoTo(mark);
    }

    private static boolean propagateAll() {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int r = 0; r < rowAmount; r++) {
                if (!filterLine(true, r, knownFilled[r], knownBlank[r], columnsMask)) {
                    return false;
                }

                long[] candidates = rowCandidates[r];
                int candidateSize = rowCandidateSize[r];
                long newKnownFilled = commonOnes(candidates, candidateSize, columnsMask) & ~knownFilled[r];
                long newKnownBlank = commonZeros(candidates, candidateSize, columnsMask) & ~knownBlank[r];
                if (newKnownFilled != 0) {
                    setKnownFilled(r, knownFilled[r] | newKnownFilled);
                    changed = true;
                }
                if (newKnownBlank != 0) {
                    setKnownBlank(r, knownBlank[r] | newKnownBlank);
                    changed = true;
                }
            }
            for (int c = 0; c < columnAmount; c++) {
                long colFilled = extractColumnBits(knownFilled, c);
                long colBlank = extractColumnBits(knownBlank, c);
                if (!filterLine(false, c, colFilled, colBlank, rowsMask)) {
                    return false;
                }

                long[] candidates = columnCandidates[c];
                int candidateSize = columnCandidateSize[c];
                long newKnownFilled = commonOnes(candidates, candidateSize, rowsMask) & ~colFilled;
                long newKnownBlank = commonZeros(candidates, candidateSize, rowsMask) & ~colBlank;
                if (newKnownFilled != 0 || newKnownBlank != 0) {
                    for (int r = 0; r < rowAmount; r++) {
                        if (((newKnownFilled >> r) & 1) != 0) {
                            setKnownFilled(r, knownFilled[r] | (1L << c));
                        }
                        if (((newKnownBlank >> r) & 1) != 0) {
                            setKnownBlank(r, knownBlank[r] | (1L << c));
                        }
                    }
                    changed = true;
                }
            }
        }
        return true;
    }

    private static boolean filterLine(boolean isRow, int index, long knownFilled, long knownBlank, long mask) {
        long[] candidates = isRow ? rowCandidates[index] : columnCandidates[index];
        int candidateSize = isRow ? rowCandidateSize[index] : columnCandidateSize[index];
        int oldCandidateSize = candidateSize;

        int i = 0;
        while (i < candidateSize) {
            long candidate = candidates[i];
            // check if candidate is invalid based on known filled & blank bits
            if ((candidate & knownBlank) != 0 || (~candidate & knownFilled & mask) != 0) {
                candidateSize--;
                var tmp = candidates[i];
                candidates[i] = candidates[candidateSize];
                candidates[candidateSize] = tmp;
            } else {
                i++;
            }
        }

        if (candidateSize != oldCandidateSize) {
            recordSizeChange(isRow, index, oldCandidateSize);
            if (isRow) {
                rowCandidateSize[index] = candidateSize;
            } else {
                columnCandidateSize[index] = candidateSize;
            }
        }
        return candidateSize > 0;
    }

    private static void forceCandidate(boolean isRow, int lineIndex, int candidateIndex) {
        long[] candidates = isRow ? rowCandidates[lineIndex] : columnCandidates[lineIndex];
        int candidateSize = isRow ? rowCandidateSize[lineIndex] : columnCandidateSize[lineIndex];

        if (candidateIndex != 0) {
            long tmp = candidates[0];
            candidates[0] = candidates[candidateIndex];
            candidates[candidateIndex] = tmp;
        }
        recordSizeChange(isRow, lineIndex, candidateSize);
        if (isRow) {
            rowCandidateSize[lineIndex] = 1;
        } else {
            columnCandidateSize[lineIndex] = 1;
        }
    }

    private static void initializeCandidates() {
        knownFilled = new long[rowAmount];
        knownBlank = new long[rowAmount];
        rowCandidates = new long[rowAmount][];
        rowCandidateSize = new int[rowAmount];
        columnCandidates = new long[columnAmount][];
        columnCandidateSize = new int[columnAmount];
        for (int r = 0; r < rowAmount; r++) {
            long[] arr = toArray(generateCandidates(rowClues[r], columnAmount));
            rowCandidates[r] = arr;
            rowCandidateSize[r] = arr.length;
        }
        for (int c = 0; c < columnAmount; c++) {
            long[] arr = toArray(generateCandidates(columnClues[c], rowAmount));
            columnCandidates[c] = arr;
            columnCandidateSize[c] = arr.length;
        }
    }

    private static List<Long> generateCandidates(int[] clues, int length) {
        List<Long> candidates = new ArrayList<>();
        generateCandidatesRec(clues, 0, length, 0, 0L, candidates);
        return candidates;
    }

    private static void generateCandidatesRec(int[] clues, int clueIndex, int length, int start,
                                              long candidate, List<Long> candidates) {
        if (clueIndex == clues.length) { 
            candidates.add(candidate);
            return;
        }
        
        int clue = clues[clueIndex];
        int fieldsToFill = clue;
        for (int i = clueIndex + 1; i < clues.length; i++) {
            fieldsToFill += clues[i];
        }

        // add number of gaps: one for each clue remaining after this one
        fieldsToFill += (clues.length - clueIndex - 1);

        for (int i = start; i + fieldsToFill <= length; i++) {
            // fill block of 1s for the current clue
            long block = ((1L << clue) - 1) << i;
            generateCandidatesRec(clues, clueIndex + 1, length, i + clue + 1, candidate | block, candidates);
        }
    }

    private static void parseHeaderClues() throws IOException {
        var header = reader.readLine().trim().split("\\s+");
        columnAmount = Integer.parseInt(header[0]);
        rowAmount = Integer.parseInt(header[1]);
        assert columnAmount < 63 && rowAmount < 63;
        columnsMask = (1L << columnAmount) - 1;
        rowsMask = (1L << rowAmount) - 1;
    }

    private static void parseClues() throws Exception {
        rowClues = new int[rowAmount][];
        for (int r = 0; r < rowAmount; r++) rowClues[r] = parseClueLine();
        columnClues = new int[columnAmount][];
        for (int c = 0; c < columnAmount; c++) columnClues[c] = parseClueLine();
    }

    private static int[] parseClueLine() throws IOException {
        var line = reader.readLine();
        if (line == null || line.isBlank()) return new int[0];
        var tokens = line.trim().split("\\s+");
        var clues = new int[tokens.length];
        for (int i = 0; i < tokens.length; i++) clues[i] = Integer.parseInt(tokens[i]);
        return clues;
    }


    private static String boardToString() {
        StringBuilder sb = new StringBuilder(rowAmount * columnAmount);
        for (int r = 0; r < rowAmount; r++) {
            for (int c = 0; c < columnAmount; c++) {
                sb.append(((knownFilled[r] >> c) & 1L) != 0 ? '#' : '.');
            }
        }
        return sb.toString();
    }

    private static void writeSolutionsToFile() throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFilename))) {
            for (String solution : solutions) {
                writer.println(solution);
            }
        }
    }

    private static void setKnownFilled(int r, long newValue) {
        trailPush(KIND_ROW_FILLED, r, knownFilled[r]);
        knownFilled[r] = newValue;
    }

    private static void setKnownBlank(int r, long newValue) {
        trailPush(KIND_ROW_BLANK, r, knownBlank[r]);
        knownBlank[r] = newValue;
    }

    private static void undoTo(int mark) {
        while (trailSize > mark) {
            trailSize--;
            int index = trailIndex[trailSize];
            long oldValue = trailValue[trailSize];
            switch (trailKind[trailSize]) {
                case KIND_ROW_SIZE -> rowCandidateSize[index] = (int) oldValue;
                case KIND_COL_SIZE -> columnCandidateSize[index] = (int) oldValue;
                case KIND_ROW_FILLED -> knownFilled[index] = oldValue;
                case KIND_ROW_BLANK -> knownBlank[index] = oldValue;
            }
        }
    }

    private static void recordSizeChange(boolean isRow, int index, int oldSize) {
        trailPush(isRow ? KIND_ROW_SIZE : KIND_COL_SIZE, index, oldSize);
    }

    private static int trailMark() {
        return trailSize;
    }

    private static void trailPush(int kind, int index, long oldValue) {
        if (trailSize == trailKind.length) growTrail();
        trailKind[trailSize] = kind;
        trailIndex[trailSize] = index;
        trailValue[trailSize] = oldValue;
        trailSize++;
    }

    private static void growTrail() {
        int newCap = trailKind.length * 2;
        trailKind = Arrays.copyOf(trailKind, newCap);
        trailIndex = Arrays.copyOf(trailIndex, newCap);
        trailValue = Arrays.copyOf(trailValue, newCap);
    }

    private static long commonOnes(long[] lineCandidates, int size, long mask) {
        long common = mask;
        for (int i = 0; i < size; i++) {
            common &= lineCandidates[i];
        }
        return common;
    }

    private static long commonZeros(long[] lineCandidates, int size, long mask) {
        long common = mask;
        for (int i = 0; i < size; i++) {
            common &= (~lineCandidates[i] & mask);
        }
        return common;
    }

    private static long extractColumnBits(long[] rows, int columnIndex) {
        long bits = 0;
        for (int r = 0; r < rowAmount; r++) {
            if (((rows[r] >> columnIndex) & 1L) != 0) {
                bits |= (1L << r);
            }
        }
        return bits;
    }

    private static long[] toArray(List<Long> list) {
        long[] arr = new long[list.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = list.get(i);
        return arr;
    }
}