/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SQLi
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.plugins.php.phpunit.sensor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;

import org.apache.maven.project.MavenProject;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.plugins.php.core.resources.PhpFile;
import org.sonar.plugins.php.phpunit.configuration.PhpUnitConfiguration;

import com.thoughtworks.xstream.XStreamException;

/**
 * The Class PhpUnitResultParserTest.
 */
public class PhpUnitResultParserTest {

  /** The context. */
  private SensorContext context;

  /** The config. */
  private PhpUnitConfiguration config;

  /** The project. */
  private Project project;

  /** The metric. */
  private Metric metric;

  /**
   * Inits the.
   */
  private void init() {
    try {
      config = mock(PhpUnitConfiguration.class);
      project = mock(Project.class);
      context = mock(SensorContext.class);
      MavenProject mavenProject = mock(MavenProject.class);
      ProjectFileSystem fs = mock(ProjectFileSystem.class);
      when(project.getPom()).thenReturn(mavenProject);
      when(project.getFileSystem()).thenReturn(fs);
      when(fs.getSourceDirs()).thenReturn(Arrays.asList(new File("C:\\projets\\PHP\\Monkey\\sources\\main")));
      when(fs.getTestDirs()).thenReturn(Arrays.asList(new File("C:\\projets\\PHP\\Monkey\\Sources\\test")));
      when(mavenProject.getPackaging()).thenReturn("php");
      File reportFile = new File(getClass().getResource("/org/sonar/plugins/php/phpunit/sensor/phpunit.xml").getFile());
      when(config.getReportFile()).thenReturn(reportFile);
      PhpUnitResultParser parser = new PhpUnitResultParser(project, context);
      parser.parse(config.getReportFile());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Should not throw an exception when report not found.
   */
  @Test
  public void shouldNotThrowAnExceptionWhenReportNotFound() {
    config = mock(PhpUnitConfiguration.class);
    project = mock(Project.class);
    context = mock(SensorContext.class);
    MavenProject mavenProject = mock(MavenProject.class);
    when(project.getPom()).thenReturn(mavenProject);
    when(mavenProject.getPackaging()).thenReturn("maven-plugin");
    when(config.getReportFile()).thenReturn(new File("path/to/nowhere"));
    PhpUnitResultParser parser = new PhpUnitResultParser(project, context);
    parser.parse(null);
  }

  /**
   * Should save zero value when report not found.
   */
  @Test
  public void shouldSaveZeroValueWhenReportNotFound() {
    metric = CoreMetrics.TESTS;
    config = mock(PhpUnitConfiguration.class);
    project = mock(Project.class);
    context = mock(SensorContext.class);
    MavenProject mavenProject = mock(MavenProject.class);
    when(project.getPom()).thenReturn(mavenProject);
    when(mavenProject.getPackaging()).thenReturn("maven-plugin");
    PhpUnitResultParser parser = new PhpUnitResultParser(project, context);
    parser.parse(null);
    verify(context).saveMeasure(metric, 0.0);
  }

  /**
   * Should throw an exception when report is invalid.
   */
  @Test(expected = XStreamException.class)
  public void shouldNotThrowAnExceptionWhenReportIsInvalid() {
    config = mock(PhpUnitConfiguration.class);
    project = mock(Project.class);
    context = mock(SensorContext.class);
    MavenProject mavenProject = mock(MavenProject.class);
    when(project.getPom()).thenReturn(mavenProject);
    when(mavenProject.getPackaging()).thenReturn("maven-plugin");
    when(config.getReportFile()).thenReturn(
        new File(getClass().getResource("/org/sonar/plugins/php/phpunit/sensor/phpunit-invalid.xml").getFile()));
    PhpUnitResultParser parser = new PhpUnitResultParser(project, context);
    parser.parse(config.getReportFile());
  }

  /**
   * Should generate tests metrics.
   */
  @Test()
  public void shouldGenerateTestsMeasures() {
    metric = CoreMetrics.TESTS;
    init();
    verify(context).saveMeasure(new PhpFile("Monkey", true), metric, 3.0);
    verify(context).saveMeasure(new PhpFile("Banana", true), metric, 1.0);
  }

  /**
   * Should generate failure test metrics.
   */
  @Test()
  public void shouldGenerateFailedMeasures() {
    metric = CoreMetrics.TEST_FAILURES;
    init();
    verify(context).saveMeasure(new PhpFile("Monkey", true), metric, 2.0);
    verify(context).saveMeasure(new PhpFile("Banana", true), metric, 0.0);
  }

  /**
   * Should generate error test metrics.
   */
  @Test()
  public void shouldGenerateErrorMeasures() {
    metric = CoreMetrics.TEST_ERRORS;
    init();
    verify(context).saveMeasure(new PhpFile("Monkey", true), metric, 1.0);
    verify(context).saveMeasure(new PhpFile("Banana", true), metric, 1.0);
  }

  /**
   * Should generate execution time test metrics.
   */
  @Test()
  public void shouldGenerateTestExecutionTimeMeasures() {
    metric = CoreMetrics.TEST_EXECUTION_TIME;
    init();
    PhpFile monkey = new PhpFile("Monkey", true);
    verify(context).saveMeasure(monkey, metric, 447.0);
    verify(context).saveMeasure(monkey, CoreMetrics.TESTS, 3.0);
    verify(context).saveMeasure(monkey, CoreMetrics.TEST_ERRORS, 1.0);
    verify(context).saveMeasure(monkey, CoreMetrics.TEST_FAILURES, 2.0);
    verify(context).saveMeasure(monkey, CoreMetrics.TEST_FAILURES, 2.0);
    verify(context).saveMeasure(monkey, CoreMetrics.TEST_SUCCESS_DENSITY, 0.0);
    verify(context).saveMeasure(new PhpFile("Banana", true), metric, 570.0);
  }

}