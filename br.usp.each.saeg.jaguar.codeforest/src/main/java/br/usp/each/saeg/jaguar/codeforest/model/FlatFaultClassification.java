package br.usp.each.saeg.jaguar.codeforest.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

@XmlRootElement(name = "FlatFaultClassification")
@XmlSeeAlso({DuaRequirement.class, LineRequirement.class})
public class FlatFaultClassification extends FaultClassification {

	private List<Requirement> requirements = new ArrayList<Requirement>();

	@XmlElement
	public List<Requirement> getRequirements() {
		return requirements;
	}

	public void setRequirements(List<Requirement> requirements) {
		this.requirements = requirements;
	}

	@Override
	public List<? extends SuspiciousElement> getSuspiciousElementList() {
		return getRequirements();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((requirements == null) ? 0 : requirements.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		FlatFaultClassification other = (FlatFaultClassification) obj;
		if (requirements == null) {
			if (other.requirements != null)
				return false;
		} else if (!requirements.equals(other.requirements))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "FlatFaultClassification [requirements=" + requirements + ", project=" + project + ", heuristic=" + heuristic
				+ ", requirementType=" + requirementType + ", timeSpent=" + timeSpent + "]";
	}

	
}
