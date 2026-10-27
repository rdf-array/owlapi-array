/* This file is part of the OWL API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright 2014, The University of Manchester
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License. */
package org.semanticweb.owlapi.util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.annotation.Nonnull;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLAnonymousIndividualByIdProvider;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLPropertyAssertionObject;
import org.semanticweb.owlapi.model.OWLSequence;

/**
 * rdf-array: the reversible node ID that carries an {@link OWLSequence} through the syntaxes that
 * have no spelling for a sequence yet (Functional-style, OWL/XML, Manchester), as an anonymous
 * individual whose node ID is {@code _:seq_} followed by the base64url (unpadded) of the UTF-8
 * N-Triples-array spelling of the sequence. Equal sequences have equal node IDs. A parser that
 * reads such a node ID gets the sequence back ({@link #decode}); an ordinary anonymous individual
 * inside a sequence keeps its own node ID inside the spelling, and is resolved through the
 * provider the parser uses for the rest of the document. See owl-array.md §4.
 */
public final class SequenceNodeID {

    /** The prefix of a sequence node ID. */
    public static final String PREFIX = "_:seq_";
    private static final String BARE_PREFIX = "seq_";

    private SequenceNodeID() {}

    /**
     * @param id a node ID, with or without its {@code _:}
     * @return whether it is the node ID of a sequence
     */
    public static boolean isSequenceNodeID(String id) {
        return id != null && (id.startsWith(PREFIX) || id.startsWith(BARE_PREFIX));
    }

