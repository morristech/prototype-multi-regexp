# RegExtractor

Library for building efficient log-line extractor from a multi-regexp input definition,
starting with an ordered set of regular expressions (a subset of the usual Java regexp definition;
more on this below),
building a big [DFA](https://en.wikipedia.org/wiki/Deterministic_finite_automaton)
using excellent [Automaton](http://www.brics.dk/automaton/), as well as complementary extractors
for actual value extraction.

## Basic operation

(to be completed)

## Basic usage

```java
DefinitionReader r = DefinitionReader.reader(new File("extractions.conf"));
RegExtractor extractor = r.read();
final String TEST_INPUT = "prefix: time=12546778 verb=PUT";
ExtractionResult result = extractor.extract(TEST_INPUT);
if (result == null) { // no match, handle
   throw new IllegalArgumentException("no match!");
}
Map<String,Object> properties = asMap();
// and then use extracted property values
```

## Extractor input definition

(to be completed)

## Regexp supported

Expressions supported for named and inline patterns can be thought of either as a subset of
the full `java.util.regexp.Pattern`, or as a superset of what [Automaton](http://www.brics.dk/automaton/)
`RegExp` implementation supports
(see [Automaton RegExp Javadocs](http://www.brics.dk/automaton/doc/index.html?dk/brics/automaton/RegExp.html)).

Additions above and beyond `Automaton` `RegExp` are:

* Quoted control characters like `\\t` (stock Automaton does NOT allow those, only literal tabs!)
* Addition of pre-defined character classes `\d`/`\D`, `s`/`S`, `\w`/`\W`

Basic `Automaton` supports

* Simple character classes (one level of brackets, optionally starting with `^` for negation`
* Basic repetition markers `*` (Kleene star), `+`, `?`, `{n}`
* Grouping (`(....)`)
* Literal escaping with `\` (that is, character immediately following is used as-is)
    * NOTE: due to extension here, literal quoting is ONLY used for-alphanumeric characters!
* Concatentation, union (`|`)

but none of the extension features are enabled, to make it more likely that the same input
patterns can be used with both `Automaton` and the regexp-based extractors.





