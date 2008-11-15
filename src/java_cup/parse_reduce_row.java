
package java_cup;

/** This class represents one row (corresponding to one machine state) of the 
 *  reduce-goto parse table. 
 */
public class parse_reduce_row {
  /*-----------------------------------------------------------*/
  /*--- Constructor(s) ----------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Simple constructor. Note: this should not be used until the number
   *  of terminals in the grammar has been established.
   */
  public parse_reduce_row(Grammar g)
    {
      /* allocate the array */
      under_non_term = new lalr_state[g.num_non_terminals()];
    }

  /*-----------------------------------------------------------*/
  /*--- (Access to) Instance Variables ------------------------*/
  /*-----------------------------------------------------------*/

  /** Actual entries for the row. */
  public lalr_state under_non_term[];
}

