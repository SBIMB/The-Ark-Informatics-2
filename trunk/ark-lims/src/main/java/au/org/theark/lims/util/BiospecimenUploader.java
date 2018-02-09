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
package au.org.theark.lims.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jxl.Workbook;
import jxl.read.biff.BiffException;

import org.apache.commons.lang.time.StopWatch;
import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.theark.core.Constants;
import au.org.theark.core.exception.ArkBaseException;
import au.org.theark.core.exception.ArkSystemException;
import au.org.theark.core.exception.FileFormatException;
import au.org.theark.core.model.lims.entity.BioCollection;
import au.org.theark.core.model.lims.entity.BioSampletype;
import au.org.theark.core.model.lims.entity.BioTransaction;
import au.org.theark.core.model.lims.entity.BioTransactionStatus;
import au.org.theark.core.model.lims.entity.Biospecimen;
import au.org.theark.core.model.lims.entity.BiospecimenAnticoagulant;
import au.org.theark.core.model.lims.entity.BiospecimenGrade;
import au.org.theark.core.model.lims.entity.BiospecimenProtocol;
import au.org.theark.core.model.lims.entity.BiospecimenQuality;
import au.org.theark.core.model.lims.entity.BiospecimenStatus;
import au.org.theark.core.model.lims.entity.BiospecimenStorage;
import au.org.theark.core.model.lims.entity.InvCell;
import au.org.theark.core.model.lims.entity.TreatmentType;
import au.org.theark.core.model.lims.entity.Unit;
import au.org.theark.core.model.study.entity.LinkSubjectStudy;
import au.org.theark.core.model.study.entity.Study;
import au.org.theark.core.service.IArkCommonService;
import au.org.theark.core.util.XLStoCSV;
import au.org.theark.lims.service.IInventoryService;
import au.org.theark.lims.service.ILimsService;
import au.org.theark.lims.web.component.biolocation.BioLocationDetailPanel;

import com.csvreader.CsvReader;

/**
 * BiospecimenUploader provides support for uploading biospecimen matrix-formatted files. It features state-machine behaviour to allow an external class to
 * deal with how to store the data pulled out of the files.
 * 
 * @author cellis
 */
public class BiospecimenUploader {
	private long					recordCount;
	private long					insertCount;
	private long					updateCount;
	private long					curPos;
	private long					srcLength				= -1;
	private StopWatch				timer						= null;
	private char					delimiterCharacter	= Constants.DEFAULT_DELIMITER_CHARACTER;
	private Study					study;
	static Logger					log						= LoggerFactory.getLogger(BiospecimenUploader.class);
	@SuppressWarnings("unchecked")
	private IArkCommonService		iArkCommonService		= null;
	private ILimsService			iLimsService			= null;
	private IInventoryService		iInventoryService		= null;
	private StringBuffer			uploadReport			= null;
	private SimpleDateFormat		simpleDateFormat		= new SimpleDateFormat(au.org.theark.core.Constants.DD_MM_YYYY);
	private SimpleDateFormat		simpleTimeFormat		= new SimpleDateFormat(au.org.theark.core.Constants.HH_MM_SS);
	private List<Biospecimen>		insertBiospecimens	= new ArrayList<Biospecimen>();
	private List<Biospecimen>		updateBiospecimens	= new ArrayList<Biospecimen>();
	private List<InvCell>			updateInvCells			= new ArrayList<InvCell>();

