import java.util.*;
import java.io.*;

public class Grid {

    //functions and terminals
    public final static int ZERO = 0;
    public final static int ONE = 1; 
    public final static int TWO = 2; 
    public final static int UR = 3;  
    public final static int MR = 4;
    public final static int LR = 5;  
    public final static int UM = 6;  
    public final static int LM = 7;  
    public final static int UL = 8;   
    public final static int ML = 9;   
    public final static int LL = 10;  
    public final static int INC = 11;  
    public final static int DEC = 12;  
    public final static int ADD = 13;  
    public final static int SUB = 14;  
    public final static int MAX = 15;  
    public final static int MIN = 16;  
    public final static int ITE = 17;


    // grid private vars
    private char[][] grid;
    private int xdim, ydim;
    private Random rgen;
    private int numBoxes = 6;
    private int dozerX, dozerY;
    public int dozerFacing;
    char[] dirs = new char[] {'e','n','w','s'};

    // if no seed given, use -1 for cur time
    public Grid(int xdim, int ydim, int numBoxes) {
	this(xdim, ydim, numBoxes, -1);
    }
    
    // create a Grid of characters with the given dimensions and number of boxes
    public Grid(int xdim, int ydim, int numBoxes, int seed) {
        this.xdim = xdim;
        this.ydim = ydim;
        this.numBoxes = numBoxes;

	// grid is just 2d array of chars, initially fill with spaces
	grid = new char[xdim][ydim];
        for (int i=0; i<xdim; i++)
            for (int j=0; j<ydim; j++)
                grid[i][j] = ' ';
	
	// create rand generator (if seed is -1, use time instead)
	if (seed == -1) rgen = new Random();
	else rgen = new Random(seed);

        initGrid();
    }

    // place boxes and bulldozer on the grid, may not create 2x2 square of boxes
    // as bulldozer could not move any of them no matter how good the strategy
    private void initGrid() {

        int toPlace = numBoxes;
        int remLocs = (xdim-2)*(ydim-2);
        int x=1, y=1;
        while (toPlace > 0) {
	    
            // the probability that this square should get a block is
	    // (blocks still to place) / (squares not yet considered)
	    // Note that this probability will grow to 1 when there are only as
	    // many squares left as there are blocks to place.
            double p = (double)toPlace/remLocs;
            if (rgen.nextDouble() < p) {
		// only place if won't create a 2x2 square of blocks
		// if p is 1, place block even if creates square, so as to avoid infinitie loop
                if (grid[x-1][y]!='b' || grid[x][y-1]!='b' || grid[x-1][y-1]!='b' || p>=.99) {
                    grid[x][y] = 'b';
		    toPlace--;
		}
            }
            remLocs--;
	    
            // at end of each row, move to beginning of next row
            if (++x == xdim-1) {
                x=1;
                y++;
            }
        }
	
        // place dozer in random start location
        x = rgen.nextInt(xdim-2) + 1;
        y = rgen.nextInt(ydim-2) + 1;

        // if chosen dozer location already has a box, just search 2x2 space around
        // it because know can't have 2x2 squares all with boxes
        if (grid[x][y] == 'b') {
            if (grid[x+1][y] != 'b') x++;
            else if (grid[x][y+1] != 'b') y++;
            else {
                x++; 
                y++;
            }
        }

	// if 2x2 square is all blocks because of one weird case, oh well, put dozer
	// somewhere at least, even if shares location with block
        grid[x][y] = 'D';
        dozerX = x;
        dozerY = y;

        // set dozer to face random direction
        dozerFacing = rgen.nextInt(4);
    }


    // turn dozer left
    public void left() {
	left(null);
    }

    // turn dozer left and update output file
    public void left(BufferedWriter out) {
        dozerFacing = (dozerFacing + 1) % 4;
        if (out != null) updateFile(out, dozerX, dozerY, dozerFacing);
    }

