package disco;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;

public class DiscoSlow
{
    static final BufferedReader inp;private static int inpPos=0;private static String inpLine;
    static{try{inp=new BufferedReader(new FileReader("disco.in"));inpLine=inp.readLine();}catch(IOException _e){throw new RuntimeException(_e);}}
    static String next() throws Exception{int nextPos=inpLine.indexOf(' ', inpPos+1);String token=inpLine.substring(inpPos,nextPos==-1?inpLine.length():nextPos);if(nextPos==-1)inpLine=inp.readLine();inpPos=nextPos+1;return token;}

	//TODO: BigInteger used
	static long minSteps = 0;

    public static void main(String[] _args) throws Exception
    {
	PrintWriter out = new PrintWriter("disco.out");

	var w = Integer.parseInt(next());
	var h = Integer.parseInt(next());
	var lit = new boolean[w][h];

	for (var y = 0; y < h; y++)
	{
	    char[] line = next().toCharArray();
	    for (var x = 0; x < w; x++)
		lit[x][y] = line[x] == '1';
	}

	//TODO: BigInteger
	countSteps(lit, new boolean[w][h], w, h, 0L);

	//TODO: new Object BigInteger
	if (minSteps == -1)
	{
	    out.println("IMPOSSIBLE");
	    System.out.println("IMPOSSIBLE");
	}
	else
	{
	    out.println(minSteps);
	    System.out.println(minSteps);
	}

	out.close();
    }

    private static void countSteps(boolean[][] _field, boolean[][] _steppedOn, int _w, int _h, long _steps)
    {
	// check if all tiles are lit
	var allLights = true;
	for (var y = 0; y < _h; y++)
	    for (var x = 0; x < _w; x++)
		if (!_field[x][y])
			//TODO: loop could be terminated much faster
			allLights = false;

	if (allLights)
	{
	    System.out.println("Found solution with " + _steps + " steps");
		//TODO: new BigInt
		if ((minSteps == -1) || (_steps < minSteps))
		minSteps = _steps;
	    return;
	}

	//for (var y = 0; y < _h; y++)
	//{
	//    for (var x = 0; x < _w; x++)
	//	System.out.print(_field[x][y] ? "#" : ".");
	//    System.out.println();
	//}

	// pick next step
	for (var y = 0; y < _h; y++)
	    for (var x = 0; x < _w; x++)
		if (!_steppedOn[x][y])
		{
		    // make copies of the current state
			//TODO: Copies!!
			var steppedOnCopy = new boolean[_w][_h];
		    var fieldCopy = new boolean[_w][_h];
		    for (var cy = 0; cy < _h; cy++)
			for (var cx = 0; cx < _w; cx++)
			{
			    steppedOnCopy[cx][cy] = _steppedOn[cx][cy];
			    fieldCopy[cx][cy] = _field[cx][cy];
			}

		    // step on field (x,y)
		    //System.out.println("Step on " + x + "," + y);
		    _steppedOn[x][y] = true;
		    _field[x][y] = !_field[x][y];
		    if (x > 0)
			_field[x - 1][y] = !_field[x - 1][y];
		    if (y > 0)
			_field[x][y - 1] = !_field[x][y - 1];
		    if (x + 1 < _w)
			_field[x + 1][y] = !_field[x + 1][y];
		    if (y + 1 < _h)
			_field[x][y + 1] = !_field[x][y + 1];

		    // continue the search, perform more steps
		    countSteps(_field, _steppedOn, _w, _h, _steps+1);

		    // restore from copy made above
		    for (var cy = 0; cy < _h; cy++)
			for (var cx = 0; cx < _w; cx++)
			{
			    _steppedOn[cx][cy] = steppedOnCopy[cx][cy];
			    _field[cx][cy] = fieldCopy[cx][cy];
			}
		}
    }
}