	/**
	 * BiospecimenUploader constructor
	 * 
	 * @param study
	 *           study identifier in context
	 * @param iArkCommonService
	 *           common ARK service to perform select/insert/updates to the database
	 * @param iLimsService
	 *           LIMS service to perform select/insert/updates to the LIMS database
	 * @param iInventoryService
	 * 	       LIMS inventory service to perform select/insert/updates to the LIMS database
	 */
	@SuppressWarnings("unchecked")
	public BiospecimenUploader(Study study, IArkCommonService iArkCommonService, ILimsService iLimsService, IInventoryService iInventoryService) {
		this.study = study;
		this.iArkCommonService = iArkCommonService;
		this.iLimsService = iLimsService;
		this.iInventoryService = iInventoryService;
		simpleDateFormat.setLenient(false);
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
	 */
	public StringBuffer uploadAndReportMatrixBiospecimenFile(InputStream fileInputStream, long inLength, String inFileFormat, char inDelimChr) throws FileFormatException, ArkSystemException {
		delimiterCharacter = inDelimChr;
		uploadReport = new StringBuffer();
		curPos = 0;

		InputStreamReader inputStreamReader = null;
		CsvReader csvReader = null;
		DecimalFormat decimalFormat = new DecimalFormat("0.00");
		DateFormat dfTime = new SimpleDateFormat("HH:MM");
		org.apache.shiro.subject.Subject currentUser = SecurityUtils.getSubject();
		
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

			srcLength = inLength;
			if (srcLength <= 0) {
				uploadReport.append("The input size was not greater than 0. Actual length reported: ");
				uploadReport.append(srcLength);
				uploadReport.append("\n");
				throw new FileFormatException("The input size was not greater than 0. Actual length reported: " + srcLength);
			}

			timer = new StopWatch();
			timer.start();

			// Set field list (note 2th column to Nth column)
			// SUBJECTUID BIOSPECIMENUID F1 F2 FN
			// 0 1 2 3 N
			csvReader.readHeaders();

			srcLength = inLength - csvReader.getHeaders().toString().length();
			log.debug("Header length: " + csvReader.getHeaders().toString().length());
						
			// Loop through all rows in file
			while (csvReader.readRecord()) {
				
				log.info("At record: " + recordCount);
				String subjectUID = csvReader.get("SUBJECTUID");
				String biospecimenUID = csvReader.get("BIOSPECIMENUID");

				LinkSubjectStudy linkSubjectStudy = new LinkSubjectStudy();
				//linkSubjectStudy.setStudy(study);

				linkSubjectStudy = iArkCommonService.getSubjectByUIDAndStudy(subjectUID, study);

				//this is validated in prior step and should never happen
				if(linkSubjectStudy==null){
					log.error("\n\n\n\n\n\n\n\n\n\n\n\nUnexpected subject? a shouldnt happen...we should have errored this in validation");
					break;//TODO : log appropriately or do some handling
				}

				Biospecimen biospecimen = iLimsService.getBiospecimenByUid(biospecimenUID,study);
				Biospecimen parentBiospecimen = null;
				Double amtUsed = null;
				BioCollection bioCollection;
				
				if(biospecimen == null) {
					biospecimen = new Biospecimen();
				}
				else{
					log.error("\n\n\n\n\n\n\n\n\n....We should NEVER have existing biospecimens this should be  validated in prior step");
					break;
				}
				
				//PARENTID
				if (csvReader.getIndex("PARENTID") > 0) {
					if(csvReader.get("PARENTID")!=""){
						String parentID = csvReader.get("PARENTID");
					
						log.info("Parent ID = "+parentID.toString());
										
						parentBiospecimen = iLimsService.getBiospecimenByUid(parentID, study);
						
						if(parentBiospecimen!=null){
							//Pull biocollection details from the parent sample
							bioCollection = parentBiospecimen.getBioCollection();
							biospecimen.setBioCollection(bioCollection);
							amtUsed = new Double(csvReader.get("AMOUNTUSED"));
							biospecimen.setParentUid(parentID);
							parentBiospecimen.setQuantity(parentBiospecimen.getQuantity() - amtUsed);
							
							if ((parentBiospecimen.getQuantity() - amtUsed) <= 0.0){
								InvCell invCell = iInventoryService.getInvCellByBiospecimen(parentBiospecimen);
								invCell.setBiospecimen(null);
								invCell.setStatus("Empty");
								iInventoryService.updateInvCell(invCell);
							}
						}else{
							log.info("Parent biospecimen does not exist");
						}						
					} else {
						//BIOCOLLECTIONUID
						if (csvReader.getIndex("BIOCOLLECTIONUID") > 0) {
							String biocollectionUid = csvReader.get("BIOCOLLECTIONUID");
							bioCollection = iLimsService.getBioCollectionByUID(biocollectionUid,this.study.getId(), subjectUID);
						
							if(bioCollection == null){
								bioCollection = new BioCollection();
								bioCollection.setLinkSubjectStudy(linkSubjectStudy);
								bioCollection.setStudy(study);
								bioCollection.setBiocollectionUid(biocollectionUid);
								bioCollection = iLimsService.createBioCollection(bioCollection);
								biospecimen.setBioCollection(bioCollection);
							}else{
								biospecimen.setBioCollection(bioCollection);
							}
						
							//BIOCOLLECTIONNAME
							if(csvReader.getIndex("BIOCOLLECTIONNAME") > 0){
								String biocollectionName = csvReader.get("BIOCOLLECTIONNAME");
								bioCollection.setName(biocollectionName);					
							}
						
							//BIOCOLLECTIONDATE
							log.info(csvReader.get("BIOCOLLECTIONDATE"));
							if(csvReader.getIndex("BIOCOLLECTIONDATE") > 0){
								Date biocollectionDate = simpleDateFormat.parse(csvReader.get("BIOCOLLECTIONDATE"));
								bioCollection.setCollectionDate(biocollectionDate);
							}	
						
							//AGEATCOLLECTION
							if(csvReader.getIndex("AGEATCOLLECTION") > 0){
								String ageAtCollection = csvReader.get("AGEATCOLLECTION");
								bioCollection.setPatientAge(Integer.parseInt(ageAtCollection));					
							}	
						
							//BIOCOLLECTIONCOMMENTS
							if(csvReader.getIndex("BIOCOLLECTIONCOMMENTS") > 0){
								String biocollectionComments = csvReader.get("BIOCOLLECTIONCOMMENTS");
								bioCollection.setComments(biocollectionComments);					
							}				
						}
					}
				}
				
				biospecimen.setLinkSubjectStudy(linkSubjectStudy);
				
				//SAMPLETYPE
				if (csvReader.getIndex("SAMPLETYPE") > 0) {
					String name = csvReader.get("SAMPLETYPE");
					BioSampletype sampleType = new BioSampletype();
					sampleType = iLimsService.getBioSampleTypeByName(name);
					biospecimen.setSampleType(sampleType);
				}
				
				/*//BIOCOLLECTIONDATE
				if(csvReader.getIndex("BIOCOLLECTIONDATE") > 0){	
					if(csvReader.get("BIOCOLLECTIONDATE")!=""){
						Date bioCollectionDate = simpleDateFormat.parse(csvReader.get("BIOCOLLECTIONDATE"));
						biospecimen.setProcessedDate(bioCollectionDate);
					}
				}
				
				//BIOCOLLECTIONTIME
					if(csvReader.getIndex("BIOCOLLECTIONTIME") > 0){
						if(csvReader.get("BIOCOLLECTIONTIME")!=""){
							Date d = simpleTimeFormat.parse(csvReader.get("BIOCOLLECTIONTIME"));
							biospecimen.setSampleTime(d);
						}
					}*/
				
				//PROCESSDATE
				if(csvReader.getIndex("PROCESSDATE") > 0){
					if(csvReader.get("PROCESSDATE")!=""){
						Date processDate = simpleDateFormat.parse(csvReader.get("PROCESSDATE"));
						biospecimen.setProcessedDate(processDate);
					}
				}
					
				//PROCESSTIME
				if(csvReader.getIndex("PROCESSTIME") > 0){
					
					if(csvReader.get("PROCESSTIME")!=""){
						Date d = simpleTimeFormat.parse(csvReader.get("PROCESSTIME")); 
						biospecimen.setProcessedTime(d);
					}
				}	
					
				//COMMENTS										
				if (csvReader.getIndex("COMMENTS") > 0) {
					String comments = csvReader.get("COMMENTS");
					biospecimen.setComments(comments);
				}
				
				//BARCODED
				if (csvReader.getIndex("BARCODED") > 0) {
					Boolean barcoded = Boolean.valueOf(csvReader.get("BARDCODED"));
					biospecimen.setBarcoded(barcoded);
				}
				
				//GRADE
				if (csvReader.getIndex("GRADE") > 0) {
					BiospecimenGrade grade = iLimsService.getBiospecimenGradeByName(csvReader.get("GRADE"));
					biospecimen.setGrade(grade);
				}	
				
				//STOREDIN
				if (csvReader.getIndex("STOREDIN") > 0) {
					BiospecimenStorage storedIn = iLimsService.getBiospecimenStorageByName(csvReader.get("STOREDIN"));
					biospecimen.setStoredIn(storedIn);
				}	
				
				//ANTICOAGULANTTYPE
				if (csvReader.getIndex("ANTICOAGULANTTYPE") > 0) {
					BiospecimenAnticoagulant anticoagulant = iLimsService.getBiospecimenAnticoagulantByName(csvReader.get("ANTICOAGULANTTYPE"));
					biospecimen.setAnticoag(anticoagulant);
				}
				
				//STATUS
				if (csvReader.getIndex("STATUS") > 0) {
					BiospecimenStatus status = iLimsService.getBiospecimenStatusByName(csvReader.get("STATUS"));
					biospecimen.setStatus(status);
				}	
				
				//PROTOCOL
				if (csvReader.getIndex("PROTOCOL") > 0) {
					BiospecimenProtocol protocol = iLimsService.getBiospecimenProtocolByName(csvReader.get("PROTOCOL"));
					biospecimen.setBiospecimenProtocol(protocol);
				}	
				
				//PURITY280
				if (csvReader.getIndex("PURITY280") > 0) {
					if(csvReader.get("PURITY280")!=""){
						Double purity280 = Double.valueOf(csvReader.get("PURITY280"));
						biospecimen.setPurity280(purity280);
					}
				}
				
				//PURITY230
				if (csvReader.getIndex("PURITY230") > 0) {
					if(csvReader.get("PURITY230")!=""){
						Double purity230 = Double.valueOf(csvReader.get("PURITY230"));
						biospecimen.setPurity230(purity230);
					}
				}	
				
				//QUALITY
				if (csvReader.getIndex("QUALITY") > 0) {
					BiospecimenQuality quality = iLimsService.getBiospecimenQualityByName(csvReader.get("QUALITY"));
					biospecimen.setQuality(quality);
				}	
				
				//QUANTITY				
				if (csvReader.getIndex("QUANTITY") > 0) {
					if(csvReader.get("QUANTITY")!=""){
						biospecimen.setQuantity(new Double(csvReader.get("QUANTITY")));
					}
				}
				
				//CONCENTRATION				
				if (csvReader.getIndex("CONCENTRATION") > 0) {
					if(csvReader.get("CONCENTRATION")!=""){
						biospecimen.setConcentration(new Double(csvReader.get("CONCENTRATION")));
					}
				}
				
				//UNITS
				if (csvReader.getIndex("UNITS") > 0) {
					String name = csvReader.get("UNITS");
					Unit unit = iLimsService.getUnitByName(name);
					biospecimen.setUnit(unit);
				}

				//TREATMENT
				if (csvReader.getIndex("TREATMENT") > 0) {
					String name = csvReader.get("TREATMENT");
					TreatmentType treatmentType = new TreatmentType(); 
					treatmentType = iLimsService.getTreatmentTypeByName(name);
					biospecimen.setTreatmentType(treatmentType);
				}
				
				if (biospecimen.getBiospecimenUid() == null || biospecimen.getBiospecimenUid().isEmpty()) {
					biospecimen.setStudy(study);
					
					Set<BioTransaction> bioTransactions = new HashSet<BioTransaction>(0);
					String bioTransactionStatus = csvReader.get("TRANSACTIONSTATUS");
							
					// Inheriently create a transaction for the initial quantity
					BioTransaction bioTransaction = new BioTransaction();
					bioTransaction.setBiospecimen(biospecimen);
					bioTransaction.setTransactionDate(Calendar.getInstance().getTime());
					bioTransaction.setQuantity(biospecimen.getQuantity());
					bioTransaction.setReason(au.org.theark.lims.web.Constants.BIOTRANSACTION_STATUS_INITIAL_QUANTITY);
					
					if(bioTransactionStatus.equalsIgnoreCase(au.org.theark.lims.web.Constants.BIOSPECIMEN_PROCESSING_ALIQUOTING)){
						BioTransactionStatus parentTransaction = iLimsService.getBioTransactionStatusByName(au.org.theark.lims.web.Constants.BIOSPECIMEN_PROCESSING_PROCESSING);
					}else if(bioTransactionStatus.equalsIgnoreCase(au.org.theark.lims.web.Constants.BIOSPECIMEN_PROCESSING_PROCESSING)){
						
					}
					
					

					BioTransactionStatus initialStatus = iLimsService.getBioTransactionStatusByName(au.org.theark.lims.web.Constants.BIOTRANSACTION_STATUS_INITIAL_QUANTITY);
					bioTransaction.setStatus(initialStatus);	//ensure that the initial transaction can be identified
					bioTransactions.add(bioTransaction);
					if (parentBiospecimen!=null){
						BioTransaction parentBioTransaction = new BioTransaction();
						parentBioTransaction.setBiospecimen(parentBiospecimen);
						parentBioTransaction.setTransactionDate(Calendar.getInstance().getTime());
						parentBioTransaction.setQuantity(amtUsed*-1);
						
						parentBioTransaction.setRecorder(currentUser.getPrincipal().toString());
						
						BioTransactionStatus parentTransaction = null;
						
						if(csvReader.get("BIOTRANSACTION").equalsIgnoreCase("ALIQUOTED")) {
							parentTransaction = iLimsService.getBioTransactionStatusByName("aliqouted");
							parentBioTransaction.setReason("Sub-Aliquot of " + biospecimenUID);
						}
						if(csvReader.get("BIOTRANSACTION").equalsIgnoreCase("PROCESSED")) {
							parentTransaction = iLimsService.getBioTransactionStatusByName("Processed");
							parentBioTransaction.setReason("Processed for " + biospecimenUID);  
						}
						
						parentBioTransaction.setStatus(parentTransaction);
						bioTransactions.add(parentBioTransaction);
					}
					biospecimen.setBioTransactions(bioTransactions);
					//validation SHOULD make sure these cases will work.  TODO:  test scripts
					if(study.getAutoGenerateBiospecimenUid()){
						biospecimen.setBiospecimenUid(iLimsService.getNextGeneratedBiospecimenUID(study));
					}
					else{
						biospecimen.setBiospecimenUid(biospecimenUID);
					}
					insertBiospecimens.add(biospecimen);
					StringBuffer sb = new StringBuffer();
					sb.append("Biospecimen UID: ");
					sb.append(biospecimen.getBiospecimenUid());
					sb.append(" has been created successfully.");
					if(parentBiospecimen!=null){
						updateBiospecimens.add(parentBiospecimen);
						sb.append(" Biospecimen UID: ");
						sb.append(parentBiospecimen.getBiospecimenUid());
						sb.append(" has been updated.");
					}
					sb.append("\n");
					uploadReport.append(sb);
					insertCount++;
				}
				else {
					updateBiospecimens.add(biospecimen);
					StringBuffer sb = new StringBuffer();
					sb.append("Biospecimen UID: ");
					sb.append(biospecimen.getBiospecimenUid());
					sb.append(" has been updated successfully.");
					if(parentBiospecimen!=null){
						updateBiospecimens.add(parentBiospecimen);
						sb.append(" Biospecimen UID: ");
						sb.append(parentBiospecimen.getBiospecimenUid());
						sb.append(" has been updated.");
					}
					sb.append("\n");
					uploadReport.append(sb);
					updateCount++;
				}
				
				// Allocation details
				InvCell invCell;
				String siteName = null;
				String freezerName = null;
				String shelfName = null;
				String rackName = null;
				String boxName = null;
				String row = null;
				String column = null;
				
				//SITE	
				if (csvReader.getIndex("SITE") > 0) {
					siteName = csvReader.get("SITE");
				}

				//FREEZER	
				if (csvReader.getIndex("FREEZER") > 0) {
					freezerName = csvReader.get("FREEZER");
				}
				
				//SHELF	
				if (csvReader.getIndex("SHELF") > 0) {
					shelfName = csvReader.get("SHELF");
				}

				//RACK	
				if (csvReader.getIndex("RACK") > 0) {
					rackName = csvReader.get("RACK");
				}

				//BOX	
				if (csvReader.getIndex("BOX") > 0) {
					boxName = csvReader.get("BOX");
				}
				
				//ROW	
				if (csvReader.getIndex("ROW") > 0) {
					row = csvReader.get("ROW");
				}

				//COLUMN
				if (csvReader.getIndex("COLUMN") > 0) {
					column = csvReader.get("COLUMN");
				}
				
				invCell = new InvCell();
				invCell = iInventoryService.getInvCellByLocationNames(siteName, freezerName, shelfName, rackName, boxName, row, column);
				//TODO : null checking here.  should be picked up ikn validation  JIRA 657 Created  log.info("invcell null?" + (invCell == null));

				if(invCell != null && invCell.getId() != null) {
					biospecimen.setInvCell(invCell); //.set
//					invCell.setBiospecimen(biospecimen);
					
					//updateInvCells.add(invCell);
				}
				
				recordCount++;
			}
		}
		catch (IOException ioe) {
			uploadReport.append("Unexpected I/O exception whilst reading the biospecimen data file\n");
			log.error("processMatrixBiospecimenFile IOException stacktrace:", ioe);
			throw new ArkSystemException("Unexpected I/O exception whilst reading the biospecimen data file");
		}
		catch (Exception ex) {
			uploadReport.append("Unexpected exception whilst reading the biospecimen data file\n");
			log.error("processMatrixBiospecimenFile Exception stacktrace:", ex);
			throw new ArkSystemException("Unexpected exception occurred when trying to process biospecimen data file");
		}
		finally {
			// Clean up the IO objects
			timer.stop();
			uploadReport.append("\n");
			uploadReport.append("Total elapsed time: ");
			uploadReport.append(timer.getTime());
			uploadReport.append(" ms or ");
			uploadReport.append(decimalFormat.format(timer.getTime() / 1000.0));
			uploadReport.append(" s");
			uploadReport.append("\n");
			uploadReport.append("Total file size: ");
			uploadReport.append(inLength);
			uploadReport.append(" B or ");
			uploadReport.append(decimalFormat.format(inLength / 1024.0 / 1024.0));
			uploadReport.append(" MB");
			uploadReport.append("\n");

			if (timer != null)
				timer = null;

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
		}
		uploadReport.append("Processed ");
		uploadReport.append(recordCount);
		uploadReport.append(" records.");
		uploadReport.append("\n");
		uploadReport.append("Inserted ");
		uploadReport.append(insertCount);
		uploadReport.append(" records.");
		uploadReport.append("\n");
		uploadReport.append("Updated ");
		uploadReport.append(updateCount);
		uploadReport.append(" records.");
		uploadReport.append("\n");

		// Batch insert/update
		iLimsService.batchInsertBiospecimens(insertBiospecimens);
		//iLimsService.batchUpdateBiospecimens(updateBiospecimens);
		//iLimsService.batchUpdateInvCells(updateInvCells);
		
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
	 */
	public StringBuffer uploadAndReportMatrixLocationFile(InputStream fileInputStream, long inLength, String inFileFormat, char inDelimChr) throws FileFormatException, ArkSystemException {
		delimiterCharacter = inDelimChr;
		uploadReport = new StringBuffer();
		curPos = 0;
		List<InvCell> cellsToUpdate = new ArrayList<InvCell>();

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

			srcLength = inLength;
			if (srcLength <= 0) {
				uploadReport.append("The input size was not greater than 0. Actual length reported: ");
				uploadReport.append(srcLength);
				uploadReport.append("\n");
				throw new FileFormatException("The input size was not greater than 0. Actual length reported: " + srcLength);
			}

			timer = new StopWatch();
			timer.start();

			// Set field list (note 2th column to Nth column)
			// BIOSPECIMENUID F1 F2 FN
			// 0 1 2 N
			csvReader.readHeaders();

			srcLength = inLength - csvReader.getHeaders().toString().length();
			log.debug("Header length: " + csvReader.getHeaders().toString().length());

			// Loop through all rows in file
			while (csvReader.readRecord()) {
				
				log.info("At record: " + recordCount);
				String biospecimenUID = csvReader.get("BIOSPECIMENUID");

/*				//this is validated in prior step and should never happen
				if(linkSubjectStudy==null){
					log.error("\n\n\n\n\n\n\n\n\n\n\n\nUnexpected subject? a shouldnt happen...we should have errored this in validation");
					break;//TODO : log appropriately or do some handling
				}
*/
				Biospecimen biospecimen = iLimsService.getBiospecimenByUid(biospecimenUID,study);
				if(biospecimen == null) {
					log.error("\n\n\n\n\n\n\n\n\n....We should NEVER have null biospecimens this should be  validated in prior step");
					break;
				}
								
					
				// Allocation details
				InvCell invCell;
				String siteName = null;
				String freezerName = null;
				String shelfName = null;
				String rackName = null;
				String boxName = null;
				String row = null;
				String column = null;
				
				if (csvReader.getIndex("SITE") > 0) {
					siteName = csvReader.get("SITE");
				}

				if (csvReader.getIndex("FREEZER") > 0) {
					freezerName = csvReader.get("FREEZER");
				}
				
				if (csvReader.getIndex("SHELF") > 0) {
					shelfName = csvReader.get("SHELF");
				}

				if (csvReader.getIndex("RACK") > 0) {
					rackName = csvReader.get("RACK");
				}

				if (csvReader.getIndex("BOX") > 0) {
					boxName = csvReader.get("BOX");
				}
				
				if (csvReader.getIndex("ROW") > 0) {
					row = csvReader.get("ROW");
				}
				
				if (csvReader.getIndex("COLUMN") > 0) {
					column = csvReader.get("COLUMN");
				}
				
				invCell = iInventoryService.getInvCellByLocationNames(siteName, freezerName, shelfName, rackName, boxName, row, column);
				//TODO : null checking here.  should be picked up ikn validation  JIRA 657 Created  log.info("invcell null?" + (invCell == null));

				if(invCell != null && invCell.getId() != null) {
					if(invCell.getBiospecimen()!=null){
						log.error("This should NEVER happen as validation should ensure no cell will wipte another");
						break;
					}
					invCell.setBiospecimen(biospecimen);
					cellsToUpdate.add(invCell);
//					biospecimen.setInvCell(invCell); 
					updateCount++;
				}
				else{
					log.error("This should NEVER happen as validation should ensure all cells valid");
					break;
				}
				
				recordCount++;
			}
		}
		catch (IOException ioe) {
			uploadReport.append("Unexpected I/O exception whilst reading the biospecimen data file\n");
			log.error("processMatrixBiospecimenFile IOException stacktrace:", ioe);
			throw new ArkSystemException("Unexpected I/O exception whilst reading the biospecimen data file");
		}
		catch (Exception ex) {
			uploadReport.append("Unexpected exception whilst reading the biospecimen data file\n");
			log.error("processMatrixBiospecimenFile Exception stacktrace:", ex);
			throw new ArkSystemException("Unexpected exception occurred when trying to process biospecimen data file");
		}
		finally {
			// Clean up the IO objects
			timer.stop();
			uploadReport.append("\n");
			uploadReport.append("Total elapsed time: ");
			uploadReport.append(timer.getTime());
			uploadReport.append(" ms or ");
			uploadReport.append(decimalFormat.format(timer.getTime() / 1000.0));
			uploadReport.append(" s");
			uploadReport.append("\n");
			uploadReport.append("Total file size: ");
			uploadReport.append(inLength);
			uploadReport.append(" B or ");
			uploadReport.append(decimalFormat.format(inLength / 1024.0 / 1024.0));
			uploadReport.append(" MB");
			uploadReport.append("\n");

			if (timer != null)
				timer = null;

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
		}
		iLimsService.batchUpdateInvCells(cellsToUpdate);//TODO:  finally after everything for timing...or remove timing
		uploadReport.append("Processed ");
		uploadReport.append(recordCount);
		uploadReport.append(" records.");
		uploadReport.append("\n");
		uploadReport.append("Updated ");
		uploadReport.append(updateCount);
		uploadReport.append(" records.");
		uploadReport.append("\n");

		// Batch insert/update
		//iLimsService.batchInsertBiospecimens(insertBiospecimens);
		//iLimsService.batchUpdateBiospecimens(updateBiospecimens);
		//iLimsService.batchUpdateInvCells(updateInvCells);
		
		return uploadReport;
	}

	
	
	
	
	
	
	
	/**
	 * Return the progress of the current process in %
	 * 
	 * @return if a process is actively running, then progress in %; or if no process running, then returns -1
	 */
	public double getProgress() {
		double progress = -1;

		if (srcLength > 0)
			progress = curPos * 100.0 / srcLength; // %

		return progress;
	}

	/**
	 * Return the speed of the current process in KB/s
	 * 
	 * @return if a process is actively running, then speed in KB/s; or if no process running, then returns -1
	 */
	public double getSpeed() {
		double speed = -1;

		if (srcLength > 0)
			speed = curPos / 1024 / (timer.getTime() / 1000.0); // KB/s

		return speed;
	}
}