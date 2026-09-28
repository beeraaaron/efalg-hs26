package nonogram;

import org.openjdk.jmh.annotations.Benchmark;

import java.io.IOException;

public class NonogramBenchmark {
    @Benchmark
    public void measureX() throws IOException {
        var header = "33 30".trim().split("\\s+");
        var columns = Integer.parseInt(header[0]);
        var rows = Integer.parseInt(header[1]);
        assert columns < 63 && rows < 63;
    }
}
