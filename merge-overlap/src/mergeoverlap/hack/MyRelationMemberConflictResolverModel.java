// License: GPL. For details, see LICENSE file.
package mergeoverlap.hack;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.table.DefaultTableModel;

import mergeoverlap.MergeOverlapAction;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.RelationToChildReference;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.conflict.tags.RelationMemberConflictDecision;
import org.openstreetmap.josm.gui.conflict.tags.RelationMemberConflictDecisionType;

/**
     * This model manages a list of conflicting relation members.
     *
     * It can be used as {@see TableModel}.
     *
     *
     */
    public class MyRelationMemberConflictResolverModel extends DefaultTableModel {
        /** the property name for the number conflicts managed by this model */
        static public final String NUM_CONFLICTS_PROP = MyRelationMemberConflictResolverModel.class.getName() + ".numConflicts";

        /** the list of conflict decisions */
        private List<RelationMemberConflictDecision> decisions;
        /** the collection of relations for which we manage conflicts */
        private Collection<Relation> relations;
        /** the number of conflicts */
        private int numConflicts;
        private PropertyChangeSupport support;

        /**
         * Replies the current number of conflicts
         *
         * @return the current number of conflicts
         */
        public int getNumConflicts() {
            return numConflicts;
        }

        /**
         * Updates the current number of conflicts from list of decisions and emits
         * a property change event if necessary.
         *
         */
        protected void updateNumConflicts() {
            int count = 0;
            for (RelationMemberConflictDecision decision: decisions) {
                if (!decision.isDecided()) {
                    count++;
                }
            }
            int oldValue = numConflicts;
            numConflicts = count;
            if (numConflicts != oldValue) {
                support.firePropertyChange(NUM_CONFLICTS_PROP, oldValue, numConflicts);
            }
        }

        public void addPropertyChangeListener(PropertyChangeListener l) {
            support.addPropertyChangeListener(l);
        }

        public void removePropertyChangeListener(PropertyChangeListener l) {
            support.removePropertyChangeListener(l);
        }

        public MyRelationMemberConflictResolverModel() {
            decisions = new ArrayList<RelationMemberConflictDecision>();
            support = new PropertyChangeSupport(this);
        }

        @Override
        public int getRowCount() {
            if (decisions == null) return 0;
            return decisions.size();
        }

        @Override
        public Object getValueAt(int row, int column) {
            if (decisions == null) return null;

            RelationMemberConflictDecision d = decisions.get(row);
            switch(column) {
            case 0: /* relation */ return d.getRelation();
            case 1: /* pos */ return Integer.toString(d.getPos() + 1); // position in "user space" starting at 1
            case 2: /* role */ return d.getRole();
            case 3: /* original */ return d.getOriginalPrimitive();
            case 4: /* decision */ return d.getDecision();
            }
            return null;
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            RelationMemberConflictDecision d = decisions.get(row);
            switch(column) {
            case 2: /* role */
                d.setRole((String)value);
                break;
            case 4: /* decision */
                d.decide((RelationMemberConflictDecisionType)value);
                refresh();
                break;
            }
            fireTableDataChanged();
        }

        /**
         * Populates the model with the members of the relation <code>relation</code>
         * referring to <code>primitive</code>.
         *
         * @param relation the parent relation
         * @param primitive the child primitive
         */
        protected void populate(Relation relation, OsmPrimitive primitive, Map<Way, Way> oldWays) {
            for (int i = 0; i<relation.getMembersCount(); i++) {
                if (MergeOverlapAction.getOld(relation.getMember(i).getWay(), oldWays) == MergeOverlapAction.getOld((Way)primitive, oldWays)) {
                    decisions.add(new RelationMemberConflictDecision(relation, i));
                }
            }
        }

        /**
         * Populates the model with the relation members belonging to one of the relations in <code>relations</code>
         * and referring to one of the primitives in <code>memberPrimitives</code>.
         *
         * @param relations  the parent relations. Empty list assumed if null.
         * @param memberPrimitives the child primitives. Empty list assumed if null.
         */
        public void populate(Collection<Relation> relations, Collection<? extends OsmPrimitive> memberPrimitives, Map<Way, Way> oldWays) {
            decisions.clear();
        	
            relations = relations == null ? new LinkedList<Relation>() : relations;
            memberPrimitives = memberPrimitives == null ? new LinkedList<OsmPrimitive>() : memberPrimitives;
            for (Relation r : relations) {
                for (OsmPrimitive p: memberPrimitives) {
                    populate(r, p, oldWays);
                }
            }
            this.relations = relations;
            refresh();
        }

        /**
         * Populates the model with the relation members represented as a collection of
         * {@see RelationToChildReference}s.
         *
         * @param references the references. Empty list assumed if null.
         */
        public void populate(Collection<RelationToChildReference> references) {
            references = references == null ? new LinkedList<RelationToChildReference>() : references;
            decisions.clear();
            this.relations = new HashSet<Relation>(references.size());
            for (RelationToChildReference reference: references) {
                decisions.add(new RelationMemberConflictDecision(reference.getParent(), reference.getPosition()));
                relations.add(reference.getParent());
            }
            refresh();
        }

        /**
         * Replies the decision at position <code>row</code>
         *
         * @param row
         * @return the decision at position <code>row</code>
         */
        public RelationMemberConflictDecision getDecision(int row) {
            return decisions.get(row);
        }

        /**
         * Replies the number of decisions managed by this model
         *
         * @return the number of decisions managed by this model
         */
        public int getNumDecisions() {
            return  getRowCount();
        }

        /**
         * Refreshes the model state. Invoke this method to trigger necessary change
         * events after an update of the model data.
         *
         */
        public void refresh() {
            updateNumConflicts();
            fireTableDataChanged();
        }

        /**
         * Apply a role to all member managed by this model.
         *
         * @param role the role. Empty string assumed if null.
         */
        public void applyRole(String role) {
            role = role == null ? "" : role;
            for (RelationMemberConflictDecision decision : decisions) {
                decision.setRole(role);
            }
            refresh();
        }

        protected RelationMemberConflictDecision getDecision(Relation relation, int pos) {
            for(RelationMemberConflictDecision decision: decisions) {
                if (decision.matches(relation, pos)) return decision;
            }
            return null;
        }

        protected void buildResolveCorrespondance(Relation relation, OsmPrimitive newPrimitive, Map<Relation, Relation> newRelations, Map<Way, Way> oldWays) {

        	List<RelationMember> relationsMembers = relation.getMembers();
        	Relation modifiedRelation = MergeOverlapAction.getNew(relation, newRelations);
            modifiedRelation.setMembers(null);
//            boolean isChanged = false;
            for (int i=0; i < relationsMembers.size(); i++) {
            	RelationMember rm = relationsMembers.get(i);
//                RelationMember rm = relation.getMember(i);
//                RelationMember rmNew;
                RelationMemberConflictDecision decision = getDecision(relation, i);
                if (decision == null) {
                    modifiedRelation.addMember(rm);
                } else {
                	System.out.println(modifiedRelation);
                	System.out.println(111);
                    switch(decision.getDecision()) {
                    case KEEP:
//                    	modifiedRelation.removeMembersFor(newPrimitive);
                    	System.out.println(222);
                    	if (newPrimitive instanceof Way) {
	                        modifiedRelation.addMember(new RelationMember(decision.getRole(), MergeOverlapAction.getOld((Way)newPrimitive, oldWays)));
                    	}
                    	else {
                    		modifiedRelation.addMember(new RelationMember(decision.getRole(), newPrimitive));
                    	}
//                    	modifiedRelation.addMember(new RelationMember(decision.getRole(), newPrimitive));
                        break;
                    case REMOVE:
                    	System.out.println(333);
//                    	modifiedRelation.removeMembersFor(rm.getMember());
//                        isChanged = true;
                        // do nothing
                        break;
                    case UNDECIDED:
                        // FIXME: this is an error
                        break;
                    }
                }
            }
        }

        /**
         * Builds a collection of commands executing the decisions made in this model.
         *
         * @param newPrimitive the primitive which members shall refer to if the
         * decision is {@see RelationMemberConflictDecisionType#REPLACE}
         * @return a list of commands
         */
        public void buildRelationCorrespondance(OsmPrimitive newPrimitive, Map<Relation, Relation> newRelations, Map<Way, Way> oldWays) {
            for (Relation relation : relations) {
            	buildResolveCorrespondance(relation, newPrimitive, newRelations, oldWays);
            }
        }

        protected boolean isChanged(Relation relation, OsmPrimitive newPrimitive) {
            for (int i=0; i < relation.getMembersCount(); i++) {
                RelationMemberConflictDecision decision = getDecision(relation, i);
                if (decision == null) {
                    continue;
                }
                switch(decision.getDecision()) {
                case REMOVE: return true;
                case KEEP:
                    if (!relation.getMember(i).getRole().equals(decision.getRole()))
                        return true;
                    if (relation.getMember(i).getMember() != newPrimitive)
                        return true;
                case UNDECIDED:
                    // FIXME: handle error
                }
            }
            return false;
        }

        /**
         * Replies the set of relations which have to be modified according
         * to the decisions managed by this model.
         *
         * @param newPrimitive the primitive which members shall refer to if the
         * decision is {@see RelationMemberConflictDecisionType#REPLACE}
         *
         * @return the set of relations which have to be modified according
         * to the decisions managed by this model
         */
        public Set<Relation> getModifiedRelations(OsmPrimitive newPrimitive) {
            HashSet<Relation> ret = new HashSet<Relation>();
            for (Relation relation: relations) {
                if (isChanged(relation, newPrimitive)) {
                    ret.add(relation);
                }
            }
            return ret;
        }
    }