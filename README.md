Concrete Gigaword
====
Library to take Gigaword documents and convert them to Concrete `Communication` objects.

Maven dependency
---
```xml
<dependency>
  <groupId>edu.jhu.hlt</groupId>
  <artifactId>concrete-gigaword</artifactId>
  <version>4.2.1</version>
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