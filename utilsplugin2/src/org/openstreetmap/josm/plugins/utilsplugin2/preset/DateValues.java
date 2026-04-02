package org.openstreetmap.josm.plugins.utilsplugin2.preset;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DateValues {
    public static String[] lastTwoWeeks() {
        Calendar cal = Calendar.getInstance();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        List<String> days = new ArrayList<>();
        for (int day = 0; day < 14; day += 1) {
            days.add(df.format(cal.getTime()));
            cal.add(Calendar.DAY_OF_MONTH, -1);
        }
        return days.toArray(new String[0]);
    }
}
