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
package org.semanticweb.owlapi.vocab;

import static org.semanticweb.owlapi.model.EntityType.*;

import javax.annotation.Nonnull;

import org.semanticweb.owlapi.model.EntityType;
import org.semanticweb.owlapi.model.HasIRI;
import org.semanticweb.owlapi.model.HasPrefixedName;
import org.semanticweb.owlapi.model.HasShortForm;
import org.semanticweb.owlapi.model.IRI;

/**
 * rdf-array: the built-in entities of OWL-array (owl-array.md §3.2), in the namespace
 * {@value #NAMESPACE}. Their meaning is fixed by the semantics, not by the vocabulary ontology
 * that declares them ({@link #VOCABULARY_ONTOLOGY}).
 */
public enum RDFArrayVocabulary implements HasShortForm, HasIRI, HasPrefixedName {
    //@formatter:off
    /** ra:Sequence, the sequences and nothing else */                SEQUENCE("Sequence", CLASS),
    /** ra:EmptySequence, the class of () */                          EMPTY_SEQUENCE("EmptySequence", CLASS),
    /** ra:ObjectSequence, no literal members */                      OBJECT_SEQUENCE("ObjectSequence", CLASS),
    /** ra:DataSequence, no individual members */                     DATA_SEQUENCE("DataSequence", CLASS),
    /** ra:member, each element that is an individual or a sequence */ MEMBER("member", OBJECT_PROPERTY),
    /** ra:first, the first element when an individual */             FIRST("first", OBJECT_PROPERTY),
    /** ra:last, the last element when an individual */               LAST("last", OBJECT_PROPERTY),
    /** ra:memberValue, each element that is a literal */             MEMBER_VALUE("memberValue", DATA_PROPERTY),
    /** ra:firstValue, the first element when a literal */            FIRST_VALUE("firstValue", DATA_PROPERTY),
    /** ra:lastValue, the last element when a literal */              LAST_VALUE("lastValue", DATA_PROPERTY),
    /** ra:length, the number of elements */                          LENGTH("length", DATA_PROPERTY);
    //@formatter:on

    /** The ra: namespace. */
    public static final String NAMESPACE = "https://rdf-array.github.io/rdf-array/vocab#";
    /** The prefix conventionally bound to the namespace. */
    public static final String PREFIX_NAME = "ra";
    /** The vocabulary ontology, which declares the entities and states their axioms. */
    public static final IRI VOCABULARY_ONTOLOGY = IRI.create("https://rdf-array.github.io/rdf-array/vocab/owl-array.ttl");

    @Nonnull
    private final String localName;
    @Nonnull
    private final IRI iri;
    @Nonnull
    private final EntityType<?> entityType;
    @Nonnull
    private final String prefixedName;

    RDFArrayVocabulary(@Nonnull String localName, @Nonnull EntityType<?> entityType) {
        this.localName = localName;
        this.entityType = entityType;
        prefixedName = PREFIX_NAME + ':' + localName;
        iri = IRI.create(NAMESPACE, localName);
    }

    /** @return the kind of entity */
    public EntityType<?> getEntityType() {
        return entityType;
    }

    /** @return the local name */
    public String getLocalName() {
        return localName;
    }

    @Override
    public IRI getIRI() {
        return iri;
    }

    @Override
    public String getShortForm() {
        return localName;
    }

    @Override
    public String getPrefixedName() {
        return prefixedName;
    }
}
