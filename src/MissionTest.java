import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

/**
 * MissionTest.java
 * 
 * Prévue pour être lancée dans un processus dédié, cette classe lance les tests
 * d'une classe de test JUnit passée en arguments à la méthode principale.
 * 
 * @author Bastien Bodart (bastien.bodart@uclouvain.be)
 * @version 1.0
 * @date 13 août 2013
 */
public class MissionTest
{
	/** Le nom de la classe de test à exécuter */
	String className;
	
	/** Le class loader qui sera utilisé pour charger la classe de test */
	URLClassLoader urlClassLoader;
	
	/**
	 * Méthode principale
	 * 
	 * @param args
	 *            args[0] doit être l'URI du dossier où se trouve le fichier de
	 *            la classe de test args[1] doit être le nom de la classe de
	 *            test
	 */
	public static void main(String[] args)
	{
		try
		{
			MissionTest mt = new MissionTest(args[0], args[1]);
			mt.test();
			mt.urlClassLoader.close();
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Constructeur
	 * 
	 * @param uri
	 *            un string représentant l'URI du dossier contenant le fichier
	 *            de la classe de test
	 * @param className
	 *            le nom de la classe de test
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 */
	public MissionTest(String uri, String className) throws MalformedURLException, URISyntaxException
	{
		this.className = className;
		this.urlClassLoader = new URLClassLoader(new URL[] { new URI(uri).toURL() });
	}
	
	/**
	 * Lance les tests contenus dans la classe de test et imprime les résultats
	 * sous un format défini sur la sortie d'erreur. € est utilisé comme
	 * caractère de reconnaissance pour les erreurs de test. Imprime aussi le
	 * temps d'exécution (ns) sur la sortie standard.
	 * 
	 * Format d'impression d'erreur : €nomDeLErreur message ligne methode
	 * 
	 * @throws Throwable
	 */
	public void test() throws Throwable
	{
		Result result = JUnitCore.runClasses(this.urlClassLoader.loadClass(this.className));
		
		for (Failure failure : result.getFailures())
		{
			String message;
			
			if(failure.getException().getClass().equals(InvocationTargetException.class))
				if(failure.getException().getCause() != null)
				{
					System.err.println("€" + failure.getException().getCause().getClass().getSimpleName());
					message = failure.getException().getCause().getMessage();
				}
				else
				{
					System.err.println("€InvocationTargetException");
					message = failure.getException().getMessage();
				}
			else
			{
				System.err.println("€" + failure.getException().getClass().getSimpleName());
				message = failure.getMessage();
			}
			
			if(message != null)
				System.err.println(message.replace("\n", "").replace("<", "").replace(">", ""));
			else				
				System.err.println("No message");
			
			System.err.println(((failure.getTrace().split(this.className + ".java:")[1]).split("\\)", 2)[0]).trim());
			System.err.println(failure.getTestHeader().replace(this.className, "").trim());
		}
	}
}