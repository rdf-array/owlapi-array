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

import static org.semanticweb.owlapi.vocab.RDFArrayVocabulary.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.OWLObjectDuplicator;
import org.semanticweb.owlapi.util.RemappingIndividualProvider;
import org.semanticweb.owlapi.util.OWLObjectVisitorAdapter;
import org.semanticweb.owlapi.util.OWLObjectWalker;
import org.semanticweb.owlapi.util.SequenceNodeID;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

/**
 * rdf-array: owl-array.md §6 as code, the expansion that gives a reasoner which does not know
 * sequences their meaning. Each {@link OWLSequence} in the axioms becomes an individual, its
 * <em>expansion individual</em>: a named one with a canonical IRI when the sequence holds no anonymous
 * individual, so that it can stand in {@code ObjectHasValue} and {@code ObjectOneOf}, an anonymous one
 * otherwise. The axioms are rewritten with expansion individuals in place of sequences, and for every
 * sequence (nested ones included) the expansion adds {@code ra:Sequence}, {@code ra:length},
 * {@code ra:member} or {@code ra:memberValue} for each element, {@code ra:first}/{@code ra:last}
 * (or their value forms) and, unless disabled for an EL reasoner, the closure that says these are all
 * the members. The IRIs never leave the expansion: {@link #sequenceOf} maps an expansion individual
 * back to its sequence.
 */
public class SequenceAxioms {

    /** The namespace of the canonical IRIs of expansion individuals. */
    public static final String EXPANSION_NAMESPACE = "urn:rdf-array:sequence:";

    @Nonnull
    private final OWLDataFactory df;
    private final boolean closure;
    private final Map<OWLSequence, OWLIndividual> expansion = new LinkedHashMap<>();
    private final Map<OWLIndividual, OWLSequence> back = new HashMap<>();

    /**
     * @param df the data factory
     * @param closure whether to add the closure axioms (false for an EL reasoner, which cannot
     *        express them)
     */
    public SequenceAxioms(@Nonnull OWLDataFactory df, boolean closure) {
        this.df = df;
        this.closure = closure;
    }

    /**
     * @param s a sequence
     * @return its expansion individual, made the first time it is asked for
     */
    @Nonnull
    public OWLIndividual expansionOf(@Nonnull OWLSequence s) {
        OWLIndividual i = expansion.get(s);
        if (i == null) {
            i = holdsAnonymous(s) ? df.getOWLAnonymousIndividual()
                : df.getOWLNamedIndividual(IRI.create(EXPANSION_NAMESPACE + s.getID().getID().substring(SequenceNodeID.PREFIX.length())));
            expansion.put(s, i);
            back.put(i, s);
        }
        return i;
    }

    /**
     * @param i an individual
     * @return the sequence it is the expansion individual of, or null
     */
    @Nullable
    public OWLSequence sequenceOf(OWLIndividual i) {
        return back.get(i);
    }

    /** @return the sequences met so far and their expansion individuals */
    public Map<OWLSequence, OWLIndividual> getExpansion() {
        return expansion;
    }

