package java_cup;

import java.util.Collection;
import java.util.ArrayList;

/**
 * This class represents a production in the grammar. It contains a LHS non
 * terminal, and an array of RHS symbols. As various transformations are done on
 * the RHS of the production, it may shrink. As a result a separate length is
 * always maintained to indicate how much of the RHS array is still valid.
 * <p>
 * 
 * I addition to construction and manipulation operations, productions provide
 * methods for factoring out actions (see remove_embedded_actions()), for
 * computing the nullability of the production (i.e., can it derive the empty
 * string, see check_nullable()), and operations for computing its first set
 * (i.e., the set of terminals that could appear at the beginning of some string
 * derived from the production, see check_first_set()).
 * 
 * @see java_cup.production_part
 * @see java_cup.symbol_part
 * @see java_cup.action_part
 * @version last updated: 7/3/96
 * @author Frank Flannery
 */

public class production {

  /*-----------------------------------------------------------*/
  /*--- Constructor(s) ----------------------------------------*/
  /*-----------------------------------------------------------*/

  /**
   * Full constructor. This constructor accepts a LHS non terminal, an array of
   * RHS parts (including terminals, non terminals, and actions), and a string
   * for a final reduce action. It does several manipulations in the process of
   * creating a production object. After some validity checking it translates
   * labels that appear in actions into code for accessing objects on the
   * runtime parse stack. It them merges adjacent actions if they appear and
   * moves any trailing action into the final reduce actions string. Next it
   * removes any embedded actions by factoring them out with new action
   * productions. Finally it assigns a unique index to the production.
   * <p>
   * 
   * Factoring out of actions is accomplished by creating new "hidden" non
   * terminals. For example if the production was originally:
   * 
   * <pre>
   *    A ::= B {action} C D
   * </pre>
   * 
   * then it is factored into two productions:
   * 
   * <pre>
   *    A ::= B X C D
   *    X ::= {action}
   * </pre>
   * 
   * (where X is a unique new non terminal). This has the effect of placing all
   * actions at the end where they can be handled as part of a reduce by the
   * parser.
   */
  public production(non_terminal lhs_sym, production_part rhs_parts[], int rhs_len)
    {
      int i;

      /* make sure we have a valid left-hand-side */
      assert lhs_sym != null : "Attempt to construct a production with a null LHS";


      /* count use of lhs */
      lhs_sym.note_use();

      /* create the part for left-hand-side */
      _lhs = new symbol_part(lhs_sym);

      /* merge adjacent actions (if any) */
      rhs_len = merge_adjacent_actions(rhs_parts, rhs_len);

      /* strip off any trailing action */
      if (rhs_len > 0 && rhs_parts[rhs_len-1].is_action())
	{
	  _action = (action_part) rhs_parts[--rhs_len];
	}

      int last_act_loc = -1;
      /* allocate and copy over the right-hand-side */
      /* count use of each rhs symbol */
      _rhs_length = rhs_len;
      _rhs = new symbol_part[rhs_len];
      for (i = 0; i < rhs_len; i++)
	{
	  if (rhs_parts[i].is_action())
	    {
	      /* create a new non terminal for the action production */
	      non_terminal new_nt = non_terminal.create_new(
		  null, lhs().the_symbol().stack_type()); 
	      new_nt.is_embedded_action = true;

	      /* create a new production with just the action */
	      new action_production(this, new_nt, (action_part) rhs_parts[i], 
		  i, last_act_loc);
	      last_act_loc = i;

	      /* replace the action with the generated non terminal */
	      _rhs[i] = new symbol_part(new_nt);
	    }
	  else
	    {
	      _rhs[i] = (symbol_part) rhs_parts[i];
	      symbol rhs_sym = _rhs[i].the_symbol();
	      rhs_sym.note_use();
	      if (rhs_sym instanceof terminal)
		{
		  terminal term = (terminal) rhs_sym;
		  if (term.precedence_num() != assoc.no_prec)
		    {
		      _rhs_prec = term.precedence_num();
		      _rhs_assoc = term.precedence_side();
		    }
		}
	    }
	}
      indexOfIntermediateResult = last_act_loc;

      /* assign an index */
      _index = _all.size();

      /* put us in the global collection of productions */
      _all.add(this);

      /* put us in the production list of the lhs non terminal */
      lhs_sym.add_production(this);
    }

  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /*
   * Constructor w/ no action string and contextual precedence defined
   */
  public production(non_terminal lhs_sym, production_part rhs_parts[], int rhs_len, 
      int prec_num, int prec_side)
    {
      this(lhs_sym, rhs_parts, rhs_len);
      /* set the precedence */
      set_precedence_num(prec_num);
      set_precedence_side(prec_side);
    }

  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /*-----------------------------------------------------------*/
  /*--- (Access to) Static (Class) Variables ------------------*/
  /*-----------------------------------------------------------*/

  /**
   * Table of all productions. Elements are stored using their index as the key.
   */
  protected static ArrayList<production> _all = new ArrayList<production>();

