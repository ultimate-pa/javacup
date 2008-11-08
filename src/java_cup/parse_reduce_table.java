
package java_cup;

/** This class represents the complete "reduce-goto" table of the parser.
 *  It has one row for each state in the parse machines, and a column for
 *  each terminal symbol.  Each entry contains a state number to shift to
 *  as the last step of a reduce. 
 *
 * @see     java_cup.parse_reduce_row
 * @version last updated: 11/25/95
 * @author  Scott Hudson
 */
public class parse_reduce_table {
 
  /*-----------------------------------------------------------*/
  /*--- Constructor(s) ----------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Simple constructor.  Note: all terminals, non-terminals, and productions 
   *  must already have been entered, and the viable prefix recognizer should
   *  have been constructed before this is called.
   */
  public parse_reduce_table()
    {
      /* determine how many states we are working with */
      _num_states = lalr_state.number();

      /* allocate the array and fill it in with empty rows */
      under_state = new parse_reduce_row[_num_states];
      for (int i=0; i<_num_states; i++)
	under_state[i] = new parse_reduce_row();
    }

   
  /*-----------------------------------------------------------*/
  /*--- (Access to) Instance Variables ------------------------*/
  /*-----------------------------------------------------------*/

  /** How many rows/states in the machine/table. */
  protected int _num_states;

  /** How many rows/states in the machine/table. */
  public int num_states() {return _num_states;}

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Actual array of rows, one per state */
  public  parse_reduce_row[] under_state;
 
  /*-----------------------------------------------------------*/
  /*--- General Methods ---------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Convert to a string. */
  public String toString()
    {
      StringBuilder result = new StringBuilder();
      lalr_state goto_st;
      int cnt;

      result.append("-------- REDUCE_TABLE --------\n");
      for (int row = 0; row < num_states(); row++)
	{
	  result.append("From state #").append(row).append("\n");
	  cnt = 0;
	  for (int col = 0; col < non_terminal.number(); col++)
	    {
	      /* pull out the table entry */
	      goto_st = under_state[row].under_non_term[col];

	      /* if it has action in it, print it */
	      if (goto_st != null)
		{
		  result.append(" [non term ").append(col).append("->"); 
		  result.append("state ").append(goto_st.index()).append("]");

		  /* end the line after the 3rd one */
		  cnt++;
		  if (cnt == 3)
		    {
		      result.append("\n");
		      cnt = 0;
		    }
		}
	    }
          /* finish the line if we haven't just done that */
	  if (cnt != 0) result.append("\n");
	}
      result.append("-----------------------------");

      return result.toString();
    }

  /*-----------------------------------------------------------*/

}

