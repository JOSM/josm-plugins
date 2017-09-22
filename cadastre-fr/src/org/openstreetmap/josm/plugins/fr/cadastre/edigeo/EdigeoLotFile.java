// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fr.cadastre.edigeo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.plugins.fr.cadastre.edigeo.EdigeoFileTHF.ChildBlock;
import org.openstreetmap.josm.plugins.fr.cadastre.edigeo.EdigeoFileTHF.Lot;
import org.openstreetmap.josm.plugins.fr.cadastre.edigeo.utils.ClassToInstancesMap;
import org.openstreetmap.josm.plugins.fr.cadastre.edigeo.utils.MutableClassToInstancesMap;

/**
 * An Egideo file belonging to a geographic lot.
 * @param <B> superclass of all blocks defined in this file
 */
public abstract class EdigeoLotFile<B extends ChildBlock> extends EdigeoFile {

    protected final Lot lot;
    private final String subsetId;

    private final Map<String, Class<? extends B>> classes = new HashMap<>();
    protected final ClassToInstancesMap<B> blocks = new MutableClassToInstancesMap<>();

    EdigeoLotFile(Lot lot, String subsetId, Path path) throws IOException {
        super(path);
        this.lot = Objects.requireNonNull(lot, "lot");
        this.subsetId = Objects.requireNonNull(subsetId, "subsetId");
    }

    protected final void register(String key, Class<? extends B> klass) {
        classes.put(key, klass);
        blocks.putInstances(klass, new ArrayList<>());
    }

    @Override
    protected final Block createBlock(String type) throws ReflectiveOperationException {
        Class<? extends B> klass = classes.get(type);
        return addBlock(blocks.get(klass), klass.getDeclaredConstructor(Lot.class, String.class).newInstance(lot, type));
    }

    @Override
    public EdigeoLotFile<B> read(DataSet ds) throws IOException, ReflectiveOperationException {
        super.read(ds);
        return this;
    }

    @Override
    boolean isValid() {
        return blocks.values().stream().allMatch(l -> l.stream().allMatch(Block::isValid));
    }

    /**
     * Finds a descriptor by its identifier.
     * @param values 4 values obtained from a {@link EdigeoRecord}:<ol>
     * <li>Lot identifier</li>
     * <li>subset identifier</li>
     * <li>Descriptor type</li>
     * <li>Descriptor identifier</li></ol>
     * @param klass descriptor class
     * @return found descriptor
     */
    public final B find(List<String> values) {
        assert values.size() == 4 : values;
        return find(values, classes.get(values.get(2)));
    }

    /**
     * Finds a descriptor by its identifier.
     * @param values 4 values obtained from a {@link EdigeoRecord}:<ol>
     * <li>Lot identifier</li>
     * <li>subset identifier</li>
     * <li>Descriptor type</li>
     * <li>Descriptor identifier</li></ol>
     * @param klass descriptor class
     * @return found descriptor
     */
    @SuppressWarnings("unchecked")
    public final <T extends B> T find(List<String> values, Class<T> klass) {
        assert values.size() == 4 : values;
        assert values.get(0).equals(lot.identifier) : values;
        assert values.get(1).equals(subsetId) : values;
        assert klass.isAssignableFrom(classes.get(values.get(2))) : values;
        List<T> list = blocks.getInstances(klass);
        if (list == null) {
            Class<? extends B> realClass = classes.get(values.get(2));
            if (klass.isAssignableFrom(realClass)) {
                list = (List<T>) blocks.getInstances(realClass);
            } else {
                throw new IllegalArgumentException(values + " / " + klass + " / " + realClass);
            }
        }
        return list.stream().filter(x -> x.identifier.equals(values.get(3))).findAny().orElseThrow(
                () -> new IllegalArgumentException(values + " / " + klass));
    }
}
