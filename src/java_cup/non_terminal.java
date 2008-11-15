package java_cup;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/** This class represents a non-terminal symbol in the grammar.  Each
 *  non terminal has a textual name, an index, and a string which indicates
 *  the type of object it will be implemented with at runtime (i.e. the class
 *  of object that will be pushed on the parse stack to represent it). 
 *
 * @version last updated: 11/25/95
 * @author  Scott Hudson
 */

public class non_terminal extends symbol {

  /*-----------------------------------------------------------*/
  /*--- Constructor(s) ----------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Full constructor.
   * @param nm  the name of the non terminal.
   * @param tp  the type string for the non terminal.
   */
  public non_terminal(String nm, String tp, int index) 
    {
      /* super class does most of the work */
      super(nm, tp);
      _index = index;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Constructor with default type. 
   * @param nm  the name of the non terminal.
   */
  public non_terminal(String nm, int index) 
    {
      this(nm, null, index);
    }

  /*-----------------------------------------------------------*/
  /*--- (Access to) Static (Class) Variables ------------------*/
  /*-----------------------------------------------------------*/

  /** Table of all non-terminals -- elements are stored using name strings 
   *  as the key 
   */
  protected static HashMap<String, non_terminal> _all = new HashMap<String, non_terminal>();

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** special non-terminal for start symbol */
  public static final non_terminal START_nt = new non_terminal("$START", "Object", 0);

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** flag non-terminals created to embed action productions */
  public boolean is_embedded_action = false; /* added 24-Mar-1998, CSA */

  /*-----------------------------------------------------------*/
  /*--- (Access to) Instance Variables ------------------------*/
  /*-----------------------------------------------------------*/

  /** Table of all productions with this non terminal on the LHS. */
  protected Set<production> _productions = new HashSet<production>();

  /** Access to productions with this non terminal on the LHS. */
  public Set<production> productions() {return _productions;}

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Total number of productions with this non terminal on the LHS. */
  public int num_productions() {return _productions.size();}

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Add a production to our set of productions. */
  public void add_production(production prod)
    {
      /* catch improper productions */
      assert (prod != null && prod.lhs() == this) :
	  "Attempt to add invalid production to non terminal production table";

      /* add it to the table, keyed with itself */
      _productions.add(prod);
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Nullability of this non terminal. */
  protected boolean _nullable;

  /** Nullability of this non terminal. */
  public boolean nullable() {return _nullable;}

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** First set for this non-terminal. */
  protected terminal_set _first_set;

  /** First set for this non-terminal. */
  public terminal_set first_set() {return _first_set;}

  /*-----------------------------------------------------------*/
  /*--- General Methods ---------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Indicate that this symbol is a non-terminal. */
  public boolean is_non_term() 
    {
      return true;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Test to see if this non terminal currently looks nullable. */
  protected boolean looks_nullable()
    {
      /* look and see if any of the productions now look nullable */
      for (production prod : productions())
	/* if the production can go to empty, we are nullable */
	if (prod.check_nullable())
	  return true;

      /* none of the productions can go to empty, so we are not nullable */
      return false;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** convert to string */
  public String toString()
    {
      return super.toString() + "[" + index() + "]" + (nullable() ? "*" : "");
    }

  /*-----------------------------------------------------------*/
}
