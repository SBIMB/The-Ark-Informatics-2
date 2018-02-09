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
package za.ac.theark.study.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jxl.Workbook;
import jxl.read.biff.BiffException;

import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.theark.core.Constants;
import au.org.theark.core.exception.ArkBaseException;
import au.org.theark.core.exception.ArkSystemException;
import au.org.theark.core.exception.EntityNotFoundException;
import au.org.theark.core.exception.FileFormatException;
import au.org.theark.core.model.study.entity.Address;
import au.org.theark.core.model.study.entity.AddressStatus;
import au.org.theark.core.model.study.entity.AddressType;
import au.org.theark.core.model.study.entity.ArkFunction;
import au.org.theark.core.model.study.entity.Consent;
import au.org.theark.core.model.study.entity.ConsentOption;
import au.org.theark.core.model.study.entity.ConsentStatus;
import au.org.theark.core.model.study.entity.ConsentType;
import au.org.theark.core.model.study.entity.Country;
import au.org.theark.core.model.study.entity.CustomField;
import au.org.theark.core.model.study.entity.CustomFieldDisplay;
import au.org.theark.core.model.study.entity.EmailStatus;
import au.org.theark.core.model.study.entity.EthnicityType;
import au.org.theark.core.model.study.entity.GenderType;
import au.org.theark.core.model.study.entity.LinkSubjectPedigree;
import au.org.theark.core.model.study.entity.LinkSubjectStudy;
import au.org.theark.core.model.study.entity.LinkSubjectTwin;
import au.org.theark.core.model.study.entity.MaritalStatus;
import au.org.theark.core.model.study.entity.Person;
import au.org.theark.core.model.study.entity.PersonContactMethod;
import au.org.theark.core.model.study.entity.PersonLastnameHistory;
import au.org.theark.core.model.study.entity.Phone;
import au.org.theark.core.model.study.entity.PhoneStatus;
import au.org.theark.core.model.study.entity.PhoneType;
import au.org.theark.core.model.study.entity.Relationship;
import au.org.theark.core.model.study.entity.State;
import au.org.theark.core.model.study.entity.Study;
import au.org.theark.core.model.study.entity.StudyComp;
import au.org.theark.core.model.study.entity.StudyCompStatus;
import au.org.theark.core.model.study.entity.SubjectCustomFieldData;
import au.org.theark.core.model.study.entity.SubjectFile;
import au.org.theark.core.model.study.entity.SubjectStatus;
import au.org.theark.core.model.study.entity.TitleType;
import au.org.theark.core.model.study.entity.TwinType;
import au.org.theark.core.model.study.entity.VitalStatus;
import au.org.theark.core.model.study.entity.YesNo;
import au.org.theark.core.service.IArkCommonService;
import au.org.theark.core.util.DataConversionAndManipulationHelper;
import au.org.theark.core.util.XLStoCSV;
import au.org.theark.core.vo.ConsentVO;
import au.org.theark.core.vo.SubjectVO;
import au.org.theark.study.service.IStudyService;
import au.org.theark.study.util.CustomFieldUploadValidator;
import au.org.theark.study.util.DataUploader;
import au.org.theark.study.util.LinkSubjectStudyConsentHistoryComparator;

import com.csvreader.CsvReader;
import com.google.code.jqwicket.api.S.SBuilder;

/**
 * SubjectUploader provides support for uploading subject matrix-formatted files.
 * 
 * @author cellis
 */
public class DataREDCapUploader extends DataUploader{
	private char					delimiterCharacter	= Constants.DEFAULT_DELIMITER_CHARACTER;
	private Study					study;
	static Logger					log						= LoggerFactory.getLogger(DataUploader.class);
	@SuppressWarnings("unchecked")
	private IArkCommonService	iArkCommonService		= null;
	private IStudyService		iStudyService			= null;
	private StringBuffer			uploadReport			= null;
	private SimpleDateFormat	simpleDateFormat		= new SimpleDateFormat("yyyy-mm-dd");

