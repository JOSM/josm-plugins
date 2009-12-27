package org.openstreetmap.josm.plugins.graphview.plugin.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.plugins.graphview.core.data.DataSource;
import org.openstreetmap.josm.plugins.graphview.core.data.DataSourceObserver;
import org.openstreetmap.josm.plugins.graphview.core.data.MapBasedTagGroup;
import org.openstreetmap.josm.plugins.graphview.core.data.TagGroup;

/**
 * DataSource that gets data from JOSM;
 * this DataSource type does not send updates!
 */

public class JOSMDataSource implements DataSource<Node, Way, Relation, RelationMember> {

	public double getLat(Node node) {
		return node.getCoor().lat();
	}

	public double getLon(Node node) {
		return node.getCoor().lon();
	}

	public Iterable<RelationMember> getMembers(Relation relation) {
		return relation.getMembers();
	}

	public Iterable<Node> getNodes(Way way) {
		return new FilteredOsmPrimitiveIterable<Node>(way.getNodes());
	}

	public Iterable<Node> getNodes() {
		return new FilteredOsmPrimitiveIterable<Node>(Main.main.getCurrentDataSet().getNodes());
	}

	public Iterable<Relation> getRelations() {
		return new FilteredRelationIterable(Main.main.getCurrentDataSet().getRelations());
	}

	public Iterable<Way> getWays() {
		return new FilteredOsmPrimitiveIterable<Way>(Main.main.getCurrentDataSet().getWays());
	}

	public TagGroup getTagsN(Node node) {
		return getTags(node);
	}

	public TagGroup getTagsW(Way way) {
		return getTags(way);
	}

	public TagGroup getTagsR(Relation relation) {
		return getTags(relation);
	}

	private TagGroup getTags(OsmPrimitive primitive) {
		if (primitive.getKeys() == null) {
			return EMPTY_TAG_GROUP;
		} else {
			return new MapBasedTagGroup(primitive.getKeys());
		}
	}
	
	public Object getMember(RelationMember member) {
		return member.getMember();
	}
	
	public String getRole(RelationMember member) {
		return member.getRole();
	}
	
	public boolean isNMember(RelationMember member) {
		return member.getMember() instanceof Node;
	}
	
	public boolean isWMember(RelationMember member) {
		return member.getMember() instanceof Way;
	}
	
	public boolean isRMember(RelationMember member) {
		return member.getMember() instanceof Relation;
	}
	

	private static final TagGroup EMPTY_TAG_GROUP;
	static {
		Map<String, String> emptyMap = new HashMap<String, String>(0);
		EMPTY_TAG_GROUP = new MapBasedTagGroup(emptyMap);
	}

	/**
	 * Iterable of OsmPrimitive objects based on an existing Iterable,
	 * will filter incomplete and deleted objects from the iterator.
	 *
	 * @param <P>  OsmPrimitive subtype
	 */
	public static class FilteredOsmPrimitiveIterable<P extends OsmPrimitive> implements Iterable<P> {

		private final Iterable<P> originalIterable;

		public FilteredOsmPrimitiveIterable(Iterable<P> originalIterable) {
			this.originalIterable = originalIterable;
		}

		/** returns an iterator. The iterator does not support {@link Iterator#remove()}. */
		public Iterator<P> iterator() {
			return new FilteredIterator(originalIterable.iterator());
		}

		private class FilteredIterator implements Iterator<P> {

			private final Iterator<P> originalIterator;

			private P next;

			public FilteredIterator(Iterator<P> originalIterator) {
				this.originalIterator = originalIterator;
				updateNext();
			}

			public boolean hasNext() {
				return next != null;
			}

			public P next() {
				if (next != null) {
					P result = next;
					updateNext();
					return result;
				} else {
					throw new NoSuchElementException();
				}
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}

			private void updateNext() {
				if (originalIterator.hasNext()) {
					next = originalIterator.next();
					if (!accept(next)) {
						updateNext();
					}
				} else {
					next = null;
				}
			}

		}

		protected boolean accept(P primitive) {
			return !primitive.isDeleted() && !primitive.isIncomplete();
		}
	}

	/**
	 * Relation-specific variant of the FilteredOsmPrimitiveIterable,
	 * also checks completeness of relation's members
	 */
	public static class FilteredRelationIterable extends FilteredOsmPrimitiveIterable<Relation> {

		public FilteredRelationIterable(Iterable<Relation> originalIterable) {
			super(originalIterable);
		}

		@Override
		protected boolean accept(Relation relation) {
			boolean complete = true;
			for (org.openstreetmap.josm.data.osm.RelationMember member : relation.getMembers()) {
				if (member.getMember() == null || member.getMember().isDeleted() || member.getMember().isIncomplete()) {
					complete = false;
				}
			}
			return complete && super.accept(relation);
		}
	}

	static class RelationMemberImpl {
		private final String role;
		private final Object member;
		public RelationMemberImpl(org.openstreetmap.josm.data.osm.RelationMember originalMember) {
			this.role = originalMember.getRole();
			this.member = originalMember.getMember();
		}
		public String getRole() {
			return role;
		}
		public Object getMember() {
			return member;
		}
	}

	private final Set<DataSourceObserver> observers = new HashSet<DataSourceObserver>();

	public void addObserver(DataSourceObserver observer) {
		observers.add(observer);
	}

	public void deleteObserver(DataSourceObserver observer) {
		observers.remove(observer);
	}

}
