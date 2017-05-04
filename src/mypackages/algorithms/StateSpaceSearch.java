package mypackages.algorithms;

import java.awt.event.ActionListener;
import java.util.List;
import java.util.concurrent.Callable;

public interface StateSpaceSearch<State, Action extends ActionListener, Result>
extends Callable<Result>
{
	public void initialState();
	public boolean isGoal();
	public boolean findSolution();
	public List<Action> actions(State state);
}