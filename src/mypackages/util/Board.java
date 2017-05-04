package mypackages.util;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.util.Collection;

public abstract class Board<Square extends Enum<Square>, Action extends ActionListener>
{
	protected Dimension bounds;
	protected Square[][] grid;
	protected Collection<Action> actions;
}