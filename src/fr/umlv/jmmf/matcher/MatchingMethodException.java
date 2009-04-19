package fr.umlv.jmmf.matcher;

import java.io.PrintStream;
import java.io.PrintWriter;

/** This class is the root class for all matching exceptions.

    @author Remi Forax
    @version 0.6.1
 */
public abstract class MatchingMethodException
  extends RuntimeException
{
  /** construct a MatchingMethodException with an exception
      raised during the matching algorithm.
   */
  protected MatchingMethodException(Exception e)
  { this.e=e; }

  /** construct a MatchingMethodException with a detail message.
   */
  protected MatchingMethodException(String message)
  { super(message); }

  /** get the exception thrown in the matching method.
   */
  public Exception getTargetException()
  { return e; }

  /** prints the stack trace of the thrown target exception.
   */
  public void printStackTrace()
  { printStackTrace(System.err); }

  /** prints the stack trace of the thrown target exception to the
      specified print stream.
   */
  public void printStackTrace(PrintStream ps)
  {
    if (e!=null)
    {
      synchronized (ps)
      {
        ps.print(getClass().getName()+": ");
        e.printStackTrace(ps);
      }
    }
    else
      super.printStackTrace(ps);
  }

  /** prints the stack trace of the thrown target exception to the
      specified print writer.
   */
  public void printStackTrace(PrintWriter pw)
  {
    if (e!=null)
    {
      synchronized (pw)
      {
        pw.print(getClass().getName()+": ");
        e.printStackTrace(pw);
      } 
    }
    else
      super.printStackTrace(pw);
  }

  private Exception e;
}
