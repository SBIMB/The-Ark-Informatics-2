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
package au.org.theark.study.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
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
import java.util.Calendar;
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

import com.csvreader.CsvReader;
import com.google.code.jqwicket.api.S.SBuilder;

/**
 * SubjectUploader provides support for uploading subject matrix-formatted files.
 * 
 * @author cellis
 */
public class DataUploader {
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
	public DataUploader(Study study, IArkCommonService iArkCommonService, IStudyService iStudyService) {
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
	@SuppressWarnings("unchecked")
	public StringBuffer uploadAndReportMatrixSubjectFile(InputStream fileInputStream, long inLength, String inFileFormat, char inDelimChr, List<String> uidsWhichNeedUpdating)
			throws FileFormatException, ArkSystemException {
		List<LinkSubjectStudy> insertSubjects = new ArrayList<LinkSubjectStudy>();
		List<LinkSubjectStudy> updateSubjects = new ArrayList<LinkSubjectStudy>();
		List<List> insertChildStudies = new ArrayList<List>();
		long rowCount = 0;
		long subjectCount = 0;
		long insertCount = 0;
		long updateCount = 0;
		long srcLength = -1; // -1 means nothing being processed
		delimiterCharacter = inDelimChr;
		uploadReport = new StringBuffer();

		InputStreamReader inputStreamReader = null;
		CsvReader csvReader = null;
		DecimalFormat decimalFormat = new DecimalFormat("0.00");

		// If Excel, convert to CSV for validation
		if (inFileFormat.equalsIgnoreCase("XLS")) {
			Workbook w;
			try {
				w = Workbook.getWorkbook(fileInputStream);
				delimiterCharacter = ',';
				XLStoCSV xlsToCsv = new XLStoCSV(delimiterCharacter);
				fileInputStream = xlsToCsv.convertXlsToCsv(w);
				fileInputStream.reset();
			}
			catch (BiffException e) {
				log.error(e.getMessage());
			}
			catch (IOException e) {
				log.error(e.getMessage());
			}
		}

		try {
			inputStreamReader = new InputStreamReader(fileInputStream);
			csvReader = new CsvReader(inputStreamReader, delimiterCharacter);
			
			String[] stringLineArray;

			// this is a list of all our somewhat enum-like reference tables...
			// much better to call this once than each one n times in the for loop...plus each ones default is n times
			// should save 200,000-250,000 selects for a 17K insert. may still wish to evaluate whats best here
			Collection<SubjectStatus> subjectStatusPossible = iArkCommonService.getSubjectStatus();
			Collection<GenderType> genderTypesPossible = iArkCommonService.getGenderTypes();
			Collection<EthnicityType> ethnicityTypesPossible =iArkCommonService.getEthnicityTypes();
			YesNo yes = iArkCommonService.getYes();// TODO: boolean
			YesNo no = iArkCommonService.getNo();

			// things inherant... "CONSENT_ID", "CONSENT_STUDY_ID", "CONSENT_LINK_SUBJECT_STUDY_ID",
			// things needed from file... "CONSENT_STUDY_COMP_ID", "CONSENT_STUDY_COMP_STATUS_ID", "CONSENT_CONSENT_STATUS_ID",
			// "CONSENT_CONSENT_TYPE_ID", "CONSENT_CONSENT_DATE",
			// "CONSENT_CONSENTED_BY", "CONSENT_COMMENTS", "CONSENT_REQUESTED_DATE", "CONSENT_RECEIVED_DATE", "CONSENT_COMPLETED_DATE",
			// "CONSENT_CONSENT_DOWNLOADED_ID"?
			/*
			 * int consentComponentIndex = csvReader.getIndex("CONSENT_STUDY_COMP_ID"); int consentComponentStatusIndex =
			 * csvReader.getIndex("CONSENT_STUDY_COMP_STATUS_ID"); int consentStatusIndex = csvReader.getIndex("CONSENT_CONSENT_STATUS_ID"); int
			 * consentTypeIndex = csvReader.getIndex("CONSENT_CONSENT_TYPE_ID"); int consentDateIndex = csvReader.getIndex("CONSENT_CONSENT_DATE"); int
			 * consentByIndex = csvReader.getIndex("CONSENT_CONSENTED_BY"); int consentCommentsIndex = csvReader.getIndex("CONSENT_COMMENTS"); int
			 * consentRequestedDateIndex = csvReader.getIndex("CONSENT_REQUESTED_DATE"); int consentReceivedDateIndex =
			 * csvReader.getIndex("CONSENT_RECEIVED_DATE"); int consentCompletedDateIndex = csvReader.getIndex("CONSENT_COMPLETED_DATE"); //???
			 * "CONSENT_CONSENT_DOWNLOADED_ID";
			 * 
			 * 
			 * 
			 * 
			 * "CONSENT_TO_ACTIVE_CONTACT_ID", "CONSENT_TO_PASSIVE_DATA_GATHERING_ID", "CONSENT_TO_USE_DATA_ID", "CONSENT_STATUS_ID", "CONSENT_TYPE_ID",
			 * "CONSENT_DATE", "CONSENT_DOWNLOADED" consent_option c_o c_o c_status c_type date study.yes_no each of these appears to have a link to
			 * consent_option table which has a number 1-6 for id each representing a status...there appears to be no default...therefore may need
			 * validation
			 * 
			 * the diffrence being consent statuys and consent type link to consent_status and consent_type tables...so look ups and validation needed
			 */

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
			}
			else {
				allSubjectWhichWillBeUpdated = new ArrayList();
			}

			srcLength = inLength;
			if (srcLength <= 0) {
				uploadReport.append("ERROR:  The input size was not greater than 0. Actual length reported: ");
				uploadReport.append(srcLength);
				uploadReport.append("\n");
				throw new FileFormatException("The input size was not greater than 0. Actual length reported: " + srcLength);
			}
			
			csvReader.readHeaders();
			
			srcLength = inLength - csvReader.getHeaders().toString().length();
			log.info(srcLength + " " + csvReader.getHeaders().toString().length());
			for(int i=0; i < 14; i++)
				log.info("Header : " + csvReader.getHeader(i));
						
			int commentsIndex = csvReader.getIndex("COMMENTS");
			int statusIndex = csvReader.getIndex("SUBJECT_STATUS");
			int ageIndex = csvReader.getIndex("AGE_AT_ENROLLMENT"); 
			int doeIndex = csvReader.getIndex("DATE_OF_ENROLLMENT");
			int ethncityIndex = csvReader.getIndex("ETHNICITY");
			int genderIndex = ((csvReader.getIndex("GENDER_TYPE") > 0) ? csvReader.getIndex("GENDER_TYPE") : ((csvReader.getIndex("GENDER") > 0) ? csvReader.getIndex("GENDER") : ((csvReader
					.getIndex("SEX") > 0) ? csvReader.getIndex("SEX") : -1)));
			int subStudiesIndex = csvReader.getIndex("SUBSTUDYS");

			boolean isAutoGen = study.getAutoGenerateSubjectUid();

			while (csvReader.readRecord()) {
				rowCount++;
				LinkSubjectStudy subject = null;

				// Hack to ensure XLS rows contain all cells (ie empty strings for cells after the right-hand non-null value
				List<String> stringList = new ArrayList<String>(csvReader.getHeaders().length);
				for (int i = 0; i < csvReader.getHeaders().length; i++) {
					stringList.add(csvReader.get(i));
					//log.info(csvReader.getHeader(i) + ", ");
				}
								
				stringLineArray = stringList.toArray(new String[csvReader.getHeaders().length]);
				
				String subjectUID = stringLineArray[0];

				boolean hasSomeData = false;
				for (String next : stringLineArray) {
					if (next != null && !next.isEmpty()) {
						hasSomeData = true;
					}
				}

				if (!isAutoGen && (subjectUID == null || subjectUID.isEmpty())) {
					if (!hasSomeData) {
						uploadReport.append("Warning/Info: Row " + rowCount + ":  There appeared to be no data on this row, so we ignored this line");
					}
					else {
						// THIS SHOULD NEVER EVER HAPPEN IF VALIDATION IS CORRECT
						uploadReport.append("Error: Row " + rowCount + ":  There is no subject UID on this row, "
								+ "yet the study is not set up to auto generate subject UIDs.  This line was ignored.  Please remove this line or provide an ID");
					}
				}
				else if (isAutoGen && (subjectUID == null || subjectUID.isEmpty()) && !hasSomeData) {
					uploadReport.append("Warning/Info: Row " + rowCount + ":  There appeared to be no data on this row, so we ignored this line");
				}
				else {

					subject = getSubjectByUIDFromExistList(allSubjectWhichWillBeUpdated, subjectUID);
					boolean thisSubjectAlreadyExists = (subject != null);

					Person person = null;
					if (thisSubjectAlreadyExists) {
						person = subject.getPerson();
					}
					
					else {
						subject = new LinkSubjectStudy();
						log.warn("Subject ID : " + subjectUID);
						subject.setSubjectUID(subjectUID);// note: this will be overwritten IF study.isautogenerate
						subject.setStudy(study);
						person = new Person();
					}

					/*if (firstNameIndex > 0)
						person.setFirstName(stringLineArray[firstNameIndex]);

					if (heardAboutStudyIndex > 0)
						subject.setHeardAboutStudy(stringLineArray[heardAboutStudyIndex]);
*/
					if(subStudiesIndex > 0){
						String str = stringLineArray[subStudiesIndex];
						String[] strArray = str.split(",");
						List<Study> selectedSubStudies = new ArrayList<Study>();
						for(String s : strArray){
							if(iArkCommonService.getStudy(Long.parseLong(s))!=null){
								selectedSubStudies.add(iArkCommonService.getStudy(Long.parseLong(s)));
							}
						}
						insertChildStudies.add(selectedSubStudies);
					}
					
					if (commentsIndex > 0)
						subject.setComment(stringLineArray[commentsIndex]);
					
					if (doeIndex > 0)
						if(stringLineArray[doeIndex]!=""){
							subject.setDateOfEnrollment(simpleDateFormat.parse(stringLineArray[doeIndex]));
						}else{
							subject.setDateOfEnrollment(simpleDateFormat.parse("1900-01-01"));
						}

					if (ageIndex > 0)
						if(stringLineArray[ageIndex]!=""){
							subject.setAgeAtEnrollment(Integer.parseInt(stringLineArray[ageIndex]));
						}else{
							subject.setAgeAtEnrollment(0);
						}
					
					if (genderIndex > 0) {
						if (stringLineArray[genderIndex] != null && stringLineArray[genderIndex].length() > 0) {
							for (GenderType boygirl : genderTypesPossible) {
								if (boygirl.getName().equalsIgnoreCase(stringLineArray[genderIndex])) {
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
						if (stringLineArray[ethncityIndex] != null && stringLineArray[ethncityIndex].length() > 0) {
							for (EthnicityType ethnicity : ethnicityTypesPossible) {
								if (ethnicity.getName().equalsIgnoreCase(stringLineArray[ethncityIndex])) {
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
						String statusStr = (stringLineArray[statusIndex]);
						for (SubjectStatus subjectStat : subjectStatusPossible) {
							if (subjectStat.getName().equalsIgnoreCase(statusStr)) {
								subject.setSubjectStatus(subjectStat);
							}
						}
						
						if (subject.getSubjectStatus() == null || StringUtils.isBlank(subject.getSubjectStatus().getName())) {
							subject.setSubjectStatus(defaultSubjectStatus);
						}

					}
					else {
						subject.setSubjectStatus(defaultSubjectStatus);
					}

					// if the study is autoconsent...then there are some defaults we have to set TODO get rid of hardcoding
					subject.setUpdateConsent(false);
					if (autoConsent && subject.getSubjectStatus().getName().equalsIgnoreCase("Subject")) {
						subject.setConsentDate(Calendar.getInstance().getTime());
						subject.setConsentStatus(consentStatusOfConsented);
						//subject.setConsentType(consentTypeOfElectronic);

						ConsentOption defaultConsentOption = concentOptionOfYes;
						//subject.setConsentToActiveContact(defaultConsentOption);
						//subject.setConsentToPassiveDataGathering(defaultConsentOption);
						subject.setConsentToUseData(defaultConsentOption);
						subject.setConsentToUseData(defaultConsentOption);
						subject.setConsentToUseBiospecimen(defaultConsentOption);
						subject.setConsentToShareData(defaultConsentOption);
						subject.setConsentToShareBiospecimen(defaultConsentOption);
					}
					else {
						// Manual Consent details
						String consentDate = csvReader.get("CONSENT_DATE");
						String consentStatusStr = csvReader.get("CONSENT_STATUS");
						//String consentTypeStr = csvReader.get("CONSENT_TYPE");
						//String passiveDataStr = csvReader.get("CONSENT_TO_PASSIVE_DATA_GATHERING");
						//String activeContactStr = csvReader.get("CONSENT_TO_ACTIVE_CONTACT");
						String useDataStr = csvReader.get("CONSENT_TO_USE_DATA");
						String useBioStr = csvReader.get("CONSENT_TO_USE_BIOSPECIMEN");
						String shareDataStr = csvReader.get("CONSENT_TO_SHARE_DATA");
						String shareBioStr = csvReader.get("CONSENT_TO_SHARE_BIOSPECIMEN");

						if (!consentDate.isEmpty() || !consentStatusStr.isEmpty() || !useDataStr.isEmpty() || !useBioStr.isEmpty() || !shareDataStr.isEmpty() || !shareBioStr.isEmpty()) {
							LinkSubjectStudy newSubject = new LinkSubjectStudy();

							if (!consentDate.isEmpty() || consentDate != "" ) {
								newSubject.setConsentDate(simpleDateFormat.parse(consentDate));
							}else{
								newSubject.setConsentDate(simpleDateFormat.parse("1900-01-01"));
							}

							if (!consentStatusStr.isEmpty()) {
								for (ConsentStatus consentStatus : consentStatusPossible) {
									if (consentStatus.getName().equalsIgnoreCase(consentStatusStr)) {
										newSubject.setConsentStatus(consentStatus);
									}
								}
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
									/*subject.setConsentType(newSubject.getConsentType());
									subject.setConsentToPassiveDataGathering(newSubject.getConsentToPassiveDataGathering());
									subject.setConsentToActiveContact(newSubject.getConsentToActiveContact());*/
									subject.setConsentToUseData(newSubject.getConsentToUseData());
									subject.setConsentToUseBiospecimen(newSubject.getConsentToUseBiospecimen());
									subject.setConsentToShareData(newSubject.getConsentToShareData());
									subject.setConsentToShareBiospecimen(newSubject.getConsentToShareBiospecimen());
								}
								else {
									subject.setUpdateConsent(false);
								}
							}
							else {
								// New Subject with consent details
								subject.setConsentDate(newSubject.getConsentDate());
								subject.setConsentStatus(newSubject.getConsentStatus());
								/*subject.setConsentType(newSubject.getConsentType());
								subject.setConsentToPassiveDataGathering(newSubject.getConsentToPassiveDataGathering());
								subject.setConsentToActiveContact(newSubject.getConsentToActiveContact());*/
								subject.setConsentToUseData(newSubject.getConsentToUseData());
								subject.setConsentToUseBiospecimen(newSubject.getConsentToUseBiospecimen());
								subject.setConsentToShareData(newSubject.getConsentToShareData());
								subject.setConsentToShareBiospecimen(newSubject.getConsentToShareBiospecimen());
							}
						}
					}					
					
					subject.setPerson(person);
					
					log.error(subject.getSubjectUID());

					if (subject.getId() == null || subject.getPerson().getId() == 0) {
						insertSubjects.add(subject);
						
						
																	
						/*
						 * StringBuffer sb = new StringBuffer(); //does this report have to happen? ... and in reality it hasnt had success yet
						 * sb.append("\nCreated subject from original Subject UID: "); sb.append(subject.getSubjectUID());
						 * //sb.append(" has been created successfully and linked to the study: "); //sb.append(study.getName()); //sb.append("\n");
						 * uploadReport.append(sb);
						 */
						insertCount++;
					}
					else {
						// iStudyService.updateSubject(subjectVo);
						updateSubjects.add(subject);
						//log.error("Subject updated" + subject.getSubjectUID());
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
		catch (IOException ioe) {
			uploadReport.append("System Error:   Unexpected I/O exception whilst reading the subject data file\n");
			log.error("processMatrixSubjectFile IOException stacktrace:", ioe);
			throw new ArkSystemException("Unexpected I/O exception whilst reading the subject data file");
		}
		catch (Exception ex) {
			uploadReport.append("System Error:  Unexpected exception whilst reading the subject data file\n");
			log.error("processMatrixSubjectFile Exception stacktrace:", ex);
			throw new ArkSystemException("Unexpected exception occurred when trying to process subject data file");
		}
		finally {
			uploadReport.append("Total file size: ");
			uploadReport.append(decimalFormat.format(inLength / 1024.0 / 1024.0));
			uploadReport.append(" MB");
			uploadReport.append("\n");

			if (csvReader != null) {
				try {
					csvReader.close();
				}
				catch (Exception ex) {
					log.error("Cleanup operation failed: csvRdr.close()", ex);
				}
			}
			if (inputStreamReader != null) {
				try {
					inputStreamReader.close();
				}
				catch (Exception ex) {
					log.error("Cleanup operation failed: isr.close()", ex);
				}
			}
			// Restore the state of variables
			srcLength = -1;
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
				log.info("Person ID : " + subject.getSubjectUID());
				assignChildStudies(subject, insertChildStudies.get(insertSubjects.indexOf(subject)));
			}
		}

		return uploadReport;
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
				log.info("Person ID : " + subject.getSubjectUID());
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
	
	private State findStateWithinThisCountry(String stateString, Country country) {
		if (stateString != null && !StringUtils.isBlank(stateString)) {
			for (State state : country.getStates()) {
				if (state.getName().equalsIgnoreCase(stateString)) {
					return state;
				}
			}
		}
		return null;
	}

	private Country findCountry(List<Country> countriesPossible, String countryString) {
		if (countryString != null && !StringUtils.isBlank(countryString)) {
			for (Country country : countriesPossible) {
				if (country.getName().equalsIgnoreCase(countryString)) {
					return country;
				}
			}
		}
		return null;
	}

	/*
	 * private AddressStatus findAddressStatusOrSetDefault(List<AddressStatus> StatiiAlreadyExisting, AddressStatus defaultAddressStatus, String
	 * stringRepresentingTheStatusWeWant) { if(stringRepresentingTheStatusWeWant!=null && !StringUtils.isBlank(stringRepresentingTheStatusWeWant)){
	 * for(AddressStatus nextStatus : StatiiAlreadyExisting){ if(nextStatus.getName().equalsIgnoreCase(stringRepresentingTheStatusWeWant)){ return
	 * nextStatus; } } } return defaultAddressStatus; }
	 */

	private AddressStatus findAddressStatus(List<AddressStatus> StatiiAlreadyExisting, String stringRepresentingTheStatusWeWant) {
		if (stringRepresentingTheStatusWeWant != null && !StringUtils.isBlank(stringRepresentingTheStatusWeWant)) {
			for (AddressStatus nextStatus : StatiiAlreadyExisting) {
				if (nextStatus.getName().equalsIgnoreCase(stringRepresentingTheStatusWeWant)) {
					return nextStatus;
				}
			}
		}
		return null;
	}

	/*
	 * private AddressType findAddressTypeOrSetDefault(List<AddressType> typesAlreadyExisting, AddressType defaultAddressType, String
	 * stringRepresentingTheTypeWeWant) { if(stringRepresentingTheTypeWeWant!=null && !StringUtils.isBlank(stringRepresentingTheTypeWeWant)){
	 * for(AddressType nextType : typesAlreadyExisting){ if(nextType.getName().equalsIgnoreCase(stringRepresentingTheTypeWeWant)){ return nextType; } }
	 * } return defaultAddressType; }
	 */

	private AddressType findAddressType(List<AddressType> typesAlreadyExisting, String stringRepresentingTheTypeWeWant) {
		if (stringRepresentingTheTypeWeWant != null && !StringUtils.isBlank(stringRepresentingTheTypeWeWant)) {
			for (AddressType nextType : typesAlreadyExisting) {
				if (nextType.getName().equalsIgnoreCase(stringRepresentingTheTypeWeWant)) {
					return nextType;
				}
			}
		}
		return null;
	}

	private PhoneStatus findPhoneStatusOrSetDefault(List<PhoneStatus> StatiiAlreadyExisting, PhoneStatus defaultPhoneStatus, String stringRepresentingTheStatusWeWant) {
		if (stringRepresentingTheStatusWeWant != null && !StringUtils.isBlank(stringRepresentingTheStatusWeWant)) {
			for (PhoneStatus nextStatus : StatiiAlreadyExisting) {
				if (nextStatus.getName().equalsIgnoreCase(stringRepresentingTheStatusWeWant)) {
					return nextStatus;
				}
			}
		}
		return defaultPhoneStatus;
	}

	private PhoneType findPhoneTypeOrSetDefault(List<PhoneType> typesAlreadyExisting, PhoneType defaultPhoneType, String stringRepresentingTheTypeWeWant) {
		if (stringRepresentingTheTypeWeWant != null && !StringUtils.isBlank(stringRepresentingTheTypeWeWant)) {
			for (PhoneType nextType : typesAlreadyExisting) {
				if (nextType.getName().equalsIgnoreCase(stringRepresentingTheTypeWeWant)) {
					return nextType;
				}
			}
		}
		return defaultPhoneType;
	}

	@SuppressWarnings("unchecked")
	public StringBuffer uploadAndReportCustomDataFile(InputStream inputStream, long size, String fileFormat, char delimChar, List<String> listOfUIDsToUpdate) throws FileFormatException,
			ArkSystemException {
		List<SubjectCustomFieldData> customFieldsToUpdate = new ArrayList<SubjectCustomFieldData>();
		List<SubjectCustomFieldData> customFieldsToInsert = new ArrayList<SubjectCustomFieldData>();
		delimiterCharacter = delimChar;
		uploadReport = new StringBuffer();

		InputStreamReader inputStreamReader = null;
		CsvReader csvReader = null;
		DecimalFormat decimalFormat = new DecimalFormat("0.00");
		
		if (fileFormat.equalsIgnoreCase("XLS")) {
			Workbook w;
			try {
				w = Workbook.getWorkbook(inputStream);
				delimiterCharacter = ',';
				XLStoCSV xlsToCsv = new XLStoCSV(delimiterCharacter);
				inputStream = xlsToCsv.convertXlsToCsv(w);
				inputStream.reset();
			}
			catch (BiffException e) {
				log.error(e.getMessage());
			}
			catch (IOException e) {
				log.error(e.getMessage());
			}
		}
		

		int subjectCount = 0;
		long updateFieldsCount = 0L;
		long insertFieldsCount = 0L;
		long emptyDataCount = 0L;
		try {
			inputStreamReader = new InputStreamReader(inputStream);
			csvReader = new CsvReader(inputStreamReader, delimiterCharacter);
			String[] stringLineArray;

			List<LinkSubjectStudy> allSubjectWhichWillBeUpdated = null;
			if (listOfUIDsToUpdate.size() > 0) {
				allSubjectWhichWillBeUpdated = iArkCommonService.getUniqueSubjectsWithTheseUIDs(study, listOfUIDsToUpdate);
			}
			else {
				allSubjectWhichWillBeUpdated = new ArrayList<LinkSubjectStudy>(0);
			}
			if (size <= 0) {
				uploadReport.append("ERROR:  The input size was not greater than 0. Actual length reported: ");
				uploadReport.append(size);
				uploadReport.append("\n");
				throw new FileFormatException("The input size was not greater than 0. Actual length reported: " + size);
			}

			csvReader.readHeaders();

			List<String> fieldNameCollection = Arrays.asList(csvReader.getHeaders());
			ArkFunction subjectCustomFieldArkFunction = iArkCommonService.getArkFunctionByName(Constants.FUNCTION_KEY_VALUE_SUBJECT_CUSTOM_FIELD);// ");

			List<CustomFieldDisplay> cfdsThatWeNeed = iArkCommonService.getCustomFieldDisplaysIn(fieldNameCollection, study, subjectCustomFieldArkFunction);
			List<SubjectCustomFieldData> dataThatWeHave = iArkCommonService.getCustomFieldDataFor(cfdsThatWeNeed, allSubjectWhichWillBeUpdated);
			// read one line which contains potentially many custom fields
			while (csvReader.readRecord()) {
				log.info("reading record " + subjectCount);

				stringLineArray = csvReader.getValues();
				String subjectUID = stringLineArray[0];
				LinkSubjectStudy subject = getSubjectByUIDFromExistList(allSubjectWhichWillBeUpdated, subjectUID);
				// log.info("get subject from list");
				CustomField customField = null;
				for (CustomFieldDisplay cfd : cfdsThatWeNeed) {
					customField = cfd.getCustomField();
					// log.info("got customfield from cfd");
					SubjectCustomFieldData dataInDB = getCustomFieldFromList(dataThatWeHave, subjectUID, cfd);
					// log.info("got 'data in db' from cfd, subject and ALL data");
					String theDataAsString = csvReader.get(cfd.getCustomField().getName());
					// log.info("read data from file");

					if (theDataAsString != null && !theDataAsString.isEmpty()) {
						if (dataInDB != null) {
							dataInDB = setValue(customField, cfd, dataInDB, theDataAsString);
							// log.info("have set value to entity");
							customFieldsToUpdate.add(dataInDB);
							// log.info("added entity to list");
							updateFieldsCount++;
						}
						else {
							SubjectCustomFieldData dataToInsert = new SubjectCustomFieldData();
							dataToInsert.setCustomFieldDisplay(cfd);
							dataToInsert.setLinkSubjectStudy(subject);
							setValue(customField, cfd, dataToInsert, theDataAsString);
							customFieldsToInsert.add(dataToInsert);
							insertFieldsCount++;
						}
					}
					else {
						emptyDataCount++;
					}
				}

				subjectCount++;
			}
			log.info("finished message for " + subjectCount + "         updates= " + updateFieldsCount + " or \ncustomFieldsToupdate.size=" + customFieldsToUpdate.size() + "\n     inserts = "
					+ insertFieldsCount + "  or  \ncustomFieldsToInsert.size = " + customFieldsToInsert.size() + "   amount of empty scells =" + emptyDataCount);
		}
		catch (IOException ioe) {
			uploadReport.append("SYSTEM ERROR:   Unexpected I/O exception whilst reading the subject data file\n");
			log.error("processMatrixSubjectFile IOException stacktrace:", ioe);
			throw new ArkSystemException("Unexpected I/O exception whilst reading the subject data file");
		}
		catch (Exception ex) {
			uploadReport.append("SYSTEM ERROR:   Unexpected exception whilst reading the subject data file\n");
			log.error("processMatrixSubjectFile Exception stacktrace:", ex);
			throw new ArkSystemException("Unexpected exception occurred when trying to process subject data file");
		}
		finally {
			uploadReport.append("Total file size: ");
			uploadReport.append(decimalFormat.format(size / 1024.0 / 1024.0));
			uploadReport.append(" MB");
			uploadReport.append("\n");

			if (csvReader != null) {
				try {
					csvReader.close();
				}
				catch (Exception ex) {
					log.error("Cleanup operation failed: csvRdr.close()", ex);
				}
			}
			if (inputStreamReader != null) {
				try {
					inputStreamReader.close();
				}
				catch (Exception ex) {
					log.error("Cleanup operation failed: isr.close()", ex);
				}
			}

		}

		uploadReport.append("Inserted ");
		uploadReport.append(subjectCount);
		uploadReport.append(" rows of data");
		uploadReport.append("\n");

		uploadReport.append(insertFieldsCount);
		uploadReport.append(" fields were inserted.");
		uploadReport.append("\n");
		uploadReport.append(updateFieldsCount);
		uploadReport.append(" fields were updated.");
		uploadReport.append("\n");

		// TODO better exceptionhandling
		iStudyService.processFieldsBatch(customFieldsToUpdate, study, customFieldsToInsert);
		return uploadReport;
	}

	private SubjectCustomFieldData setValue(CustomField customField, CustomFieldDisplay customFieldDisplay, SubjectCustomFieldData data, String theDataAsString) {
		// Rerun the validation check to determine if "invalid" data was in fact ignored and forcibly set to be loaded in
		boolean isValidData = CustomFieldUploadValidator.validateFieldData(customField, theDataAsString, "", new ArrayList<String>(0), customFieldDisplay.getAllowMultiselect());

		if (customField.getFieldType().getName().equalsIgnoreCase(Constants.FIELD_TYPE_NUMBER)) {
			try {
				if (isValidData) {
					data.setNumberDataValue(new Double(theDataAsString));
				}
				else {
					if (data.getId() != null) {
						data.setNumberDataValue(null);
					}
					data.setErrorDataValue(theDataAsString);
				}
			}
			catch (NumberFormatException nfe) {
				if (data.getId() != null) {
					data.setNumberDataValue(null);
				}
				data.setErrorDataValue(theDataAsString);
			}
		}
		else if (customField.getFieldType().getName().equalsIgnoreCase(Constants.FIELD_TYPE_DATE)) {
			DateFormat dateFormat = new SimpleDateFormat(au.org.theark.core.Constants.DD_MM_YYYY);
			Date dateFieldValue;
			try {
				dateFieldValue = dateFormat.parse(theDataAsString);
				if (isValidData) {
					data.setDateDataValue(dateFieldValue);
					if (data.getId() != null) {
						data.setErrorDataValue(null);
					}
				}
				else {
					if (data.getId() != null) {
						data.setDateDataValue(null);
					}
					data.setErrorDataValue(theDataAsString);
				}
			}
			catch (ParseException e) {
				if (data.getId() != null) {
					data.setDateDataValue(null);
				}
				data.setErrorDataValue(theDataAsString);
			}
		}
		else if (customField.getFieldType().getName().equalsIgnoreCase(Constants.FIELD_TYPE_CHARACTER)) {
			if (customField.getEncodedValues() != null && !customField.getEncodedValues().isEmpty() && customFieldDisplay.getAllowMultiselect()) {
				if (theDataAsString != null) {
					theDataAsString = theDataAsString.replaceAll(" ", ";");
				}
			}
			if (isValidData) {
				data.setTextDataValue(theDataAsString);
			}
			else {
				if (data.getId() != null) {
					data.setTextDataValue(null);
				}
				data.setErrorDataValue(theDataAsString);
			}
		}
		return data;
	}

	private SubjectCustomFieldData getCustomFieldFromList(List<SubjectCustomFieldData> dataThatWeHave, String subjectUID, CustomFieldDisplay cfd) {
		for (SubjectCustomFieldData data : dataThatWeHave) {
			// TODO ASAP return to ignores case?
			if (data.getLinkSubjectStudy().getSubjectUID().equals(subjectUID) && data.getCustomFieldDisplay().getId().equals(cfd.getId())) {
				dataThatWeHave.remove(data); // TODO test this: but it seems to have drastically reduced the exponential nature of our upload by the final
														// lookups becoming faster rather than exponentially slower
				return data;
			}
		}
		return null;
	}

	public StringBuffer uploadAndReportSubjectConsentDataFile(InputStream inputStream, long size, String fileFormat, char delimChar) throws FileFormatException, ArkSystemException {
		uploadReport = new StringBuffer();
		long rowCount = 0;
		long insertFieldsCount = 0;
		long updateFieldsCount = 0;
		List<Consent> consentFieldsToUpdate = new ArrayList<Consent>();
		List<Consent> consentFieldsToInsert = new ArrayList<Consent>();
		delimiterCharacter = delimChar;

		InputStreamReader inputStreamReader = null;
		CsvReader csvReader = null;
		DecimalFormat decimalFormat = new DecimalFormat("0.00");
		
		if (fileFormat.equalsIgnoreCase("XLS")) {
			Workbook w;
			try {
				w = Workbook.getWorkbook(inputStream);
				delimiterCharacter = ',';
				XLStoCSV xlsToCsv = new XLStoCSV(delimiterCharacter);
				inputStream = xlsToCsv.convertXlsToCsv(w);
				inputStream.reset();
			}
			catch (BiffException e) {
				log.error(e.getMessage());
			}
			catch (IOException e) {
				log.error(e.getMessage());
			}
		}

		try {
			inputStreamReader = new InputStreamReader(inputStream);
			csvReader = new CsvReader(inputStreamReader, delimiterCharacter);
			csvReader.readHeaders();
			String[] stringLineArray;

			List<StudyComp> studyComList = iArkCommonService.getStudyComponentByStudy(study);
			Map<String, StudyComp> studyCompMap = new HashMap<String, StudyComp>();
			for (StudyComp studuComp : studyComList) {
				studyCompMap.put(studuComp.getName().toUpperCase(), studuComp);
			}

			List<StudyCompStatus> studyCompStatusList = iArkCommonService.getStudyComponentStatus();
			Map<String, StudyCompStatus> studyCompStatusMap = new HashMap<String, StudyCompStatus>();
			for (StudyCompStatus studyCompStatus : studyCompStatusList) {
				studyCompStatusMap.put(studyCompStatus.getName().toUpperCase(), studyCompStatus);
			}

			List<ConsentType> consentTypeList = iArkCommonService.getConsentType();
			Map<String, ConsentType> consentTypeMap = new HashMap<String, ConsentType>();
			for (ConsentType consentType : consentTypeList) {
				consentTypeMap.put(consentType.getName().toUpperCase(), consentType);
			}

			List<ConsentStatus> consentStatusList = iArkCommonService.getConsentStatus();
			Map<String, ConsentStatus> consentStatusMap = new HashMap<String, ConsentStatus>();
			for (ConsentStatus consentStatus : consentStatusList) {
				consentStatusMap.put(consentStatus.getName().toUpperCase(), consentStatus);
			}

			List<YesNo> consentDownloadedList = iArkCommonService.getYesNoList();
			Map<String, YesNo> consentDownloadedMap = new HashMap<String, YesNo>();
			for (YesNo consentDownloaded : consentDownloadedList) {
				consentDownloadedMap.put(consentDownloaded.getName().toUpperCase(), consentDownloaded);
			}

			ConsentVO consentVO = new ConsentVO();
			consentVO.getConsent().setStudy(study);
			int subjectUidIndex = csvReader.getIndex("SUBJECTUID");
			int studyComponentIndex = csvReader.getIndex("STUDY_COMPONENT");
			int studyComponentStatusIndex = csvReader.getIndex("STUDY_COMPONENT_STATUS");
			int consentTypeIndex = csvReader.getIndex("CONSENT_TYPE");
			int consentStatusIndex = csvReader.getIndex("CONSENT_STATUS");
			int consentDownloadedIndex = csvReader.getIndex("CONSENT_DOWNLOADED");
			int consentedByIndex = csvReader.getIndex("CONSENTED_BY");
			int consentDateIndex = csvReader.getIndex("CONSENT_DATE");
			int commentIndex = csvReader.getIndex("COMMENT");
			int completedDateIndex = csvReader.getIndex("COMPLETED_DATE");

			while (csvReader.readRecord()) {
				++rowCount;
				stringLineArray = csvReader.getValues();
				String subjectUID = stringLineArray[subjectUidIndex];
				LinkSubjectStudy subject = iArkCommonService.getSubjectByUID(subjectUID, study);

				consentVO.getConsent().setLinkSubjectStudy(subject);
				consentVO.getConsent().setStudyComp(studyCompMap.get(stringLineArray[studyComponentIndex].toUpperCase()));

				List<Consent> existingConcentList = iStudyService.searchConsent(consentVO);

				if (existingConcentList.size() > 0) {
					++updateFieldsCount;
					Consent existingConsent = existingConcentList.get(0);
					existingConsent.setStudyComponentStatus(studyCompStatusMap.get(stringLineArray[studyComponentStatusIndex].toUpperCase()));
					existingConsent.setConsentType(consentTypeMap.get(stringLineArray[consentTypeIndex].toUpperCase()));
					existingConsent.setConsentStatus(consentStatusMap.get(stringLineArray[consentStatusIndex].toUpperCase()));
					existingConsent.setConsentDownloaded(consentDownloadedMap.get(stringLineArray[consentDownloadedIndex].toUpperCase()));

					if (stringLineArray.length > consentedByIndex) {
						existingConsent.setConsentedBy(stringLineArray[consentedByIndex]);
					}

					if (stringLineArray.length > consentDateIndex) {
						String consentDate = stringLineArray[consentDateIndex];
						if (consentDate != null && consentDate.trim().length() > 0) {
							existingConsent.setConsentDate(simpleDateFormat.parse(consentDate));
						}
					}

					if (stringLineArray.length > commentIndex) {
						existingConsent.setComments(stringLineArray[commentIndex]);
					}

					if ("Completed".equalsIgnoreCase(existingConsent.getStudyComponentStatus().getName())) {
						try {
							existingConsent.setCompletedDate(simpleDateFormat.parse(stringLineArray[completedDateIndex]));
						}
						catch (Exception e) {
							existingConsent.setCompletedDate(null);
						}
					}
					else {
						existingConsent.setCompletedDate(null);
					}
					consentFieldsToUpdate.add(existingConsent);
				}
				else {
					++insertFieldsCount;
					Consent consent = new Consent();
					consent.setStudy(study);
					consent.setLinkSubjectStudy(subject);
					consent.setStudyComp(studyCompMap.get(stringLineArray[studyComponentIndex].toUpperCase().trim()));
					consent.setStudyComponentStatus(studyCompStatusMap.get(stringLineArray[studyComponentStatusIndex].toUpperCase().trim()));
					consent.setConsentType(consentTypeMap.get(stringLineArray[consentTypeIndex].toUpperCase().trim()));
					consent.setConsentStatus(consentStatusMap.get(stringLineArray[consentStatusIndex].toUpperCase().trim()));
					consent.setConsentDownloaded(consentDownloadedMap.get(stringLineArray[consentDownloadedIndex].toUpperCase().trim()));

					if (stringLineArray.length > consentedByIndex) {
						consent.setConsentedBy(stringLineArray[consentedByIndex]);
					}

					if (stringLineArray.length > consentDateIndex) {
						String consentDate = stringLineArray[consentDateIndex].trim();
						if (consentDate != null && consentDate.trim().length() > 0) {
							try {
								consent.setConsentDate(simpleDateFormat.parse(consentDate));
							}
							catch (Exception e) {
								consent.setConsentDate(simpleDateFormat.parse(null));
							}
						}
					}

					if (stringLineArray.length > commentIndex) {
						consent.setComments(stringLineArray[commentIndex].trim());
					}

					if ("Completed".equalsIgnoreCase(consent.getStudyComponentStatus().getName())) {
						try {
							consent.setCompletedDate(simpleDateFormat.parse(stringLineArray[completedDateIndex].trim()));
						}
						catch (Exception e) {
							consent.setCompletedDate(null);
						}
					}
					consentFieldsToInsert.add(consent);
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new ArkSystemException(e.getMessage());
		}
		finally {
			uploadReport.append("Total file size: ");
			uploadReport.append(decimalFormat.format(size / 1024.0 / 1024.0));
			uploadReport.append(" MB");
			uploadReport.append("\n");

			if (csvReader != null) {
				try {
					csvReader.close();
				}
				catch (Exception ex) {
					log.error("Cleanup operation failed: csvRdr.close()", ex);
				}
			}
			if (inputStreamReader != null) {
				try {
					inputStreamReader.close();
				}
				catch (Exception ex) {
					log.error("Cleanup operation failed: isr.close()", ex);
				}
			}

		}

		uploadReport.append("Process ");
		uploadReport.append(rowCount);
		uploadReport.append(" rows of data");
		uploadReport.append("\n");

		uploadReport.append(insertFieldsCount);
		uploadReport.append(" fields were inserted.");
		uploadReport.append("\n");
		uploadReport.append(updateFieldsCount);
		uploadReport.append(" fields were updated.");
		uploadReport.append("\n");

		try {
			iStudyService.processSubjectConsentBatch(consentFieldsToUpdate, consentFieldsToInsert);
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new ArkSystemException(e.getMessage());
		}

		return uploadReport;
	}

	public StringBuffer uploadAndReportPedigreeDataFile(InputStream inputStream, long size, String fileFormat, char delimChar) throws FileFormatException, ArkSystemException {
		uploadReport = new StringBuffer();
		long rowCount = 0;
		long insertFieldsCount = 0;
		long updateFieldsCount = 0;

		List<LinkSubjectPedigree> parentSubjectLinkRelationships = new ArrayList<LinkSubjectPedigree>();

		List<LinkSubjectTwin> twinSubjectLinkRelationships = new ArrayList<LinkSubjectTwin>();

		delimiterCharacter = delimChar;

		InputStreamReader inputStreamReader = null;
		CsvReader csvReader = null;
		DecimalFormat decimalFormat = new DecimalFormat("0.00");

		try {
			inputStreamReader = new InputStreamReader(inputStream);
			csvReader = new CsvReader(inputStreamReader, delimiterCharacter);
			// csvReader.readHeaders();
			String[] stringLineArray;

			List<Relationship> familyRelationshipList = iArkCommonService.getFamilyRelationships();
			HashMap<String, Relationship> familyRelationshipMap = new HashMap<String, Relationship>();
			for (Relationship relationship : familyRelationshipList) {
				familyRelationshipMap.put(relationship.getName(), relationship);
			}

			List<TwinType> twinRelationshipList = iStudyService.getTwinTypes();
			HashMap<String, TwinType> twinRelationshipMap = new HashMap<String, TwinType>();
			for (TwinType type : twinRelationshipList) {
				twinRelationshipMap.put(type.getName(), type);
			}

			while (csvReader.readRecord()) {
				++rowCount;
				int index = 0;
				stringLineArray = csvReader.getValues();
				String subjectUID = stringLineArray[index++];
				String fatherUID = stringLineArray[index++];
				String motherUID = stringLineArray[index++];
				String twinStatus = stringLineArray[index++];
				String twinUID = stringLineArray[index++];

				LinkSubjectStudy subjectUser = iArkCommonService.getSubjectByUID(subjectUID, study);

				if (fatherUID != null && !fatherUID.equalsIgnoreCase("-")) {
					LinkSubjectPedigree father = new LinkSubjectPedigree();
					father.setSubject(subjectUser);
					father.setRelationship(familyRelationshipMap.get("Father"));
					LinkSubjectStudy fatherUser = iArkCommonService.getSubjectByUID(fatherUID, study);
					father.setRelative(fatherUser);
					parentSubjectLinkRelationships.add(father);
				}

				if (motherUID != null && !motherUID.equalsIgnoreCase("-")) {
					LinkSubjectPedigree mother = new LinkSubjectPedigree();
					mother.setSubject(subjectUser);
					mother.setRelationship(familyRelationshipMap.get("Mother"));
					LinkSubjectStudy motherUser = iArkCommonService.getSubjectByUID(motherUID, study);
					mother.setRelative(motherUser);
					parentSubjectLinkRelationships.add(mother);
				}

				if (twinStatus != null && !twinStatus.equalsIgnoreCase("-") && !isTwinRelationshipExists(twinSubjectLinkRelationships, subjectUID, twinUID)) {
					LinkSubjectTwin twin = new LinkSubjectTwin();
					if ("M".equalsIgnoreCase(twinStatus)) {
						twin.setTwinType(twinRelationshipMap.get("MZ"));
					}
					else {
						twin.setTwinType(twinRelationshipMap.get("DZ"));
					}
					twin.setFirstSubject(subjectUser);
					LinkSubjectStudy siblingUser = iArkCommonService.getSubjectByUID(twinUID, study);
					twin.setSecondSubject(siblingUser);
					twinSubjectLinkRelationships.add(twin);
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new ArkSystemException(e.getMessage());
		}
		finally {
			uploadReport.append("Total file size: ");
			uploadReport.append(decimalFormat.format(size / 1024.0 / 1024.0));
			uploadReport.append(" MB");
			uploadReport.append("\n");

			if (csvReader != null) {
				try {
					csvReader.close();
				}
				catch (Exception ex) {
					log.error("Cleanup operation failed: csvRdr.close()", ex);
				}
			}
			if (inputStreamReader != null) {
				try {
					inputStreamReader.close();
				}
				catch (Exception ex) {
					log.error("Cleanup operation failed: isr.close()", ex);
				}
			}
		}

		uploadReport.append("Process ");
		uploadReport.append(rowCount);
		uploadReport.append(" rows of data");
		uploadReport.append("\n");

		uploadReport.append(insertFieldsCount);
		uploadReport.append(" fields were inserted.");
		uploadReport.append("\n");
		uploadReport.append(updateFieldsCount);
		uploadReport.append(" fields were updated.");
		uploadReport.append("\n");

		try {
			iStudyService.processPedigreeBatch(parentSubjectLinkRelationships, twinSubjectLinkRelationships);
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new ArkSystemException(e.getMessage());
		}

		return uploadReport;
	}

	private boolean isTwinRelationshipExists(final List<LinkSubjectTwin> twinSubjectLinkRelationships, final String subjectUid1, final String subjectUid2) {
		boolean exists = false;
		LinkSubjectStudy firstSubject = null;
		LinkSubjectStudy secondSubject = null;
		for (LinkSubjectTwin twin : twinSubjectLinkRelationships) {
			firstSubject = twin.getFirstSubject();
			secondSubject = twin.getSecondSubject();

			if ((firstSubject.getSubjectUID().equalsIgnoreCase(subjectUid1) && secondSubject.getSubjectUID().equalsIgnoreCase(subjectUid2))
					|| (secondSubject.getSubjectUID().equalsIgnoreCase(subjectUid1) && firstSubject.getSubjectUID().equalsIgnoreCase(subjectUid2))) {
				exists = true;
			}
		}
		return exists;
	}
	
	public StringBuffer uploadAndReportSubjectAttachmentDataFile(InputStream inputStream, long size, String fileFormat, char delimChar) throws FileFormatException, ArkSystemException {
		uploadReport = new StringBuffer();
		long rowCount = 0;
		long insertFieldsCount = 0;
		long updateFieldsCount = 0;

		List<SubjectFile> subjectFiles = new ArrayList<SubjectFile>();

		InputStreamReader inputStreamReader = null;
		CsvReader csvReader = null;
		DecimalFormat decimalFormat = new DecimalFormat("0.00");
		delimiterCharacter = delimChar;
		
		if (fileFormat.equalsIgnoreCase("XLS")) {
			Workbook w;
			try {
				w = Workbook.getWorkbook(inputStream);
				delimiterCharacter = ',';
				XLStoCSV xlsToCsv = new XLStoCSV(delimiterCharacter);
				inputStream = xlsToCsv.convertXlsToCsv(w);
				inputStream.reset();
			}
			catch (BiffException e) {
				log.error(e.getMessage());
			}
			catch (IOException e) {
				log.error(e.getMessage());
			}
		}

		try {
			inputStreamReader = new InputStreamReader(inputStream);
			csvReader = new CsvReader(inputStreamReader, delimiterCharacter);
			csvReader.readHeaders();
			String[] stringLineArray;

			int subjectUidIndex = csvReader.getIndex("SUBJECTUID");
			int filePathIndex = csvReader.getIndex("FILE_NAME_WITH_FULL_PATH");
			int studyComponentIndex = csvReader.getIndex("STUDY_COMPONENT");
			int commentIndex = csvReader.getIndex("COMMENT");

			List<StudyComp> studyCompList = iArkCommonService.getStudyComponentByStudy(study);

			while (csvReader.readRecord()) {
				++rowCount;
				stringLineArray = csvReader.getValues();

				SubjectFile subjectFile = new SubjectFile();
				
				String userId = SecurityUtils.getSubject().getPrincipal().toString();
				subjectFile.setUserId(userId);
				
				String subjectUID = stringLineArray[subjectUidIndex];
				String studyCompName = stringLineArray[studyComponentIndex];
				LinkSubjectStudy subject = iArkCommonService.getSubjectByUID(subjectUID, study);
				subjectFile.setLinkSubjectStudy(subject);
				for (StudyComp studyComp : studyCompList) {
					if (studyComp.getName().equals(studyCompName)) {
						subjectFile.setStudyComp(studyComp);
						break;
					}
				}
				subjectFile.setComments(stringLineArray[commentIndex]);

				// File processing

				String sourcePath = stringLineArray[filePathIndex];

				File file = new File(sourcePath);

				subjectFile.setChecksum(iArkCommonService.generateArkFileChecksum(file, "MD5"));
				String fileName = file.getName();
				subjectFile.setFilename(fileName);
				String fileId = iArkCommonService.generateArkFileId(fileName);
				subjectFile.setFileId(fileId);

				String directoryName = iArkCommonService.getArkFileDirName(study.getId(), subjectUID, au.org.theark.study.web.Constants.ARK_SUBJECT_ATTACHEMENT_DIR);
				// TODO need to check directory created successfully
				iArkCommonService.createArkFileAttachmentDirectoy(directoryName);
				String destinationPath = directoryName + File.separator + fileId;
				iArkCommonService.copyArkLargeFileAttachments(sourcePath, destinationPath);

				subjectFiles.add(subjectFile);
			}

		}
		catch (Exception e) {
			e.printStackTrace();
			throw new ArkSystemException(e.getMessage());
		}
		finally {
			uploadReport.append("Total file size: ");
			uploadReport.append(decimalFormat.format(size / 1024.0 / 1024.0));
			uploadReport.append(" MB");
			uploadReport.append("\n");

			if (csvReader != null) {
				try {
					csvReader.close();
				}
				catch (Exception ex) {
					log.error("Cleanup operation failed: csvRdr.close()", ex);
				}
			}
			if (inputStreamReader != null) {
				try {
					inputStreamReader.close();
				}
				catch (Exception ex) {
					log.error("Cleanup operation failed: isr.close()", ex);
				}
			}
		}

		uploadReport.append("Process ");
		uploadReport.append(rowCount);
		uploadReport.append(" rows of data");
		uploadReport.append("\n");

		uploadReport.append(insertFieldsCount);
		uploadReport.append(" fields were inserted.");
		uploadReport.append("\n");
		uploadReport.append(updateFieldsCount);
		uploadReport.append(" fields were updated.");
		uploadReport.append("\n");

		try {
			iStudyService.processSubjectAttachmentBatch(subjectFiles);
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new ArkSystemException(e.getMessage());
		}

		return uploadReport;
	}
}