  /** Access to all productions. */
  public static Collection<production> all()
    {
      return _all;
    }

  /** Lookup a production by index. */
  public static production find(int indx)
    {
      return _all.get(indx);
    }

  // Hm Added clear to clear all static fields
  public static void clear()
    {
      _all.clear();
    }

  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /** Total number of productions. */
  public static int number()
    {
      return _all.size();
    }

  /*-----------------------------------------------------------*/
  /*--- (Access to) Instance Variables ------------------------*/
  /*-----------------------------------------------------------*/

  /** The left hand side non-terminal. */
  protected symbol_part _lhs;

  /** The left hand side non-terminal. */
  public symbol_part lhs()
    {
      return _lhs;
    }

  /** The precedence of the rule */
  protected int _rhs_prec = -1;
  protected int _rhs_assoc = -1;

  /** Access to the precedence of the rule */
  public int precedence_num()
    {
      return _rhs_prec;
    }

  public int precedence_side()
    {
      return _rhs_assoc;
    }

  /** Setting the precedence of a rule */
  public void set_precedence_num(int prec_num)
    {
      _rhs_prec = prec_num;
    }

  public void set_precedence_side(int prec_side)
    {
      _rhs_assoc = prec_side;
    }

  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /** A collection of parts for the right hand side. */
  protected symbol_part _rhs[];

  /** Access to the collection of parts for the right hand side. */
  public symbol_part rhs(int indx)
    {
      assert indx >= 0 && indx < _rhs_length : "Index out of range for right hand side of production";
      return _rhs[indx];
    }

  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /** How much of the right hand side array we are presently using. */
  protected int _rhs_length;

  /** How much of the right hand side array we are presently using. */
  public int rhs_length()
    {
      return _rhs_length;
    }

  public int rhs_params() 
    {
      return _rhs_length;
    }

  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /**
   * An action_part containing code for the action to be performed when we
   * reduce with this production.
   */
  protected action_part _action;

  /**
   * An action_part containing code for the action to be performed when we
   * reduce with this production.
   */
  public action_part action()
    {
      return _action;
    }

  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /** Index number of the production. */
  protected int _index;

  /** Index number of the production. */
  public int index()
    {
      return _index;
    }

  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /** initial lr item corresponding to the production. */
  protected lr_item _itm;

  /** Index number of the production. */
  public lr_item item()
    {
      if (_itm == null)
	_itm = new lr_item(this);
      return _itm;
    }

  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /** Count of number of reductions using this production. */
  protected int _num_reductions = 0;

  /** Count of number of reductions using this production. */
  public int num_reductions()
    {
      return _num_reductions;
    }

  /** Increment the count of reductions with this non-terminal */
  public void note_reduction_use()
    {
      _num_reductions++;
    }

  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /** Is the nullability of the production known or unknown? */
  protected boolean _nullable_known = false;

  /** Is the nullability of the production known or unknown? */
  public boolean nullable_known()
    {
      return _nullable_known;
    }

  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /** Nullability of the production (can it derive the empty string). */
  protected boolean _nullable = false;

  /** Nullability of the production (can it derive the empty string). */
  public boolean nullable()
    {
      return _nullable;
    }

  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /**
   * First set of the production. This is the set of terminals that could appear
   * at the front of some string derived from this production.
   */
  protected terminal_set _first_set = new terminal_set();

  /**
   * First set of the production. This is the set of terminals that could appear
   * at the front of some string derived from this production.
   */
  public terminal_set first_set()
    {
      return _first_set;
    }

  /*-----------------------------------------------------------*/
  /*--- Static Methods ----------------------------------------*/
  /*-----------------------------------------------------------*/

  /**
   * Determine if a given character can be a label id starter.
   * 
   * @param c
   *                the character in question.
   */
  protected static boolean is_id_start(char c)
    {
      return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c == '_');

