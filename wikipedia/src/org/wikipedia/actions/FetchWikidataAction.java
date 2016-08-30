// License: GPL. For details, see LICENSE file.
package org.wikipedia.actions;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.MultiMap;
import org.openstreetmap.josm.tools.Utils;
import org.wikipedia.WikipediaApp;
import org.wikipedia.data.WikipediaLangArticle;
import org.wikipedia.gui.GuiUtils;

public class FetchWikidataAction extends JosmAction {

    public FetchWikidataAction() {
        super(tr("Fetch Wikidata IDs"), "dialogs/wikidata",
                tr("Fetch Wikidata IDs using the ''wikipedia'' tag"), null, true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        DataSet ds = getLayerManager().getEditDataSet();
        if (ds == null) {
            return;
        }
        Main.worker.submit(new Fetcher(ds.getSelected()));
    }

    public static class Fetcher extends PleaseWaitRunnable {
        private final Collection<? extends OsmPrimitive> selection;
        private boolean canceled = false;
        private final List<Command> commands = new ArrayList<>();
        private final Collection<WikipediaLangArticle> notFound = new ArrayList<>();

        public Fetcher(Collection<? extends OsmPrimitive> selection) {
            super(tr("Fetching Wikidata IDs"));
            this.selection = selection;
        }

        @Override
        protected void cancel() {
            canceled = true;
        }

        @Override
        protected void realRun() {
            final Map<String, PrimitivesWithWikipedia> wikipediaByLanguage = getLanguageToArticlesMap(selection);
            getProgressMonitor().setTicksCount(wikipediaByLanguage.keySet().size());
            for (final Map.Entry<String, PrimitivesWithWikipedia> i : wikipediaByLanguage.entrySet()) {
                if (canceled) {
                    break;
                }
                final PrimitivesWithWikipedia fetcher = i.getValue();
                fetcher.updateWikidataIds(getProgressMonitor().createSubTaskMonitor(1, false));
                final Command command = fetcher.getCommand();
                if (command != null) {
                    commands.add(command);
                }
                notFound.addAll(fetcher.getNotFound());
            }
        }

        static Map<String, PrimitivesWithWikipedia> getLanguageToArticlesMap(final Iterable<? extends OsmPrimitive> selection) {
            final Map<String, PrimitivesWithWikipedia> r = new HashMap<>();
            for (final OsmPrimitive i : selection) {
                final WikipediaLangArticle tag = WikipediaLangArticle.parseTag("wikipedia", i.get("wikipedia"));
                if (tag != null) {
                    if (!r.containsKey(tag.lang)) {
                        r.put(tag.lang, new PrimitivesWithWikipedia(tag.lang));
                    }
                    r.get(tag.lang).put(i, tag.article);
                }
            }
            return r;
        }

        @Override
        protected void finish() {
            if (!canceled && !commands.isEmpty()) {
                Main.main.undoRedo.add(commands.size() == 1 ? commands.get(0) : new SequenceCommand(tr("Add Wikidata"), commands));
            }
            if (!canceled && !notFound.isEmpty()) {
                new Notification(tr("No Wikidata ID found for: {0}", Utils.joinAsHtmlUnorderedList(notFound)))
                        .setIcon(JOptionPane.WARNING_MESSAGE)
                        .setDuration(Notification.TIME_LONG)
                        .show();
            }
        }
    }

    private static class PrimitivesWithWikipedia {
        final String lang;
        final MultiMap<String, OsmPrimitive> byArticle = new MultiMap<>();
        final List<Command> commands = new ArrayList<>();
        final List<WikipediaLangArticle> notFound = new ArrayList<>();

        PrimitivesWithWikipedia(String lang) {
            this.lang = lang;
        }

        public void put(OsmPrimitive key, String wikipedia) {
            byArticle.put(wikipedia, key);
        }

        void updateWikidataIds(ProgressMonitor monitor) {
            final int size = byArticle.keySet().size();
            monitor.beginTask(trn(
                    "Fetching {0} Wikidata ID for language ''{1}''",
                    "Fetching {0} Wikidata IDs for language ''{1}''", size, size, lang));
            final Map<String, String> wikidataByWikipedia = WikipediaApp.getWikidataForArticles(lang, new ArrayList<>(byArticle.keySet()));
            ConditionalOptionPaneUtil.startBulkOperation(GuiUtils.PREF_OVERWRITE);
            for (Map.Entry<String, Set<OsmPrimitive>> i : byArticle.entrySet()) {
                final String wikipedia = i.getKey();
                final String wikidata = wikidataByWikipedia.get(wikipedia);
                if (wikidata != null) {
                    if (GuiUtils.confirmOverwrite("wikidata", wikidata, i.getValue())) {
                        commands.add(new ChangePropertyCommand(i.getValue(), "wikidata", wikidata));
                    }
                } else {
                    final WikipediaLangArticle article = new WikipediaLangArticle(lang, wikipedia);
                    Main.warn(tr("No Wikidata ID found for: {0}", article));
                    notFound.add(article);
                }
            }
            ConditionalOptionPaneUtil.endBulkOperation(GuiUtils.PREF_OVERWRITE);
            monitor.finishTask();
        }

        public Command getCommand() {
            return commands.isEmpty()
                    ? null
                    : new SequenceCommand(tr("Add Wikidata for language ''{0}''", lang), commands);
        }

        List<WikipediaLangArticle> getNotFound() {
            return notFound;
        }
    }

    @Override
    protected void updateEnabledState() {
        updateEnabledStateOnCurrentSelection();
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        for (final OsmPrimitive i : selection) {
            if (i.hasKey("wikipedia")) {
                setEnabled(true);
                return;
            }
        }
        setEnabled(false);
    }
}
