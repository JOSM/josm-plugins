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

package it.stefanochizzolini.clown.documents.interaction.forms;

import it.stefanochizzolini.clown.documents.Document;
import it.stefanochizzolini.clown.objects.PdfArray;
import it.stefanochizzolini.clown.objects.PdfDirectObject;
import it.stefanochizzolini.clown.objects.PdfIndirectObject;
import it.stefanochizzolini.clown.objects.PdfObjectWrapper;
import it.stefanochizzolini.clown.objects.PdfTextString;
import it.stefanochizzolini.clown.util.NotImplementedException;

/**
  Field option [PDF:1.6:8.6.3].

  @author Stefano Chizzolini (http://www.stefanochizzolini.it)
  @since 0.0.7
  @version 0.0.7
*/
public class ChoiceItem
  extends PdfObjectWrapper<PdfDirectObject>
{
  // <fields>
  private ChoiceItems items;
  // </fields>

  // <constructors>
  public ChoiceItem(
    String value
    )
  {super(new PdfTextString(value),null);}

  public ChoiceItem(
    Document context,
    String value,
    String text
    )
  {
    super(
      context.getFile(),
      new PdfArray(
        new PdfDirectObject[]
        {
          new PdfTextString(value),
          new PdfTextString(text)
        }
        )
      );
  }

  ChoiceItem(
    PdfDirectObject baseObject,
    PdfIndirectObject container,
    ChoiceItems items
    )
  {
    super(baseObject,container);

    setItems(items);
  }
  // </constructors>

  // <interface>
  // <public>
  @Override
  public ChoiceItem clone(
    Document context
    )
  {throw new NotImplementedException();}

  /**
    Gets the displayed text.
  */
  public String getText()
  {
    PdfDirectObject baseDataObject = getBaseDataObject();
    if(baseDataObject instanceof PdfArray) // <value,text> pair.
      return (String)((PdfTextString)((PdfArray)baseDataObject).get(1)).getValue();
    else // Single text string.
      return (String)((PdfTextString)baseDataObject).getValue();
  }

  /**
    Gets the export value.
  */
  public String getValue()
  {
    PdfDirectObject baseDataObject = getBaseDataObject();
    if(baseDataObject instanceof PdfArray) // <value,text> pair.
      return (String)((PdfTextString)((PdfArray)baseDataObject).get(0)).getValue();
    else // Single text string.
      return (String)((PdfTextString)baseDataObject).getValue();
  }

  //TODO:make the class immutable (to avoid needing wiring it up to its collection...)!!!
  /**
    @see #getText()
  */
  public void setText(
    String value
    )
  {
    PdfDirectObject baseDataObject = getBaseDataObject();
    if(baseDataObject instanceof PdfTextString)
    {
      PdfDirectObject oldBaseDataObject = baseDataObject;

      setBaseObject(
        baseDataObject = new PdfArray(
          new PdfDirectObject[]
          {
            oldBaseDataObject
          }
          )
        );
      ((PdfArray)baseDataObject).add(new PdfTextString());

      if(items != null)
      {
        // Force list update!
        /*
          NOTE: This operation is necessary in order to substitute
          the previous base object with the new one within the list.
        */
        PdfArray itemsObject = items.getBaseDataObject();
        itemsObject.set(itemsObject.indexOf(oldBaseDataObject),baseDataObject);
      }
    }

    ((PdfTextString)((PdfArray)baseDataObject).get(1)).setValue(value);
  }

  /**
    @see #getValue()
  */
  public void setValue(
    String value
    )
  {
    PdfDirectObject baseDataObject = getBaseDataObject();
    if(baseDataObject instanceof PdfArray) // <value,text> pair.
      ((PdfTextString)((PdfArray)baseDataObject).get(0)).setValue(value);
    else // Single text string.
      ((PdfTextString)baseDataObject).setValue(value);
  }
  // </public>

  // <internal>
  void setItems(
    ChoiceItems value
    )
  {
    if(items != null)
      throw new IllegalArgumentException("Item already associated to another choice field.");

    items = value;
  }
  // </internal>
  // </interface>
}
