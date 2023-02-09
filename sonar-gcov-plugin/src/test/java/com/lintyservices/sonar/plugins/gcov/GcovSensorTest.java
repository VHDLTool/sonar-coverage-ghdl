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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
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
    Configuration configuration = new MapSettings().asConfig();
    settings = new MapSettings();
    sensor = new GcovSensor(fs, configuration);
    when(context.fileSystem()).thenReturn(fs);
    when(fs.predicates()).thenReturn(predicates);
    when(inputFile.file()).thenReturn(file);
    when(predicates.is(file)).thenReturn(predicate);
    when(fs.inputFile(predicate)).thenReturn(inputFile);
    when(fs.baseDir()).thenReturn(file);
    when(context.newCoverage()).thenReturn(newCoverage);
  }

  @Test
  public void should_not_fail_if_coverage_not_enabled() {
    when(pathResolver.relativeFile(any(File.class), anyString())).thenReturn(new File("noReport.gcov"));
    settings.setProperty(GcovPlugin.ENABLE_COVERAGE, "false");

    sensor.execute(context);

    verify(context, never()).newCoverage();
    verify(newCoverage, never()).save();
  }

  @Test
  public void should_properly_collect_coverage_data_on_unix_file() {
    when(fs.baseDir().getAbsolutePath()).thenReturn("/my-project/my-repo");
    when(context.fileSystem().inputFile(context.fileSystem().predicates().hasPath(anyString()))).thenReturn(inputFile);

    sensor.parseFile(getGcovFile("unix-path.gcov"), context);

    verify(context, times(1)).newCoverage();
    verify(newCoverage, times(1)).onFile(inputFile);
    verify(newCoverage).lineHits(2, 7);
    verify(newCoverage).lineHits(3, 0);
    verify(newCoverage).conditions(2, 3, 1);
    verify(newCoverage, times(1)).save();
  }

  @Test
  public void should_properly_collect_coverage_data_on_windows_file() {
    when(fs.baseDir().getAbsolutePath()).thenReturn("D:/my-project/my-repo");
    when(context.fileSystem().inputFile(context.fileSystem().predicates().hasPath(anyString()))).thenReturn(inputFile);

    sensor.parseFile(getGcovFile("windows-path.gcov"), context);

    verify(context, times(1)).newCoverage();
    verify(newCoverage, times(1)).onFile(inputFile);
    verify(newCoverage).lineHits(2, 7);
    verify(newCoverage).lineHits(3, 0);
    verify(newCoverage).conditions(2, 3, 1);
    verify(newCoverage, times(1)).save();
  }

  @Test
  public void should_not_save_coverage_data_on_non_existing_resource() {
    when(fs.baseDir().getAbsolutePath()).thenReturn("fake");
    when(fs.inputFile(predicate)).thenReturn(null);

    sensor.parseFile(getGcovFile("windows-path.gcov"), context);

    verify(context, never()).newCoverage();
  }

  // TODO: Could it really happen to pass null file?
  @Test
  public void should_trigger_an_illegal_state_exception_when_null_file_is_passed_as_gcov_file_to_parse() {
    when(context.fileSystem().inputFile(context.fileSystem().predicates().hasPath(anyString()))).thenReturn(inputFile);

    Exception thrown = assertThrows(IllegalStateException.class, () -> sensor.parseFile(null, context));
    assertEquals("[Gcov] Cannot parse Gcov report", thrown.getMessage());
  }

  @Test
  public void should_not_trigger_any_coverage_computation_as_gcov_file_is_invalid() {
    when(context.fileSystem().inputFile(context.fileSystem().predicates().hasPath(anyString()))).thenReturn(inputFile);

    sensor.parseFile(getGcovFile("badly-formatted.gcov"), context);

    verify(context, never()).newCoverage();
  }


  @Test
  // FIXME: 0 line coverage instead
  public void should_not_trigger_any_coverage_computation_as_gcov_file_does_not_contain_line_coverage_data() {
    when(fs.baseDir().getAbsolutePath()).thenReturn("D:/my-project/my-repo");
    when(context.fileSystem().inputFile(context.fileSystem().predicates().hasPath(anyString()))).thenReturn(inputFile);

    sensor.parseFile(getGcovFile("no-line-coverage.gcov"), context);

    verify(context, times(1)).newCoverage();
    verify(newCoverage, times(1)).save();
  }

  private File getGcovFile(String fileName) {
    try {
      return new File(getClass().getResource("/com/lintyservices/sonar/plugins/gcov/GcovSensorTest/" + fileName).toURI());
    } catch (URISyntaxException e) {
      throw new IllegalStateException("Cannot create test file", e);
    }
  }

}
