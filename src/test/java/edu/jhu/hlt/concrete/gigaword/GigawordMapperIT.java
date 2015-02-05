package edu.jhu.hlt.concrete.gigaword;

import static org.junit.Assert.fail;
import edu.jhu.hlt.concrete.Communication;
import edu.jhu.hlt.concrete.gigaword.expt.ExperimentUtils;
import edu.jhu.hlt.concrete.gigaword.expt.RepairDuplicateIDs;
import edu.jhu.hlt.concrete.util.ConcreteException;
import gigaword.api.GigawordDocumentConverter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.zip.GZIPInputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GigawordMapperIT {

  private static final Logger logger = LoggerFactory.getLogger(GigawordMapperIT.class);

  String prop = "fileToIdSetPath";
  Optional<String> exptFile = Optional.ofNullable(System.getProperty(prop));
  GigawordDocumentConverter conv = new GigawordDocumentConverter();

  Path pathToLDC;
  Map<String, Set<String>> exptMap;

  @Before
  public void setUp() throws Exception {
    if (!exptFile.isPresent())
      fail("Property: '" + prop + "' was not set. It must be set to run the integration test.");
    pathToLDC = Paths.get(exptFile.get());
    try (InputStream is = Files.newInputStream(pathToLDC);) {
      InputStream gis;
      if (pathToLDC.toString().endsWith(".gz"))
        gis = new GZIPInputStream(is);
      else
        gis = is;
      try (Reader r = new InputStreamReader(gis, StandardCharsets.UTF_8);
          BufferedReader br = new BufferedReader(r)) {
        logger.info("Parsing expt map.");
        exptMap = ExperimentUtils.createFilenameToIdMap(r);
      }
    }
  }

  @After
  public void tearDown() throws Exception {

  }

  @Test
  public void test() {
    ConcurrentLinkedDeque<String> q = new ConcurrentLinkedDeque<>();
    logger.info("Beginning integration test.");
    this.exptMap
      .entrySet()
      // .parallelStream()
      .forEach(e -> {
        final String pathStr = e.getKey();
        final Set<String> ids = e.getValue();

        logger.info("Testing path: {}", pathStr);
//        Iterator<GigawordDocument> gdi = conv.iterator(pathStr);
//        while (gdi.hasNext()) {
//          GigawordDocument doc = gdi.next();
//          final String did = doc.getId();
//          List<TextSpan> tsl = doc.getTextSpans();
//          tsl.forEach(ts -> {
//            final int bg = ts.getStart();
//            final int en = ts.getEnding();
//            final int diff = en - bg;
//            if (diff == 1) {
//              logger.info("Section length of {} in document: {}", 1, did);
//            } else if (diff == 0) {
//              logger.info("Section length of {} in document: {}", 0, did);
//            }
//          });
//
//          // logger.info("Retrieved document: {}", did);
//          // logger.info("Headline: {}", doc.getHeadline());
//          // logger.info("Dateline: {}", doc.getDateline());
//          // logger.info("Text: {}", doc.getText());
//
//          // Document ID must be in the set. Remove.
//          boolean wasInSet = ids.remove(did);
//          if (!wasInSet) {
//            logger.debug("ID: {} was parsed from path: {}, but was not in the experiment map. Attempting to remove dupe.", did, pathStr);
//            String newId = RepairDuplicateIDs.repairDuplicate(did);
//            boolean dupeRemoved = ids.remove(newId);
//            if (!dupeRemoved) {
//              logger.info("Failed to remove dupe.");
//              q.add(pathStr);
//              return;
//            }
//          }
//        }


        Iterator<Communication> citer;
        try {
          citer = new ConcreteGigawordDocumentFactory().iterator(Paths.get(pathStr));
          while (citer.hasNext()) {
            Communication c = citer.next();
            String cId = c.getId();

            // Document ID must be in the set. Remove.
            boolean wasInSet = ids.remove(cId);
            if (!wasInSet) {
              logger.debug("ID: {} was parsed from path: {}, but was not in the experiment map. Attempting to remove dupe.", cId, pathStr);
              String newId = RepairDuplicateIDs.repairDuplicate(cId);
              boolean dupeRemoved = ids.remove(newId);
              if (!dupeRemoved) {
                logger.info("Failed to remove dupe.");
                q.add(pathStr);
                return;
              }
            }
          }
        } catch (ConcreteException ex) {
          logger.error("Caught ConcreteException during Concrete mapping.", ex);
          logger.error("Path: {}", pathStr);
        }


        // Set should be empty at end of iterating.
        int nDocsLeft = ids.size();
        if (nDocsLeft > 0) {
          q.add(pathStr);
        } else
          logger.info("Path {} passed.", pathStr);
      });

    logger.info("Test complete.");
    if (q.size() > 0) {
      logger.info("The following paths failed:");
      for (String s : q)
        logger.info(s);

      fail("Some paths did not pass.");
    }
  }
}
