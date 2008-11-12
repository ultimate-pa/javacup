
package java_cup;

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
  public parse_action_row()
    {
      int num_terminals = terminal.number();
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
      /* allocate the count table */
      int[] reduction_count = new int[production.number()];

      /* clear the reduction count table and maximums */
      int max_prod = -1;
     
      /* walk down the row and look at the reduces */
      for (int i = 0; i < under_term.length; i++)
	if (under_term[i].kind() == parse_action.REDUCE)
	  {
	    /* count the reduce in the proper production slot and keep the 
	       max up to date */
	    int prod = ((reduce_action)under_term[i]).reduce_with().index();
	    reduction_count[prod]++;
	    if (max_prod < 0 || reduction_count[prod] > reduction_count[max_prod])
	      max_prod = prod;
	  }

       /* record the max as the default (or -1 for not found) */
       default_reduce = max_prod;
    }

  /*-----------------------------------------------------------*/

}

