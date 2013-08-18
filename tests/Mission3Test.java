import static org.hamcrest.CoreMatchers.equalTo;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class Mission3Test
{
	Class<?> libMath;
	Method method;
	ThreadMXBean threadMXB;
	long start;
	long end;
	double result;
	
	@Rule
	public ErrorCollector collector = new ErrorCollector();
	
	@Before
	public void before() throws ClassNotFoundException
	{
		libMath = Class.forName("LibMath");
		threadMXB = ManagementFactory.getThreadMXBean();
	}
	
	public void printTime(String name)
	{
		System.out.println("€" + name);
		System.out.println(end-start);
	}
	
	public void checkMethod(Class<?> returnType, String name, Class<?>... parameters) throws NoSuchMethodException, SecurityException
	{
		method = libMath.getDeclaredMethod(name, parameters);
		if (!method.getReturnType().equals(returnType))
			throw new NoSuchMethodException("Wrong return type");
		method.setAccessible(true);
	}
	
	@Test
	public void average() throws Throwable
	{
		checkMethod(double.class, "average", double.class, double.class, double.class);
		
		start = threadMXB.getCurrentThreadCpuTime();
		result = (double) method.invoke(null, 3.0, 2.0, 7.0);
		end = threadMXB.getCurrentThreadCpuTime();
		
		collector.checkThat(result, equalTo(4.0));
		
		printTime("average");
	}
	
	@Test
	public void median() throws Throwable
	{
		checkMethod(double.class, "median", double.class, double.class, double.class);
		
		start = threadMXB.getCurrentThreadCpuTime();
		result = (double) method.invoke(null, 3.0, 2.0, 7.0);
		end = threadMXB.getCurrentThreadCpuTime();
		
		collector.checkThat(result, equalTo(3.0));
		
		printTime("median");
	}
	
	@Test
	public void maximum() throws Throwable
	{
		checkMethod(double.class, "maximum", double.class, double.class, double.class);
		
		start = threadMXB.getCurrentThreadCpuTime();
		result = (double) method.invoke(null, 3.0, 2.0, 7.0);
		end = threadMXB.getCurrentThreadCpuTime();
		
		collector.checkThat(result, equalTo(7.0));
		
		printTime("maximum");
	}
	
	@Test
	public void minimum() throws Throwable
	{
		checkMethod(double.class, "minimum", double.class, double.class, double.class);
		
		start = threadMXB.getCurrentThreadCpuTime();
		result = (double) method.invoke(null, 3.0, 2.0, 7.0);
		end = threadMXB.getCurrentThreadCpuTime();
		
		collector.checkThat(result, equalTo(2.0));
		
		printTime("minimum");
	}
}