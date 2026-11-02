package org.semanticweb.owlapi.api.test.rdfarray;

import static org.junit.jupiter.api.Assertions.*;
import static org.semanticweb.owlapi.vocab.RDFArrayVocabulary.*;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.Test;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.rdfarray.SequenceAwareReasoner;
import org.semanticweb.owlapi.rdfarray.SequenceAwareReasonerFactory;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

/**
 * rdf-undef: what a reasoner concludes about a sequence with empty slots through the expansion
 * (rdf-undef.md §4): the slot counts in ra:length and ra:holes, has no member and gives no first or
 * last, and the odd cases that follow, checked against HermiT.
 */
public class UndefReasoningTestCase {

    static final String EX = "http://example/";
    static final OWLDataFactory df = OWLManager.getOWLDataFactory();
    static final OWLUndef undef = df.getOWLUndef();
    static final OWLObjectProperty rgb = op(EX + "rgb"), shifts = op(EX + "shifts"), cells = op(EX + "cells");
    static final OWLObjectProperty member = op(MEMBER.getIRI().toString());
    static final OWLObjectProperty first = op(FIRST.getIRI().toString());
    static final OWLObjectProperty last = op(LAST.getIRI().toString());
    static final OWLDataProperty memberValue = dp(MEMBER_VALUE), firstValue = dp(FIRST_VALUE), lastValue = dp(LAST_VALUE);
    static final OWLDataProperty length = dp(LENGTH), holes = dp(HOLES);
    static final OWLClass sequence = cls(SEQUENCE), empty = cls(EMPTY_SEQUENCE), dense = cls(DENSE_SEQUENCE);
    static final OWLClass objectSequence = cls(OBJECT_SEQUENCE), dataSequence = cls(DATA_SEQUENCE);
    static final OWLClass swatch = cls(EX + "Swatch"), denseSwatch = cls(EX + "DenseSwatch");
    static final OWLClass shift = cls(EX + "Shift"), night = cls(EX + "Night");
    static final OWLNamedIndividual x = ind("x"), y = ind("y"), z = ind("z"), a = ind("a"), b = ind("b");

    static OWLObjectProperty op(String iri) {
        return df.getOWLObjectProperty(IRI.create(iri));
    }

    static OWLDataProperty dp(org.semanticweb.owlapi.vocab.RDFArrayVocabulary v) {
        return df.getOWLDataProperty(v.getIRI());
    }

    static OWLClass cls(String iri) {
        return df.getOWLClass(IRI.create(iri));
    }

    static OWLClass cls(org.semanticweb.owlapi.vocab.RDFArrayVocabulary v) {
        return df.getOWLClass(v.getIRI());
    }

    static OWLNamedIndividual ind(String n) {
        return df.getOWLNamedIndividual(IRI.create(EX + n));
    }

    static OWLSequence seq(OWLPropertyAssertionObject... e) {
        return df.getOWLSequence(Arrays.asList(e));
    }

    static OWLLiteral i(int v) {
        return df.getOWLLiteral(v);
    }

    static OWLLiteral count(int v) {
        return df.getOWLLiteral(Integer.toString(v), OWL2Datatype.XSD_NON_NEGATIVE_INTEGER);
    }

    static OWLClassAssertionAxiom is(OWLClassExpression ce, OWLIndividual i) {
        return df.getOWLClassAssertionAxiom(ce, i);
    }

    static OWLClassExpression not(OWLClassExpression ce) {
        return df.getOWLObjectComplementOf(ce);
    }

    static OWLClassExpression has(OWLDataProperty p, OWLLiteral v) {
        return df.getOWLDataHasValue(p, v);
    }