    // turn dozer right
    public void right() {
	right(null);
    }

    // turn dozer right and update output file
    public void right(BufferedWriter out) {
        if (--dozerFacing < 0) dozerFacing = 3;
        if (out != null) updateFile(out, dozerX, dozerY, dozerFacing);
    }
    
   // if no out file specified, use null
    public void forward() {
	forward(null);
    }


    // if dozer is facing wall or 2 consecutive squares with blocks, nothing happens
    // otherwise move dozer forward 1 and if block in front move it too
    public void forward(BufferedWriter out) {
        // get coordinates of space in front of dozer and space 2 in front
        int frontX = dozerX, frontY = dozerY;
        int forw2X = dozerX, forw2Y = dozerY;
        if (dozerFacing==0) {
            frontX++;
            forw2X += 2;
        }
        else if (dozerFacing==1) {
            frontY--;
            forw2Y -= 2;
        }
        else if (dozerFacing==2) {
            frontX--;
            forw2X -= 2;
        }
        else {
            frontY++;
            forw2Y += 2;
        }
	
        // if facing wall, do nothing
        if (frontX<0 || frontX >= xdim || frontY<0 || frontY>=ydim) {
            //record that step spent not moving
	    if (out != null) updateFile(out, dozerX, dozerY, dozerFacing);
            return;
        }

        // if facing block 
        if (grid[frontX][frontY] == 'b') {
            // if has wall or another block behind it, do nothing
            if (forw2X<0 || forw2X>=xdim || forw2Y<0 || forw2Y>=ydim || grid[forw2X][forw2Y]=='b') {
                //record that step spent not moving
		if (out != null) updateFile(out, dozerX, dozerY, dozerFacing);
                return;
            }
	    
            // if clear behind block, move block
	    grid[forw2X][forw2Y] = 'b';

	    // record move
	    if (out != null) {
		try{
		    out.write(forw2X + " " + forw2Y + " " + "b\n");
		} catch (IOException e) {
		    System.out.println("Error while writing to file in forward method");
		}
	    }
 
        }
        // if here were either facing block with nothing behind it or empty space so will move dozer
	// record move
	if (out != null) {
	    try{
		out.write(dozerX + " " + dozerY + " " + " \n");
	    } catch (IOException e) {
		System.out.println("Error while writing to file in forward method");
	    }
	}
	
	// dozer moves
        grid[frontX][frontY] = 'D';
        grid[dozerX][dozerY] = ' ';
        dozerX = frontX;
        dozerY = frontY;

	if (out!= null) updateFile(out, dozerX, dozerY, dozerFacing);
	

    }

    // frontOffset is 1 for square in front of dozer, 0 for inline with dozer, and -1 for behind
    // sideOffset is 1 for left of dozer, 0 inline, -1 right
    // y goes from 0 at the top to max val at the bottom of grid/screen
    // returns 0,1, or 2 for empty, box, wall respectively 
    public int sensor(int frontOffset, int sideOffset) {
        // determine the appropriate square to check if is empty, wall, or box
        int checkX=-1, checkY=-1;
        if (dirs[dozerFacing] == 'w') {
            checkX = dozerX - frontOffset;
            checkY = dozerY + sideOffset;
        }
        else if (dirs[dozerFacing] == 'e') {
            checkX = dozerX + frontOffset;
            checkY = dozerY - sideOffset;
        }
        else if (dirs[dozerFacing] == 'n') {
            checkX = dozerX - sideOffset;
            checkY = dozerY - frontOffset;
        }
        else if (dirs[dozerFacing] == 's') {
            checkX = dozerX + sideOffset;
            checkY = dozerY + frontOffset;
        }

        // if box to check is out of bounds, return 2 for wall
        if (checkX < 0 || checkY < 0 || checkX >= xdim || checkY >= ydim) return 2;
        
        // otherwise 0 for empty and 1 for box
        if (grid[checkX][checkY] == 'b') return 1;
        else return 0;
    }

