package fr.umlv.jmmf.matcher;

/** Exception thrown if no matching method could be called with
    the given parameter types.

    @author Remi Forax
    @version 0.6.1
 */
public class NoMatchingMethodException
  extends MatchingMethodException
{
  /** construct a NoMatchingMethodException with a message.
   */
  public NoMatchingMethodException(String message)
  { super(message); }

  /** construct a NoMatchingMethodException with an exception.
   */
  public NoMatchingMethodException(Exception e)
  { super(e); }
}
