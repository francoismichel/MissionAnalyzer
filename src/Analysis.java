import java.io.File;
import java.io.IOException;

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
 * @author Bastien Bodart (bastien.bodart@uclouvain.be)
 * @version 1.0
 * @date 14 août 2013
 */
public class Analysis
{
	/** Affiche des informations lors de l'exécution */
	boolean verbose;
	
	/** Fichier de configuration de la mission */
	File missionFile;
	
	/** Mission à analyser */
	Mission mission;
	
	/** Utilise PMD pour faire un analyse du code source */
	boolean pmd;
	
	/** Fichier .java définissant la classe de test JUnit à utiliser */
	File testFile;
	
	/**
	 * Temps (ms) au-delà duquel le process de test est détruit. Permet de tuer
	 * les tests en boucle infinie. Ne doit pas être trop court (<1000) ou des
	 * processus pourraient ne pas être détruits correctement (bug java 9005842)
	 */
	long watchdogTimer = 5000;
	
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
		Option vOption = new Option("v", "verbose", false, "Display informations in the console");
		Option mOption = new Option("m", "mission", true, "Mission configuration file location");
		mOption.setRequired(true);
		Option dOption = new Option("d", "directory", true, "Directory of the mission");
		dOption.setRequired(true);
		Option pOption = new Option("p", "pmd", false, "Execute PMD?");
		pOption.setOptionalArg(true);
		Option tOption = new Option("t", "testfile", true, "Test file");
		tOption.setRequired(true);
		Option wOption = new Option("w", "watchdog", true, "Watchdog timer");
		Option threadsOption = new Option("threads", true, "Number of threads");
		
		Options options = new Options()
				.addOption(vOption)
				.addOption(mOption)
				.addOption(dOption)
				.addOption(pOption)
				.addOption(tOption)
				.addOption(wOption)
				.addOption(threadsOption);
		
		CommandLine line = new PosixParser().parse(options, args);
		
		this.verbose = line.hasOption("v");
		this.missionFile = new File(line.getOptionValue("m"));
		this.mission = new Mission(this, line.getOptionValue("d"));
		this.testFile = new File(line.getOptionValue("t"));
		this.pmd = line.hasOption("p");
		if (line.getOptionValue("p") != null)
			this.pmdRules = line.getOptionValue("p");
		if (line.hasOption("w"))
			this.watchdogTimer = Long.parseLong(line.getOptionValue("w"));
		if (line.hasOption("threads"))
			this.maximumThreads = Integer.parseInt(line.getOptionValue("threads"));
	}
}