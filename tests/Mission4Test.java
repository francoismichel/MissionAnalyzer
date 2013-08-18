import static org.hamcrest.CoreMatchers.equalTo;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

public class Mission4Test
{
	Class<?> bioInfo;
	Method method;
	ThreadMXBean threadMXB;
	long start;
	long end;
	
	@Rule
    public ErrorCollector collector = new ErrorCollector();
	
	@Before
	public void before() throws ClassNotFoundException
	{
		bioInfo = Class.forName("BioInfo");
		threadMXB = ManagementFactory.getThreadMXBean();
	}
	
	public void printTime(String name)
	{
		System.out.println("€" + name);
		System.out.println(end-start);
	}
	
	public void checkMethod(Class<?> returnType, String name, Class<?>... parameters) throws Throwable
	{
		method = bioInfo.getDeclaredMethod(name, parameters);
		if (!method.getReturnType().equals(returnType))
			throw new NoSuchMethodException("Wrong return type");
		method.setAccessible(true);
	}
	
	@Test
	public void isADN() throws Throwable
	{
		checkMethod(boolean.class, "isADN", String.class);
		
		collector.checkThat((boolean) method.invoke(null, "a"), equalTo(true));
		collector.checkThat((boolean) method.invoke(null, "g"), equalTo(true));
		collector.checkThat((boolean) method.invoke(null, "t"), equalTo(true));
		collector.checkThat((boolean) method.invoke(null, "c"), equalTo(true));
		collector.checkThat((boolean) method.invoke(null, "x"), equalTo(false));
		
		collector.checkThat((boolean) method.invoke(null, "xagtc"), equalTo(false));
		collector.checkThat((boolean) method.invoke(null, "agxtc"), equalTo(false));
		collector.checkThat((boolean) method.invoke(null, "agtcx"), equalTo(false));
		collector.checkThat((boolean) method.invoke(null, "agtc"), equalTo(true));
	}
	
	@Test
	public void count() throws Throwable
	{
		checkMethod(int.class, "count", String.class, char.class);
		
		collector.checkThat((int) method.invoke(null, "a", 'a'), equalTo(1));
		collector.checkThat((int) method.invoke(null, "a", 'x'), equalTo(0));
		collector.checkThat((int) method.invoke(null, "", 'x'), equalTo(0));
		
		collector.checkThat((int) method.invoke(null, "aaaaa", 'a'), equalTo(5));
		collector.checkThat((int) method.invoke(null, "xaaaa", 'a'), equalTo(4));
		collector.checkThat((int) method.invoke(null, "aaxaa", 'a'), equalTo(4));
		collector.checkThat((int) method.invoke(null, "aaaax", 'a'), equalTo(4));
		collector.checkThat((int) method.invoke(null, "xxxxx", 'a'), equalTo(0));
	}
	
	@Test
	public void distanceH() throws Throwable
	{
		checkMethod(int.class, "distanceH", String.class, String.class);
		
		collector.checkThat((int) method.invoke(null, "a", "a"), equalTo(0));
		collector.checkThat((int) method.invoke(null, "abcd", "abcd"), equalTo(0));
		
		collector.checkThat((int) method.invoke(null, "a", "b"), equalTo(1));
		collector.checkThat((int) method.invoke(null, "b", "a"), equalTo(1));
		collector.checkThat((int) method.invoke(null, "aaaaa", "baaaa"), equalTo(1));
		collector.checkThat((int) method.invoke(null, "baaaa", "aaaaa"), equalTo(1));
		collector.checkThat((int) method.invoke(null, "aaaaa", "aaaab"), equalTo(1));
		collector.checkThat((int) method.invoke(null, "aaaab", "aaaaa"), equalTo(1));
		collector.checkThat((int) method.invoke(null, "aabaa", "aaaaa"), equalTo(1));
		collector.checkThat((int) method.invoke(null, "aaaaa", "aabaa"), equalTo(1));
		
		collector.checkThat((int) method.invoke(null, "aaaaa", "bbbbb"), equalTo(5));
		collector.checkThat((int) method.invoke(null, "bbbbb", "aabaa"), equalTo(4));
	}
	
	@Test
	public void plusLongPalindrome() throws Throwable
	{
		checkMethod(String.class, "plusLongPalindrome", String.class);
		
		collector.checkThat((String) method.invoke(null, "abcde"), equalTo(""));
		collector.checkThat((String) method.invoke(null, "aa"), equalTo("aa"));
		collector.checkThat((String) method.invoke(null, "aabcd"), equalTo("aa"));
		collector.checkThat((String) method.invoke(null, "abbcd"), equalTo("bb"));
		collector.checkThat((String) method.invoke(null, "abcdd"), equalTo("dd"));
	}
}