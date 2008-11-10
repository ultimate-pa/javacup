package java_cup;

import java_cup.assoc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/** This class represents a terminal symbol in the grammar.  Each terminal 
 *  has a textual name, an index, and a string which indicates the type of 
 *  object it will be implemented with at runtime (i.e. the class of object 
 *  that will be returned by the scanner and pushed on the parse stack to 
 *  represent it). 
 *
 * @version last updated: 7/3/96
 * @author  Frank Flannery
 */
public class terminal extends symbol {

  /*-----------------------------------------------------------*/
  /*--- Constructor(s) ----------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Full constructor.
   * @param nm the name of the terminal.
   * @param tp the type of the terminal.
   */
  public terminal(String nm, String tp, int precedence_side, int precedence_num) 
    {
      /* superclass does most of the work */
      super(nm, tp);

      /* set the precedence */
      _precedence_num = precedence_num;
      _precedence_side = precedence_side;

      /* add to by_index set */
      _index = _all_by_index.size();
      _all_by_index.add(this);
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Constructor for non-precedented terminal
    */ 

  public terminal(String nm, String tp) 
    {
      this(nm, tp, assoc.no_prec, -1);
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Constructor with default type. 
   * @param nm the name of the terminal.
   */
  public terminal(String nm) 
    {
      this(nm, null);
    }

  /*-----------------------------------------------------------*/
  /*-------------------  Class Variables  ---------------------*/
  /*-----------------------------------------------------------*/

  private int _precedence_num;
  private int _precedence_side;

  /*-----------------------------------------------------------*/
  /*--- (Access to) Static (Class) Variables ------------------*/
  /*-----------------------------------------------------------*/

  //Hm Added clear  to clear all static fields
  public static void clear() {
      _all_by_index.clear();
      EOF = new terminal("EOF");
      error = new terminal ("error");
  }
  
  /** Access to all terminals. */
  public static Collection<terminal> all() {return _all_by_index;}

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Table of all terminals indexed by their index number. */
  protected static ArrayList<terminal> _all_by_index = new ArrayList<terminal>();

  /** Lookup a terminal by index. */
  public static terminal find(int indx)
    {
      return _all_by_index.get(indx);
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Total number of terminals. */
  public static int number() {return _all_by_index.size();}

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Special terminal for end of input. */
  public static terminal EOF = new terminal("EOF");

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** special terminal used for error recovery */
  public static terminal error = new terminal("error");

  /*-----------------------------------------------------------*/
  /*--- General Methods ---------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Report this symbol as not being a non-terminal. */
  public boolean is_non_term() 
    {
      return false;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Convert to a string. */
  public String toString()
    {
      return super.toString() + "[" + index() + "]";
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** get the precedence of a terminal */
  public int precedence_num() {
    return _precedence_num;
  }
  public int precedence_side() {
    return _precedence_side;
  }

  /** set the precedence of a terminal */
  public void set_precedence(int p, int new_prec) {
    _precedence_side = p;
    _precedence_num = new_prec;
  }

  /*-----------------------------------------------------------*/

}
