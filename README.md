# SonarQube Linty Gcov Plugin

## Feature

This plugin allows to import Gcov reports data in SonarQqube.
Gcov reports can be obtained from VHDL files using ghdl-gcc.

## Build Plugin

Without integration tests:
```
mvn clean package
```

With integration tests on SonarQube 7.9.4 version:
```
mvn clean verify -Pits -Dsonar.runtimeVersion=7.9.4
```

## Usage
All .gcov files present in the project directory will be analysed. The results will be shown in the project's coverage tab in Sonarqube.  

More information
  
1. [Install Ghdl](https://github.com/Linty-Services/sonar-coverage-ghdl/wiki/GHDL-Installation)  
2. [Get gcov results](https://github.com/Linty-Services/sonar-coverage-ghdl/wiki/Gcov-results)
3. [Run an analysis](https://github.com/Linty-Services/sonar-coverage-ghdl/wiki/Run-Analysis)
