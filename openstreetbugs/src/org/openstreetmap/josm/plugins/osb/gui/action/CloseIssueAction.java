/* Copyright (c) 2008, Henrik Niehaus
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of the project nor the names of its
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.openstreetmap.josm.plugins.osb.gui.action;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.plugins.osb.OsbPlugin;
import org.openstreetmap.josm.plugins.osb.api.CloseAction;
import org.openstreetmap.josm.plugins.osb.api.EditAction;
import org.openstreetmap.josm.plugins.osb.gui.dialogs.TextInputDialog;
import org.openstreetmap.josm.plugins.osb.gui.historycombobox.HistoryChangedListener;
import org.openstreetmap.josm.plugins.osb.gui.historycombobox.StringUtils;

public class CloseIssueAction extends OsbAction {

    private static final long serialVersionUID = 1L;

    private CloseAction closeAction = new CloseAction();
    private EditAction commentAction = new EditAction();

    public CloseIssueAction() {
        super(tr("Mark as done"));
    }

    @Override
    protected void doActionPerformed(ActionEvent e) throws Exception {
        List<String> history = StringUtils.stringToList(Main.pref.get("osb.comment.history"), "§§§");
        HistoryChangedListener l = new HistoryChangedListener() {
            public void historyChanged(List<String> history) {
                Main.pref.put("osb.comment.history", StringUtils.listToString(history, "§§§"));
            }
        };
        String comment = TextInputDialog.showDialog(Main.map,
                tr("Really close?"),
                tr("<html>Really mark this issue as ''done''?<br><br>You may add an optional comment:</html>"),
                OsbPlugin.loadIcon("icon_valid22.png"),
                history, l);

        if(comment != null) {
            if(comment.length() > 0) {
                commentAction.execute(getSelectedNode(), comment);
            }
            closeAction.execute(getSelectedNode());
        }
    }
}
