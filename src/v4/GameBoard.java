/*
 * Author: Gabriel Morales
 * Course: CSC 258 - Parallel & Distributed Systems (SPRING 2017)
 */
package v4;

import mypackages.util.Board;
import mypackages.algorithms.StateSpaceSearch;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.swing.AbstractAction;

/**
 * GameBoard class that defines all the necessary parts for the path puzzle
 * board, as well as the entire algorithm for solving a puzzle
**/
public class GameBoard extends Board<Square, GameBoard.MoveAction>
implements StateSpaceSearch<Square[][], GameBoard.MoveAction, Boolean> {
    /* keeps track of the current path length */
    protected int pathSize;
    /* stores the number of workers available to complete the algorithm */
    protected int numWorkers;
    /* keeps track of the current state of the algorithm */
    protected State state;
    /* keeps track of the current location on the board */
    protected Point loc;
    /* stores the path (an ordered list of points) */
    protected List<Point> path;
    
    /**
     * Constructor that initializes the board based on the number of rows and
     * columns provided.
    **/
    public GameBoard(int rows, int columns, int workerCount) {
        if (rows < 1 || columns < 1) {
            System.err.println(
                "Invalid Board Dimensions.\n" +
                "\tBoard requires positive dimension."
            );
            throw new IllegalArgumentException();
        }
        
        initialize(rows, columns, workerCount);
        initialState();
        pathSize = 0;
    }
    
    /**
     * Constructor that initializes the board based on a 2D boolean array
     * representation of the board and starting coordinates.
    **/
    public GameBoard(boolean[][] initState, int y, int x, int workerCount) {
        if (initState == null) {
            System.err.println("Uninitialized Board.");
            throw new IllegalArgumentException();
        }
        
        if (initState[0] == null) {
            System.err.println(
                "Invalid Board Dimensions.\n\tBoard must be rectangular."
            );
            throw new IllegalArgumentException();
        }
        
        pathSize = initState[0].length;
        for (int i = 1; i < initState.length; i++)
            if (initState[i] == null || initState[i].length != pathSize) {
                System.err.println(
                    "Invalid Board Dimensions.\n\tBoard must be rectangular."
                );
                throw new IllegalArgumentException();
            }
        
        if (initState.length == 0 || pathSize == 0) {
            System.err.println(
                "Invalid Board Dimensions.\n" +
                "\tBoard requires positive dimension."
            );
            throw new IllegalArgumentException();
        }
            
        if (y < 0 || y >= initState.length || x < 0 || x >= pathSize) {
            System.err.println("Out-Of-Bounds Starting Point.");
            throw new IllegalArgumentException();
        }
        
        if (initState[y][x]) {
            System.err.println("Invalid Starting Point.");
            throw new IllegalArgumentException();
        }
        
        initialize(initState.length, initState[0].length, workerCount);
        for (int i = 0; i < bounds.height; i++)
            for (int j = 0; j < bounds.width; j++)
                if (initState[i][j]) {
                    grid[i][j] = Square.BLOCK;
                    pathSize--;
                } else {
                    grid[i][j] = Square.FREE;
                }
        
        path = path.subList(0, pathSize);
        loc.setLocation(x, y);
        pathSize = 0;
        path.get(pathSize++).setLocation(loc);
        grid[loc.y][loc.x] = Square.USED;
    }
    
    public GameBoard(GameBoard old) {
        initialize(old.bounds.height, old.bounds.width, old.numWorkers);
        for (int i = 0; i < bounds.height; i++)
            System.arraycopy(old.grid[i], 0, grid[i], 0, bounds.width);
        
        pathSize = old.pathSize;
        path = path.subList(0, old.path.size());
        for (int i = 0; i < pathSize; i++)
            path.get(i).setLocation(old.path.get(i));
        
        loc.setLocation(old.loc);
    }
    
    /**
     * initialize()
     *
     * Description:
     *   Initializes the grid, and various other variables.
     *
     * Inputs:
     *   rows - number of rows (height)
     *   columns - number of columns (width)
    **/
    private final void initialize(int rows, int columns, int workerCount) {
        pathSize = rows * columns;
        numWorkers = workerCount;
        state = State.STOPPED;
        bounds = new Dimension(columns, rows);
        loc = new Point(0, 0);
        
        grid = new Square[rows][columns];
        actions = new ArrayList<MoveAction>(4);
        path = new ArrayList<Point>(pathSize);
        
        actions.add(new MoveLeftAction());
        actions.add(new MoveRightAction());
        actions.add(new MoveUpAction());
        actions.add(new MoveDownAction());
        
        initPath();
    }
    
    /**
     * initialState()
     *
     * Description:
     *   Initializes the grid by setting every square to 'FREE'.
    **/
    public void initialState() {
        for (int i = 0; i < bounds.height; i++)
            for (int j = 0; j < bounds.width; j++)
                grid[i][j] = Square.FREE;
    }
    
    /**
     * initPath()
     *
     * Description:
     *   Initializes the path by setting every coordinate to (-1, -1).
    **/
    protected void initPath() {
        for (int i = 0; i < pathSize; i++)
            path.add(new Point(-1, -1));
    }
    
    /**
     * forward()
     *
     * Description:
     *   Moves forward on the grid by adding the current location to the path
     *   and marking the square as 'USED'.
    **/
    protected void forward() {
        path.get(pathSize++).setLocation(loc);
        setSquare(Square.USED);
    }
    
    /**
     * backward()
     *
     * Description:
     *   Moves backward on the grid by removing the current location from the
     *   path and marking the square as 'FREE'.
    **/
    protected void backward() {
        path.get(--pathSize).setLocation(-1, -1);
        setSquare(Square.FREE);
    }
    
    /**
     * setSquare()
     *
     * Description:
     *   Updates the state of the square at the current location.
     *
     * Inputs:
     *   square - the enum value to set for the current square
    **/
    protected void setSquare(Square square) {
        grid[loc.y][loc.x] = square;
    }
    
    /**
     * getSquare()
     *
     * Description:
     *   Returns the state of the square at the given location
     *
     * Inputs:
     *   point - the location to check
    **/
    protected Square getSquare(Point point) {
        return grid[point.y][point.x];
    }
    
    /**
     * getWidth()
     *
     * Description:
     *   Returns the bounds of the board.
    **/
    protected Dimension getBounds() {
        return bounds;
    }
    
    /**
     * getWidth()
     *
     * Description:
     *   Returns the width of the board (number of columns).
    **/
    protected int getWidth() {
        return bounds.width;
    }
    
    /**
     * getWidth()
     *
     * Description:
     *   Returns the height of the board (number of rows).
    **/
    protected int getHeight() {
        return bounds.height;
    }
    
    /**
     * pointToString()
     *
     * Description:
     *   Returns a string to represent the given point.
     *
     * Inputs:
     *   point - coordinate to form as text
     *
     * Return Value:
     *   String - text-formatted point
    **/
    public String pointToString(Point point) {
        return String.format("[%d, %d]", point.y, point.x);
    }
    
    /**
     * pathToString()
     *
     * Description:
     *   Returns a string to represent the current path.
     *
     * Return Value:
     *   String - text-formatted path
    **/
    public String pathToString() {
        String text = "";
        for (Point point : path)
            text += String.format("%s\n", pointToString(point));
        return text;
    }
    
    /**
     * isOrderOk()
     *
     * Description:
     *   Determines if the ordered list of directions is acceptable by checking
     *   if it contains any duplicates.
     *
     * Inputs:
     *   list - ordered list of directions
     *
     * Return Value:
     *   boolean - true if order is acceptable
     *           - false if order is unacceptable
    **/
    protected boolean isOrderOk(List<MoveAction> list) {
        return new HashSet<MoveAction>(list).size() == actions.size();
    }
    
    /**
     * setOrder()
     *
     * Description:
     *   Checks if ordered list is acceptable and updates the order for the
     *   algorithm.
     *
     * Inputs:
     *   list - ordered list of directions
     *
     * Return Value:
     *   boolean - true if order was updated
     *           - false if order was not updated
    **/
    protected boolean setOrder(List<MoveAction> list) {
        if (!isOrderOk(list))
            return false;
        int size = actions.size();
        for (int i = 0; i < size; i++)
            actions(grid).set(i, list.get(i));
        return true;
    }
    
    /**
     * gridToIntVals()
     *
     * Description:
     *   Converts the Square grid to a grid of integers in order to perform
     *   calculations.
     *
     * Return Value:
     *   int[][] - 2D array of integer values representing state of the grid
    **/
    protected int[][] gridToIntVals() {
        int[][] vals = new int[bounds.height][bounds.width];
        for (int i = 0; i < bounds.height; i++)
            for (int j = 0; j < bounds.width; j++)
                vals [i][j] = grid[i][j].STATUS;
        return vals;
    }
    
    /**
     * isSolvable()
     *
     * Description:
     *   Checks whether the current board is solvable.
     *
     * Return Value:
     *   boolean - true if puzzle is solvable
     *           - false if puzzle is unsolvable
    **/
    protected boolean isSolvable() {
        return isDominoTileable() && isGoodMove();
    }
    
    /**
     * call() (from Callable)
     *
     * Description:
     *   Starts the algorithm in search for a solution. Stops when solution is
     *   found or 'stop' event is issued.
    **/
    @Override
    public Boolean call() {
        state = State.RUNNING;
        
        if (submitTasks()) {
            state = State.FINISHED;
            return true;
        }
        
        state = State.STOPPED;
        return false;
    }
    
    // TODO: Comment
    public boolean submitTasks() {
        int size;
        ExecutorService executor = Executors.newFixedThreadPool(numWorkers);
        Queue<WorkerBoard> queue = new LinkedList<WorkerBoard>();
        Queue<Future<Boolean>> futures = new LinkedList<Future<Boolean>>();
        Future<Boolean> future;
        WorkerBoard board;
        
        queue.add(new WorkerBoard(this));
        while ((size = queue.size()) < numWorkers) {
            if (size == 0) {
                queue.add(new WorkerBoard(this));
                size = 1;
                break;
            }
            
            for (int i = 0; i < size; i++) {
                board = queue.poll();
                for (GameBoard.MoveAction action : board.actions(board.grid)) {
                    if (action.forwardCondition()) {
                        action.updatePosition();
                        board.forward();
                        queue.add(new WorkerBoard(board));
                        board.backward();
                        action.undoPosition();
                    }
                }
            }
        }
        
        for (WorkerBoard task : queue)
            futures.add(executor.submit(task));
        
        while ((size = futures.size()) > 0 && state != State.STOPPED) {
            for (int i = 0; i < size; i++) {
                future = futures.poll();
                board = queue.poll();
                if (future.isDone()) {
                    try {
                        if (future.get()) {
                            while (pathSize < board.pathSize) {
                                loc.setLocation(board.path.get(pathSize));
                                forward();
                            }
                            executor.shutdownNow();
                            executor.awaitTermination(60, TimeUnit.SECONDS);
                            return true;
                        }
                    } catch (Exception e) {
                        System.err.println("Future Error");
                    }
                } else {
                    futures.add(future);
                    queue.add(board);
                }
            }
        }
        try {
            executor.shutdownNow();
            executor.awaitTermination(60, TimeUnit.SECONDS);
        } catch (Exception e) {}
        return false;
    }
    
    /**
     * isGoal() (from StateSpaceSearch)
     *
     * Description:
     *   Determines whether the goal (a Hamiltonian Path from the starting
     *   location) has been achieved.
     *
     * Return Value:
     *   boolean - true if goal has been reahed
     *           - false if goal is not yet reached
    **/
    @Override
    public boolean isGoal() {
        return pathSize == path.size();
    }
    
    /**
     * actions() (from StateSpaceSearch)
     *
     * Description:
     *   Returns a list form of the possible actions to take.
     *
     * Inpputs:
     *   state - grid/current state of the board
     *
     * Return Value:
     *   List - list of possible actions
    **/
    @Override
    public List<MoveAction> actions(Square[][] state) {
        return (List<MoveAction>) actions;
    }
    
    /**
     * findSolution()
     *
     * Description:
     *   Recursive solving algorithm. Checks if the goal has been reached or if
     *   a 'stop' event was trigerred. Moves to all possible directions (if
     *   possible) in order.
     *
     * Return Value:
     *   boolean - true if solution was found
     *           - false if no solution was found or if process was stopped
    **/
    @Override
    public boolean findSolution() {
        if (isGoal())
            return true;
        
        if (state == State.STOPPED)
            return false;
        
        for (MoveAction action : actions(grid))
            if (action.forwardCondition()) {
                action.updatePosition();
                forward();
                if (isGoodMove() && findSolution())
                    return true;
                backward();
                action.undoPosition();
            }
        
        return false;
    }
    
    /**
     * isDominoTileable()
     *
     * Description:
     *   Determines if the board can be tiled by dominoes, which helps
     *   determine if a solution exists.
     *
     * Return Value:
     *   boolean - true if the board is domino tileable
     *           - false if the board is not domino tileable
    **/
    protected boolean isDominoTileable() {
        int white = 0;
        int black = 0;
        boolean flag = true;
        
        for (int i = 0; i < bounds.height; i++, flag = (i % 2) == 0)
            for (int j = 0; j < bounds.width; j++, flag = !flag)
                if (grid[i][j] != Square.FREE)
                    continue;
                else if (flag)
                    black++;
                else
                    white++;
        
        if ((loc.y + loc.x) % 2 == 0)
            black++;
        else
            white++;
        
        return Math.abs(white - black) < 2;
    }
    
    /**
     * isGoodMove()
     *
     * Description:
     *   Determines whether the algorithm should continue in this direction,
     *   which helps cut time.
     *
     * Return Value:
     *   boolean - true if move was good
     *           - false if move was bad
    **/
    protected boolean isGoodMove() {
        return blobCount() < 2 && singlePathCount() < 2;
    }
    
    /**
     * blobCount()
     *
     * Description:
     *   Union-Find algorithm to count number of blobs on the board.
     *
     * Return Value:
     *   int - number of blobs detected
    **/
    protected int blobCount() {
        int label = 1;
        int[][] vals = gridToIntVals();
        if (grid[0][0] == Square.FREE)
            vals[0][0] = ++label;
        for (int i = 1; i < bounds.height; i++)
            if (grid[i][0] == Square.FREE)
                if (grid[i-1][0] != Square.FREE)
                    vals[i][0] = ++label;
                else 
                    vals[i][0] = vals[i-1][0];
        for (int j = 1; j < bounds.width; j++)
            if (grid[0][j] == Square.FREE)
                if (grid[0][j-1] != Square.FREE)
                    vals[0][j] = ++label;
                else
                    vals[0][j] = vals[0][j-1];
        for (int i = 1; i < bounds.height; i++)
            for (int j = 1; j < bounds.width; j++)
                if (grid[i][j] == Square.FREE)
                    label = union(vals, i, j, label);
        return --label;
    }
    
    /**
     * union()
     *
     * Description:
     *   Marks blobs on the board and returns the number of blobs counted.
     *
     * Inputs:
     *   vals - integer version of grid
     *   i - y-coordinate
     *   j - x-coordinate
     *   label - counter that keeps track of the blob id and count
     *
     * Return Value:
     *   int - current blob count
    **/
    protected int union(int[][] vals, int i, int j, int label) {
        if (grid[i-1][j] != Square.FREE && grid[i][j-1] != Square.FREE) {
            vals[i][j] = ++label;
        } else if (grid[i][j-1] != Square.FREE) {
            vals[i][j] = vals[i-1][j];
        } else if (grid[i-1][j] != Square.FREE) {
            vals[i][j] = vals[i][j-1];
        } else {
            vals[i][j] = Math.min(vals[i-1][j], vals[i][j-1]);
            if (vals[i-1][j] != vals[i][j-1]) {
                --label;
                int max = Math.max(vals[i-1][j], vals[i][j-1]);
                for (int a = 0; a < i; a++)
                    for (int b = 0; b < bounds.width; b++)
                        if (vals[a][b] == max)
                            vals[a][b] = vals[i][j];
                for (int b = 0; b < j; b++)
                    if (vals[i][b] == max)
                        vals[i][b] = vals[i][j];
            }
        }
        return label;
    }
    
    /**
     * singlePathCount()
     *
     * Description:
     *   Keeps track of how many cells have only on way in, in which case the
     *   board cannot be solved at the current state.
     *
     * Return Value:
     *   int - number of squares with only one way in
    **/
    protected int singlePathCount() {
        int count = 0;
        for (int i = 0; i < bounds.height; i++)
            for (int j = 0; j < bounds.width; j++)
                if (grid[i][j] == Square.FREE && oneWayOut(i, j))
                    count++;
        return count;
    }
    
    /**
     * oneWayOut()
     *
     * Description:
     *   Determines if the square at (j, i) only has one way out/in.
     *
     * Inputs:
     *   i - y-coordinate
     *   j - x-coordinate
     *
     * Return Value:
     *   boolean - true if there is only one way out of the square
     *           - false if there is more than one way out
    **/
    protected boolean oneWayOut(int i, int j) {
        if (loc.y == i && (loc.x == j-1 || loc.x == j+1))
            return false;
        
        if (loc.x == j && (loc.y == i-1 || loc.y == i+1))
            return false;
        
        int surround = 0;
        if (j == 0 || grid[i][j-1] != Square.FREE)
            surround++;
        
        if (j == bounds.width-1 || grid[i][j+1] != Square.FREE)
            surround++;
        
        if (i == 0 || grid[i-1][j] != Square.FREE)
            surround++;
        
        if (i == bounds.height-1 || grid[i+1][j] != Square.FREE)
            surround++;
        
        return surround == 3;
    }
    
    // TODO: Comment
    protected static class WorkerBoard extends GameBoard {
        public WorkerBoard(GameBoard old) {super(old);}
        @Override
        public final Boolean call() {
            state = State.RUNNING;
            
            if (findSolution()) {
                state = State.FINISHED;
                return true;
            }
            
            if (state != State.STOPPED)
                state = State.STOPPED;
            
            return false;
        }
    }
    
    /**
     * MoveAction class defines the general procedure for moving
     * forward/backward on the grid
    **/
    protected abstract class MoveAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            if (state == State.STOPPED)
                if (forwardCondition()) {
                    updatePosition();
                    forward();
                } else if (pathSize > 1 && backwardCondition()) {
                    backward();
                    updatePosition();
                }
        }
        
        public void undoPosition() {loc.setLocation(path.get(pathSize-1));}
        public abstract void updatePosition();
        public abstract boolean forwardCondition();
        public abstract boolean backwardCondition();
    }
    
    /**
     * MoveLeftAction class allows moving to the left
    **/
    protected class MoveLeftAction extends MoveAction {
        public void updatePosition()
        {loc.x--;}
        public boolean forwardCondition()
        {return loc.x > 0 && grid[loc.y][loc.x-1] == Square.FREE;}
        public boolean backwardCondition()
        {return path.get(pathSize-2).x == loc.x-1;}
    }
    
    /**
     * MoveRightAction class allows moving to the right
    **/
    protected class MoveRightAction extends MoveAction {
        public void updatePosition()
        {loc.x++;}
        public boolean forwardCondition()
        {return loc.x < bounds.width-1 && grid[loc.y][loc.x+1] == Square.FREE;}
        public boolean backwardCondition()
        {return path.get(pathSize-2).x == loc.x+1;}
    }
    
    /**
     * MoveUpAction class allows moving up
    **/
    protected class MoveUpAction extends MoveAction {
        public void updatePosition()
        {loc.y--;}
        public boolean forwardCondition()
        {return loc.y > 0 && grid[loc.y-1][loc.x] == Square.FREE;}
        public boolean backwardCondition()
        {return path.get(pathSize-2).y == loc.y-1;}
    }
    
    /**
     * MoveDownAction class allows moving down
    **/
    protected class MoveDownAction extends MoveAction {
        public void updatePosition()
        {loc.y++;}
        public boolean forwardCondition() {
            return loc.y < bounds.height-1 &&
                grid[loc.y+1][loc.x] == Square.FREE;
        }
        public boolean backwardCondition()
        {return path.get(pathSize-2).y == loc.y+1;}
    }
    
    /**
     * MoveBackAction class allows moving backwards
    **/
    protected class MoveBackAction extends MoveAction {
        public void updatePosition()
        {undoPosition();}
        public boolean forwardCondition()
        {return false;}
        public boolean backwardCondition()
        {return true;}
    }
}