    /** a Swatch's rgb is three bytes; a DenseSwatch's has no empty slot */
    static OWLOntology ontology(OWLAxiom... more) throws OWLOntologyCreationException {
        OWLOntologyManager m = OWLManager.createOWLOntologyManager();
        OWLOntology o = m.createOntology(new HashSet<>(Arrays.asList(
            df.getOWLDeclarationAxiom(rgb), df.getOWLDeclarationAxiom(shifts), df.getOWLDeclarationAxiom(cells),
            df.getOWLFunctionalObjectPropertyAxiom(rgb),   // so that "rgb only" follows from the one value
            df.getOWLSubClassOfAxiom(swatch, df.getOWLObjectAllValuesFrom(rgb, df.getOWLObjectIntersectionOf(
                has(length, count(3)),
                df.getOWLDataAllValuesFrom(memberValue, df.getOWLDatatypeMinMaxInclusiveRestriction(0, 255))))),
            df.getOWLEquivalentClassesAxiom(denseSwatch, df.getOWLObjectIntersectionOf(swatch,
                df.getOWLObjectAllValuesFrom(rgb, dense))),
            df.getOWLDifferentIndividualsAxiom(a, b))));
        m.addAxioms(o, new HashSet<>(Arrays.asList(more)));
        return o;
    }

    static SequenceAwareReasoner reasoner(OWLOntology o) {
        return (SequenceAwareReasoner) new SequenceAwareReasonerFactory(new ReasonerFactory()).createReasoner(o);
    }

    /** the expansion an EL reasoner would get: no closure, no functional properties */
    static SequenceAwareReasoner reasonerWithoutClosure(OWLOntology o) {
        return (SequenceAwareReasoner) new SequenceAwareReasonerFactory(new ReasonerFactory(), false).createReasoner(o);
    }

    @Test
    public void aSlotCountsInTheLengthAndTheHolesAndHasNoMember() throws Exception {
        OWLSequence s = seq(i(255), undef, i(0));
        SequenceAwareReasoner r = reasoner(ontology(df.getOWLObjectPropertyAssertionAxiom(rgb, x, s)));
        assertTrue(r.isConsistent());
        assertTrue(r.isEntailed(is(has(length, count(3)), s)));
        assertTrue(r.isEntailed(is(has(holes, count(1)), s)));
        assertTrue(r.isEntailed(df.getOWLDataPropertyAssertionAxiom(memberValue, s, i(255))));
        assertTrue(r.isEntailed(df.getOWLDataPropertyAssertionAxiom(memberValue, s, i(0))));
        // the closure: these are all the members
        assertTrue(r.isEntailed(is(df.getOWLDataAllValuesFrom(memberValue, df.getOWLDataOneOf(i(255), i(0))), s)));
        assertTrue(r.isEntailed(is(dataSequence, s)));
        assertTrue(r.isEntailed(is(sequence, s)));
        assertFalse(r.isEntailed(is(dense, s)));
        assertTrue(r.isEntailed(is(not(dense), s)));
        assertFalse(r.isEntailed(is(empty, s)));
        assertEquals(new HashSet<>(Arrays.asList(s)), r.getSequenceInstances(not(dense), false));
    }

    @Test
    public void aSwatchWithASlotIsConsistentUnlessItMustBeDense() throws Exception {
        OWLSequence s = seq(i(255), undef, i(0));
        assertTrue(reasoner(ontology(is(swatch, x), df.getOWLObjectPropertyAssertionAxiom(rgb, x, s))).isConsistent());
        assertFalse(reasoner(ontology(is(denseSwatch, x), df.getOWLObjectPropertyAssertionAxiom(rgb, x, s))).isConsistent());
        // the dense swatch, for the record
        SequenceAwareReasoner r = reasoner(ontology(is(swatch, x), df.getOWLObjectPropertyAssertionAxiom(rgb, x, seq(i(255), i(0), i(0)))));
        assertTrue(r.isEntailed(is(denseSwatch, x)));
    }

