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


import org.sonar.api.batch.sensor.SensorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;



import org.sonar.api.batch.sensor.coverage.NewCoverage;

public class GcovReportParser {

  private final SensorContext context;
  
  private boolean definedPath=false;
  
  private NewCoverage coverage;
  
  private static final Logger LOGGER = LoggerFactory.getLogger(GcovReportParser.class);


  private GcovReportParser(SensorContext context) {
    this.context = context;
  }

  /**
   * Parse a gcov report and create measures accordingly
   */
  public static void parseReport(File gcovFile, SensorContext context) {
    new GcovReportParser(context).parse(gcovFile);
  }

  private void parse(File gcovFile) {  
	  BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(gcovFile));
			try{
			String line = reader.readLine();
			while (line != null) {	
				line=line.replaceAll("\\s","");
				collectReportMeasures(line);
				line = reader.readLine();
			}
			if(coverage!=null)
				coverage.save();
			} finally{
				reader.close();
			}			
		}
		catch (IOException e) {
			  LOGGER.warn("Error while trying to parse gcov file");
			}
  }

  private void collectReportMeasures(String line) {
	  String path;	  
	  InputFile resource;
	  String[] lines=line.split("\\:",3);
	  try {
		  int lineNumber=Integer.parseInt(lines[1]);
		  if (lineNumber==0 && !definedPath){
			  definedPath=true;
			  path=lines[2].replaceFirst("Source:", "");
			  if (path.startsWith("/"))
				  path=path.replaceFirst("/", "");
			  resource =context.fileSystem().inputFile(context.fileSystem().predicates().hasPath(path));
			  if (resourceExists(resource)) {
			    coverage = context.newCoverage();
			    coverage.onFile(resource);
			  }
		  } else if (lineNumber!=0 && coverage != null) {
			  int visits=0;
			  if(!lines[0].equals("-"))
				  try {
				  visits = Integer.parseInt(lines[0]);}
				  catch (NumberFormatException e) {
					  LOGGER.warn("Abnormal characters in gcov report");
					  }
				  coverage.lineHits(lineNumber, visits);
		  }
	  }catch (Exception e) {
		  LOGGER.warn("Ignored line in gcov report");
		  }
  }

  private boolean resourceExists(InputFile file) {
    return file != null;
  }

}