    private static boolean holdsAnonymous(OWLSequence s) {
        for (OWLPropertyAssertionObject e : s.getElements()) {
            if (e instanceof OWLSequence ? holdsAnonymous((OWLSequence) e) : e instanceof OWLAnonymousIndividual) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param axioms the axioms of an ontology's imports closure
     * @return the axioms a stock reasoner needs: the logical axioms and declarations with every
     *         sequence replaced by its expansion individual, the expansion of every sequence, and the
     *         vocabulary's axioms
     */
    @Nonnull
    public Set<OWLAxiom> expand(@Nonnull Collection<OWLAxiom> axioms) {
        Set<OWLAxiom> result = new LinkedHashSet<>();
        Set<OWLSequence> sequences = new LinkedHashSet<>();
        for (OWLAxiom ax : axioms) {
            if (!ax.isLogicalAxiom() && !(ax instanceof OWLDeclarationAxiom)) {
                continue;
            }
            OWLAxiom plain = ax.getAxiomWithoutAnnotations();
            collect(plain, sequences);
            result.add(rewrite(plain));
        }
        Set<OWLSequence> all = new LinkedHashSet<>();
        for (OWLSequence s : sequences) {
            addWithNested(s, all);
        }
        for (OWLSequence s : all) {
            result.addAll(expansionAxioms(s));
        }
        result.addAll(vocabularyAxioms(df, closure));
        return result;
    }

    /**
     * @param object an axiom or class expression
     * @param <T> its type
     * @return it with every sequence replaced by its expansion individual
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    public <T extends OWLObject> T rewrite(@Nonnull T object) {
        if (!(object instanceof OWLAxiom)) {
            // wrap a class expression in an axiom to rewrite it, then take it back out
            if (object instanceof OWLClassExpression) {
                OWLSubClassOfAxiom ax = rewrite(df.getOWLSubClassOfAxiom((OWLClassExpression) object, df.getOWLThing()));
                return (T) ax.getSubClass();
            }
            return object;
        }
        Set<OWLSequence> found = new LinkedHashSet<>();
        collect(object, found);
        if (found.isEmpty()) {
            return object;
        }
        return (T) new ExpansionDuplicator().duplicateObject(object);
    }

    /** Copies an object with every sequence replaced by its expansion individual. */
    private final class ExpansionDuplicator extends OWLObjectDuplicator {

        ExpansionDuplicator() {
            // no remapping: blank nodes keep their identity, as the expansion axioms refer to them
            super(new HashMap<OWLEntity, IRI>(), df, Collections.<OWLLiteral, OWLLiteral> emptyMap(),
                new RemappingIndividualProvider(df, false));
        }

        @Override
        public void visit(OWLSequence sequence) {
            setLastObject(expansionOf(sequence));
        }
    }

    private static void collect(OWLObject o, Set<OWLSequence> into) {
        new OWLObjectWalker<>(Collections.singleton(o)).walkStructure(new OWLObjectVisitorAdapter() {

            @Override
            public void visit(OWLSequence sequence) {
                into.add(sequence);
            }
        });
    }

    private static void addWithNested(OWLSequence s, Set<OWLSequence> into) {
        if (into.add(s)) {
            for (OWLPropertyAssertionObject e : s.getElements()) {
                if (e instanceof OWLSequence) {
                    addWithNested((OWLSequence) e, into);
                }
            }
        }
    }

    /**
     * @param s a sequence
     * @return the axioms describing it (owl-array.md §6)
     */
    @Nonnull
    public Set<OWLAxiom> expansionAxioms(@Nonnull OWLSequence s) {
        Set<OWLAxiom> axioms = new LinkedHashSet<>();
        OWLIndividual x = expansionOf(s);
        List<OWLPropertyAssertionObject> elements = s.getElements();
        axioms.add(df.getOWLClassAssertionAxiom(df.getOWLClass(SEQUENCE.getIRI()), x));
        axioms.add(df.getOWLDataPropertyAssertionAxiom(df.getOWLDataProperty(LENGTH.getIRI()), x,
            df.getOWLLiteral(Integer.toString(elements.size()), OWL2Datatype.XSD_NON_NEGATIVE_INTEGER)));
        Set<OWLIndividual> individuals = new LinkedHashSet<>();
        Set<OWLLiteral> values = new LinkedHashSet<>();
        for (OWLPropertyAssertionObject e : elements) {
            if (e instanceof OWLLiteral) {
                values.add((OWLLiteral) e);
            } else {
                individuals.add(element(e));
            }
        }
        for (OWLIndividual i : individuals) {
            axioms.add(df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectProperty(MEMBER.getIRI()), x, i));
        }
        for (OWLLiteral v : values) {
            axioms.add(df.getOWLDataPropertyAssertionAxiom(df.getOWLDataProperty(MEMBER_VALUE.getIRI()), x, v));
        }
        if (!elements.isEmpty()) {
            end(axioms, x, elements.get(0), FIRST, FIRST_VALUE);
            end(axioms, x, elements.get(elements.size() - 1), LAST, LAST_VALUE);
        }
        if (closure) {
            OWLObjectProperty member = df.getOWLObjectProperty(MEMBER.getIRI());
            OWLDataProperty memberValue = df.getOWLDataProperty(MEMBER_VALUE.getIRI());
            if (individuals.isEmpty()) {
                axioms.add(df.getOWLClassAssertionAxiom(df.getOWLObjectMaxCardinality(0, member), x));
            } else if (allNamed(individuals)) {
                // ObjectOneOf needs named individuals in OWL 2 DL; a sequence holding an anonymous
                // individual gets no object closure (rdf-array/rdf-array#20)
                axioms.add(df.getOWLClassAssertionAxiom(df.getOWLObjectAllValuesFrom(member,
                    df.getOWLObjectOneOf(individuals)), x));
            }
            if (values.isEmpty()) {
                axioms.add(df.getOWLClassAssertionAxiom(df.getOWLDataMaxCardinality(0, memberValue), x));
            } else {
                axioms.add(df.getOWLClassAssertionAxiom(df.getOWLDataAllValuesFrom(memberValue,
                    df.getOWLDataOneOf(values)), x));
            }
        }
        return axioms;
    }

    private OWLIndividual element(OWLPropertyAssertionObject e) {
        return e instanceof OWLSequence ? expansionOf((OWLSequence) e) : (OWLIndividual) e;
    }

    private void end(Set<OWLAxiom> axioms, OWLIndividual x, OWLPropertyAssertionObject e,
        org.semanticweb.owlapi.vocab.RDFArrayVocabulary object, org.semanticweb.owlapi.vocab.RDFArrayVocabulary value) {
        if (e instanceof OWLLiteral) {
            axioms.add(df.getOWLDataPropertyAssertionAxiom(df.getOWLDataProperty(value.getIRI()), x, (OWLLiteral) e));
        } else {
            axioms.add(df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectProperty(object.getIRI()), x, element(e)));
        }
    }

    private static boolean allNamed(Collection<OWLIndividual> individuals) {
        for (OWLIndividual i : individuals) {
            if (i.isAnonymous()) {
                return false;
            }
        }
        return true;
    }

