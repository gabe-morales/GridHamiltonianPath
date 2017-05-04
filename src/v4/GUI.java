/*
 * Author: Gabriel Morales
 * Course: CSC 258 - Parallel & Distributed Systems (SPRING 2017)
 */
package v4;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.Timer;

/**
 * GUI class that defines and creates all the graphical components for this
 * program
**/
public class GUI extends JFrame implements Runnable {
    /**
     * Direction enum class that represents all the directions available from a
     * given square on the grid
    **/
    protected enum Direction {
        LEFT, RIGHT, UP, DOWN;
        protected GameBoard.MoveAction action;
    }
    
    /* instance of GameBoard class that contains the grid and algorithm */
    protected GameBoard board;
    /* instance of the board as a canvas to add as a component */
    protected Canvas canvas;
    /* buttons to display the path and change the directional order used by the
     * algorithm
     */
    protected JButton replayButton, setOrderButton;
    /* boxes used to set the directional order used by the algorithm */
    protected JComboBox<Direction>[] boxes;
    /* text area displaying messages about the current status of the program to
     * the user
     */
    protected JTextArea textArea;
    /* timer used for displaying found path */
    protected Timer timer;
    
    /**
     * Constructor that initializes canvas and board based on number of rows
     * and columns.
    **/
    public GUI(int rows, int columns, int workerCount) {
        super("Puzzle");
        canvas = new Canvas(rows, columns, workerCount);
        initialize();
    }
    
    /**
     * Constructor that initializes canvas and board based on a 2D boolean
     * array representing the board and starting coordinates x and y.
    **/
    public GUI(boolean[][] start, int y, int x, int workerCount) {
        super("Puzzle");
        canvas = new Canvas(start, y, x, workerCount);
        initialize();
    }
    
    /**
     * initialize()
     *
     * Description:
     *   Initializes all the components for the GUI, placing the replay button
     *   ontop, the board on the left, the informative text area on the right,
     *   and the directional order boxes and button at the bottom of the
     *   JFrame.
    **/
    @SuppressWarnings("unchecked")
    protected void initialize() {
        board = canvas.board;
        
        canvas.setInform(this::checkDone);
        canvas.setPreferredSize(canvas.determineGridSize());
        canvas.setBackground(Color.BLACK);
        canvas.requestFocus();
        
        replayButton = new JButton("Replay");
        replayButton.setEnabled(false);
        
        setOrderButton = new JButton("Set");
        setOrderButton.setEnabled(false);
        
        textArea = new JTextArea();
        textArea.setBackground(Color.BLACK);
        textArea.setForeground(Color.LIGHT_GRAY);
        textArea.setFont(new Font(Font.SERIF, Font.BOLD, 12));
        textArea.setLineWrap(true);
        textArea.setEditable(false);
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(265, 90));
        
        boxes = (JComboBox<Direction>[]) new JComboBox[
            Direction.values().length
        ];
        for (int i = 0; i < boxes.length; i++) {
            boxes[i] = new JComboBox<Direction>(Direction.values());
            boxes[i].setSelectedIndex(i);
        }
        
        JPanel south = new JPanel();
        south.setBackground(new Color(178, 102, 255));
        south.add(new JLabel("Order of moves:"));
        for (JComboBox box : boxes)
            south.add(box);
        south.add(setOrderButton);
        
        GridBagLayout gridbag = new GridBagLayout(); 
        GridBagConstraints c = new GridBagConstraints();
        
        c.fill = GridBagConstraints.BOTH;
        c.weightx = canvas.getPreferredSize().width;
        c.weighty = 1.0;
        gridbag.setConstraints(canvas, c);
        
        c.weightx = scrollPane.getPreferredSize().width;
        gridbag.setConstraints(scrollPane, c);
        
