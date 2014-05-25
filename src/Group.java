package missionanalyzer;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;

import org.apache.commons.io.FileUtils;

import com.google.gson.annotations.Expose;
import com.hp.gagawa.java.elements.A;
import com.hp.gagawa.java.elements.Body;
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
 * Group.java
 * 
 * Représente un des groupes de travail de la mission
 * 
 * @author Bastien Bodart (bastien.bodart@student.uclouvain.be)
 * @version 1.2
 * @date 13 mai 2014
 */
public class Group extends File
{
	/**  */
	private static final long serialVersionUID = -2685997761326944065L;

	/** Mission à laquelle le groupe appartient */
	Mission mission;
	
	/**
	 * Etudiants faisant partie du groupe On utilise une hashtable afin d'éviter
	 * les doublons dus à plusieurs soummissions
	 */
	@Expose Hashtable<String, Student> students = new Hashtable<String, Student>();
	
	/**
	 * Noms des étudiants
	 */
	@Expose ArrayList<String> studentNames = new ArrayList<String>();
	
	/** Répertoire contenant les rapports d'analyse */
	File directory;
	
	/** Rapport d'analyse html*/
	File htmlReport;
	
	/** Rapport d'analyse json*/
	File jsonReport;
	
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
	
	/**
	 * Constructeur
	 * 
	 * @param mission mission du groupe
	 * @param name nom du dossier du groupe
	 */
	public Group(Mission mission, String name)
	{
		super(mission.getAbsolutePath() + "/" + name);
		this.mission = mission;
		this.directory = new File(this.mission.directory.getAbsolutePath() + "/" + this.getName());
		this.directory.mkdir();		
		this.htmlReport = new File(this.directory.getAbsolutePath() + "/" + this.getName() + ".html");
		this.jsonReport = new File(this.directory.getAbsolutePath() + "/" + this.getName() + ".json");
		this.students = this.createStudents();
	}
	
	/**
	 * Crée la hashtable contenant les étudiant en listant les dossiers présents
	 * dans le dossier du groupe
	 * 
	 * @return une hashtable contenant des Student correspondant aux dossiers
	 */
	public Hashtable<String, Student> createStudents()
	{
		Hashtable<String, Student> students = new Hashtable<String, Student>();
		
		String[] files = this.list();
		
		if (files != null && files.length > 0)
		{
			Arrays.sort(files); // Nécessaire pour garder uniquement la dernière
								// soumission
			for (String file : files)
				if(!file.startsWith(".") && file.length() > 11)
				{
					Student student = new Student(this, file);
					if (student.isDirectory())
						students.put(student.studentName, student);
				}
		}
		
		for(Student student : students.values())
		{
			student.directory.mkdir();
			this.studentNames.add(student.getName());
		}
		
		return students;
	}
	
	/**
	 * Récupère les données d'analyse des étudiants et met à jour les données du
	 * groupe
	 */
	public void getData()
	{
		for (Student student : this.students.values())
		{
			if (student.compilationSuccess)
				this.compilations++;
			if (student.testSuccess)
				this.tests++;
			
			this.updateMap(student.compilationErrorsMap, this.compilationsErrorsMap);
			this.updateMap(student.testErrorsMap, this.testErrorsMap);
			
			this.compilationsErrors += student.compilationErrorsMap.size();
			this.testErrors += student.testErrorsMap.size();
		}
		
		this.compilationAverage = ((float) this.compilations / this.students.size()) * 100;
		this.testAverage = ((float) this.tests / this.students.size()) * 100;
	}
	
	/**
	 * Met à jour une map d'erreur du groupe avec les données d'une map d'erreur
	 * d'étudiant
	 * 
	 * @param errors
	 *            map d'erreur d'étudiant
	 * @param map
	 *            map d'erreur du groupe
	 */
	public void updateMap(Hashtable<String, Integer> errors, Hashtable<String, Integer[]> map)
	{
		for (String error : errors.keySet())
			if (map.containsKey(error))
				map.put(error, new Integer[] { map.get(error)[0] + errors.get(error), map.get(error)[1] + 1 });
			else
				map.put(error, new Integer[] { errors.get(error), 1 });
	}
	