    /**
     * @param elements the elements of a sequence
     * @return its node ID, {@code _:seq_…}
     */
    @Nonnull
    public static String encode(List<? extends OWLPropertyAssertionObject> elements) {
        return PREFIX + Base64.getUrlEncoder().withoutPadding()
            .encodeToString(spell(elements).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * @param elements the elements of a sequence
     * @return its N-Triples-array spelling, {@code ( e1 e2 … )} or {@code ()}
     */
    @Nonnull
    public static String spell(List<? extends OWLPropertyAssertionObject> elements) {
        StringBuilder b = new StringBuilder();
        spell(elements, b);
        return b.toString();
    }

    private static void spell(List<? extends OWLPropertyAssertionObject> elements, StringBuilder b) {
        if (elements.isEmpty()) {
            b.append("()");
            return;
        }
        b.append('(');
        for (OWLPropertyAssertionObject e : elements) {
            b.append(' ');
            spellElement(e, b);
        }
        b.append(" )");
    }

    private static void spellElement(OWLPropertyAssertionObject e, StringBuilder b) {
        if (e instanceof OWLSequence) {
            spell(((OWLSequence) e).getElements(), b);
        } else if (e instanceof OWLLiteral) {
            OWLLiteral l = (OWLLiteral) e;
            b.append('"');
            escape(l.getLiteral(), b);
            b.append('"');
            if (l.hasLang()) {
                b.append('@').append(l.getLang());
            } else {
                b.append("^^<").append(l.getDatatype().getIRI().toString()).append('>');
            }
        } else if (e instanceof OWLAnonymousIndividual) {
            String id = ((OWLAnonymousIndividual) e).getID().getID();
            b.append(id.startsWith("_:") ? id : "_:" + id);
        } else {
            b.append('<').append(((OWLIndividual) e).asOWLNamedIndividual().getIRI().toString()).append('>');
        }
    }

    private static void escape(String s, StringBuilder b) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    b.append("\\\"");
                    break;
                case '\\':
                    b.append("\\\\");
                    break;
                case '\n':
                    b.append("\\n");
                    break;
                case '\r':
                    b.append("\\r");
                    break;
                default:
                    b.append(c);
            }
        }
    }

    // --- Spelling from raw terms, for the RDF parsers -------------------------------------------

    /**
     * @param iri an IRI, a blank node's {@code _:} IRI, or a sequence node ID
     * @return its spelling as an element of a sequence
     */
    @Nonnull
    public static String spellNode(String iri) {
        if (isSequenceNodeID(iri)) {
            return spellingOf(iri);
        }
        if (iri.startsWith("_:")) {
            return iri;
        }
        return '<' + iri + '>';
    }

    /**
     * @param lexical the lexical form
     * @param lang a language tag, or null or empty
     * @param datatype the datatype IRI, or null for xsd:string (or rdf:langString with a tag)
     * @return the literal's spelling as an element of a sequence
     */
    @Nonnull
    public static String spellLiteral(String lexical, String lang, String datatype) {
        StringBuilder b = new StringBuilder("\"");
        escape(lexical, b);
        b.append('"');
        if (lang != null && !lang.isEmpty()) {
            b.append('@').append(lang);
        } else {
            b.append("^^<")
                .append(datatype == null ? "http://www.w3.org/2001/XMLSchema#string" : datatype)
                .append('>');
        }
        return b.toString();
    }

    /**
     * @param elementSpellings the spellings of the elements, in order
     * @return the node ID of the sequence of them
     */
    @Nonnull
    public static String encodeSpellings(List<String> elementSpellings) {
        String spelling = elementSpellings.isEmpty() ? "()" : "( " + String.join(" ", elementSpellings) + " )";
        return PREFIX + Base64.getUrlEncoder().withoutPadding()
            .encodeToString(spelling.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * @param nodeID a sequence node ID
     * @return the N-Triples-array spelling it encodes
     */
    @Nonnull
    public static String spellingOf(String nodeID) {
        String body = nodeID.startsWith(PREFIX) ? nodeID.substring(PREFIX.length())
            : nodeID.substring(BARE_PREFIX.length());
        return new String(Base64.getUrlDecoder().decode(body), StandardCharsets.UTF_8);
    }

    /**
     * Decode a sequence node ID into raw elements, for an RDF consumer: an element that is a named
     * individual or a blank node is its {@link IRI} (a blank node's is {@code _:…}), a nested
     * sequence is the {@link IRI} of its own node ID, and a literal is an {@link OWLLiteral}.
     *
     * @param nodeID a sequence node ID
     * @param df builds the literals
     * @return the elements
     */
    @Nonnull
    public static List<Object> decodeRaw(String nodeID, OWLDataFactory df) {
        Reader r = new Reader(spellingOf(nodeID), df, null);
        r.raw = true;
        r.skip();
        List<Object> elements = r.rawSequence();
        return elements;
    }

    /**
     * @param nodeID a node ID for which {@link #isSequenceNodeID} holds
     * @param df the data factory that builds the elements
     * @param anonymous resolves the node IDs of the anonymous individuals inside, as the rest of
     *        the document does
     * @return the elements of the sequence
     */
    @Nonnull
    public static List<OWLPropertyAssertionObject> decode(String nodeID, OWLDataFactory df,
        OWLAnonymousIndividualByIdProvider anonymous) {
        String body = nodeID.startsWith(PREFIX) ? nodeID.substring(PREFIX.length())
            : nodeID.substring(BARE_PREFIX.length());
        String spelling = new String(Base64.getUrlDecoder().decode(body), StandardCharsets.UTF_8);
        return parse(spelling, df, anonymous);
    }

    /**
     * Read an N-Triples-array sequence spelling.
     *
     * @param spelling {@code ( … )} or {@code ()}
     * @param df the data factory that builds the elements
     * @param anonymous resolves blank node labels
     * @return the elements
     */
    @Nonnull
    public static List<OWLPropertyAssertionObject> parse(String spelling, OWLDataFactory df,
        OWLAnonymousIndividualByIdProvider anonymous) {
        Reader r = new Reader(spelling, df, anonymous);
        r.skip();
        List<OWLPropertyAssertionObject> elements = r.sequence();
        r.skip();
        if (r.pos != spelling.length()) {
            throw r.error("trailing characters");
        }
        return elements;
    }

    private static final class Reader {

        final String s;
        final OWLDataFactory df;
        final OWLAnonymousIndividualByIdProvider anonymous;
        int pos;
        boolean raw;

        Reader(String s, OWLDataFactory df, OWLAnonymousIndividualByIdProvider anonymous) {
            this.s = s;
            this.df = df;
            this.anonymous = anonymous;
        }

        IllegalArgumentException error(String what) {
            return new IllegalArgumentException(
                "Bad sequence spelling (" + what + ") at " + pos + ": " + s);
        }

        void skip() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
                pos++;
            }
        }

        List<OWLPropertyAssertionObject> sequence() {
            if (pos >= s.length() || s.charAt(pos) != '(') {
                throw error("expected (");
            }
            pos++;
            List<OWLPropertyAssertionObject> elements = new ArrayList<>();
            skip();
            while (pos < s.length() && s.charAt(pos) != ')') {
                elements.add(element());
                skip();
            }
            if (pos >= s.length()) {
                throw error("unclosed (");
            }
            pos++;
            return elements;
        }

        List<Object> rawSequence() {
            if (pos >= s.length() || s.charAt(pos) != '(') {
                throw error("expected (");
            }
            pos++;
            List<Object> elements = new ArrayList<>();
            skip();
            while (pos < s.length() && s.charAt(pos) != ')') {
                char c = s.charAt(pos);
                if (c == '(') {
                    int start = pos;
                    rawSequence();
                    // re-spell the nested sequence to give it its own node ID
                    String nested = s.substring(start, pos).trim();
                    elements.add(IRI.create(PREFIX + Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(nested.getBytes(StandardCharsets.UTF_8))));
                } else if (c == '<') {
                    int end = s.indexOf('>', pos);
                    if (end < 0) {
                        throw error("unclosed <");
                    }
                    elements.add(IRI.create(s.substring(pos + 1, end)));
                    pos = end + 1;
                } else if (c == '_' && s.startsWith("_:", pos)) {
                    int start = pos;
                    while (pos < s.length() && !Character.isWhitespace(s.charAt(pos)) && s.charAt(pos) != ')') {
                        pos++;
                    }
                    elements.add(IRI.create(s.substring(start, pos)));
                } else if (c == '"') {
                    elements.add(literal());
                } else {
                    throw error("unexpected " + c);
                }
                skip();
            }
            if (pos >= s.length()) {
                throw error("unclosed (");
            }
            pos++;
            return elements;
        }

        OWLPropertyAssertionObject element() {
            char c = s.charAt(pos);
            if (c == '(') {
                return df.getOWLSequence(sequence());
            }
            if (c == '<') {
                int end = s.indexOf('>', pos);
                if (end < 0) {
                    throw error("unclosed <");
                }
                IRI iri = IRI.create(s.substring(pos + 1, end));
                pos = end + 1;
                return df.getOWLNamedIndividual(iri);
            }
            if (c == '_' && s.startsWith("_:", pos)) {
                int start = pos;
                while (pos < s.length() && !Character.isWhitespace(s.charAt(pos)) && s.charAt(pos) != ')') {
                    pos++;
                }
                return anonymous.getOWLAnonymousIndividual(s.substring(start, pos));
            }
            if (c == '"') {
                return literal();
            }
            throw error("unexpected " + c);
        }

        OWLLiteral literal() {
            pos++;
            StringBuilder lex = new StringBuilder();
            while (pos < s.length() && s.charAt(pos) != '"') {
                char c = s.charAt(pos++);
                if (c == '\\' && pos < s.length()) {
                    char n = s.charAt(pos++);
                    lex.append(n == 'n' ? '\n' : n == 'r' ? '\r' : n);
                } else {
                    lex.append(c);
                }
            }
            if (pos >= s.length()) {
                throw error("unclosed literal");
            }
            pos++;
            if (s.startsWith("@", pos)) {
                int start = ++pos;
                while (pos < s.length() && (Character.isLetterOrDigit(s.charAt(pos)) || s.charAt(pos) == '-')) {
                    pos++;
                }
                return df.getOWLLiteral(lex.toString(), s.substring(start, pos));
            }
            if (s.startsWith("^^<", pos)) {
                int end = s.indexOf('>', pos);
                if (end < 0) {
                    throw error("unclosed datatype");
                }
                IRI dt = IRI.create(s.substring(pos + 3, end));
                pos = end + 1;
                return df.getOWLLiteral(lex.toString(), df.getOWLDatatype(dt));
            }
            return df.getOWLLiteral(lex.toString(), "");
        }
    }
}
