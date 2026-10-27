/* This file is part of the OWL API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright 2014, The University of Manchester
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License. */
package org.semanticweb.owlapi.rdfarray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.reasoner.impl.OWLNamedIndividualNodeSet;
import org.semanticweb.owlapi.util.Version;

/**
 * rdf-array: an {@link OWLReasoner} that gives a stock reasoner the meaning of sequences
 * (owl-array.md §6). It hands the delegate an in-memory ontology, in a manager of its own, that
 * holds the source's imports closure with every sequence replaced by its expansion individual, the
 * expansion of every sequence and the vocabulary's axioms; the source ontology is never changed.
 * Queries are rewritten the same way. Results are individuals of the source: the expansion
 * individuals are left out of {@link #getInstances} and the like, and {@link #getSequenceInstances}
 * and {@link #getTypes(OWLSequence, boolean)} answer for sequences. A change to the source is taken
 * into account at the next {@link #flush()}, or at once for a non-buffering reasoner.
 */
public class SequenceAwareReasoner implements OWLReasoner {

    @Nonnull
    private final OWLOntology source;
    @Nonnull
    private final OWLOntologyManager manager;
    @Nonnull
    private final OWLReasonerFactory factory;
    @Nonnull
    private final OWLReasonerConfiguration config;
    private final boolean closure;
    private final boolean buffering;
    private final OWLOntologyChangeListener listener = changes -> markDirty(changes);
    private final List<OWLOntologyChange> pending = new ArrayList<>();
    private boolean dirty = true;
    private SequenceAxioms sequences;
    private OWLOntology expanded;
    private OWLReasoner delegate;

    /**
     * @param source the ontology to reason over
     * @param factory the factory of the stock reasoner
     * @param config the reasoner configuration
     * @param closure whether to add the closure axioms (false for EL)
     * @param buffering whether changes wait for {@link #flush()}
     */
    public SequenceAwareReasoner(@Nonnull OWLOntology source, @Nonnull OWLReasonerFactory factory,
        @Nonnull OWLReasonerConfiguration config, boolean closure, boolean buffering) {
        this.source = source;
        // kept: a removed ontology forgets its manager, and dispose() must still find the listener
        this.manager = source.getOWLOntologyManager();
        this.factory = factory;
        this.config = config;
        this.closure = closure;
        this.buffering = buffering;
        manager.addOntologyChangeListener(listener);
        rebuild();
    }

    private void markDirty(List<? extends OWLOntologyChange> changes) {
        if (!manager.contains(source)) {
            // the source was removed from its manager and this reasoner not disposed: the changes
            // of the manager's other ontologies are not ours, and getImportsClosure() would throw
            return;
        }
        Set<OWLOntology> closureSet = source.getImportsClosure();
        for (OWLOntologyChange c : changes) {
            if (closureSet.contains(c.getOntology())) {
                pending.add(c);
                dirty = true;
            }
        }
    }

    private void rebuild() {
        try {
            OWLOntologyManager m = OWLManager.createOWLOntologyManager();
            sequences = new SequenceAxioms(m.getOWLDataFactory(), closure);
            Set<OWLAxiom> axioms = new HashSet<>(source.getAxioms(Imports.INCLUDED));
            expanded = m.createOntology(sequences.expand(axioms));
            if (delegate != null) {
                delegate.dispose();
            }
            delegate = buffering ? factory.createReasoner(expanded, config)
                : factory.createNonBufferingReasoner(expanded, config);
            pending.clear();
            dirty = false;
        } catch (OWLOntologyCreationException e) {
            throw new OWLRuntimeException(e);
        }
    }

    private OWLReasoner d() {
        if (dirty && !buffering) {
            rebuild();
        }
        return delegate;
    }

    private <T extends OWLObject> T in(T o) {
        return sequences.rewrite(o);
    }

    private OWLIndividual in(OWLIndividual i) {
        return i instanceof OWLSequence ? sequences.expansionOf((OWLSequence) i) : i;
    }

