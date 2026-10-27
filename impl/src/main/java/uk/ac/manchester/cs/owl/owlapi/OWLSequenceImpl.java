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
package uk.ac.manchester.cs.owl.owlapi;

import static org.semanticweb.owlapi.util.OWLAPIPreconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.semanticweb.owlapi.model.NodeID;
import org.semanticweb.owlapi.model.OWLAnnotationSubjectVisitor;
import org.semanticweb.owlapi.model.OWLAnnotationSubjectVisitorEx;
import org.semanticweb.owlapi.model.OWLAnnotationValueVisitor;
import org.semanticweb.owlapi.model.OWLAnnotationValueVisitorEx;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLIndividualVisitor;
import org.semanticweb.owlapi.model.OWLIndividualVisitorEx;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectVisitor;
import org.semanticweb.owlapi.model.OWLObjectVisitorEx;
import org.semanticweb.owlapi.model.OWLPropertyAssertionObject;
import org.semanticweb.owlapi.model.OWLSequence;
import org.semanticweb.owlapi.util.OWLObjectTypeIndexProvider;
import org.semanticweb.owlapi.util.SequenceNodeID;

/**
 * rdf-array: a sequence term, ( e1 … en ), an individual equal by its elements (owl-array.md §3).
 * Its node ID is the reversible {@code _:seq_…} of {@link SequenceNodeID}; equality, hash code and
 * order go by the element list, never by the node ID.
 */
public class OWLSequenceImpl extends OWLAnonymousIndividualImpl implements OWLSequence {

    private static final long serialVersionUID = 40000L;
    @Nonnull
    private final List<OWLPropertyAssertionObject> elements;

    /**
     * @param elements
     *        the elements, in order
     */
    public OWLSequenceImpl(@Nonnull List<? extends OWLPropertyAssertionObject> elements) {
        super(NodeID.getNodeID(SequenceNodeID.encode(checkNotNull(elements, "elements cannot be null"))));
        this.elements = Collections.unmodifiableList(new ArrayList<>(elements));
    }

    @Override
    public List<OWLPropertyAssertionObject> getElements() {
        return elements;
    }

    @Override
    protected int index() {
        return OWLObjectTypeIndexProvider.SEQUENCE;
    }

    @Override
    protected int compareObjectOfSameType(OWLObject object) {
        List<OWLPropertyAssertionObject> other = ((OWLSequence) object).getElements();
        for (int i = 0; i < elements.size() && i < other.size(); i++) {
            int diff = elements.get(i).compareTo(other.get(i));
            if (diff != 0) {
                return diff;
            }
        }
        return elements.size() - other.size();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        return obj instanceof OWLSequence && elements.equals(((OWLSequence) obj).getElements());
    }

    @Override
    public int hashCode() {
        return 31 * elements.hashCode() + 7;
    }

    @Override
    public void accept(OWLObjectVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public <O> O accept(OWLObjectVisitorEx<O> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void accept(OWLIndividualVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public <O> O accept(OWLIndividualVisitorEx<O> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void accept(OWLAnnotationValueVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public <O> O accept(OWLAnnotationValueVisitorEx<O> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void accept(OWLAnnotationSubjectVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public <E> E accept(OWLAnnotationSubjectVisitorEx<E> visitor) {
        return visitor.visit(this);
    }

    // The named individuals among the elements, at any depth, are in the signature.
    @Override
    public void addSignatureEntitiesToSet(Set<OWLEntity> entities) {
        for (OWLPropertyAssertionObject e : elements) {
            if (e instanceof OWLSequence) {
                ((OWLSequenceImpl) e).addSignatureEntitiesToSet(entities);
            } else if (e instanceof OWLNamedIndividual) {
                entities.add((OWLNamedIndividual) e);
            } else if (!(e instanceof OWLAnonymousIndividual)) {
                entities.addAll(e.getSignature());   // a literal's datatype
            }
        }
    }

    // A sequence is a value, not a shared blank node: only the anonymous individuals inside it count.
    @Override
    public void addAnonymousIndividualsToSet(Set<OWLAnonymousIndividual> anons) {
        for (OWLPropertyAssertionObject e : elements) {
            if (e instanceof OWLSequence) {
                ((OWLSequenceImpl) e).addAnonymousIndividualsToSet(anons);
            } else if (e instanceof OWLAnonymousIndividual) {
                anons.add((OWLAnonymousIndividual) e);
            }
        }
    }
}
