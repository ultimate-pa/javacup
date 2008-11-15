package java_cup;

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
  public production(int index, non_terminal lhs_sym, symbol_part rhs[], action_part action, terminal precedence)
    {
      int last_act_loc = -1;
      if (precedence != null)
	{
	  _rhs_prec = precedence.precedence_num();
	  _rhs_assoc = precedence.precedence_side();
	}
      _lhs = lhs_sym;
      _rhs = rhs;
      _action = action;
      _index = index;
      for (int i = 0; i < rhs.length; i++)
	{
	  symbol rhs_sym = rhs[i].the_symbol();
	  if (rhs_sym.is_non_term() &&
	      ((non_terminal)rhs_sym).is_embedded_action)
	    {
	      last_act_loc = i;
	    }
	  else
	    {
	      rhs_sym.note_use();
	      if (precedence == null && rhs_sym instanceof terminal)
		{
		  terminal term = (terminal) rhs_sym;
		  if (term.precedence_num() != assoc.no_prec)
		    {
		      if (_rhs_prec == assoc.no_prec)
			{
			  _rhs_prec = term.precedence_num();
			  _rhs_assoc = term.precedence_side();
			}
		      else if (term.precedence_num() != _rhs_prec)
			{
			  ErrorManager.getManager().emit_error("Production "+this+
			      " has more than one precedence symbol");
			}
		    }
		}
	    }
	}
      indexOfIntermediateResult = last_act_loc;
      /* put us in the production list of the lhs non terminal */
      lhs_sym.add_production(this);
    }

  /*-----------------------------------------------------------*/
  /*--- (Access to) Instance Variables ------------------------*/
  /*-----------------------------------------------------------*/

  /** The left hand side non-terminal. */
  protected symbol _lhs;

  /** The left hand side non-terminal. */
  public symbol lhs()
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
      return _rhs[indx];
    }

  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /** How much of the right hand side array we are presently using. */
  public int rhs_length()
    {
      return _rhs.length;
    }

  public int rhs_stackdepth() 
    {
      return _rhs.length;
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
   * Return the first set based on current NT firsts. This assumes
   * that nullability has already been computed for all non terminals and
   * productions.
   */
  public terminal_set first_set(Grammar grammar)
    {
      int part;
      symbol sym;
      terminal_set first_set = new terminal_set(grammar);

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
		  first_set.add(((non_terminal) sym).first_set());

		  /* if its not nullable, we are done */
		  if (!((non_terminal) sym).nullable())
		    break;
		} else
		{
		  /* its a terminal -- add that to the set */
		  first_set.add((terminal) sym);

		  /* we are done */
		  break;
		}
	    }
	}

      /* return our updated first set */
      return first_set;
    }

  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /** Produce a hash code. */
  public int hashCode()
    {
      /* just use a simple function of the index */
      return _index;
    }

  /* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

  /** Convert to a simpler string. */
  public String toString()
    {
      StringBuilder result = new StringBuilder();

      result.append((lhs() != null) ? lhs().name() : "NULL_LHS");
      result.append(" ::= ");
      for (int i = 0; i < rhs_length(); i++)
	if (!rhs(i).is_action())
	  result.append(((symbol_part) rhs(i)).the_symbol().name()).append(" ");

      return result.toString();
    }

  /*-----------------------------------------------------------*/

}
