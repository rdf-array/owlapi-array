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

/**
 * rdf-undef: the empty slot of a sequence, written {@code undef} in Turtle-array and SPARQL-array,
 * the word SPARQL's {@code VALUES} uses for "no value here". A slot is a position with no value: not
 * a term, not an individual and not a literal, so it stands nowhere but as an element of an
 * {@link OWLSequence}, where it counts towards the length ({@code ( 1 undef 3 )} has length 3) and
 * is equal only to another slot. It says that there is nothing at the position, not that something
 * unknown is there, which a blank node says. There is one slot,
 * {@link OWLDataFactory#getOWLUndef()}; a visitor meets it through {@link OWLUndefVisitor}.
 */
public interface OWLUndef extends OWLPropertyAssertionObject {}