    // determine the fitness of the current state of the grid. fitness is (maxScore+1) - score
    // where score is the number of sides of blocks that are touching a wall
    public int calcFitness() {
        int fit = 0;
        int maxFit = 0;
        if (numBoxes>=4) maxFit = numBoxes+4;
        else maxFit = numBoxes*2;
        
        // increase fitness if find boxes in first or last col
        int i=0;
        for (int j=0; j<ydim; j++) 
            if (grid[i][j] == 'b') fit++;
        i=xdim-1;
        for (int j=0; j<ydim; j++) 
            if (grid[i][j] == 'b') fit++;

        // increase fitness for boxes in first or last row, note that 
        // boxes in the corner will have fitness increased twice which 
        // correctly gives them their bonus of 2 instead of 1.
        i=0;
        for (int j=0; j<xdim; j++) 
            if (grid[j][i] == 'b') fit++;
        i=ydim-1;
        for (int j=0; j<xdim; j++) 
            if (grid[j][i] == 'b') fit++;

        return maxFit + 1 - fit;
    }

    public void print() {
        print(System.out);
    }

    // print the current state of the grid, showing blocks and the dozer 
    // pointing in the correct direction
    public void print(PrintStream os) {
        for (int y=0; y<ydim; y++) {
            os.print("|");
            for (int x=0; x<xdim; x++) {
                char out = grid[x][y];
                if (out=='D') {
                    if (dozerFacing==0) out = '>';
                    else if (dozerFacing==1) out = '^';
                    else if (dozerFacing==2) out = '<';
                    else if (dozerFacing==3) out = 'v';
                }
                os.print(out+"|");
            }
            os.println();
        }
        os.println();
    }

    private void updateFile(BufferedWriter out, int x, int y, int dir) {
        try{
            out.write(x + " " + y + " ");
            if (dir==0) out.write('>');
            else if (dir==1) out.write('^');
            else if (dir==2) out.write('<');
            else if (dir==3) out.write('v');
            out.write("\n*******\n");
        } catch (IOException e) {
            System.out.println("Error while writing to file in update method");
        }
    }

    public void initSimulationFile(BufferedWriter out){
        try{
            out.write("New Grid\n");
        } catch (IOException e) {
            System.out.println("Error while writing to file in init method");
        }
        for (int y=0; y<ydim; y++) {
            for (int x=0; x<xdim; x++) {
                char c = grid[x][y];
                if (c=='b') {
                    try{
                        out.write(x + " " + y + " b");
                        out.newLine();
                    } catch (IOException e) {
                        System.out.println("Error while writing to file in init method");
                    }
                }
            }
        }
        updateFile(out, dozerX, dozerY, dozerFacing);
    }

    public void outputFitness(BufferedWriter out, int gridFitness) {
        try{
            out.write("Fitness = " + gridFitness + "\n");
        } catch (IOException e) {
            System.out.println("Error while writing to file in fitness method");
        }
    }
    
    //********** START MODIFIED CODE **********//
    
    // returns the average of all values in an array of ints
    private static double getArrayAverage(int[] array) {
        double sum = 0.0;
        for(int i = 0; i < array.length; i++) {
            sum += array[i];
        }
        return sum / array.length;
    }
    
    private static void printResults(double[][] toPrint) {
        for(int i = 0; i < 4; i++) {
            for(int j = 0; j < 2; j++) {
                System.out.print(toPrint[j][i] + "  ");
            }
            System.out.println();
        }
    }
    
    private enum Move {
        FORWARD, RIGHT, LEFT;
    }
    
