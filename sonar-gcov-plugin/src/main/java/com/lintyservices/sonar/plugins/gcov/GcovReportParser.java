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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class GcovReportParser {

  private final SensorContext context;
  private NewCoverage coverage;

  private String previousLineCode = "";
  private int previousLineNumber = 0;
  private int branchHits = 0;
  private int branchMisses = 0;

  public GcovReportParser(SensorContext context) {
    this.context = context;
  }

  public void parseFile(File gcovFile) {
    try (BufferedReader reader = new BufferedReader(new FileReader(gcovFile))) {
      String line = reader.readLine();
      while (line != null) {
        line = line.replaceAll("\\s", "");
        collectCoverageData(line);
        line = reader.readLine();
      }
      if (coverage != null) {
        coverage.save();
      }
    } catch (Exception e) {
      throw new IllegalStateException("[Gcov] Cannot parse Gcov report", e);
    }
  }

  private void collectCoverageData(String line) {
    if (getLineNumber(line) == 0) {
      if (coverage == null) {
        createCoverageObject(line);
      }
    } else if (coverage != null) {
      if (isBranchLine(line)) {
        computeBranchHits(line);
      } else {
        // No longer in a branch context => Save branch coverage from the previous lines if it exists
        saveBranchCoverageData();
        resetBranchCoverageData();
        saveLineCoverageData(line);
      }
    }
  }

  private void createCoverageObject(String line) {
    String absolutePath = context.fileSystem().baseDir().getAbsolutePath().replace("\\", "/").replace(" ", "");
    String path = getVhdlFileRelativePath(absolutePath, getLineDetails(line)[2]);
    InputFile resource = context.fileSystem().inputFile(context.fileSystem().predicates().hasPath(path));
    if (vhdlFileExist(resource)) {
      coverage = context.newCoverage();
      coverage.onFile(resource);
    }
  }

  private void saveLineCoverageData(String line) {
    String[] lineDetails = getLineDetails(line);
    int lineNumber = getLineNumber(line);

    if (!lineDetails[0].equals("-")) {
      int visits = 0;
      try {
        visits = Integer.parseInt(lineDetails[0]);
      } catch (NumberFormatException e) {
        // TODO: Is it possible? Or report is wrong? Comments?
        // Do not take into account this line as it is not a line about code coverage
      }
      coverage.lineHits(lineNumber, visits);
    }
    previousLineNumber = lineNumber;
    previousLineCode = lineDetails[2];
  }

  private void computeBranchHits(String line) {
    String branchLine = getLineDetails(line)[0];
    branchLine = branchLine.substring(7);
    if (branchLine.startsWith("taken") && !branchLine.substring(6).startsWith("0")) {
      branchHits++;
    } else {
      branchMisses++;
    }
  }

  private void saveBranchCoverageData() {
    int totalBranches = branchHits + branchMisses;
    if (totalBranches != 0 && previousLineNumber != 0 && coverage != null
      && (previousLineCode.startsWith("if") || previousLineCode.startsWith("elsif") || previousLineCode.startsWith("case") || previousLineCode.contains("when"))) {
      coverage.conditions(previousLineNumber, totalBranches, branchHits);
    }
  }

  private void resetBranchCoverageData() {
    branchHits = 0;
    branchMisses = 0;
  }

  private boolean vhdlFileExist(InputFile file) {
    return file != null;
  }

  private String getVhdlFileRelativePath(String gcovReportAbsolutePath, String vhdlFilePathLineInGcovReport) {
    return vhdlFilePathLineInGcovReport
      .replaceFirst("Source:", "")
      .replace(gcovReportAbsolutePath, "");
  }

  private boolean isBranchLine(String line) {
    return getLineDetails(line)[0].startsWith("branch");
  }

  private int getLineNumber(String line) {
    try {
      return Integer.parseInt(getLineDetails(line)[1]);
    } catch (Exception e) {
      // Not a line containing line coverage information (could be a line containing branch coverage information for instance)
      return -1;
    }
  }

  private String[] getLineDetails(String line) {
    return line.split("\\:", 3);
  }
}
