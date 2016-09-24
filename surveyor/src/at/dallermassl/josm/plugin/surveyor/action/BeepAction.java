// License: GPL. For details, see LICENSE file.
package at.dallermassl.josm.plugin.surveyor.action;

import java.awt.Toolkit;
import java.util.List;

import org.openstreetmap.josm.Main;

import at.dallermassl.josm.plugin.surveyor.GpsActionEvent;
import at.dallermassl.josm.plugin.surveyor.SurveyorAction;

/**
 * @author cdaller
 *
 */
public class BeepAction implements SurveyorAction {
    int beepNumber = 1;

    @Override
    public void actionPerformed(GpsActionEvent event) {
        // run as a separate thread
        Main.worker.execute(new Runnable() {
            public void run() {
                for (int index = 0; index < beepNumber; ++index) {
                    Toolkit.getDefaultToolkit().beep();
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ignore) {
                        Main.debug(ignore);
                    }
                }
            }
        });
    }

    @Override
    public void setParameters(List<String> parameters) {
        try {
            beepNumber = Integer.parseInt(parameters.get(0));
        } catch (NumberFormatException e) {
            // print but recover
            Main.warn(e);
        }
    }
}
