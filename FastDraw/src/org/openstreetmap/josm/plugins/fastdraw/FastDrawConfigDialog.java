package org.openstreetmap.josm.plugins.fastdraw;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;
import java.text.ParseException;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.widgets.HistoryComboBox;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.TextTagParser;
import org.openstreetmap.josm.tools.Utils;

public class FastDrawConfigDialog extends ExtendedDialog {

    public FastDrawConfigDialog(FDSettings settings) {
        super(Main.parent,tr("FastDraw configuration"),new String[] {tr("Ok"), tr("Cancel")});
        JPanel all = new JPanel();
        GridBagLayout layout = new GridBagLayout();
        all.setLayout(layout);
        
        JLabel label1=new JLabel(tr("Epsilon multiplier"));
        JLabel label2=new JLabel(tr("Starting Epsilon"));
        JLabel label3=new JLabel(tr("Max points count per 1 km"));
        JLabel label4=new JLabel(/* I18n: Combobox to select what a press to return key does */ tr("Enter key mode"));
        JLabel label5=new JLabel(tr("Auto add tags"));
        JFormattedTextField text1=new JFormattedTextField(NumberFormat.getInstance());
        JFormattedTextField text2=new  JFormattedTextField(NumberFormat.getInstance());
        JFormattedTextField text3=new  JFormattedTextField(NumberFormat.getInstance());
//        JComboBox combo1=new JComboBox(new String[]{tr("Autosimplify and wait"),
//            tr("Autosimplify and save"),tr("Simplify and wait"),tr("Simplify and save"),
//            tr("Save as is")});
        JComboBox combo1=new JComboBox(new String[]{tr("Autosimplify"),
            tr("Simplify with initial epsilon"),tr("Save as is")});
        JCheckBox snapCb=new JCheckBox(tr("Snap to nodes"));
        JCheckBox fixedClickCb = new JCheckBox(tr("Add fixed points on click"));
        JCheckBox fixedSpaceCb = new JCheckBox(tr("Add fixed points on spacebar"));
        JCheckBox drawClosedCb = new JCheckBox(tr("Draw closed polygons only"));
        final HistoryComboBox addTags = new HistoryComboBox();
        JButton pasteButton = new JButton(new AbstractAction(tr("Paste"), ImageProvider.get("apply")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                String s = Utils.getClipboardContent();
                if (TextTagParser.getValidatedTagsFromText(s)!=null) {
                    addTags.setText(s);
                }
            }
        });
        pasteButton.setToolTipText(tr("Try copying tags from properties table"));
        
        addTags.setPossibleItems(Main.pref.getCollection("fastdraw.tags-history"));
        
        all.add(label1,GBC.std().insets(10,0,0,0));
        all.add(text1, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,5));
        all.add(label2,GBC.std().insets(10,0,0,0));
        all.add(text2, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,5));
        all.add(label3,GBC.std().insets(10,0,0,0));
        all.add(text3, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,5));
        all.add(label4,GBC.std().insets(10,0,0,0));
        all.add(combo1, GBC.eop().fill(GBC.HORIZONTAL).insets(5,0,0,5));
        
        all.add(label5,GBC.std().insets(10,0,0,0));
        all.add(pasteButton, GBC.eop().insets(0,0,0,5));
        
        all.add(addTags, GBC.eop().fill(GBC.HORIZONTAL).insets(10,0,5,10));
        
        all.add(snapCb,GBC.eop().insets(20,0,0,0));
        
        all.add(fixedClickCb,GBC.eop().insets(20,0,0,0));
        all.add(fixedSpaceCb,GBC.eop().insets(20,0,0,0));
        all.add(drawClosedCb,GBC.eop().insets(20,0,0,0));
        
        addTags.setText(settings.autoTags);
        text1.setValue(settings.epsilonMult);
        text2.setValue(settings.startingEps);
        text3.setValue(settings.maxPointsPerKm);
        snapCb.setSelected(settings.snapNodes);
        fixedClickCb.setSelected(settings.fixedClick);
        fixedSpaceCb.setSelected(settings.fixedSpacebar);
        drawClosedCb.setSelected(settings.drawClosed);
        combo1.setSelectedIndex(settings.simplifyMode);
        
        ExtendedDialog dialog = new ExtendedDialog(Main.parent,
                tr("FastDraw settings"),
                new String[] {tr("Ok"), tr("Cancel")}
        );
        setContent(all, false);
        setButtonIcons(new String[] {"ok.png", "cancel.png"});
        setToolTipTexts(new String[] {
                tr("Save settings"),
                tr("Cancel")
        });
        setDefaultButton(1);
        //configureContextsensitiveHelp("/Action/DownloadObject", true /* show help button */);
        showDialog();
        if (dialog.getValue() == 0) {
            try {
            settings.epsilonMult=NumberFormat.getInstance().parse(text1.getText()).doubleValue();
            settings.startingEps=NumberFormat.getInstance().parse(text2.getText()).doubleValue();
            settings.maxPointsPerKm=NumberFormat.getInstance().parse(text3.getText()).doubleValue();
            settings.snapNodes=snapCb.isSelected();
            settings.fixedClick=fixedClickCb.isSelected();
            settings.fixedSpacebar=fixedSpaceCb.isSelected();
            settings.drawClosed=drawClosedCb.isSelected();
            settings.simplifyMode=combo1.getSelectedIndex();
            settings.autoTags=addTags.getText();
            addTags.addCurrentItemToHistory();
            Main.pref.putCollection("fastdraw.tags-history", addTags.getHistory());
            settings.savePrefs();
            } catch (ParseException e) {
              JOptionPane.showMessageDialog(Main.parent,
                  tr("Can not read settings"));
            }
        }
            
    }
    
}