    /**
     * The axioms of the vocabulary ontology, vocab/owl-array.ttl (owl-array.md §3.2), built here so
     * that the expansion needs no parser.
     *
     * @param df the data factory
     * @param full false to leave out what OWL 2 EL cannot express (complements, unions,
     *        functional properties)
     * @return the axioms
     */
    @Nonnull
    public static Set<OWLAxiom> vocabularyAxioms(@Nonnull OWLDataFactory df, boolean full) {
        Set<OWLAxiom> a = new LinkedHashSet<>();
        OWLClass sequence = df.getOWLClass(SEQUENCE.getIRI());
        OWLClass empty = df.getOWLClass(EMPTY_SEQUENCE.getIRI());
        OWLClass objectSequence = df.getOWLClass(OBJECT_SEQUENCE.getIRI());
        OWLClass dataSequence = df.getOWLClass(DATA_SEQUENCE.getIRI());
        OWLObjectProperty member = df.getOWLObjectProperty(MEMBER.getIRI());
        OWLObjectProperty first = df.getOWLObjectProperty(FIRST.getIRI());
        OWLObjectProperty last = df.getOWLObjectProperty(LAST.getIRI());
        OWLDataProperty memberValue = df.getOWLDataProperty(MEMBER_VALUE.getIRI());
        OWLDataProperty firstValue = df.getOWLDataProperty(FIRST_VALUE.getIRI());
        OWLDataProperty lastValue = df.getOWLDataProperty(LAST_VALUE.getIRI());
        OWLDataProperty length = df.getOWLDataProperty(LENGTH.getIRI());
        OWLDatatype nonNegative = df.getOWLDatatype(OWL2Datatype.XSD_NON_NEGATIVE_INTEGER.getIRI());
        for (OWLEntity e : new OWLEntity[] {sequence, empty, objectSequence, dataSequence, member, first,
            last, memberValue, firstValue, lastValue, length}) {
            a.add(df.getOWLDeclarationAxiom(e));
        }
        a.add(df.getOWLObjectPropertyDomainAxiom(member, sequence));
        a.add(df.getOWLSubObjectPropertyOfAxiom(first, member));
        a.add(df.getOWLSubObjectPropertyOfAxiom(last, member));
        a.add(df.getOWLDataPropertyDomainAxiom(memberValue, sequence));
        a.add(df.getOWLSubDataPropertyOfAxiom(firstValue, memberValue));
        a.add(df.getOWLSubDataPropertyOfAxiom(lastValue, memberValue));
        a.add(df.getOWLDataPropertyDomainAxiom(length, sequence));
        a.add(df.getOWLDataPropertyRangeAxiom(length, nonNegative));
        a.add(df.getOWLSubClassOfAxiom(sequence, df.getOWLDataSomeValuesFrom(length, nonNegative)));
        a.add(df.getOWLEquivalentClassesAxiom(empty, df.getOWLObjectIntersectionOf(sequence,
            df.getOWLDataHasValue(length, df.getOWLLiteral("0", OWL2Datatype.XSD_NON_NEGATIVE_INTEGER)))));
        if (full) {
            a.add(df.getOWLFunctionalObjectPropertyAxiom(first));
            a.add(df.getOWLFunctionalObjectPropertyAxiom(last));
            a.add(df.getOWLFunctionalDataPropertyAxiom(firstValue));
            a.add(df.getOWLFunctionalDataPropertyAxiom(lastValue));
            a.add(df.getOWLFunctionalDataPropertyAxiom(length));
            a.add(df.getOWLSubClassOfAxiom(empty, df.getOWLObjectMaxCardinality(0, member)));
            a.add(df.getOWLSubClassOfAxiom(empty, df.getOWLDataMaxCardinality(0, memberValue)));
            a.add(df.getOWLEquivalentClassesAxiom(objectSequence, df.getOWLObjectIntersectionOf(sequence,
                df.getOWLObjectComplementOf(df.getOWLDataSomeValuesFrom(memberValue, df.getTopDatatype())))));
            a.add(df.getOWLEquivalentClassesAxiom(dataSequence, df.getOWLObjectIntersectionOf(sequence,
                df.getOWLObjectComplementOf(df.getOWLObjectSomeValuesFrom(member, df.getOWLThing())))));
            OWLClassExpression nonEmpty = df.getOWLObjectIntersectionOf(sequence, df.getOWLObjectComplementOf(empty));
            a.add(df.getOWLSubClassOfAxiom(nonEmpty, df.getOWLObjectUnionOf(
                df.getOWLObjectSomeValuesFrom(first, df.getOWLThing()),
                df.getOWLDataSomeValuesFrom(firstValue, df.getTopDatatype()))));
            a.add(df.getOWLSubClassOfAxiom(nonEmpty, df.getOWLObjectUnionOf(
                df.getOWLObjectSomeValuesFrom(last, df.getOWLThing()),
                df.getOWLDataSomeValuesFrom(lastValue, df.getTopDatatype()))));
        }
        return a;
    }
}
