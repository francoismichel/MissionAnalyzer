package missionanalyzer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.hp.gagawa.java.elements.Body;
import com.hp.gagawa.java.elements.H1;
import com.hp.gagawa.java.elements.H3;
import com.hp.gagawa.java.elements.Head;
import com.hp.gagawa.java.elements.Html;
import com.hp.gagawa.java.elements.P;
import com.hp.gagawa.java.elements.Script;
import com.hp.gagawa.java.elements.Table;
import com.hp.gagawa.java.elements.Tbody;
import com.hp.gagawa.java.elements.Td;
import com.hp.gagawa.java.elements.Text;
import com.hp.gagawa.java.elements.Thead;
import com.hp.gagawa.java.elements.Title;
import com.hp.gagawa.java.elements.Tr;

/**
 * Mission.java
 * 
 * Représente une mission
 * 
 * @author Bastien Bodart (bastien.bodart@student.uclouvain.be)
 * @version 1.2
 * @date 13 mai 2014
 */
public class Mission extends File
{
	/**  */
	private static final long serialVersionUID = 3635694588971414809L;

	/** Analyse */
	Analysis analysis;
	
	/** Répertoire contenant les rapports d'analyse */
	File directory;
	
	/** Rapport d'analyse html */
	File htmlReport;
	
	/** Rapport d'analyse json */
	File jsonReport;
	
	/**
	 * Groupes de travail de la mission définis dans le fichier de configuration
	 */
	ArrayList<Group> groups = new ArrayList<Group>();
	
	/**
	 * Noms des groupes
	 */
	@Expose ArrayList<String> groupNames = new ArrayList<String>();
	
	/**
	 * Fichiers à fournir par l'étudiant définis dans le fichier de
	 * configuration
	 */
	@Expose ArrayList<String> requiredFilesNames = new ArrayList<String>();
	
	/** Compilateur */
	JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
	
	/** Nombre de threads lancés */
	int runningThreads = 0;
	
	/** Groupe avec la meilleure moyenne de compilation */
	Group mostCompilations = null;
	
	/** Groupe avec la meilleure moyenne de test */
	Group mostTests = null;
	
	/** Groupe avec la pire moyenne de compilation */
	Group leastCompilations = null;
	
	/** Groupe avec la pire moyenne de test */
	Group leastTests = null;
	
	/** Nombre d'étudiants */
	int students = 0;
	
	/** Compilations réussies */
	int compilations = 0;
	
	/** Tests réussis */
	int tests = 0;
	
	/** Moyenne de compilation */
	float compilationAverage = 0;
	
	/** Moyenne de test */
	float testAverage = 0;
	
	/** Erreurs de compilation {absolu, relatif} */
	Hashtable<String, Integer[]> compilationsErrorsMap = new Hashtable<String, Integer[]>();
	
	/** Erreurs de test {absolu, relatif} */
	Hashtable<String, Integer[]> testErrorsMap = new Hashtable<String, Integer[]>();
	
	/** Erreurs relatives de compilation */
	int compilationsErrors = 0;
	
	/** Erreurs relatives de test */
	int testErrors = 0;

	/** Path du dossier de la mission sur le serveur */
	@Expose String serverMissionPath = null;
	
	/** Path du dossier contenant le fichier de test */
	@Expose String serverTestPath = null;
	
	/**
	 * Constructeur
	 * 
	 * @param analysis
	 *            l'analyse qui a créé la mission
	 * @param path
	 *            le chemin du dossier de la mission
	 * @throws IOException
	 *             si le fichier de configuration ne peut pas être lu
	 */
	public Mission(Analysis analysis, String path) throws IOException
	{
		super(path);
		this.analysis = analysis;
		this.directory = new File(this.getAbsolutePath() + "/analysis");
		this.directory.mkdir();
		this.htmlReport = new File(this.directory.getAbsolutePath() + "/" + this.getName() + ".html");
		this.jsonReport = new File(this.directory.getAbsolutePath() + "/" + this.getName() + ".json");
		this.readMissionFile(analysis.missionFile);
	}
	
	/**
	 * Analyse les soumissions des étudiants
	 * 
	 * @throws InterruptedException
	 *             si le thread principal est interrompu
	 * @throws IOException
	 *             si les rapports ne peuvent être écrit
	 */
	public void analyze() throws InterruptedException, IOException
	{
		for (Group group : this.groups)
		{
			this.groupNames.add(group.getName());
			for (Student student : group.students.values())
				synchronized (this)
				{
					while (this.runningThreads >= this.analysis.maximumThreads)
						this.wait();
					// On démarre un nouveau thread pour chaque étudiant
					this.runningThreads++;
					new Thread(student).start();
					if (this.analysis.verbose)
						System.out.println(student.getName() + " " + group.getName() + " thread started");
				}
		}
		// On attend que tous les threads aient terminé
		synchronized (this)
		{
			while (this.runningThreads > 0)
				this.wait();
		}
		
		this.getData();
		
		this.writeReport(this.htmlReport);
		Mission.writeJson(this, this.jsonReport);
	}
	
