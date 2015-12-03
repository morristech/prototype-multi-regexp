# RegExtractor

Library for building efficient log-line extractor from a multi-regexp input definition,
starting with an ordered set of regular expressions (a subset of the usual Java regexp definition;
more on this below),
building a big [DFA](https://en.wikipedia.org/wiki/Deterministic_finite_automaton)
using excellent [Automaton](http://www.brics.dk/automaton/), as well as complementary extractors
for actual value extraction.

## Basic operation

To use RegExtractor, you need three things:

1. This library: comes in a single jar which includes shaded dependencies (so-called "uber-jar")
2. Extraction definition: often a `File`, either stand-alone or a resource from within bigger jar; or possibly read from external storage like Amazon S3
3. Input, in form of `java.lang.String`s, often coming from a line-oriented input source like (a set of) log file(s).

## Basic usage

Assuming you have file `extractions.xtr` which contains extraction definition (2), and wanted to extract values out of it, you could use:

```java
DefinitionReader r = DefinitionReader.reader(new File("extractions.xtr"));
RegExtractor extractor = r.read();
final String TEST_INPUT = "prefix: time=12546778 verb=PUT";
ExtractionResult result = extractor.extract(TEST_INPUT);
if (result == null) { // no match, handle
   throw new IllegalArgumentException("no match!");
}
Map<String,Object> properties = asMap();
// and then use extracted property values
```

and a sample extraction definition could be something like:

```
pattern %num \d+
pattern %word \w+
template @extractTime time=$time(%num)
template @extractVerb verb=$verb(%word)
extract SimpleEntry {
   template prefix: @extractTime @extractVerb
}
```

and as a result you would get Map like:

```json
{
  "time" : "12546778",
  "verb" : "PUT"
}

```

## Extractor input definition

Extractor input definition is a line-oriented text document, consisting of 3 kinds of declarations:

1. Pattern declarations: define low-level building blocks that consists of snippets of Regular Expressions and/or references to other patterns
2. Template declarations: define intermediate building blocks that consist of references to patterns, literal text segments, references to named templates, inlined patterns and extractors
3. Extraction declarations: named matching rules that associate a named template with output, possible augmented by additional properties

In addition to these declarations, individual `extractors` are declared as part of templates of extractions.

### Sample Extractor Input Definition

TO BE WRITTEN

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





