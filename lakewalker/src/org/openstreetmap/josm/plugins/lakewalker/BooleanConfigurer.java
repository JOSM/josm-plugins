/*
 * $Id: BooleanConfigurer.java 705 2005-09-15 13:24:50 +0000 (Thu, 15 Sep 2005) rodneykinney $
 *
 * Copyright (c) 2000-2003 by Rodney Kinney
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License (LGPL) as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, copies are available 
 * at http://www.opensource.org.
 */
package org.openstreetmap.josm.plugins.lakewalker;

/**
 * Configurer for Boolean values
 */
public class BooleanConfigurer extends Configurer {
  private javax.swing.JCheckBox box;

  public BooleanConfigurer() {
    this(false);
  }
  
  public BooleanConfigurer(boolean val) {
    this(null, "", val);
  }
  
  public BooleanConfigurer(String key, String name, Boolean val) {
    super(key, name, val);
  }

  public BooleanConfigurer(String key, String name, boolean val) {
    super(key, name, val ? Boolean.TRUE : Boolean.FALSE);
  }

  public BooleanConfigurer(String key, String name) {
    this(key, name, Boolean.FALSE);
  }

  public String getValueString() {
    return booleanValue().toString();
  }

  public void setValue(Object o) {
    super.setValue(o);
    if (box != null
      && !o.equals(new Boolean(box.isSelected()))) {
      box.setSelected(booleanValue().booleanValue());
    }
  }

  public void setValue(String s) {
    setValue(Boolean.valueOf(s));
  }

  public void setName(String s) {
    super.setName(s);
    if (box != null) {
      box.setText(s);
    }
  }

  public java.awt.Component getControls() {
    if (box == null) {
      box = new javax.swing.JCheckBox(getName());
      box.setSelected(booleanValue().booleanValue());
      box.addItemListener(new java.awt.event.ItemListener() {
        public void itemStateChanged(java.awt.event.ItemEvent e) {
          setValue(new Boolean(box.isSelected()));
        }
      });
    }
    return box;
  }

  public Boolean booleanValue() {
    return (Boolean) value;
  }
}
