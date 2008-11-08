
package java_cup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Stack;
import java.util.Map.Entry;

/** This class represents a state in the LALR viable prefix recognition machine.
 *  A state consists of a mapping from LR items to a set of terminals (lookaheads)
 *  and of transitions to other states under terminal and non-terminal symbols.
 *  Each state represents a potential configuration of the parser.  If the item 
 *  set of a state includes an item such as: <pre>
 *    [A ::= B * C d E , {a,b,c}]
 *  </pre> 
 *  this indicates that when the parser is in this state it is currently 
 *  looking for an A of the given form, has already seen the B, and would
 *  expect to see an a, b, or c after this sequence is complete.  Note that
 *  the parser is normally looking for several things at once (represented
 *  by several items).  In our example above, the state would also include
 *  items such as: <pre>
 *    [C ::= * X e Z, {d}]
 *    [X ::= * f, {e}]
 *  </pre> 
 *  to indicate that it was currently looking for a C followed by a d (which
 *  would be reduced into a C, matching the first symbol in our production 
 *  above), and the terminal f followed by e.<p>
 *
 *  At runtime, the parser uses a viable prefix recognition machine made up
 *  of these states to parse.  The parser has two operations, shift and reduce.
 *  In a shift, it consumes one Symbol and makes a transition to a new state.
 *  This corresponds to "moving the dot past" a terminal in one or more items
 *  in the state (these new shifted items will then be found in the state at
 *  the end of the transition).  For a reduce operation, the parser is 
 *  signifying that it is recognizing the RHS of some production.  To do this
 *  it first "backs up" by popping a stack of previously saved states.  It 
 *  pops off the same number of states as are found in the RHS of the 
 *  production.  This leaves the machine in the same state is was in when the
 *  parser first attempted to find the RHS.  From this state it makes a 
 *  transition based on the non-terminal on the LHS of the production.  This
 *  corresponds to placing the parse in a configuration equivalent to having 
 *  replaced all the symbols from the the input corresponding to the RHS with 
 *  the symbol on the LHS.
 *
 * @see     java_cup.lr_item
 * @see     java_cup.lalr_transition
 * @version last updated: 7/3/96
 * @author  Frank Flannery
 *  
 */

public class lalr_state {
  /*-----------------------------------------------------------*/
  /*--- Constructor(s) ----------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Constructor for building a state from a set of items.
   * @param itms the set of items that makes up this state.
   */
  private lalr_state(HashMap<lr_item, terminal_set> kernel)
   {
     /* don't allow null or duplicate item sets */
     if (kernel == null)
       throw new AssertionError(
	 "Attempt to construct an LALR state from a null item set");

     /* assign a unique index */
      _index = next_index++;

     /* store the items */
     _items = new HashMap<lr_item, lookaheads>();
     for (Entry<lr_item, terminal_set> entry : kernel.entrySet())
       _items.put(entry.getKey(), new lookaheads(entry.getValue()));
   }
  
  public static lalr_state get_lalr_state(
      Stack<lalr_state> work_stack, 
      HashMap<lr_item, terminal_set> kernel)
    {
      Collection<lr_item> key = kernel.keySet();
      lalr_state state = _all_kernels.get(key);
      if (state != null)
	{
	  state.propagate_lookaheads(kernel);
	}
      else
	{
	  state = new lalr_state(kernel);
	  _all_kernels.put(key, state);
	  work_stack.push(state);
	}
      return state;
    }

  /*-----------------------------------------------------------*/
  /*--- (Access to) Static (Class) Variables ------------------*/
  /*-----------------------------------------------------------*/

  /** Collection of all states. */
  public static Collection<lalr_state> all() {return _all_kernels.values();}

