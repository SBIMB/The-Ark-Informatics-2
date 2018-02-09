/*******************************************************************************
 * Copyright (c) 2011  University of Western Australia. All rights reserved.
 * 
 * This file is part of The Ark.
 * 
 * The Ark is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * 
 * The Ark is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package au.org.theark.report.model.vo.report;

import java.io.Serializable;
import java.util.Date;

public class ConsentDetailsDataRow implements Serializable {

	private static final long	serialVersionUID	= 1L; 

	protected String				subjectUID;
	protected String 				otherID_Source;
	protected String				otherID;
	protected String				subjectStatus;
	protected Date					dateOfEnrollment;
	protected Integer				ageAtEnrollment;
	protected String				sex;
	protected String				ethnicity;
	protected String				consentStatus;
	protected Date					consentDate;
	protected String				consentToUseData;
	protected String				consentToShareData;
	protected String				consentToUseBiospecimen;
	protected String				consentToShareBiospecimen;
	
	public ConsentDetailsDataRow() {

	}

	public ConsentDetailsDataRow(String subjectUID, String otherID_Source, String otherID, String subjectStatus, String sex, Date dateOfEnrollment, Integer ageAtEnrollment, String ethnicity, Date consentDate, String consentStatus, String consentToUseData, String consentToShareData, String consentToUseBiospecimen,
			String consentToShareBiospecimen) {
		super();
		this.subjectUID = subjectUID;
		this.otherID_Source = otherID_Source;
		this.otherID = otherID;
		this.subjectStatus = subjectStatus;
		this.dateOfEnrollment = dateOfEnrollment;
		this.ageAtEnrollment = ageAtEnrollment;
		this.sex = sex;
		this.ethnicity = ethnicity;
		this.consentStatus = consentStatus;
		this.consentDate = consentDate;
		this.consentToUseData = consentToUseData;
		this.consentToShareData = consentToShareData;
		this.consentToUseBiospecimen = consentToUseBiospecimen;
		this.consentToShareBiospecimen = consentToShareBiospecimen;
		
	}

	public String getSubjectUID() {
		return subjectUID;
	}

	public void setSubjectUID(String subjectUID) {
		this.subjectUID = subjectUID;
	}

	public String getOtherIDSource() {
		return otherID_Source;
	}
	
	public void setOtherIDSource(String otherID_Source) {
		this.otherID_Source = otherID_Source;
	}
	
	public String getOtherID() {
		return otherID;
	}
	
	public void setOtherID(String otherID) {
		this.otherID = otherID;
	}

	public String getConsentStatus() {
		return consentStatus;
	}

	public void setConsentStatus(String consentStatus) {
		this.consentStatus = consentStatus;
	}

	public String getSubjectStatus() {
		return subjectStatus;
	}

	public void setSubjectStatus(String subjectStatus) {
		this.subjectStatus = subjectStatus;
	}

	public Date getDateOfEnrollment() {
		return dateOfEnrollment;
	}

	public void setDateOfEnrollment(Date dateOfEnrollment) {
		this.dateOfEnrollment = dateOfEnrollment;
	}

	public Integer getAgeAtEnrollment() {
		return ageAtEnrollment;
	}

	public void setAgeAtEnrollment(Integer ageAtEnrollment) {
		this.ageAtEnrollment = ageAtEnrollment;
	}

	public String getEthnicity() {
		return ethnicity;
	}

	public void setEthnicity(String ethnicity) {
		this.ethnicity = ethnicity;
	}

	public String getConsentToUseData() {
		return consentToUseData;
	}

	public void setConsentToUseData(String consentToUseData) {
		this.consentToUseData = consentToUseData;
	}

	public String getConsentToShareData() {
		return consentToShareData;
	}

	public void setConsentToShareData(String consentToShareData) {
		this.consentToShareData = consentToShareData;
	}

	public String getConsentToUseBiospecimen() {
		return consentToUseBiospecimen;
	}

	public void setConsentToUseBiospecimen(String consentToUseBiospecimen) {
		this.consentToUseBiospecimen = consentToUseBiospecimen;
	}

	public String getConsentToShareBiospecimen() {
		return consentToShareBiospecimen;
	}

	public void setConsentToShareBiospecimen(String consentToShareBiospecimen) {
		this.consentToShareBiospecimen = consentToShareBiospecimen;
	}

	public String getSex() {
		return sex;
	}

	public void setSex(String sex) {
		this.sex = sex;
	}

	public Date getConsentDate() {
		return consentDate;
	}

	public void setConsentDate(Date consentDate) {
		this.consentDate = consentDate;
	}

}
