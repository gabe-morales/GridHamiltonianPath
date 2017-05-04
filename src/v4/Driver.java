/*
 * Author: Gabriel Morales
 * Course: CSC 258 - Parallel & Distributed Systems (SPRING 2017)
 */
package v4;

import java.awt.Dimension;
import java.util.Scanner;
import java.io.FileReader;

/**
 * Driver class to run the project
**/
public class Driver {
    
    /**
     * usage()
     *
     * Description:
     *   Displays a usage message to the user.
    **/
    public static void usage() {
        System.out.println(
            "Usage: java Driver [-options]\n" +
            "OPTIONS\n" +
            "\t-a, --args <rows> <columns>\tcreate a board with the" +
                "specified dimensions\n" +
            "\t-f, --file <file>\t\timport board from file\n" +
            "\t-t, --threads <tc>\t\tuse 'tc' many threads\n" +
            "\t-h, --headless\t\trun without gui"
        );
    }
    
    /**
     * main()
     * 
     * Description:
     *   Takes command line arguments for height (#rows) and width (#columns),
     *   or scans them in from standard input, and spawns a new thread to
     *   create the board and run the path puzzle software.
     *
     * Inputs:
     *   args - Array of strings to be parsed into height and width dimensions
    **/
    public static void main(String[] args) {
        int v1 = -1, v2 = -1, numThreads = 1;
        long startTime, endTime;
        boolean headless = false;
        boolean[][] board = null;
        Dimension dimension = null;
        Scanner scanner;
        String t, f, string;
        Runnable run;
        
        if (args.length == 0) {
            scanner = new Scanner(System.in);
            System.out.print("Enter number of rows: ");
            v1 = scanner.nextInt();
            System.out.print("Enter number of columns: ");
            v2 = scanner.nextInt();
            scanner.close();
            dimension = new Dimension(v2, v1);
        } else {
            for (int i = 0; i < args.length; i++) {
                if (args[i].matches("-(a|-args)")) {
                    if (args.length < i+3) {
                        usage();
                        System.exit(-1);
                    }
                    try {
                        v1 = Integer.parseInt(args[++i]);
                        v2 = Integer.parseInt(args[++i]);
                        dimension = new Dimension(v2, v1);
                    } catch (Exception e) {
                        usage();
                        System.exit(-1);
                    }
                } else if (args[i].matches("-(f|-file)")) {
                    if (args.length < i+2) {
                        usage();
                        System.exit(-1);
                    }
                    try {
                        scanner = new Scanner(new FileReader(args[++i]));
                        t = scanner.next();
                        f = scanner.next();
                        v1 = scanner.nextInt();
                        v2 = scanner.nextInt();
                        board = new boolean[v1][v2];
                        for (int y = 0; y < v1; y++)
                            for (int x = 0; x < v2; x++) {
                                if (!scanner.hasNext()) {
                                    System.err.println("Unexpected EOF");
                                    System.exit(-1);
                                }
                                string = scanner.next();
                                if (string.equals(t)) {
                                    board[y][x] = true;
                                } else if (string.equals(f)) {
                                    board[y][x] = false;
                                } else {
                                    System.err.println("Invalid item in file");
                                    System.exit(-1);
                                }
                            }
                        v1 = scanner.nextInt();
                        v2 = scanner.nextInt();
                        scanner.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.exit(-1);
                    }
                } else if (args[i].matches("-(t|-threads)")) {
                    if (args.length < i+2) {
                        usage();
                        System.exit(-1);
                    }
                    try {
                        numThreads = Integer.parseInt(args[++i]);
                    } catch (Exception e) {
                        usage();
                        System.exit(-1);
                    }
                } else if (args[i].matches("-(h|-headless)")) {
                    headless = true;
                } else {
                    usage();
                    System.exit(-1);
                }
            }
        }
        
        if (headless && board == null) {
            System.err.println("Use of 'headless' without an input file");
            System.exit(-1);
        }
        
        run = ((board != null)?
            ((headless)?
                new TTY(board, v1, v2, numThreads) :
                new GUI(board, v1, v2, numThreads)
            ) :
            new GUI(dimension.height, dimension.width, numThreads)
        );
        run.run();
    }
}