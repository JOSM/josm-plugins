/*
  Copyright 2010 Stefano Chizzolini. http://clown.stefanochizzolini.it

  Contributors:
    * Stefano Chizzolini (original code developer, http://www.stefanochizzolini.it)

  This file should be part of the source code distribution of "PDF Clown library"
  (the Program): see the accompanying README files for more info.

  This Program is free software; you can redistribute it and/or modify it under the terms
  of the GNU Lesser General Public License as published by the Free Software Foundation;
  either version 3 of the License, or (at your option) any later version.

  This Program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY,
  either expressed or implied; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE. See the License for more details.

  You should have received a copy of the GNU Lesser General Public License along with this
  Program (see README files); if not, go to the GNU website (http://www.gnu.org/licenses/).

  Redistribution and use, with or without modification, are permitted provided that such
  redistributions retain the above copyright notice, license and disclaimer, along with
  this list of conditions.
*/

package it.stefanochizzolini.clown.documents.contents;

import java.awt.geom.Rectangle2D;

/**
  Text character.
  <h3>Remarks</h3>
  <p>Its purpose is to describe a text element extracted from content streams.</p>

  @author Stefano Chizzolini (http://www.stefanochizzolini.it)
  @since 0.0.8
  @version 0.0.8
*/
public final class TextChar
{
  // <class>
  // <dynamic>
  // <fields>
  public final Rectangle2D box;
  public final TextStyle style;
  public final char value;
  public final boolean virtual;
  // </fields>

  // <constructors>
  public TextChar(
    char value,
    Rectangle2D box,
    TextStyle style,
    boolean virtual
    )
  {
    this.value = value;
    this.box = box;
    this.style = style;
    this.virtual = virtual;
  }
  // </constructors>

  // <interface>
  // <public>
  @Override
  public String toString(
    )
  {return Character.toString(value);}
  // </public>
  // </interface>
  // </dynamic>
  // </class>
}