	/**
	 * Lit le fichier de configuration de la mission et récupère les données
	 * Crée les groupes renseignés
	 * 
	 * @param missionFile
	 *            Fichier de configuration de la mission renseignant les
	 *            fichiers nécessaires à la mission de l'étudiant et les groupes
	 *            à tester Doit d'abord contenir les noms des fichiers (un par
	 *            ligne) puis une ligne vide puis les noms des groupes à tester
	 *            (un par ligne)
	 * @throws IOException
	 *             si le fichier ne peut pas être lu
	 */
	public void readMissionFile(File missionFile) throws IOException
	{
		BufferedReader reader = new BufferedReader(new FileReader(missionFile));
		String line;
		
		line = reader.readLine();
		if (!line.equals("void"))
			this.serverMissionPath = line.trim();
		line = reader.readLine();
		if (!line.equals("void"))
			this.serverTestPath = line.trim();
		line = reader.readLine();
		while (!(line = reader.readLine()).equals(""))
			this.requiredFilesNames.add(line.trim());
		
		while ((line = reader.readLine()) != null)
		{
			Group group = new Group(this, line.trim());
			if (group.exists() && group.isDirectory() && !group.students.isEmpty())
				this.groups.add(group);
		}
		
		reader.close();
	}
	
	/**
	 * Récupère les données analysées dans les groupes
	 * 
	 * @throws IOException
	 *             si les donnéees ne peuvent pas être récupérées
	 */
	public void getData() throws IOException
	{
		for (Group group : this.groups)
		{
			// On récupère les données et on écrit le rapport de chaque groupe
			group.getData();
			group.writeReport(group.htmlReport);
			Mission.writeJson(group, group.jsonReport);			
			
			this.students += group.students.size();
			this.compilations += group.compilations;
			this.tests += group.tests;
			
			if (this.mostCompilations == null || this.mostCompilations.compilationAverage < group.compilationAverage)
				this.mostCompilations = group;
			if (this.mostTests == null || this.mostTests.testAverage < group.testAverage)
				this.mostTests = group;
			if (this.leastCompilations == null || this.leastCompilations.compilationAverage > group.compilationAverage)
				this.leastCompilations = group;
			if (this.leastTests == null || this.leastTests.testAverage > group.testAverage)
				this.leastTests = group;
			
			this.updateMap(group.compilationsErrorsMap, this.compilationsErrorsMap);
			this.updateMap(group.testErrorsMap, this.testErrorsMap);
			
			this.compilationsErrors += group.compilationsErrors;
			this.testErrors += group.testErrors;
		}
		
		this.compilationAverage = ((float) this.compilations / this.students) * 100;
		this.testAverage = ((float) this.tests / this.students) * 100;
	}
	
	/**
	 * Met à jour une map d'erreur de la mission avec les données d'une map
	 * d'erreur de groupe
	 * 
	 * @param errors
	 *            map d'erreur de groupe
	 * @param map
	 *            map d'erreur de la mission
	 */
	public void updateMap(Hashtable<String, Integer[]> errors, Hashtable<String, Integer[]> map)
	{
		for (String error : errors.keySet())
			if (map.containsKey(error))
				map.put(error, new Integer[] { map.get(error)[0] + errors.get(error)[0], map.get(error)[1] + errors.get(error)[1] });
			else
				map.put(error, new Integer[] { errors.get(error)[0], errors.get(error)[1] });
	}
	
	/**
	 * Ecrit le fichier json de l'objet pour une utilisation avec GUI
	 * 
	 * @param jsonFile
	 * @throws IOException
	 */
	public static void writeJson(Object object, File jsonFile) throws IOException
	{
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		FileUtils.writeStringToFile(jsonFile, gson.toJson(object));
	}
	