	/**
	 * SubjectUploader constructor
	 * 
	 * @param study
	 *           study identifier in context
	 * @param iArkCommonService
	 *           common ARK service to perform select/insert/updates to the database
	 * @param iStudyService
	 *           study service to perform select/insert/updates to the study database
	 */
	@SuppressWarnings("unchecked")
	public DataREDCapUploader(Study study, IArkCommonService iArkCommonService, IStudyService iStudyService) {
		super(study, iArkCommonService, iStudyService);
		this.study = study;
		this.iArkCommonService = iArkCommonService;
		this.iStudyService = iStudyService;
		simpleDateFormat.setLenient(false);
	}
	
	/**
	 * Assumes a UID must be unique as this is only looking for a listOfSubjects PRE FILTERED based on a studies list of subjects to be changed
	 */
	public LinkSubjectStudy getSubjectByUIDFromExistList(List<LinkSubjectStudy> listOfSubjects, String subjectUID) {
		for (LinkSubjectStudy potentialSubject : listOfSubjects) {
			if (potentialSubject.getSubjectUID().equals(subjectUID)) {
				return potentialSubject;
			}
		}
		return null;
	}

	/**
	 * Imports the subject data file to the database tables, and creates report on the process Assumes the file is in the default "matrix" file format:
	 * SUBJECTUID,FIELD1,FIELD2,FIELDN... 1,01/01/1900,99.99,99.99,, ...
	 * 
	 * Where N is any number of columns
	 * 
	 * @param fileInputStream
	 *           is the input stream of a file
	 * @param inLength
	 *           is the length of a file
	 * @throws FileFormatException
	 *            file format Exception
	 * @throws ArkBaseException
	 *            general ARK Exception
	 * @return the upload report detailing the upload process
	 **/
		