    /** ra:first over ( undef 2 3 ): there is none, and the reasoner must not take 2 or 3 for it */
    @Test
    public void noFirstAtALeadingSlot() throws Exception {
        OWLSequence s = seq(undef, i(2), i(3));
        SequenceAwareReasoner r = reasoner(ontology(df.getOWLObjectPropertyAssertionAxiom(rgb, x, s)));
        assertTrue(r.isConsistent());
        assertTrue(r.isEntailed(df.getOWLDataPropertyAssertionAxiom(lastValue, s, i(3))));
        assertFalse(r.isEntailed(df.getOWLDataPropertyAssertionAxiom(firstValue, s, i(2))));
        assertFalse(r.isEntailed(is(df.getOWLDataSomeValuesFrom(firstValue, df.getTopDatatype()), s)));
        // the phantom: without the end closure, "a non-empty sequence has a first" plus "the members
        // are 2 and 3" would give the sequence a first value in {2, 3}
        assertFalse(r.isEntailed(is(df.getOWLDataSomeValuesFrom(firstValue, df.getOWLDataOneOf(i(2), i(3))), s)));
        assertTrue(r.isEntailed(is(df.getOWLDataMaxCardinality(0, firstValue), s)));
        assertTrue(r.isEntailed(is(df.getOWLObjectMaxCardinality(0, first), s)));
        // and the mirror image
        OWLSequence t = seq(i(2), i(3), undef);
        SequenceAwareReasoner r2 = reasoner(ontology(df.getOWLObjectPropertyAssertionAxiom(rgb, x, t)));
        assertTrue(r2.isEntailed(df.getOWLDataPropertyAssertionAxiom(firstValue, t, i(2))));
        assertTrue(r2.isEntailed(is(df.getOWLDataMaxCardinality(0, lastValue), t)));
    }

    /** ( undef ): length 1, no members, no first, consistent; it is both an object and a data sequence */
    @Test
    public void aSequenceOfSlotsOnlyIsNotEmpty() throws Exception {
        OWLSequence s = seq(undef);
        SequenceAwareReasoner r = reasoner(ontology(df.getOWLObjectPropertyAssertionAxiom(rgb, x, s)));
        assertTrue(r.isConsistent());
        assertTrue(r.isEntailed(is(has(length, count(1)), s)));
        assertTrue(r.isEntailed(is(has(holes, count(1)), s)));
        assertTrue(r.isEntailed(is(not(empty), s)));
        assertTrue(r.isEntailed(is(df.getOWLObjectMaxCardinality(0, member), s)));
        assertTrue(r.isEntailed(is(df.getOWLDataMaxCardinality(0, memberValue), s)));
        // vacuously both, as () is: "no members" no longer means empty
        assertTrue(r.isEntailed(is(objectSequence, s)));
        assertTrue(r.isEntailed(is(dataSequence, s)));
        OWLClassExpression noMembers = df.getOWLObjectIntersectionOf(sequence,
            df.getOWLObjectMaxCardinality(0, member), df.getOWLDataMaxCardinality(0, memberValue));
        assertTrue(r.isEntailed(is(noMembers, s)));
        assertFalse(r.isEntailed(df.getOWLSubClassOfAxiom(noMembers, empty)));
        OWLSequence two = seq(undef, undef);
        SequenceAwareReasoner r2 = reasoner(ontology(df.getOWLObjectPropertyAssertionAxiom(rgb, x, two)));
        assertTrue(r2.isEntailed(is(has(holes, count(2)), two)));
        assertTrue(r2.isEntailed(df.getOWLDifferentIndividualsAxiom(two, s)));
    }