	/**
	 * Ecrit le rapport d'analyse de la mission dans un fichier sous format html
	 * 
	 * @param report
	 *            fichier où écrire le rapport
	 * @throws IOException
	 *             si le fichier ne peut pas être écrit
	 */
	public void writeReport(File report) throws IOException
	{
		Html html = new Html();
		html.appendChild(new Head()
							.appendChild(new Script("text/javascript").setSrc("sorttable.js"))
							.appendChild(new Title().appendChild(new Text(this.getName()))));
		
		Body body = new Body();
		html.appendChild(body);
		
		body.appendChild(new H1().setAlign("center").appendChild(new Text(this.getName())));
		body.appendChild(new H3().setAlign("center").appendChild(new Text(new Date().toString())));
		body.appendChild(new H3().setAlign("center").appendChild(new Text("JRE version " + System.getProperties().get("java.version").toString())));
		body.appendChild(new H3().appendChild(new Text("Students mission results")));
		
		Table table = new Table().setBorder("1").setCellpadding("3");
		body.appendChild(table);
		Thead thead = new Thead().setAlign("center")
				.appendChild(new Tr()
						.appendChild(new Td().appendChild(new Text("Students")))
						.appendChild(new Td().appendChild(new Text("Successful compilation (%)")))
						.appendChild(new Td().appendChild(new Text("Successful test (%)"))));
		Tbody tbody = new Tbody().setAlign("center")
				.appendChild(new Tr()
						.appendChild(new Td().appendChild(new Text(this.students)))
						.appendChild(new Td().appendChild(new Text(this.compilations + " (" + this.getPercentage(this.compilationAverage) + ")")))
						.appendChild(new Td().appendChild(new Text(this.tests + " (" + this.getPercentage(this.testAverage) + ")"))));
		table.appendChild(thead, tbody);
		
		body.appendChild(new H3().appendChild(new Text("Mission errors")));
		if (this.compilationsErrors > 0)
		{
			body.appendChild(this.getErrorsTable(this.compilationsErrorsMap, this.compilationsErrors, this.students, "Compilation error"));
			body.appendChild(new P());
		}
		if (this.testErrors > 0)
		{
			body.appendChild(this.getErrorsTable(this.testErrorsMap, this.testErrors, this.compilations, "Test error"));
			body.appendChild(new P());
		}
		
		body.appendChild(new H3().appendChild(new Text("Groups")));
		body.appendChild(this.getGroupsAverages());
		body.appendChild(new P());
		body.appendChild(this.getGroupsTable());
		
		body.appendChild(new H3().appendChild(new Text("Students")));
		body.appendChild(this.getStudentsTable());
		
		if (this.serverMissionPath != null)
			FileUtils.writeStringToFile(report, html.write().replaceAll(this.getAbsolutePath(), this.serverMissionPath));
		else
			FileUtils.writeStringToFile(report, html.write());
		try
		{
			FileUtils.copyFile(new File(new File(this.analysis.jarPath).getParent() + "/sorttable.js"), new File(this.directory.getAbsolutePath() + "/sorttable.js"));
		}
		catch(IOException e)
		{
			System.err.println("Fichier sorttable.js non trouvé dans le répertoire de l'exécutable");
		}
	}
	
	/**
	 * Crée un tableau HTML d'un type d'erreur
	 * 
	 * @param errors
	 *            map des erreurs à traiter
	 * @param relative
	 *            nombre d'erreurs relatives total
	 * @param title
	 *            titre du tableau
	 * @return un objet Table contenant une ligne par erreur, les nombres absolu
	 *         et relatif de cette erreur et sa proportion relative
	 */
	public Table getErrorsTable(Hashtable<String, Integer[]> errors, int relative, int proportion, String title)
	{
		Table table = new Table().setBorder("1").setCellpadding("3").setCSSClass("sortable");
		
		Thead thead = new Thead().setAlign("center")
				.appendChild(new Tr()
						.appendChild(new Td().appendChild(new Text(title)))
						.appendChild(new Td().appendChild(new Text("Absolute")))
						.appendChild(new Td().appendChild(new Text("Relative")))
						.appendChild(new Td().appendChild(new Text("Students proportion (in %)"))));
		
		Tbody tbody = new Tbody();
		
		for (String errorName : errors.keySet())
			tbody.appendChild(new Tr()
					.appendChild(new Td().appendChild(new Text(errorName)))
					.appendChild(new Td().setAlign("center").appendChild(new Text(errors.get(errorName)[0])))
					.appendChild(new Td().setAlign("center").appendChild(new Text(errors.get(errorName)[1]
											+ " (" + this.getPercentage((float) errors.get(errorName)[1] * 100 / relative) + ")")))
					.appendChild(new Td().setAlign("center").appendChild(new Text(this.getPercentage((float) errors.get(errorName)[1] * 100 / proportion)))));
		
		
		return table.appendChild(thead, tbody);
	}
	