    // Expansion individuals are not individuals of the source; leave them out of results
    private NodeSet<OWLNamedIndividual> out(NodeSet<OWLNamedIndividual> ns) {
        Set<Node<OWLNamedIndividual>> nodes = new HashSet<>();
        for (Node<OWLNamedIndividual> n : ns.getNodes()) {
            Set<OWLNamedIndividual> kept = new HashSet<>();
            for (OWLNamedIndividual i : n.getEntities()) {
                if (sequences.sequenceOf(i) == null) {
                    kept.add(i);
                }
            }
            if (!kept.isEmpty()) {
                nodes.add(new org.semanticweb.owlapi.reasoner.impl.OWLNamedIndividualNode(kept));
            }
        }
        return new OWLNamedIndividualNodeSet(nodes);
    }

    // --- sequences ----------------------------------------------------------------------------

    /**
     * @param ce a class expression
     * @param direct whether direct instances only
     * @return the sequences, among those in the ontology, that are instances of it. Only sequences
     *         whose expansion individual is named, those that hold no anonymous individual, are
     *         answered: a stock reasoner reads a class assertion on an anonymous individual
     *         existentially (HermiT entails {@code ClassAssertion(C, _:x)} as soon as some C exists),
     *         so asking it of an anonymous expansion individual would return every such sequence.
     *         A sequence holding a blank node is never returned until rdf-array/rdf-array#20 is
     *         decided.
     */
    public Set<OWLSequence> getSequenceInstances(@Nonnull OWLClassExpression ce, boolean direct) {
        Set<OWLSequence> result = new HashSet<>();
        NodeSet<OWLNamedIndividual> instances = null;
        for (OWLSequence s : sequences.getExpansion().keySet()) {
            OWLIndividual x = sequences.expansionOf(s);
            if (x.isNamed()) {
                if (instances == null) {
                    instances = d().getInstances(in(ce), direct);
                }
                if (instances.containsEntity(x.asOWLNamedIndividual())) {
                    result.add(s);
                }
            }
        }
        return result;
    }

    /**
     * @param s a sequence in the ontology
     * @param direct whether direct types only
     * @return its types
     */
    public NodeSet<OWLClass> getTypes(@Nonnull OWLSequence s, boolean direct) {
        OWLIndividual x = sequences.expansionOf(s);
        if (x.isNamed()) {
            return d().getTypes(x.asOWLNamedIndividual(), direct);
        }
        throw new UnsupportedOperationException("the types of a sequence holding a blank node: ask isEntailed");
    }

    // --- OWLReasoner ------------------------------------------------------------------------

    @Override
    public String getReasonerName() {
        return delegate.getReasonerName() + " (rdf-array sequences)";
    }

    @Override
    public Version getReasonerVersion() {
        return delegate.getReasonerVersion();
    }

    @Override
    public BufferingMode getBufferingMode() {
        return buffering ? BufferingMode.BUFFERING : BufferingMode.NON_BUFFERING;
    }

    @Override
    public void flush() {
        if (dirty) {
            rebuild();
        }
    }

    @Override
    public List<OWLOntologyChange> getPendingChanges() {
        return buffering ? Collections.unmodifiableList(new ArrayList<>(pending)) : Collections.<OWLOntologyChange> emptyList();
    }

    @Override
    public Set<OWLAxiom> getPendingAxiomAdditions() {
        Set<OWLAxiom> s = new HashSet<>();
        for (OWLOntologyChange c : getPendingChanges()) {
            if (c.isAddAxiom()) {
                s.add(c.getAxiom());
            }
        }
        return s;
    }

    @Override
    public Set<OWLAxiom> getPendingAxiomRemovals() {
        Set<OWLAxiom> s = new HashSet<>();
        for (OWLOntologyChange c : getPendingChanges()) {
            if (c.isRemoveAxiom()) {
                s.add(c.getAxiom());
            }
        }
        return s;
    }

    @Override
    public OWLOntology getRootOntology() {
        return source;
    }

