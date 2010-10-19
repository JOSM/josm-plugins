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
import it.stefanochizzolini.clown.objects.PdfBoolean;
import it.stefanochizzolini.clown.objects.PdfDirectObject;
import it.stefanochizzolini.clown.objects.PdfIndirectObject;
import it.stefanochizzolini.clown.objects.PdfName;
import it.stefanochizzolini.clown.util.NotImplementedException;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
  Text annotation [PDF:1.6:8.4.5].
  <p>It represents a sticky note attached to a point in the PDF document.</p>

  @author Stefano Chizzolini (http://www.stefanochizzolini.it)
  @since 0.0.7
  @version 0.0.7
*/
public class Note
  extends Annotation
{
  // <class>
  // <classes>
  /**
    Icon to be used in displaying the annotation [PDF:1.6:8.4.5].
  */
  public enum IconTypeEnum
  {
    // <class>
    // <static>
    // <fields>
    /**
      Comment.
    */
    Comment(PdfName.Comment),
    /**
      Help.
    */
    Help(PdfName.Help),
    /**
      Insert.
    */
    Insert(PdfName.Insert),
    /**
      Key.
    */
    Key(PdfName.Key),
    /**
      New paragraph.
    */
    NewParagraph(PdfName.NewParagraph),
    /**
      Note.
    */
    Note(PdfName.Note),
    /**
      Paragraph.
    */
    Paragraph(PdfName.Paragraph);
    // </fields>

    // <interface>
    // <public>
    /**
      Gets the highlighting mode corresponding to the given value.
    */
    public static IconTypeEnum get(
      PdfName value
      )
    {
      for(IconTypeEnum iconType : IconTypeEnum.values())
      {
        if(iconType.getCode().equals(value))
          return iconType;
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
    private IconTypeEnum(
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
  public Note(
    Page page,
    Point2D location,
    String text
    )
  {
    super(
      page.getDocument(),
      PdfName.Text,
      new Rectangle2D.Double(location.getX(),location.getY(),0,0),
      page
      );

    setText(text);
  }

  public Note(
    PdfDirectObject baseObject,
    PdfIndirectObject container
    )
  {super(baseObject,container);}
  // </constructors>

  // <interface>
  // <public>
  @Override
  public Note clone(
    Document context
    )
  {throw new NotImplementedException();}

  /**
    Gets the icon to be used in displaying the annotation.
  */
  public IconTypeEnum getIconType(
    )
  {
    /*
      NOTE: 'Name' entry may be undefined.
    */
    PdfName nameObject = (PdfName)getBaseDataObject().get(PdfName.Name);
    if(nameObject == null)
      return IconTypeEnum.Note;

    return IconTypeEnum.get(nameObject);
  }

  /**
    Gets whether the annotation should initially be displayed open.
  */
  public boolean isOpen(
    )
  {
    /*
      NOTE: 'Open' entry may be undefined.
    */
    PdfBoolean openObject = (PdfBoolean)getBaseDataObject().get(PdfName.Open);
    if(openObject == null)
      return false;

    return ((Boolean)openObject.getValue()).booleanValue();
  }

//TODO:State and StateModel!!!

  /**
    @see #getIconType()
  */
  public void setIconType(
    IconTypeEnum value
    )
  {getBaseDataObject().put(PdfName.Name, value.getCode());}

  /**
    @see #isOpen()
  */
  public void setOpen(
    boolean value
    )
  {getBaseDataObject().put(PdfName.Open, new PdfBoolean(value));}
  // </public>
  // </interface>
  // </dynamic>
  // </class>
}