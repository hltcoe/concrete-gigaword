/*
 * Copyright 2012-2015 Johns Hopkins University HLTCOE. All rights reserved.
 * See LICENSE in the project root directory.
 */
package edu.jhu.hlt.concrete.gigaword;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.jhu.hlt.concrete.AnnotationMetadata;
import edu.jhu.hlt.concrete.Communication;
import edu.jhu.hlt.concrete.Section;
import edu.jhu.hlt.concrete.util.ConcreteException;
import edu.jhu.hlt.concrete.uuid.UUIDFactory;
import gigaword.api.GigawordDocumentConverter;
import gigaword.interfaces.GigawordDocument;
import gigaword.interfaces.TextSpan;

/**
 * Class that is capable of converting {@link GigawordDocument} objects to Concrete
 * {@link Communication} objects. Additionally, can stream Communication objects
 * from a {@link Path} that points to a .gz file from the Gigaword corpus.
 */
public class ConcreteGigawordDocumentFactory {

  /**
   * Get the {@link AnnotationMetadata} for the {@link ConcreteGigawordDocumentFactory} class.
   */
  public static final AnnotationMetadata getMetadata() {
    return new AnnotationMetadata()
      .setTool("ConcreteGigawordDocumentFactory")
      .setTimestamp(System.currentTimeMillis());
  }

  /**
   *
   */
  public ConcreteGigawordDocumentFactory() {
    // TODO Auto-generated constructor stub
  }

  public Communication convert(GigawordDocument gd) throws ConcreteException {
    Communication c = new Communication()
      .setUuid(UUIDFactory.newUUID())
      .setId(gd.getId())
      .setStartTime(gd.getMillis() / 1000)
      .setType(gd.getType().toString())
      .setText(gd.getText())
      .setMetadata(getMetadata());

    List<Section> sectList = new ArrayList<Section>();
    int nCtr = 0;
    for (TextSpan ts : gd.getTextSpans()) {
      Section s = new Section()
        .setUuid(UUIDFactory.newUUID())
        .setKind("Passage")
        .setTextSpan(new edu.jhu.hlt.concrete.TextSpan(ts.getStart(), ts.getEnding()));
      s.addToNumberList(nCtr);
      nCtr += 1;
      sectList.add(s);
    }

    boolean hasHeadline = gd.getHeadline().isPresent();
    boolean hasDateline = gd.getDateline().isPresent();

    // Headline + dateline --> Section 1 == Title, Section 2 == Dateline
    if (hasHeadline && hasDateline) {
      sectList.get(0).setKind("Title");
      sectList.get(1).setKind("Dateline");
    // Only headline --> Section 1 == Title
    } else if (hasHeadline && !hasDateline)
      sectList.get(0).setKind("Title");
    // Only dateline --> Section 1 == Dateline
    else if (!hasHeadline && hasDateline)
      sectList.get(0).setKind("Dateline");

    c.setSectionList(sectList);
    return c;
  }

  public Iterator<Communication> iterator(Path pathToGigawordGZ) throws ConcreteException {
    return new ConcreteGigawordDocumentIterator(pathToGigawordGZ);
  }

  private class ConcreteGigawordDocumentIterator implements Iterator<Communication> {

    final Iterator<GigawordDocument> baseIter;

    public ConcreteGigawordDocumentIterator(Path pathToGigawordGZ) {
      this.baseIter = new GigawordDocumentConverter().iterator(pathToGigawordGZ.toString());
    }

    @Override
    public boolean hasNext() {
      return this.baseIter.hasNext();
    }

    @Override
    public Communication next() {
      try {
        return convert(this.baseIter.next());
      } catch (ConcreteException e) {
        throw new RuntimeException("Exception during GigawordDocument -> Communication conversion.", e);
      }
    }
  }
}
