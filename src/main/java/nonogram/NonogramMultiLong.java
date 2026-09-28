package nonogram;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NonogramMultiLong {
    private static final String inputFilename = "nonogram.in";
    private static final String outputFilename = "nonogram-my.out";
    private static final BufferedReader reader;

    private static int columnAmount;
    private static int rowAmount;

    private static int[][] columnClues;
    private static int[][] rowClues;

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

        // A single `long` can only address 64 bit positions (and the naive
        // `(1L << n) - 1` mask trick only works cleanly up to n == 64), so the
        // fast single-word path is only correct while both dimensions fit.
        // Everything above that needs the multi-word ("wide") path.
        if (columnAmount <= 64 && rowAmount <= 64) {
            initializeCandidatesFast();
            solveRecFast();
        } else {
            initializeCandidatesWide();
            solveRecWide();
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

    private static void writeSolutionsToFile() throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFilename))) {
            for (String solution : solutions) {
                writer.println(solution);
            }
        }
    }

    // ====================================================================
    // shared trail: both paths push/undo through the SAME arrays -- the
    // trail itself (kind/index/oldValue) has nothing bit-width-specific
    // about it, so there's no benefit to duplicating this part. Only one
    // path's KIND_* constants are ever pushed in a given run, since `main`
    // picks exactly one path and never mixes them.
    // ====================================================================
    private static final int KIND_FAST_ROW_SIZE = 0, KIND_FAST_COL_SIZE = 1,
            KIND_FAST_ROW_FILLED = 2, KIND_FAST_ROW_BLANK = 3,
            KIND_WIDE_ROW_SIZE = 4, KIND_WIDE_COL_SIZE = 5,
            KIND_WIDE_KNOWN_FILLED_WORD = 6, KIND_WIDE_KNOWN_BLANK_WORD = 7;
    private static int[] trailKind = new int[64];
    private static int[] trailIndex = new int[64];
    private static long[] trailValue = new long[64];
    private static int trailSize = 0;

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

    private static void undoTo(int mark) {
        while (trailSize > mark) {
            trailSize--;
            int index = trailIndex[trailSize];
            long oldValue = trailValue[trailSize];
            switch (trailKind[trailSize]) {
                case KIND_FAST_ROW_SIZE -> fastRowCandidateSize[index] = (int) oldValue;
                case KIND_FAST_COL_SIZE -> fastColumnCandidateSize[index] = (int) oldValue;
                case KIND_FAST_ROW_FILLED -> fastKnownFilled[index] = oldValue;
                case KIND_FAST_ROW_BLANK -> fastKnownBlank[index] = oldValue;
                case KIND_WIDE_ROW_SIZE -> wideRowCandidateSize[index] = (int) oldValue;
                case KIND_WIDE_COL_SIZE -> wideColumnCandidateSize[index] = (int) oldValue;
                case KIND_WIDE_KNOWN_FILLED_WORD -> wideKnownFilled[index] = oldValue;
                case KIND_WIDE_KNOWN_BLANK_WORD -> wideKnownBlank[index] = oldValue;
            }
        }
    }

    // ====================================================================
    // FAST PATH -- one `long` per line. Used when columnAmount <= 64 and
    // rowAmount <= 64. Structurally and performance-wise identical to the
    // single-word trail version from before the wide-path work.
    // ====================================================================
    private static long fastColumnsMask, fastRowsMask;
    private static long[] fastKnownFilled, fastKnownBlank;
    private static long[][] fastRowCandidates, fastColumnCandidates;
    private static int[] fastRowCandidateSize, fastColumnCandidateSize;

    private static void initializeCandidatesFast() {
        fastColumnsMask = (columnAmount == 64) ? -1L : (1L << columnAmount) - 1;
        fastRowsMask = (rowAmount == 64) ? -1L : (1L << rowAmount) - 1;

        fastKnownFilled = new long[rowAmount];
        fastKnownBlank = new long[rowAmount];
        fastRowCandidates = new long[rowAmount][];
        fastRowCandidateSize = new int[rowAmount];
        fastColumnCandidates = new long[columnAmount][];
        fastColumnCandidateSize = new int[columnAmount];

        for (int r = 0; r < rowAmount; r++) {
            long[] arr = toArrayFast(generateCandidatesFast(rowClues[r], columnAmount));
            fastRowCandidates[r] = arr;
            fastRowCandidateSize[r] = arr.length;
        }
        for (int c = 0; c < columnAmount; c++) {
            long[] arr = toArrayFast(generateCandidatesFast(columnClues[c], rowAmount));
            fastColumnCandidates[c] = arr;
            fastColumnCandidateSize[c] = arr.length;
        }
    }

    private static void solveRecFast() {
        int mark = trailMark();
        if (!propagateAllFast()) {
            undoTo(mark);
            return;
        }

        boolean bestLineIsRow = true;
        int bestSize = Integer.MAX_VALUE;
        int bestIndex = -1;
        for (int r = 0; r < rowAmount; r++) {
            int size = fastRowCandidateSize[r];
            if (size > 1 && size < bestSize) { bestSize = size; bestIndex = r; }
        }
        for (int c = 0; c < columnAmount; c++) {
            int size = fastColumnCandidateSize[c];
            if (size > 1 && size < bestSize) { bestLineIsRow = false; bestSize = size; bestIndex = c; }
        }

        if (bestIndex == -1) {
            assert validNonogramFast() : "propagation produced an invalid board (fast path)";
            solutions.add(boardToStringFast());
            undoTo(mark);
            return;
        }

        int candidateSize = bestLineIsRow ? fastRowCandidateSize[bestIndex] : fastColumnCandidateSize[bestIndex];
        for (int i = 0; i < candidateSize; i++) {
            int branchMark = trailMark();
            forceCandidateFast(bestLineIsRow, bestIndex, i);
            solveRecFast();
            undoTo(branchMark);
        }
        undoTo(mark);
    }

    private static boolean propagateAllFast() {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int r = 0; r < rowAmount; r++) {
                if (!filterLineFast(true, r, fastKnownFilled[r], fastKnownBlank[r], fastColumnsMask)) return false;

                long[] candidates = fastRowCandidates[r];
                int size = fastRowCandidateSize[r];
                long forcedOnes = commonOnesFast(candidates, size, fastColumnsMask) & ~fastKnownFilled[r];
                long forcedZeros = commonZerosFast(candidates, size, fastColumnsMask) & ~fastKnownBlank[r];
                if (forcedOnes != 0) { setKnownFilledFast(r, fastKnownFilled[r] | forcedOnes); changed = true; }
                if (forcedZeros != 0) { setKnownBlankFast(r, fastKnownBlank[r] | forcedZeros); changed = true; }
            }
            for (int c = 0; c < columnAmount; c++) {
                long colFilled = extractColumnBitsFast(fastKnownFilled, c);
                long colBlank = extractColumnBitsFast(fastKnownBlank, c);
                if (!filterLineFast(false, c, colFilled, colBlank, fastRowsMask)) return false;

                long[] candidates = fastColumnCandidates[c];
                int size = fastColumnCandidateSize[c];
                long forcedOnes = commonOnesFast(candidates, size, fastRowsMask) & ~colFilled;
                long forcedZeros = commonZerosFast(candidates, size, fastRowsMask) & ~colBlank;
                if (forcedOnes != 0 || forcedZeros != 0) {
                    for (int r = 0; r < rowAmount; r++) {
                        if (((forcedOnes >> r) & 1) != 0) setKnownFilledFast(r, fastKnownFilled[r] | (1L << c));
                        if (((forcedZeros >> r) & 1) != 0) setKnownBlankFast(r, fastKnownBlank[r] | (1L << c));
                    }
                    changed = true;
                }
            }
        }
        return true;
    }

    private static boolean filterLineFast(boolean isRow, int index, long knownFilled, long knownBlank, long mask) {
        long[] candidates = isRow ? fastRowCandidates[index] : fastColumnCandidates[index];
        int size = isRow ? fastRowCandidateSize[index] : fastColumnCandidateSize[index];
        int oldSize = size;

        int i = 0;
        while (i < size) {
            long cand = candidates[i];
            if ((cand & knownBlank) != 0 || (~cand & knownFilled & mask) != 0) {
                size--;
                long tmp = candidates[i]; candidates[i] = candidates[size]; candidates[size] = tmp;
            } else {
                i++;
            }
        }

        if (size != oldSize) {
            recordFastSizeChange(isRow, index, oldSize);
            if (isRow) fastRowCandidateSize[index] = size; else fastColumnCandidateSize[index] = size;
        }
        return size > 0;
    }

    private static void forceCandidateFast(boolean isRow, int lineIndex, int candidateIndex) {
        long[] candidates = isRow ? fastRowCandidates[lineIndex] : fastColumnCandidates[lineIndex];
        int candidateSize = isRow ? fastRowCandidateSize[lineIndex] : fastColumnCandidateSize[lineIndex];

        if (candidateIndex != 0) {
            long tmp = candidates[0]; candidates[0] = candidates[candidateIndex]; candidates[candidateIndex] = tmp;
        }
        recordFastSizeChange(isRow, lineIndex, candidateSize);
        if (isRow) fastRowCandidateSize[lineIndex] = 1; else fastColumnCandidateSize[lineIndex] = 1;
    }

    private static List<Long> generateCandidatesFast(int[] clues, int length) {
        List<Long> result = new ArrayList<>();
        generateCandidatesRecFast(clues, 0, length, 0, 0L, result);
        return result;
    }

    private static void generateCandidatesRecFast(int[] clues, int clueIndex, int length, int start,
                                                  long acc, List<Long> result) {
        if (clueIndex == clues.length) { result.add(acc); return; }

        int clue = clues[clueIndex];
        int fieldsToFill = clue;
        for (int i = clueIndex + 1; i < clues.length; i++) fieldsToFill += clues[i];
        fieldsToFill += (clues.length - clueIndex - 1);

        for (int i = start; i + fieldsToFill <= length; i++) {
            long block = (clue == 64) ? -1L : (((1L << clue) - 1) << i);
            generateCandidatesRecFast(clues, clueIndex + 1, length, i + clue + 1, acc | block, result);
        }
    }

    private static long[] toArrayFast(List<Long> list) {
        long[] arr = new long[list.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = list.get(i);
        return arr;
    }

    private static long extractColumnBitsFast(long[] rows, int c) {
        long bits = 0;
        for (int r = 0; r < rowAmount; r++) if (((rows[r] >> c) & 1L) != 0) bits |= (1L << r);
        return bits;
    }

    private static long commonOnesFast(long[] arr, int size, long mask) {
        long common = mask;
        for (int i = 0; i < size; i++) common &= arr[i];
        return common;
    }

    private static long commonZerosFast(long[] arr, int size, long mask) {
        long common = mask;
        for (int i = 0; i < size; i++) common &= (~arr[i] & mask);
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

    private static boolean lineMatchesCluesFast(long lineBits, int length, int[] clues) {
        int clueIndex = 0, runLength = 0;
        for (int i = 0; i < length; i++) {
            boolean filled = ((lineBits >> i) & 1L) != 0;
            if (filled) {
                runLength++;
            } else if (runLength > 0) {
                if (clueIndex >= clues.length || clues[clueIndex] != runLength) return false;
                clueIndex++;
                runLength = 0;
            }
        }
        if (runLength > 0) {
            if (clueIndex >= clues.length || clues[clueIndex] != runLength) return false;
            clueIndex++;
        }
        return clueIndex == clues.length;
    }

    private static boolean validNonogramFast() {
        for (int r = 0; r < rowAmount; r++)
            if (!lineMatchesCluesFast(fastKnownFilled[r], columnAmount, rowClues[r])) return false;
        for (int c = 0; c < columnAmount; c++)
            if (!lineMatchesCluesFast(extractColumnBitsFast(fastKnownFilled, c), rowAmount, columnClues[c])) return false;
        return true;
    }

    private static String boardToStringFast() {
        StringBuilder sb = new StringBuilder(rowAmount * columnAmount);
        for (int r = 0; r < rowAmount; r++)
            for (int c = 0; c < columnAmount; c++)
                sb.append(((fastKnownFilled[r] >> c) & 1L) != 0 ? '#' : '.');
        return sb.toString();
    }

    // ====================================================================
    // WIDE PATH -- an array of `long` words per line. Used whenever either
    // dimension exceeds what a single `long` can address.
    // ====================================================================
    private static int wideRowWords, wideColWords;
    private static long wideRowFinalWordMask, wideColFinalWordMask;
    private static long[] wideKnownFilled, wideKnownBlank;
    private static long[][] wideRowCandidates, wideColumnCandidates;
    private static int[] wideRowCandidateSize, wideColumnCandidateSize;
    private static long[] wideColFilledScratch, wideColBlankScratch;
    private static long[] wideForcedOnesScratchRow, wideForcedZerosScratchRow;
    private static long[] wideForcedOnesScratchCol, wideForcedZerosScratchCol;

    private static void initializeCandidatesWide() {
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
            List<long[]> list = generateCandidatesWide(rowClues[r], columnAmount, wideRowWords);
            wideRowCandidates[r] = toFlatArrayWide(list, wideRowWords);
            wideRowCandidateSize[r] = list.size();
        }
        for (int c = 0; c < columnAmount; c++) {
            List<long[]> list = generateCandidatesWide(columnClues[c], rowAmount, wideColWords);
            wideColumnCandidates[c] = toFlatArrayWide(list, wideColWords);
            wideColumnCandidateSize[c] = list.size();
        }

        wideColFilledScratch = new long[wideColWords];
        wideColBlankScratch = new long[wideColWords];
        wideForcedOnesScratchRow = new long[wideRowWords];
        wideForcedZerosScratchRow = new long[wideRowWords];
        wideForcedOnesScratchCol = new long[wideColWords];
        wideForcedZerosScratchCol = new long[wideColWords];
    }

    private static void solveRecWide() {
        int mark = trailMark();
        if (!propagateAllWide()) {
            undoTo(mark);
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
            assert validNonogramWide() : "propagation produced an invalid board (wide path)";
            solutions.add(boardToStringWide());
            undoTo(mark);
            return;
        }

        int candidateSize = bestLineIsRow ? wideRowCandidateSize[bestIndex] : wideColumnCandidateSize[bestIndex];
        for (int i = 0; i < candidateSize; i++) {
            int branchMark = trailMark();
            forceCandidateWide(bestLineIsRow, bestIndex, i);
            solveRecWide();
            undoTo(branchMark);
        }
        undoTo(mark);
    }

    private static boolean propagateAllWide() {
        boolean changed = true;
        while (changed) {
            changed = false;

            for (int r = 0; r < rowAmount; r++) {
                int base = r * wideRowWords;
                if (!filterLineWide(true, r, wideKnownFilled, base, wideKnownBlank, base, wideRowWords)) return false;

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
                if (!filterLineWide(false, c, wideColFilledScratch, 0, wideColBlankScratch, 0, wideColWords)) return false;

                long[] candidates = wideColumnCandidates[c];
                int size = wideColumnCandidateSize[c];
                commonOnesWide(candidates, size, wideColWords, wideForcedOnesScratchCol);
                commonZerosWide(candidates, size, wideColWords, wideColFinalWordMask, wideForcedZerosScratchCol);

                boolean anyForced = false;
                for (int w = 0; w < wideColWords; w++) {
                    wideForcedOnesScratchCol[w] &= ~wideColFilledScratch[w];
                    wideForcedZerosScratchCol[w] &= ~wideColBlankScratch[w];
                    if (wideForcedOnesScratchCol[w] != 0 || wideForcedZerosScratchCol[w] != 0) anyForced = true;
                }

                if (anyForced) {
                    int cWord = c >>> 6, cBit = c & 63;
                    for (int r = 0; r < rowAmount; r++) {
                        boolean setOne = ((wideForcedOnesScratchCol[r >>> 6] >>> (r & 63)) & 1L) != 0;
                        boolean setZero = ((wideForcedZerosScratchCol[r >>> 6] >>> (r & 63)) & 1L) != 0;
                        if (!setOne && !setZero) continue;

                        int flatIdx = r * wideRowWords + cWord;
                        if (setOne) setKnownFilledWordWide(flatIdx, wideKnownFilled[flatIdx] | (1L << cBit));
                        if (setZero) setKnownBlankWordWide(flatIdx, wideKnownBlank[flatIdx] | (1L << cBit));
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
        int size = isRow ? wideRowCandidateSize[index] : wideColumnCandidateSize[index];
        int oldSize = size;

        int i = 0;
        while (i < size) {
            int base = i * words;
            boolean invalid = false;
            for (int w = 0; w < words; w++) {
                long cand = candidates[base + w];
                long kf = knownFilled[knownFilledBase + w];
                long kb = knownBlank[knownBlankBase + w];
                if ((cand & kb) != 0 || (~cand & kf) != 0) { invalid = true; break; }
            }
            if (invalid) {
                size--;
                swapBlocksWide(candidates, i, size, words);
            } else {
                i++;
            }
        }

        if (size != oldSize) {
            recordWideSizeChange(isRow, index, oldSize);
            if (isRow) wideRowCandidateSize[index] = size; else wideColumnCandidateSize[index] = size;
        }
        return size > 0;
    }

    private static void forceCandidateWide(boolean isRow, int lineIndex, int candidateIndex) {
        long[] candidates = isRow ? wideRowCandidates[lineIndex] : wideColumnCandidates[lineIndex];
        int candidateSize = isRow ? wideRowCandidateSize[lineIndex] : wideColumnCandidateSize[lineIndex];
        int words = isRow ? wideRowWords : wideColWords;

        if (candidateIndex != 0) {
            swapBlocksWide(candidates, 0, candidateIndex, words);
        }
        recordWideSizeChange(isRow, lineIndex, candidateSize);
        if (isRow) wideRowCandidateSize[lineIndex] = 1; else wideColumnCandidateSize[lineIndex] = 1;
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

    private static List<long[]> generateCandidatesWide(int[] clues, int length, int words) {
        List<long[]> result = new ArrayList<>();
        long[] acc = new long[words];
        generateCandidatesRecWide(clues, 0, length, 0, acc, result);
        return result;
    }

    private static void generateCandidatesRecWide(int[] clues, int clueIndex, int length, int start,
                                                  long[] acc, List<long[]> result) {
        if (clueIndex == clues.length) {
            result.add(acc.clone());
            return;
        }

        int clue = clues[clueIndex];
        int fieldsToFill = clue;
        for (int i = clueIndex + 1; i < clues.length; i++) fieldsToFill += clues[i];
        fieldsToFill += (clues.length - clueIndex - 1);

        for (int i = start; i + fieldsToFill <= length; i++) {
            setBitsWide(acc, i, clue);
            generateCandidatesRecWide(clues, clueIndex + 1, length, i + clue + 1, acc, result);
            clearBitsWide(acc, i, clue);
        }
    }

    private static void setBitsWide(long[] words, int start, int len) {
        int end = start + len;
        int w0 = start >>> 6, w1 = (end - 1) >>> 6;
        if (w0 == w1) {
            long m = (len == 64) ? -1L : (((1L << len) - 1) << (start & 63));
            words[w0] |= m;
        } else {
            words[w0] |= (-1L << (start & 63));
            for (int w = w0 + 1; w < w1; w++) words[w] = -1L;
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
            for (int w = w0 + 1; w < w1; w++) words[w] = 0L;
            int lastBits = end - w1 * 64;
            long m = (lastBits == 64) ? -1L : ((1L << lastBits) - 1);
            words[w1] &= ~m;
        }
    }

    private static long[] toFlatArrayWide(List<long[]> list, int words) {
        long[] flat = new long[list.size() * words];
        for (int i = 0; i < list.size(); i++) {
            System.arraycopy(list.get(i), 0, flat, i * words, words);
        }
        return flat;
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
            for (int w = 0; w < words; w++) out[w] &= candidates[base + w];
        }
    }

    private static void commonZerosWide(long[] candidates, int size, int words, long finalWordMask, long[] out) {
        Arrays.fill(out, -1L);
        for (int i = 0; i < size; i++) {
            int base = i * words;
            for (int w = 0; w < words; w++) out[w] &= ~candidates[base + w];
        }
        out[words - 1] &= finalWordMask;
    }

    private static void extractColumnWordsWide(long[] rowMajor, int c, long[] out) {
        Arrays.fill(out, 0L);
        int cWord = c >>> 6, cBit = c & 63;
        for (int r = 0; r < rowAmount; r++) {
            long bit = (rowMajor[r * wideRowWords + cWord] >>> cBit) & 1L;
            if (bit != 0) out[r >>> 6] |= (1L << (r & 63));
        }
    }

    private static boolean lineMatchesCluesWide(long[] words, int base, int length, int[] clues) {
        int clueIndex = 0;
        int i = 0;
        while (i < length) {
            long bit = (words[base + (i >>> 6)] >>> (i & 63)) & 1L;
            if (bit == 0) { i++; continue; }
            int runStart = i;
            while (i < length) {
                long b = (words[base + (i >>> 6)] >>> (i & 63)) & 1L;
                if (b == 0) break;
                i++;
            }
            int runLength = i - runStart;
            if (clueIndex >= clues.length || clues[clueIndex] != runLength) return false;
            clueIndex++;
        }
        return clueIndex == clues.length;
    }

    private static boolean validNonogramWide() {
        for (int r = 0; r < rowAmount; r++)
            if (!lineMatchesCluesWide(wideKnownFilled, r * wideRowWords, columnAmount, rowClues[r])) return false;
        for (int c = 0; c < columnAmount; c++) {
            extractColumnWordsWide(wideKnownFilled, c, wideColFilledScratch);
            if (!lineMatchesCluesWide(wideColFilledScratch, 0, rowAmount, columnClues[c])) return false;
        }
        return true;
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