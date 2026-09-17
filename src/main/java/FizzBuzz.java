//code by aaron.beer@students.fhnw.ch
import java.io.*;

public class FizzBuzz
{
    static final BufferedReader inpBr;
    private static int inpPos = 0;
    private static String inpLine;

    static{
        try{
            inpBr = new BufferedReader(new FileReader("fizzbuzz.in"));
            inpLine = inpBr.readLine();
        }
        catch(IOException _e) {
            throw new RuntimeException(_e);
        }
    }

    static String next() throws Exception{
        var nextPos = inpLine.indexOf(' ', inpPos + 1);
        var token = inpLine.substring(inpPos, nextPos == -1 ? inpLine.length() : nextPos);
        if (nextPos == -1) inpLine = inpBr.readLine();
        inpPos = nextPos + 1;
        return token;
    }

    static void main(String[] args) throws Exception
    {
        try(inpBr; var out = new PrintWriter("fizzbuzz.out")) {
            var s = Integer.parseInt(next());
            var e = Integer.parseInt(next());

            while (s <= e) {
                if (s % 3 == 0 && s % 5 == 0) {
                    out.println("FizzBuzz");
                } else if (s % 3 == 0) {
                    out.println("Fizz");
                } else if (s % 5 == 0) {
                    out.println("Buzz");
                } else {
                    out.println(s);
                }
                s++;
            }
        }
    }
}

