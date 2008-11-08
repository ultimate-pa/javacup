
package java_cup;

/** The "core" of an LR item.  This includes a production and the position
 *  of a marker (the "dot") within the production.  Typically item cores 
 *  are written using a production with an embedded "dot" to indicate their 
 *  position.  For example: <pre>
 *     A ::= B * C d E
 *  </pre>
 *  This represents a point in a parse where the parser is trying to match
 *  the given production, and has succeeded in matching everything before the 
 *  "dot" (and hence is expecting to see the symbols after the dot next).  See 
 *  lalr_item, lalr_item_set, and lalr_start for full details on the meaning 
 *  and use of items.
 *
 * @see     java_cup.lalr_state
 * @version last updated: 11/25/95
 * @author  Scott Hudson
*/

public class lr_item {
   
  /*-----------------------------------------------------------*/
  /*--- Constructor(s) ----------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Full constructor.
   * @param prod production this item uses.
   * @param pos  position of the "dot" within the item.
   */
  public lr_item(production prod, int pos)
    {
      production_part part;

      assert prod != null: "Attempt to create an lr_item_core with a null production";

      _the_production = prod;

      assert pos >= 0 && pos <= _the_production.rhs_length():
	  "Attempt to create an lr_item_core with a bad dot position";

      _dot_pos = pos;

      /* compute and cache hash code now */
      _core_hash_cache = 13*_the_production.hashCode() + pos;

      /* cache the symbol after the dot */
      if (_dot_pos < _the_production.rhs_length())
	{
	  part = _the_production.rhs(_dot_pos);
	  if (!part.is_action())
	    _symbol_after_dot = ((symbol_part)part).the_symbol();
	}
    } 

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Constructor for dot at start of right hand side. 
   * @param prod production this item uses.
   */
  public lr_item(production prod)
    {
      this(prod,0);
    }

  /*-----------------------------------------------------------*/
  /*--- (Access to) Instance Variables ------------------------*/
  /*-----------------------------------------------------------*/

  /** The production for the item. */
  private production _the_production;

  /** The production for the item. */
  public production the_production() {return _the_production;}

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** The position of the "dot" -- this indicates the part of the production 
   *  that the marker is before, so 0 indicates a dot at the beginning of 
   *  the RHS.
   */
  private int _dot_pos;
  
