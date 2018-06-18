// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.utils;

import java.awt.Color;

import javax.swing.JComponent;

public final class StreetsideColorScheme {
  /**
   * Color for unselected images
   */
  public static final Color SEQ_UNSELECTED = new Color(0x00ccd1);
  /**
   * Color for the camera angle indicator of images in unselected sequences
   */
  public static final Color SEQ_UNSELECTED_CA = new Color(0x4169e1);
  /**
   * Color for the marker of images in a selected sequence
   */
  public static final Color SEQ_SELECTED = new Color(0x00b5f5);
  /**
   * Color for the camera angle indicator of images in selected sequences
   */
  public static final Color SEQ_SELECTED_CA = new Color(0x8b008b);
  /**
   * Color for the marker of currently selected images
   */
  public static final Color SEQ_HIGHLIGHTED = new Color(0xf5811a);
  /**
   * Color for the camera angle indicator of the currently selected images
   */
  public static final Color SEQ_HIGHLIGHTED_CA = new Color(0xf5b81a);

  public static final Color SEQ_IMPORTED_SELECTED = new Color(0xdddddd);
  public static final Color SEQ_IMPORTED_SELECTED_CA = SEQ_IMPORTED_SELECTED.brighter();
  public static final Color SEQ_IMPORTED_UNSELECTED = new Color(0x999999);
  public static final Color SEQ_IMPORTED_UNSELECTED_CA = SEQ_IMPORTED_UNSELECTED.brighter();
  public static final Color SEQ_IMPORTED_HIGHLIGHTED = new Color(0xbb2222);
  public static final Color SEQ_IMPORTED_HIGHLIGHTED_CA = SEQ_IMPORTED_HIGHLIGHTED.brighter();

  public static final Color STREETSIDE_BLUE = new Color(0x0000ff);
  public static final Color TOOLBAR_DARK_GREY = new Color(0x242528);

  public static final Color IMAGEDETECTION_TRAFFICSIGN = new Color(0xffc01b);
  public static final Color IMAGEDETECTION_UNKNOWN = new Color(0x33bb44);

  private StreetsideColorScheme() {
    // Private constructor to avoid instantiation
  }

  /**
   * Styles the given components as default panels (currently only the background is set to white)
   * @param components the components to style as default panels (e.g. checkboxes also, that's why
   *   not only JPanels are accepted)
   */
  public static void styleAsDefaultPanel(JComponent... components) {
    if (components != null && components.length >= 1) {
      for (JComponent component : components) {
        component.setBackground(Color.WHITE);
      }
    }
  }
}
