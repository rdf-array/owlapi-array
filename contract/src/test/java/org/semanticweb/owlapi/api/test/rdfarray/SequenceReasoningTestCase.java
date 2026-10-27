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
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.semanticweb.owlapi.vocab.OWLFacet;

/**
 * rdf-array: the examples of owl-array.md §3.3, checked against HermiT through the expansion of §6
 * (SequenceAwareReasonerFactory), as entailments (§3.4) and inconsistencies, with the two caveats of
 * §3.5 as tests that the naive reading does not hold.
 */
public class SequenceReasoningTestCase {

    static final String EX = "http://example/";
    static final OWLDataFactory df = OWLManager.getOWLDataFactory();
    static final OWLObjectProperty authorList = op("http://purl.org/ontology/bibo/authorList");
    static final OWLObjectProperty creator = op("http://purl.org/dc/terms/creator");
    static final OWLObjectProperty firstAuthor = op(EX + "firstAuthor");
    static final OWLObjectProperty steps = op(EX + "steps");
    static final OWLObjectProperty points = op(EX + "points");
    static final OWLObjectProperty rgb = op(EX + "rgb");
    static final OWLObjectProperty member = op(MEMBER.getIRI().toString());
    static final OWLObjectProperty first = op(FIRST.getIRI().toString());
    static final OWLObjectProperty last = op(LAST.getIRI().toString());
    static final OWLDataProperty memberValue = df.getOWLDataProperty(MEMBER_VALUE.getIRI());
    static final OWLDataProperty length = df.getOWLDataProperty(LENGTH.getIRI());
    static final OWLClass agent = cls("http://xmlns.com/foaf/0.1/Agent");
    static final OWLClass paper = cls(EX + "Paper"), bigPaper = cls(EX + "BigPaper");
    static final OWLClass step = cls(EX + "Step"), serving = cls(EX + "Serving"), recipe = cls(EX + "Recipe");
    static final OWLClass ring = cls(EX + "Ring"), swatch = cls(EX + "Swatch"), redThing = cls(EX + "RedThing");
    static final OWLClass sequenceClass = cls(SEQUENCE.getIRI().toString());
    static final OWLNamedIndividual p = ind("p"), ada = ind("ada"), alan = ind("alan"), grace = ind("grace");

    static OWLObjectProperty op(String iri) {
        return df.getOWLObjectProperty(IRI.create(iri));
    }

