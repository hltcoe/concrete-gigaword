/*
 * Copyright 2012-2015 Johns Hopkins University HLTCOE. All rights reserved.
 * See LICENSE in the project root directory.
 */
package edu.jhu.hlt.concrete.gigaword.expt;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;

/**
 * Class that contains utilities for dealing with the Gigaword corpus.
 */
public class ExperimentUtils {

  private ExperimentUtils() {
    // TODO Auto-generated constructor stub
  }

  public static Map<String, Set<String>> createFilenameToIdMap(Reader is) throws IOException {
    // try (Scanner sc = new Scanner(is, cs.toString());) {
    try (Scanner sc = new Scanner(is)) {
      Map<String, Set<String>> filenameToIdMap = new HashMap<>();
      while (sc.hasNextLine()) {
        String line = sc.nextLine();
        String[] split = line.split(" ");
        String k = split[0];
        String v = split[1];
        if (filenameToIdMap.containsKey(k)) {
          boolean res = filenameToIdMap.get(k).add(v);
          if (!res)
            throw new IllegalArgumentException("Tried to add to a set but failed... must be duplicate IDs?");
        } else {
          Set<String> ss = new HashSet<>();
          ss.add(v);
          filenameToIdMap.put(k, ss);
        }
      }

      return filenameToIdMap;
    }
  }

  static void exptMapToFile(ConcurrentHashMap<String, Set<String>> map, String marker, Writer writer) throws IOException {
    for (Map.Entry<String, Set<String>> e : map.entrySet()) {
      final String c1 = e.getKey();
      Set<String> vals = e.getValue();
      for (String id : vals) {
        StringBuilder sb = new StringBuilder();
        sb.append(c1);
        sb.append(marker);
        sb.append(id);
        sb.append("\n");

        writer.write(sb.toString());
      }
    }
  }

  static void exptMapToFile(ConcurrentHashMap<String, Set<String>> map, Writer writer) throws IOException {
    exptMapToFile(map, " ", writer);
  }

  public static Reader createReader(Path p, Charset cs) throws IOException {
    InputStream is = Files.newInputStream(p);
    InputStream gis;
    if (p.toString().endsWith(".gz"))
      gis = new GZIPInputStream(is);
    else
      gis = is;
    Reader r = new InputStreamReader(gis, cs);
    return r;
  }

  public static Reader createReader(Path p) throws IOException {
    return createReader(p, StandardCharsets.UTF_8);
  }
}
