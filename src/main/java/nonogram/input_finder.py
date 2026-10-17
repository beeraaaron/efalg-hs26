#!/usr/bin/env python3
"""
Searches randomly generated nonogram.in files for ones that make your Java
solver run for a specific amount of time, with a specific number of
solutions -- useful for picking "calibration" puzzles of known difficulty.

Keeps generating, running, and measuring until it has found TARGET_COUNT
distinct nonogram.in files that satisfy both:
  - MIN_SECONDS <= runtime <= MAX_SECONDS
  - MIN_SOLUTIONS <= number of lines in nonogram-my.out <= MAX_SOLUTIONS

Adjust the CONFIG block below to match your setup before running.
"""
import random
import subprocess
import time
import os
import shutil

# ============================== CONFIG ==================================
# How you'd normally invoke your program. Adjust this to match your setup,
# e.g. ["java", "-cp", "out", "nonogram.NonogramFast3"] if you compile to
# .class files instead of running the .java source directly.
JAVA_CMD = ["java", "-ea", "NonogramSolver.java"]

# Directory your program runs in -- must be wherever it expects to find
# "nonogram.in" and will write "nonogram-my.out" (both hardcoded relative
# paths in the program itself, so this script writes/reads them there).
WORKDIR = "."
IN_FILE = os.path.join(WORKDIR, "nonogram-test.in")
OUT_FILE = os.path.join(WORKDIR, "nonogram-test.out")

MIN_SECONDS, MAX_SECONDS = 30, 120
MIN_SOLUTIONS, MAX_SOLUTIONS = 1, 50
TARGET_COUNT = 2          # how many distinct matching files to find
MAX_ATTEMPTS = 200        # give up after this many tries
TIMEOUT_BUFFER = 30       # kill a run this many seconds past MAX_SECONDS

OUTPUT_DIR = "found_puzzles"
# ==========================================================================


def generate_grid(rows, cols, seed, num_shapes, max_shape):
    """Random grid built from small filled rectangles -- keeps the resulting
    clues' candidate counts from exploding (see the puzzle-generation
    discussion earlier): few, larger blocks solve far more predictably than
    many tiny ones."""
    rng = random.Random(seed)
    grid = [[False] * cols for _ in range(rows)]
    for _ in range(num_shapes):
        w = rng.randint(1, max_shape)
        h = rng.randint(1, max_shape)
        x = rng.randint(0, cols - w)
        y = rng.randint(0, rows - h)
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                grid[yy][xx] = True
    return grid


def line_clue(cells):
    clue, run = [], 0
    for v in cells:
        if v:
            run += 1
        else:
            if run:
                clue.append(run)
                run = 0
    if run:
        clue.append(run)
    return clue


def write_nonogram_in(path, rows, cols, grid):
    row_clues = [line_clue(grid[r]) for r in range(rows)]
    col_clues = [line_clue([grid[r][c] for r in range(rows)]) for c in range(cols)]
    lines = [f"{cols} {rows}"]
    lines += [" ".join(map(str, c)) for c in row_clues]
    lines += [" ".join(map(str, c)) for c in col_clues]
    with open(path, "w") as f:
        f.write("\n".join(lines) + "\n")


