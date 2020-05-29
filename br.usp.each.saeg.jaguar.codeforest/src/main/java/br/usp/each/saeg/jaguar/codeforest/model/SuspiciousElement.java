package br.usp.each.saeg.jaguar.codeforest.model;

import java.util.Collection;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement
public abstract class SuspiciousElement implements Comparable<SuspiciousElement> {

	protected String name;
	protected Integer number = 0;

	protected Integer location = 0;
	protected Double suspiciousValue = 0.0; //TODO check to change it for Big Decimal - more precision
	
	protected int cef = 0;
	protected int cep = 0;
	protected int cnf = 0;
	protected int cnp = 0;
	
	protected boolean enabled = true;
	protected int start;
	protected int end;
	protected String content;
	
	/**
	 * Return its children (e.g. packages should return classes). If it has no
	 * children (e.g. Requirements) return null.
	 */
	public abstract Collection<? extends SuspiciousElement> getChildren();

	/**
	 * Return the name. Package name, simple class name, method signature or
	 * line number.
	 */
	@XmlAttribute
	public String getName() {
		return name;
	}

	/**
	 * Set the name. Package name, simple class name or method signature.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Return the total amount of requirements within this element which has the
	 * maximum suspicious value
	 */
	@XmlAttribute
	public Integer getNumber() {
		return number;
	}

	/**
	 * Set the total amount of requirements within this element which has the
	 * maximum suspicious value
	 */
	public void setNumber(Integer number) {
		this.number = number;
	}

	/**
	 * Return the line number where the element begins
	 */
	//@XmlAttribute
	public Integer getLocation() {
		return location;
	}
	
	/**
	 * Return how many times the element was executed in failing tests. 
	 * 
	 * @return how many times the element was executed in failing tests.
	 */
	@XmlAttribute
	public Integer getCef() {
		return cef;
	}

	public void setCef(Integer cef) {
		this.cef = cef;
	}
	
	/**
	 * Return how many times the element was executed in passing tests. 
	 * 
	 * @return how many times the element was executed in passing tests.
	 */
	@XmlAttribute
	public Integer getCep() {
		return cep;
	}

	public void setCep(Integer cep) {
		this.cep = cep;
	}
	
	/**
	 * Return how many times the element was NOT executed in failing tests. 
	 * 
	 * @return how many times the element was NOT executed in failing tests.
	 */
	@XmlAttribute
	public Integer getCnf() {
		return cnf;
	}

	public void setCnf(Integer cnf) {
		this.cnf = cnf;
	}

	/**
	 * Return how many times the element was NOT executed in passing tests. 
	 * 
	 * @return how many times the element was NOT executed in passing tests.
	 */
	@XmlAttribute
	public Integer getCnp() {
		return cnp;
	}

	public void setCnp(Integer cnp) {
		this.cnp = cnp;
	}
	
	/**
	 * Set the line number where the element begins
	 */
	public void setLocation(Integer location) {
		if (this.location == null || location < this.location) {
			this.location = location;
		}
	}
	
	/**
	 * Return the suspicious value of the element. For package, class or method
	 * represent its children's maximum suspicious value.
	 */
	@XmlAttribute(name = "suspicious-value")
	public Double getSuspiciousValue() {
		return suspiciousValue;
	}

	/**
	 * Set the suspicious value of the element. For package, class or method
	 * represent its children's maximum suspicious value.
	 */
	public void setSuspiciousValue(Double suspiciousValue) {
		this.suspiciousValue = suspiciousValue;
	}

	/**
	 * If the the new value is greater than the old one, update the value and
	 * quantity. If the new value is equal to the old one, update only the
	 * quantity (sum the new and old value). Otherwise, do nothing.
	 * 
	 * @param value
	 *            suspicious value
	 * @param quantity
	 *            quantity of elements with this suspicious value
	 */
	public void updateSupicousness(Double value, Integer quantity) {
		if (quantity == 0) {
			quantity = 1;
		}

		if (value > this.suspiciousValue) {
			this.suspiciousValue = value;
			this.number = quantity;
		} else if (value.equals(this.suspiciousValue)) {
			this.number += quantity;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		/*result = prime * result
				+ ((location == null) ? 0 : location.hashCode());*/
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((number == null) ? 0 : number.hashCode());
		result = prime * result
				+ ((suspiciousValue == null) ? 0 : suspiciousValue.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SuspiciousElement other = (SuspiciousElement) obj;
		/*if (location == null) {
			if (other.location != null)
				return false;
		} else if (!location.equals(other.location))
			return false;*/
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (number == null) {
			if (other.number != null)
				return false;
		} else if (!number.equals(other.number))
			return false;
		if (suspiciousValue == null) {
			if (other.suspiciousValue != null)
				return false;
		} else if (!suspiciousValue.equals(other.suspiciousValue))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SuspiciousElement [name=" + name + ", number=" + number
				+/* ", location=" + location +*/ ", suspiciousValue="
				+ suspiciousValue + "]";
	}

	public int compareTo(SuspiciousElement other) {
		if (this.suspiciousValue > other.suspiciousValue) {
			return -1;
		} else if (this.suspiciousValue < other.suspiciousValue) {
			return 1;
		}
		return 0;
	}

	public void enable() {
        if (enabled) {
            return;
        }
        enabled = true;
    }

    public void disable() {
        if (!enabled) {
            return;
        }
        enabled = false;
    }
    
    public boolean isEnabled(){
    	return enabled;
    }

    @XmlTransient
	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	@XmlTransient
	public int getEnd() {
		return end;
	}

	public void setEnd(int end) {
		this.end = end;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
	
}
