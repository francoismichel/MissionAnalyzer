package missionanalyzer;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

/**
 * MissionTest.java
 * 
 * Prévue pour être lancée dans un processus dédié, cette classe lance les tests
 * d'une classe de test JUnit passée en argument à la méthode principale.
 * 
 * @author Bastien Bodart (bastien.bodart@uclouvain.be)
 * @version 1.0
 * @date 13 août 2013
 */
public class MissionTest
{
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
			URLClassLoader urlClassLoader = new URLClassLoader(new URL[] { new URI(args[0]).toURL() });
			
			test(urlClassLoader, args[1]);
			
			urlClassLoader.close();
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Lance les tests contenus dans la classe de test et imprime les résultats
	 * sous un format défini sur la sortie d'erreur. € est utilisé comme
	 * caractère de reconnaissance pour les erreurs de test. Imprime aussi le
	 * temps d'exécution (ns) sur la sortie standard.
	 * 
	 * Format d'impression d'erreur : €nomDeLErreur message ligne methode
	 * 
	 * @param urlClassLoader class loader utilisé pour charger la classe de test
	 * @param className nom de la classe de test
	 * @throws Throwable
	 */
	public static void test(URLClassLoader urlClassLoader, String className) throws Throwable
	{
		Result result = JUnitCore.runClasses(urlClassLoader.loadClass(className));
		HashSet<String> set = new HashSet<String>();
		
		for (Failure failure : result.getFailures())
		{
			set.add(failure.getTestHeader().split("\\(")[0]);
			// Teste si l'erreur provient d'un sous-test et si le test général a
			// échoué. Auquel cas, l'erreur n'est pas prise en compte.
			if (!failure.getTestHeader().contains("_")
					|| (!set.contains(failure.getTestHeader().substring(0, failure.getTestHeader().lastIndexOf("_"))) &&
							!set.contains(failure.getTestHeader().substring(0, failure.getTestHeader().indexOf("_")))))
			{
				String message;
				
				if (failure.getException().getClass().equals(InvocationTargetException.class))
					if (failure.getException().getCause() != null)
					{
						System.err.println("€" + failure.getException().getCause().getClass().getSimpleName());
						message = failure.getException().getCause().getMessage();
					}
					else
					{
						System.err.println("€InvocationTargetException");
						message = failure.getException().getMessage();
					}
				else if(failure.getException().getClass().equals(NoSuchMethodException.class))
				{
					System.err.println("€" + failure.getException().getClass().getSimpleName() + " : " + failure.getTestHeader().split("\\(")[0]);
					message = failure.getMessage();
				}
				else
				{
					System.err.println("€" + failure.getException().getClass().getSimpleName());
					message = failure.getMessage();
				}
				
				if (message != null)
					System.err.println(message.replace("\n", "").replace("<", "").replace(">", ""));
				else
					System.err.println("No message");
				
				if(failure.getTrace().contains(className + ".java:"))
					System.err.println(((failure.getTrace().split(className + ".java:")[1]).split("\\)", 2)[0]).trim());
				else
					System.err.println("-1");
				
				System.err.println(failure.getTestHeader().split("\\(")[0]);
			}
		}
	}
}