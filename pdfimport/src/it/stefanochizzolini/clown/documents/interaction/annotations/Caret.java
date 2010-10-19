/*
  Copyright 2008 Stefano Chizzolini. http://clown.stefanochizzolini.it

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

package it.stefanochizzolini.clown.documents.interaction.annotations;

import it.stefanochizzolini.clown.documents.Document;
import it.stefanochizzolini.clown.documents.Page;
import it.stefanochizzolini.clown.objects.PdfDirectObject;
import it.stefanochizzolini.clown.objects.PdfIndirectObject;
import it.stefanochizzolini.clown.objects.PdfName;
import it.stefanochizzolini.clown.util.NotImplementedException;

import java.awt.geom.Rectangle2D;

/**
  Caret annotation [PDF:1.6:8.4.5].
  <p>It displays a visual symbol that indicates the presence of text edits.</p>

  @author Stefano Chizzolini (http://www.stefanochizzolini.it)
  @since 0.0.7
  @version 0.0.7
*/
public class Caret
  extends Annotation
{
  // <class>
  // <classes>
  /**
    Symbol type [PDF:1.6:8.4.5].
  */
  public enum SymbolTypeEnum
  {
    // <class>
    // <static>
    // <fields>
    /**
      New paragraph.
    */
    NewParagraph(PdfName.P),
    /**
      None.
    */
    None(PdfName.None);
    // </fields>

    // <interface>
    // <public>
    /**
      Gets the symbol type corresponding to the given value.
    */
    public static SymbolTypeEnum get(
      PdfName value
      )
    {
      for(SymbolTypeEnum symbolType : SymbolTypeEnum.values())
      {
        if(symbolType.getCode().equals(value))
          return symbolType;
      }
      return null;
    }
    // </public>
    // </interface>
    // </static>

    // <dynamic>
    // <fields>
    private final PdfName code;
    // </fields>

    // <constructors>
    private SymbolTypeEnum(
      PdfName code
      )
    {this.code = code;}
    // </constructors>

    // <interface>
    // <public>
    public PdfName getCode(
      )
    {return code;}
    // </public>
    // </interface>
    // </dynamic>
    // </class>
  }
  // </classes>

  // <dynamic>
  // <constructors>
  public Caret(
    Page page,
    Rectangle2D box
    )
  {
    super(
      page.getDocument(),
      PdfName.Caret,
      box,
      page
      );
  }

  public Caret(
    PdfDirectObject baseObject,
    PdfIndirectObject container
    )
  {super(baseObject,container);}
  // </constructors>

  // <interface>
  // <public>
  @Override
  public Caret clone(
    Document context
    )
  {throw new NotImplementedException();}

  /**
    Gets the symbol to be used in displaying the annotation.
  */
  public SymbolTypeEnum getSymbolType(
    )
  {
    /*
      NOTE: 'Sy' entry may be undefined.
    */
    PdfName symbolObject = (PdfName)getBaseDataObject().get(PdfName.Sy);
    if(symbolObject == null)
      return SymbolTypeEnum.None;

    return SymbolTypeEnum.get(symbolObject);
  }

  /**
    @see #getSymbolType()
  */
  public void setSymbolType(
    SymbolTypeEnum value
    )
  {getBaseDataObject().put(PdfName.Sy, value.getCode());}
  // </public>
  // </interface>
  // </dynamic>
  // </class>
}