    /** ra:member over ( a undef b ): two members in three positions */
    @Test
    public void memberCountsTheMembersNotThePositions() throws Exception {
        OWLSequence s = seq(a, undef, b);
        SequenceAwareReasoner r = reasoner(ontology(df.getOWLObjectPropertyAssertionAxiom(shifts, x, s)));
        assertTrue(r.isEntailed(is(df.getOWLObjectExactCardinality(2, member), s)));
        assertFalse(r.isEntailed(is(df.getOWLObjectMinCardinality(3, member), s)));
        assertTrue(r.isEntailed(is(has(length, count(3)), s)));
        assertTrue(r.isEntailed(is(df.getOWLObjectAllValuesFrom(member, df.getOWLObjectOneOf(a, b)), s)));
        assertTrue(r.isEntailed(df.getOWLObjectPropertyAssertionAxiom(first, s, a)));
        assertTrue(r.isEntailed(df.getOWLObjectPropertyAssertionAxiom(last, s, b)));
        // "every position holds a Shift" needs the sequence dense as well as closed
        OWLClassExpression everyPositionAShift = df.getOWLObjectIntersectionOf(dense, objectSequence,
            df.getOWLObjectAllValuesFrom(member, shift));
        SequenceAwareReasoner typed = reasoner(ontology(df.getOWLObjectPropertyAssertionAxiom(shifts, x, s),
            is(shift, a), is(shift, b)));
        assertTrue(typed.isEntailed(is(df.getOWLObjectAllValuesFrom(member, shift), s)));
        assertFalse(typed.isEntailed(is(everyPositionAShift, s)));
        assertTrue(typed.isEntailed(is(everyPositionAShift, seq(a, b))));
    }

    /** which sequences the expansion can tell apart */
    @Test
    public void differentFromThroughTheCountsAndTheEnds() throws Exception {
        SequenceAwareReasoner r = reasoner(ontology());
        assertTrue(r.isEntailed(df.getOWLDifferentIndividualsAxiom(seq(i(1), undef), seq(i(1), i(2)))));
        assertTrue(r.isEntailed(df.getOWLDifferentIndividualsAxiom(seq(i(1), undef), seq(i(1)))));
        // the end closure tells ( 1 undef ) from ( undef 1 ), which the counts and members do not
        assertTrue(r.isEntailed(df.getOWLDifferentIndividualsAxiom(seq(i(1), undef), seq(undef, i(1)))));
        assertTrue(r.isEntailed(df.getOWLDifferentIndividualsAxiom(seq(undef, i(2), i(3)), seq(i(2), undef, i(3)))));
        // the limit, as for ( 1 2 3 4 ) and ( 1 3 2 4 ) already: the inner positions are invisible
        assertFalse(r.isEntailed(df.getOWLDifferentIndividualsAxiom(seq(i(1), undef, i(2), i(3)), seq(i(1), i(2), undef, i(3)))));
        assertFalse(r.isEntailed(df.getOWLDifferentIndividualsAxiom(seq(i(1), i(2), i(3), i(4)), seq(i(1), i(3), i(2), i(4)))));
    }

    /** ra:holes counts the sequence's own slots: ( ( undef ) ) is dense */
    @Test
    public void holesAreNotCountedThroughNesting() throws Exception {
        OWLSequence inner = seq(undef);
        OWLSequence s = seq(inner);
        SequenceAwareReasoner r = reasoner(ontology(df.getOWLObjectPropertyAssertionAxiom(cells, x, s)));
        assertTrue(r.isEntailed(is(dense, s)));
        assertTrue(r.isEntailed(is(has(holes, count(0)), s)));
        assertTrue(r.isEntailed(is(df.getOWLObjectSomeValuesFrom(member, has(holes, count(1))), s)));
        assertFalse(r.isEntailed(is(df.getOWLObjectAllValuesFrom(member, dense), s)));
        assertTrue(r.isEntailed(df.getOWLObjectPropertyAssertionAxiom(first, s, inner)));
    }

