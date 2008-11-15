package java_cup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * Represents the context-free grammar for which we build a parser.  An object of this class is 
 * created by the JavaCUP parser which reads in the user grammar.  A grammar is a collection of
 * non-terminal and terminal symbols and productions.
 * 
 * This class also contains the code to build the viable prefix recognizer, which is the heart of
 * the created parser.
 *
 * @author hoenicke
 *
 */
public class Grammar {
  private final ArrayList<terminal>     _terminals;
  private final ArrayList<non_terminal> _nonterminals;
  private final ArrayList<production>   _productions;

  /** Hash table to find states by their kernels (i.e, the original, 
   *  unclosed, set of items -- which uniquely define the state).  This table 
   *  stores state objects using (a copy of) their kernel item sets as keys. 
   */
  private final HashMap<Collection<lr_item>, lalr_state> _kernels_to_lalr = 
    new HashMap<Collection<lr_item>, lalr_state>();
  private final ArrayList<lalr_state> _lalr_states = new ArrayList<lalr_state>();

  /** Number of conflict found while building tables. */
  private int _num_conflicts = 0;

  public Grammar(ArrayList<terminal> t, ArrayList<non_terminal> nt, ArrayList<production> p)
    {
      _nonterminals = nt;
      _terminals = t;
      _productions =p;
    }
  
  public void build_states(non_terminal startsymbol)
    {
      
    }
  
  public non_terminal find_nonterminal(int i)
    {
      return _nonterminals.get(i);
    }
  
  public terminal find_terminal(int i)
    {
      return _terminals.get(i);
    }
  public int num_terminals()
    {
      return _terminals.size();
    }
  public int num_non_terminals()
    {
      return _nonterminals.size();
    }
  public int num_productions()
    {
      return _productions.size();
    }
  
  public Iterable<terminal> terminals()
    {
      return _terminals;
    }

  public Iterable<non_terminal> non_terminals()
  {
    return _nonterminals;
  }

  public Iterable<production> productions()
  {
    return _productions;
  }

  public int num_conflicts()
    {
      return _num_conflicts;
    }
  
  public lalr_state get_lalr_state(HashMap<lr_item, terminal_set> kernel)
    {
      Collection<lr_item> key = kernel.keySet();
      lalr_state state = _kernels_to_lalr.get(key);
      if (state != null)
	{
	  state.propagate_lookaheads(kernel);
	}
      else
	{
	  state = new lalr_state(kernel, _lalr_states.size());
	  _lalr_states.add(state);
	  _kernels_to_lalr.put(key, state);
	}
      return state;
    }

  /*-----------------------------------------------------------*/
  /*--- (Access to) Static (Class) Variables ------------------*/
  /*-----------------------------------------------------------*/

  /** Collection of all states. */
  public Collection<lalr_state> lalr_states() 
    {
      return _lalr_states;
    }
  
