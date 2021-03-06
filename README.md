Deprecated
===
This library has been deprecated. Please see
[this page](https://github.com/hltcoe/concrete-java/tree/master/ingesters/gigaword)
for information about the latest Concrete Gigaword ingester.

If starting a project using Concrete and Gigaword, please use the above link
to the main concrete-java project.

Concrete Gigaword
====
Library to take Gigaword documents and convert them to Concrete `Communication` objects.

Maven dependency
---
```xml
<dependency>
  <groupId>edu.jhu.hlt</groupId>
  <artifactId>concrete-gigaword</artifactId>
  <version>4.4.0</version>
</dependency>
```

Quick start / API Usage
---
Create converter object:
```java
ConcreteGigawordDocumentFactory factory = new ConcreteGigawordDocumentFactory();
```

SGML `.gz` file to `Iterator<Communication>`:

```java
Path gzPath = Paths.get("path/to/sgml/file.gz");
Iterator<Communication> iter = factory.iterator(gzPath);
while (iter.hasNext()) {
  Communication c = iter.next();
  // process c
}
```

Concretely Annotated Gigaword
---
See `GIGAWORD.md` for instructions about how to reproduce the Concrete representation of English Gigaword v5, one of the data sets described in the publication `Concretely Annotated Corpora`.

License
---
Apache 2
