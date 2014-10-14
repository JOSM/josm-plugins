/**
 * Copyright by Christof Dallermassl
 * This program is free software and licensed under GPL.
 */
package at.dallermassl.josm.plugin.surveyor.action;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.openstreetmap.josm.Main;

import at.dallermassl.josm.plugin.surveyor.GpsActionEvent;

/**
 * @author cdaller
 *
 */
public class SystemExecuteAction extends AbstractSurveyorAction {

    @Override
    public void actionPerformed(GpsActionEvent event) {
        final ProcessBuilder builder = new ProcessBuilder(getParameters());
        builder.directory(new File(System.getProperty("user.home")));

        Main.debug("Directory : " + builder.directory());
        Thread executionThread = new Thread() {

            @Override
            public void run() {
                try {
                    final Process process = builder.start();
                    InputStream is = process.getInputStream();
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader br = new BufferedReader(isr);
                    String line;

                    while ((line = br.readLine()) != null) {
                        Main.info(getClass().getSimpleName() + ": " +  line);
                    }

                    Main.info(getClass().getSimpleName() + "Program terminated!");
                } catch (Exception t) {
                    Main.error(t);
                }
            }
        };
        executionThread.start();
    }
}
