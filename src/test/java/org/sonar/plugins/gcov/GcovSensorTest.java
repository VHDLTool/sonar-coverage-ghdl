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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.scan.filesystem.PathResolver;

import java.io.File;
import java.net.URISyntaxException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class GcovSensorTest {

  private GcovSensor sensor;
  private MapSettings settings;

  @Mock
  private SensorContext context;
  @Mock
  private PathResolver pathResolver;
  @Mock
  private InputFile inputFile;
  @Mock
  private File file;
  @Mock
  private FileSystem fs;
  @Mock
  private FilePredicates predicates;
  @Mock
  private FilePredicate predicate;
  @Mock
  private NewCoverage newCoverage;

  @Before
  public void setUp() {
    initMocks(this);      
    Configuration configuration=new MapSettings().asConfig();
    settings = new MapSettings();
    sensor = new GcovSensor(fs, settings, configuration);
    sensor.defaultToFalse();
    sensor.toString();
    when(context.fileSystem()).thenReturn(fs);
    when(fs.predicates()).thenReturn(predicates);
    when(inputFile.file()).thenReturn(file);
    when(predicates.is(file)).thenReturn(predicate);
    when(fs.inputFile(predicate)).thenReturn(inputFile);

    when(context.newCoverage()).thenReturn(newCoverage);
  }

  @Test
  public void shouldNotFailIfReportNotSpecifiedOrNotFound() throws URISyntaxException {
    when(pathResolver.relativeFile(any(File.class), anyString()))
            .thenReturn(new File("notFound.gcov"));

    settings.setProperty(GcovPlugin.ENABLE_COVERAGE, "notFound.gcov");
    sensor.execute(context);


    File report = getCoverageReport();
    settings.setProperty(GcovPlugin.ENABLE_COVERAGE, report.getParent());
    when(pathResolver.relativeFile(any(File.class), anyString()))
            .thenReturn(report.getParentFile().getParentFile());
    sensor.execute(context);
  }

  @Test
  public void collectFileLineCoverage() throws URISyntaxException {
	when(context.fileSystem().inputFile(context.fileSystem().predicates().hasPath(anyString()))).thenReturn(inputFile);
	sensor.parseReport(getCoverageReport(), context);
    verify(context, times(1)).newCoverage();
    verify(newCoverage, times(1)).onFile(inputFile);
    verify(newCoverage).lineHits(1,0);
    verify(newCoverage).lineHits(2,7);
    verify(newCoverage).lineHits(3,0);

    verify(newCoverage, times(1)).save();
  }

  @Test
  public void testDoNotSaveMeasureOnResourceWhichDoesntExistInTheContext() throws URISyntaxException {
    when(fs.inputFile(predicate)).thenReturn(null);
    sensor.parseReport(getCoverageReport(), context);
    verify(context, never()).newCoverage();
  }
 
  @Test
  public void testForceExecute() throws URISyntaxException {
	when(context.fileSystem().inputFile(context.fileSystem().predicates().hasPath(anyString()))).thenReturn(inputFile);
    sensor.defaultToTrue();
	sensor.execute(context);
    verify(context, never()).newCoverage();
  }

  
  @Test
  public void testBadReport() throws URISyntaxException {
	when(context.fileSystem().inputFile(context.fileSystem().predicates().hasPath(anyString()))).thenReturn(inputFile);
    sensor.parseReport(new File(getClass().getResource("/org/sonar/plugins/gcov/GcovSensorTests/bad-coverage.gcov").toURI()), context);
    verify(context, never()).newCoverage();
  }
  
  @Test
  public void testSlashPath() throws URISyntaxException {
	when(context.fileSystem().inputFile(context.fileSystem().predicates().hasPath(anyString()))).thenReturn(inputFile);
    sensor.parseReport(new File(getClass().getResource("/org/sonar/plugins/gcov/GcovSensorTests/slash-coverage.gcov").toURI()), context);
    verify(context, times(1)).newCoverage();
  }

  @Test
  public void testVoidReport() throws URISyntaxException {
	when(context.fileSystem().inputFile(context.fileSystem().predicates().hasPath(anyString()))).thenReturn(inputFile);
    sensor.parseReport(null, context);
    verify(context, never()).newCoverage();
  }
  
  @Test
  public void testBadValueReport() throws URISyntaxException {
	when(context.fileSystem().inputFile(context.fileSystem().predicates().hasPath(anyString()))).thenReturn(inputFile);
    sensor.parseReport(new File(getClass().getResource("/org/sonar/plugins/gcov/GcovSensorTests/badvalue-coverage.gcov").toURI()), context);
    verify(context, times(1)).newCoverage();
  }
  
  private File getCoverageReport() throws URISyntaxException {
    return new File(getClass().getResource("/org/sonar/plugins/gcov/GcovSensorTests/commons-chain-coverage.gcov").toURI());
  }

}
