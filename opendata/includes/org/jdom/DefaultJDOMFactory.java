/*--

 $Id: DefaultJDOMFactory.java,v 1.7 2007/11/10 05:28:58 jhunter Exp $

 Copyright (C) 2000-2007 Jason Hunter & Brett McLaughlin.
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions, and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions, and the disclaimer that follows
    these conditions in the documentation and/or other materials
    provided with the distribution.

 3. The name "JDOM" must not be used to endorse or promote products
    derived from this software without prior written permission.  For
    written permission, please contact <request_AT_jdom_DOT_org>.

 4. Products derived from this software may not be called "JDOM", nor
    may "JDOM" appear in their name, without prior written permission
    from the JDOM Project Management <request_AT_jdom_DOT_org>.

 In addition, we request (but do not require) that you include in the
 end-user documentation provided with the redistribution and/or in the
 software itself an acknowledgement equivalent to the following:
     "This product includes software developed by the
      JDOM Project (http://www.jdom.org/)."
 Alternatively, the acknowledgment may be graphical using the logos
 available at http://www.jdom.org/images/logos.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED.  IN NO EVENT SHALL THE JDOM AUTHORS OR THE PROJECT
 CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGE.

 This software consists of voluntary contributions made by many
 individuals on behalf of the JDOM Project and was originally
 created by Jason Hunter <jhunter_AT_jdom_DOT_org> and
 Brett McLaughlin <brett_AT_jdom_DOT_org>.  For more information
 on the JDOM Project, please see <http://www.jdom.org/>.

 */

package org.jdom;


/**
 * Creates the standard top-level JDOM classes (Element, Document, Comment,
 * etc). A subclass of this factory might construct custom classes.
 *
 * @version $Revision: 1.7 $, $Date: 2007/11/10 05:28:58 $
 * @author  Ken Rune Holland
 * @author  Phil Nelson
 * @author  Bradley S. Huffman
 */
public class DefaultJDOMFactory implements JDOMFactory {

    public DefaultJDOMFactory() { }

    // Allow Javadocs to inherit from JDOMFactory

    public Attribute attribute(String name, String value,
                                            int type, Namespace namespace) {
        return new Attribute(name, value, type, namespace);
    }

    public CDATA cdata(String text) {
        return new CDATA(text);
    }

    public Text text(String text) {
        return new Text(text);
    }

    public Comment comment(String text) {
        return new Comment(text);
    }

    public DocType docType(String elementName,
                           String publicID, String systemID) {
        return new DocType(elementName, publicID, systemID);
    }

    public Document document(Element rootElement) {
        return new Document(rootElement);
    }

    public Element element(String name, Namespace namespace) {
        return new Element(name, namespace);
    }

    public ProcessingInstruction processingInstruction(String target,
                                                       String data) {
        return new ProcessingInstruction(target, data);
    }

    public EntityRef entityRef(String name) {
        return new EntityRef(name);
    }

    public EntityRef entityRef(String name, String publicID, String systemID) {
        return new EntityRef(name, publicID, systemID);
    }

    // =====================================================================
    // List manipulation
    // =====================================================================

    public void addContent(Parent parent, Content child) {
        if (parent instanceof Document) {
            ((Document) parent).addContent(child);
        }
        else {
            ((Element) parent).addContent(child);
        }
    }

    public void setAttribute(Element parent, Attribute a) {
        parent.setAttribute(a);
    }
}
