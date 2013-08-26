package missionanalyzer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;

import net.sourceforge.pmd.PMD;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;

import com.hp.gagawa.java.Node;
import com.hp.gagawa.java.elements.A;
import com.hp.gagawa.java.elements.Body;
import com.hp.gagawa.java.elements.Br;
import com.hp.gagawa.java.elements.H1;
import com.hp.gagawa.java.elements.H3;
import com.hp.gagawa.java.elements.Head;
import com.hp.gagawa.java.elements.Html;
import com.hp.gagawa.java.elements.P;
import com.hp.gagawa.java.elements.Table;
import com.hp.gagawa.java.elements.Tbody;
import com.hp.gagawa.java.elements.Td;
import com.hp.gagawa.java.elements.Text;
import com.hp.gagawa.java.elements.Thead;
import com.hp.gagawa.java.elements.Title;
import com.hp.gagawa.java.elements.Tr;

/**
 * Student.java
 * 
 * Représente un étudiant et son dossier de soumission. Le nom de dossier doit
 * avoir la forme nom-YYYY-MM-DD
 * 
 * @author Bastien Bodart (bastien.bodart@uclouvain.be)
 * @version 1.0
 * @date 14 août 2013
 */
public class Student extends File implements Runnable
{
	/**  */
	private static final long serialVersionUID = 8056215688190036928L;

	/** Groupe dont fait partie l'étudiant */
	Group group;
	
	/** Nom de l'étudiant récupéré dans le nom de dossier */
	String studentName;
	
	/** Répertoire contenant les rapports d'analyse */
	File directory;
	
	/** Rapport d'analyse */
	File report;
	
	/** Rapport PMD éventuel */
	File pmdReport;
	
	/** Fichier de sortie éventuel */
	File outFile;
	
	/** Fichier d'erreur éventuel */
	File errFile;
	
	/** Fichiers manquants */
	ArrayList<String> missingFiles = new ArrayList<String>();
	
	/** Erreurs de compilation */
	ArrayList<StudentError> compilationErrors = new ArrayList<StudentError>();
	Hashtable<String, Integer> compilationErrorsMap = new Hashtable<String, Integer>();
	
	/** Erreurs de test */
	ArrayList<StudentError> testErrors = new ArrayList<StudentError>();
	Hashtable<String, Integer> testErrorsMap = new Hashtable<String, Integer>();
	
	/** Compilation réussie */
	boolean compilationSuccess = false;
	
	/** Test réussi */
	boolean testSuccess = false;
	
	/** Processus de test détruit */
	boolean testDestroyed = false;
	
	/** Temps d'exécution des tests */
	Hashtable<String, String> executionTime = new Hashtable<String, String>();
	
	/**
	 * Constructeur
	 * 
	 * @param group
	 *            groupe de l'étudiant
	 * @param name
	 *            nom du dossier de l'étudiant
	 */
	public Student(Group group, String name)
	{
		super(group.getAbsolutePath() + "/" + name);
		this.group = group;
		this.setStudentName(name);
		this.directory = new File(this.group.directory.getAbsolutePath() + "/" + this.getName());
		this.directory.mkdir();
		this.report = new File(this.directory.getAbsolutePath() + "/" + this.getName() + ".html");
		this.pmdReport = new File(this.directory.getAbsolutePath() + "/PMD-" + this.getName() + ".html");
		this.outFile = new File(this.directory.getAbsolutePath() + "/TestOutput-" + this.getName() + ".txt");
		this.errFile = new File(this.directory.getAbsolutePath() + "/TestError-" + this.getName() + ".txt");
	}
	
