# SonarQube Linty Gcov Plugin

This plugin is now useless as this feature is available out-of-the-box from gcovr: https://gcovr.com/en/master/output/sonarqube.html


## Feature

This plugin allows to import Gcov reports data in SonarQube.
Gcov reports can be obtained from VHDL files using ghdl-gcc.

## Build Plugin

Without integration tests:

```bash
mvn clean package
```

With integration tests on SonarQube 9.7.0.61563 version:

```bash
mvn clean verify -Pits -Dsonar.runtimeVersion=9.7.0.61563
```

Update license headers:
```bash
mvn license:format -Pits
```

## Update All Dependencies

```bash
# Check for Maven dependencies to update
mvn org.codehaus.mojo:versions-maven-plugin:2.12.0:display-dependency-updates -Pits

# Check for Maven plugins to update
mvn org.codehaus.mojo:versions-maven-plugin:2.12.0:display-plugin-updates -Pits

# Check for versions in properties to update
mvn org.codehaus.mojo:versions-maven-plugin:2.12.0:display-property-updates -Pits

# Update parent POM
mvn org.codehaus.mojo:versions-maven-plugin:2.12.0:update-parent
```

## Usage

All .gcov files present in the project directory will be analysed. The results will be shown in the project's coverage
tab in Sonarqube.

More information

1. [Install Ghdl](https://github.com/Linty-Services/sonar-coverage-ghdl/wiki/GHDL-Installation)
2. [Get gcov results](https://github.com/Linty-Services/sonar-coverage-ghdl/wiki/Gcov-results)
3. [Run an analysis](https://github.com/Linty-Services/sonar-coverage-ghdl/wiki/Run-Analysis)
