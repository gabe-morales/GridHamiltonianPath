/*
 * Author: Gabriel Morales
 * Course: CSC 258 - Parallel & Distributed Systems (SPRING 2017)
 */
package v4;

import java.awt.Color;

/**
 * Square enum class to define the states of each cell on the grid/board
 *   FREE - available cell
 *   USED - unavailable cell (part of path)
 *   BLOCK - unavailable cell (unusable)
**/
enum Square {
    FREE        (0, Color.BLUE),
    USED        (1, Color.RED),
    BLOCK       (1, Color.GRAY);
    
    protected final int STATUS;
    protected final Color COLOR;
    Square(int status, Color color) {
        this.STATUS = status;
        this.COLOR = color;
    }
}