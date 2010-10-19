/*
  Copyright 2006 Stefano Chizzolini. http://clown.stefanochizzolini.it

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

import java.util.HashMap;
import java.util.Map;

/**
  Shape to be used at the ends of stroked open subpaths [PDF:1.6:4.3.2].

  @author Stefano Chizzolini (http://www.stefanochizzolini.it)
  @version 0.0.4
*/
public enum LineCapEnum
{
  // <class>
  // <static>
  // <fields>
  /**
    Truncated line cap.
  */
  Butt(0),
  /**
    Rounded line cap.
  */
  Round(1),
  /**
    Squared-off line cap.
  */
  Square(2);

  private static Map<Integer, LineCapEnum> map = new HashMap<Integer, LineCapEnum>();
  // </fields>

  // <constructors>
  static
  {
    for (LineCapEnum value : LineCapEnum.values())
    {map.put(value.getCode(), value);}
  }
  // </constructors>

  // <interface>
  // <public>
  public static LineCapEnum valueOf(
    int code
    )
  {return map.get(code);}
  // </public>
  // </interface>
  // </static>

  // <dynamic>
  // <fields>
  /**
    <h3>Remarks</h3>
    <p>Code MUST be explicitly distinct from the ordinal position of the enum constant
    as they coincide by chance only.</p>
  */
  private final int code;
  // </fields>

  // <constructors>
  private LineCapEnum(
    int code
    )
  {this.code = code;}
  // </constructors>

  // <interface>
  // <public>
  public int getCode(
    )
  {return code;}
  // </public>
  // </interface>
  // </dynamic>
  // </class>
}