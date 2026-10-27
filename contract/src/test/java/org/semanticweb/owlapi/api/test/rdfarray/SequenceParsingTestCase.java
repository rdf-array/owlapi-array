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

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFParserMetaData;
import org.semanticweb.owlapi.io.RDFTriple;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.*;

/**
 * rdf-array: Turtle and RDF/XML read a collection as one sequence term (owl-array.md §2, §3);
 * OWL's own lists come from sequences and a ladder is not a list.
 */
public class SequenceParsingTestCase {

    static final String EX = "http://example/";
    static final String PREFIXES = "@prefix ex: <" + EX + "> .\n"
        + "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
        + "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"
        + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
        + "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n"
        + "<" + EX + "o> a owl:Ontology .\n"
        + "ex:p a owl:ObjectProperty . ex:q a owl:ObjectProperty . ex:r a owl:ObjectProperty .\n"
        + "ex:A a owl:Class . ex:B a owl:Class . ex:C a owl:Class .\n"
        + "ex:a a owl:NamedIndividual . ex:b a owl:NamedIndividual . ex:x a owl:NamedIndividual .\n";

    static OWLDataFactory df = OWLManager.getOWLDataFactory();

    static OWLOntology load(String doc) throws OWLOntologyCreationException {
        return OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(new StringDocumentSource(doc));
    }

    static OWLNamedIndividual ind(String n) {
        return df.getOWLNamedIndividual(IRI.create(EX + n));
    }

    static OWLObjectProperty op(String n) {
        return df.getOWLObjectProperty(IRI.create(EX + n));
    }

    static OWLClass cls(String n) {
        return df.getOWLClass(IRI.create(EX + n));
    }

    static OWLSequence seq(OWLPropertyAssertionObject... e) {
        return df.getOWLSequence(Arrays.asList(e));
    }

    static Set<RDFTriple> unparsed(OWLOntology o) {
        OWLDocumentFormat f = o.getOWLOntologyManager().getOntologyFormat(o);
        return ((RDFParserMetaData) f.getOntologyLoaderMetaData()).getUnparsedTriples();
    }

    @Test
    public void owlListsComeFromSequences() throws Exception {
        OWLOntology o = load(PREFIXES + "ex:C owl:equivalentClass [ a owl:Class ; owl:unionOf ( ex:A ex:B ) ] .\n"
            + "ex:r owl:propertyChainAxiom ( ex:p ex:q ) .\n"
            + "[ a owl:AllDifferent ; owl:members ( ex:a ex:b ex:x ) ] .\n"
            + "ex:C owl:hasKey ( ex:p ) .\n");
        assertTrue(o.containsAxiom(df.getOWLEquivalentClassesAxiom(cls("C"),
            df.getOWLObjectUnionOf(cls("A"), cls("B")))));
        assertTrue(o.containsAxiom(df.getOWLSubPropertyChainOfAxiom(Arrays.asList(op("p"), op("q")), op("r"))));
        assertTrue(o.containsAxiom(df.getOWLDifferentIndividualsAxiom(ind("a"), ind("b"), ind("x"))));
        assertTrue(o.containsAxiom(df.getOWLHasKeyAxiom(cls("C"), Collections.singleton(op("p")))));
        assertTrue(unparsed(o).isEmpty(), unparsed(o).toString());
    }

    @Test
    public void classExpressionInsideAListHoistsItsTriples() throws Exception {
        OWLOntology o = load(PREFIXES
            + "ex:C owl:equivalentClass [ a owl:Class ; owl:intersectionOf ( ex:A [ a owl:Restriction ; owl:onProperty ex:p ; owl:someValuesFrom ex:B ] ) ] .\n");
        assertTrue(o.containsAxiom(df.getOWLEquivalentClassesAxiom(cls("C"),
            df.getOWLObjectIntersectionOf(cls("A"), df.getOWLObjectSomeValuesFrom(op("p"), cls("B"))))));
    }