class DifficultyPicker:
    """Tracks a single scalar "difficulty" and nudges it up when runs finish
    too fast, down when they're too slow/time out -- so the search tends to
    converge toward the target window instead of guessing blindly forever."""

    def __init__(self):
        self.level = 3.0  # arbitrary starting point, tuned by feedback below

    def params(self, rng):
        jitter = rng.uniform(0.85, 1.15)
        level = max(0.5, self.level * jitter)
        size = int(min(100, max(15, round(20 + level * 9))))
        divisor = int(min(150, max(15, round(140 - level * 12))))
        max_shape = rng.choice([2, 3, 4, 5, 6])
        num_shapes = max(3, (size * size) // divisor)
        seed = rng.randint(0, 10_000_000)
        return size, size, seed, num_shapes, max_shape

    def feedback(self, elapsed, timed_out):
        if timed_out or (elapsed is not None and elapsed > MAX_SECONDS):
            self.level *= 0.7
        elif elapsed is not None and elapsed < MIN_SECONDS:
            self.level *= 1.3


def run_once(timeout_s):
    start = time.monotonic()
    try:
        result = subprocess.run(
            JAVA_CMD, cwd=WORKDIR, timeout=timeout_s,
            capture_output=True, text=True,
        )
    except subprocess.TimeoutExpired:
        return None, None, "timeout"
    elapsed = time.monotonic() - start
    if result.returncode != 0:
        return elapsed, None, f"exit={result.returncode}: {result.stderr[-500:]}"
    if not os.path.exists(OUT_FILE):
        return elapsed, None, "no output file produced"
    with open(OUT_FILE) as f:
        num_solutions = sum(1 for _ in f)
    return elapsed, num_solutions, None


def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    rng = random.Random()
    picker = DifficultyPicker()
    found = []
    seen = set()

    for attempt in range(1, MAX_ATTEMPTS + 1):
        if len(found) >= TARGET_COUNT:
            break

        rows, cols, seed, num_shapes, max_shape = picker.params(rng)
        grid = generate_grid(rows, cols, seed, num_shapes, max_shape)
        write_nonogram_in(IN_FILE, rows, cols, grid)

        print(f"[{attempt}/{MAX_ATTEMPTS}] {rows}x{cols} shapes={num_shapes} "
              f"max_shape={max_shape} seed={seed} (difficulty={picker.level:.2f}) ... ",
              end="", flush=True)

        elapsed, num_solutions, err = run_once(MAX_SECONDS + TIMEOUT_BUFFER)

        if err == "timeout":
            dest_in = os.path.join(OUTPUT_DIR, f"nonogram-timeout{attempt}.in")
            shutil.copy(IN_FILE, dest_in)
            print(f"TIMEOUT (>{MAX_SECONDS + TIMEOUT_BUFFER}s)")
            picker.feedback(None, timed_out=True)
            continue
        if err:
            dest_in = os.path.join(OUTPUT_DIR, f"nonogram-error{attempt}.in")
            shutil.copy(IN_FILE, dest_in)
            print(f"ERROR: {err}")
            continue

        print(f"{elapsed:.1f}s, {num_solutions} solution(s)", end="")
        picker.feedback(elapsed, timed_out=False)

        if not (MIN_SECONDS <= elapsed <= MAX_SECONDS):
            print(" -> outside time window")
            continue
        if not (MIN_SOLUTIONS <= num_solutions <= MAX_SOLUTIONS):
            print(" -> outside solution-count window")
            continue

        with open(IN_FILE) as f:
            content = f.read()
        if content in seen:
            print(" -> duplicate of an already-found file, skipping")
            continue
        seen.add(content)

        idx = len(found) + 1
        dest_in = os.path.join(OUTPUT_DIR, f"nonogram-match{idx}.in")
        dest_out = os.path.join(OUTPUT_DIR, f"nonogram-match{idx}.out")
        shutil.copy(IN_FILE, dest_in)
        shutil.copy(OUT_FILE, dest_out)
        found.append((dest_in, elapsed, num_solutions))
        print(f" -> MATCH! saved as {dest_in}")

    print()
    if len(found) < TARGET_COUNT:
        print(f"Only found {len(found)}/{TARGET_COUNT} matching files after {MAX_ATTEMPTS} attempts.")
        print("Try raising MAX_ATTEMPTS, widening the time/solution windows, or")
        print("adjusting DifficultyPicker's starting level / mapping formula.")
    else:
        print(f"Found {TARGET_COUNT} matching files:")
        for path, elapsed, nsol in found:
            print(f"  {path}: {elapsed:.1f}s, {nsol} solution(s)")


if __name__ == "__main__":
    main()