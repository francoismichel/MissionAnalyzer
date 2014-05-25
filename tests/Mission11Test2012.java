import static org.hamcrest.CoreMatchers.equalTo;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;

@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class Mission11Test
{
	Class<?> stackIF;
	Class<?> stack;
	Class<?> state;
	Class<? extends Throwable> emptyStackException;
	Method method;
	Constructor<?> constructor;
	Object object1;
	Object object2;
	
	ThreadMXBean threadMXB;
	long start;
	long end;
	
	@Rule
	public ErrorCollector collector = new ErrorCollector();
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	@SuppressWarnings("unchecked")
	@Before
	public void before() throws ClassNotFoundException
	{
		stackIF = Class.forName("StackIF");
		stack = Class.forName("Stack");
		state = Class.forName("State");
		emptyStackException = (Class<? extends Throwable>) Class.forName("EmptyStackException");
		threadMXB = ManagementFactory.getThreadMXBean();
	}
	
	public void printTime(String name, Object... parameters) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		start = threadMXB.getCurrentThreadCpuTime();
		method.invoke(null, parameters);
		end = threadMXB.getCurrentThreadCpuTime();
		
		System.out.println("§" + name);
		System.out.println(end - start);
	}
	
	public void checkMethod(Class<?> cl, Class<?> returnType, String name, Class<?>... parameters) throws Throwable
	{
		method = cl.getDeclaredMethod(name, parameters);
		if (returnType != null && !method.getReturnType().equals(returnType))
			throw new NoSuchMethodException("Wrong return type");
		method.setAccessible(true);
	}
	
	public void checkConstructor(Class<?> cl, Class<?>... parameters) throws Throwable
	{
		constructor = cl.getDeclaredConstructor(parameters);
		constructor.setAccessible(true);
	}
	
	public Object newState() throws Throwable
	{
		checkConstructor(state, double.class, double.class, double.class);
		
		return constructor.newInstance(1.0, 2.0, 3.0);
	}
	
	@Test
	public void stack() throws Throwable
	{
		object1 = stack.newInstance();
		
		collector.checkThat(Arrays.asList(object1.getClass().getInterfaces()).contains(stackIF), equalTo(true));
	}
	
	@Test
	public void stack_push() throws Throwable
	{
		checkMethod(stack, null, "push", state);
		object1 = stack.newInstance();
		
		method.invoke(object1, newState());
	}
	
	@Test
	public void stack_pop_push() throws Throwable
	{
		checkMethod(stack, null, "push", state);
		object1 = stack.newInstance();
		object2 = newState();
		
		method.invoke(object1, object2);
		
		checkMethod(stack, state, "pop", new Class[]{});		
		collector.checkThat(method.invoke(object1).equals(object2), equalTo(true));
		
		try
		{
			thrown.expect(emptyStackException);
			method.invoke(object1);
		}
		catch(InvocationTargetException e)
		{
			throw e.getCause();
		}
	}
	
	@Test
	public void stack_pop() throws Throwable
	{
		checkMethod(stack, state, "pop", new Class[]{});
		object1 = stack.newInstance();
		
		try
		{
			thrown.expect(emptyStackException);
			method.invoke(object1);
		}
		catch(InvocationTargetException e)
		{
			throw e.getCause();
		}
	}
	
	@Test
	public void stack_isEmpty() throws Throwable
	{
		checkMethod(stack, boolean.class, "isEmpty", new Class[]{});
		object1 = stack.newInstance();				
		collector.checkThat((boolean) method.invoke(object1), equalTo(true));
	}
	
	@Test
	public void stack_isEmpty_push() throws Throwable
	{
		checkMethod(stack, null, "push", state);		
		object1 = stack.newInstance();
		method.invoke(object1, newState());
		
		checkMethod(stack, boolean.class, "isEmpty", new Class[]{});
		collector.checkThat((boolean) method.invoke(object1), equalTo(false));
	}
	
	@Test
	public void stack_size() throws Throwable
	{
		checkMethod(stack, int.class, "size", new Class[]{});	
		object1 = stack.newInstance();			
		collector.checkThat((int) method.invoke(object1), equalTo(0));
	}
	
	@Test
	public void stack_size_push() throws Throwable
	{
		checkMethod(stack, null, "push", state);		
		object1 = stack.newInstance();
		method.invoke(object1, newState());
		
		checkMethod(stack, int.class, "size", new Class[]{});		
		collector.checkThat((int) method.invoke(object1), equalTo(1));
	}
	
	@Test
	public void stack_peek() throws Throwable
	{
		checkMethod(stack, state, "peek", int.class);
	}
	
	@Test
	public void stack_peek_push() throws Throwable
	{
		checkMethod(stack, null, "push", state);
		object1 = stack.newInstance();
		object2 = newState();
		
		method.invoke(object1, object2);
		method.invoke(object1, newState());
		method.invoke(object1, newState());
		method.invoke(object1, newState());
		
		checkMethod(stack, state, "peek", int.class);
		collector.checkThat(method.invoke(object1, 3) == object2, equalTo(true));
	}
}