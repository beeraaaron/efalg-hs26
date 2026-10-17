package nonogram;

import java.io.*;
import java.util.Arrays;

public class NonogramSolver {
    private static final String inputFilename = "nonogram-mountain.in";
    private static final String outputFilename = "nonogram-mountain-my.out";
    private static final BufferedReader reader;

    private static int columnAmount;
    private static int rowAmount;

    private static int[][] columnClues;
    private static int[][] rowClues;

    private static String[] solutions = new String[8];
    private static int solutionsSize = 0;

    private static final int KIND_FAST_ROW_SIZE = 0, KIND_FAST_COL_SIZE = 1,
            KIND_FAST_ROW_FILLED = 2, KIND_FAST_ROW_BLANK = 3,
            KIND_WIDE_ROW_SIZE = 4, KIND_WIDE_COL_SIZE = 5,
            KIND_WIDE_KNOWN_FILLED_WORD = 6, KIND_WIDE_KNOWN_BLANK_WORD = 7;
    private static int[] trailKind = new int[64];
    private static int[] trailIndex = new int[64];
    private static long[] trailValue = new long[64];
    private static int trailSize = 0;

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

        // A single long can only address 64 bit positions
        if (columnAmount <= 64 && rowAmount <= 64) {
            if (initializeCandidatesFast()) {
                solveRecFast();
            }
        } else {
            if (initializeCandidatesWide()) {
                solveRecWide();
            }
        }

