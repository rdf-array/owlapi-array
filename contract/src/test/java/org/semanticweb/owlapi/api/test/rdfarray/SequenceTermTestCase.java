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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividualVisitorEx;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLPropertyAssertionObject;
import org.semanticweb.owlapi.model.OWLSequence;
import org.semanticweb.owlapi.util.RemappingIndividualProvider;
import org.semanticweb.owlapi.util.SequenceNodeID;

/** rdf-array: the OWLSequence term (owl-array.md §3.1) and its reversible node ID (§4). */
public class SequenceTermTestCase {

    private static final OWLDataFactory df = OWLManager.getOWLDataFactory();
    private static final OWLNamedIndividual a = df.getOWLNamedIndividual(IRI.create("http://example/a"));
    private static final OWLNamedIndividual b = df.getOWLNamedIndividual(IRI.create("http://example/b"));

    private static OWLSequence seq(OWLPropertyAssertionObject... elements) {
        return df.getOWLSequence(Arrays.asList(elements));
    }

    @Test
    public void equalByElements() {
        OWLSequence s1 = seq(a, df.getOWLLiteral(1), seq(b));
        OWLSequence s2 = seq(a, df.getOWLLiteral(1), seq(b));
        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
        assertEquals(0, s1.compareTo(s2));
        assertEquals(s1.getID(), s2.getID());
        assertNotEquals(seq(a, b), seq(b, a));
        assertNotEquals(seq(a), seq(a, a));
        assertEquals(3, s1.getLength());
    }

    @Test
    public void notEqualToAnOrdinaryAnonymousIndividual() {
        OWLSequence s = seq(a);
        OWLAnonymousIndividual anon = df.getOWLAnonymousIndividual();
        assertNotEquals(s, anon);
        assertNotEquals(anon, s);
        assertTrue(s.isAnonymous());
        assertNotEquals(0, s.compareTo(anon));
    }

    @Test
    public void emptySequence() {
        OWLSequence empty = df.getOWLSequence(Collections.<OWLPropertyAssertionObject> emptyList());
        assertEquals(0, empty.getLength());
        assertEquals("()", SequenceNodeID.spell(empty.getElements()));
        assertNotEquals(empty, seq(seq()));
    }

    @Test
    public void nodeIdRoundTrip() {
        OWLAnonymousIndividual blank = df.getOWLAnonymousIndividual("_:x1");
        OWLSequence s = seq(a, df.getOWLLiteral("say \"hi\"\n", "en"), df.getOWLLiteral(2.5D),
            seq(), seq(b, blank));
        String id = s.getID().getID();
        assertTrue(id.startsWith(SequenceNodeID.PREFIX));
        assertTrue(id.matches("_:seq_[A-Za-z0-9_-]+"), id);
        // the data factory decodes the node ID back to the sequence
        assertEquals(s, df.getOWLAnonymousIndividual(id));
        List<OWLPropertyAssertionObject> decoded = SequenceNodeID.decode(id, df, df::getOWLAnonymousIndividual);
        assertEquals(s.getElements(), decoded);
    }

    @Test
    public void remappingKeepsTheSequenceAndRemapsItsBlankNodesConsistently() {
        RemappingIndividualProvider p = new RemappingIndividualProvider(df, true);
        OWLAnonymousIndividual blank = df.getOWLAnonymousIndividual("_:x2");
        OWLSequence s = seq(a, blank);
        OWLAnonymousIndividual read = p.getOWLAnonymousIndividual(s.getID().getID());
        assertTrue(read instanceof OWLSequence);
        OWLPropertyAssertionObject inner = ((OWLSequence) read).getElements().get(1);
        // the blank node inside is the one the rest of the document gets for _:x2
        assertEquals(p.getOWLAnonymousIndividual("_:x2"), inner);
        assertEquals(a, ((OWLSequence) read).getElements().get(0));
    }

    @Test
    public void visitorsSeeASequenceOrAnAnonymousIndividual() {
        OWLSequence s = seq(a);
        OWLIndividualVisitorEx<String> plain = new OWLIndividualVisitorEx<String>() {

            @Override
            public String visit(OWLNamedIndividual individual) {
                return "named";
            }

            @Override
            public String visit(OWLAnonymousIndividual individual) {
                return "anonymous";
            }
        };
        assertEquals("anonymous", s.accept(plain));
        OWLIndividualVisitorEx<String> aware = new OWLIndividualVisitorEx<String>() {

            @Override
            public String visit(OWLNamedIndividual individual) {
                return "named";
            }

            @Override
            public String visit(OWLAnonymousIndividual individual) {
                return "anonymous";
            }

            @Override
            public String visit(OWLSequence sequence) {
                return "sequence of " + sequence.getLength();
            }
        };
        assertEquals("sequence of 1", s.accept(aware));
    }

    @Test
    public void signatureHoldsTheNamedElements() {
        OWLSequence s = seq(a, seq(b), df.getOWLLiteral(1));
        assertTrue(s.getSignature().contains(a));
        assertTrue(s.getSignature().contains(b));
        assertTrue(s.getIndividualsInSignature().containsAll(Arrays.asList(a, b)));
    }
}
