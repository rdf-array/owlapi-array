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
package org.semanticweb.owlapi.rdfarray;

import javax.annotation.Nonnull;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;

/**
 * rdf-array: wraps the factory of a reasoner that does not know sequences (HermiT, JFact, ELK) so
 * that its reasoners meet owl-array.md §3 through the expansion of §6 ({@link SequenceAxioms}).
 * Nothing is added to the ontology the user edits or saves.
 */
public class SequenceAwareReasonerFactory implements OWLReasonerFactory {

    @Nonnull
    private final OWLReasonerFactory delegate;
    private final boolean closure;

    /**
     * @param delegate the factory of the reasoner to wrap
     */
    public SequenceAwareReasonerFactory(@Nonnull OWLReasonerFactory delegate) {
        this(delegate, true);
    }

    /**
     * @param delegate the factory of the reasoner to wrap
     * @param closure false for an OWL 2 EL reasoner, which cannot express the closure axioms or the
     *        vocabulary's complements and unions
     */
    public SequenceAwareReasonerFactory(@Nonnull OWLReasonerFactory delegate, boolean closure) {
        this.delegate = delegate;
        this.closure = closure;
    }

    @Override
    public String getReasonerName() {
        return delegate.getReasonerName() + " (rdf-array sequences)";
    }

    @Override
    public OWLReasoner createNonBufferingReasoner(OWLOntology ontology) {
        return createNonBufferingReasoner(ontology, new SimpleConfiguration());
    }

    @Override
    public OWLReasoner createReasoner(OWLOntology ontology) {
        return createReasoner(ontology, new SimpleConfiguration());
    }

    @Override
    public OWLReasoner createNonBufferingReasoner(OWLOntology ontology, OWLReasonerConfiguration config) {
        return new SequenceAwareReasoner(ontology, delegate, config, closure, false);
    }

    @Override
    public OWLReasoner createReasoner(OWLOntology ontology, OWLReasonerConfiguration config) {
        return new SequenceAwareReasoner(ontology, delegate, config, closure, true);
    }
}
