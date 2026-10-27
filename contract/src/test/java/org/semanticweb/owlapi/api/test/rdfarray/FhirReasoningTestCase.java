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

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.rdfarray.SequenceAwareReasoner;
import org.semanticweb.owlapi.rdfarray.SequenceAwareReasonerFactory;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleIRIMapper;
import org.semanticweb.owlapi.util.mansyntax.ManchesterOWLSyntaxParser;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * rdf-array: rdf-array's examples/fhir, the SWAT4LS 2016 FHIR reasoning process over today's FHIR RDF,
 * whose repeated elements are lists. The files under {@code rdfarray/fhir/} (and the OWL-array
 * vocabulary under {@code vocab/}, where the catalog's {@code ../../vocab/owl-array.ttl} finds it) are
 * unchanged copies of rdf-array's; the imports are resolved through {@code catalog-v001.xml} as the
 * example's FhirReasoning.java and Protégé read it, and the DL queries are those of its table, with
 * HermiT behind {@link SequenceAwareReasonerFactory}.
 */
public class FhirReasoningTestCase {

    static final Map<String, String> PREFIXES = new LinkedHashMap<>();
    static {
        PREFIXES.put("fhir", "http://hl7.org/fhir/");
        PREFIXES.put("sct", "http://snomed.info/id/");
        PREFIXES.put("loinc", "https://loinc.org/rdf/");
        PREFIXES.put("ra", "https://rdf-array.github.io/rdf-array/vocab#");
        PREFIXES.put("xsd", "http://www.w3.org/2001/XMLSchema#");
        PREFIXES.put("", "https://rdf-array.github.io/rdf-array/examples/fhir/arthritis.ttl#");
        PREFIXES.put("bp", "https://rdf-array.github.io/rdf-array/examples/fhir/blood-pressure.ttl#");
        PREFIXES.put("amy", "https://rdf-array.github.io/rdf-array/examples/fhir/subject-amy.ttl#");
        PREFIXES.put("bob", "https://rdf-array.github.io/rdf-array/examples/fhir/subject-bob.ttl#");
        PREFIXES.put("sue", "https://rdf-array.github.io/rdf-array/examples/fhir/subject-sue.ttl#");
    }
    static final String FHIR_MIN = "https://rdf-array.github.io/rdf-array/examples/fhir/fhir-min.ttl";

    static OWLOntology subjects;
    static SequenceAwareReasoner reasoner;
    static ManchesterOWLSyntaxParser parser;

    static String qname(IRI iri) {
        String s = iri.toString();
        for (Map.Entry<String, String> e : PREFIXES.entrySet()) {
            if (s.startsWith(e.getValue())) {
                return e.getKey() + ":" + s.substring(e.getValue().length());
            }
        }
        return "<" + s + ">";
    }

    static IRI iri(String qname) {
        int colon = qname.indexOf(':');
        return IRI.create(PREFIXES.get(qname.substring(0, colon)) + qname.substring(colon + 1));
    }

    /** subjects.ttl in a fresh manager, its imports mapped by catalog-v001.xml */
    static OWLOntology load() throws Exception {
        File dir = new File(FhirReasoningTestCase.class.getResource("/rdfarray/fhir/catalog-v001.xml").toURI())
            .getParentFile();
        OWLOntologyManager m = OWLManager.createOWLOntologyManager();
        NodeList uris = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(new File(dir, "catalog-v001.xml")).getElementsByTagName("uri");
        for (int i = 0; i < uris.getLength(); i++) {
            Element u = (Element) uris.item(i);
            m.getIRIMappers().add(new SimpleIRIMapper(IRI.create(u.getAttribute("name")),
                IRI.create(new File(dir, u.getAttribute("uri")).getCanonicalFile().toURI())));
        }
        return m.loadOntologyFromOntologyDocument(new File(dir, "subjects.ttl"));
    }

    @BeforeAll
    static void setUp() throws Exception {
        subjects = load();
        reasoner = (SequenceAwareReasoner) new SequenceAwareReasonerFactory(new ReasonerFactory())
            .createReasoner(subjects);
        parser = OWLManager.createManchesterParser();
        parser.setDefaultOntology(subjects);
        ShortFormProvider sfp = new ShortFormProvider() {

            @Override
            public String getShortForm(OWLEntity e) {
                return qname(e.getIRI());
            }

            @Override
            public void dispose() {}
        };
        parser.setOWLEntityChecker(new ShortFormEntityChecker(new BidirectionalShortFormProviderAdapter(
            subjects.getOWLOntologyManager(), subjects.getImportsClosure(), sfp)));
    }

    @AfterAll
    static void tearDown() {
        reasoner.dispose();
    }

    static OWLClassExpression parse(String manchester) {
        parser.setStringToParse(manchester);
        return parser.parseClassExpression();
    }

    @Test
    public void theClosureLoadsWithoutALadderAndIsConsistent() {
        assertEquals(10, subjects.getImportsClosure().size());
        for (OWLOntology o : subjects.getImportsClosure()) {
            for (OWLEntity e : o.getSignature()) {
                String iri = e.getIRI().toString();
                assertFalse(iri.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#first")
                    || iri.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#rest"),
                    "a list read as rdf:first/rdf:rest triples: " + e);
            }
        }
        assertTrue(reasoner.isConsistent());
    }

    @Test
    public void noListIsASequenceInstanceUntilIssue20() {
        // every FHIR list holds blank nodes, so none has a named expansion individual
        // (rdf-array/rdf-array#20); asking HermiT about the anonymous ones would answer all 16
        assertTrue(reasoner.getSequenceInstances(parse("fhir:Observation"), false).isEmpty());
        assertTrue(reasoner.getSequenceInstances(parse("ra:Sequence"), false).isEmpty());
    }

    static Stream<Arguments> queries() {
        return Stream.of(new String[][] {
            {"fhir:Observation and fhir:code some sct:Rheumatoid_arthritis-disorder", "amy:Obs1 bob:Obs1 sue:Obs1"},
            {"fhir:Observation and fhir:code some sct:69896004", "amy:Obs1 bob:Obs1 sue:Obs1"},
            {":Observation-of-Rheumatoid_arthritis", "amy:Obs1 bob:Obs1 sue:Obs1"},
            {":Observation-of-Seronegative_rheumatoid_arthritis", "bob:Obs1"},
            {":Observation-of-Seropositive_rheumatoid_arthritis", "sue:Obs1"},
            {"fhir:Observation and fhir:code some sct:Arthritis-disorder", "amy:Obs1 bob:Obs1 sue:Obs1"},
            {":Observation-of-inflammatory_musculoskeletal_disorder", "amy:Obs1 bob:Obs1 sue:Obs1"},
            {"fhir:Observation and fhir:code some (sct:Inflammatory_disorder-disorder and sct:role_group-attr some (sct:Associated_morphology-attr some sct:Inflammatory_morphology-morph and sct:Finding_site-attr some sct:Structure_of_musculoskeletal_system-body))", "amy:Obs1 bob:Obs1 sue:Obs1"},
            {"fhir:Observation and fhir:code some (fhir:coding some (ra:member some :Coding-in-ICD-10))", "amy:Obs1 bob:Obs1 sue:Obs1"},
            {"fhir:Observation and fhir:code some (fhir:coding some (ra:member some :Coding-of-icd10-M05.9))", "sue:Obs1"},
            {":Patient_with_rheumatoid_arthritis", "amy:asPatient bob:asPatient sue:asPatient"},
            {"fhir:Patient and inverse fhir:l some (inverse fhir:subject some :Observation-of-Seronegative_rheumatoid_arthritis)", "bob:asPatient"},
            {":Observation-coded-first-in-SNOMED", "amy:Obs1"},
            {"fhir:Observation and fhir:code some (fhir:coding some (ra:first some :Coding-in-SNOMED))", "amy:Obs1"},
            {"fhir:Observation and fhir:code some (fhir:coding some (ra:last some :Coding-in-SNOMED))", "amy:Obs2 bob:Obs1"},
            {":Observation-with-several-codings", "amy:Obs1 amy:Obs2 bob:Obs1"},
            {"fhir:Observation and fhir:code some (fhir:coding some (ra:length value 1))", "sue:Obs1"},
            {"bp:Observation-of-blood-pressure", "amy:Obs2"},
            {"bp:Observation-of-elevated-blood-pressure", "amy:Obs2"},
            {"bp:Two-component-blood-pressure", "amy:Obs2"},
            {"bp:Complete-blood-pressure", "amy:Obs2"},
            {"fhir:Observation and fhir:component some (ra:member some (fhir:value some (fhir:value some (fhir:v some xsd:decimal[>= 160]))))", ""},
            {"fhir:Patient and fhir:name some (ra:member some (fhir:given some (ra:length some xsd:integer[>= 2])))", "sue:asPatient"},
            {"fhir:Observation and fhir:code some (fhir:coding some ra:EmptySequence)", ""},
        }).map(q -> Arguments.of(q[0], q[1]));
    }

    @ParameterizedTest
    @MethodSource("queries")
    public void dlQuery(String query, String expected) {
        TreeSet<String> got = new TreeSet<>();
        for (OWLNamedIndividual i : reasoner.getInstances(parse(query), false).getFlattened()) {
            got.add(qname(i.getIRI()));
        }
        assertEquals(new TreeSet<>(Arrays.asList(expected.isEmpty() ? new String[0] : expected.split(" "))), got);
    }

    @Test
    public void fhirTtlsListAxiomMakesTheDataInconsistent() throws Exception {
        // fhir.ttl's `fhir:coding only fhir:Coding` types the list, and fhir-min.ttl says fhir:Base
        // is disjoint from ra:Sequence
        OWLOntology o = load();
        OWLOntologyManager m = o.getOWLOntologyManager();
        OWLDataFactory df = m.getOWLDataFactory();
        OWLOntology fhirMin = m.getOntology(IRI.create(FHIR_MIN));
        OWLAxiom fhirTtl = df.getOWLSubClassOfAxiom(df.getOWLClass(iri("fhir:CodeableConcept")),
            df.getOWLObjectAllValuesFrom(df.getOWLObjectProperty(iri("fhir:coding")),
                df.getOWLClass(iri("fhir:Coding"))));
        SequenceAwareReasoner r = (SequenceAwareReasoner) new SequenceAwareReasonerFactory(new ReasonerFactory())
            .createReasoner(o);
        try {
            assertTrue(r.isConsistent());
            m.addAxiom(fhirMin, fhirTtl);
            r.flush();
            assertFalse(r.isConsistent());
            m.removeAxiom(fhirMin, fhirTtl);
            r.flush();
            assertTrue(r.isConsistent());
        } finally {
            r.dispose();
        }
        assertTrue(o.getAxiomCount(Imports.INCLUDED) > 1000);
    }
}
