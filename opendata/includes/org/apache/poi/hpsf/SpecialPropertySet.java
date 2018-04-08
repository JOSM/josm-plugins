/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.hpsf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.poi.hpsf.wellknown.PropertyIDMap;
import org.apache.poi.poifs.filesystem.DirectoryEntry;

/**
 * <p>Abstract superclass for the convenience classes {@link
 * SummaryInformation} and {@link DocumentSummaryInformation}.</p>
 *
 * <p>The motivation behind this class is quite nasty if you look
 * behind the scenes, but it serves the application programmer well by
 * providing him with the easy-to-use {@link SummaryInformation} and
 * {@link DocumentSummaryInformation} classes. When parsing the data a
 * property set stream consists of (possibly coming from an {@link
 * java.io.InputStream}) we want to read and process each byte only
 * once. Since we don't know in advance which kind of property set we
 * have, we can expect only the most general {@link
 * PropertySet}. Creating a special subclass should be as easy as
 * calling the special subclass' constructor and pass the general
 * {@link PropertySet} in. To make things easy internally, the special
 * class just holds a reference to the general {@link PropertySet} and
 * delegates all method calls to it.</p>
 *
 * <p>A cleaner implementation would have been like this: The {@link
 * PropertySetFactory} parses the stream data into some internal
 * object first.  Then it finds out whether the stream is a {@link
 * SummaryInformation}, a {@link DocumentSummaryInformation} or a
 * general {@link PropertySet}.  However, the current implementation
 * went the other way round historically: the convenience classes came
 * only late to my mind.</p>
 *
 * @author Rainer Klute <a
 * href="mailto:klute@rainer-klute.de">&lt;klute@rainer-klute.de&gt;</a>
 */
public abstract class SpecialPropertySet extends MutablePropertySet
{
	/**
	 * The id to name mapping of the properties
	 *  in this set.
	 */
	public abstract PropertyIDMap getPropertySetIDMap();

    /**
     * <p>The "real" property set <code>SpecialPropertySet</code>
     * delegates to.</p>
     */
    private MutablePropertySet delegate;



    /**
     * <p>Creates a <code>SpecialPropertySet</code>.
     *
     * @param ps The property set to be encapsulated by the
     * <code>SpecialPropertySet</code>
     */
    public SpecialPropertySet(final PropertySet ps)
    {
        delegate = new MutablePropertySet(ps);
    }

    /**
     * @see PropertySet#getByteOrder
     */
    @Override
    public int getByteOrder()
    {
        return delegate.getByteOrder();
    }



    /**
     * @see PropertySet#getFormat
     */
    @Override
    public int getFormat()
    {
        return delegate.getFormat();
    }



    /**
     * @see PropertySet#getOSVersion
     */
    @Override
    public int getOSVersion()
    {
        return delegate.getOSVersion();
    }



    /**
     * @see PropertySet#getClassID
     */
    @Override
    public ClassID getClassID()
    {
        return delegate.getClassID();
    }



    /**
     * @see PropertySet#getSectionCount
     */
    @Override
    public int getSectionCount()
    {
        return delegate.getSectionCount();
    }



    /**
     * @see PropertySet#getSections
     */
    @Override
    public List<Section> getSections()
    {
        return delegate.getSections();
    }



    /**
     * @see PropertySet#isSummaryInformation
     */
    @Override
    public boolean isSummaryInformation()
    {
        return delegate.isSummaryInformation();
    }



    /**
     * @see PropertySet#isDocumentSummaryInformation
     */
    @Override
    public boolean isDocumentSummaryInformation()
    {
        return delegate.isDocumentSummaryInformation();
    }



    /**
     * @see PropertySet
     */
    @Override
    public Section getFirstSection()
    {
        return delegate.getFirstSection();
    }


    /**
     * @see org.apache.poi.hpsf.MutablePropertySet#addSection(org.apache.poi.hpsf.Section)
     */
    @Override
    public void addSection(final Section section)
    {
        delegate.addSection(section);
    }



    /**
     * @see org.apache.poi.hpsf.MutablePropertySet#clearSections()
     */
    @Override
    public void clearSections()
    {
        delegate.clearSections();
    }



    /**
     * @see org.apache.poi.hpsf.MutablePropertySet#setByteOrder(int)
     */
    @Override
    public void setByteOrder(final int byteOrder)
    {
        delegate.setByteOrder(byteOrder);
    }



    /**
     * @see org.apache.poi.hpsf.MutablePropertySet#setClassID(org.apache.poi.hpsf.ClassID)
     */
    @Override
    public void setClassID(final ClassID classID)
    {
        delegate.setClassID(classID);
    }



    /**
     * @see org.apache.poi.hpsf.MutablePropertySet#setFormat(int)
     */
    @Override
    public void setFormat(final int format)
    {
        delegate.setFormat(format);
    }



    /**
     * @see org.apache.poi.hpsf.MutablePropertySet#setOSVersion(int)
     */
    @Override
    public void setOSVersion(final int osVersion)
    {
        delegate.setOSVersion(osVersion);
    }



    /**
     * @see org.apache.poi.hpsf.MutablePropertySet#toInputStream()
     */
    @Override
    public InputStream toInputStream() throws IOException, WritingNotSupportedException
    {
        return delegate.toInputStream();
    }



    /**
     * @see org.apache.poi.hpsf.MutablePropertySet#write(org.apache.poi.poifs.filesystem.DirectoryEntry, java.lang.String)
     */
    @Override
    public void write(final DirectoryEntry dir, final String name) throws WritingNotSupportedException, IOException
    {
        delegate.write(dir, name);
    }



    /**
     * @see org.apache.poi.hpsf.MutablePropertySet#write(java.io.OutputStream)
     */
    @Override
    public void write(final OutputStream out) throws WritingNotSupportedException, IOException
    {
        delegate.write(out);
    }



    /**
     * @see org.apache.poi.hpsf.PropertySet#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object o)
    {
        return delegate.equals(o);
    }



    /**
     * @see org.apache.poi.hpsf.PropertySet#getProperties()
     */
    @Override
    public Property[] getProperties() throws NoSingleSectionException
    {
        return delegate.getProperties();
    }



    /**
     * @see org.apache.poi.hpsf.PropertySet#getProperty(int)
     */
    @Override
    protected Object getProperty(final int id) throws NoSingleSectionException
    {
        return delegate.getProperty(id);
    }



    /**
     * @see org.apache.poi.hpsf.PropertySet#getPropertyBooleanValue(int)
     */
    @Override
    protected boolean getPropertyBooleanValue(final int id) throws NoSingleSectionException
    {
        return delegate.getPropertyBooleanValue(id);
    }



    /**
     * @see org.apache.poi.hpsf.PropertySet#getPropertyIntValue(int)
     */
    @Override
    protected int getPropertyIntValue(final int id) throws NoSingleSectionException
    {
        return delegate.getPropertyIntValue(id);
    }



    /**
     * @see org.apache.poi.hpsf.PropertySet#hashCode()
     */
    @Override
    public int hashCode()
    {
        return delegate.hashCode();
    }



    /**
     * @see org.apache.poi.hpsf.PropertySet#toString()
     */
    @Override
    public String toString()
    {
        return delegate.toString();
    }



    /**
     * @see org.apache.poi.hpsf.PropertySet#wasNull()
     */
    @Override
    public boolean wasNull() throws NoSingleSectionException
    {
        return delegate.wasNull();
    }

}
