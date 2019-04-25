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

import com.google.common.collect.ImmutableList;
import org.sonar.api.CoreProperties;
import org.sonar.api.Plugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

import java.util.List;

public final class GcovPlugin implements Plugin{

  public static final String ENABLE_COVERAGE = "true";

  public List<Object> getExtensions() {
    return ImmutableList.of(
            PropertyDefinition.builder(ENABLE_COVERAGE)
                    .category(CoreProperties.CATEGORY_CODE_COVERAGE)
                    .subCategory("Gcov")
                    .name("Enable coverage")
                    .description("Enable parsing of gcov files and display of coverage results in sonarqube.")
                    .defaultValue("true")
                    .onQualifiers(Qualifiers.PROJECT)
                    .build(),

            GcovSensor.class);
  }

  @Override
  public void define(Context context) {
    context.addExtensions(getExtensions());
  }
}