  /** The position of the "dot" -- this indicates the part of the production 
   *  that the marker is before, so 0 indicates a dot at the beginning of 
   *  the RHS.
   */
  public int dot_pos() {return _dot_pos;}

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Cache of the hash code. */
  protected int _core_hash_cache;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Cache of symbol after the dot. */
  protected symbol _symbol_after_dot = null;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Is the dot at the end of the production? */
  public boolean dot_at_end() 
    {
       return _dot_pos >= _the_production.rhs_length();
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Return the symbol after the dot.  If there is no symbol after the dot
   *  we return null. */
  public symbol symbol_after_dot()
    {
      /* use the cached symbol */
      return _symbol_after_dot;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Determine if we have a dot before a non terminal, and if so which one 
   *  (return null or the non terminal). 
   */
  public non_terminal dot_before_nt()
    {
      symbol sym;

      /* get the symbol after the dot */
      sym = symbol_after_dot();

      /* if it exists and is a non terminal, return it */
      if (sym != null && sym.is_non_term())
	return (non_terminal)sym;
      else
	return null;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/
  private lr_item _shifted;
  
  /** Produce a new lr_item_core that results from shifting the dot one 
   *  position to the right. 
   */
  public lr_item shift_core()
    {
      if (dot_at_end()) 
	throw new AssertionError(
	  "Attempt to shift past end of an lr_item_core");

      if (_shifted == null)
	_shifted = new lr_item(_the_production, _dot_pos+1);
      return _shifted;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Equality comparison for the core only.  This is separate out because we 
   *  need separate access in a super class. 
   */
  public boolean core_equals(lr_item other)
    {
      return other != null && 
	     _the_production.equals(other._the_production) && 
	     _dot_pos == other._dot_pos;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Equality comparison. */
  public boolean equals(lr_item other) {return core_equals(other);}

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Generic equality comparison. */
  public boolean equals(Object other)
    {
      if (!(other instanceof lr_item))
	return false;
      else
	return equals((lr_item)other);
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Hash code for the core (separated so we keep non overridden version). */
  public int core_hashCode()
    {
      return _core_hash_cache;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Hash code for the item. */
  public int hashCode() 
    {
      return _core_hash_cache;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Return the hash code that object would have provided for us so we have 
   *  a (nearly) unique id for debugging.
   */
  protected int obj_hash()
    {
      return super.hashCode();
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Convert to a string.
   */
  public String toString()
    {
      String result;
      production_part part;

      if (_the_production.lhs() != null && 
	  _the_production.lhs().the_symbol() != null &&
	  _the_production.lhs().the_symbol().name() != null)
	result = _the_production.lhs().the_symbol().name();
      else
	result = "$$NULL$$";

      result += " ::= ";

      for (int i = 0; i<_the_production.rhs_length(); i++)
	{
	  /* do we need the dot before this one? */
	  if (i == _dot_pos)
	    result += "(*) ";
	  
	  /* print the name of the part */
	  if (_the_production.rhs(i) == null)
	    {
	      result += "$$NULL$$ ";
	    }
	  else
	    {
	      part = _the_production.rhs(i);
	      if (part == null)
		result += "$$NULL$$ ";
	      else if (part.is_action())
		result += "{ACTION} ";
	      else if (((symbol_part)part).the_symbol() != null &&
                       ((symbol_part)part).the_symbol().name() != null)
		result += ((symbol_part)part).the_symbol().name() + " ";
	      else
		result += "$$NULL$$ ";
	    }
	}

      /* put the dot after if needed */
      if (_dot_pos == _the_production.rhs_length())
	result += "(*) ";

      return result;
    }

  /*-----------------------------------------------------------*/

  /** Calculate lookahead representing symbols that could appear after the
   *   symbol that the dot is currently in front of.  Note: this routine must
   *   not be invoked before first sets and nullability has been calculated
   *   for all non terminals. 
   */ 
  public terminal_set calc_lookahead() 
    {
      terminal_set    result;
      int             pos;
      production_part part;
      symbol          sym;

      /* start with an empty result */
      result = new terminal_set();

      /* consider all nullable symbols after the one to the right of the dot */
      for (pos = dot_pos(); pos < the_production().rhs_length(); pos++) 
  	{
  	   part = the_production().rhs(pos);

  	   /* consider what kind of production part it is -- skip actions */ 
  	   if (!part.is_action())
  	     {
  	       sym = ((symbol_part)part).the_symbol();

  	       /* if its a terminal add it in and we are done */
  	       if (!sym.is_non_term())
  		 {
  		   result.add((terminal)sym);
  		   break;
  		 }
  	       else
  		 {
  		   /* otherwise add in first set of the non terminal */
  		   result.add(((non_terminal)sym).first_set());

  		   /* if its nullable we continue adding, if not, we are done */
  		   if (!((non_terminal)sym).nullable())
  		     break;
  		 }
  	     }
  	}
      return result;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Determine if everything from the symbol one beyond the dot all the 
   *  way to the  end of the right hand side is nullable.  This would indicate
   *  that the lookahead of this item must be included in the lookaheads of
   *  all items produced as a closure of this item.  Note: this routine should 
   *  not be invoked until after first sets and nullability have been 
   *  calculated for all non terminals. 
   */
  public boolean is_nullable()
    {
      production_part part;
      symbol          sym;

      /* walk down the rhs and bail if we get a non-nullable symbol */
      for (int pos = dot_pos(); pos < the_production().rhs_length(); pos++)
  	{
  	  part = the_production().rhs(pos);

  	  /* skip actions */
  	  if (!part.is_action())
  	    {
  	      sym = ((symbol_part)part).the_symbol();

  	      /* if its a terminal we fail */
  	      if (!sym.is_non_term()) return false;

  	      /* if its not nullable we fail */
  	      if (!((non_terminal)sym).nullable()) return false;
  	    }
  	}

      /* if we get here its all nullable */
      return true;
    }    
}