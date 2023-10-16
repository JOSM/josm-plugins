// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.core.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.text.WordUtils;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.plugins.opendata.core.OdConstants;
import org.openstreetmap.josm.plugins.opendata.core.datasets.SimpleDataSetHandler;
import org.openstreetmap.josm.tools.Logging;

/**
 * Utilities for French names.
 */
public abstract class NamesFrUtils {

    private static final Map<String, String> dictionary = initDictionary();

    public static String checkDictionary(String value) {
        StringBuilder result = new StringBuilder();
        for (String word : value.split(" ")) {
            if (result.length() != 0) {
                result.append(' ');
            }
            result.append(dictionary.getOrDefault(word, word));
        }
        return result.toString();
    }

    private static Map<String, String> initDictionary() {
        Map<String, String> result = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                SimpleDataSetHandler.class.getResourceAsStream(OdConstants.DICTIONARY_FR), StandardCharsets.UTF_8))) {
            String line = reader.readLine(); // Skip first line
            Logging.trace(line);
            while ((line = reader.readLine()) != null) {
                String[] tab = line.split(";");
                result.put(tab[0].replace("\"", ""), tab[1].replace("\"", ""));
            }
        } catch (IOException e) {
            Logging.error(e);
        }
        return result;
    }

    public static String getStreetLabel(String label) {
        if (label == null) {
            return null;
        } else if (label.startsWith("All")) {
            return "Allée";
        } else if ("Autoroute".equals(label)) {
            return label;
        } else if (label.startsWith("Anc")) {
            return "Ancien";
        } else if (label.startsWith("Av")) {
            return "Avenue";
        } else if (label.startsWith("Barr")) {
            return "Barrière";
        } else if ("Bd".equals(label) || "Boulevard".equals(label)) {
            return "Boulevard";
        } else if (label.startsWith("Bret")) {
            return "Bretelle";
        } else if ("Bre".equals(label)) {
            return "Bré";
        } else if ("Caminot".equals(label)) {
            return label;
        } else if ("Carrefour".equals(label)) {
            return label;
        } else if ("Carré".equals(label)) {
            return label;
        } else if (label.startsWith("Chemine")) {
            return "Cheminement";
        } else if (label.startsWith("Che")) {
            return "Chemin";
        } else if (label.startsWith("Cit")) {
            return "Cité";
        } else if ("Clos".equals(label)) {
            return label;
        } else if ("Cote".equals(label) || "Côte".equals(label)) {
            return "Côte";
        } else if ("Cours".equals(label)) {
            return label;
        } else if (label.startsWith("Dep") || label.startsWith("Dép")) {
            return "Départementale";
        } else if (label.startsWith("Dom")) {
            return "Domaine";
        } else if ("Dsc".equals(label) || label.startsWith("Desc")) {
            return "Descente";
        } else if ("Esp".equals(label) || label.startsWith("Espl")) {
            return "Esplanade";
        } else if (label.startsWith("Espa")) {
            return "Espace";
        } else if ("Giratoire".equals(label)) {
            return label;
        } else if ("Grande-rue".equals(label)) {
            return label;
        } else if ("Hameau".equals(label)) {
            return label;
        } else if (label.startsWith("Imp") || "Ipasse".equals(label)) {
            return "Impasse";
        } else if (label.startsWith("Itin")) {
            return "Itinéraire";
        } else if ("Jardin".equals(label)) {
            return label;
        } else if (label.startsWith("L'") || "La".equals(label) || "Le".equals(label) || "Les".equals(label) ||
                "Saint".equals(label)) { // Lieux-dits
            return label;
        } else if (label.startsWith("Lot")) {
            return "Lotissement";
        } else if ("Mail".equals(label)) {
            return label;
        } else if ("Mas".equals(label)) {
            return label;
        } else if (label.startsWith("Nat")) {
            return "Nationale";
        } else if ("Parc".equals(label)) {
            return label;
        } else if ("Passerelle".equals(label)) {
            return label;
        } else if (label.startsWith("Pas")) {
            return "Passage";
        } else if ("Pch".equals(label) || label.startsWith("Petit-chem")) {
            return "Petit-chemin";
        } else if ("Petit".equals(label) || "Petite".equals(label)) {
            return label;
        } else if ("Petite-allée".equals(label)) {
            return label;
        } else if ("Petite-rue".equals(label)) {
            return label;
        } else if ("Plan".equals(label)) {
            return label;
        } else if (label.startsWith("Pl")) {
            return "Place";
        } else if ("Pont".equals(label)) {
            return label;
        } else if ("Port".equals(label)) {
            return label;
        } else if ("Porte".equals(label)) {
            return label;
        } else if (label.startsWith("Prom")) {
            return "Promenade";
        } else if ("Prv".equals(label) || label.startsWith("Parv")) {
            return "Parvis";
        } else if (label.startsWith("Qu")) {
            return "Quai";
        } else if ("Rampe".equals(label)) {
            return label;
        } else if (label.startsWith("Res") || label.startsWith("Rés")) {
            return "Résidence";
        } else if ("Rocade".equals(label)) {
            return label;
        } else if ("Rpt".equals(label) || label.startsWith("Ron")) {
            return "Rond-Point";
        } else if ("Rte".equals(label) || "Route".equals(label)) {
            return "Route";
        } else if ("Rue".equals(label) || "Rued".equals(label)) {
            return "Rue";
        } else if ("Sentier".equals(label)) {
            return label;
        } else if (label.startsWith("Sq")) {
            return "Square";
        } else if ("Théâtre".equals(label)) {
            return "Théâtre";
        } else if (label.startsWith("Tra")) {
            return "Traverse";
        } else if ("Vieux".equals(label)) {
            return label;
        } else if ("Voie".equals(label)) {
            return label;
        } else if ("Zone".equals(label)) {
            return label;
        } else {
            Logging.warn("unknown street label: "+label);
            return label;
        }
    }

    public static String checkStreetName(OsmPrimitive p, String key) {
        String value = null;
        if (p != null) {
            value = p.get(key);
            if (value != null) {
                value = WordUtils.capitalizeFully(value);
                // Cas particuliers
                if ("Boulingrin".equals(value)) { // square Boulingrin, mal formé
                    value = "Sq Boulingrin";
                } else if (value.matches("A[0-9]+")) { // Autoroutes sans le mot "Autoroute"
                    value = "Autoroute "+value;
                } else if ("All A61".equals(value)) { // A61 qualifiée d'Allée ?
                    value = "Autoroute A61";
                } else if (value.startsWith("Che Vieux Che")) { // "Che" redondant
                    value = value.replaceFirst("Che ", "");
                } else if (value.startsWith("Petite Allee ")) { // Tiret, comme grand-rue, petite-rue
                    value = value.replaceFirst("Petite Allee ", "Petite-allée ");
                } else if (value.startsWith("Ld De ")) { // Lieux-dit
                    value = value.replaceFirst("Ld De ", "");
                }
                while (value.startsWith("Ld ")) { // Lieux-dit, inutile. Plus le cas avec "Ld Ld"
                    value = value.replaceFirst("Ld ", "");
                }
                if (value.startsWith("L ")) {
                    value = value.replaceFirst("L ", "L'");
                }
                String[] words = value.split(" ");
                if (words.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder();
                    List<String> list = Arrays.asList(words);
                    words[0] = getStreetLabel(words[0]);
                    if ("Ancien".equals(words[0]) && words.length > 1 && "Che".equals(words[1])) {
                        words[1] = "Chemin";
                    }
                    for (int i = 0; i < words.length; i++) {
                        if (i > 0) {
                            stringBuilder.append(' ');
                            // Prénoms/Noms propres abrégés
                            if ("A".equals(words[i]) && list.contains("Bernard")) {
                                words[i] = "Arnaud";
                            } else if ("A".equals(words[i]) && list.contains("Passerieu")) {
                                words[i] = "Ariste";
                            } else if ("A".equals(words[i]) && list.contains("Bougainville")) {
                                words[i] = "Antoine";
                            } else if ("Ch".equals(words[i]) && list.contains("Leconte")) {
                                words[i] = "Charles";
                            } else if ("Frs".equals(words[i]) && list.contains("Dugua")) {
                                words[i] = "François";
                            } else if ("G".equals(words[i]) && list.contains("Latecoere")) {
                                words[i] = "Georges";
                            } else if ("H".equals(words[i]) && list.contains("Lautrec")) {
                                words[i] = "Henri";
                            } else if ("J".equals(words[i]) && list.contains("Dieulafoy")) {
                                words[i] = "Jane";
                            } else if ("J".equals(words[i]) && (list.contains("Champollion") || list.contains("Stanislas"))) {
                                words[i] = "Jean";
                            } else if ("L".equals(words[i]) && list.contains("Zamenhof")) {
                                words[i] = "Ludwik";
                            } else if ("L".equals(words[i]) && list.contains("Sacha")) {
                                words[i] = "Lucien";
                                if (!list.contains("Et")) {
                                    words[i] += " et";
                                }
                            } else if ("L".equals(words[i]) && (list.contains("Vauquelin") || list.contains("Bougainville"))) {
                                words[i] = "Louis";
                            } else if ("M".equals(words[i]) && list.contains("Dieulafoy")) {
                                words[i] = "Marcel";
                            } else if ("M".equals(words[i]) && list.contains("Arifat")) {
                                words[i] = "Marie";
                            } else if ("N".equals(words[i]) && list.contains("Djamena")) {
                                words[i] = "N'";
                            } else if ("Oo".equals(words[i])) {
                                words[i] = "Oô";
                            } else if ("Ph".equals(words[i]) && list.contains("Ravary")) {
                                words[i] = "Philippe";
                            } else if ("R".equals(words[i]) && list.contains("Folliot")) {
                                words[i] = "Raphaël";
                            } else if ("W".equals(words[i]) && list.contains("Booth")) {
                                words[i] = "William";
                            // Mots de liaison non couverts par le dictionnaire
                            } else if ("A".equals(words[i])) {
                                words[i] = "à";
                            } else if ("D".equals(words[i]) || "L".equals(words[i])) {
                                words[i] = words[i].toLowerCase(Locale.FRENCH)+"'";
                            } else if ("La".equals(words[i]) || "Le".equals(words[i])) {
                                words[i] = words[i].toLowerCase(Locale.FRENCH);
                            }
                        }
                        stringBuilder.append(words[i]);
                    }
                    value = stringBuilder.toString();
                }
                // Ponctuation
                value = value.replace("' ", "'");
                // Dictionnaire
                value = checkDictionary(value);
                p.put(key, value);
            }
        }
        return value;
    }
}
