/*
 * SonarQube Linty Gcov :: Plugin
 * Copyright (C) 2019-2020 Linty Services
 * mailto:contact@linty-services.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.lintyservices.sonar.plugins.gcov;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class GcovReportParser {

  private final SensorContext context;

  private boolean definedPath = false;

  private int previousLineNumber = 0;

  private String previousLineCode = "";

  private int branchHits = 0;

  private int branchMisses = 0;

  private NewCoverage coverage;

  private static final Logger LOGGER = Loggers.get(GcovReportParser.class);

  private GcovReportParser(SensorContext context) {
    this.context = context;
  }

  public static void parseReport(File gcovFile, SensorContext context) {
    new GcovReportParser(context).parse(gcovFile);
  }

  private void parse(File gcovFile) {
    BufferedReader reader;
    try {
      reader = new BufferedReader(new FileReader(gcovFile));
      try {
        String line = reader.readLine();
        while (line != null) {
          line = line.replaceAll("\\s", "");
          collectReportMeasures(line);
          line = reader.readLine();
        }
        if (coverage != null)
          coverage.save();
      } finally {
        reader.close();
      }
    } catch (Exception e) {
      LOGGER.warn("Error while trying to parse gcov file");
    }
  }

  private void collectReportMeasures(String line) {
    String path;
    InputFile resource;
    String[] lines = line.split("\\:", 3);
    if (lines[0].startsWith("branch")) {
      String branchLine = lines[0];
      branchLine = branchLine.substring(7);
      if (branchLine.startsWith("taken") && !branchLine.substring(6).startsWith("0"))
        branchHits++;
      else
        branchMisses++;
    } else {
      int totalBranches = branchHits + branchMisses;
      if (totalBranches != 0 && previousLineNumber != 0 && coverage != null && (previousLineCode.startsWith("if") || previousLineCode.startsWith("elsif") || previousLineCode.startsWith("case") || previousLineCode.contains("when"))) {
        coverage.conditions(previousLineNumber, totalBranches, branchHits);
      }
      try {
        String absolutePath = "";
        try {
          absolutePath = context.fileSystem().baseDir().getAbsolutePath().replace("\\", "/").replace(" ", "");
        } catch (Exception e) {
        }
        int lineNumber = Integer.parseInt(lines[1]);
        if (lineNumber == 0 && !definedPath) {
          definedPath = true;
          path = lines[2].replaceFirst("Source:", "");
          if (path.startsWith("/"))
            path = path.substring(1);
          path = path.replace(absolutePath, "");
          try {
            path = path.replace(absolutePath.substring(1), "");
          } catch (Exception e) {
          }
          try {
            path = path.replace(absolutePath.substring(2), "");
          } catch (Exception e) {
          }
          try {
            path = path.replace(absolutePath.substring(3), "");
          } catch (Exception e) {
          }
          if (path.startsWith("/"))
            path = path.substring(1);
          resource = context.fileSystem().inputFile(context.fileSystem().predicates().hasPath(path));
          if (resourceExists(resource)) {
            coverage = context.newCoverage();
            coverage.onFile(resource);
          }
        } else if (lineNumber != 0 && coverage != null) {
          int visits = 0;
          if (!lines[0].equals("-")) {
            try {
              visits = Integer.parseInt(lines[0]);
            } catch (NumberFormatException e) {
              //LOGGER.warn("Abnormal characters in gcov report");
            }
            coverage.lineHits(lineNumber, visits);
          }
          previousLineNumber = lineNumber;
          previousLineCode = lines[2];
        }
      } catch (Exception e) {
        //LOGGER.warn("Ignored line in gcov report");
      }
      branchHits = 0;
      branchMisses = 0;
    }
  }

  private boolean resourceExists(InputFile file) {
    return file != null;
  }

}