	/**
	 * Imports the subject data file to the database tables, and creates report on the process Assumes the file is in the default "matrix" file format:
	 * SUBJECTUID,FIELD1,FIELD2,FIELDN... 1,01/01/1900,99.99,99.99,, ...
	 * 
	 * Where N is any number of columns
	 * 
	 * @param fileInputStream
	 *           is the input stream of a file
	 * @param inLength
	 *           is the length of a file
	 * @throws FileFormatException
	 *            file format Exception
	 * @throws ArkBaseException
	 *            general ARK Exception
	 * @return the upload report detailing the upload process
	 **/
	@SuppressWarnings("unchecked")
	public StringBuffer uploadAndReportMatrixSubject(List<List> subjectList, List<String> columnHeaders, List<StringBuilder> uidsWhichNeedUpdating){
		List<LinkSubjectStudy> insertSubjects = new ArrayList<LinkSubjectStudy>();
		List<LinkSubjectStudy> updateSubjects = new ArrayList<LinkSubjectStudy>();
		List<List> insertChildStudies = new ArrayList<List>();
		long rowCount = 0;
		long subjectCount = 0;
		long insertCount = 0;
		long updateCount = 0;
		long srcLength = -1; // -1 means nothing being processed
		uploadReport = new StringBuffer();

		DecimalFormat decimalFormat = new DecimalFormat("0.00");

		try {
			String[] stringLineArray;

			// this is a list of all our somewhat enum-like reference tables...
			// much better to call this once than each one n times in the for loop...plus each ones default is n times
			// should save 200,000-250,000 selects for a 17K insert. may still wish to evaluate whats best here
			Collection<SubjectStatus> subjectStatusPossible = iArkCommonService.getSubjectStatus();
			Collection<GenderType> genderTypesPossible = iArkCommonService.getGenderTypes();
			Collection<EthnicityType> ethnicityTypesPossible =iArkCommonService.getEthnicityTypes();
			YesNo yes = iArkCommonService.getYes();// TODO: boolean
			YesNo no = iArkCommonService.getNo();

			boolean autoConsent = study.getAutoConsent();
			SubjectStatus defaultSubjectStatus = iStudyService.getDefaultSubjectStatus();
			GenderType defaultGenderType = iStudyService.getDefaultGenderType();
			EthnicityType defaultEthnicityType = iStudyService.getDefaultEthnicityType();
			ConsentOption concentOptionOfYes = iStudyService.getConsentOptionForBoolean(true);// sounds a lot like boolean blah = true????
			ConsentStatus consentStatusOfConsented = iStudyService.getConsentStatusByName("Consented");
			ConsentType consentTypeOfElectronic = iStudyService.getConsentTypeByName("Electronic");

			List<ConsentOption> consentOptionsPossible = iStudyService.getConsentOptions();
			List<ConsentStatus> consentStatusPossible = iStudyService.getConsentStatus();
			List<ConsentType> consentTypePossible = iStudyService.getConsentType();
			List<Study> subStudiesAvailable = iStudyService.getChildStudyListOfParent(study);
			
			List<LinkSubjectStudy> allSubjectWhichWillBeUpdated = null;
			if (uidsWhichNeedUpdating.size() > 0) {
				// TODO analyse performance of bringing all back and having to iterate everytime, vs conditional query + looping through less
				// TODO analyze performance of getting that big list of UIDs and doing a .contains(subjectuid) VS getting all the entities and doing a
				// .getSubjectUID.equals(subjectUID)
				allSubjectWhichWillBeUpdated = iArkCommonService.getUniqueSubjectsWithTheseUIDs(study, uidsWhichNeedUpdating);
			}else{
				allSubjectWhichWillBeUpdated = new ArrayList();
			}
			
			//"SUBJECTUID","","","","","","","","","	","","","",""
			int dateOfEnrollmentIndex = columnHeaders.indexOf("DATE_OF_ENROLLMENT");
			int otherIdIndex = columnHeaders.indexOf("OTHER_ID");
			int commentsIndex = columnHeaders.indexOf("COMMENTS");
			int statusIndex = columnHeaders.indexOf("SUBJECT_STATUS");
			int ageIndex =  columnHeaders.indexOf("AGE_AT_ENROLLMENT");
			int ethncityIndex = columnHeaders.indexOf("ETHNICITY");
			int genderIndex = columnHeaders.indexOf("GENDER");
			int subStudiesIndex = columnHeaders.indexOf("SUBSTUDYS");
			int consentDateIndex = columnHeaders.indexOf("CONSENT_DATE");
			int consentStatusIndex = columnHeaders.indexOf("CONSENT_STATUS");
			int consentToUseDataIndex = columnHeaders.indexOf("CONSENT_TO_USE_DATA");
			int consentToShareDataIndex = columnHeaders.indexOf("CONSENT_TO_SHARE_DATA");
			int consentToUseBiospecimenIndex = columnHeaders.indexOf("CONSENT_TO_USE_BIOSPECIMEN");
			int consentToShareBiospecimenIndex = columnHeaders.indexOf("CONSENT_TO_SHARE_BIOSPECIMEN");
						
			boolean isAutoGen = study.getAutoGenerateSubjectUid();

			for(List<StringBuilder> record : subjectList) {
				LinkSubjectStudy subject = null;
				Boolean b = false;
								
				// Hack to ensure XLS rows contain all cells (ie empty strings for cells after the right-hand non-null value
				/*List<String> stringList = new ArrayList<String>(csvReader.getHeaders().length);
				for (int i = 0; i < csvReader.getHeaders().length; i++) {
					stringList.add(csvReader.get(i));
				}*/
				/*stringLineArray = stringList.toArray(new String[csvReader.getHeaders().length]);*/
				String subjectUID = record.get(0).toString();
				rowCount++;
				
				boolean hasSomeData = false;
				for (StringBuilder next : record){
					if (next != null && !next.toString().isEmpty()){
						hasSomeData = true;
					}
				}

				if (!isAutoGen && (subjectUID == null || subjectUID.isEmpty())){
					if (!hasSomeData){
						uploadReport.append("Warning/Info: Row " + rowCount + ":  There appeared to be no data on this row, so we ignored this line");
					}else{
						// THIS SHOULD NEVER EVER HAPPEN IF VALIDATION IS CORRECT
						uploadReport.append("Error: Row " + rowCount + ":  There is no subject UID on this row, "
								+ "yet the study is not set up to auto generate subject UIDs.  This line was ignored.  Please remove this line or provide an ID");
					}
				}else if (isAutoGen && (subjectUID == null || subjectUID.isEmpty()) && !hasSomeData){
					uploadReport.append("Warning/Info: Row " + rowCount + ":  There appeared to be no data on this row, so we ignored this line");
				}else{
					subject = getSubjectByUIDFromExistList(allSubjectWhichWillBeUpdated, subjectUID);
					
					boolean thisSubjectAlreadyExists = (subject != null);

					Person person = null;
					if (thisSubjectAlreadyExists){
						person = subject.getPerson();
					}else{
						subject = new LinkSubjectStudy();
						subject.setSubjectUID(subjectUID);// note: this will be overwritten IF study.isautogenerate
						subject.setStudy(study);
						person = new Person();
					}
										
					if (commentsIndex > 0)
						subject.setComment("");
					
					if (dateOfEnrollmentIndex > 0)
							subject.setDateOfEnrollment(simpleDateFormat.parse(record.get(dateOfEnrollmentIndex).toString()));

					if (ageIndex > 0){
						subject.setAgeAtEnrollment(Integer.parseInt(record.get(ageIndex).toString()));
									
					if (genderIndex > 0) {
						if (record.get(genderIndex).toString() != null && record.get(genderIndex).length() > 0) {
							for (GenderType boygirl : genderTypesPossible) {
								if (boygirl.getName().equalsIgnoreCase(record.get(genderIndex).toString())) {
									Hibernate.initialize(subject.getPerson().getGenderType());
									person.setGenderType(boygirl);
								}
							}
						}
						if (person.getGenderType() == null || StringUtils.isBlank(person.getGenderType().getName())) {
							person.setGenderType(defaultGenderType);
						}
					}
					
					if (person.getGenderType() == null) {
						person.setGenderType(defaultGenderType);
					}

					if (ethncityIndex > 0) {
						if (record.get(ethncityIndex) != null && record.get(ethncityIndex).length() > 0) {
							for (EthnicityType ethnicity : ethnicityTypesPossible) {
								if (ethnicity.getName().equalsIgnoreCase(record.get(ethncityIndex).toString())) {
									person.setEthnicityType(ethnicity);
								}
							}
						}
						if (person.getEthnicityType() == null || StringUtils.isBlank(person.getEthnicityType().getName())) {
							person.setEthnicityType(defaultEthnicityType);
						}
					}
					if (person.getEthnicityType() == null) {
						person.setEthnicityType(defaultEthnicityType);
					}
					
					if (statusIndex > 0) {
						String statusStr = (record.get(statusIndex).toString());
						for (SubjectStatus subjectStat : subjectStatusPossible) {
							if (subjectStat.getName().equalsIgnoreCase(statusStr)) {
								subject.setSubjectStatus(subjectStat);
							}
						}
						
						if (subject.getSubjectStatus() == null || StringUtils.isBlank(subject.getSubjectStatus().getName())) {
							subject.setSubjectStatus(defaultSubjectStatus);
						}

					} else {
						subject.setSubjectStatus(defaultSubjectStatus);
					}
					
					// if the study is autoconsent...then there are some defaults we have to set TODO get rid of hardcoding
					subject.setUpdateConsent(false);
					
					if (autoConsent && subject.getSubjectStatus().getName().equalsIgnoreCase("Subject")) {
						subject.setConsentDate(new Date());
						subject.setConsentStatus(consentStatusOfConsented);
						
						ConsentOption defaultConsentOption = concentOptionOfYes;
						subject.setConsentToUseData(defaultConsentOption);
						subject.setConsentToUseData(defaultConsentOption);
						subject.setConsentToUseBiospecimen(defaultConsentOption);
						subject.setConsentToShareData(defaultConsentOption);
						subject.setConsentToShareBiospecimen(defaultConsentOption);
					} else {
						// Manual Consent details
						String consentDate = record.get(dateOfEnrollmentIndex).toString();
						String consentStatusStr = record.get(statusIndex).toString();
						String useDataStr = record.get(consentToUseDataIndex).toString();
						String useBioStr = record.get(consentToUseBiospecimenIndex).toString();
						String shareDataStr = record.get(consentToShareDataIndex).toString();
						String shareBioStr = record.get(consentToShareBiospecimenIndex).toString();

						if (!consentDate.isEmpty() || !consentStatusStr.isEmpty() || !useDataStr.isEmpty() || !useBioStr.isEmpty() || !shareDataStr.isEmpty() || !shareBioStr.isEmpty()) {
							LinkSubjectStudy newSubject = new LinkSubjectStudy();

							if (!consentDate.isEmpty()) {
								newSubject.setConsentDate(simpleDateFormat.parse(consentDate));
							}

							if (!consentStatusStr.isEmpty()) {
								for (ConsentStatus consentStatus : consentStatusPossible) {
									if (consentStatus.getName().equalsIgnoreCase(consentStatusStr)) {
										newSubject.setConsentStatus(consentStatus);
									}else{
										newSubject.setConsentStatus(consentStatusOfConsented);
									}
									
								}
							}else{
								newSubject.setConsentStatus(consentStatusOfConsented);
							}

							if (!useDataStr.isEmpty() || !useBioStr.isEmpty() || !shareDataStr.isEmpty() || !shareBioStr.isEmpty()) {
								for (ConsentOption consentOption : consentOptionsPossible) {
									if (consentOption.getName().equalsIgnoreCase(useDataStr)) {
										newSubject.setConsentToUseData(consentOption);
									}
									
									if (consentOption.getName().equalsIgnoreCase(useBioStr)) {
										newSubject.setConsentToUseBiospecimen(consentOption);
									}

									if (consentOption.getName().equalsIgnoreCase(shareDataStr)) {
										newSubject.setConsentToShareData(consentOption);
									}

									if (consentOption.getName().equalsIgnoreCase(shareBioStr)) {
										newSubject.setConsentToShareBiospecimen(consentOption);
									}
								}
							}

							if (thisSubjectAlreadyExists) {
								// Existing Subject to compare if consent actually changed (inherently handles when no consent previously)
								LinkSubjectStudyConsentHistoryComparator comparator = new LinkSubjectStudyConsentHistoryComparator();
								if (comparator.compare(subject, newSubject) != 0) {
									subject.setUpdateConsent(true);
									subject.setConsentDate(newSubject.getConsentDate());
									subject.setConsentStatus(newSubject.getConsentStatus());
									subject.setConsentToUseData(newSubject.getConsentToUseData());
									subject.setConsentToUseBiospecimen(newSubject.getConsentToUseBiospecimen());
									subject.setConsentToShareData(newSubject.getConsentToShareData());
									subject.setConsentToShareBiospecimen(newSubject.getConsentToShareBiospecimen());
								} else {
									subject.setUpdateConsent(false);
								}
							} else {
								// New Subject with consent details
								subject.setConsentDate(newSubject.getConsentDate());
								subject.setConsentStatus(newSubject.getConsentStatus());
								subject.setConsentToUseData(newSubject.getConsentToUseData());
								subject.setConsentToUseBiospecimen(newSubject.getConsentToUseBiospecimen());
								subject.setConsentToShareData(newSubject.getConsentToShareData());
								subject.setConsentToShareBiospecimen(newSubject.getConsentToShareBiospecimen());
							}
						}
					}					
					
					subject.setPerson(person);

					if (subject.getId() == null || subject.getPerson().getId() == 0) {
						insertSubjects.add(subject);
																	
						/*
						 * StringBuffer sb = new StringBuffer(); //does this report have to happen? ... and in reality it hasnt had success yet
						 * sb.append("\nCreated subject from original Subject UID: "); sb.append(subject.getSubjectUID());
						 * //sb.append(" has been created successfully and linked to the study: "); //sb.append(study.getName()); //sb.append("\n");
						 * uploadReport.append(sb);
						 */
						insertCount++;
					} else {
						// iStudyService.updateSubject(subjectVo);
						updateSubjects.add(subject);
						/*
						 * StringBuffer sb = new StringBuffer(); sb.append("\nUpdate subject with Subject UID: "); sb.append(subject.getSubjectUID());
						 * //sb.append(" has been updated successfully and linked to the study: "); //sb.append(study.getName()); //sb.append("\n");
						 * uploadReport.append(sb);
						 */
						updateCount++;
						}
					subjectCount++;
					}
				}
			}
		}
		catch (Exception ex) {
			//uploadReport.append("System Error:  Unexpected exception whilst reading the subject data file\n");
			log.error("processMatrixSubjectFile Exception stacktrace:", ex);
			ex.printStackTrace();
			//throw new ArkSystemException("Unexpected exception occurred when trying to process subject data file");
		}
		finally {
			
		}/*
		 * uploadReport.append("Processed "); uploadReport.append(subjectCount); uploadReport.append(" rows for "); uploadReport.append(subjectCount);
		 * uploadReport.append(" subjects."); uploadReport.append("\n"); uploadReport.append(insertCount);
		 * uploadReport.append(" fields were inserted."); uploadReport.append("\n"); uploadReport.append(updateCount);
		 * uploadReport.append(" fields were updated."); uploadReport.append("\n");
		 */

		uploadReport.append("Processed ");
		uploadReport.append(subjectCount);
		uploadReport.append(" rows.");
		uploadReport.append("\n");
		uploadReport.append("Inserted ");
		uploadReport.append(insertCount);
		uploadReport.append(" subjects.");
		uploadReport.append("\n");
		uploadReport.append("Updated ");
		uploadReport.append(updateCount);
		uploadReport.append(" subjects.");
		uploadReport.append("\n");

		// TODO better exceptionhandling
		iStudyService.processBatch(insertSubjects, study, updateSubjects);
		
		for(LinkSubjectStudy subject : insertSubjects){
			
			if(insertChildStudies!=null){
				assignChildStudies(subject, insertChildStudies.get(insertSubjects.indexOf(subject)));
			}
		}

		return uploadReport;
	}
	
