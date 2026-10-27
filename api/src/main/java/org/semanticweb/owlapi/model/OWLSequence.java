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
package org.semanticweb.owlapi.model;

import java.util.List;

import javax.annotation.Nonnull;

/**
 * rdf-array: a sequence term, ( e1 … en ), read as an individual of a sort of its own (owl-array.md
 * §3). Its elements are named individuals, anonymous individuals, literals or sequences. Two
 * sequences are equal exactly when their element lists are equal; a sequence has no IRI and no
 * label. It is a kind of anonymous individual, so that code which handles anonymous individuals
 * handles a sequence unchanged until it is taught better; its node ID ({@link #getID()}) is the
 * reversible {@code _:seq_…} of {@link org.semanticweb.owlapi.util.SequenceNodeID}, which carries it
 * through the syntaxes that have no spelling for it yet.
 */
public interface OWLSequence extends OWLAnonymousIndividual {

    /**
     * @return the elements, in order; empty for the empty sequence ()
     */
    @Nonnull
    List<OWLPropertyAssertionObject> getElements();

    /**
     * @return the number of elements
     */
    default int getLength() {
        return getElements().size();
    }
}
