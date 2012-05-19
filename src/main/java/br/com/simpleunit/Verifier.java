package br.com.simpleunit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import br.com.simpleunit.annotations.Unit;
import br.com.simpleunit.objects.AssertionResult;
import br.com.simpleunit.objects.CurrentUnitMethodExecution;
import br.com.simpleunit.objects.CurrentUnitMethodExecutionAssertion;
import br.com.simpleunit.objects.TestCase;
import br.com.simpleunit.objects.UnitResult;
import br.com.simpleunit.util.None;

/**
 * This class do the hard work. SimpleUnit call Verifier to run every TestCase and at the final,
 * return the results of every @Unit method of passed TestCases
 * 
 * @author Robson Paulo Kraemer (rpkraemer@gmail.com)
 *
 */
public class Verifier {

	private List<TestCase> testCases;
	private List<UnitResult> unitResults;

	protected static CurrentUnitMethodExecution currentUnitMethodExecution;

	/**
	 * Public method invoked by SimpleUnit class, receives testCases to be executed
	 * 
	 * @param testCases
	 * @throws IllegalArgumentException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public void verify(List<TestCase> testCases)
			throws IllegalArgumentException, InstantiationException,
			IllegalAccessException, InvocationTargetException {
		
		this.testCases = testCases;
		this.unitResults = new ArrayList<UnitResult>();
		
		verify();
	}

	private void verify() throws InstantiationException,
			IllegalAccessException, IllegalArgumentException,
			InvocationTargetException {
		
		for (int i = 0; i < testCases.size(); i++) {

			TestCase testCase = testCases.get(i);		
			Class<?> testClass = testCase.getTestClass();
			Object instance = testClass.newInstance();

			for (Method unit : testCase.getUnitMethods()) {
				
				//Antes de prosseguir com a invoca��o do m�todo @Unit, executa todos os m�todos de fixtures @BeforeUnit na TestCase
				invokeBeforeUnitMethods(testCase, instance);
				
				
				//Antes de invocar o m�todo @Unit, inicializa uma nova execu��o corrente
				//Seta o testCase na execu��o corrente
				currentUnitMethodExecution = new CurrentUnitMethodExecution();
				currentUnitMethodExecution.setTestCase(testCase);
				
				//Recupera informa��es de exce��o esperada da anota��o do @Unit corrente
				Unit unitAnnotation = unit.getAnnotation(Unit.class);
				Class<?> expectedException = unitAnnotation.shouldRise();
				
				//Se o @Unit espera exce��o, coloca ela no objeto que abriga a execu��o do @Unit corrente
				if (expectedException != None.class) {
					currentUnitMethodExecution.setExpectedException(expectedException);
				}
				
				try {
					unit.invoke(instance);
				} catch (Throwable exception) {
					//Se houve uma exce��o, coloca informa��es da
					//Throwable exception ocorrida no objeto de execu��o corrente
					currentUnitMethodExecution.setExceptionOccurred(exception.getCause().getClass());
				}	

				//cria objeto de resultado para a execu��o do Unit corrente
				UnitResult unitResult = new UnitResult();
				unitResult.setTestCase(testCase);
				unitResult.setUnitMethod(unit);
				unitResult.setNumberOfAssertions(currentUnitMethodExecution.totalNumberOfAssertions());
				unitResult.setNumberOfFailedAssertions(currentUnitMethodExecution.getNumberOfFailedAssertions());
				unitResult.setNumberOfPassedAssertions(currentUnitMethodExecution.getNumberOfPassedAssertions());
				
				unitResult.setExpectedException(currentUnitMethodExecution.getExpectedException());
				unitResult.setExceptionOccurred(currentUnitMethodExecution.getExceptionOccurred());
				
				unitResult.setPassed(currentUnitMethodExecution.isPassed());
				
				//Cria objetos com os detalhes de cada asser��o executada nesse Unit e adiciona ao objeto de resultado
				for (CurrentUnitMethodExecutionAssertion assertion : currentUnitMethodExecution.getAssertions()) {
					AssertionResult assertionResult = new AssertionResult();
					assertionResult.setAssertionType(assertion.getAssertionType());
					assertionResult.setMessage(assertion.getMessage());
					assertionResult.setPassed(assertion.isPassed());
					
					unitResult.addAssertion(assertionResult);
				}
				
				
				unitResults.add(unitResult);
			}
		}
	}

	private void invokeBeforeUnitMethods(TestCase testCase, Object instance) 
			throws IllegalArgumentException, 
				   IllegalAccessException, 
				   InvocationTargetException {
		
		for (Method beforeUnitMethod : testCase.getBeforeUnitMethods())
			beforeUnitMethod.invoke(instance);
	}
	
	/**
	 * 
	 * @return the results of every @Unit executed by Verifier
	 */
	public List<UnitResult> getUnitMethodsResults() {
		return unitResults;
	}			
}