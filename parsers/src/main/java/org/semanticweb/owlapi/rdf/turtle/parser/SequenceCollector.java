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
package org.semanticweb.owlapi.rdf.turtle.parser;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.util.SequenceNodeID;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.semanticweb.owlapi.vocab.XSDVocabulary;

/**
 * rdf-array: collects the elements of a Turtle collection, {@code ( … )}, into one sequence node
 * instead of an rdf:first/rdf:rest ladder. While a collection is parsed the parser's handler is this
 * collector: each element arrives as the object of {@link #CELL} {@code rdf:first} and is recorded;
 * every other triple (a blank node property list's inside the collection, say) is passed on to the
 * handler outside. A nested collection's collector wraps this one, and hands it the nested
 * sequence node as an element.
 */
class SequenceCollector implements TripleHandler {

    /** The placeholder subject each element is parsed against. */
    static final IRI CELL = IRI.create("urn:rdf-array:turtle-collection-cell");
    private static final IRI FIRST = OWLRDFVocabulary.RDF_FIRST.getIRI();
    @Nonnull
    private final TripleHandler outer;
    private final List<String> elements = new ArrayList<>();

    SequenceCollector(@Nonnull TripleHandler outer) {
        this.outer = outer;
    }

    /** @return the handler outside the collection */
    TripleHandler outer() {
        return outer;
    }

    /** @return the IRI of the sequence node for the elements collected */
    IRI sequenceNode() {
        return IRI.create(SequenceNodeID.encodeSpellings(elements));
    }

    private boolean isElement(IRI subject, IRI predicate) {
        return CELL.equals(subject) && FIRST.equals(predicate);
    }

    @Override
    public void handleTriple(IRI subject, IRI predicate, IRI object) {
        if (isElement(subject, predicate)) {
            elements.add(SequenceNodeID.spellNode(object.toString()));
        } else {
            outer.handleTriple(subject, predicate, object);
        }
    }

    @Override
    public void handleTriple(IRI subject, IRI predicate, String object) {
        if (isElement(subject, predicate)) {
            elements.add(SequenceNodeID.spellLiteral(object, null, null));
        } else {
            outer.handleTriple(subject, predicate, object);
        }
    }

    @Override
    public void handleTriple(IRI subject, IRI predicate, String object, @Nullable String lang) {
        if (isElement(subject, predicate)) {
            elements.add(SequenceNodeID.spellLiteral(object, lang, null));
        } else {
            outer.handleTriple(subject, predicate, object, lang);
        }
    }

    @Override
    public void handleTriple(IRI subject, IRI predicate, String object, @Nullable IRI datatype) {
        if (isElement(subject, predicate)) {
            elements.add(SequenceNodeID.spellLiteral(object, null,
                datatype == null ? XSDVocabulary.STRING.getIRI().toString() : datatype.toString()));
        } else {
            outer.handleTriple(subject, predicate, object, datatype);
        }
    }

    @Override
    public void handlePrefixDirective(String prefixName, String prefix) {
        outer.handlePrefixDirective(prefixName, prefix);
    }

    @Override
    public void handleBaseDirective(IRI base) {
        outer.handleBaseDirective(base);
    }

    @Override
    public void handleComment(String comment) {
        outer.handleComment(comment);
    }

    @Override
    public void handleEnd() {
        outer.handleEnd();
    }
}
