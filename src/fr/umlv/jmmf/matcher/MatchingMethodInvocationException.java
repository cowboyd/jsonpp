package fr.umlv.jmmf.matcher;

/** This class is the same of InvocationTargetException but
    extends RuntimeException.

    @author Remi Forax
    @version 0.6.1
 */
public class MatchingMethodInvocationException
  extends MatchingMethodException
{
  /** construct a MatchingMethodInvocationException.
   */
  public MatchingMethodInvocationException(Exception e)
  { super(e); }
}
