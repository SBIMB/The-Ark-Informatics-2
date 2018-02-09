package au.org.theark.report.model.vo.report;

import java.io.Serializable;

public class BiospecimenNucleicAcidSummaryDataRow implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String studyName;
	private String subjectUId;
	private Long biospecimenId;
	private String parentId;
	private String sampleType;
	private Double quantity;
	private String	biospecimenUid;
	private Double	concentration;
	private Double	purity260230;
	private Double	purity260280;
	private Double  amountDNA;
	private String grade;
	
	public BiospecimenNucleicAcidSummaryDataRow() {
		
	}
	
	public BiospecimenNucleicAcidSummaryDataRow(String studyName, String subjectUId,
			Long biospecimenId, String parentId, String sampleType,
			Double quantity, String initialStatus, String biospecimenUid, 
			Double concentration, Double purity260230, Double purity260280, Double amountDNA, String grade){
		this.studyName = studyName;
		this.subjectUId = subjectUId;
		this.biospecimenId = biospecimenId;
		this.parentId = parentId;
		this.sampleType = sampleType;
		this.quantity = quantity;
		this.biospecimenUid = biospecimenUid;
		this.concentration = concentration;
		this.purity260230 = purity260230;
		this.purity260280 = purity260280;
		this.amountDNA = amountDNA;
		this.grade = grade;		
	}

	public String getStudyName() {
		return studyName;
	}

	public void setStudyName(String studyName) {
		this.studyName = studyName;
	}

	public String getSubjectUId() {
		return subjectUId;
	}

	public void setSubjectUId(String subjectUId) {
		this.subjectUId = subjectUId;
	}

	public Long getBiospecimenId() {
		return biospecimenId;
	}

	public void setBiospecimenId(Long biospecimenId) {
		this.biospecimenId = biospecimenId;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public String getSampleType() {
		return sampleType;
	}

	public void setSampleType(String sampleType) {
		this.sampleType = sampleType;
	}

	public Double getQuantity() {
		return quantity;
	}

	public void setQuantity(Double quantity) {
		this.quantity = quantity;
	}

	public String getBiospecimenUid() {
		return biospecimenUid;
	}

	public void setBiospecimenUid(String biospecimenUid) {
		this.biospecimenUid = biospecimenUid;
	}

	public Double getConcentration() {
		return concentration;
	}

	public void setConcentration(Double concentration) {
		this.concentration = concentration;
	}

	public Double getPurity260230() {
		return purity260230;
	}

	public void setPurity260230(Double purity260230) {
		this.purity260230 = purity260230;
	}

	public Double getPurity260280() {
		return purity260280;
	}

	public void setPurity260280(Double purity260280) {
		this.purity260280 = purity260280;
	}

	public Double getAmountDNA() {
		return quantity*concentration*0.001;
	}

	public String getGrade() {
		return grade;
	}

	public void setGrade(String grade) {
		this.grade = grade;
	}
}