  //Hm Added clear  to clear all static fields
  public static void clear() {
      _all_kernels.clear();
      next_index=0;
      num_conflicts = 0;
  }
  
  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Number of conflict found while building tables. */
  public static int num_conflicts = 0;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Indicate total number of states there are. */
  public static int number() {return _all_kernels.size();}

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Hash table to find states by their kernels (i.e, the original, 
   *  unclosed, set of items -- which uniquely define the state).  This table 
   *  stores state objects using (a copy of) their kernel item sets as keys. 
   */
  protected static HashMap<Collection<lr_item>,lalr_state> _all_kernels = 
    new HashMap<Collection<lr_item>, lalr_state>();

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Static counter for assigning unique state indexes. */
  protected static int next_index = 0;

  /*-----------------------------------------------------------*/
  /*--- (Access to) Instance Variables ------------------------*/
  /*-----------------------------------------------------------*/

  /** The item set for this state. */
  protected HashMap<lr_item, lookaheads> _items;

  /** The item set for this state. */
  public HashMap<lr_item, lookaheads> items() {return _items;}

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** List of transitions out of this state. */
  protected lalr_transition _transitions = null;

  /** List of transitions out of this state. */
  public lalr_transition transitions() {return _transitions;}

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Index of this state in the parse tables */
  protected int _index;

  /** Index of this state in the parse tables */
  public int index() {return _index;}

  /*-----------------------------------------------------------*/
  /*--- Static Methods ----------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Helper routine for debugging -- produces a dump of the given state
    * onto System.out.
    */
  void dump_state()
    {
      System.out.println("lalr_state [" + index() + "] {");
      for (Entry<lr_item, lookaheads> itm : items().entrySet() )
	{
	  System.out.print("  [");
	  System.out.print(itm.getKey().the_production().lhs().the_symbol().name());
	  System.out.print(" ::= ");
	  for (int i = 0; i<itm.getKey().the_production().rhs_length(); i++)
	    {
	      if (i == itm.getKey().dot_pos()) System.out.print("(*) ");
	      production_part part = itm.getKey().the_production().rhs(i);
	      if (part.is_action()) 
		System.out.print("{action} ");
	      else
		System.out.print(((symbol_part)part).the_symbol().name() + " ");
	    }
	  if (itm.getKey().dot_at_end()) System.out.print("(*) ");
	  System.out.println("]");
	}
      System.out.println("}");
    }

  /*-----------------------------------------------------------*/
  /*--- General Methods ---------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Compute the closure of the set using the LALR closure rules.  Basically
   *  for every item of the form: <pre>
   *    [L ::= a *N alpha, l] 
   *  </pre>
   *  (where N is a a non terminal and alpha is a string of symbols) make 
   *  sure there are also items of the form:  <pre>
   *    [N ::= *beta, first(alpha l)] 
   *  </pre>
   *  corresponding to each production of N.  Items with identical cores but 
   *  differing lookahead sets are merged by creating a new item with the same 
   *  core and the union of the lookahead sets (the LA in LALR stands for 
   *  "lookahead merged" and this is where the merger is).  This routine 
   *  assumes that nullability and first sets have been computed for all 
   *  productions before it is called.
   */
  public void compute_closure()
    {
      terminal_set  new_lookaheads;
      boolean       need_prop;

      /* each current element needs to be considered */
      Stack<lr_item> consider = new Stack<lr_item>();
      consider.addAll(_items.keySet());

      /* repeat this until there is nothing else to consider */
      while (consider.size() > 0)
	{
	  /* get one item to consider */
	  lr_item itm = consider.pop();

	  /* do we have a dot before a non terminal */
	  non_terminal nt = itm.dot_before_nt();
	  if (nt != null)
	    {
	      lr_item nextitm = itm.shift_core();
	      /* create the lookahead set based on first symbol after dot */
	      new_lookaheads = nextitm.calc_lookahead();

	      /* are we going to need to propagate our lookahead to new item */
	      need_prop = nextitm.is_nullable();
	      if (need_prop)
		new_lookaheads.add(_items.get(itm));
	      
	      /* create items for each production of that non term */
	      for (production prod : nt.productions() )
		{
		  /* create new item with dot at start and that lookahead */
		  lr_item new_itm = prod.item();
		  lookaheads new_la;
		  if (_items.containsKey(new_itm))
		    {
		      new_la = _items.get(new_itm); 
		      new_la.add(new_lookaheads);
		    }
		  else
		    {
		      new_la = new lookaheads(new_lookaheads);
		      _items.put(new_itm, new_la);
		      /* that may need further closure, consider it also */ 
		      consider.push(new_itm);
		    }
		  
		  /* if propagation is needed link to that item */
		  if (need_prop)
		    _items.get(itm).add_listener(new_la);
		} 
	    }
	} 
    }

