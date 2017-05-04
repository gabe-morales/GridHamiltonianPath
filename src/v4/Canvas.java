/*
 * Author: Gabriel Morales
 * Course: CSC 258 - Parallel & Distributed Systems (SPRING 2017)
 */
package v4;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.function.Supplier;
import javax.swing.JPanel;

/**
 * Canvas class representing the canvas for the board to bee seen in the GUI
**/
public class Canvas extends JPanel implements Runnable {
    /* method object pointing to a method to alert when the solver has finished
     */
    protected Supplier<Void> inform;
    /* dimension object that stores the size of an individual cell on the grid
     */
    protected Dimension squareSize;
    /* instance of GameBoard class that contains the grid */
    protected GameBoard board;
    /* integer arrays that keep track of the coordinates of individual cells to
     * avoid recalculating them
     */
    protected int[] ystart, xstart;
    
    /**
     * Constructor that initializes the an empty GameBoard based on the
     * dimensions given.
    **/
    public Canvas(int rows, int columns, int workerCount) {
        board = new CanvasBoard(rows, columns, workerCount);
        initialize(board.getBounds());
    }
    
    /**
     * Constructor that initializes the GameBoard based on a 2D boolean array
     * version of the board defined in a separate Java class and starting
     * coordinates y and x.
    **/
    public Canvas(boolean[][] start, int y, int x, int workerCount) {
        board = new CanvasBoard(start, y, x, workerCount);
        initialize(board.getBounds());
    }
    
    /**
     * initialize()
     *
     * Description:
     *   Determines the proper sizes to give each cell on the grid (giving
     *   equal weight to each). Calculates the starting coordinates for each
     *   cell.
     *
     * Inputs:
     *   bounds - contians the height and width dimensions for the board
     *
    **/
    private void initialize(Dimension bounds) {
        ystart = new int[bounds.height];
        xstart = new int[bounds.width];
        squareSize = determineSquareSize(bounds.height);
        updateStarts();
    }
    
    /**
     * setInfrom()
     *
     * Description:
     *   Initializes 'inform' to point to a method to alert once the solver has
     *   finished calculating or has been paused.
     *
     * Inputs:
     *   fun - method pointer from an outside class that needs to be 'informed'
    **/
    protected void setInform(Supplier<Void> func) {
        inform = func;
    }
    
    /**
     * determineSquareSize()
     *
     * Description:
     *   Determines how many pixels to use for each cell, ensuring the minimum
     *   dimensions for the GameBoard are 60 x 60pxs.
     *
     * Inputs:
     *   rows - number of rows on the grid.
     *
     * Return Value:
     *   Dimension - contains the height and row values to use for each cell on
     *               the grid
    **/
    protected Dimension determineSquareSize(int rows) {
        int length = (rows > 5)? 10 : 60/rows;
        return new Dimension(length, length);
    }
    
    /**
     * determineGridSize()
     *
     * Description:
     *   Determines the total size of the GUI grid in pixels.
     *
     * Return Value:
     *   Dimension - stores the size of the grid
    **/
    protected Dimension determineGridSize() {
        Dimension gridSize = new Dimension(squareSize);
        gridSize.height *= board.getHeight();
        gridSize.width *= board.getWidth();
        return gridSize;
    }
    
    /**
     * updateSquareSize()
     *
     * Description:
     *   Change the size of individual squares/cells to the given dimensions.
     *
     * Inputs:
     *   size - new dimensions for each cell
    **/
    protected void updateSquareSize(Dimension size) {
        squareSize.setSize(size);
        squareSize.height /= board.getHeight();
        squareSize.width /= board.getWidth();
    }
    
    /**
     * setInfrom()
     *
     * Description:
     *   Updates the int arrays keeping track of coordinates when a resize
     *   event occurs.
    **/
    protected void updateStarts() {
        for (int i = 0; i < ystart.length; i++)
            ystart[i] = i*squareSize.height+1;
        for (int i = 0; i < xstart.length; i++)
            xstart[i] = i*squareSize.width+1;
    }
    
    /**
     * paintComponent()
     *
     * Description:
     *   Paints the canvas of the entire board when a request is sent. Updates
     *   the starting coordinates for the cells. Paints each individual cell
     *   with its current color based on that of its current Square state.
     *
     * Inputs:
     *   g - graphic component of this canvas
    **/
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (!getSize().equals(squareSize)) {
            updateSquareSize(getSize());
            updateStarts();
        }
        
        Point point = new Point();
        for (point.y = 0; point.y < board.getHeight(); point.y++)
            for (point.x = 0; point.x < board.getWidth(); point.x++)
                paintSquare(g, point);
    }
    
    /**
     * paintSquare()
     *
     * Description:
     *   Paints an individual square on the board's canvas.
     *
     * Inputs:
     *   g - graphic component of this canvas
     *   point - specific coordinate of square to paint
    **/
    protected void paintSquare(Graphics g, Point point) {
        Rectangle clip = new Rectangle();
        clip.setLocation(xstart[point.x], ystart[point.y]);
        clip.setSize(squareSize.width-2, squareSize.height-2);
        g.setClip(clip);
        g.setColor(board.getSquare(point).COLOR);
        g.fillRect(clip.x, clip.y, clip.width, clip.height);
    }
    
    /**
     * run() (from Runnable)
     *
     * Description:
     *   Instructs solver in GameBoard class to run and waits for it to finish
     *   to alert the 'inform' method pointer that the solving has
     *   finished/stopped.
    **/
    @Override
    public void run() {
        board.call();
        if (inform != null)
            inform.get();
    }
    
    /**
     * CanvasBoard class that extends GameBoard to shorten code replication and
     * enable the board to be displayed to the user.
    **/
    protected class CanvasBoard extends GameBoard {
        public CanvasBoard(int rows, int columns, int workerCount) {
            super(rows, columns, workerCount);
        }
        
        public CanvasBoard(boolean[][] start, int y, int x, int workerCount) {
            super(start, y, x, workerCount);
        }
        
        /**
         * setSquare() (from GameBoard)
         *
         * Description:
         *   adds Canvas/GUI updating to general method of GameBoard class.
        **/
        @Override
        protected void setSquare(Square square) {
            super.setSquare(square);
            paintSquare(getGraphics(), loc);
        }
    }
}