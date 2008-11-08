
package java_cup;

/** A specialized version of a production used when we split an existing
 *  production in order to remove an embedded action.  Here we keep a bit 
 *  of extra bookkeeping so that we know where we came from.
 * @version last updated: 11/25/95
 * @author  Scott Hudson
 */

public class action_production extends production {

  /** Constructor.
   * @param base       the production we are being factored out of.
   * @param lhs_sym    the LHS symbol for this production.
   * @param rhs_parts  array of production parts for the RHS.
   * @param rhs_len    how much of the rhs_parts array is valid.
   * @param action_str the trailing reduce action for this production.
   * @param indexOfIntermediateResult the index of the result of the previous intermediate action on the stack relative to top, -1 if no previous action
   */ 
  public action_production(
    production      base,
    non_terminal    lhs_sym, 
    production_part action,
    int             indexOfAction,
    int             indexOfIntermediateResult)
    {
      super(lhs_sym, new production_part[] { action }, 1);
      _base_production = base;
      this.indexOfAction = indexOfAction;
      this.indexOfIntermediateResult = indexOfIntermediateResult;
    }
  
  private int indexOfAction;
  
  public int rhs_params() {
    return indexOfAction;
  }
  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** The production we were taken out of. */
  private production _base_production;

  /** The production we were taken out of. */
  public production base_production() {return _base_production;}
}