    static OWLClass cls(String iri) {
        return df.getOWLClass(IRI.create(iri));
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

    static OWLDataRange atLeast(int n) {
        return df.getOWLDatatypeRestriction(df.getIntegerOWLDatatype(), OWLFacet.MIN_INCLUSIVE, i(n));
    }

    /** owl-array.md §3.3: the papers, recipes, rings, swatches and the chains */
    static OWLOntology ontology(OWLAxiom... more) throws OWLOntologyCreationException {
        OWLOntologyManager m = OWLManager.createOWLOntologyManager();
        OWLOntology o = m.createOntology(new HashSet<>(Arrays.asList(
            df.getOWLDeclarationAxiom(authorList), df.getOWLDeclarationAxiom(creator),
            df.getOWLDeclarationAxiom(firstAuthor), df.getOWLDeclarationAxiom(steps),
            df.getOWLDeclarationAxiom(points), df.getOWLDeclarationAxiom(rgb),
            df.getOWLSubClassOfAxiom(paper, df.getOWLObjectAllValuesFrom(authorList,
                df.getOWLObjectAllValuesFrom(member, agent))),
            df.getOWLEquivalentClassesAxiom(bigPaper, df.getOWLObjectIntersectionOf(paper,
                df.getOWLObjectSomeValuesFrom(authorList, df.getOWLDataSomeValuesFrom(length, atLeast(3))))),
            df.getOWLSubPropertyChainOfAxiom(Arrays.asList(authorList, member), creator),
            df.getOWLSubPropertyChainOfAxiom(Arrays.asList(authorList, first), firstAuthor),
            df.getOWLSubClassOfAxiom(recipe, df.getOWLObjectAllValuesFrom(steps,
                df.getOWLObjectSomeValuesFrom(last, serving))),
            df.getOWLSubClassOfAxiom(ring, df.getOWLObjectAllValuesFrom(points,
                df.getOWLDataSomeValuesFrom(length, atLeast(4)))),
            df.getOWLSubClassOfAxiom(swatch, df.getOWLObjectAllValuesFrom(rgb, df.getOWLObjectIntersectionOf(
                df.getOWLDataHasValue(length, df.getOWLLiteral("3", OWL2Datatype.XSD_NON_NEGATIVE_INTEGER)),
                df.getOWLDataAllValuesFrom(memberValue, df.getOWLDatatypeMinMaxInclusiveRestriction(0, 255))))),
            df.getOWLEquivalentClassesAxiom(redThing, df.getOWLObjectHasValue(rgb, seq(i(255), i(0), i(0)))),
            df.getOWLClassAssertionAxiom(paper, p),
            df.getOWLObjectPropertyAssertionAxiom(authorList, p, seq(ada, alan, grace)))));
        m.addAxioms(o, new HashSet<>(Arrays.asList(more)));
        return o;
    }

    static SequenceAwareReasoner reasoner(OWLOntology o) {
        return (SequenceAwareReasoner) new SequenceAwareReasonerFactory(new ReasonerFactory()).createReasoner(o);
    }

    @Test
    public void everyAuthorIsAnAgent() throws Exception {
        OWLReasoner r = reasoner(ontology());
        assertTrue(r.isConsistent());
        for (OWLNamedIndividual a : Arrays.asList(ada, alan, grace)) {
            assertTrue(r.isEntailed(df.getOWLClassAssertionAxiom(agent, a)), a.toString());
        }
        assertTrue(r.getInstances(agent, false).containsEntity(ada));
    }

    @Test
    public void lengthClassifiesABigPaper() throws Exception {
        OWLReasoner r = reasoner(ontology());
        assertTrue(r.isEntailed(df.getOWLClassAssertionAxiom(bigPaper, p)));
        assertTrue(r.getInstances(bigPaper, false).containsEntity(p));
        OWLReasoner two = reasoner(ontology(df.getOWLObjectPropertyAssertionAxiom(authorList, ind("q"), seq(ada, alan)),
            df.getOWLClassAssertionAxiom(paper, ind("q"))));
        assertFalse(two.isEntailed(df.getOWLClassAssertionAxiom(bigPaper, ind("q"))));
    }

    @Test
    public void chainsGiveExistingVocabularyAViewOfTheList() throws Exception {
        OWLReasoner r = reasoner(ontology());
        assertTrue(r.isEntailed(df.getOWLObjectPropertyAssertionAxiom(creator, p, ada)));
        assertTrue(r.isEntailed(df.getOWLObjectPropertyAssertionAxiom(creator, p, grace)));
        assertTrue(r.isEntailed(df.getOWLObjectPropertyAssertionAxiom(firstAuthor, p, ada)));
        assertFalse(r.isEntailed(df.getOWLObjectPropertyAssertionAxiom(firstAuthor, p, alan)));
        assertTrue(r.getObjectPropertyValues(p, creator).containsEntity(alan));
    }

    @Test
    public void theSequenceIsASequenceAndNotEmpty() throws Exception {
        SequenceAwareReasoner r = reasoner(ontology());
        OWLSequence authors = seq(ada, alan, grace);
        assertTrue(r.isEntailed(df.getOWLClassAssertionAxiom(sequenceClass, authors)));
        assertTrue(r.isEntailed(df.getOWLClassAssertionAxiom(cls(OBJECT_SEQUENCE.getIRI().toString()), authors)));
        assertTrue(r.isEntailed(df.getOWLClassAssertionAxiom(
            df.getOWLObjectComplementOf(cls(EMPTY_SEQUENCE.getIRI().toString())), authors)));
        assertTrue(r.getSequenceInstances(sequenceClass, false).contains(authors));
        // expansion individuals are not individuals of the ontology
        for (OWLNamedIndividual n : r.getInstances(df.getOWLThing(), false).getFlattened()) {
            assertFalse(n.getIRI().toString().startsWith("urn:rdf-array:sequence:"), n.toString());
        }
    }

    @Test
    public void emptySequence() throws Exception {
        OWLReasoner r = reasoner(ontology(df.getOWLObjectPropertyAssertionAxiom(steps, ind("nothing"), seq())));
        assertTrue(r.isEntailed(df.getOWLClassAssertionAxiom(cls(EMPTY_SEQUENCE.getIRI().toString()), seq())));
    }

    @Test
    public void lastStep() throws Exception {
        OWLReasoner r = reasoner(ontology(
            df.getOWLClassAssertionAxiom(recipe, ind("recipe")),
            df.getOWLObjectPropertyAssertionAxiom(steps, ind("recipe"), seq(ind("chop"), ind("fry"), ind("serve")))));
        assertTrue(r.isEntailed(df.getOWLClassAssertionAxiom(serving, ind("serve"))));
        assertFalse(r.isEntailed(df.getOWLClassAssertionAxiom(serving, ind("chop"))));
    }

    @Test
    public void threePointRingIsInconsistent() throws Exception {
        OWLSequence pt = seq(df.getOWLLiteral(0.0D), df.getOWLLiteral(0.0D));
        OWLReasoner ok = reasoner(ontology(df.getOWLClassAssertionAxiom(ring, ind("r")),
            df.getOWLObjectPropertyAssertionAxiom(points, ind("r"), seq(pt, seq(i(0), i(1)), seq(i(1), i(1)), pt))));
        assertTrue(ok.isConsistent());
        OWLReasoner bad = reasoner(ontology(df.getOWLClassAssertionAxiom(ring, ind("r")),
            df.getOWLObjectPropertyAssertionAxiom(points, ind("r"), seq(pt, seq(i(0), i(1)), pt))));
        assertFalse(bad.isConsistent());
    }

    @Test
    public void twoMemberRgbIsInconsistent() throws Exception {
        assertTrue(reasoner(ontology(df.getOWLClassAssertionAxiom(swatch, ind("red")),
            df.getOWLObjectPropertyAssertionAxiom(rgb, ind("red"), seq(i(255), i(0), i(0))))).isConsistent());
        assertFalse(reasoner(ontology(df.getOWLClassAssertionAxiom(swatch, ind("red")),
            df.getOWLObjectPropertyAssertionAxiom(rgb, ind("red"), seq(i(255), i(0))))).isConsistent());
        assertFalse(reasoner(ontology(df.getOWLClassAssertionAxiom(swatch, ind("pink")),
            df.getOWLObjectPropertyAssertionAxiom(rgb, ind("pink"), seq(i(255), i(192), i(256))))).isConsistent());
    }

    @Test
    public void aSequenceAsANominal() throws Exception {
        OWLReasoner r = reasoner(ontology(df.getOWLObjectPropertyAssertionAxiom(rgb, ind("x"), seq(i(255), i(0), i(0))),
            df.getOWLObjectPropertyAssertionAxiom(rgb, ind("y"), seq(i(0), i(0), i(255)))));
        assertTrue(r.isEntailed(df.getOWLClassAssertionAxiom(redThing, ind("x"))));
        assertFalse(r.isEntailed(df.getOWLClassAssertionAxiom(redThing, ind("y"))));
        assertTrue(r.isEntailed(df.getOWLDifferentIndividualsAxiom(seq(i(255), i(0), i(0)), seq(i(0), i(0), i(255)))));
    }

    @Test
    public void memberCountsDistinctIndividualsNotPositions() throws Exception {
        // §3.5: three positions are not three members unless the individuals are different
        OWLClassExpression threeMembers = df.getOWLObjectMinCardinality(3, member);
        OWLReasoner r = reasoner(ontology());
        assertFalse(r.isEntailed(df.getOWLClassAssertionAxiom(threeMembers, seq(ada, alan, grace))));
        OWLReasoner different = reasoner(ontology(df.getOWLDifferentIndividualsAxiom(ada, alan, grace)));
        assertTrue(different.isEntailed(df.getOWLClassAssertionAxiom(threeMembers, seq(ada, alan, grace))));
    }

    @Test
    public void memberOnlyIsVacuousOfALiteralOnlySequence() throws Exception {
        // §3.5: ra:member only C says nothing about literal members
        OWLReasoner r = reasoner(ontology(df.getOWLObjectPropertyAssertionAxiom(rgb, ind("x"), seq(i(1), i(2), i(3)))));
        assertTrue(r.isEntailed(df.getOWLClassAssertionAxiom(df.getOWLObjectAllValuesFrom(member, agent), seq(i(1), i(2), i(3)))));
        assertTrue(r.isEntailed(df.getOWLClassAssertionAxiom(cls(DATA_SEQUENCE.getIRI().toString()), seq(i(1), i(2), i(3)))));
    }

    @Test
    public void theSourceOntologyIsUnchanged() throws Exception {
        OWLOntology o = ontology();
        int before = o.getAxiomCount();
        OWLReasoner r = reasoner(o);
        r.isConsistent();
        assertEquals(before, o.getAxiomCount());
        assertEquals(1, o.getOWLOntologyManager().getOntologies().size());
    }

    @Test
    public void readsTurtleArray() throws Exception {
        String ttl = "@prefix ex: <" + EX + "> . @prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
            + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
            + "@prefix ra: <https://rdf-array.github.io/rdf-array/vocab#> .\n"
            + "@prefix bibo: <http://purl.org/ontology/bibo/> . @prefix foaf: <http://xmlns.com/foaf/0.1/> .\n"
            + "<" + EX + "o> a owl:Ontology . bibo:authorList a owl:ObjectProperty . ra:member a owl:ObjectProperty .\n"
            + "ex:Paper a owl:Class . foaf:Agent a owl:Class . ex:p a owl:NamedIndividual , ex:Paper .\n"
            + "ex:ada a owl:NamedIndividual . ex:alan a owl:NamedIndividual .\n"
            + "ex:Paper rdfs:subClassOf [ a owl:Restriction ; owl:onProperty bibo:authorList ;\n"
            + "  owl:allValuesFrom [ a owl:Restriction ; owl:onProperty ra:member ; owl:allValuesFrom foaf:Agent ] ] .\n"
            + "ex:p bibo:authorList ( ex:ada ex:alan ) .\n";
        OWLOntology o = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(new StringDocumentSource(ttl));
        assertTrue(reasoner(o).isEntailed(df.getOWLClassAssertionAxiom(agent, ada)));
    }

    @Test
    public void sequenceInstancesLeaveOutSequencesHoldingABlankNode() throws Exception {
        // HermiT entails ClassAssertion(C, _:x) once some C exists, so an anonymous expansion
        // individual must not be asked about (rdf-array/rdf-array#20)
        OWLSequence withBlank = seq(ada, df.getOWLAnonymousIndividual());
        SequenceAwareReasoner r = reasoner(ontology(df.getOWLObjectPropertyAssertionAxiom(steps, ind("r"), withBlank)));
        // the ground sequences: the author list and RedThing's nominal; not the one holding a blank node
        assertEquals(new HashSet<>(Arrays.asList(seq(ada, alan, grace), seq(i(255), i(0), i(0)))),
            r.getSequenceInstances(sequenceClass, false));
        assertTrue(r.getSequenceInstances(paper, false).isEmpty());
    }

    @Test
    public void survivesTheSourceBeingRemovedFromItsManager() throws Exception {
        OWLOntology o = ontology();
        OWLOntologyManager m = o.getOWLOntologyManager();
        OWLOntology other = m.createOntology(IRI.create(EX + "other"));
        OWLReasoner r = reasoner(o);
        r.isConsistent();
        m.removeOntology(o);
        m.addAxiom(other, df.getOWLDeclarationAxiom(paper));
        r.dispose();
        m.addAxiom(other, df.getOWLDeclarationAxiom(agent));
        assertEquals(2, other.getAxiomCount());
    }
}
