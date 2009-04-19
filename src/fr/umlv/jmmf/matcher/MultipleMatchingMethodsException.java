package fr.umlv.jmmf.matcher;

/** Exception thrown if multiple matching methods could be called with
    the given parameter types.

    @author Remi Forax
    @version 0.6.1
 */
public class MultipleMatchingMethodsException
  extends MatchingMethodException
{
  /** construct a MultipleMatchingMethodsException with a message.
   */
  public MultipleMatchingMethodsException(String message)
  { super(message); }

  /** construct a MultipleMatchingMethodsException with an exception.
   */
  public MultipleMatchingMethodsException(Exception e)
  { super(e); }
}