    @Override
    public void interrupt() {
        delegate.interrupt();
    }

    @Override
    public void precomputeInferences(InferenceType... inferenceTypes) {
        d().precomputeInferences(inferenceTypes);
    }

    @Override
    public boolean isPrecomputed(InferenceType inferenceType) {
        return d().isPrecomputed(inferenceType);
    }

    @Override
    public Set<InferenceType> getPrecomputableInferenceTypes() {
        return delegate.getPrecomputableInferenceTypes();
    }

    @Override
    public boolean isConsistent() {
        return d().isConsistent();
    }

    @Override
    public boolean isSatisfiable(OWLClassExpression classExpression) {
        return d().isSatisfiable(in(classExpression));
    }

    @Override
    public Node<OWLClass> getUnsatisfiableClasses() {
        return d().getUnsatisfiableClasses();
    }

    @Override
    public boolean isEntailed(OWLAxiom axiom) {
        OWLReasoner r = d();
        Set<OWLSequence> before = new HashSet<>(sequences.getExpansion().keySet());
        OWLAxiom rewritten = in(axiom.getAxiomWithoutAnnotations());
        if (!sequences.getExpansion().keySet().equals(before)) {
            // the axiom mentions a sequence the ontology does not: answer over a copy that also
            // holds that sequence's expansion
            return entailedWithNewSequences(rewritten, before);
        }
        return r.isEntailed(rewritten);
    }

    private boolean entailedWithNewSequences(OWLAxiom rewritten, Set<OWLSequence> before) {
        try {
            OWLOntologyManager m = expanded.getOWLOntologyManager();
            OWLOntology copy = m.createOntology(expanded.getAxioms());
            for (OWLSequence s : sequences.getExpansion().keySet()) {
                if (!before.contains(s)) {
                    m.addAxioms(copy, sequences.expansionAxioms(s));
                }
            }
            OWLReasoner r = factory.createNonBufferingReasoner(copy, config);
            try {
                return r.isEntailed(rewritten);
            } finally {
                r.dispose();
                m.removeOntology(copy);
            }
        } catch (OWLOntologyCreationException e) {
            throw new OWLRuntimeException(e);
        }
    }

