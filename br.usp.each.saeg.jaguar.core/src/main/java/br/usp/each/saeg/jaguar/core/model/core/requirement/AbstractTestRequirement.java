package br.usp.each.saeg.jaguar.core.model.core.requirement;

public abstract class AbstractTestRequirement implements Comparable<AbstractTestRequirement> {

	protected String className;
	private Integer classFirstLine;

	private Integer methodId;
	private Integer methodLine;
	private String methodSignature;
	
	private int cef = 0;
	private int cep = 0;
	private int cnf = 0;
	private int cnp = 0;
	
	private Double suspiciousness = 0.0;
	private Boolean covered;

	public enum Type{
		LINE, DUA;
	}
	
	public abstract Type getType();
	
	public String getClassName() {
		return className;
	}
	
	public void setClassName(String className) {
		this.className = className;
	}

	public Integer getClassFirstLine() {
		return classFirstLine;
	}

	public void setClassFirstLine(Integer classFirstLine) {
		this.classFirstLine = classFirstLine;
	}

	public Integer getMethodId() {
		return methodId;
	}

	public void setMethodId(Integer methodId) {
		this.methodId = methodId;
	}

	public Integer getMethodLine() {
		return methodLine;
	}

	public void setMethodLine(Integer methodLine) {
		this.methodLine = methodLine;
	}

	public String getMethodSignature() {
		return methodSignature;
	}

	public void setMethodSignature(String methodSignature) {
		this.methodSignature = methodSignature;
	}

	public void increaseFailed(){
		cef++;
	}
	
	public int getCef() {
		return cef;
	}

	public void increasePassed(){
		cep++;
	}

	public int getCep() {
		return cep;
	}

	public int getCnf() {
		return cnf;
	}

	public void setCnf(int cnf) {
		this.cnf = cnf;
	}

	public int getCnp() {
		return cnp;
	}

	public void setCnp(int cnp) {
		this.cnp = cnp;
	}

	public double getSuspiciousness() {
		return suspiciousness;
	}

	public void setSuspiciousness(double suspiciousness) {
		this.suspiciousness = suspiciousness;
	}


	public int compareTo(AbstractTestRequirement o) {
		return this.suspiciousness.compareTo(o.getSuspiciousness());
	}

	public Boolean getCovered() {
		return covered;
	}

	public void setCovered(Boolean covered) {
		this.covered = covered;
	}

	@Override
	public String toString() {
		return "AbstractTestRequirement [className=" + className + ", classFirstLine=" + classFirstLine + ", methodId=" + methodId
				+ ", methodLine=" + methodLine + ", methodSignature=" + methodSignature + ", cef=" + cef + ", cep=" + cep + ", cnf=" + cnf
				+ ", cnp=" + cnp + ", suspiciousness=" + suspiciousness + ", covered=" + covered + "]";
	}

}
