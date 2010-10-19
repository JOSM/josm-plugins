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
import java.util.EnumSet;

/**
  Widget annotation [PDF:1.6:8.4.5].

  @author Stefano Chizzolini (http://www.stefanochizzolini.it)
  @version 0.0.7
  @since 0.0.7
*/
public class Widget
  extends Annotation
{
  // <class>
  // <classes>
  /**
    Highlighting mode [PDF:1.6:8.4.5].
  */
  public enum HighlightModeEnum
  {
    // <class>
    // <static>
    // <fields>
    /**
      No highlighting.
    */
    None(PdfName.N),
    /**
      Invert the contents of the annotation rectangle.
    */
    Invert(PdfName.I),
    /**
      Invert the annotation's border.
    */
    Outline(PdfName.O),
    /**
      Display the annotation's down appearance.
    */
    Push(PdfName.P),
    /**
      Same as Push (which is preferred).
    */
    Toggle(PdfName.T);
    // </fields>

    // <interface>
    // <public>
    /**
      Gets the highlighting mode corresponding to the given value.
    */
    public static HighlightModeEnum get(
      PdfName value
      )
    {
      for(HighlightModeEnum mode : HighlightModeEnum.values())
      {
        if(mode.getCode().equals(value))
          return mode;
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
    private HighlightModeEnum(
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
  public Widget(
    Page page,
    Rectangle2D box
    )
  {
    super(
      page.getDocument(),
      PdfName.Widget,
      box,
      page
      );

    EnumSet<FlagsEnum> flags = getFlags(); flags.add(FlagsEnum.Print); setFlags(flags);
  }

  public Widget(
    PdfDirectObject baseObject,
    PdfIndirectObject container
    )
  {super(baseObject,container);}
  // </constructors>

  // <interface>
  // <public>
  @Override
  public Widget clone(
    Document context
    )
  {throw new NotImplementedException();}

  @Override
  public AnnotationActions getActions(
    )
  {
    PdfDirectObject actionsObject = getBaseDataObject().get(PdfName.AA);
    if(actionsObject == null)
      return null;

    return new WidgetActions(this,actionsObject,getContainer());
  }

  /**
    Gets the annotation's appearance characteristics
    to be used for its visual presentation on the page.
  */
  public AppearanceCharacteristics getAppearanceCharacteristics(
    )
  {
    /*
      NOTE: 'MK' entry may be undefined.
    */
    PdfDirectObject appearanceObject = getBaseDataObject().get(PdfName.MK);
    if(appearanceObject == null)
      return null;

    return new AppearanceCharacteristics(appearanceObject,getContainer());
  }

  /**
    Gets the annotation's highlighting mode, the visual effect to be used
    when the mouse button is pressed or held down inside its active area.
  */
  public HighlightModeEnum getHighlightMode(
    )
  {
    /*
      NOTE: 'H' entry may be undefined.
    */
    PdfName highlightModeObject = (PdfName)getBaseDataObject().get(PdfName.H);
    if(highlightModeObject == null)
      return HighlightModeEnum.Invert;

    return HighlightModeEnum.get(highlightModeObject);
  }

  /**
    Sets the annotation's appearance characteristics.

    @see #getAppearanceCharacteristics()
  */
  public void setAppearanceCharacteristics(
    AppearanceCharacteristics value
    )
  {getBaseDataObject().put(PdfName.MK, value.getBaseObject());}

  /**
    Sets the annotation's highlighting mode.

    @see #getHighlightMode()
  */
  public void setHighlightMode(
    HighlightModeEnum value
    )
  {getBaseDataObject().put(PdfName.H, value.getCode());}
  // </public>
  // </interface>
  // </dynamic>
  // </class>
}