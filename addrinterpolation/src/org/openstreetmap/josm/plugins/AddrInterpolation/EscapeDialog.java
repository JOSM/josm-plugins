// License: GPL. Copyright 2009 by Mike Nice and others
// Handles ESC key to close a dialog
package org.openstreetmap.josm.plugins.AddrInterpolation;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

public class EscapeDialog extends JDialog {
	public EscapeDialog() {
		this((Frame)null, false);
	}
	public EscapeDialog(Frame owner) {
		this(owner, false);
	}
	public EscapeDialog(Frame owner, boolean modal) {
		this(owner, null, modal);
	}
	public EscapeDialog(Frame owner, String title) {
		this(owner, title, false);
	}
	public EscapeDialog(Frame owner, String title, boolean modal) {
		super(owner, title, modal);
	}
	public EscapeDialog(Dialog owner) {
		this(owner, false);
	}
	public EscapeDialog(Dialog owner, boolean modal) {
		this(owner, null, modal);
	}
	public EscapeDialog(Dialog owner, String title) {
		this(owner, title, false);
	}
	public EscapeDialog(Dialog owner, String title, boolean modal) {
		super(owner, title, modal);
	}


	@Override
	protected JRootPane createRootPane() {
		ActionListener escapeActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				dispose();
				// setVisible(false);
			}
		};
		JRootPane rootPane = new JRootPane();
		KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		rootPane.registerKeyboardAction(escapeActionListener, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);

		return rootPane;
	}
}

