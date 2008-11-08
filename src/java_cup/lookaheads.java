package java_cup;

import java.util.ArrayList;
import java.util.Stack;

public class lookaheads extends terminal_set {
  private ArrayList<lookaheads> propagations;
  
  public lookaheads(terminal_set t)
    {
      super(t);
      propagations = new ArrayList<lookaheads>();
    }

  /** Propagate lookahead sets through the constructed viable prefix 
   *  recognizer.  When the machine is constructed, each item that results
      in the creation of another such that its lookahead is included in the
      other's propagate info.  This allows additions
      to the lookahead of one item to be included in other items that it 
      was used to directly or indirectly create.
   */
  public void add_propagation(lookaheads child)
    {
      propagations.add(child); 
    }

  private boolean add_without_prop(terminal_set new_lookaheads)
    {
      return super.add(new_lookaheads);
    }
  
  public boolean add(terminal_set new_lookaheads)
    {
      if (!super.add(new_lookaheads))
	return false;
      
      Stack<lookaheads> work = new Stack<lookaheads>();
      work.addAll(propagations);
      while (!work.isEmpty())
	{
	  lookaheads la = work.pop();
	  if (la.add_without_prop(new_lookaheads))
	    {
	      work.addAll(la.propagations);
	    }
	}
      return true;
    }


}