	/**
	 * Crée un tableau HTML avec les meilleurs et pire groupes de la mission
	 * 
	 * @return un tableau contenant les noms des groupes ainsi que les
	 *         pourcentages de réussite en compilation et test
	 */
	public Table getGroupsAverages()
	{
		Table table = new Table().setBorder("1").setCellpadding("3");
		
		Thead thead = new Thead().setAlign("center")
				.appendChild(new Tr()
						.appendChild(new Td())
						.appendChild(new Td().appendChild(new Text("Compilation")))
						.appendChild(new Td().appendChild(new Text("Test"))));
		
		Tbody tbody = new Tbody().setAlign("center")
				.appendChild(new Tr()
						.appendChild(new Td().appendChild(new Text("Most successful")))
						.appendChild(new Td().appendChild(new Text(this.mostCompilations.getName() + " (" + this.getPercentage(this.mostCompilations.compilationAverage) + ")")))
						.appendChild(new Td().appendChild(new Text(this.mostTests.getName() + " (" + this.getPercentage(this.mostTests.testAverage) + ")"))))
				.appendChild(new Tr()
						.appendChild(new Td().appendChild(new Text("Least successful")))
						.appendChild(new Td().appendChild(new Text(this.leastCompilations.getName() + " (" + this.getPercentage(this.leastCompilations.compilationAverage) + ")")))
						.appendChild(new Td().appendChild(new Text(this.leastTests.getName() + " (" + this.getPercentage(this.leastTests.testAverage) + ")"))));
		
		return table.appendChild(thead, tbody);
	}
	
	/**
	 * Crée un tableau HTML reprenant les groupes de la mission
	 * 
	 * @return un tableau comprennant les groupes, leur nombre d'étudiants, de
	 *         compilations et tests réussis
	 */
	public Table getGroupsTable()
	{
		Table table = new Table().setBorder("1").setCellpadding("3");
		
		Thead thead = new Thead().setAlign("center")
				.appendChild(new Tr()
						.appendChild(new Td().appendChild(new Text("Group")))
						.appendChild(new Td().appendChild(new Text("Students")))
						.appendChild(new Td().appendChild(new Text("Successful compilation")))
						.appendChild(new Td().appendChild(new Text("Successful test"))));
		
		Tbody tbody = new Tbody();
		
		for (Group group : this.groups)
			tbody.appendChild(new Tr()
					.appendChild(group.getGroupLink())
					.appendChild(group.getGroupStudents().setAlign("center"))
					.appendChild(group.getGroupCompilations().setAlign("center"))
					.appendChild(group.getGroupTests().setAlign("center")));
		
		return table.appendChild(thead, tbody);
	}
	
	/**
	 * Crée un tableau HTML avec les étudiant de la mission
	 * 
	 * @return un objet Table contenant une ligne par étudiant, les éventuels
	 *         fichiers manquants, rapport PMD, fichier out et err, les erreurs
	 *         et le résultat final
	 */
	public Table getStudentsTable()
	{
		Table table = new Table().setBorder("1").setCellpadding("3");
		
		Thead thead = new Thead().setAlign("center")
				.appendChild(new Tr()
						.appendChild(new Td().appendChild(new Text("Student")))
						.appendChild(new Td().appendChild(new Text("Missing")))
						.appendChild(new Td().appendChild(new Text("PMD")))
						.appendChild(new Td().appendChild(new Text("Output")))
						.appendChild(new Td().appendChild(new Text("Error")))
						.appendChild(new Td().appendChild(new Text("Compilation")))
						.appendChild(new Td().appendChild(new Text("Test")))
						.appendChild(new Td().appendChild(new Text("Result"))));
		
		Tbody tbody = new Tbody();
		
		for (Group group : this.groups)
			for (Student student : group.students.values())
				tbody.appendChild(new Tr()
						.appendChild(student.getFileLink(student.htmlReport, student.studentName))
						.appendChild(student.getMissingFiles())
						.appendChild(student.getFileLink(student.htmlPmdReport, "PMD report"))
						.appendChild(student.getFileLink(student.outFile, "Output file"))
						.appendChild(student.getFileLink(student.errFile, "Error file"))
						.appendChild(student.getErrorsText(student.compilationErrorsMap))
						.appendChild(student.getErrorsText(student.testErrorsMap))
						.appendChild(student.getResult()));
		
		return table.appendChild(thead, tbody);
	}
	
	/**
	 * Retourne un float sous forme string à deux décimale
	 * 
	 * @param f
	 *            le float à retourner
	 * @return f avec une décimale et le symbole %
	 */
	public String getPercentage(float f)
	{
		return String.format("%.2f", f);
	}
}