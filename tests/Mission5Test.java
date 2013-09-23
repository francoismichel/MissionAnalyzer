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
public class Mission5Test
{
	Class<?> imageGray;
	Method method;
	ThreadMXBean threadMXB;
	long start;
	long end;
	
	int[][] img1;
	int[][] img2;
	int[][] img3;
	
	@Rule
	public ErrorCollector collector = new ErrorCollector();
	
	@Before
	public void before() throws ClassNotFoundException
	{
		imageGray = Class.forName("ImageGray");
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
		method = imageGray.getDeclaredMethod(name, parameters);
		if (!method.getReturnType().equals(returnType))
			throw new NoSuchMethodException("Wrong return type");
		method.setAccessible(true);
	}
	
	@Test
	public void subtract() throws Throwable
	{
		checkMethod(int[][].class, "subtract", int[][].class, int[][].class, int.class);
		
		img1 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};
		img2 = new int[][]{{1,2,3},{4,0,6},{7,8,9}};
		
		img3 = new int[][]{{255,255,255},{255,5,255},{255,255,255}};
		
		collector.checkThat((int[][]) method.invoke(null, (Object) img1, (Object) img2, 0), equalTo(img3));
		
		img1 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};
		img2 = new int[][]{{1,2,3},{0,0,0},{7,8,9}};
		
		img3 = new int[][]{{255,255,255},{4,5,6},{255,255,255}};
		
		collector.checkThat((int[][]) method.invoke(null, (Object) img1, (Object) img2, 0), equalTo(img3));
		
		img1 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};
		img2 = new int[][]{{0,0,0},{0,0,0},{0,0,0}};
		
		img3 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};
		
		collector.checkThat((int[][]) method.invoke(null, (Object) img1, (Object) img2, 0), equalTo(img3));
	}
	
	@Test
	public void subtract_threshold() throws Throwable
	{
		checkMethod(int[][].class, "subtract", int[][].class, int[][].class, int.class);
		
		img1 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};
		img2 = new int[][]{{2,3,4},{5,7,7},{8,9,10}};
		
		img3 = new int[][]{{255,255,255},{255,5,255},{255,255,255}};
		
		collector.checkThat((int[][]) method.invoke(null, (Object) img1, (Object) img2, 1), equalTo(img3));
		
		img1 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};
		img2 = new int[][]{{1,2,3},{6,7,8},{7,8,9}};
		
		img3 = new int[][]{{255,255,255},{4,5,6},{255,255,255}};
		
		collector.checkThat((int[][]) method.invoke(null, (Object) img1, (Object) img2, 1), equalTo(img3));
		
		img1 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};
		img2 = new int[][]{{3,4,5},{6,7,8},{9,10,11}};
		
		img3 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};
		
		collector.checkThat((int[][]) method.invoke(null, (Object) img1, (Object) img2, 1), equalTo(img3));
	}
	
	@Test
	public void subtract_modify() throws Throwable
	{
		checkMethod(int[][].class, "subtract", int[][].class, int[][].class, int.class);
		
		img3 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};
		
		img1 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};
		img2 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};		
		method.invoke(null, (Object) img1, (Object) img2, 0);
		collector.checkThat(img1, equalTo(img3));
		collector.checkThat(img2, equalTo(img3));
		
		img1 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};
		img2 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};
		method.invoke(null, (Object) img1, (Object) img2, 1);
		collector.checkThat(img1, equalTo(img3));
		collector.checkThat(img2, equalTo(img3));
	}
	
	@Test
	public void brighten() throws Throwable
	{
		checkMethod(int[][].class, "brighten", int[][].class);
		
		img1 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};		
		img2 = new int[][]{{ 
			(int) Math.sqrt(255.0 * 1),
			(int) Math.sqrt(255.0 * 2),
			(int) Math.sqrt(255.0 * 3)
		},
		{
			(int) Math.sqrt(255.0 * 4),
			(int) Math.sqrt(255.0 * 5),
			(int) Math.sqrt(255.0 * 6)
		},
		{
			(int) Math.sqrt(255.0 * 7),
			(int) Math.sqrt(255.0 * 8),
			(int) Math.sqrt(255.0 * 9)
		}};
		img3 = new int[][]{{ 
			(int) Math.round(Math.sqrt(255.0 * 1)),
			(int) Math.round(Math.sqrt(255.0 * 2)),
			(int) Math.round(Math.sqrt(255.0 * 3))
		},
		{
			(int) Math.round(Math.sqrt(255.0 * 4)),
			(int) Math.round(Math.sqrt(255.0 * 5)),
			(int) Math.round(Math.sqrt(255.0 * 6))
		},
		{
			(int) Math.round(Math.sqrt(255.0 * 7)),
			(int) Math.round(Math.sqrt(255.0 * 8)),
			(int) Math.round(Math.sqrt(255.0 * 9))
		}};
		
		collector.checkThat((int[][]) method.invoke(null, (Object) img1), anyOf(equalTo(img2), equalTo(img3)));
		
		img1 = new int[][]{{1,2},{3,4}};
		img2 = new int[][]{{ 
			(int) Math.sqrt(255.0 * 1),
			(int) Math.sqrt(255.0 * 2)
		},
		{
			(int) Math.sqrt(255.0 * 3),
			(int) Math.sqrt(255.0 * 4)		
		}};
		img3 = new int[][]{{ 
			(int) Math.round(Math.sqrt(255.0 * 1)),
			(int) Math.round(Math.sqrt(255.0 * 2))
		},
		{
			(int) Math.round(Math.sqrt(255.0 * 3)),
			(int) Math.round(Math.sqrt(255.0 * 4))		
		}};
		
		collector.checkThat((int[][]) method.invoke(null, (Object) img1), anyOf(equalTo(img2), equalTo(img3)));
		
		img1 = new int[][]{{1}};
		img2 = new int[][]{{(int) Math.sqrt(255.0 * 1)}};
		img3 = new int[][]{{(int) Math.round(Math.sqrt(255.0 * 1))}};
		
		collector.checkThat((int[][]) method.invoke(null, (Object) img1), anyOf(equalTo(img2), equalTo(img3)));
	}
	
	@Test
	public void brighten_modify() throws Throwable
	{
		checkMethod(int[][].class, "brighten", int[][].class);
		
		img1 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};
		img2 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};
		
		method.invoke(null, (Object) img1);
		collector.checkThat(img1, equalTo(img2));
	}
	
	@Test
	public void contains() throws Throwable
	{
		checkMethod(boolean.class, "contains", int[][].class, int[][].class, int.class);
		
		img1 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};
		img2 = new int[][]{{5,6},{8,9}};
		
		collector.checkThat((boolean) method.invoke(null, (Object) img1, (Object) img2, 0), equalTo(true));
		
		img1 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};
		img2 = new int[][]{{9}};
		
		collector.checkThat((boolean) method.invoke(null, (Object) img1, (Object) img2, 0), equalTo(true));
	}
	
	@Test
	public void contains_threshold() throws Throwable
	{
		checkMethod(boolean.class, "contains", int[][].class, int[][].class, int.class);
		
		img1 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};
		img2 = new int[][]{{5,6},{9,10}};
		
		collector.checkThat((boolean) method.invoke(null, (Object) img1, (Object) img2, 1), equalTo(true));
		
		img1 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};
		img2 = new int[][]{{5,6},{9,10}};
		
		collector.checkThat((boolean) method.invoke(null, (Object) img1, (Object) img2, 0), equalTo(false));
		
		img1 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};
		img2 = new int[][]{{10}};
		
		collector.checkThat((boolean) method.invoke(null, (Object) img1, (Object) img2, 1), equalTo(true));
		
		img1 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};
		img2 = new int[][]{{10}};
		
		collector.checkThat((boolean) method.invoke(null, (Object) img1, (Object) img2, 0), equalTo(false));
	}
	
	@Test
	public void contains_equal() throws Throwable
	{
		checkMethod(boolean.class, "contains", int[][].class, int[][].class, int.class);
		
		img1 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};
		img2 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};
		
		collector.checkThat((boolean) method.invoke(null, (Object) img1, (Object) img2, 0), equalTo(true));
	}
	
	@Test
	public void contains_modify() throws Throwable
	{
		checkMethod(boolean.class, "contains", int[][].class, int[][].class, int.class);
		
		img3 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};
		
		img1 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};
		img2 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};
		method.invoke(null, (Object) img1, (Object) img2, 0);
		collector.checkThat(img1, equalTo(img3));
		collector.checkThat(img2, equalTo(img3));
	}
	
	@Test
	public void rescale() throws Throwable
	{
		checkMethod(int[][].class, "rescale", int[][].class, int.class, int.class);
		
		img1 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};
		img2 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};
		
		collector.checkThat((int[][]) method.invoke(null, (Object) img1, img1.length, img1[0].length), equalTo(img2));
		
		img1 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};
		img2 = new int[][]{{1,1,2,2,3,3},{4,4,5,5,6,6},{7,7,8,8,9,9}};
		
		collector.checkThat((int[][]) method.invoke(null, (Object) img1, img1.length, img1[0].length * 2), equalTo(img2));
		
		img1 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};
		img2 = new int[][]{{1,2,3},{1,2,3},{4,5,6},{4,5,6},{7,8,9},{7,8,9}};
		
		collector.checkThat((int[][]) method.invoke(null, (Object) img1, img1.length * 2, img1[0].length), equalTo(img2));
		
		img1 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};
		img2 = new int[][]{{1,1,2,2,3,3},{1,1,2,2,3,3},{4,4,5,5,6,6},{4,4,5,5,6,6},{7,7,8,8,9,9},{7,7,8,8,9,9}};
		
		collector.checkThat((int[][]) method.invoke(null, (Object) img1, img1.length * 2, img1[0].length * 2), equalTo(img2));
		
		img1 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};
		img2 = new int[][]{{1},{4},{7}};
		
		collector.checkThat((int[][]) method.invoke(null, (Object) img1, img1.length, img1[0].length / 2), equalTo(img2));
		
		img1 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};
		img2 = new int[][]{{1,2,3}};
		
		collector.checkThat((int[][]) method.invoke(null, (Object) img1, img1.length / 2, img1[0].length), equalTo(img2));
		
		img1 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};
		img2 = new int[][]{{1}};
		
		collector.checkThat((int[][]) method.invoke(null, (Object) img1, img1.length / 2, img1[0].length / 2), equalTo(img2));
	}
	
	@Test
	public void rescale_modify() throws Throwable
	{
		checkMethod(int[][].class, "rescale", int[][].class, int.class, int.class);
		
		img1 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};
		img2 = new int[][]{{1,2,3},{4,5,6},{7,8,9}};
		method.invoke(null, (Object) img1, img1.length, img1[0].length);
		collector.checkThat(img1, equalTo(img2));
	}
}