    public static void main(String[] args) {
        int xDim = 6;
        int yDim = 6;
        int numBoxes = 5;
        int numBoards = 1000;
        int totalIterations = 160;
        int fitnessCheck = 80;
        double[][] results = new double[2][];
        for(int i = 0; i < 2; i++) {
            results[i] = new double[4];
        }
        int[] fitnesses80 = new int[numBoards];
        int[] fitnesses160 = new int[numBoards];
        Random r = new Random();
        
        //********** STRATEGY 1 **********//
        for(int i = 0; i < numBoards; i++) {
            Grid curBoard = new Grid(xDim, yDim, numBoxes);
            for(int j = 0; j < totalIterations; j++) {
                
                int dir = r.nextInt(3);
                if(dir == Move.FORWARD.ordinal()) curBoard.forward();
                else if(dir == Move.RIGHT.ordinal()) curBoard.right();
                else curBoard.left();
                
                if(j == fitnessCheck - 1) {
                    fitnesses80[i] = curBoard.calcFitness();
                }
            }
            fitnesses160[i] = curBoard.calcFitness();
        }
        results[0][0] = getArrayAverage(fitnesses80);
        results[1][0] = getArrayAverage(fitnesses160);
        
        //********** STRATEGY 2 **********//
        for(int i = 0; i < numBoards; i++) {
            Grid curBoard = new Grid(xDim, yDim, numBoxes);
            for(int j = 0; j < totalIterations; j++) {
                
                double rand = r.nextDouble();
                if(rand <= 0.6) curBoard.forward();
                else if(rand <= 0.8) curBoard.left();
                else curBoard.right();
                
                if(j == fitnessCheck - 1) {
                    fitnesses80[i] = curBoard.calcFitness();
                }
            }
            fitnesses160[i] = curBoard.calcFitness();
        }
        results[0][1] = getArrayAverage(fitnesses80);
        results[1][1] = getArrayAverage(fitnesses160);
        
        //********** STRATEGY 3 **********//
        for(int i = 0; i < numBoards; i++) {
            Move prevMove = Move.FORWARD;
            Grid curBoard = new Grid(xDim, yDim, numBoxes);
            for(int j = 0; j < totalIterations; j++) {
                
                if(prevMove == Move.FORWARD) {
                    int rand = r.nextInt(3);
                    if(rand == Move.FORWARD.ordinal()) {
                        curBoard.forward();
                        prevMove = Move.FORWARD;
                    }
                    else if(rand == Move.RIGHT.ordinal()) {
                        curBoard.right();
                        prevMove = Move.RIGHT;
                    }
                    else {
                        curBoard.left();
                        prevMove = Move.LEFT;
                    }
                } else if(prevMove == Move.RIGHT) {
                    boolean forward = r.nextBoolean();
                    if(forward) {
                        curBoard.forward();
                        prevMove = Move.FORWARD;
                    } else {
                        curBoard.left();
                        prevMove = Move.LEFT;
                    }
                } else {
                    boolean forward = r.nextBoolean();
                    if(forward) {
                        curBoard.forward();
                        prevMove = Move.FORWARD;
                    } else {
                        curBoard.right();
                        prevMove = Move.RIGHT;
                    }
                }
                
                if(j == fitnessCheck - 1) {
                    fitnesses80[i] = curBoard.calcFitness();
                }
            }
            fitnesses160[i] = curBoard.calcFitness();
        }
        results[0][2] = getArrayAverage(fitnesses80);
        results[1][2] = getArrayAverage(fitnesses160);
        
        //********** STRATEGY 4 (ours) **********//
        for(int i = 0; i < numBoards; i++) {
            Grid curBoard = new Grid(xDim, yDim, numBoxes);
            Move prevMove = Move.FORWARD;
            for(int j = 0; j < totalIterations; j++) {
                
                if(j < 5) curBoard.forward();
                else if(j == 5) curBoard.left();
                else if(j < 11) curBoard.forward();
                
                else if(j < 13) curBoard.left();
                else if(j == 13) curBoard.forward();
                else if(j == 14) curBoard.right();
                
                else if (j < 19) curBoard.forward();
                else if(j == 19) curBoard.left();
                else if(j < 23) curBoard.forward();
                else if(j == 23) curBoard.left();
                else if(j < 27) curBoard.forward();
                else if(j == 27) curBoard.left();
                else if(j < 31) curBoard.forward();
                
                else if(j < 33) curBoard.left();
                else if(j == 33) curBoard.forward();
                else if(j == 34) curBoard.right();
                
                else if(j < 38) curBoard.forward();
                else if(j == 38) curBoard.right();
                else if(j == 39) curBoard.forward();
                else if(j == 40) curBoard.right();
                else if(j == 42) curBoard.forward();
                else if(j == 43) curBoard.right();
                
                else if(j < 46) curBoard.forward();
                else if(j == 46) curBoard.right();
                else if(j == 47) curBoard.forward();
                else if(j == 48) curBoard.right();
                else if(j == 49) curBoard.forward();
                else if(j == 50) curBoard.right();
                
                else if(j < 54) curBoard.forward();
                else if(j == 54) curBoard.right();
                else if(j == 55) curBoard.forward();
                else if(j == 56) curBoard.right();
                else if(j == 57) curBoard.forward();
                else if(j == 58) curBoard.right();
                
                else if(j < 62) curBoard.forward();
                
                
                if(j < 62 + 5) curBoard.forward();
                else if(j == 62 + 5) curBoard.left();
                else if(j < 62 + 11) curBoard.forward();
                
                else if(j < 62 + 13) curBoard.left();
                else if(j == 62 + 13) curBoard.forward();
                else if(j == 62 + 14) curBoard.right();
                
                else if (j < 62 + 19) curBoard.forward();
                else if(j == 62 + 19) curBoard.left();
                else if(j < 62 + 23) curBoard.forward();
                else if(j == 62 + 23) curBoard.left();
                else if(j < 62 + 27) curBoard.forward();
                else if(j == 62 + 27) curBoard.left();
                else if(j < 62 + 31) curBoard.forward();
                
                else if(j < 62 + 33) curBoard.left();
                else if(j == 62 + 33) curBoard.forward();
                else if(j == 62 + 34) curBoard.right();
                
                else if(j < 62 + 38) curBoard.forward();
                else if(j == 62 + 38) curBoard.right();
                else if(j == 62 + 39) curBoard.forward();
                else if(j == 62 + 40) curBoard.right();
                else if(j == 62 + 42) curBoard.forward();
                else if(j == 62 + 43) curBoard.right();
                
                else if(j < 62 + 46) curBoard.forward();
                else if(j == 62 + 46) curBoard.right();
                else if(j == 62 + 47) curBoard.forward();
                else if(j == 62 + 48) curBoard.right();
                else if(j == 62 + 49) curBoard.forward();
                else if(j == 62 + 50) curBoard.right();
                
                else if(j < 62 + 54) curBoard.forward();
                else if(j == 62 + 54) curBoard.right();
                else if(j == 62 + 55) curBoard.forward();
                else if(j == 62 + 56) curBoard.right();
                else if(j == 62 + 57) curBoard.forward();
                else if(j == 62 + 58) curBoard.right();
                
                else if(j < 62 + 62) curBoard.forward();
                
                else {
                    if(prevMove == Move.LEFT) curBoard.forward();
                    double rand = r.nextDouble();
                    if(rand <= 0.65) {
                        curBoard.forward();
                        prevMove = Move.FORWARD;
                    }
                    else {
                        curBoard.left();
                        prevMove = Move.LEFT;
                    }
                }
                
                if(j == fitnessCheck - 1) {
                    fitnesses80[i] = curBoard.calcFitness();
                }
            }
            fitnesses160[i] = curBoard.calcFitness();
        }
        results[0][3] = getArrayAverage(fitnesses80);
        results[1][3] = getArrayAverage(fitnesses160);
        
        printResults(results);
    }
}
