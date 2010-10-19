/*
  Copyright 2008-2010 Stefano Chizzolini. http://clown.stefanochizzolini.it

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
import it.stefanochizzolini.clown.documents.interaction.annotations.DualWidget;
import it.stefanochizzolini.clown.documents.interaction.annotations.Widget;
import it.stefanochizzolini.clown.files.File;
import it.stefanochizzolini.clown.objects.PdfArray;
import it.stefanochizzolini.clown.objects.PdfDataObject;
import it.stefanochizzolini.clown.objects.PdfDictionary;
import it.stefanochizzolini.clown.objects.PdfDirectObject;
import it.stefanochizzolini.clown.objects.PdfIndirectObject;
import it.stefanochizzolini.clown.objects.PdfName;
import it.stefanochizzolini.clown.objects.PdfObjectWrapper;
import it.stefanochizzolini.clown.objects.PdfReference;
import it.stefanochizzolini.clown.util.NotImplementedException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
  Field widget annotations [PDF:1.6:8.6].

  @author Stefano Chizzolini (http://www.stefanochizzolini.it)
  @version 0.0.8
  @since 0.0.7
*/
public class FieldWidgets
  extends PdfObjectWrapper<PdfDataObject>
  implements List<Widget>
{
  /*
    NOTE: Widget annotations may be singular (either merged to their field or within an array)
    or multiple (within an array).
    This implementation hides such a complexity to the user, smoothly exposing just the most
    general case (array) yet preserving its internal state.
  */
  // <class>
  // <dynamic>
  // <fields>
  private Field field;

  private boolean isDual;
  // </fields>

  // <constructors>
  FieldWidgets(
    PdfDirectObject baseObject,
    PdfIndirectObject container,
    Field field
    )
  {
    super(
      baseObject,
      container
      );

    this.field = field;

    isDual = (field instanceof CheckBox
      || field instanceof RadioButton);
  }
  // </constructors>

  // <interface>
  // <public>
  @Override
  public FieldWidgets clone(
    Document context
    )
  {throw new NotImplementedException();}

  /**
    Gets the field associated to these widgets.
  */
  public Field getField(
    )
  {return field;}

  // <List>
  public void add(
    int index,
    Widget value
    )
  {ensureArray().add(index,value.getBaseObject());}

  public boolean addAll(
    int index,
    Collection<? extends Widget> values
    )
  {
    PdfArray items = ensureArray();
    for(Widget value : values)
    {items.add(index++,value.getBaseObject());}

    return true;
  }

  public Widget get(
    int index
    )
  {
    PdfDataObject baseDataObject = getBaseDataObject();
    if(baseDataObject instanceof PdfDictionary) // Single annotation.
    {
      if(index != 0)
        throw new IndexOutOfBoundsException("Index: " + index + ", Size: 1");

      return newWidget(getBaseObject());
    }
    else // Array.
    {
      return newWidget(((PdfArray)baseDataObject).get(index));
    }
  }

  public int indexOf(
    Object value
    )
  {
    if(!(value instanceof Widget))
      return -1;

    PdfDataObject baseDataObject = getBaseDataObject();
    if(baseDataObject instanceof PdfDictionary) // Single annotation.
    {
      if(((Widget)value).getBaseObject().equals(getBaseObject()))
        return 0;
      else
        return -1;
    }

    return ((PdfArray)baseDataObject).indexOf(((Widget)value).getBaseObject());
  }

  public int lastIndexOf(
    Object value
    )
  {
    /*
      NOTE: Widgets are expected not to be duplicate.
    */
    return indexOf(value);
  }

  public ListIterator<Widget> listIterator(
    )
  {throw new NotImplementedException();}

  public ListIterator<Widget> listIterator(
    int index
    )
  {throw new NotImplementedException();}

  public Widget remove(
    int index
    )
  {
    PdfDirectObject widgetObject = ensureArray().remove(index);

    return newWidget(widgetObject);
  }

  public Widget set(
    int index,
    Widget value
    )
  {return newWidget(ensureArray().set(index,value.getBaseObject()));}

  public List<Widget> subList(
    int fromIndex,
    int toIndex
    )
  {throw new NotImplementedException();}

  // <Collection>
  public boolean add(
    Widget value
    )
  {
    value.getBaseDataObject().put(PdfName.Parent,field.getBaseObject());

    return ensureArray().add(value.getBaseObject());
  }

  public boolean addAll(
    Collection<? extends Widget> values
    )
  {
    for(Widget value : values)
    {add(value);}

    return true;
  }

  public void clear(
    )
  {ensureArray().clear();}

  public boolean contains(
    Object value
    )
  {
    if(!(value instanceof Widget))
      return false;

    PdfDataObject baseDataObject = getBaseDataObject();
    if(baseDataObject instanceof PdfDictionary) // Single annotation.
      return ((Widget)value).getBaseObject().equals(getBaseObject());

    return ((PdfArray)baseDataObject).contains(((Widget)value).getBaseObject());
  }

  public boolean containsAll(
    Collection<?> values
    )
  {throw new NotImplementedException();}

  public boolean equals(
    Object object
    )
  {throw new NotImplementedException();}

  public int hashCode(
    )
  {throw new NotImplementedException();}

  public boolean isEmpty(
    )
  {
    PdfDataObject baseDataObject = getBaseDataObject();
    if(baseDataObject instanceof PdfDictionary) // Single annotation.
      return false;
    else // Array.
      return ((PdfArray)baseDataObject).isEmpty();
  }

  public boolean remove(
    Object value
    )
  {
    if(!(value instanceof Widget))
      return false;

    return ensureArray().remove(((Widget)value).getBaseObject());
  }

  public boolean removeAll(
    Collection<?> values
    )
  {throw new NotImplementedException();}

  public boolean retainAll(
    Collection<?> values
    )
  {throw new NotImplementedException();}

  public int size(
    )
  {
    PdfDataObject baseDataObject = getBaseDataObject();
    if(baseDataObject instanceof PdfDictionary) // Single annotation.
      return 1;
    else // Array.
      return ((PdfArray)baseDataObject).size();
  }

  public Object[] toArray(
    )
  {return toArray(new Widget[0]);}

  @SuppressWarnings("unchecked")
  public <T> T[] toArray(
    T[] values
    )
  {
    PdfDataObject baseDataObject = getBaseDataObject();
    if(baseDataObject instanceof PdfDictionary) // Single annotation.
    {
      if(values.length == 0)
      {values = (T[])new Object[1];}

      values[0] = (T)newWidget(getBaseObject());
    }
    else // Array.
    {
      PdfArray widgetObjects = (PdfArray)baseDataObject;
      if(values.length < widgetObjects.size())
      {values = (T[])new Object[widgetObjects.size()];}

      for(
        int index = 0,
          length = widgetObjects.size();
        index < length;
        index++
        )
      {values[index] = (T)newWidget(widgetObjects.get(index));}
    }
    return values;
  }

  // <Iterable>
  public Iterator<Widget> iterator(
    )
  {
    return new Iterator<Widget>()
    {
      // <class>
      // <dynamic>
      // <fields>
      /**
        Index of the next item.
      */
      private int index = 0;
      /**
        Collection size.
      */
      private int size = size();
      // </fields>

      // <interface>
      // <public>
      // <Iterator>
      public boolean hasNext(
        )
      {return (index < size);}

      public Widget next(
        )
      {
        if(!hasNext())
          throw new NoSuchElementException();

        return get(index++);
      }

      public void remove(
        )
      {throw new UnsupportedOperationException();}
      // </Iterator>
      // </public>
      // </interface>
      // </dynamic>
      // </class>
    };
  }
  // </Iterable>
  // </Collection>
  // </List>
  // </public>

  // <private>
  private PdfArray ensureArray(
    )
  {
    PdfDataObject baseDataObject = getBaseDataObject();
    if(baseDataObject instanceof PdfDictionary) // Merged annotation.
    {
      PdfArray widgetsArray = new PdfArray();
      {
        PdfDictionary fieldDictionary = (PdfDictionary)baseDataObject;
        PdfDictionary widgetDictionary = null;
        // Extracting widget entries from the field...
        for(PdfName key : new HashMap<PdfName,PdfDirectObject>(fieldDictionary).keySet())
        {
          // Is it a widget entry?
          if(key.equals(PdfName.Type)
            || key.equals(PdfName.Subtype)
            || key.equals(PdfName.Rect)
            || key.equals(PdfName.Contents)
            || key.equals(PdfName.P)
            || key.equals(PdfName.NM)
            || key.equals(PdfName.M)
            || key.equals(PdfName.F)
            || key.equals(PdfName.BS)
            || key.equals(PdfName.AP)
            || key.equals(PdfName.AS)
            || key.equals(PdfName.Border)
            || key.equals(PdfName.C)
            || key.equals(PdfName.A)
            || key.equals(PdfName.AA)
            || key.equals(PdfName.StructParent)
            || key.equals(PdfName.OC)
            || key.equals(PdfName.H)
            || key.equals(PdfName.MK))
          {
            if(widgetDictionary == null)
            {
              widgetDictionary = new PdfDictionary();
              PdfReference widgetReference = getFile().register(widgetDictionary);

              // Remove the field from the page annotations (as the widget annotation is decoupled from it)!
              PdfArray pageAnnotationsArray = (PdfArray)File.resolve(((PdfDictionary)File.resolve(fieldDictionary.get(PdfName.P))).get(PdfName.Annots));
              pageAnnotationsArray.remove(field.getBaseObject());

              // Add the widget to the page annotations!
              pageAnnotationsArray.add(widgetReference);
              // Add the widget to the field widgets!
              widgetsArray.add(widgetReference);
              // Associate the field to the widget!
              widgetDictionary.put(PdfName.Parent,field.getBaseObject());
            }

            // Transfer the entry from the field to the widget!
            widgetDictionary.put(key,fieldDictionary.get(key));
            fieldDictionary.remove(key);
          }
        }
      }
      setBaseObject(widgetsArray);
      field.getBaseDataObject().put(PdfName.Kids,widgetsArray);

      baseDataObject = widgetsArray;
    }

    return (PdfArray)baseDataObject;
  }

  private Widget newWidget(
    PdfDirectObject baseObject
    )
  {
    if(isDual)
      return new DualWidget(baseObject,getContainer());
    else
      return new Widget(baseObject,getContainer());
  }
  // </private>
  // </interface>
  // </dynamic>
  // </class>
}