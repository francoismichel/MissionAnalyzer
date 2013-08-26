import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;

@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
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
	
	public void printTime(String name, Object... parameters) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		start = threadMXB.getCurrentThreadCpuTime();
		method.invoke(null, parameters);
		end = threadMXB.getCurrentThreadCpuTime();
		
		System.out.println("€" + name);
		System.out.println(end - start);
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
		
		collector.checkThat((boolean) method.invoke(null, "agtc"), equalTo(true));
		collector.checkThat((boolean) method.invoke(null, "agxtc"), equalTo(false));
	}
	
	@Test
	public void isADN_first() throws Throwable
	{
		checkMethod(boolean.class, "isADN", String.class);
		
		collector.checkThat((boolean) method.invoke(null, "xagtc"), equalTo(false));
	}
	
	@Test
	public void isADN_last() throws Throwable
	{
		checkMethod(boolean.class, "isADN", String.class);
		
		collector.checkThat((boolean) method.invoke(null, "agtcx"), equalTo(false));
	}
	
	@Test
	public void isADN_empty() throws Throwable
	{
		checkMethod(boolean.class, "isADN", String.class);
		
		collector.checkThat((boolean) method.invoke(null, ""), equalTo(true));
	}
	
	@Test
	public void count() throws Throwable
	{
		checkMethod(int.class, "count", String.class, char.class);
		
		collector.checkThat((int) method.invoke(null, "aaxaa", 'a'), equalTo(4));
		collector.checkThat((int) method.invoke(null, "xxaxx", 'a'), equalTo(1));
		collector.checkThat((int) method.invoke(null, "xxaxx", 'x'), equalTo(4));
		collector.checkThat((int) method.invoke(null, "aaxaa", 'x'), equalTo(1));
	}
	
	@Test
	public void count_empty() throws Throwable
	{
		checkMethod(int.class, "count", String.class, char.class);
		
		collector.checkThat((int) method.invoke(null, "", 'x'), equalTo(0));
	}
	
	@Test
	public void distanceH() throws Throwable
	{
		checkMethod(int.class, "distanceH", String.class, String.class);
		
		collector.checkThat((int) method.invoke(null, "a", "a"), equalTo(0));
		collector.checkThat((int) method.invoke(null, "abcd", "abcd"), equalTo(0));
		
		collector.checkThat((int) method.invoke(null, "aabaa", "aaaaa"), equalTo(1));
		collector.checkThat((int) method.invoke(null, "aaaaa", "aabaa"), equalTo(1));
	}
	
	@Test
	public void distanceH_first() throws Throwable
	{
		checkMethod(int.class, "distanceH", String.class, String.class);
	
		collector.checkThat((int) method.invoke(null, "aaaaa", "baaaa"), equalTo(1));
		collector.checkThat((int) method.invoke(null, "baaaa", "aaaaa"), equalTo(1));		
	}
	
	@Test
	public void distanceH_last() throws Throwable
	{
		checkMethod(int.class, "distanceH", String.class, String.class);
		
		collector.checkThat((int) method.invoke(null, "aaaaa", "aaaab"), equalTo(1));
		collector.checkThat((int) method.invoke(null, "aaaab", "aaaaa"), equalTo(1));
	}
	
	@Test
	public void distanceH_empty() throws Throwable
	{
		checkMethod(int.class, "distanceH", String.class, String.class);
		
		collector.checkThat((int) method.invoke(null, "", ""), equalTo(0));
	}
	
	@Test
	public void plusLongPalindrome() throws Throwable
	{
		checkMethod(String.class, "plusLongPalindrome", String.class);
		
		collector.checkThat((String) method.invoke(null, "abbbc"), equalTo("bbb"));
		collector.checkThat((String) method.invoke(null, "abbcd"), equalTo("bb"));
		collector.checkThat((String) method.invoke(null, "abccd"), equalTo("cc"));
	}
	
	@Test
	public void plusLongPalindrome_beginning() throws Throwable
	{
		checkMethod(String.class, "plusLongPalindrome", String.class);
		
		collector.checkThat((String) method.invoke(null, "aabcd"), equalTo("aa"));
	}
	
	@Test
	public void plusLongPalindrome_end() throws Throwable
	{
		checkMethod(String.class, "plusLongPalindrome", String.class);
		
		collector.checkThat((String) method.invoke(null, "abcdd"), equalTo("dd"));
	}
	
	@Test
	public void plusLongPalindrome_empty() throws Throwable
	{
		checkMethod(String.class, "plusLongPalindrome", String.class);
		
		collector.checkThat((String) method.invoke(null, ""), equalTo(""));
	}
	
	@Test
	public void plusLongPalindrome_singleCharacter() throws Throwable
	{
		checkMethod(String.class, "plusLongPalindrome", String.class);
		
		collector.checkThat((String) method.invoke(null, "abc"), anyOf(equalTo("a"), equalTo("b"), equalTo("c")));
	}
	
	@Test
	public void plusLongPalindrome_long() throws Throwable
	{
		checkMethod(String.class, "plusLongPalindrome", String.class);
		
		collector.checkThat((String) method.invoke(null, "abbcddde"), equalTo("ddd"));
		collector.checkThat((String) method.invoke(null, "abbcdde"), anyOf(equalTo("bb"), equalTo("dd")));
	}
}