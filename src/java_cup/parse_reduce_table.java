
package java_cup;

import java.util.BitSet;

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
 
  /** Actual parse_reduce matrix, indexed by state and non-terminal. */
  public  lalr_state[][] table;
 
  /*-----------------------------------------------------------*/
  /*--- Constructor(s) ----------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Simple constructor.  Note: all terminals, non-terminals, and productions 
   *  must already have been entered, and the viable prefix recognizer should
   *  have been constructed before this is called.
   */
  public parse_reduce_table(Grammar grammar)
    {
      /* determine how many states we are working with */
      _num_states = grammar.lalr_states().size();
      _num_nonterm = grammar.num_non_terminals();

      /* allocate the array and fill it in with empty rows */
      table = new lalr_state[_num_states][_num_nonterm];
    }

   
  /*-----------------------------------------------------------*/
  /*--- (Access to) Instance Variables ------------------------*/
  /*-----------------------------------------------------------*/

  /** How many rows/states in the machine/table. */
  protected int _num_states;
  private int _num_nonterm;

  /** How many rows/states in the machine/table. */
  public int num_states() {return _num_states;}

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /*-----------------------------------------------------------*/
  /*--- General Methods ---------------------------------------*/
  /*-----------------------------------------------------------*/

  public short[] compress()
    {
      int[] baseaddrs = new int[_num_states];
      int[] rowidx = new int[_num_nonterm];
      int maxbase = 0;
      BitSet used = new BitSet();
      for (int i = 0; i < _num_states; i++)
	{
	  lalr_state[] row = table[i];
	  int rowcnt = 0;
	  for (int j = 0; j < _num_nonterm; j++)
	    {
	      if (row[j] != null)
		rowidx[rowcnt++] = j;
	    }

	next_base:
	  for (int base = 0; true; base++)
	    {
	      if (_num_states+base > Short.MAX_VALUE)
		{
		  throw new AssertionError("Reduce table overflow!");
		}
	      for (int j = 0; j < rowcnt; j++)
		{
		  if (used.get(base+rowidx[j]))
		    continue next_base;
		}
	      for (int j = 0; j < rowcnt; j++)
		{
		  used.set(base+rowidx[j]);
		  if (base+rowidx[j] >= maxbase)
		    maxbase = base+rowidx[j]+1;
		}

	      baseaddrs[i] = base;
	      break;
	    }
	}
      int minbase = 0;
      while (!used.get(minbase))
	minbase++;
      
      short[] compressed = new short[_num_states + maxbase - minbase];
      for (int i = 0; i < _num_states; i++)
	{
	  lalr_state[] row = table[i];
	  int base = _num_states + baseaddrs[i] - minbase;
	  compressed[i] = (short) base;
	  for (int j = 0; j < _num_nonterm; j++)
	    {
	      lalr_state st = row[j];
	      if (st != null)
		compressed[base+j] = (short) st.index();
	    }
	}
      return compressed;
    }

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
	  for (int col = 0; col < _num_nonterm; col++)
	    {
	      /* pull out the table entry */
	      goto_st = table[row][col];

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