    /** equal terms, slots included, are one individual; a sequence with a slot is a nominal like any */
    @Test
    public void aSequenceWithASlotAsANominal() throws Exception {
        OWLClass holey = cls(EX + "Holey");
        SequenceAwareReasoner r = reasoner(ontology(
            df.getOWLEquivalentClassesAxiom(holey, df.getOWLObjectHasValue(rgb, seq(i(1), undef))),
            df.getOWLObjectPropertyAssertionAxiom(rgb, x, seq(i(1), undef)),
            df.getOWLObjectPropertyAssertionAxiom(rgb, y, seq(i(1), undef)),
            df.getOWLObjectPropertyAssertionAxiom(rgb, z, seq(i(1), i(2)))));
        assertTrue(r.isEntailed(is(holey, x)));
        assertTrue(r.isEntailed(is(holey, y)));
        assertFalse(r.isEntailed(is(holey, z)));
        assertEquals(new HashSet<>(Arrays.asList(x, y)), r.getInstances(holey, false).getFlattened());
        assertEquals(new HashSet<>(Arrays.asList(seq(i(1), i(2)))), r.getSequenceInstances(dense, false));
    }

    /** without the closure (OWL 2 EL): density and sparseness are still decided */
    @Test
    public void withoutTheClosureDensityIsStillDecided() throws Exception {
        OWLSequence sparse = seq(i(1), undef), full = seq(i(1), i(2));
        SequenceAwareReasoner r = reasonerWithoutClosure(ontology(
            df.getOWLObjectPropertyAssertionAxiom(rgb, x, sparse), df.getOWLObjectPropertyAssertionAxiom(rgb, y, full)));
        assertTrue(r.isConsistent());
        assertTrue(r.isEntailed(is(dense, full)));
        assertTrue(r.isEntailed(is(has(holes, count(1)), sparse)));
        assertFalse(r.isEntailed(is(dense, sparse)));
        // ra:holes is functional in both modes (OWL 2 EL has functional data properties), so even
        // sparseness is: the closure is needed for "only", not for the counts
        assertTrue(r.isEntailed(is(not(dense), sparse)));
    }

    /** the whole way: Turtle-array with undef, through the parser, to HermiT */
    @Test
    public void readsTurtleWithSlots() throws Exception {
        String ttl = "@prefix ex: <" + EX + "> . @prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
            + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> . @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n"
            + "@prefix ra: <https://rdf-array.github.io/rdf-array/vocab#> .\n"
            + "<" + EX + "o> a owl:Ontology . ex:shifts a owl:ObjectProperty . ra:holes a owl:DatatypeProperty .\n"
            + "ex:RestfulWeek a owl:Class ; owl:equivalentClass [ a owl:Restriction ; owl:onProperty ex:shifts ;\n"
            + "  owl:someValuesFrom [ a owl:Restriction ; owl:onProperty ra:holes ;\n"
            + "    owl:someValuesFrom [ a rdfs:Datatype ; owl:onDatatype xsd:nonNegativeInteger ;\n"
            + "      owl:withRestrictions ( [ xsd:minInclusive 2 ] ) ] ] ] .\n"
            + "ex:ann a owl:NamedIndividual ; ex:shifts ( ex:day UNDEF ex:night undef undef ex:day ex:day ) .\n"
            + "ex:bob a owl:NamedIndividual ; ex:shifts ( ex:day ex:day ex:night undef ex:day ex:day ex:day ) .\n";
        OWLOntology o = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(new StringDocumentSource(ttl));
        OWLSequence ann = seq(ind("day"), undef, ind("night"), undef, undef, ind("day"), ind("day"));
        assertTrue(o.containsAxiom(df.getOWLObjectPropertyAssertionAxiom(shifts, ind("ann"), ann)), o.getAxioms().toString());
        SequenceAwareReasoner r = reasoner(o);
        assertTrue(r.isEntailed(is(cls(EX + "RestfulWeek"), ind("ann"))));
        assertFalse(r.isEntailed(is(cls(EX + "RestfulWeek"), ind("bob"))));
        assertTrue(r.isEntailed(is(has(holes, count(3)), ann)));
        assertTrue(r.isEntailed(is(has(length, count(7)), ann)));
    }
}