        JPanel center = new JPanel(gridbag);
        center.add(canvas);
        center.add(scrollPane);
        
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setFocusable(true);
        add(replayButton, BorderLayout.NORTH);
        add(south, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);
        pack();
        setMinimumSize(getSize());
        setVisible(true);
    }
    
    /**
     * run() (from Runnable)
     *
     * Description:
     *   Determines whether board needs to be initialized or if it was defined
     *   in a separate file and choses what step to go to next.
    **/
    @Override
    public void run() {
        if (board.pathSize < 1)
            blockSelection();
        else
            begin();
    }
    
    /**
     * blockSelection()
     *
     * Description:
     *   Informs the user to select any amount of squares to set as 'BLOCK'
     *   (unusable) and continues to next step to select a staring location.
    **/
    protected void blockSelection() {
        textArea.append(
            "Select all the squares that are blocked.\n" +
            "Press 'ENTER' when done.\n"
        );
        MouseListener mouse = new MouseHandler() {
            public void backtrack(){}
            public void whenFree() {
                if (board.pathSize < board.path.size()-1) {
                    board.setSquare(Square.BLOCK);
                    board.pathSize++;
                }
                else {
                    textArea.setText(
                        "At least one square must be free for a starting" +
                        "point.\n"
                    );
                }
            }
            
            public void whenNotFree() {
                board.setSquare(Square.FREE);
                board.pathSize--;
            }
        };
        canvas.addMouseListener(mouse);
        
        canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke("ENTER"), "continue"
        );
        canvas.getActionMap().put("continue", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                int size = board.path.size() - board.pathSize;
                canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                    KeyStroke.getKeyStroke("ENTER"), "none"
                );
                board.path = board.path.subList(0, size);
                board.pathSize = 0;
                startSelection();
            }
        });
    }
    
    /**
     * startSelection()
     *
     * Description:
     *   Informs the user to select a starting point on the board. Only one
     *   location can be selected and the user may not continue if they do not
     *   supply one correctly.
    **/
    protected void startSelection() {
        textArea.append(
            "Select the starting location.\n Press 'ENTER' when done.\n"
        );
        
        removeMouseListeners(canvas);
        MouseListener mouse = new MouseHandler() {
            public void backtrack() {
                if (board.pathSize > 0)
                    board.backward();
            }
            
            public void whenFree(){board.forward();}
            public void whenNotFree(){}
        };
        canvas.addMouseListener(mouse);
        
        canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke("ENTER"), "continue"
        );
        canvas.getActionMap().put("continue", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (board.pathSize == 0) {
                    textArea.append("You need to select a starting point.\n");
                    return;
                }
                
                removeMouseListeners(canvas);
                canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).remove(
                    KeyStroke.getKeyStroke("ENTER")
                );
                canvas.getActionMap().remove("continue");
                begin();
            }
        });
    }
    
    /**
     * begin()
     *
     * Description:
     *   Begins the game by enabling clickable components and mouse events to
     *   control the current path being used.
    **/
    protected void begin() {
        for (GameBoard.MoveAction action : board.actions(null)) {
            if (action instanceof GameBoard.MoveLeftAction)
                Direction.LEFT.action = action;
            else if (action instanceof GameBoard.MoveRightAction)
                Direction.RIGHT.action = action;
            else if (action instanceof GameBoard.MoveUpAction)
                Direction.UP.action = action;
            else if (action instanceof GameBoard.MoveDownAction)
                Direction.DOWN.action = action;
        }
        
        InputMap input = canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        input.put(KeyStroke.getKeyStroke("LEFT"), "left");
        input.put(KeyStroke.getKeyStroke("RIGHT"), "right");
        input.put(KeyStroke.getKeyStroke("UP"), "up");
        input.put(KeyStroke.getKeyStroke("DOWN"), "down");
        input.put(KeyStroke.getKeyStroke("BACK_SPACE"), "back");
        input.put(KeyStroke.getKeyStroke("ENTER"), "pause");
        input.put(KeyStroke.getKeyStroke("ESCAPE"), "exit");
        
        ActionMap actions = canvas.getActionMap();
        actions.put("left", Direction.LEFT.action);
        actions.put("right", Direction.RIGHT.action);
        actions.put("up", Direction.UP.action);
        actions.put("down", Direction.DOWN.action);
        actions.put("back", board.new MoveBackAction());
        actions.put("pause", new GamePauseAction());
        actions.put("exit", new GameEndAction());
        
        textArea.setText("You may now move around.\n");
        if (board.isSolvable()) {
            textArea.append("Press 'ENTER' when ready to begin.\n");
            setOrderButton.setEnabled(true);
            MouseListener mouse = new OtherMouseListener();
            replayButton.addMouseListener(mouse);
            setOrderButton.addMouseListener(mouse);
            timer = new Timer(250, new TimeHandler());
        } else {
            textArea.append("No Solution Exists.\n");
            input.put(KeyStroke.getKeyStroke("ENTER"), "none");
            actions.put("pause", null);
            for (JComboBox box : boxes)
                box.setEnabled(false);
        }
    }
    
    /**
     * removeMouseListeners()
     *
     * Description:
     *   Removes all MouseListeners from the given component.
     *
     * Inputs:
     *   component - component from which to remove the MouseListeners
    **/
    protected void removeMouseListeners(JComponent component) {
        for (MouseListener element : component.getMouseListeners())
            component.removeMouseListener(element);
    }
    
    /**
     * getSelectedItem()
     *
     * Decription:
     *   Returns the item selected in the given JComboBox box.
     *
     * Inputs:
     *   box - JComboBox being considered
     *
     * Return Value:
     *   Direction - direction selected in box
    **/
    protected Direction getSelectedItem(JComboBox<Direction> box) {
        return box.getItemAt(box.getSelectedIndex());
    }
    
    /**
     * setOrder()
     *
     * Description:
     *   Gets the values set for each JComboBox and sets the order used by the
     *   algorithm if order given is acceptable.
     *
     * Return Value:
     *   boolean - true if order change was succesful
     *           - false if order change was uunsuccessful
    **/
    protected boolean setOrder() {
        List<GameBoard.MoveAction> list =
            new ArrayList<GameBoard.MoveAction>();
        for (JComboBox<Direction> box : boxes)
            list.add(getSelectedItem(box).action);
        return board.setOrder(list);
    }
    
    /**
     * checkDone()
     *
     * Description:
     *   Method alerted when algorithm finishes running or was paused. Decides
     *   how to inform the user based on what state the game is in.
    **/
    protected Void checkDone() {
        if (board.state == State.FINISHED) {
            textArea.append(
                "Solution Found.\n\n" + board.pathToString() + "\n" +
                "Press ESC to close the program.\n"
            );
            replayButton.setEnabled(true);
            canvas.resetKeyboardActions();
            canvas.getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "exit");
            canvas.getActionMap().put("exit", new GameEndAction());
        } else {
            textArea.append("No Solution Found.\n");
            for (JComboBox box : boxes)
                box.setEnabled(true);
            setOrderButton.setEnabled(true);
        }
        return null;
    }
    
    /**
     * MouseReleasedListener class that implements MouseListener by not doing
     * anything when the mouse is clicked or pressed, nor when it has entered
     * or exited the component
    **/
    protected abstract class MouseReleasedListener implements MouseListener {
        public void mouseClicked(MouseEvent e){}
        public void mouseEntered(MouseEvent e){}
        public void mouseExited(MouseEvent e){}
        public void mousePressed(MouseEvent e){}
    }
    
    /**
     * MouseHandler class that implements the mouseReleased() method from
     * MouseReleasedListener
     * Calculates which square on the grid was clicked and whether the square
     * selected is selectable
    **/
    protected abstract class MouseHandler extends MouseReleasedListener {
        public void mouseReleased(MouseEvent e) {
            int i = e.getY() / canvas.squareSize.height;
            int j = e.getX() / canvas.squareSize.width;
            if (i >= board.getHeight() || j >= board.getWidth())
                return;
            backtrack();
            board.loc.setLocation(j, i);
            if (board.getSquare(board.loc) == Square.FREE)
                whenFree();
            else
                whenNotFree();
        }
        
        public abstract void backtrack();
        public abstract void whenFree();
        public abstract void whenNotFree();
    }
    
    /**
     * OtherMouseListener extends MouseReleasedListener and decides what to do when
     * the replay button and/or order button is clicked
    **/
    protected class OtherMouseListener extends MouseReleasedListener {
        public void mouseReleased(MouseEvent e) {
            Object source = e.getSource();
            if (source == replayButton) {
                if (board.state == State.FINISHED &&
                        replayButton.isEnabled()) {
                    replayButton.setEnabled(false);
                    for (int i = 0; i < board.path.size(); i++) {
                        board.loc.setLocation(board.path.get(i));
                        board.setSquare(Square.FREE);
                    }
                    timer.start();
                }
            } else if (source == setOrderButton) {
                if (board.state == State.STOPPED &&
                        setOrderButton.isEnabled()) {
                    if (!setOrder()) {
                        textArea.setText(
                            "Please select an ordering with unique values.\n"
                        );
                    }
                    canvas.requestFocus();
                }
            }
        }
    }
    
    /**
     * TimeHandler class that displays the path found with a delay
    **/
    protected class TimeHandler implements ActionListener {
        protected int i;
        public void actionPerformed(ActionEvent e) {
            if (i < board.path.size()) {
                board.loc.setLocation(board.path.get(i));
                board.setSquare(Square.USED);
                i++;
            } else {
                timer.stop();
                replayButton.setEnabled(true);
                i = 0;
            }
        }
    }
    
    /**
     * GamePauseAction class determines whether the algorithm can procede to
     * run (depending on the order specified), or should stop
     * Event is trigerred when 'enter' key is released
    **/
    protected class GamePauseAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            if (!setOrder()) {
                textArea.setText(
                    "Please select an ordering with unique values.\n"
                );
            } else if (board.state == State.STOPPED) {
                for (JComboBox box : boxes)
                    box.setEnabled(false);
                setOrderButton.setEnabled(false);
                textArea.setText("Calculating...\n");
                new Thread(canvas).start();
            } else {
                board.state = State.STOPPED;
            }
        }
    }
    
    /**
     * GameEndAction class terminates the program and exits
     * Event is trigerred when the 'esc' key is released
    **/
    protected class GameEndAction extends AbstractAction {
        public void actionPerformed(ActionEvent e)
        {System.exit(0);}
    }
}