    @Override
    public boolean isEntailed(Set<? extends OWLAxiom> axioms) {
        for (OWLAxiom ax : axioms) {
            if (!isEntailed(ax)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isEntailmentCheckingSupported(AxiomType<?> axiomType) {
        return delegate.isEntailmentCheckingSupported(axiomType);
    }

    @Override
    public Node<OWLClass> getTopClassNode() {
        return d().getTopClassNode();
    }

    @Override
    public Node<OWLClass> getBottomClassNode() {
        return d().getBottomClassNode();
    }

    @Override
    public NodeSet<OWLClass> getSubClasses(OWLClassExpression ce, boolean direct) {
        return d().getSubClasses(in(ce), direct);
    }

    @Override
    public NodeSet<OWLClass> getSuperClasses(OWLClassExpression ce, boolean direct) {
        return d().getSuperClasses(in(ce), direct);
    }

    @Override
    public Node<OWLClass> getEquivalentClasses(OWLClassExpression ce) {
        return d().getEquivalentClasses(in(ce));
    }

    @Override
    public NodeSet<OWLClass> getDisjointClasses(OWLClassExpression ce) {
        return d().getDisjointClasses(in(ce));
    }

    @Override
    public Node<OWLObjectPropertyExpression> getTopObjectPropertyNode() {
        return d().getTopObjectPropertyNode();
    }

    @Override
    public Node<OWLObjectPropertyExpression> getBottomObjectPropertyNode() {
        return d().getBottomObjectPropertyNode();
    }

    @Override
    public NodeSet<OWLObjectPropertyExpression> getSubObjectProperties(OWLObjectPropertyExpression pe, boolean direct) {
        return d().getSubObjectProperties(pe, direct);
    }

    @Override
    public NodeSet<OWLObjectPropertyExpression> getSuperObjectProperties(OWLObjectPropertyExpression pe, boolean direct) {
        return d().getSuperObjectProperties(pe, direct);
    }

    @Override
    public Node<OWLObjectPropertyExpression> getEquivalentObjectProperties(OWLObjectPropertyExpression pe) {
        return d().getEquivalentObjectProperties(pe);
    }

    @Override
    public NodeSet<OWLObjectPropertyExpression> getDisjointObjectProperties(OWLObjectPropertyExpression pe) {
        return d().getDisjointObjectProperties(pe);
    }

    @Override
    public Node<OWLObjectPropertyExpression> getInverseObjectProperties(OWLObjectPropertyExpression pe) {
        return d().getInverseObjectProperties(pe);
    }

    @Override
    public NodeSet<OWLClass> getObjectPropertyDomains(OWLObjectPropertyExpression pe, boolean direct) {
        return d().getObjectPropertyDomains(pe, direct);
    }

    @Override
    public NodeSet<OWLClass> getObjectPropertyRanges(OWLObjectPropertyExpression pe, boolean direct) {
        return d().getObjectPropertyRanges(pe, direct);
    }

    @Override
    public Node<OWLDataProperty> getTopDataPropertyNode() {
        return d().getTopDataPropertyNode();
    }

    @Override
    public Node<OWLDataProperty> getBottomDataPropertyNode() {
        return d().getBottomDataPropertyNode();
    }

    @Override
    public NodeSet<OWLDataProperty> getSubDataProperties(OWLDataProperty pe, boolean direct) {
        return d().getSubDataProperties(pe, direct);
    }

    @Override
    public NodeSet<OWLDataProperty> getSuperDataProperties(OWLDataProperty pe, boolean direct) {
        return d().getSuperDataProperties(pe, direct);
    }

    @Override
    public Node<OWLDataProperty> getEquivalentDataProperties(OWLDataProperty pe) {
        return d().getEquivalentDataProperties(pe);
    }

    @Override
    public NodeSet<OWLDataProperty> getDisjointDataProperties(OWLDataPropertyExpression pe) {
        return d().getDisjointDataProperties(pe);
    }

    @Override
    public NodeSet<OWLClass> getDataPropertyDomains(OWLDataProperty pe, boolean direct) {
        return d().getDataPropertyDomains(pe, direct);
    }

    @Override
    public NodeSet<OWLClass> getTypes(OWLNamedIndividual ind, boolean direct) {
        return d().getTypes(ind, direct);
    }

    @Override
    public NodeSet<OWLNamedIndividual> getInstances(OWLClassExpression ce, boolean direct) {
        return out(d().getInstances(in(ce), direct));
    }

    @Override
    public NodeSet<OWLNamedIndividual> getObjectPropertyValues(OWLNamedIndividual ind, OWLObjectPropertyExpression pe) {
        return out(d().getObjectPropertyValues(ind, pe));
    }

    @Override
    public Set<OWLLiteral> getDataPropertyValues(OWLNamedIndividual ind, OWLDataProperty pe) {
        return d().getDataPropertyValues(ind, pe);
    }

    @Override
    public Node<OWLNamedIndividual> getSameIndividuals(OWLNamedIndividual ind) {
        return d().getSameIndividuals(ind);
    }

    @Override
    public NodeSet<OWLNamedIndividual> getDifferentIndividuals(OWLNamedIndividual ind) {
        return out(d().getDifferentIndividuals(ind));
    }

    @Override
    public long getTimeOut() {
        return delegate.getTimeOut();
    }

    @Override
    public FreshEntityPolicy getFreshEntityPolicy() {
        return delegate.getFreshEntityPolicy();
    }

    @Override
    public IndividualNodeSetPolicy getIndividualNodeSetPolicy() {
        return delegate.getIndividualNodeSetPolicy();
    }

    @Override
    public void dispose() {
        manager.removeOntologyChangeListener(listener);
        delegate.dispose();
    }
}
