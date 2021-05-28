/*
 * SonarQube Linty Gcov :: Plugin
 * Copyright (C) 2019-2021 Linty Services
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

import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GcovSensor implements Sensor {

  private static final Logger LOG = Loggers.get(GcovSensor.class);

  private final FileSystem fs;
  private final Configuration configuration;

  public GcovSensor(FileSystem fs, Configuration configuration) {
    this.fs = fs;
    this.configuration = configuration;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.name("GcovSensor");
  }

  @Override
  public void execute(SensorContext context) {
    String gcovEnabled = configuration.get(GcovPlugin.ENABLE_COVERAGE).orElse("false");
    if (!gcovEnabled.equals("true")) {
      LOG.info("[Gcov] Code coverage computation from Gcov is not activated on this project");
      return;
    }

    List<Path> gcovReportPaths;
    LOG.info("[Gcov] Retrieving list of Gcov report files");
    try (Stream<Path> streamPaths = Files.walk(Paths.get(fs.baseDir().getAbsolutePath()))) {
      gcovReportPaths = streamPaths
        .filter(Files::isRegularFile)
        .filter(o -> o.toString().toLowerCase().endsWith(".gcov"))
        .collect(Collectors.toList());
    } catch (IOException e) {
      throw new IllegalStateException("[Gcov] Cannot retrieve list of Gcov report files", e);
    }

    for (Path gcovReportPath : gcovReportPaths) {
      File gcovReportFile = gcovReportPath.toFile();
      if (!gcovReportFile.isFile() || !gcovReportFile.exists() || !gcovReportFile.canRead()) {
        LOG.warn("[Gcov] Gcov file cannot be processed: {}", gcovReportFile);
      } else {
        parseFile(gcovReportFile, context);
      }
    }
  }

  public void parseFile(File file, SensorContext context) {
    LOG.info("[Gcov] Parsing Gcov file: {}", file);
    GcovReportParser gcovParser = new GcovReportParser(context);
    gcovParser.parseFile(file);
  }
}
