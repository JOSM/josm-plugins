// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.core.io.tabular;

import java.nio.charset.Charset;

public class DefaultCsvHandler extends DefaultSpreadSheetHandler implements CsvHandler {

    private Charset charset;
    private String separator;
    
    @Override
    public void setCharset(Charset cs) {
        charset = cs;
    }

    @Override
    public void setCharset(String charset) {
        setCharset(Charset.forName(charset));
    }

    @Override
    public Charset getCharset() {
        return charset;
    }

    @Override
    public void setSeparator(String sep) {
        separator = sep;
    }

    @Override
    public String getSeparator() {
        return separator;
    }
}
