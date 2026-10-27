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
package org.semanticweb.owlapi.io;

import static org.semanticweb.owlapi.util.OWLAPIPreconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.util.SequenceNodeID;

/**
 * rdf-array: a sequence term, ( e1 … en ), in an RDF graph: one node, not an rdf:first/rdf:rest
 * ladder. Its elements are RDF nodes, nested sequences included. Its IRI is the reversible
 * {@code _:seq_…} node ID, so that code which writes blank nodes writes a sequence it was not taught
 * to spell as that node ID. It is a kind of blank node, so that code which handles blank nodes
 * (root anonymous nodes, deferred rendering) handles a sequence until it is taught better.
 */
public class RDFResourceSequence extends RDFResourceBlankNode {

    private static final long serialVersionUID = 40000L;
    @Nonnull
    private final List<RDFNode> elements;

    /**
     * @param elements
     *        the elements, in order
     * @param isIndividual
     *        true for a sequence in the data, false for one of OWL's own lists
     */
    public RDFResourceSequence(@Nonnull List<? extends RDFNode> elements, boolean isIndividual) {
        super(IRI.create(SequenceNodeID.encodeSpellings(spellings(checkNotNull(elements, "elements cannot be null")))),
            isIndividual, false, false);
        this.elements = Collections.unmodifiableList(new ArrayList<>(elements));
    }

    private static List<String> spellings(List<? extends RDFNode> elements) {
        List<String> spellings = new ArrayList<>();
        for (RDFNode e : elements) {
            spellings.add(spell(e));
        }
        return spellings;
    }

    private static String spell(RDFNode e) {
        if (e instanceof RDFLiteral) {
            RDFLiteral l = (RDFLiteral) e;
            return SequenceNodeID.spellLiteral(l.getLexicalValue(), l.getLang(),
                l.getDatatype() == null ? null : l.getDatatype().toString());
        }
        return SequenceNodeID.spellNode(e.getIRI().toString());
    }

    /** @return the elements, in order */
    @Nonnull
    public List<RDFNode> getElements() {
        return elements;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RDFResourceSequence && super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Nonnull
    @Override
    public String toString() {
        return SequenceNodeID.spellingOf(getIRI().toString());
    }

    @Override
    public String getNodeIDValue() {
        return getIRI().toString().substring(2);
    }
}
