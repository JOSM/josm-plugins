
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
        

package org.apache.poi.poifs.filesystem;

import org.apache.poi.poifs.property.DocumentProperty;

/**
 * Simple implementation of DocumentEntry
 *
 * @author Marc Johnson (mjohnson at apache dot org)
 */

public class DocumentNode
    extends EntryNode
    implements DocumentEntry
{

    // underlying POIFSDocument instance
    private POIFSDocument _document;

    /**
     * create a DocumentNode. This method is not public by design; it
     * is intended strictly for the internal use of this package
     *
     * @param property the DocumentProperty for this DocumentEntry
     * @param parent the parent of this entry
     */

    DocumentNode(final DocumentProperty property, final DirectoryNode parent)
    {
        super(property, parent);
        _document = property.getDocument();
    }

    /**
     * get the POIFSDocument
     *
     * @return the internal POIFSDocument
     */

    POIFSDocument getDocument()
    {
        return _document;
    }

    /* ********** START implementation of DocumentEntry ********** */

    /**
     * get the zize of the document, in bytes
     *
     * @return size in bytes
     */

    public int getSize()
    {
        return getProperty().getSize();
    }

    /* **********  END  implementation of DocumentEntry ********** */
    /* ********** START implementation of Entry ********** */

    /**
     * is this a DocumentEntry?
     *
     * @return true if the Entry is a DocumentEntry, else false
     */

    public boolean isDocumentEntry()
    {
        return true;
    }

    /* **********  END  implementation of Entry ********** */
    /* ********** START extension of Entry ********** */

    /**
     * extensions use this method to verify internal rules regarding
     * deletion of the underlying store.
     *
     * @return true if it's ok to delete the underlying store, else
     *         false
     */

    protected boolean isDeleteOK()
    {
        return true;
    }

    /* **********  END  extension of Entry ********** */
}   // end public class DocumentNode

