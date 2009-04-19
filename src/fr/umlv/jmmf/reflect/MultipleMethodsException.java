/*
 * Java MultiMethod Framework API 0.8
 *
 * $Id$
 *
 * Copyright (C) 1999-2001 Remi Forax <forax@univ-mlv.fr>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

 package fr.umlv.jmmf.reflect;

/** Exception thrown if multiple matching methods could be called with
    the given parameter types.

    @author Remi Forax
    @version 0.9.0
 */
public class MultipleMethodsException
  extends Exception
{
  /** construct a MultipleMatchingMethodsException with a message.
   */
  public MultipleMethodsException(String message)
  { super(message); }

  /** construct a MultipleMatchingMethodsException without a message.
   */
  public MultipleMethodsException()
  { super(); }
}
