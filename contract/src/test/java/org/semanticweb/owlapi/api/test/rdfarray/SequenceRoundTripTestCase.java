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
import static org.semanticweb.owlapi.api.test.rdfarray.SequenceParsingTestCase.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.formats.ManchesterSyntaxDocumentFormat;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.*;

/**
 * rdf-array: OWL's lists and data sequences survive a round trip through every syntax: Turtle
 * spells ( … ), RDF/XML rdf:parseType="Collection", and the Functional-style, OWL/XML and
 * Manchester syntaxes carry a sequence as its reversible _:seq_… node ID (owl-array.md §4).
 */
public class SequenceRoundTripTestCase {

    static OWLOntology ontology(boolean literals) throws OWLOntologyCreationException {
        OWLOntologyManager m = OWLManager.createOWLOntologyManager();
        OWLOntology o = m.createOntology(IRI.create(EX + "o"));
        Set<OWLAxiom> axioms = new HashSet<>(Arrays.asList(
            df.getOWLDeclarationAxiom(op("p")), df.getOWLDeclarationAxiom(op("q")), df.getOWLDeclarationAxiom(op("r")),
            df.getOWLDeclarationAxiom(cls("A")), df.getOWLDeclarationAxiom(cls("B")), df.getOWLDeclarationAxiom(cls("C")),
            df.getOWLDeclarationAxiom(ind("a")), df.getOWLDeclarationAxiom(ind("b")), df.getOWLDeclarationAxiom(ind("x")),
            df.getOWLEquivalentClassesAxiom(cls("C"), df.getOWLObjectUnionOf(cls("A"), cls("B"))),
            df.getOWLSubPropertyChainOfAxiom(Arrays.asList(op("p"), op("q")), op("r")),
            df.getOWLDifferentIndividualsAxiom(ind("a"), ind("b"), ind("x")),
            df.getOWLObjectPropertyAssertionAxiom(op("p"), ind("x"), seq(ind("a"), ind("b"))),
            df.getOWLObjectPropertyAssertionAxiom(op("q"), ind("x"), seq())));
        if (literals) {
            axioms.addAll(Arrays.asList(
                df.getOWLObjectPropertyAssertionAxiom(op("r"), ind("x"),
                    seq(ind("a"), df.getOWLLiteral(1), df.getOWLLiteral("s", "en"), seq(ind("b"), df.getOWLLiteral(2.5D)))),
                df.getOWLObjectPropertyAssertionAxiom(op("p"), seq(ind("a")), ind("b"))));
        }
        m.addAxioms(o, axioms);
        return o;
    }

    static OWLOntology roundTrip(OWLOntology o, OWLDocumentFormat format) throws Exception {
        StringDocumentTarget target = new StringDocumentTarget();
        o.getOWLOntologyManager().saveOntology(o, format, target);
        OWLOntology back = OWLManager.createOWLOntologyManager()
            .loadOntologyFromOntologyDocument(new StringDocumentSource(target.toString()));
        assertEquals(o.getLogicalAxioms(), back.getLogicalAxioms(), target.toString());
        return back;
    }

    @Test
    public void turtle() throws Exception {
        OWLOntology o = ontology(true);
        StringDocumentTarget target = new StringDocumentTarget();
        o.getOWLOntologyManager().saveOntology(o, new TurtleDocumentFormat(), target);
        String ttl = target.toString();
        assertFalse(ttl.contains("rdf:first") || ttl.contains("rdf-syntax-ns#first"), ttl);
        assertFalse(ttl.contains("seq_"), ttl);
        assertTrue(ttl.contains("()"), ttl);
        roundTrip(o, new TurtleDocumentFormat());
    }

    @Test
    public void rdfXmlWithoutLiterals() throws Exception {
        OWLOntology o = ontology(false);
        StringDocumentTarget target = new StringDocumentTarget();
        o.getOWLOntologyManager().saveOntology(o, new RDFXMLDocumentFormat(), target);
        assertTrue(target.toString().contains("rdf:parseType=\"Collection\""), target.toString());
        assertFalse(target.toString().contains("rdf:first"), target.toString());
        roundTrip(o, new RDFXMLDocumentFormat());
    }

    @Test
    public void rdfXmlRefusesALiteralElement() throws Exception {
        OWLOntology o = ontology(true);
        OWLOntologyStorageException e = assertThrows(OWLOntologyStorageException.class,
            () -> o.getOWLOntologyManager().saveOntology(o, new RDFXMLDocumentFormat(), new StringDocumentTarget()));
        Throwable t = e;
        boolean found = false;
        while (t != null) {
            found |= t.getMessage() != null && t.getMessage().contains("RDF/XML-array cannot write");
            t = t.getCause();
        }
        assertTrue(found, e.toString());
    }

    @Test
    public void functionalSyntaxNodeId() throws Exception {
        roundTrip(ontology(true), new FunctionalSyntaxDocumentFormat());
    }

    @Test
    public void owlXmlNodeId() throws Exception {
        roundTrip(ontology(true), new OWLXMLDocumentFormat());
    }

    @Test
    public void manchesterNodeId() throws Exception {
        roundTrip(ontology(true), new ManchesterSyntaxDocumentFormat());
    }
}
