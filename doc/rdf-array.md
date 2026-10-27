# owlapi-array: the OWL API with sequences as terms

rdf-array is RDF whose data model has one more kind of term, the **sequence**: `(1 2 3)` in Turtle
is one term holding its elements in order, not a ladder of `rdf:first`/`rdf:rest` blank nodes, and
`()` is the empty sequence, not `rdf:nil`. See
[rdf-array.md](https://rdf-array.github.io/rdf-array/rdf-array.html) for the model.
[OWL-array](https://rdf-array.github.io/rdf-array/owl-array.html) is what that means for OWL: the
lists OWL's own mapping to RDF uses are sequences, and a sequence in the data is an individual of a
sort of its own, equal by its elements, with built-in properties to talk about it.

This fork of the [OWL API](https://github.com/owlcs/owlapi) implements OWL-array. It starts from the
tag `owlapi-parent-4.5.29`, the 4.x line that Protégé 5.6 loads, on the branch `rdf-sequences`. It
has no forked dependencies: the OWL API has its own Turtle and RDF/XML parsers and renderers.

| module | what it does here | upstream version |
|---|---|---|
| `api` | `OWLSequence` (an `OWLAnonymousIndividual`), its visitors, `OWLDataFactory.getOWLSequence`, `SequenceNodeID`, `RDFArrayVocabulary`, `RDFResourceSequence` | 4.5.29 |
| `impl` | `OWLSequenceImpl`, equal, hashed and ordered by elements; the data factory decodes `_:seq_…` node IDs | 4.5.29 |
| `parsers` | the native Turtle parser and the RDF/XML parser read a collection as one node; the RDF consumer reads OWL's lists from sequences | 4.5.29 |
| `oboformat`, `rio`, `tools`, `fixers` | unchanged; the Rio formats still read and write ladders ([#19](https://github.com/rdf-array/rdf-array/issues/19)) | 4.5.29 |
| `apibinding` | `org.semanticweb.owlapi.rdfarray`: the expansion of owl-array.md §6 and the reasoner decorator | 4.5.29 |
| `contract` | the tests; HermiT 1.4.5.456 as a test dependency | 4.5.29 |

The other rdf-array implementations: [jena-array](https://github.com/rdf-array/jena-array) (Java),
[comunica-array](https://github.com/rdf-array/comunica-array) and
[shex.js-array](https://github.com/rdf-array/shex.js-array) (JavaScript),
[rdflib-array](https://github.com/rdf-array/rdflib-array) and
[PyShEx-array](https://github.com/rdf-array/PyShEx-array) (Python),
[oxigraph-array](https://github.com/rdf-array/oxigraph-array) and
[rudof-array](https://github.com/rdf-array/rudof-array) (Rust).

## Setting up

The build needs Maven and a JDK 8 or newer (it was run with JDK 21). Javadoc fails under JDK 21, in
the `distribution` module too, so skip it and that module:

```bash
git clone -b rdf-sequences git@github.com:rdf-array/owlapi-array.git
cd owlapi-array
mvn install -pl '!distribution' -Dmaven.javadoc.skip -Dno-javadoc=true -Dgpg.skip
```

`mvn install` puts `4.5.29` jars in the local repository, where they shadow the upstream ones of the
same version; use `-Dmaven.repo.local=…` to keep them apart.

## What a sequence does here

```java
OWLDataFactory df = OWLManager.getOWLDataFactory();
OWLSequence s = df.getOWLSequence(Arrays.asList(df.getOWLNamedIndividual(IRI.create("http://ex/a")),
    df.getOWLLiteral(2), df.getOWLSequence(Collections.emptyList())));
s.equals(df.getOWLSequence(s.getElements()));          // true: equal by elements
s.getID().getID();                                      // "_:seq_KCA8aHR0cDovL2V4L2E-ICIy…"
OWLReasoner r = new SequenceAwareReasonerFactory(new org.semanticweb.HermiT.ReasonerFactory())
    .createReasoner(ontology);
r.getSequenceInstances(df.getOWLClass(IRI.create("http://ex/BigPaper")), false);
```

```turtle
@prefix ra: <https://rdf-array.github.io/rdf-array/vocab#> .
ex:Person owl:equivalentClass [ owl:intersectionOf ( ex:Agent [ owl:onProperty ex:age ; owl:someValuesFrom xsd:integer ] ) ] .
ex:p dc:creator ( ex:ada ex:bob ) .                    # one individual, an OWLSequence
ex:Paper rdfs:subClassOf [ owl:onProperty dc:creator ; owl:allValuesFrom [ owl:onProperty ra:member ; owl:allValuesFrom foaf:Agent ] ] .
```

- **The term.** `OWLSequence extends OWLAnonymousIndividual` holds a list of
  `OWLPropertyAssertionObject` (named and anonymous individuals, literals, sequences); `getLength()`.
  Equality, hashing and `compareTo` go by elements; its type index sorts after the anonymous
  individuals. Because it is an anonymous individual, every axiom that takes an individual takes a
  sequence, and code that knows nothing of sequences sees an anonymous individual. Its signature
  holds its named elements; `getAnonymousIndividuals()` holds the anonymous individuals inside it,
  not the sequence itself.
- **Visitors.** `OWLSequenceVisitor` and `OWLSequenceVisitorEx<O>` declare `visit(OWLSequence)` as a
  Java 8 default that calls `visit(OWLAnonymousIndividual)`; `OWLIndividualVisitor(Ex)`,
  `OWLAnnotationValueVisitor(Ex)` and `OWLAnnotationSubjectVisitor(Ex)` extend them, so existing
  visitors compile and treat a sequence as an anonymous individual unless they override the method.
  One parent interface carries the default so that `OWLObjectVisitor`, which inherits all three,
  meets one default, not three.
- **The node ID.** A sequence's node ID is `_:seq_` and the unpadded base64url of the UTF-8
  N-Triples-array spelling of its elements (`SequenceNodeID`). Equal sequences have equal node IDs;
  an anonymous element keeps its own label inside the spelling. `OWLDataFactoryImpl` and
  `RemappingIndividualProvider` decode such an ID back to the sequence, the latter resolving the
  inner labels as it resolves the rest of the document's. This is how a sequence travels through
  Functional-style, OWL/XML and Manchester syntax, which have no spelling for it yet (owl-array.md
  §4): it is written as that anonymous individual and read back as the sequence. Other OWL tools
  read it as an ordinary anonymous individual.
- **OWL's lists.** The RDF consumer no longer collects `rdf:first`/`rdf:rest`; it keeps the
  sequences the parsers hand it (`isSequence`, `getSequenceElements`), and the list translators read
  `owl:intersectionOf`, `owl:unionOf`, `owl:oneOf`, `owl:members`, `owl:propertyChainAxiom`,
  `owl:hasKey`, `owl:disjointUnionOf`, `owl:withRestrictions`, `owl:onProperties`, the SWRL atom
  lists and the rest from them. A ladder spelled out with `rdf:first`/`rdf:rest` triples is ordinary
  triples and builds no list: an ontology that relies on one does not parse as before.
- **Turtle.** The native Turtle parser reads a collection as one node: the triples inside it (a
  blank node property list, a class expression) go to the consumer as usual and the collection
  becomes a sequence. The Turtle renderer writes `( … )` and `()`, a sequence as a subject, and the
  triples of anonymous elements after the triple that holds them.
- **RDF/XML.** `rdf:parseType="Collection"` is one sequence. The renderer writes a Collection when
  every element is an IRI or a blank node and refuses the rest (a literal element, a nested sequence,
  a sequence as a subject) with an `OWLRendererException` that says to save as Turtle
  ([#18](https://github.com/rdf-array/rdf-array/issues/18)). `rdf:li` stays `rdf:li` and is no
  longer numbered into `rdf:_n`.
- **Reasoning.** `SequenceAwareReasonerFactory` wraps any `OWLReasonerFactory`.
  `SequenceAwareReasoner` hands the stock reasoner an ontology in a manager of its own: the source's
  imports closure (logical axioms and declarations) with each sequence replaced by its expansion
  individual, the §6 expansion of every sequence, nested ones included, and the vocabulary's axioms
  (vocab/owl-array.ttl, built in code). The expansion individual of a ground sequence is named
  `urn:rdf-array:sequence:<the node ID's base64>`, of one holding an anonymous individual anonymous.
  Closure (`ra:member only {…}` or `max 0`, `ra:memberValue only DataOneOf(…)` or `max 0`) is on by
  default; `new SequenceAwareReasonerFactory(delegate, false)` leaves it out for an EL reasoner. The
  source ontology is never changed; a change to it is taken into account at `flush()`, or at once
  for a non-buffering reasoner. Queries that mention a sequence are rewritten on the way in, and
  `isEntailed` of an axiom with a sequence the ontology does not hold is answered over a copy with
  that sequence's expansion added.

## Changed, layer by layer

- **§1.1 the term** (`api`, `impl`): `model/OWLSequence`, `OWLSequenceVisitor`,
  `OWLSequenceVisitorEx`; `OWLIndividualVisitor(Ex)`, `OWLAnnotationValueVisitor(Ex)`,
  `OWLAnnotationSubjectVisitor(Ex)` extend them; `OWLDataFactory.getOWLSequence`;
  `util/OWLObjectTypeIndexProvider` (`SEQUENCE`); `vocab/RDFArrayVocabulary`;
  `uk.ac.manchester.cs.owl.owlapi.OWLSequenceImpl`, `OWLDataFactoryImpl`.
- **§1.2 the node ID** (`api`): `util/SequenceNodeID`; `model/NodeID` (the `_:seq_` prefix is an
  anonymous node ID and passes through `getIRIFromNodeID`); `util/RemappingIndividualProvider`.
- **§1.3 parsers** (`parsers`): `rdf/turtle/parser/SequenceCollector` (new), `TurtleParser.jj` and
  the generated `TurtleParser.java`, `TurtleOntologyParserFactory` (priority 2);
  `rdf/rdfxml/parser/StartRDF` (Collection, `rdf:li`), `OWLRDFConsumer`, `OptimisedListTranslator`,
  `Translators`, `TripleHandlers` (the `rdf:first`/`rdf:rest`/`rdf:List` handlers removed).
- **§1.4 renderers** (`api`, `parsers`): `io/RDFResourceSequence` (new);
  `rdf/model/AbstractTranslator`, `RDFTranslator`, `RDFGraph`; `rdf/RDFRendererBase`;
  `rdf/turtle/renderer/TurtleRenderer`; `rdf/rdfxml/renderer/RDFXMLRenderer`.
- **§1.5 reasoning** (`apibinding`, `contract/pom.xml`): `org.semanticweb.owlapi.rdfarray`
  `SequenceAxioms`, `SequenceAwareReasoner`, `SequenceAwareReasonerFactory`; HermiT as a test
  dependency.
- **§1.6 tests** (`contract`): `api/test/rdfarray/SequenceTermTestCase`,
  `SequenceParsingTestCase`, `SequenceRoundTripTestCase`, `SequenceReasoningTestCase`;
  `api/test/baseclasses/RdfArrayAssumptions` and the calls to it listed under Tests; the re-recorded
  fixtures.

## Tests

`mvn test -pl contract` (with the modules above installed): **4673 tests, 0 failures, 0 errors, 318
skipped**, of which 316 are rdf-array flags and 2 are upstream's own (`BOMSafeInputStreamAndParseTestCase`, and
`SWRLRoundTripTestCase`'s "man syntax does not like annotations"). Before the fork: 4640 tests, 0 failures, 2 skipped.

- **Added**, 33: `SequenceTermTestCase` 7 (equality by elements, inequality with an ordinary
  anonymous individual, `()`, the node ID both ways, remapping, visitors, signature);
  `SequenceParsingTestCase` 7 (OWL's lists from sequences, a class expression inside a list, a data
  sequence as one individual, literals and blank nodes inside, a sequence subject, a ladder is not a
  list, RDF/XML's Collection); `SequenceRoundTripTestCase` 6 (Turtle, RDF/XML without literals,
  RDF/XML's refusal of a literal, the node ID through Functional-style, OWL/XML and Manchester);
  `SequenceReasoningTestCase` 13 with HermiT (owl-array.md §3.3 and §3.4: every author is an agent,
  `ex:p a ex:BigPaper` from `ra:length`, a property chain through `ra:first`, `ra:Sequence` and not
  `ra:EmptySequence`, `()`, `ra:last`, the three-point ring and the two-member RGB inconsistent, a
  sequence as a nominal; §3.5: `ra:member min 3` not entailed of `(ex:a ex:a ex:b)`, `ra:member only
  C` holds of a literal-only sequence; the source ontology unchanged; reading Turtle-array).
- **Re-recorded**, 9 fixtures whose RDF/XML spelled OWL's lists as ladders of IRIs and blank nodes,
  now `rdf:parseType="Collection"`: `ObjectIntersectionOf.rdf`, `ObjectUnionOf.rdf`,
  `ObjectOneOf.rdf`, `DataIntersectionOf.rdf`, `DataUnionOf.rdf`, `DatatypeRestriction.rdf`;
  `annotatedpropertychain.ttl.rdf` (N-Triples with a ladder, now `( … )`); `apitest/TAIL.txt` and
  `apitest/swrlParser.txt` (the SWRL head and body, an empty body as an empty Collection).
- **Flagged**, 316, skipped with the reason through JUnit's assumptions
  (`RdfArrayAssumptions`):
  - 300 parameterised round trips (`AbstractRoundTrippingTestCase` 230,
    `AnnotatedAxiomRoundTripTestCase` 49, `BlankNodeIdsAndAnnotationsRoundTripTestCase` 14,
    `DifferentIndividualsPairRoundTripTestCase` 7) of an ontology that
    holds a list, in a format that cannot carry it: a Rio format (the reason `RIO`,
    [#19](https://github.com/rdf-array/rdf-array/issues/19)), or RDF/XML when the list holds a
    literal or a sequence or is a subject (`RDF_XML`, [#18](https://github.com/rdf-array/rdf-array/issues/18)).
  - 9 Rio tests (`RioParserTestCase` 4, `RioRendererTestCase` 5) that count the triples a ladder
    gives (`RIO`, #19).
  - 6 tests whose RDF/XML input spells a list of literals as a ladder, the only way RDF/XML can, so
    that it is no longer a list (`LITERAL_LADDER`, #18): `DataOneOf.rdf`
    (`FileRoundTripCorrectAxiomsTestCase`); the OWL 2 Primer's `primer.rdfxml.xml`, whose
    `toddlerAge` is `DataOneOf(1 2)` (`PrimerTestCase.shouldRDFXMLBeEquivalent`); the WebOnt
    `oneOf/premises004` (`TestCornerCasesTestCase.testWebOnt`); three of the OWL 2 profile test
    cases (`ProfileDLOnlyTestCase.testDLOnly`).
  - `OutputSyntaxSortTestCase.shouldOutputAllInSameOrder` for the one format that cannot carry its
    lists (`RIO` or `RDF_XML`).
- **Not run**: the OWL 2 conformance suite (the `contract` module carries only the cases above), and
  the reasoner tests against JFact or ELK.

## Decisions

- **The native Turtle parser first.** Its priority went from 12 to 2, ahead of the Rio Turtle parser
  (3), which builds ladders; without it a `.ttl` file was read by Rio.
- **`RDFResourceSequence extends RDFResourceBlankNode`.** A sequence node in the renderer's graph is
  a blank node to code that does not know it, holds its element nodes, and its IRI is the sequence's
  node ID.
- **RDF/XML refuses rather than writes a ladder** for a sequence it cannot spell
  ([#18](https://github.com/rdf-array/rdf-array/issues/18)): a ladder would not read back as a
  sequence.
- **Results leave out the expansion individuals.** `OWLReasoner`'s `getInstances` and the like
  return named individuals, so a sequence cannot appear there; `getSequenceInstances` and
  `getTypes(OWLSequence, boolean)` answer for sequences
  ([#21](https://github.com/rdf-array/rdf-array/issues/21)).
- **No object closure for a sequence holding an anonymous individual**: `ObjectOneOf` needs named
  individuals in OWL 2 DL, so the conservative path leaves the closure out and `ra:member only C` is
  not decided of such a sequence ([#20](https://github.com/rdf-array/rdf-array/issues/20)).
- **The vocabulary axioms are built in code** (`SequenceAxioms.vocabularyAxioms`), mirroring
  rdf-array's `vocab/owl-array.ttl`, so the decorator needs no network or resource file; the two must
  be kept in step by hand.
- **Annotations stay out of the expansion**: the reasoner sees logical axioms and declarations.
- **`rdf:li` is not numbered.** As in the other forks, `rdf:li` in RDF/XML stays `rdf:li`.
- **The legacy property chain**, a list as the subject of `rdfs:subPropertyOf`
  (`( :p :q ) rdfs:subPropertyOf :r`), is still read in non-strict mode, from a sequence subject.

## Not done

- **The Rio formats** (`rio` module: RDF4J's Turtle, RDF/XML, N-Triples, N-Quads, TriG, JSON-LD,
  RDF/JSON, TriX, binary) still read and write ladders
  ([#19](https://github.com/rdf-array/rdf-array/issues/19)); RDF4J would have to know the term.
- **The proper spellings in Functional-style, OWL/XML and Manchester** (owl-array.md §4). They carry
  a sequence as its `_:seq_…` node ID, which reads back here and reads elsewhere as an anonymous
  individual; the grammars (`OWLFunctionalSyntaxParser.jj`, `OWLXMLParserHandler`,
  `ManchesterOWLSyntaxParserImpl`) and renderers are the second step.
- **Protégé.** Not tried: building the OSGi bundle (`osgidistribution`) under the upstream symbolic
  name, loading it into Protégé 5.6, and a reasoner plugin that wraps HermiT in
  `SequenceAwareReasonerFactory` (the `owl-array` skill, §2).
- **Identity under `owl:sameAs` of elements** (owl-array.md §3.1, §6): no `HasKey` axioms per length
  are added, so two ground sequences whose elements are `sameAs` are not inferred the same.
- **ELK and JFact** are not tested; the EL path (closure off) has no test.
- The OBO format, the `tools` and `fixers` modules, and OWL API 5 are untouched.
