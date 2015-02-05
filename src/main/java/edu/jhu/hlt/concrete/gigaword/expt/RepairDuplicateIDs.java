/*
 * Copyright 2012-2015 Johns Hopkins University HLTCOE. All rights reserved.
 * See LICENSE in the project root directory.
 */

package edu.jhu.hlt.concrete.gigaword.expt;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class RepairDuplicateIDs {

  private static final Logger logger = LoggerFactory.getLogger(RepairDuplicateIDs.class);
  private static final String suffix = ".duplicate";

  public static final String repairDuplicate(String orig) {
    return orig + suffix;
  }

  public static final String extractDocumentId(String sgmlDocId) {
    String[] spl = sgmlDocId.split("\"");
    return spl[1];
  }

  /**
   *
   */
  private RepairDuplicateIDs() {
    // TODO Auto-generated constructor stub
  }

  public static void main(String[] args) {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {

      @Override
      public void uncaughtException(Thread t, Throwable e) {
        logger.error("Thread {} caught unhandled exception.", t.getName());
        logger.error("Unhandled exception.", e);
      }
    });

    if (args.length != 2) {
      logger.info("Usage: {} {} {}", RepairDuplicateIDs.class.getName(), "path/to/data/dir", "path/to/out/folder");
      System.exit(1);
    }

    String rootPathStr = args[0];
    String outPathStr = args[1];

    Path rootPath = Paths.get(rootPathStr);
    if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
      logger.error("File: {} does not exist or is not a directory. Re-run with the correct path to"
          + " the annotated gigaword .gz files.");
      System.exit(1);
    }

    Path outPath = Paths.get(outPathStr);
    if(!Files.exists(outPath)) {
      logger.debug("Creating directory: {}", outPath.toString());
      try {
        Files.createDirectories(outPath);
      } catch (IOException e) {
        logger.error("Caught an IOException when creating output dir.", e);
        System.exit(1);
      }
    }

    Path outFile = outPath.resolve("filename-ids.txt.gz");

    // Approx. 1mil docs.
    ConcurrentHashMap<String, Set<String>> filenameToIdMap = new ConcurrentHashMap<>(1000 * 1000);

    // Data dir contains a few folders.
    try(Stream<Path> rootPaths = Files.list(rootPath)) {
      rootPaths
      .filter(p -> Files.isDirectory(p))
      .forEach(p -> {
        try (Stream<Path> subPaths = Files.list(p)) {
          subPaths
            .filter(np -> !Files.isDirectory(np))
            .parallel()
            .forEach(gzp -> {
              try (InputStream is = Files.newInputStream(gzp);
                  GZIPInputStream gis = new GZIPInputStream(is);
                  Reader r = new InputStreamReader(gis, StandardCharsets.UTF_8);
                  BufferedReader br = new BufferedReader(r);
                  Stream<String> lines = br.lines()) {
                Set<String> idSet = new HashSet<>(1000 * 10);
                lines
                  .filter(l -> l.startsWith("<DOC id=\""))
                  .forEach(l -> {
                    final String reducedId = extractDocumentId(l);
                    boolean added = idSet.add(reducedId);
                    if (!added) {
                      logger.debug("Found a duplicate ID: {}. Repairing it.", reducedId);
                      String repped = repairDuplicate(reducedId);
                      // logger.info("New ID: {}", repped);

                      if (!idSet.add(repped))
                        throw new IllegalArgumentException("More than one level of duplicates.");
                    }
                  });
                filenameToIdMap.put(gzp.toString(), idSet);
              } catch (IOException e) {
                logger.error("Caught IOException when reading in file.", e);
                logger.error("Path: {}", gzp.toString());
              }
            });
        } catch (IOException e) {
          logger.error("Error iterating over root path {}", rootPath.toString());
        }});

      try (OutputStream os = Files.newOutputStream(outFile);
          GZIPOutputStream gos = new GZIPOutputStream(os);
          Writer w = new OutputStreamWriter(gos, StandardCharsets.UTF_8);
          BufferedWriter bwr = new BufferedWriter(w)) {
        ExperimentUtils.exptMapToFile(filenameToIdMap, bwr);
      }
    } catch (IOException e) {
      logger.error("Error iterating over root path {}", rootPath.toString());
    }
  }
}
