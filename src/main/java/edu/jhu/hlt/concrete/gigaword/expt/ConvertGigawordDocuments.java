/*
 * Copyright 2012-2015 Johns Hopkins University HLTCOE. All rights reserved.
 * See LICENSE in the project root directory.
 */

package edu.jhu.hlt.concrete.gigaword.expt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.time.StopWatch;
import org.joda.time.Duration;
import org.joda.time.Minutes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.hlt.concrete.Communication;
import edu.jhu.hlt.concrete.gigaword.ConcreteGigawordDocumentFactory;
import edu.jhu.hlt.concrete.gigaword.GigawordConcreteConverter;
import edu.jhu.hlt.concrete.serialization.CommunicationTarGzSerializer;
import edu.jhu.hlt.concrete.serialization.TarGzCompactCommunicationSerializer;
import edu.jhu.hlt.concrete.util.ConcreteException;

/**
 *
 */
public class ConvertGigawordDocuments {

  private static final Logger logger = LoggerFactory.getLogger(ConvertGigawordDocuments.class);

  /**
   *
   */
  private ConvertGigawordDocuments() {
    // TODO Auto-generated constructor stub
  }

  /**
   * @param args
   */
  public static void main(String... args) {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {

      @Override
      public void uncaughtException(Thread t, Throwable e) {
        logger.error("Thread {} caught unhandled exception.", t.getName());
        logger.error("Unhandled exception.", e);
      }
    });

    if (args.length != 2) {
      logger.info("Usage: {} {} {}", GigawordConcreteConverter.class.getName(), "path/to/expt/file", "path/to/out/folder");
      System.exit(1);
    }

    String exptPathStr = args[0];
    String outPathStr = args[1];

    // Verify path points to something.
    Path exptPath = Paths.get(exptPathStr);
    if (!Files.exists(exptPath)) {
      logger.error("File: {} does not exist. Re-run with the correct path to " + " the experiment 2 column file. See README.md.");
      System.exit(1);
    }

    logger.info("Experiment map located at: {}", exptPathStr);

    // Create output dir if not yet created.
    Path outPath = Paths.get(outPathStr);
    if (!Files.exists(outPath)) {
      logger.info("Creating directory: {}", outPath.toString());
      try {
        Files.createDirectories(outPath);
      } catch (IOException e) {
        logger.error("Caught an IOException when creating output dir.", e);
        System.exit(1);
      }
    }

    logger.info("Output directory located at: {}", outPathStr);

    // Read in expt map. See README.md.
    Map<String, Set<String>> exptMap = null;
    try (Reader r = ExperimentUtils.createReader(exptPath); BufferedReader br = new BufferedReader(r)) {
      exptMap = ExperimentUtils.createFilenameToIdMap(br);
    } catch (IOException e) {
      logger.error("Caught an IOException when creating expt map.", e);
      System.exit(1);
    }

    // Start a timer.
    logger.info("Gigaword -> Concrete beginning.");
    StopWatch sw = new StopWatch();
    sw.start();
    // Iterate over expt map.
    exptMap.entrySet()
      // .parallelStream()
      .forEach(p -> {
      final String pathStr = p.getKey();
      final Set<String> ids = p.getValue();
      final Path lp = Paths.get(pathStr);
      logger.info("Converting path: {}", pathStr);

      // Get the file name and immediate folder it is under.
      int nElements = lp.getNameCount();
      Path fileName = lp.getName(nElements - 1);
      Path subFolder = lp.getName(nElements - 2);
      String newFnStr = fileName.toString().split("\\.")[0] + ".tar";

      // Mirror folders in output dir.
      Path localOutFolder = outPath.resolve(subFolder);
      Path localOutPath = localOutFolder.resolve(newFnStr);

      // Create output subfolders.
      if (!Files.exists(localOutFolder) && !Files.isDirectory(localOutFolder)) {
        logger.info("Creating out file: {}", localOutFolder.toString());
        try {
          Files.createDirectories(localOutFolder);
        } catch (IOException e) {
          throw new RuntimeException("Caught an IOException when creating output dir.", e);
        }
      }

      // Iterate over communications.
      Iterator<Communication> citer;
      try {
        citer = new ConcreteGigawordDocumentFactory().iterator(lp);
        Set<Communication> comms = new HashSet<>();
        while (citer.hasNext()) {
          Communication c = citer.next();
          String cId = c.getId();

          // Document ID must be in the set. Remove.
          boolean wasInSet = ids.remove(cId);
          if (!wasInSet) {
            // Some IDs are duplicated in Gigaword.
            // See ERRATA.
            logger.debug("ID: {} was parsed from path: {}, but was not in the experiment map. Attempting to remove dupe.", cId, pathStr);

            // Attempt to create a duplicate id (append .duplicate to the id).
            // Then, try to remove again.
            String newId = RepairDuplicateIDs.repairDuplicate(cId);
            boolean dupeRemoved = ids.remove(newId);
            // There are not nested duplicates, so this should never fire.
            if (!dupeRemoved) {
              logger.info("Failed to remove dupe.");
              return;
            } else
              // Modify the communication ID to the unique version.
              c.setId(newId);
          }

          comms.add(c);
        }

        CommunicationTarGzSerializer ser = new TarGzCompactCommunicationSerializer();
        ser.toTar(comms, localOutPath);
        logger.info("Finished path: {}", pathStr);
      } catch (ConcreteException ex) {
        logger.error("Caught ConcreteException during Concrete mapping.", ex);
        logger.error("Path: {}", pathStr);
      } catch (IOException e) {
        logger.error("Error archiving communications.", e);
        logger.error("Path: {}", localOutPath.toString());
      }
    });

    sw.stop();
    logger.info("Finished.");
    Minutes m = new Duration(sw.getTime()).toStandardMinutes();
    logger.info("Runtime: Approximately {} minutes.", m.getMinutes());
  }
}
