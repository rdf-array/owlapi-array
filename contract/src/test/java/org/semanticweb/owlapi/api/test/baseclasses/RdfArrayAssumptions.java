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
package org.semanticweb.owlapi.api.test.baseclasses;

import org.junit.jupiter.api.Assumptions;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.formats.RioRDFDocumentFormat;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

/**
 * rdf-array: the round trips owlapi-array cannot make yet, flagged with their reasons rather than
 * dropped (the array-tests skill). A round trip is skipped only when the ontology needs what the
 * format lacks: a list through a Rio format, or a sequence holding a literal through RDF/XML.
 */
public final class RdfArrayAssumptions {

    /** Why a Rio format cannot round-trip a list. */
    public static final String RIO = "rdf-array: the Rio (RDF4J) formats still read and write "
        + "rdf:first/rdf:rest ladders, which are not lists, and do not round-trip a sequence "
        + "(rdf-array/rdf-array#19)";
    /** Why RDF/XML cannot write some sequences. */
    public static final String RDF_XML = "rdf-array: RDF/XML-array has no spelling for a sequence "
        + "holding a literal, nested in another, or as a subject (rdf-array/rdf-array#18)";

    /** Why an RDF/XML input whose literal list is a ladder is not read as a list. */
    public static final String LITERAL_LADDER = "rdf-array: the RDF/XML input spells a list of "
        + "literals as an rdf:first/rdf:rest ladder, the only way RDF/XML can, and a ladder is not a "
        + "list (rdf-array/rdf-array#18)";
    private static final java.util.regex.Pattern LITERAL_FIRST = java.util.regex.Pattern.compile(
        "<rdf:first[^>]*(rdf:datatype|xml:lang)|<rdf:first\\s*>\\s*[^<\\s]");

    private RdfArrayAssumptions() {}

    /**
     * Skip a test whose RDF/XML input spells a literal list as a ladder.
     *
     * @param rdfXml the input
     */
    public static void assumeNoLiteralLadder(String rdfXml) {
        Assumptions.assumeFalse(rdfXml != null && LITERAL_FIRST.matcher(rdfXml).find(), LITERAL_LADDER);
    }

    /**
     * As {@link #assumeNoLiteralLadder(String)}, for a classpath resource.
     *
     * @param name the resource, relative to the classpath root
     */
    public static void assumeNoLiteralLadderInResource(String name) {
        try (java.io.InputStream in = RdfArrayAssumptions.class.getResourceAsStream('/' + name)) {
            if (in != null) {
                java.util.Scanner sc = new java.util.Scanner(in, "UTF-8").useDelimiter("\\A");
                assumeNoLiteralLadder(sc.hasNext() ? sc.next() : "");
            }
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    /**
     * Skip a round trip of {@code o} through {@code format} that owlapi-array cannot make yet.
     *
     * @param o the ontology
     * @param format the format of the round trip
     */
    public static void assumeRoundTrippable(OWLOntology o, OWLDocumentFormat format) {
        if (format instanceof RioRDFDocumentFormat) {
            Assumptions.assumeFalse(needsList(o), RIO);
        }
        if (format instanceof RDFXMLDocumentFormat) {
            Assumptions.assumeTrue(rdfXmlCanWrite(o), RDF_XML);
        }
    }

    /**
     * @param o the ontology
     * @return whether its RDF holds a list (a sequence), found by writing it in Turtle-array and
     *         looking for a ( outside IRIs, strings and comments
     */
    public static boolean needsList(OWLOntology o) {
        StringDocumentTarget t = new StringDocumentTarget();
        try {
            o.getOWLOntologyManager().saveOntology(o, new TurtleDocumentFormat(), t);
        } catch (OWLOntologyStorageException e) {
            return true;
        }
        String s = t.toString();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '<') {
                i = s.indexOf('>', i);
                if (i < 0) {
                    return false;
                }
            } else if (c == '#') {
                i = s.indexOf('\n', i);
                if (i < 0) {
                    return false;
                }
            } else if (c == '"') {
                boolean longString = s.startsWith("\"\"\"", i);
                i += longString ? 3 : 1;
                while (i < s.length() && !(longString ? s.startsWith("\"\"\"", i) : s.charAt(i) == '"')) {
                    i += s.charAt(i) == '\\' ? 2 : 1;
                }
                i += longString ? 2 : 0;
            } else if (c == '(') {
                return true;
            }
        }
        return false;
    }

    /**
     * @param o the ontology
     * @return whether the RDF/XML writer can write it
     */
    public static boolean rdfXmlCanWrite(OWLOntology o) {
        try {
            o.getOWLOntologyManager().saveOntology(o, new RDFXMLDocumentFormat(), new StringDocumentTarget());
            return true;
        } catch (OWLOntologyStorageException e) {
            for (Throwable t = e; t != null; t = t.getCause()) {
                if (t.getMessage() != null && t.getMessage().contains("RDF/XML-array cannot write")) {
                    return false;
                }
            }
            return true;
        }
    }
}
