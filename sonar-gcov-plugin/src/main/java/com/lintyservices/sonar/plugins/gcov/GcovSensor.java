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

import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class GcovSensor implements Sensor {

  private static final Logger LOG = Loggers.get(GcovSensor.class);
  private static final String FALSESTRING = "false";

  private FileSystem fs;
  private Configuration configuration;
  private String defaultEnable = FALSESTRING;

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
    String enable = configuration.get(GcovPlugin.ENABLE_COVERAGE).orElse(defaultEnable);
    if (!enable.equalsIgnoreCase(FALSESTRING)) {
      List<Path> paths = new ArrayList<>();
      try {
        Files.walk(Paths.get(fs.baseDir().getAbsolutePath())).filter(Files::isRegularFile).filter(o -> o.toString().toLowerCase().endsWith(".gcov")).forEach(o1 -> paths.add(o1));
      } catch (Exception e) {
        // FIXME: We should fail if no report can be found. Where are those reported located?
        LOG.warn("Error while trying to get gcov reports");
      }
      for (Path path : paths) {
        File report = path.toFile();
        if (!report.isFile() || !report.exists() || !report.canRead()) {
          // FIXME: On log per path?
          LOG.warn("Gcov report not found at {}", report);
        } else
          parseReport(report, context);
      }
    }
  }

  public void parseReport(File file, SensorContext context) {
    LOG.info("parsing {}", file);
    GcovReportParser.parseReport(file, context);
  }

  public void defaultToTrue() {
    this.defaultEnable = "true";
  }

  public void defaultToFalse() {
    this.defaultEnable = FALSESTRING;
  }
}
