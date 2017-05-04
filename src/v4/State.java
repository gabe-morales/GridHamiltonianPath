/*
 * Author: Gabriel Morales
 * Course: CSC 258 - Parallel & Distributed Systems (SPRING 2017)
 */
package v4;

/**
 * State enum class defining the possible states for the path puzzle solver
 *   FINISHED - solution is found/solution is determined to be nonexistent
 *   RUNNING - solver is calculating path
 *   STOPPED - solver has been paused in its current state
**/
enum State
{FINISHED, RUNNING, STOPPED;}