	/**
	 * Lance les différentes analyses et imprime le rapport
	 */
	@Override
	public void run()
	{
		if (this.requiredFilesExist())
		{
			if (this.group.mission.analysis.pmd)
				synchronized (this.group.mission)
				{
					// Ne devrait pas être synchrone mais PMD bug sans cela
					this.codeAnalysis(this.group.mission.analysis.pmdRules);
					this.group.mission.notifyAll();
				}
			
			this.compilationAnalysis();
			
			if (this.compilationSuccess)
				try
				{
					this.testAnalysis();
				}
				catch (IOException | InterruptedException e)
				{
					e.printStackTrace();
				}
		}
		
		if (this.group.mission.analysis.verbose)
			System.out.println(this.getName() + " " + this.group.getName() + " compilation success " + this.compilationSuccess + " test succes " + this.testSuccess);
		
		try
		{
			this.writeReport(this.report);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		synchronized (this.group.mission)
		{
			this.group.mission.runningThreads--;
			this.group.mission.notifyAll();
		}
	}
	
	/**
	 * Lance l'analyse de code avec PMD selon les règles définies La méthode
	 * statique run() ne fonctionne pas en multithreading, ce qui laisse penser
	 * qu'elle n'est pas vraiment statique Un bug a été soumis à l'équipe PMD à
	 * http://sourceforge.net/projects/pmd/
	 * 
	 * @param rules
	 */
	public void codeAnalysis(String rules)
	{
		String[] param = new String[] { "-dir", this.getAbsolutePath(), "-format", "summaryhtml", "-reportfile", this.pmdReport.getAbsolutePath(), "-rulesets", rules };
		PMD.run(param);
	}
	
	/**
	 * Lance l'analyse de compilation des fichiers de l'étudiant et du fichier
	 * de test
	 */
	public void compilationAnalysis()
	{
		ArrayList<String> sourceFilesPaths = new ArrayList<String>();
		ArrayList<String> arguments = new ArrayList<String>();
		
		for (String name : this.group.mission.requiredFilesNames)
			sourceFilesPaths.add(this.getAbsolutePath() + "/" + name);
		
		arguments.add("-Xlint:all");
		arguments.add("-sourcepath");
		arguments.add(this.getAbsolutePath());
		
		if (this.compileTask(sourceFilesPaths, arguments, this.compilationErrors, this.compilationErrorsMap))
		{
			sourceFilesPaths = new ArrayList<String>();
			arguments = new ArrayList<String>();
			
			sourceFilesPaths.add(this.group.mission.analysis.testFile.getAbsolutePath());
			
			arguments.add("-d");
			arguments.add(this.getAbsolutePath());
			arguments.add("-classpath");
			arguments.add(this.getAbsolutePath() + ":MissionAnalyser.jar");
			this.compilationSuccess = this.compileTask(sourceFilesPaths, arguments, this.compilationErrors, this.compilationErrorsMap);
		}
	}
	
	/**
	 * Compile un tâche et crée et enregistre les erreurs rapportées
	 * 
	 * @param sourceFilesPaths
	 *            le chemin des sources à compiler
	 * @param arguments
	 *            les arguments à passer au compilateur
	 * @param compilationErrors
	 *            liste d'erreur à remplir
	 * @param map
	 *            map d'erreur à remplir
	 * @return true si la compilation est réussie, false sinon
	 */
	public boolean compileTask(ArrayList<String> sourceFilesPaths, ArrayList<String> arguments, ArrayList<StudentError> compilationErrors, Hashtable<String, Integer> map)
	{
		DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<JavaFileObject>();
		
		boolean result = this.group.mission.javac.getTask(null,
				null,
				diagnosticCollector,
				arguments,
				null,
				this.group.mission.javac.getStandardFileManager(null, null, null).getJavaFileObjectsFromStrings(sourceFilesPaths)).call();
		
		for (Diagnostic<?> diagnostic : diagnosticCollector.getDiagnostics())
		{
			StudentError error = new StudentError(diagnostic.getMessage(null).split("\n")[0],
					diagnostic.getMessage(null), (int) diagnostic.getLineNumber(),
					null,
					new File(((JavaFileObject) diagnostic.getSource()).getName()));
			if (diagnostic.getKind().equals(Diagnostic.Kind.WARNING))
				error.name = "(Warning) " + error.name;
			compilationErrors.add(error);
			if (map.containsKey(error.name))
				map.put(error.name, map.get(error.name) + 1);
			else
				map.put(error.name, 1);
		}
		
		return result;
	}
	
	/**
	 * Imprime dans un fichier le flux de la sortie standard. Récupère les
	 * éventuels temps d'exécution écrits depuis le test.
	 * 
	 * @param outStream
	 *            le flux à imprimer
	 * @param file
	 *            le fichier où imprimer
	 * @throws IOException
	 */
	public void printTestOutput(ByteArrayOutputStream outStream, File file) throws IOException
	{
		if (outStream.size() > 0)
		{
			String s;
			BufferedReader outReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(outStream.toByteArray())));
			BufferedWriter outWriter = null;
			while ((s = outReader.readLine()) != null)
				if (s.startsWith("€"))
					this.executionTime.put(s.substring(1), outReader.readLine());
				else
				{
					if (outWriter == null)
						outWriter = new BufferedWriter(new FileWriter(file));
					outWriter.write(s + "\n");
				}
			
			outReader.close();
			if (outWriter != null)
				outWriter.close();
		}
	}
	
	/**
	 * Récupère le flux d'erreur de l'analyse de test et crée et enregistre les
	 * erreurs rapportées Imprime aussi ce qui dans le flux d'erreur ne serait
	 * pas une erreur de test Les erreurs sont écrite depuis la classe
	 * MissionTest
	 * 
	 * @param errStream
	 *            le flux d'erreur
	 * @param file
	 *            le fichier où imprimer
	 * @throws NumberFormatException
	 *             si le numéro de ligne est erroné
	 * @throws IOException
	 *             si le flux ne peut pas être lu
	 */
	public void printTestError(ByteArrayOutputStream errStream, File file) throws NumberFormatException, IOException
	{
		if (errStream.size() > 0)
		{
			String s;
			BufferedReader errReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(errStream.toByteArray())));
			BufferedWriter errWriter = null;
			
			while ((s = errReader.readLine()) != null)
				if (s.startsWith("€"))
				{
					StudentError error = new StudentError(s.substring(1), errReader.readLine(),
							Integer.parseInt(errReader.readLine()),
							errReader.readLine(),
							this.group.mission.analysis.testFile);
					if (error.name.equals("AssertionError"))
						error.name = error.name + " : " + error.method;
					
					this.testErrors.add(error);
					if (this.testErrorsMap.containsKey(error.name))
						this.testErrorsMap.put(error.name, this.testErrorsMap.get(error.name) + 1);
					else
						this.testErrorsMap.put(error.name, 1);
				}
				else
				{
					// Evite de créer un fichier vide
					if (errWriter == null)
						errWriter = new BufferedWriter(new FileWriter(file));
					errWriter.write(s + "\n");
				}
			
			errReader.close();
			if (errWriter != null)
				errWriter.close();
		}
	}
	
	/**
	 * Vérifie que les fichiers requis pour la mission existent Rempli aussi la
	 * liste des fichiers manquants
	 * 
	 * @return true si les fichiers existent, false sinon
	 */
	public boolean requiredFilesExist()
	{
		for (String file : this.group.mission.requiredFilesNames)
			if (!new File(this.getAbsolutePath() + "/" + file).exists())
				this.missingFiles.add(file);
		
		return this.missingFiles.isEmpty();
	}
	
	/**
	 * Défini le nom de l'étudiant en fonction du nom du dossier Le nom de
	 * dossier doit avoir la forme nom-YYYY-MM-DD
	 * 
	 * @param s
	 *            le nom du dossier
	 */
	public void setStudentName(String s)
	{
		this.studentName = s.substring(0, s.length() - 11);
	}
	
	/**
	 * Lance l'analyse de test dans un nouveau processus avec un timer qui
	 * détruira celui-ci en cas de temps trop important. Ceci afin de pouvoir
	 * sortir d'une boucle infinie
	 * 
	 * @throws ExecuteException
	 *             si l'exécution du processus échoue
	 * @throws IOException
	 *             si les flux ne peuvent être lus
	 * @throws InterruptedException
	 *             si le thread courant est interrompu
	 */
	public void testAnalysis() throws ExecuteException, IOException, InterruptedException
	{
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		ByteArrayOutputStream errStream = new ByteArrayOutputStream();
		
		DefaultExecutor exec = new DefaultExecutor();
		DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler();
		exec.setStreamHandler(new PumpStreamHandler(outStream, errStream));
		exec.setWatchdog(new ExecuteWatchdog(this.group.mission.analysis.watchdogTimer));
		
		CommandLine commandLine = new CommandLine("java");
		commandLine.addArguments(new String[] {
				"-classpath",
				"MissionAnalyser.jar",
				"missionanalyzer/MissionTest",
				this.toURI().toString(),
				this.group.mission.analysis.testFile.getName().replace(".java", "")
		});
		
		exec.execute(commandLine, handler);
		handler.waitFor();
		
		// Attention, valeur linux pour SIGTERM, peut ne pas fonctionner sur
		// d'autres systèmes
		this.testDestroyed = (handler.getExitValue() == 143);
		
		this.printTestOutput(outStream, this.outFile);
		this.printTestError(errStream, this.errFile);
		
		this.testSuccess = (handler.getExitValue() == 0 && this.testErrors.isEmpty());
	}
	
	/**
	 * Ecrit le rapport d'analyse de l'étudiant dans un fichier HTML
	 * 
	 * @param report
	 *            le fichier où écrire
	 * @throws IOException
	 *             si le fichier ne peut pas être écrit
	 */
	public void writeReport(File report) throws IOException
	{
		Html html = new Html();
		html.appendChild(new Head().appendChild(new Title().appendChild(new Text(this.getName()))));
		
		Body body = new Body();
		html.appendChild(body);
		body.appendChild(new H1().setAlign("center").appendChild(new A().setHref(this.getAbsolutePath()).appendChild(new Text(this.getName()))));
		body.appendChild(new H3().setAlign("center")
				.appendChild(new A().setHref(this.group.report.getAbsolutePath()).appendChild(new Text(this.group.getName())))
				.appendChild(new Text(" "))
				.appendChild(new A().setHref(this.group.mission.report.getAbsolutePath()).appendChild(new Text(this.group.mission.getName()))));
		body.appendChild(this.getBooleanTable());
		if (!this.missingFiles.isEmpty())
		{
			body.appendChild(new H3().appendChild(new Text("Missing files")));
			P p = new P();
			body.appendChild(p);
			for (String s : this.missingFiles)
				p.appendChild(new Text(s + ".java")).appendChild(new Br());
		}
		if (this.pmdReport.exists())
			body.appendChild(new P().appendChild(this.getFileLink(this.pmdReport, "PMD report")));
		if (this.outFile.exists())
			body.appendChild(new P().appendChild(this.getFileLink(this.outFile, "Output file")));
		if (this.errFile.exists())
			body.appendChild(new P().appendChild(this.getFileLink(this.errFile, "Error file")));
		
		body.appendChild(new H3().appendChild(new Text("Compilation")));
		body.appendChild(this.getErrorsTable(this.compilationErrors));
		body.appendChild(new H3().appendChild(new Text("Test")));
		body.appendChild(this.getErrorsTable(this.testErrors));
		
		FileUtils.writeStringToFile(report, html.write());
	}
	
	/**
	 * Crée une table qui résume les résultats de l'analyse
	 * 
	 * @return une table HTML précisant les résultats de la compilation et du
	 *         test
	 */
	public Table getBooleanTable()
	{
		Table table = new Table().setAlign("center").setBorder("1").setCellpadding("3");
		Tr tr = new Tr().setAlign("center");
		Td td1 = new Td().appendChild(new Text("Compilation"));
		Td td2 = new Td().appendChild(new Text("Test"));
		Td td3 = new Td().setStyle("color:red").appendChild(new Text("Test process destroyed!"));
		Td td4 = new Td().setStyle("color:red").appendChild(new Text("Missing files!"));
		
		if (!this.missingFiles.isEmpty())
			tr.appendChild(td4);
		
		if (this.compilationSuccess)
			td1.setBgcolor("green");
		else
			td1.setBgcolor("red");
		
		if (this.testSuccess)
			td2.setBgcolor("green");
		else
			td2.setBgcolor("red");
		
		tr.appendChild(td1, td2);
		
		if (this.testDestroyed)
			tr.appendChild(td3);
		
		return table.appendChild(tr);
	}
	
	/**
	 * Crée un tableau contenant un type d'erreur
	 * 
	 * @param errors
	 *            une liste des erreurs à rapporter
	 * @return une table HTML contenant le fichier, la ligne, le nom et le
	 *         message de chaque erreur. Eventuellement la méthode.
	 */
	public Node getErrorsTable(ArrayList<StudentError> errors)
	{
		if (errors.isEmpty())
			if (errors == this.testErrors)
				return this.getExecutionTimeTable();
			else
				return new P();
		
		Table table = new Table().setBorder("1").setCellpadding("3");
		Thead thead = new Thead().setAlign("center");
		Tr tr = new Tr().appendChild(new Td().appendChild(new Text("File")))
				.appendChild(new Td().appendChild(new Text("Line")))
				.appendChild(new Td().appendChild(new Text("Name")));
		if (errors == this.testErrors)
			tr.appendChild(new Td().appendChild(new Text("Method")));
		thead.appendChild(tr.appendChild(new Td().appendChild(new Text("Message"))));
		Tbody tbody = new Tbody();
		
		for (StudentError error : errors)
			tbody.appendChild(this.getErrorRow(error));
		
		return table.appendChild(thead, tbody);
	}
	
	/**
	 * Crée un tableau contenant les temps d'exécution
	 * 
	 * @return une table HTML contenant les méthodes testées et leurs temps
	 *         d'exécution
	 */
	public Table getExecutionTimeTable()
	{
		Table table = new Table().setBorder("1").setCellpadding("3");
		Thead thead = new Thead().appendChild(new Tr()
				.appendChild(new Td().appendChild(new Text("Method")))
				.appendChild(new Td().appendChild(new Text("Execution time (ns)")))
				);
		Tbody tbody = new Tbody();
		for (String s : this.executionTime.keySet())
			tbody.appendChild(new Tr()
					.appendChild(new Td().appendChild(new Text(s)))
					.appendChild(new Td().appendChild(new Text(this.executionTime.get(s))).setAlign("center"))
					);
		return table.appendChild(thead, tbody);
	}
	
	/**
	 * Crée une ligne de tableau HTML contenant les informations d'une erreur
	 * 
	 * @param error
	 *            l'erreur à afficher
	 * @return un objet Tr contenant les informations
	 */
	public Tr getErrorRow(StudentError error)
	{
		Tr tr = new Tr();
		tr.appendChild(this.getFileLink(error.file, error.file.getName()))
				.appendChild(new Td().appendChild(new Text(error.line)))
				.appendChild(new Td().appendChild(new Text(error.name)));
		if (error.method != null)
			tr.appendChild(new Td().appendChild(new Text(error.method)));
		tr.appendChild(new Td().appendChild(new Text(error.message)));
		
		return tr;
	}
	
	/**
	 * Crée une cellule contenant un lien vers un fichier
	 * 
	 * @param file
	 *            le fichier
	 * @param text
	 *            le texte du lien
	 * @return une cellule HTML contenant le lien
	 */
	public Td getFileLink(File file, String text)
	{
		if (file.exists())
			return new Td().appendChild(new A().setHref(file.getAbsolutePath()).appendChild(new Text(text)));
		else
			return new Td().appendChild(new Text("-"));
	}
	
	/**
	 * Crée une cellule contenant un résumé d'un ensemble d'erreurs
	 * 
	 * @param errors
	 *            les erreurs à rapporter
	 * @return un cellule HTML contenant un liste du nom des erreurs et leur
	 *         nombre
	 */
	public Td getErrorsText(Hashtable<String, Integer> errors)
	{
		if (this.testDestroyed && errors == this.testErrorsMap)
			return new Td().setStyle("color:red").appendChild(new Text("Process destroyed!"));
		
		if (errors.isEmpty())
			if (errors == this.testErrorsMap && this.testSuccess)
			{
				Td td = new Td();
				if (this.executionTime.isEmpty())
					return td.appendChild(new Text("-"));
				for (String s : this.executionTime.keySet())
					td.appendChild(new Text(s + " : " + this.executionTime.get(s) + "<br>"));
				return td;
			}
			else
				return new Td().appendChild(new Text("-"));
		
		String s = "";
		
		for (String error : errors.keySet())
			s += error + " : " + errors.get(error) + "<br>";
		
		return new Td().appendChild(new Text(s));
	}
	
	/**
	 * Crée une cellule contenant une liste des fichiers manquants
	 * 
	 * @return une cellule HTML contenant une liste des fichiers
	 */
	public Td getMissingFiles()
	{
		if (this.missingFiles.isEmpty())
			return new Td().appendChild(new Text("-"));
		
		String s = "";
		
		for (String string : this.missingFiles)
			s += string + "<br>";
		
		return new Td().appendChild(new Text(s));
	}
	
	/**
	 * Crée une cellule contenant le résultat final des analyses
	 * 
	 * @return une cellule HTML contenant Passed ou Failed
	 */
	public Td getResult()
	{
		if (this.testSuccess)
			return new Td().setBgcolor("green").appendChild(new Text("Passed"));
		else
			return new Td().setBgcolor("red").appendChild(new Text("Failed"));
	}
	
	/**
	 * Classe privée permettant de recueillir les infos d'une erreur (de
	 * compilation ou de test)
	 */
	private class StudentError
	{
		String name;
		String message;
		int line;
		String method;
		File file;
		
		public StudentError(String name, String message, int line, String method, File file)
		{
			this.name = name;
			this.message = message;
			this.line = line;
			this.method = method;
			this.file = file;
		}
	}
}