      // later need to handle non-8-bit chars here
    }

  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /**
   * Determine if a character can be in a label id.
   * 
   * @param c
   *                the character in question.
   */
  protected static boolean is_id_char(char c)
    {
      return is_id_start(c) || (c >= '0' && c <= '9');
    }

  /*-----------------------------------------------------------*/
  /*--- General Methods ---------------------------------------*/
  /*-----------------------------------------------------------*/

  int indexOfIntermediateResult;
  /**
   * @return the index of the result of the previous intermediate action on the stack relative to top, -1 if no previous action
   */
  public int getIndexOfIntermediateResult(){
      return indexOfIntermediateResult;
  }
  
  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /**
   * Helper routine to merge adjacent actions in a set of RHS parts
   * 
   * @param rhs_parts
   *                array of RHS parts.
   * @param len
   *                amount of that array that is valid.
   * @return remaining valid length.
   */
  protected int merge_adjacent_actions(production_part rhs_parts[], int len)
    {
      int from_loc, to_loc;

      to_loc = 0;
      for (from_loc = 0; from_loc < len; from_loc++)
	{
	  if (from_loc < len - 1 && rhs_parts[from_loc].is_action()
	      && rhs_parts[from_loc+1].is_action())
	    {
	      rhs_parts[from_loc+1] = new action_part(
		  ((action_part)rhs_parts[from_loc]).code_string()+
		  ((action_part)rhs_parts[from_loc+1]).code_string());
	    }
	  else
	    rhs_parts[to_loc++] = rhs_parts[from_loc];
	}

      /* return the used length */
      return to_loc;
    }

  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /**
   * Helper routine to strip a trailing action off rhs and return it
   * 
   * @param rhs_parts
   *                array of RHS parts.
   * @param len
   *                how many of those are valid.
   * @return the removed action part.
   */
  protected action_part strip_trailing_action(production_part rhs_parts[],
      int len)
    {
      action_part result;

      /* bail out early if we have nothing to do */
      if (rhs_parts == null || len == 0)
	return null;

      /* see if we have a trailing action */
      if (rhs_parts[len - 1].is_action())
	{
	  /* snip it out and return it */
	  result = (action_part) rhs_parts[len - 1];
	  rhs_parts[len - 1] = null;
	  return result;
	} else
	return null;
    }

  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /**
   * Remove all embedded actions from a production by factoring them out into
   * individual action production using new non terminals. if the original
   * production was:
   * 
   * <pre>
   *    A ::= B {action1} C {action2} D 
   * </pre>
   * 
   * then it will be factored into:
   * 
   * <pre>
   *    A ::= B NT$1 C NT$2 D
   *    NT$1 ::= {action1}
   *    NT$2 ::= {action2}
   * </pre>
   * 
   * where NT$1 and NT$2 are new system created non terminals.
   */

  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /**
   * Check to see if the production (now) appears to be nullable. A production
   * is nullable if its RHS could derive the empty string. This results when the
   * RHS is empty or contains only non terminals which themselves are nullable.
   */
  public boolean check_nullable()
    {
      production_part part;
      symbol sym;
      int pos;

      /* if we already know bail out early */
      if (nullable_known())
	return nullable();

      /* if we have a zero size RHS we are directly nullable */
      if (rhs_length() == 0)
	{
	  /* stash and return the result */
	  return set_nullable(true);
	}

      /* otherwise we need to test all of our parts */
      for (pos = 0; pos < rhs_length(); pos++)
	{
	  part = rhs(pos);

	  /* only look at non-actions */
	  if (!part.is_action())
	    {
	      sym = ((symbol_part) part).the_symbol();

	      /* if its a terminal we are definitely not nullable */
	      if (!sym.is_non_term())
		return set_nullable(false);
	      /* its a non-term, is it marked nullable */
	      else if (!((non_terminal) sym).nullable())
		/* this one not (yet) nullable, so we aren't */
		return false;
	    }
	}

      /* if we make it here all parts are nullable */
      return set_nullable(true);
    }

  /** set (and return) nullability */
  boolean set_nullable(boolean v)
    {
      _nullable_known = true;
      _nullable = v;
      return v;
    }

  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /**
   * Update (and return) the first set based on current NT firsts. This assumes
   * that nullability has already been computed for all non terminals and
   * productions.
   */
  public terminal_set check_first_set()
    {
      int part;
      symbol sym;

      /* walk down the right hand side till we get past all nullables */
      for (part = 0; part < rhs_length(); part++)
	{
	  /* only look at non-actions */
	  if (!rhs(part).is_action())
	    {
	      sym = ((symbol_part) rhs(part)).the_symbol();

	      /* is it a non-terminal? */
	      if (sym.is_non_term())
		{
		  /* add in current firsts from that NT */
		  _first_set.add(((non_terminal) sym).first_set());

		  /* if its not nullable, we are done */
		  if (!((non_terminal) sym).nullable())
		    break;
		} else
		{
		  /* its a terminal -- add that to the set */
		  _first_set.add((terminal) sym);

		  /* we are done */
		  break;
		}
	    }
	}

      /* return our updated first set */
      return first_set();
    }

  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /** Equality comparison. */
  public boolean equals(production other)
    {
      if (other == null)
	return false;
      return other._index == _index;
    }

  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /** Generic equality comparison. */
  public boolean equals(Object other)
    {
      if (!(other instanceof production))
	return false;
      else
	return equals((production) other);
    }

  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /** Produce a hash code. */
  public int hashCode()
    {
      /* just use a simple function of the index */
      return _index * 13;
    }

  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /** Convert to a simpler string. */
  public String toString()
    {
      StringBuilder result = new StringBuilder();

      result.append((lhs() != null) ? lhs().the_symbol().name() : "NULL_LHS");
      result.append(" ::= ");
      for (int i = 0; i < rhs_length(); i++)
	if (!rhs(i).is_action())
	  result.append(((symbol_part) rhs(i)).the_symbol().name()).append(" ");

      return result.toString();
    }

  /*-----------------------------------------------------------*/

}