	/* TODO: This is updating twice on those that are selected...this is probably worth avoiding (particularly once we start auditing etc) */
	private void assignChildStudies(LinkSubjectStudy subject, List<Study> childStudies) {
		// Archive LinkSubjectStudy for all unassigned child studies
		List<Study> availableChildStudies = null;
		availableChildStudies = iStudyService.getChildStudyListOfParent(subject.getStudy());
		for (Study childStudy : availableChildStudies) {	
			try {
				subject = iArkCommonService.getSubject(subject.getPerson().getId(), childStudy);
				subject.setSubjectStatus(iArkCommonService.getSubjectStatus("Archive"));
				iStudyService.update(subject);
			}
			catch (EntityNotFoundException e) { // TODO :probably dont need an exception here
				// log.error(e.getMessage());
			}
		}

		// Update ArkUser for all assigned child studies
		for (Study childStudy : childStudies) {
			LinkSubjectStudy linkSubjectStudy = new LinkSubjectStudy();
			try {
				// Found a previous archived record
				linkSubjectStudy = iArkCommonService.getSubject(subject.getPerson().getId(), childStudy);
				linkSubjectStudy.setSubjectStatus(iArkCommonService.getSubjectStatus("Subject"));
				iStudyService.update(linkSubjectStudy);
			}
			catch (EntityNotFoundException e) {
				// Subject not assigned to child study, clone/assign accordingly
				linkSubjectStudy = new LinkSubjectStudy();
				linkSubjectStudy.setStudy(childStudy);
				linkSubjectStudy.setPerson(subject.getPerson());
				linkSubjectStudy.setSubjectUID(subject.getSubjectUID());
				linkSubjectStudy.setSubjectStatus(subject.getSubjectStatus());
				linkSubjectStudy.setConsentStatus(subject.getConsentStatus());
				linkSubjectStudy.setConsentDate(subject.getConsentDate());
				linkSubjectStudy.setConsentToUseData(subject.getConsentToUseData());
				linkSubjectStudy.setConsentToShareData(subject.getConsentToShareData());
				linkSubjectStudy.setConsentToUseBiospecimen(subject.getConsentToUseBiospecimen());
				linkSubjectStudy.setConsentToShareBiospecimen(subject.getConsentToShareBiospecimen());
				linkSubjectStudy.setAgeAtEnrollment(subject.getAgeAtEnrollment());
				linkSubjectStudy.setDateOfEnrollment(subject.getDateOfEnrollment());
				iStudyService.cloneSubjectForSubStudy(linkSubjectStudy);
			}
		}
	}
}