        writeSolutionsToFile();
    }

    private static void parseHeaderClues() throws IOException {
        var header = reader.readLine().trim().split("\\s+");
        columnAmount = Integer.parseInt(header[0]);
        rowAmount = Integer.parseInt(header[1]);
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

    private static int countCandidates(int[] clues, int length) {
        int amountOfClues = clues.length;
        if (amountOfClues == 0) return 1;
        int freeCells = length - calcMinLineSizes(clues)[0];
        if (freeCells < 0) return 0;


        int n = freeCells + amountOfClues;
        int r = Math.min(amountOfClues, freeCells);
        long res = 1;
        for (int i = 1; i <= r; i++) {
            res = res * (n - r + i) / i; // stays exact at every step
            if (res > Integer.MAX_VALUE) {
                throw new IllegalStateException("Too many candidates for one line");
            }
        }
        return (int) res;
    }

    private static int[] calcMinLineSizes(int[] clues) {
        int amountOfClues = clues.length;
        int[] need = new int[amountOfClues];

        int totalBlockSize = 0;
        for (int i = amountOfClues - 1; i >= 0; i--) {
            totalBlockSize += clues[i];
            // need = totalBlockSize + Mandatory Gap of 1 after each Block
            need[i] = totalBlockSize + (amountOfClues - 1 - i);
        }
        return need;
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

    private static void addSolution(String solution) {
        if (solutionsSize == solutions.length) growSolutions();
        solutions[solutionsSize++] = solution;
    }

    private static void growSolutions() {
        solutions = Arrays.copyOf(solutions, solutions.length * 2);
    }

    private static void writeSolutionsToFile() throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFilename))) {
            for (int i = 0; i < solutionsSize; i++) {
                writer.println(solutions[i]);
            }
        }
    }

    // FAST PATH -- one `long` per line. Used when columnAmount <= 64 and rowAmount <= 64
    private static long fastColumnsMask, fastRowsMask;
    private static long[] fastKnownFilled, fastKnownBlank;
    private static long[][] fastRowCandidates, fastColumnCandidates;
    private static int[] fastRowCandidateSize, fastColumnCandidateSize;

    private static boolean initializeCandidatesFast() {
        fastColumnsMask = (columnAmount == 64) ? -1L : (1L << columnAmount) - 1;
        fastRowsMask = (rowAmount == 64) ? -1L : (1L << rowAmount) - 1;

        fastKnownFilled = new long[rowAmount];
        fastKnownBlank = new long[rowAmount];
        fastRowCandidates = new long[rowAmount][];
        fastRowCandidateSize = new int[rowAmount];
        fastColumnCandidates = new long[columnAmount][];
        fastColumnCandidateSize = new int[columnAmount];

        for (int r = 0; r < rowAmount; r++) {
            fastRowCandidates[r] = generateCandidatesFast(rowClues[r], columnAmount);
            fastRowCandidateSize[r] = fastRowCandidates[r].length;
            // if there are no candidates for a row: fail early
            if (fastRowCandidateSize[r] == 0) {
                return false;
            }
        }
        for (int c = 0; c < columnAmount; c++) {
            fastColumnCandidates[c] = generateCandidatesFast(columnClues[c], rowAmount);
            fastColumnCandidateSize[c] = fastColumnCandidates[c].length;
            // if there are no candidates for a column: fail early
            if (fastColumnCandidateSize[c] == 0) {
                return false;
            }
        }
        return true;
    }

    private static void solveRecFast() {
        int mark = trailMark();
        if (!propagateAllFast()) {
            undoToFast(mark);
            return;
        }

        boolean bestLineIsRow = true;
        int bestSize = Integer.MAX_VALUE;
        int bestIndex = -1;
        for (int r = 0; r < rowAmount; r++) {
            int size = fastRowCandidateSize[r];
            if (size > 1 && size < bestSize) {
                bestSize = size;
                bestIndex = r;
            }
        }
        for (int c = 0; c < columnAmount; c++) {
            int size = fastColumnCandidateSize[c];
            if (size > 1 && size < bestSize) {
                bestLineIsRow = false;
                bestSize = size;
                bestIndex = c;
            }
        }

        if (bestIndex == -1) {
            // at this point we must have a valid nonogram -> that's an invariant kept through the whole algo
            addSolution(boardToStringFast());
            undoToFast(mark);
            return;
        }

        int candidateSize = bestLineIsRow ? fastRowCandidateSize[bestIndex] : fastColumnCandidateSize[bestIndex];
        for (int i = 0; i < candidateSize; i++) {
            int branchMark = trailMark();
            forceCandidateFast(bestLineIsRow, bestIndex, i);
            solveRecFast();
            undoToFast(branchMark);
        }
        undoToFast(mark);
    }

    private static boolean propagateAllFast() {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int r = 0; r < rowAmount; r++) {
                if (!filterLineFast(true, r, fastKnownFilled[r], fastKnownBlank[r], fastColumnsMask)) {
                    return false;
                }

                long[] candidates = fastRowCandidates[r];
                int candidateSize = fastRowCandidateSize[r];
                long newKnownFilled = commonOnesFast(candidates, candidateSize, fastColumnsMask) & ~fastKnownFilled[r];
                long newKnownBlank = commonZerosFast(candidates, candidateSize, fastColumnsMask) & ~fastKnownBlank[r];
                if (newKnownFilled != 0) {
                    setKnownFilledFast(r, fastKnownFilled[r] | newKnownFilled);
                    changed = true;
                }
                if (newKnownBlank != 0) {
                    setKnownBlankFast(r, fastKnownBlank[r] | newKnownBlank);
                    changed = true;
                }
            }
            for (int c = 0; c < columnAmount; c++) {
                long colFilled = extractColumnBitsFast(fastKnownFilled, c);
                long colBlank = extractColumnBitsFast(fastKnownBlank, c);
                if (!filterLineFast(false, c, colFilled, colBlank, fastRowsMask)) {
                    return false;
                }

                long[] candidates = fastColumnCandidates[c];
                int candidateSize = fastColumnCandidateSize[c];
                long newKnownFilled = commonOnesFast(candidates, candidateSize, fastRowsMask) & ~colFilled;
                long newKnownBlank = commonZerosFast(candidates, candidateSize, fastRowsMask) & ~colBlank;
                if (newKnownFilled != 0 || newKnownBlank != 0) {
                    for (int r = 0; r < rowAmount; r++) {
                        if (((newKnownFilled >> r) & 1) != 0) {
                            setKnownFilledFast(r, fastKnownFilled[r] | (1L << c));
                        }
                        if (((newKnownBlank >> r) & 1) != 0) {
                            setKnownBlankFast(r, fastKnownBlank[r] | (1L << c));
                        }
                    }
                    changed = true;
                }
            }
        }
        return true;
    }

    private static boolean filterLineFast(boolean isRow, int index, long knownFilled, long knownBlank, long mask) {
        long[] candidates = isRow ? fastRowCandidates[index] : fastColumnCandidates[index];
        int candidateSize = isRow ? fastRowCandidateSize[index] : fastColumnCandidateSize[index];
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
            recordFastSizeChange(isRow, index, oldCandidateSize);
            if (isRow) {
                fastRowCandidateSize[index] = candidateSize;
            } else {
                fastColumnCandidateSize[index] = candidateSize;
            }
        }
        return candidateSize > 0;
    }

    private static void forceCandidateFast(boolean isRow, int lineIndex, int candidateIndex) {
        long[] candidates = isRow ? fastRowCandidates[lineIndex] : fastColumnCandidates[lineIndex];
        int candidateSize = isRow ? fastRowCandidateSize[lineIndex] : fastColumnCandidateSize[lineIndex];

        if (candidateIndex != 0) {
            long tmp = candidates[0];
            candidates[0] = candidates[candidateIndex];
            candidates[candidateIndex] = tmp;
        }
        recordFastSizeChange(isRow, lineIndex, candidateSize);
        if (isRow) {
            fastRowCandidateSize[lineIndex] = 1;
        } else {
            fastColumnCandidateSize[lineIndex] = 1;
        }
    }

    public static long[] generateCandidatesFast(int[] clues, int length) {
        int count = countCandidates(clues, length);
        long[] out = new long[count];
        if (count == 0) {
            return out;
        }

        generateCandidatesRecFast(clues, calcMinLineSizes(clues), 0, length, 0, 0L, out, 0);
        return out;
    }

    private static int generateCandidatesRecFast(int[] clues, int[] need, int clueIndex, int length, int start,
                               long candidate, long[] out, int pos) {
        if (clueIndex == clues.length) {
            out[pos] = candidate;
            return pos + 1;
        }

        int clue = clues[clueIndex];
        int last = length - need[clueIndex];
        // fill block of 1s for the current clue
        long baseBlock = (clue == 64) ? -1L : ((1L << clue) - 1);
        for (int i = start; i <= last; i++) {
            // position block
            long positionedBlock = baseBlock << i;
            pos = generateCandidatesRecFast(clues, need, clueIndex + 1, length, i + clue + 1, candidate | positionedBlock, out, pos);
        }
        return pos;
    }

    private static long extractColumnBitsFast(long[] rows, int columnIndex) {
        long bits = 0;
        for (int r = 0; r < rowAmount; r++) {
            if (((rows[r] >> columnIndex) & 1L) != 0) {
                bits |= (1L << r);
            }
        }
        return bits;
    }

    private static long commonOnesFast(long[] lineCandidates, int size, long mask) {
        long common = mask;
        for (int i = 0; i < size; i++) {
            common &= lineCandidates[i];
        }
        return common;
    }

    private static long commonZerosFast(long[] lineCandidates, int size, long mask) {
        long common = mask;
        for (int i = 0; i < size; i++) {
            common &= (~lineCandidates[i] & mask);
        }
        return common;
    }

    private static void setKnownFilledFast(int r, long newValue) {
        trailPush(KIND_FAST_ROW_FILLED, r, fastKnownFilled[r]);
        fastKnownFilled[r] = newValue;
    }

    private static void setKnownBlankFast(int r, long newValue) {
        trailPush(KIND_FAST_ROW_BLANK, r, fastKnownBlank[r]);
        fastKnownBlank[r] = newValue;
    }

    private static void recordFastSizeChange(boolean isRow, int index, int oldSize) {
        trailPush(isRow ? KIND_FAST_ROW_SIZE : KIND_FAST_COL_SIZE, index, oldSize);
    }

    private static void undoToFast(int mark) {
        while (trailSize > mark) {
            trailSize--;
            int index = trailIndex[trailSize];
            long oldValue = trailValue[trailSize];
            switch (trailKind[trailSize]) {
                case KIND_FAST_ROW_SIZE -> fastRowCandidateSize[index] = (int) oldValue;
                case KIND_FAST_COL_SIZE -> fastColumnCandidateSize[index] = (int) oldValue;
                case KIND_FAST_ROW_FILLED -> fastKnownFilled[index] = oldValue;
                case KIND_FAST_ROW_BLANK -> fastKnownBlank[index] = oldValue;
            }
        }
    }

    private static String boardToStringFast() {
        StringBuilder sb = new StringBuilder(rowAmount * columnAmount);
        for (int r = 0; r < rowAmount; r++)
            for (int c = 0; c < columnAmount; c++)
                sb.append(((fastKnownFilled[r] >> c) & 1L) != 0 ? '#' : '.');
        return sb.toString();
    }

    // WIDE PATH -- an array of `long` words per line.
    private static int wideRowWords, wideColWords;
    private static long wideRowFinalWordMask, wideColFinalWordMask;
    private static long[] wideKnownFilled, wideKnownBlank;
    private static long[][] wideRowCandidates, wideColumnCandidates;
    private static int[] wideRowCandidateSize, wideColumnCandidateSize;
    private static long[] wideColFilledScratch, wideColBlankScratch;
    private static long[] wideForcedOnesScratchRow, wideForcedZerosScratchRow;
    private static long[] wideForcedOnesScratchCol, wideForcedZerosScratchCol;

    private static boolean initializeCandidatesWide() {
        wideRowWords = (columnAmount + 63) >>> 6;
        wideColWords = (rowAmount + 63) >>> 6;

        int rowLastBits = columnAmount - (wideRowWords - 1) * 64;
        wideRowFinalWordMask = (rowLastBits == 64) ? -1L : ((1L << rowLastBits) - 1);
        int colLastBits = rowAmount - (wideColWords - 1) * 64;
        wideColFinalWordMask = (colLastBits == 64) ? -1L : ((1L << colLastBits) - 1);

        wideKnownFilled = new long[rowAmount * wideRowWords];
        wideKnownBlank = new long[rowAmount * wideRowWords];

        wideRowCandidates = new long[rowAmount][];
        wideRowCandidateSize = new int[rowAmount];
        wideColumnCandidates = new long[columnAmount][];
        wideColumnCandidateSize = new int[columnAmount];

        for (int r = 0; r < rowAmount; r++) {
            wideRowCandidates[r] = generateCandidatesWide(rowClues[r], columnAmount, wideRowWords);
            wideRowCandidateSize[r] = wideRowCandidates[r].length / wideRowWords;
            // if there are no candidates for a row: fail early
            if (wideRowCandidateSize[r] == 0) {
                return false;
            }
        }
        for (int c = 0; c < columnAmount; c++) {
            wideColumnCandidates[c] = generateCandidatesWide(columnClues[c], rowAmount, wideColWords);
            wideColumnCandidateSize[c] = wideColumnCandidates[c].length / wideColWords;
            // if there are no candidates for a row: fail early
            if (wideColumnCandidateSize[c] == 0) {
                return false;
            }
        }

        wideColFilledScratch = new long[wideColWords];
        wideColBlankScratch = new long[wideColWords];
        wideForcedOnesScratchRow = new long[wideRowWords];
        wideForcedZerosScratchRow = new long[wideRowWords];
        wideForcedOnesScratchCol = new long[wideColWords];
        wideForcedZerosScratchCol = new long[wideColWords];
        return true;
    }

    private static void solveRecWide() {
        int mark = trailMark();
        if (!propagateAllWide()) {
            undoToWide(mark);
            return;
        }

        boolean bestLineIsRow = true;
        int bestSize = Integer.MAX_VALUE;
        int bestIndex = -1;
        for (int r = 0; r < rowAmount; r++) {
            int size = wideRowCandidateSize[r];
            if (size > 1 && size < bestSize) { bestSize = size; bestIndex = r; }
        }
        for (int c = 0; c < columnAmount; c++) {
            int size = wideColumnCandidateSize[c];
            if (size > 1 && size < bestSize) { bestLineIsRow = false; bestSize = size; bestIndex = c; }
        }

        if (bestIndex == -1) {
            addSolution(boardToStringWide());
            undoToWide(mark);
            return;
        }

        int candidateSize = bestLineIsRow ? wideRowCandidateSize[bestIndex] : wideColumnCandidateSize[bestIndex];
        for (int i = 0; i < candidateSize; i++) {
            int branchMark = trailMark();
            forceCandidateWide(bestLineIsRow, bestIndex, i);
            solveRecWide();
            undoToWide(branchMark);
        }
        undoToWide(mark);
    }

    private static boolean propagateAllWide() {
        boolean changed = true;
        while (changed) {
            changed = false;

            for (int r = 0; r < rowAmount; r++) {
                int base = r * wideRowWords;
                if (!filterLineWide(true, r, wideKnownFilled, base, wideKnownBlank, base, wideRowWords)) {
                    return false;
                }

                long[] candidates = wideRowCandidates[r];
                int size = wideRowCandidateSize[r];
                commonOnesWide(candidates, size, wideRowWords, wideForcedOnesScratchRow);
                commonZerosWide(candidates, size, wideRowWords, wideRowFinalWordMask, wideForcedZerosScratchRow);

                for (int w = 0; w < wideRowWords; w++) {
                    wideForcedOnesScratchRow[w] &= ~wideKnownFilled[base + w];
                    wideForcedZerosScratchRow[w] &= ~wideKnownBlank[base + w];
                    if (wideForcedOnesScratchRow[w] != 0) {
                        setKnownFilledWordWide(base + w, wideKnownFilled[base + w] | wideForcedOnesScratchRow[w]);
                        changed = true;
                    }
                    if (wideForcedZerosScratchRow[w] != 0) {
                        setKnownBlankWordWide(base + w, wideKnownBlank[base + w] | wideForcedZerosScratchRow[w]);
                        changed = true;
                    }
                }
            }

            for (int c = 0; c < columnAmount; c++) {
                extractColumnWordsWide(wideKnownFilled, c, wideColFilledScratch);
                extractColumnWordsWide(wideKnownBlank, c, wideColBlankScratch);
                if (!filterLineWide(false, c, wideColFilledScratch, 0, wideColBlankScratch, 0, wideColWords)) {
                    return false;
                }

                long[] candidates = wideColumnCandidates[c];
                int size = wideColumnCandidateSize[c];
                commonOnesWide(candidates, size, wideColWords, wideForcedOnesScratchCol);
                commonZerosWide(candidates, size, wideColWords, wideColFinalWordMask, wideForcedZerosScratchCol);

                boolean anyForced = false;
                for (int w = 0; w < wideColWords; w++) {
                    wideForcedOnesScratchCol[w] &= ~wideColFilledScratch[w];
                    wideForcedZerosScratchCol[w] &= ~wideColBlankScratch[w];
                    if (wideForcedOnesScratchCol[w] != 0 || wideForcedZerosScratchCol[w] != 0) {
                        anyForced = true;
                    }
                }

                if (anyForced) {
                    int cWord = c >>> 6, cBit = c & 63;
                    for (int r = 0; r < rowAmount; r++) {
                        boolean setOne = ((wideForcedOnesScratchCol[r >>> 6] >>> (r & 63)) & 1L) != 0;
                        boolean setZero = ((wideForcedZerosScratchCol[r >>> 6] >>> (r & 63)) & 1L) != 0;
                        if (!setOne && !setZero) continue;

                        int flatIdx = r * wideRowWords + cWord;
                        if (setOne) {
                            setKnownFilledWordWide(flatIdx, wideKnownFilled[flatIdx] | (1L << cBit));
                        }
                        if (setZero) {
                            setKnownBlankWordWide(flatIdx, wideKnownBlank[flatIdx] | (1L << cBit));
                        }
                    }
                    changed = true;
                }
            }
        }
        return true;
    }

    private static boolean filterLineWide(boolean isRow, int index, long[] knownFilled, int knownFilledBase,
                                          long[] knownBlank, int knownBlankBase, int words) {
        long[] candidates = isRow ? wideRowCandidates[index] : wideColumnCandidates[index];
        int candidateSize = isRow ? wideRowCandidateSize[index] : wideColumnCandidateSize[index];
        int oldCandidateSize = candidateSize;

        int i = 0;
        while (i < candidateSize) {
            int base = i * words;
            boolean invalid = false;
            for (int w = 0; w < words; w++) {
                long candidate = candidates[base + w];
                long kf = knownFilled[knownFilledBase + w];
                long kb = knownBlank[knownBlankBase + w];
                if ((candidate & kb) != 0 || (~candidate & kf) != 0) { invalid = true; break; }
            }
            if (invalid) {
                candidateSize--;
                swapBlocksWide(candidates, i, candidateSize, words);
            } else {
                i++;
            }
        }

        if (candidateSize != oldCandidateSize) {
            recordWideSizeChange(isRow, index, oldCandidateSize);
            if (isRow) {
                wideRowCandidateSize[index] = candidateSize;
            } else {
                wideColumnCandidateSize[index] = candidateSize;
            }
        }
        return candidateSize > 0;
    }

    private static void forceCandidateWide(boolean isRow, int lineIndex, int candidateIndex) {
        long[] candidates = isRow ? wideRowCandidates[lineIndex] : wideColumnCandidates[lineIndex];
        int candidateSize = isRow ? wideRowCandidateSize[lineIndex] : wideColumnCandidateSize[lineIndex];
        int words = isRow ? wideRowWords : wideColWords;

        if (candidateIndex != 0) {
            swapBlocksWide(candidates, 0, candidateIndex, words);
        }
        recordWideSizeChange(isRow, lineIndex, candidateSize);
        if (isRow) {
            wideRowCandidateSize[lineIndex] = 1;
        } else {
            wideColumnCandidateSize[lineIndex] = 1;
        }
    }

    private static void swapBlocksWide(long[] arr, int posA, int posB, int words) {
        if (posA == posB) return;
        int baseA = posA * words, baseB = posB * words;
        for (int w = 0; w < words; w++) {
            long tmp = arr[baseA + w];
            arr[baseA + w] = arr[baseB + w];
            arr[baseB + w] = tmp;
        }
    }

    public static long[] generateCandidatesWide(int[] clues, int length, int words) {
        int count = countCandidates(clues, length);
        long[] out = new long[count * words];
        if (count == 0) {
            return out;
        }

        long[] acc = new long[words];
        generateCandidatesRecWide(clues, calcMinLineSizes(clues), 0, length, 0, acc, out, 0, words);
        return out;
    }

    private static int generateCandidatesRecWide(int[] clues, int[] need, int clueIndex, int length, int start,
                                                 long[] acc, long[] out, int pos, int words) {
        if (clueIndex == clues.length) {
            System.arraycopy(acc, 0, out, pos * words, words);
            return pos + 1;
        }

        int clue = clues[clueIndex];
        int last = length - need[clueIndex];

        for (int i = start; i <= last; i++) {
            setBitsWide(acc, i, clue);
            pos = generateCandidatesRecWide(clues, need, clueIndex + 1, length, i + clue + 1, acc, out, pos, words);
            clearBitsWide(acc, i, clue);
        }
        return pos;
    }

    private static void setBitsWide(long[] words, int start, int len) {
        int end = start + len;
        int w0 = start >>> 6, w1 = (end - 1) >>> 6;
        if (w0 == w1) {
            long m = (len == 64) ? -1L : (((1L << len) - 1) << (start & 63));
            words[w0] |= m;
        } else {
            words[w0] |= (-1L << (start & 63));
            for (int w = w0 + 1; w < w1; w++) {
                words[w] = -1L;
            }
            int lastBits = end - w1 * 64;
            words[w1] |= (lastBits == 64) ? -1L : ((1L << lastBits) - 1);
        }
    }

    private static void clearBitsWide(long[] words, int start, int len) {
        int end = start + len;
        int w0 = start >>> 6, w1 = (end - 1) >>> 6;
        if (w0 == w1) {
            long m = (len == 64) ? -1L : (((1L << len) - 1) << (start & 63));
            words[w0] &= ~m;
        } else {
            words[w0] &= ~(-1L << (start & 63));
            for (int w = w0 + 1; w < w1; w++) {
                words[w] = 0L;
            }
            int lastBits = end - w1 * 64;
            long m = (lastBits == 64) ? -1L : ((1L << lastBits) - 1);
            words[w1] &= ~m;
        }
    }

    private static void setKnownFilledWordWide(int flatIndex, long newValue) {
        trailPush(KIND_WIDE_KNOWN_FILLED_WORD, flatIndex, wideKnownFilled[flatIndex]);
        wideKnownFilled[flatIndex] = newValue;
    }

    private static void setKnownBlankWordWide(int flatIndex, long newValue) {
        trailPush(KIND_WIDE_KNOWN_BLANK_WORD, flatIndex, wideKnownBlank[flatIndex]);
        wideKnownBlank[flatIndex] = newValue;
    }

    private static void recordWideSizeChange(boolean isRow, int index, int oldSize) {
        trailPush(isRow ? KIND_WIDE_ROW_SIZE : KIND_WIDE_COL_SIZE, index, oldSize);
    }

    private static void commonOnesWide(long[] candidates, int size, int words, long[] out) {
        Arrays.fill(out, -1L);
        for (int i = 0; i < size; i++) {
            int base = i * words;
            for (int w = 0; w < words; w++) {
                out[w] &= candidates[base + w];
            }
        }
    }

    private static void commonZerosWide(long[] candidates, int size, int words, long finalWordMask, long[] out) {
        Arrays.fill(out, -1L);
        for (int i = 0; i < size; i++) {
            int base = i * words;
            for (int w = 0; w < words; w++) {
                out[w] &= ~candidates[base + w];
            }
        }
        out[words - 1] &= finalWordMask;
    }

    private static void extractColumnWordsWide(long[] rowMajor, int c, long[] out) {
        Arrays.fill(out, 0L);
        int cWord = c >>> 6, cBit = c & 63;
        for (int r = 0; r < rowAmount; r++) {
            long bit = (rowMajor[r * wideRowWords + cWord] >>> cBit) & 1L;
            if (bit != 0) {
                out[r >>> 6] |= (1L << (r & 63));
            }
        }
    }

    private static void undoToWide(int mark) {
        while (trailSize > mark) {
            trailSize--;
            int index = trailIndex[trailSize];
            long oldValue = trailValue[trailSize];
            switch (trailKind[trailSize]) {
                case KIND_WIDE_ROW_SIZE -> wideRowCandidateSize[index] = (int) oldValue;
                case KIND_WIDE_COL_SIZE -> wideColumnCandidateSize[index] = (int) oldValue;
                case KIND_WIDE_KNOWN_FILLED_WORD -> wideKnownFilled[index] = oldValue;
                case KIND_WIDE_KNOWN_BLANK_WORD -> wideKnownBlank[index] = oldValue;
            }
        }
    }

    private static String boardToStringWide() {
        StringBuilder sb = new StringBuilder(rowAmount * columnAmount);
        for (int r = 0; r < rowAmount; r++) {
            int base = r * wideRowWords;
            for (int c = 0; c < columnAmount; c++) {
                long bit = (wideKnownFilled[base + (c >>> 6)] >>> (c & 63)) & 1L;
                sb.append(bit != 0 ? '#' : '.');
            }
        }
        return sb.toString();
    }
}