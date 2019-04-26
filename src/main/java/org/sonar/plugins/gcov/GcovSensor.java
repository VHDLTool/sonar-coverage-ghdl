/*
 * sonar-coverage-ghdl plugin for Sonarqube & GHDL
 * Copyright (C) 2019 Linty Services
 * 
 * Based on :
 *  SonarQube Cobertura Plugin
 * Copyright (C) 2018-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.sonar.plugins.gcov;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.Settings;

import com.s2ceh.sonar.plugins.vhdl.CCPP;
import com.s2ceh.sonar.plugins.vhdl.Vhdl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class GcovSensor implements Sensor {

	private static final Logger LOGGER = LoggerFactory.getLogger(GcovSensor.class);

	private FileSystem fs;

	private Configuration configuration;

	public GcovSensor(FileSystem fs, Settings settings,
			Configuration configuration) {
		this.fs = fs;
		this.configuration=configuration;
	}

	@Override
	public void describe(SensorDescriptor descriptor) {
		descriptor.onlyOnLanguages(Vhdl.KEY,CCPP.KEY).name("GcovSensor");
	}

	@Override
	public void execute(SensorContext context) {
		String enable=configuration.get(GcovPlugin.ENABLE_COVERAGE).orElse("false");
		if (!enable.equalsIgnoreCase("false")) {
			List<Path> paths = new ArrayList <>();
			try {
				Files.walk(Paths.get(fs.baseDir().getAbsolutePath())).filter(Files::isRegularFile).filter(o->o.toString().toLowerCase().endsWith(".gcov")).forEach(o1->paths.add(o1));
			} catch (IOException e) {
				LOGGER.warn("Error while trying to get gcov reports");
			}
			for (Path path : paths) {
				File report = path.toFile();
				if (!report.isFile() || !report.exists() || !report.canRead()) {
					LOGGER.warn("Gcov report not found at {}", report);
				} 
				else 
					parseReport(report, context);
			}
		}
	}

	public void parseReport(File file, SensorContext context) {
		LOGGER.info("parsing {}", file);
		GcovReportParser.parseReport(file, context);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

}
