package net.helipilot50.graphql.export;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;


@SuppressWarnings("deprecation")
public class Export {
	private static Logger log = Logger.getLogger(Export.class);
	public static void main(String[] args) {
		try {
			Options options = new Options();
			options.addOption("i", "input", true, "input file name ");
			options.addOption("l", "language", true, "Target language, supported languages: PLANTUML, TEXTUML (future: XMI)");
			options.addOption("p", "package", true, "UML package name");
			options.addOption("o", "output", true, "Output directory name. ");
			options.addOption("h", "help", false, "Print usage.");

			CommandLineParser parser = new PosixParser();
			CommandLine cl = parser.parse(options, args, false);

			if (cl.hasOption("h")) {
				logUsage(options);
				return;
			}


			String inputFileName = cl.getOptionValue("i", "default.gql");
			log.info("Exporting: " + inputFileName);
			File inputFile = new File(inputFileName);
			if (!inputFile.exists()){
				log.error("Input file does not exist: " + inputFileName);
				System.exit(-1);
			}
			if (!(inputFileName.toLowerCase().endsWith(".gql") )){
				log.error("Input file is not .gql: " + inputFileName);
				System.exit(-1);
			}

			String packageString = cl.getOptionValue("p", "root");

			String languageString = cl.getOptionValue("l", "PLANTUML");
			Language language = Language.valueOf(languageString);


			String outputFileName = inputFileName.substring(0, inputFileName.lastIndexOf("."));

			if (cl.hasOption("o")){
				outputFileName = cl.getOptionValue("o");
			}

			log.debug("Output directory: " + outputFileName);
			log.debug("Language: " + languageString);
			IDLExport exporter = new IDLExport();
			exporter.generate(inputFileName, outputFileName, language, packageString);
			log.info("Completed export of " + inputFileName);

		} catch (Exception e) {
			log.error("Critical error", e);
		}
	}
	/**
	 * Write usage to console.
	 * @throws IOException 
	 */
	public static void logUsage(Options options)  {
		HelpFormatter formatter = new HelpFormatter();
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		String syntax = Export.class.getName() + " [<options>]";
		formatter.printHelp(pw, 100, syntax, "options:", options, 0, 2, null);
		log.info(sw.toString());
		printHelp();
	}

	public static void printHelp(){
		URL helpUrl = Export.class.getResource("commands.txt");
		BufferedReader br;
		try {
			br = new BufferedReader(new InputStreamReader(helpUrl.openStream()));
			try {
				String line = br.readLine();

				while (line != null) {
					log.info(line);
					line = br.readLine();
				}

			} finally {
				br.close();
			}
		} catch (FileNotFoundException e) {
			log.error(e.getMessage());
			log.debug("Detailed error:", e);
		} catch (IOException e){
			log.error(e.getMessage());
			log.debug("Detailed error:", e);
		}
	}

}
