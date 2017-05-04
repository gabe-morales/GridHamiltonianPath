/*
 * Author: Gabriel Morales
 * Course: CSC 258 - Parallel & Distributed Systems (SPRING 2017)
 */
package v4;

public class TTY implements Runnable {
    /* instance of GameBoard class that contains the grid and algorithm */
    protected GameBoard board;
    
    /**
     * Constructor that initializes a TTY version of the interface on a 2D
     * boolean array representing the board and starting coordinates x and y.
    **/
    public TTY(boolean[][] start, int y, int x, int workerCount) {
        board = new GameBoard(start, y, x, workerCount);
    }
    
    /**
     * run() (from Runnable)
     *
     * Description:
     *   Runs the algorithm and outputs its solution.
    **/
    @Override
    public void run() {
        long startTime = 0, endTime = 0;
        if (board.isSolvable()) {
            System.out.println("Calculating...");
            startTime = System.currentTimeMillis();
            board.call();
            endTime = System.currentTimeMillis();
            System.out.println("Duration: " + (endTime - startTime) + " ms");
            if (board.state == State.FINISHED) {
                System.out.print(
                    "Solution Found.\n\n" + board.pathToString()
                );
                return;
            }
        }
        
        System.out.println("No Solution Exists.");
    }
}