  /** Add a transition out of this state to another.
   * @param on_sym the symbol the transition is under.
   * @param to_st  the state the transition goes to.
   */
  public void add_transition(symbol on_sym, lalr_state to_st) 
    {
      lalr_transition trans;

      /* create a new transition object and put it in our list */
      trans = new lalr_transition(on_sym, to_st, _transitions);
      _transitions = trans;
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

  public static lalr_state build_machine(production start_prod) 
    {
      lalr_state        start_state;
      Stack<lalr_state> work_stack = new Stack<lalr_state>();

      /* sanity check */
      assert start_prod != null:
 	  "Attempt to build viable prefix recognizer using a null production";

	{
	  /* build item with dot at front of start production and EOF lookahead */	
	  HashMap<lr_item, terminal_set> start_items = new HashMap<lr_item, terminal_set>();
	  terminal_set lookahead = new terminal_set();
	  lookahead.add(terminal.EOF);
	  lr_item core = start_prod.item();
	  start_items.put(core, lookahead);
	  start_state = get_lalr_state(work_stack, start_items);
	}

      /* continue looking at new states until we have no more work to do */
      while (!work_stack.empty())
	{
	  /* remove a state from the work set */
	  lalr_state st = (lalr_state) work_stack.pop();
	  st.compute_closure();
	  st.compute_successors(work_stack);
	}
      
      return start_state;
    }
  
  private void compute_successors(Stack<lalr_state> work_stack) 
    {
      /* gather up all the symbols that appear before dots */
      HashMap<symbol, ArrayList<lr_item>> outgoing =
	new HashMap<symbol, ArrayList<lr_item>>();
      for (lr_item itm : _items.keySet() )
	{
	  /* add the symbol after the dot (if any) to our collection */
	  symbol sym = itm.symbol_after_dot();
	  if (sym != null)
	    {
	      if (!outgoing.containsKey(sym))
		outgoing.put(sym, new ArrayList<lr_item>());
	      outgoing.get(sym).add(itm);
	    }
	}
      
      /* now create a transition out for each individual symbol */
      for (Entry<symbol,ArrayList<lr_item>> out : outgoing.entrySet() )
	{
	  /* gather up shifted versions of all the items that have this
		 symbol before the dot */
	  HashMap<lr_item, terminal_set> new_items = new HashMap<lr_item, terminal_set>();
	  for (lr_item itm : out.getValue())
	    {
	      /* add to the kernel of the new state */
	      new_items.put(itm.shift_core(), items().get(itm));
	    }

	  /* create/get succesor state */
	  lalr_state new_st = get_lalr_state(work_stack, new_items);
	  for (lr_item itm : out.getValue())
	    {
	      /* ... remember that itm has propagate link to it */
	      items().get(itm).add_listener(
		  new_st.items().get(itm.shift_core()));
	    }

	  /* add a transition from current state to that state */
	  add_transition(out.getKey(), new_st);
	}
    }
 
  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Propagate lookahead sets out of this state. This recursively 
   *  propagates to all items that have propagation links from some item 
   *  in this state. 
   */
  protected void propagate_lookaheads(HashMap<lr_item, terminal_set> new_kernel)
    {
      /* recursively propagate out from each item in the state */
      for (Entry<lr_item, terminal_set> entry : new_kernel.entrySet())
	{
	  _items.get(entry.getKey()).add(entry.getValue());
	}
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Fill in the parse table entries for this state.  There are two 
   *  parse tables that encode the viable prefix recognition machine, an 
   *  action table and a reduce-goto table.  The rows in each table 
   *  correspond to states of the machine.  The columns of the action table
   *  are indexed by terminal symbols and correspond to either transitions 
   *  out of the state (shift entries) or reductions from the state to some
   *  previous state saved on the stack (reduce entries).  All entries in the
   *  action table that are not shifts or reduces, represent errors.    The
   *  reduce-goto table is indexed by non terminals and represents transitions 
   *  out of a state on that non-terminal.<p>
   *  Conflicts occur if more than one action needs to go in one entry of the
   *  action table (this cannot happen with the reduce-goto table).  Conflicts
   *  are resolved by always shifting for shift/reduce conflicts and choosing
   *  the lowest numbered production (hence the one that appeared first in
   *  the specification) in reduce/reduce conflicts.  All conflicts are 
   *  reported and if more conflicts are detected than were declared by the
   *  user, code generation is aborted.
   *
   * @param act_table    the action table to put entries in.
   * @param reduce_table the reduce-goto table to put entries in.
   */
  public void build_table_entries(
    parse_action_table act_table, 
    parse_reduce_table reduce_table)
    {
      parse_action_row our_act_row;
      parse_reduce_row our_red_row;
      parse_action     act, other_act;
      symbol           sym;
      terminal_set     conflict_set = new terminal_set();

      /* pull out our rows from the tables */
      our_act_row = act_table.under_state[index()];
      our_red_row = reduce_table.under_state[index()];

      /* consider each item in our state */
      for (Entry<lr_item, lookaheads> itm : items().entrySet())
	{

	  /* if its completed (dot at end) then reduce under the lookahead */
	  if (itm.getKey().dot_at_end())
	    {
	      act = new reduce_action(itm.getKey().the_production());

	      /* consider each lookahead symbol */
	      for (int t = 0; t < terminal.number(); t++)
		{
		  /* skip over the ones not in the lookahead */
		  if (!itm.getValue().contains(t)) continue;

	          /* if we don't already have an action put this one in */
	          if (our_act_row.under_term[t].kind() == 
		      parse_action.ERROR)
		    {
	              our_act_row.under_term[t] = act;
		    }
	          else
		    {
		      /* we now have at least one conflict */
		      terminal term = terminal.find(t);
		      other_act = our_act_row.under_term[t];

		      /* if the other act was not a shift */
		      if (other_act.kind() != parse_action.SHIFT)
		        {
		        /* if we have lower index hence priority, replace it*/
		          if (itm.getKey().the_production().index() < 
			      ((reduce_action)other_act).reduce_with().index())
			    {
			      /* replace the action */
			      our_act_row.under_term[t] = act;
			    }
		        } 
		      else
			{
			  /*  Check precedences,see if problem is correctable */
			  if(fix_with_precedence(itm.getKey().the_production(), 
			      t, our_act_row, act)) 
			    {
			      term = null;
			    }
			}
		      if(term!=null) 
			{
			  conflict_set.add(term);
			}
		    }
		}
	    }
	}

      /* consider each outgoing transition */
      for (lalr_transition trans=transitions(); trans!=null; trans=trans.next())
	{
	  /* if its on an terminal add a shift entry */
	  sym = trans.on_symbol();
	  if (!sym.is_non_term())
	    {
	      act = new shift_action(trans.to_state());

	      /* if we don't already have an action put this one in */
	      if ( our_act_row.under_term[sym.index()].kind() == 
		   parse_action.ERROR)
		{
	          our_act_row.under_term[sym.index()] = act;
		}
	      else
		{
		  /* we now have at least one conflict */
		  production p = ((reduce_action)our_act_row.under_term[sym.index()]).reduce_with();

		  /* shift always wins */
		  if (!fix_with_precedence(p, sym.index(), our_act_row, act))
		    {
		      our_act_row.under_term[sym.index()] = act;
		      conflict_set.add(terminal.find(sym.index()));
		    }
		}
	    }
	  else
	    {
	      /* for non terminals add an entry to the reduce-goto table */
	      our_red_row.under_non_term[sym.index()] = trans.to_state();
	    }
	}

      /* if we end up with conflict(s), report them */
      if (!conflict_set.empty())
        report_conflicts(conflict_set);
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

    
  /** Procedure that attempts to fix a shift/reduce error by using
   * precedences.  --frankf 6/26/96
   *  
   *  if a production (also called rule) and the lookahead terminal
   *  has a precedence, then the table can be fixed.  if the rule
   *  has greater precedence than the terminal, a reduce by that rule
   *  in inserted in the table.  If the terminal has a higher precedence, 
   *  it is shifted.  if they have equal precedence, then the associativity
   *  of the precedence is used to determine what to put in the table:
   *  if the precedence is left associative, the action is to reduce. 
   *  if the precedence is right associative, the action is to shift.
   *  if the precedence is non associative, then it is a shift/reduce error.
   *
   *  @param p           the production
   *  @param term_index  the index of the lokahead terminal
   *  @param parse_action_row  a row of the action table
   *  @param act         the rule in conflict with the table entry
   */

    protected boolean fix_with_precedence(
		        production       p,
			int              term_index,
			parse_action_row table_row,
			parse_action     act)
      {

      terminal term = terminal.find(term_index);

      /* if both production and terminal have a precedence number, 
       * it can be fixed */
      if (p.precedence_num() > assoc.no_prec
	  && term.precedence_num() > assoc.no_prec) {

	/* if production precedes terminal, put reduce in table */
	if (p.precedence_num() > term.precedence_num()) {
	  table_row.under_term[term_index] = 
	    insert_reduce(table_row.under_term[term_index],act);
	  return true;
	} 

	/* if terminal precedes rule, put shift in table */
	else if (p.precedence_num() < term.precedence_num()) {
	  table_row.under_term[term_index] = 
	    insert_shift(table_row.under_term[term_index],act);
	  return true;
	} 
	else {  /* they are == precedence */

	  /* equal precedences have equal sides, so only need to 
	     look at one: if it is right, put shift in table */
	  if (term.precedence_side() == assoc.right) {
	  table_row.under_term[term_index] = 
	    insert_shift(table_row.under_term[term_index],act);
	    return true;
	  }

	  /* if it is left, put reduce in table */
	  else if (term.precedence_side() == assoc.left) {
	    table_row.under_term[term_index] = 
	      insert_reduce(table_row.under_term[term_index],act);
	    return true;
	  }

	  /* if it is nonassoc, we're not allowed to have two nonassocs
	     of equal precedence in a row */
	  else {
	    assert term.precedence_side() == assoc.nonassoc;
	    return false;
	  }
	}
      }
       
      /* otherwise, neither the rule nor the terminal has a precedence,
	 so it can't be fixed. */
      return false;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/


  /*  given two actions, and an action type, return the 
      action of that action type.  give an error if they are of
      the same action, because that should never have tried
      to be fixed 
     
  */
    protected parse_action insert_action(
					parse_action a1,
					parse_action a2,
					int act_type) 
    {
      assert (a1.kind() == act_type) != (a2.kind() == act_type) : 
	"Conflict resolution of bogus actions";
      if (a1.kind() == act_type)
	return a1;
      else
	return a2;
    }

    /* find the shift in the two actions */
    protected parse_action insert_shift(
					parse_action a1,
					parse_action a2) 
    {
      return insert_action(a1, a2, parse_action.SHIFT);
    }

    /* find the reduce in the two actions */
    protected parse_action insert_reduce(
					parse_action a1,
					parse_action a2) 
    {
      return insert_action(a1, a2, parse_action.REDUCE);
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Produce warning messages for all conflicts found in this state.  */
  protected void report_conflicts(terminal_set conflict_set)
    {
      boolean      after_itm;

      /* consider each element */
      for (Entry<lr_item,lookaheads> itm : items().entrySet())
	{
	  /* clear the S/R conflict set for this item */

	  /* if it results in a reduce, it could be a conflict */
	  if (itm.getKey().dot_at_end())
	    {
	      /* not yet after itm */
	      after_itm = false;

	      /* compare this item against all others before this,
	       * looking for conflicts */
	      for (Entry<lr_item,lookaheads> compare : items().entrySet())
		{
		  /* if this is the item break out of the loop */
		  if (itm.getKey() == compare.getKey())
		    break;

		  /* is it a reduce */
		  if (compare.getKey().dot_at_end())
		    {
		      if (after_itm)
			/* does the comparison item conflict? */
			if (compare.getValue().intersects(itm.getValue()))
			  /* report a reduce/reduce conflict */
			  report_reduce_reduce(itm, compare);
		    }
		}
	      /* report S/R conflicts under all the symbols we conflict under */
	      for (int t = 0; t < terminal.number(); t++)
		if (itm.getValue().contains(t) && conflict_set.contains(t))
		  report_shift_reduce(itm.getKey(),t);
	    }
	}
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Produce a warning message for one reduce/reduce conflict. 
   *
   * @param itm1 first item in conflict.
   * @param itm2 second item in conflict.
   */
  protected void report_reduce_reduce(
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
      message.append("*** Reduce/Reduce conflict found in state #").append(index()).append("\n")
      	.append("  between ").append(itm1.getKey().toString()).append("\n")
      	.append("  and     ").append(itm2.getKey().toString()).append("\n")
	.append("  under symbols: {");
      String comma = "";
      for (int t = 0; t < terminal.number(); t++)
	{
	  if (itm1.getValue().contains(t) && itm2.getValue().contains(t))
	    {
	      message.append(comma).append(terminal.find(t).name());
	      comma = ", ";
	    }
	}
      message.append("}\n  Resolved in favor of the first production.\n");

      /* count the conflict */
      num_conflicts++;
      ErrorManager.getManager().emit_warning(message.toString());
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Produce a warning message for one shift/reduce conflict.
   *
   * @param red_itm      the item with the reduce.
   * @param conflict_sym the index of the symbol conflict occurs under.
   */
  protected void report_shift_reduce(
    lr_item red_itm, 
    int       conflict_sym)
    {
      symbol       shift_sym;

      /* emit top part of message including the reduce item */
      String message = "*** Shift/Reduce conflict found in state #"+index()+"\n" +
      "  between " + red_itm.toString()+"\n";

      /* find and report on all items that shift under our conflict symbol */
      for (lr_item itm : items().keySet())
	{
	  /* only look if its not the same item and not a reduce */
	  if (itm != red_itm && !itm.dot_at_end())
	    {
	      /* is it a shift on our conflicting terminal */
	      shift_sym = itm.symbol_after_dot();
	      if (!shift_sym.is_non_term() && shift_sym.index() == conflict_sym)
	        {
		  /* yes, report on it */
                  message += "  and     " + itm.toString()+"\n";
		}
	    }
	}
      message += "  under symbol "+ terminal.find(conflict_sym).name() + "\n"+
      "  Resolved in favor of shifting.\n";

      /* count the conflict */
      num_conflicts++;
      ErrorManager.getManager().emit_warning(message);
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Equality comparison. */
  public boolean equals(lalr_state other)
    {
      /* we are equal if our item sets are equal */
      return other != null && items().equals(other.items());
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Generic equality comparison. */
  public boolean equals(Object other)
    {
      if (!(other instanceof lalr_state))
	return false;
      else
	return equals((lalr_state)other);
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Produce a hash code. */
  public int hashCode()
    {
      /* just use the item set hash code */
      return items().hashCode();
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Convert to a string. */
  public String toString()
    {
      StringBuilder result = new StringBuilder();
      lalr_transition tr;

      /* dump the item set */
      result.append("lalr_state [").append(index()).append("]: {\n");
      for (Entry<lr_item,lookaheads> itm : items().entrySet()) 
	{
	  /* only print the kernel */
	  if (itm.getKey().dot_pos() == 0)
	    continue;
	  result.append("  [").append(itm.getKey()).append(", ");
	  result.append(itm.getValue()).append("]\n");
	}
      result.append("}\n");

      /* do the transitions */
      for (tr = transitions(); tr != null; tr = tr.next())
	{
	  result.append(tr).append("\n");
	}

      return result.toString();
    }

  /*-----------------------------------------------------------*/
}
