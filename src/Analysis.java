package missionanalyzer;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/**
 * Analysis.java
 * 
 * Classe principale. Gère les arguments et lance l'analyse de la mission.
 * 
 * @author Bastien Bodart (bastien.bodart@student.uclouvain.be)
 * @version 1.2
 * @date 13 mai 2014
 */
public class Analysis
{
	/**
	 * Path du fichier jar
	 */
	String jarPath;
	
	/** Affiche des informations lors de l'exécution */
	boolean verbose;
	
	/** Fichier de configuration de la mission */
	File missionFile;
	
	/** Mission à analyser */
	Mission mission;
	
	/** Utilise PMD pour faire un analyse du code source */
	boolean pmd;
	
	/** Fichier .java définissant la classe de test JUnit à utiliser */
	File testFile = null;
	
	/**
	 * Temps (ms) au-delà duquel le process de test est détruit. Permet de tuer
	 * les tests en boucle infinie. Ne doit pas être trop court (<1000) ou des
	 * processus pourraient ne pas être détruits correctement (bug java 9005842)
	 */
	long watchdogTimer = 60000;
	
	/**
	 * Charset des fichiers Java à compiler
	 */
	Charset charset = Charset.forName("ISO-8859-1");
	
	/** Nombre de threads principaux à utiliser */
	int maximumThreads = 4;
	
	/** Règles pour PMD */
	String pmdRules = "java-basic";
	
	/**
	 * Méthode principale
	 * 
	 * @param args
	 *            voir constructeur
	 */
	public static void main(String[] args)
	{
		try
		{
			Analysis analysis = new Analysis(args);
			analysis.mission.analyze();
		}
		catch (InterruptedException | IOException | ParseException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Constructeur
	 * 
	 * @param args
	 *            les différents options définissant cette analyse
	 * @throws ParseException
	 * @throws IOException
	 */
	public Analysis(String[] args) throws ParseException, IOException
	{
		this.jarPath = Analysis.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		
		Option vOption = new Option("v", "verbose", false, "Display informations in the console");
		Option mOption = new Option("m", "mission", true, "Mission configuration file location");
		mOption.setRequired(true);
		Option dOption = new Option("d", "directory", true, "Directory of the mission");
		dOption.setRequired(true);
		Option pOption = new Option("p", "pmd", true, "Execute PMD?");
		pOption.setOptionalArg(true);
		Option tOption = new Option("t", "testfile", true, "Test file");
		Option wOption = new Option("w", "watchdog", true, "Watchdog timer");
		Option eOption = new Option("e", "encoding", true, "File encoding");
		Option threadsOption = new Option("threads", true, "Number of threads");
		
		Options options = new Options()
				.addOption(vOption)
				.addOption(mOption)
				.addOption(dOption)
				.addOption(pOption)
				.addOption(tOption)
				.addOption(wOption)
				.addOption(eOption)
				.addOption(threadsOption);
		
		CommandLine line = new PosixParser().parse(options, args);
		
		this.verbose = line.hasOption("v");
		this.missionFile = new File(line.getOptionValue("m"));
		this.mission = new Mission(this, line.getOptionValue("d"));
		if (line.hasOption("t"))
			this.testFile = new File(line.getOptionValue("t"));
		this.pmd = line.hasOption("p");
		if (line.getOptionValue("p") != null)
			this.pmdRules = line.getOptionValue("p");
		if (line.hasOption("w"))
			this.watchdogTimer = Long.parseLong(line.getOptionValue("w"));
		if (line.hasOption("e"))
			this.charset = Charset.forName(line.getOptionValue("e"));
		if (line.hasOption("threads"))
			this.maximumThreads = Integer.parseInt(line.getOptionValue("threads"));
	}
}