	/**
	 * Ecrit le rapport d'analyse du groupe dans un fichier sous format html
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
				.appendChild(new Title().appendChild(new Text(this.getName()))));
		Body body = new Body();
		html.appendChild(body);
		body.appendChild(new H1().setAlign("center").appendChild(new Text(this.getName())));
		body.appendChild(new H3().setAlign("center")
				.appendChild(new A().setHref(this.mission.htmlReport.getAbsolutePath())
						.appendChild(new Text(this.mission.getName()))));
		body.appendChild(new H3().setAlign("center").appendChild(new Text(new Date().toString())));
		body.appendChild(new H3().appendChild(new Text("Students group results")));
		Table table = new Table().setBorder("1").setCellpadding("3");
		body.appendChild(table);
		Thead thead = new Thead().setAlign("center")
				.appendChild(new Tr()
						.appendChild(new Td().appendChild(new Text("Students")))
						.appendChild(new Td().appendChild(new Text("Successful compilation")))
						.appendChild(new Td().appendChild(new Text("Successful test"))));
		Tbody tbody = new Tbody().setAlign("center")
				.appendChild(new Tr()
						.appendChild(new Td().appendChild(new Text(this.students.size() + " (100 %)")))
						.appendChild(new Td().appendChild(new Text(this.compilations + " (" + this.compilationAverage + " %)")))
						.appendChild(new Td().appendChild(new Text(this.tests + " (" + this.testAverage + " %)"))));
		table.appendChild(thead, tbody);
		body.appendChild(new H3().appendChild(new Text("Group errors")));
		if (this.compilationsErrors > 0)
		{
			body.appendChild(this.getErrorsTable(this.compilationsErrorsMap, this.compilationsErrors, this.students.size(), "Compilation error"));
			body.appendChild(new P());
		}
		if (this.testErrors > 0)
		{
			body.appendChild(this.getErrorsTable(this.testErrorsMap, this.testErrors, this.compilations, "Test error"));
			body.appendChild(new P());
		}
		body.appendChild(new H3().appendChild(new Text("Students")));
		body.appendChild(this.getStudentsTable());
		
		if (this.mission.serverMissionPath != null)
			FileUtils.writeStringToFile(report, html.write().replaceAll(this.mission.getAbsolutePath(), this.mission.serverMissionPath));
		else
			FileUtils.writeStringToFile(report, html.write());
		try
		{
			FileUtils.copyFile(new File(new File(this.mission.analysis.jarPath).getParent() + "/sorttable.js"), new File(this.directory.getAbsolutePath() + "/sorttable.js"));
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
		return this.mission.getErrorsTable(errors, relative, proportion, title);
	}
	
	/**
	 * Crée un tableau HTML avec les étudiant du groupe
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
		
		for (Student student : this.students.values())
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
	 * Crée une cellule HTML contenant un lien vers le rapport du groupe
	 * 
	 * @return un objet TD contenant un lien
	 */
	public Td getGroupLink()
	{
		return new Td().appendChild(new A().setHref(this.htmlReport.getAbsolutePath()).appendChild(new Text(this.getName())));
	}
	
	/**
	 * Crée une cellule HTML contenant le nombre d'étudiants du groupe
	 * 
	 * @return un objet TD contenant le nombre
	 */
	public Td getGroupStudents()
	{
		return new Td().appendChild(new Text(this.students.size()));
	}
	
	/**
	 * Crée une cellule HTML contenant le nombre de compilations réussies du
	 * groupe et la proportion totale par rapport au nombre d'étudiants
	 * 
	 * @return un objet TD contenant les nombres
	 */
	public Td getGroupCompilations()
	{
		return new Td().appendChild(new Text(this.compilations + " (" + this.mission.getPercentage(this.compilationAverage) + ")"));
	}
	
	/**
	 * Crée une cellule HTML contenant le nombre de test réussis du groupe et la
	 * proportion totale par rapport au nombre d'étudiants
	 * 
	 * @return un objet TD contenant les nombres
	 */
	public Td getGroupTests()
	{
		return new Td().appendChild(new Text(this.tests + " (" + this.mission.getPercentage(this.testAverage) + ")"));
	}
}