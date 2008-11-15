
package java_cup;

import java.util.HashMap;

/** This class represents one row (corresponding to one machine state) of the 
 *  parse action table.
 */
public class parse_action_row {

  /*-----------------------------------------------------------*/
  /*--- Constructor(s) ----------------------------------------*/
  /*-----------------------------------------------------------*/
	       
  /** Simple constructor.  Note: this should not be used until the number of
   *  terminals in the grammar has been established.
   */
  public parse_action_row(Grammar g)
    {
      int num_terminals = g.num_terminals();
      /* allocate the array */
      under_term = new parse_action[num_terminals];

      /* set each element to an error action */
      for (int i=0; i < num_terminals; i++)
	under_term[i] = new parse_action();
    }

  /*-----------------------------------------------------------*/
  /*--- (Access to) Instance Variables ------------------------*/
  /*-----------------------------------------------------------*/

  /** Actual action entries for the row. */
  public parse_action under_term[];

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Default (reduce) action for this row.  -1 will represent default 
   *  of error. 
   */
  public int default_reduce;

  /*-----------------------------------------------------------*/
  /*--- General Methods ---------------------------------------*/
  /*-----------------------------------------------------------*/
	
  /** Compute the default (reduce) action for this row and store it in 
   *  default_reduce.  In the case of non-zero default we will have the 
   *  effect of replacing all errors by that reduction.  This may cause 
   *  us to do erroneous reduces, but will never cause us to shift past 
   *  the point of the error and never cause an incorrect parse.  -1 will 
   *  be used to encode the fact that no reduction can be used as a 
   *  default (in which case error will be used).
   */
  public void compute_default()
    {
      /* Check if there is already an action for the error symbol.
       * This must be the default action.
       */
      parse_action error_action = under_term[terminal.error.index()]; 
      if (error_action.kind() != parse_action.ERROR)
	{
	  if (error_action.kind() == parse_action.REDUCE)
	    default_reduce = 
	      ((reduce_action) error_action).reduce_with().index();
	  else
	    default_reduce = -1;
	  return;
	}
      
      /* allocate the count table */
      HashMap<production, Integer> reduction_count
      	= new HashMap<production, Integer>();

      /* clear the reduction count table and maximums */
      int max_prod = -1;
      int max_count = 0;
     
      /* walk down the row and look at the reduces */
      for (int i = 0; i < under_term.length; i++)
	if (under_term[i].kind() == parse_action.REDUCE)
	  {
	    /* count the reduce in the proper production slot and keep the 
	       max up to date */
	    production prod = ((reduce_action)under_term[i]).reduce_with();
	    Integer oldcount = reduction_count.get(prod);
	    int count = oldcount == null ? 1 : oldcount+1; 
	    reduction_count.put(prod, count);
	    if (count > max_count)
	      {
		max_prod = prod.index();
		max_count = count;
	      }
	  }

       /* record the max as the default (or -1 for not found) */
       default_reduce = max_prod;
    }

  /*-----------------------------------------------------------*/

}