    @Test
    public void dataSequenceIsOneIndividual() throws Exception {
        OWLOntology o = load(PREFIXES + "ex:x ex:p ( ex:a ex:b ) ; ex:q () ; ex:r ( ex:a ( ex:b ) ) .\n");
        assertTrue(o.containsAxiom(df.getOWLObjectPropertyAssertionAxiom(op("p"), ind("x"), seq(ind("a"), ind("b")))));
        assertTrue(o.containsAxiom(df.getOWLObjectPropertyAssertionAxiom(op("q"), ind("x"), seq())));
        assertTrue(o.containsAxiom(df.getOWLObjectPropertyAssertionAxiom(op("r"), ind("x"), seq(ind("a"), seq(ind("b"))))));
        assertTrue(unparsed(o).isEmpty(), unparsed(o).toString());
    }

    @Test
    public void literalsAndBlankNodesInsideASequence() throws Exception {
        OWLOntology o = load(PREFIXES + "ex:x ex:p ( 1 \"s\"@en [ a ex:A ] ) .\n");
        OWLObjectPropertyAssertionAxiom ax = o.getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION).iterator().next();
        OWLSequence s = (OWLSequence) ax.getObject();
        assertEquals(3, s.getLength());
        assertEquals(df.getOWLLiteral(1), s.getElements().get(0));
        assertEquals(df.getOWLLiteral("s", "en"), s.getElements().get(1));
        OWLAnonymousIndividual blank = (OWLAnonymousIndividual) s.getElements().get(2);
        // the blank node inside is the individual its own triples describe
        assertTrue(o.containsAxiom(df.getOWLClassAssertionAxiom(cls("A"), blank)), o.getAxioms().toString());
    }

    @Test
    public void sequenceAsSubject() throws Exception {
        OWLOntology o = load(PREFIXES + "( ex:a ) ex:p ex:b .\n");
        assertTrue(o.containsAxiom(df.getOWLObjectPropertyAssertionAxiom(op("p"), seq(ind("a")), ind("b"))));
    }

    @Test
    public void ladderIsNotAList() throws Exception {
        OWLOntology o = load(PREFIXES + "ex:C owl:equivalentClass [ a owl:Class ; owl:unionOf _:l1 ] .\n"
            + "_:l1 rdf:first ex:A ; rdf:rest _:l2 . _:l2 rdf:first ex:B ; rdf:rest rdf:nil .\n");
        assertFalse(o.containsAxiom(df.getOWLEquivalentClassesAxiom(cls("C"),
            df.getOWLObjectUnionOf(cls("A"), cls("B")))));
        assertFalse(unparsed(o).isEmpty());
    }

    @Test
    public void rdfXmlCollectionIsOneSequence() throws Exception {
        String xml = "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"
            + " xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:ex=\"" + EX + "\">\n"
            + " <owl:Ontology rdf:about=\"" + EX + "o\"/>\n"
            + " <owl:ObjectProperty rdf:about=\"" + EX + "p\"/>\n"
            + " <owl:Class rdf:about=\"" + EX + "A\"/><owl:Class rdf:about=\"" + EX + "B\"/><owl:Class rdf:about=\"" + EX + "C\"/>\n"
            + " <owl:NamedIndividual rdf:about=\"" + EX + "a\"/><owl:NamedIndividual rdf:about=\"" + EX + "b\"/><owl:NamedIndividual rdf:about=\"" + EX + "x\"/>\n"
            + " <owl:Class rdf:about=\"" + EX + "C\"><owl:equivalentClass><owl:Class>\n"
            + "  <owl:unionOf rdf:parseType=\"Collection\"><rdf:Description rdf:about=\"" + EX + "A\"/><rdf:Description rdf:about=\"" + EX + "B\"/></owl:unionOf>\n"
            + " </owl:Class></owl:equivalentClass></owl:Class>\n"
            + " <rdf:Description rdf:about=\"" + EX + "x\"><ex:p rdf:parseType=\"Collection\"><rdf:Description rdf:about=\"" + EX + "a\"/><rdf:Description rdf:about=\"" + EX + "b\"/></ex:p></rdf:Description>\n"
            + "</rdf:RDF>\n";
        OWLOntology o = load(xml);
        assertTrue(o.containsAxiom(df.getOWLEquivalentClassesAxiom(cls("C"),
            df.getOWLObjectUnionOf(cls("A"), cls("B")))));
        assertTrue(o.containsAxiom(df.getOWLObjectPropertyAssertionAxiom(op("p"), ind("x"), seq(ind("a"), ind("b")))));
    }
}