  /** Compute nullability of all non-terminals. */
  public void compute_nullability()
    {
      boolean      change = true;

      /* repeat this process until there is no change */
      while (change)
	{
	  /* look for a new change */
	  change = false;

	  /* consider each non-terminal */
	  for (non_terminal nt : _nonterminals)
	    {
	      /* only look at things that aren't already marked nullable */
	      if (!nt._nullable && nt.looks_nullable())
		{
		  nt._nullable = true;
		  change = true;
		}
	    }
	}
      
      /* do one last pass over the productions to finalize all of them */
      for (production prod : _productions)
	{
	  prod.set_nullable(prod.check_nullable());
	}
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Compute first sets for all non-terminals.  This assumes nullability has
   *  already computed.
   */
  public void compute_first_sets()
    {
      boolean      change = true;

      /* initialize first sets */
      for (non_terminal nt : _nonterminals)
	nt._first_set = new terminal_set(this);
      
      /* repeat this process until we have no change */
      while (change)
	{
	  /* look for a new change */
	  change = false;

	  /* consider each non-terminal */
	  for (non_terminal nt : _nonterminals)
	    {
	      /* consider every production of that non terminal */
	      for (production prod : nt.productions())
		{
		  /* get the updated first of that production */
		  terminal_set prod_first = prod.first_set(this);

		  /* if this going to add anything, add it */
		  change |= nt._first_set.add(prod_first);
		}
	    }
	}
    }
  
  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Build an LALR viable prefix recognition machine given a start 
   *  production.  This method operates by first building a start state
   *  from the start production (based on a single item with the dot at
   *  the beginning and EOF as expected lookahead).  Then for each state
   *  it attempts to extend the machine by creating transitions out of
   *  the state to new or existing states.  When considering extension
   *  from a state we make a transition on each symbol that appears before
   *  the dot in some item.  For example, if we have the items: <pre>
   *    [A ::= a b * X c, {d,e}]
   *    [B ::= a b * X d, {a,b}]
   *  </pre>
   *  in some state, then we would be making a transition under X to a new
   *  state.  This new state would be formed by a "kernel" of items 
   *  corresponding to moving the dot past the X.  In this case: <pre>
   *    [A ::= a b X * c, {d,e}]
   *    [B ::= a b X * Y, {a,b}]
   *  </pre>
   *  The full state would then be formed by "closing" this kernel set of 
   *  items so that it included items that represented productions of things
   *  the parser was now looking for.  In this case we would items 
   *  corresponding to productions of Y, since various forms of Y are expected
   *  next when in this state (see lalr_item_set.compute_closure() for details 
   *  on closure). <p>
   *
   *  The process of building the viable prefix recognizer terminates when no
   *  new states can be added.  However, in order to build a smaller number of
   *  states (i.e., corresponding to LALR rather than canonical LR) the state 
   *  building process does not maintain full loookaheads in all items.  
   *  Consequently, after the machine is built, we go back and propagate 
   *  lookaheads through the constructed machine using a call to 
   *  propagate_all_lookaheads().  This makes use of propagation links 
   *  constructed during the closure and transition process.
   *
   * @param start_prod the start production of the grammar
   * @see   java_cup.lalr_item_set#compute_closure
   * @see   java_cup.lalr_state#propagate_all_lookaheads
   */

  public lalr_state build_machine(production start_prod) 
    {
      /* sanity check */
      assert start_prod != null:
	"Attempt to build viable prefix recognizer using a null production";

      /* build item with dot at front of start production and EOF lookahead */	
      HashMap<lr_item, terminal_set> start_items = new HashMap<lr_item, terminal_set>();
      terminal_set lookahead = new terminal_set(this);
      lookahead.add(terminal.EOF);
      lr_item core = start_prod.item();
      start_items.put(core, lookahead);
      lalr_state start_state = get_lalr_state(start_items);

      /* continue looking at new states until we have no more work to do.
       * Note that the lalr_states are continually expanded.
       */
      for (int i = 0; i < _lalr_states.size(); i++)
	{
	  /* remove a state from the work set */
	  lalr_state st = _lalr_states.get(i);;
	  st.compute_closure(this);
	  st.compute_successors(this);
	}
      
      return start_state;
    }
  
  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /** Produce a human readable dump of the grammar. */
  public void dump_grammar()
    {
      System.err.println("===== Terminals =====");
      int cnt = 0;
      for (terminal t : terminals())
	{
	  System.err.print("[" + t.index() + "]" + t.name() + " ");
	  if ((++cnt) % 5 == 0)
	    System.err.println();
	}
      System.err.println();
      System.err.println();

      System.err.println("===== Non terminals =====");
      cnt = 0;
      for (non_terminal nt : non_terminals())
	{
	  System.err.print("[" + nt.index() + "]" + nt.name()
	      + " ");
	  if ((++cnt) % 5 == 0)
	    System.err.println();
	}
      System.err.println();
      System.err.println();

      System.err.println("===== Productions =====");
      for (production prod : productions())
	{
	  System.err.print("[" + prod.index() + "] " + prod.lhs().name()
	      + " ::= ");
	  for (int i = 0; i < prod.rhs_length(); i++)
	    if (prod.rhs(i).is_action())
	      System.err.print("{action} ");
	    else
	      System.err.print(((symbol_part) prod.rhs(i)).the_symbol().name()
		  + " ");
	  System.err.println();
	}
      System.err.println();
    }
  
  
  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Produce a warning message for one reduce/reduce conflict. 
   *
   * @param itm1 first item in conflict.
   * @param itm2 second item in conflict.
   */
  public void report_reduce_reduce(lalr_state state,
      Entry<lr_item,lookaheads> itm1,
      Entry<lr_item,lookaheads> itm2)
    {
      if (itm1.getKey().the_production().index() > itm2.getKey().the_production().index())
	{
	  Entry<lr_item,lookaheads> tmpitm = itm1;
	  itm1 = itm2;
	  itm2 = tmpitm;
	}
      
      StringBuilder message = new StringBuilder();
      message.append("*** Reduce/Reduce conflict found in state #").append(state.index()).append("\n")
      	.append("  between ").append(itm1.getKey().toString()).append("\n")
      	.append("  and     ").append(itm2.getKey().toString()).append("\n")
	.append("  under symbols: {");
      String comma = "";
      for (int t = 0; t < num_terminals(); t++)
	{
	  if (itm1.getValue().contains(t) && itm2.getValue().contains(t))
	    {
	      message.append(comma).append(find_terminal(t).name());
	      comma = ", ";
	    }
	}
      message.append("}\n  Resolved in favor of the first production.\n");

      /* count the conflict */
      _num_conflicts++;
      ErrorManager.getManager().emit_warning(message.toString());
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Produce a warning message for one shift/reduce conflict.
   *
   * @param p            the production that is not reduced.
   * @param conflict_sym the index of the symbol conflict occurs under.
   */
  public void report_shift_reduce(lalr_state state,
    production p,
    symbol     conflict_sym)
    {
      /* emit top part of message including the reduce item */
      StringBuilder message = new StringBuilder();
      message.append("*** Shift/Reduce conflict found in state #").append(state.index()).append("\n");
      message.append("  between ").append(p).append("(*)\n");

      /* find and report on all items that shift under our conflict symbol */
      for (lr_item itm : state.items().keySet())
	{
	  /* only look if its not the same item and not a reduce */
	  if (!itm.dot_at_end() && itm.symbol_after_dot().equals(conflict_sym))
	    {
	      /* yes, report on it */
	      message.append("  and     ").append(itm).append("\n");
	    }
	}
      message.append("  under symbol ").append(conflict_sym).append("\n");
      message.append("  Resolved in favor of shifting.\n");

      /* count the conflict */
      _num_conflicts++;
      ErrorManager.getManager().emit_warning(message.toString());
    }

  
}
