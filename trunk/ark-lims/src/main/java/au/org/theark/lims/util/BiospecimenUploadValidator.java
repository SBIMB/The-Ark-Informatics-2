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
import java.util.Date;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import jxl.Workbook;
import jxl.read.biff.BiffException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.theark.core.Constants;
import au.org.theark.core.exception.ArkBaseException;
import au.org.theark.core.exception.ArkSystemException;
import au.org.theark.core.exception.FileFormatException;
import au.org.theark.core.model.lims.entity.BioCollection;
import au.org.theark.core.model.lims.entity.BioSampletype;
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
import au.org.theark.core.util.DataConversionAndManipulationHelper;
import au.org.theark.core.util.XLStoCSV;
import au.org.theark.core.vo.UploadVO;
import au.org.theark.core.web.component.worksheet.ArkGridCell;
import au.org.theark.lims.model.dao.BioPersonDao;
import au.org.theark.lims.service.IInventoryService;
import au.org.theark.lims.service.ILimsService;

import com.csvreader.CsvReader;

/**
 * BiospecimenUploadValidator provides support for validating Biospecimen matrix-formatted files.
 * 
 * @author cellis
 */
public class BiospecimenUploadValidator {
	private static Logger			log							= LoggerFactory.getLogger(BiospecimenUploadValidator.class);
	@SuppressWarnings("unchecked")
	private IArkCommonService		iArkCommonService;
	private ILimsService				iLimsService;
	private IInventoryService		iInventoryService;
	private Long						studyId;
	private Study						study;
	java.util.Collection<String>	fileValidationMessages	= new java.util.ArrayList<String>();
	java.util.Collection<String>	dataValidationMessages	= new java.util.ArrayList<String>();
	private HashSet<Integer>		insertRows;
	private HashSet<Integer>		updateRows;
	private HashSet<ArkGridCell>	errorCells;
	private long						recordCount;
	private long						srcLength					= -1;
//	private StopWatch					timer							= null;
	private char						delimiterCharacter		= au.org.theark.core.Constants.DEFAULT_DELIMITER_CHARACTER;
	private String						fileFormat					= au.org.theark.core.Constants.DEFAULT_FILE_FORMAT;
	private SimpleDateFormat		simpleDateFormat			= new SimpleDateFormat(au.org.theark.core.Constants.DD_MM_YYYY);
	private SimpleDateFormat		simpleTimeFormat			=	new SimpleDateFormat(au.org.theark.core.Constants.HH_MM_SS);
	private Timestamp	timeStamp;
	private int							row							= 1;

	@SuppressWarnings("unchecked")
	public BiospecimenUploadValidator(Study study, IArkCommonService iArkCommonService, ILimsService iLimsService, IInventoryService iInventoryService) {
		super();
		this.iArkCommonService = iArkCommonService;
		this.iLimsService = iLimsService;
		this.iInventoryService = iInventoryService;
		this.study = study;
		this.insertRows = new HashSet<Integer>();
		this.updateRows = new HashSet<Integer>();
		this.errorCells = new HashSet<ArkGridCell>();
		simpleDateFormat.setLenient(false);
	}

	public java.util.Collection<String> getFileValidationMessages() {
		return fileValidationMessages;
	}

	public void setFileValidationMessages(java.util.Collection<String> fileValidationMessages) {
		this.fileValidationMessages = fileValidationMessages;
	}

	public java.util.Collection<String> getDataValidationMessages() {
		return dataValidationMessages;
	}

	public void setDataValidationMessages(java.util.Collection<String> dataValidationMessages) {
		this.dataValidationMessages = dataValidationMessages;
	}

	public Long getStudyId() {
		return studyId;
	}

	public void setStudyId(Long studyId) {
		this.studyId = studyId;
	}

	public Study getStudy() {
		return study;
	}

	public void setStudy(Study study) {
		this.study = study;
	}

	public HashSet<Integer> getInsertRows() {
		return insertRows;
	}

	public void setInsertRows(HashSet<Integer> insertRows) {
		this.insertRows = insertRows;
	}

	public HashSet<Integer> getUpdateRows() {
		return updateRows;
	}

	public void setUpdateRows(HashSet<Integer> updateRows) {
		this.updateRows = updateRows;
	}

	public HashSet<ArkGridCell> getErrorCells() {
		return errorCells;
	}

	public void setErrorCells(HashSet<ArkGridCell> errorCells) {
		this.errorCells = errorCells;
	}

	/**
	 * 
	 * @param uploadVo
	 *           is the UploadVO of the file
	 * @return a collection of validation messages
	 */
	public Collection<String> validateLocationFileFormat(UploadVO uploadVo) {
		java.util.Collection<String> validationMessages = null;
		try {
			InputStream inputStream = uploadVo.getFileUpload().getInputStream();
			String filename = uploadVo.getFileUpload().getClientFileName();
			fileFormat = filename.substring(filename.lastIndexOf('.') + 1).toUpperCase();
			delimiterCharacter = uploadVo.getUpload().getDelimiterType().getDelimiterCharacter();
			validationMessages = validateLocationFileFormat(inputStream, fileFormat, delimiterCharacter);
		}
		catch (IOException e) {
			log.error(e.getMessage());
		}
		return validationMessages;
	}


	/**
	 * Validates the file in the default "matrix" file format assumed: SUBJECTUID,FIELD1,FIELD2,FIELDN... Where N is any number of columns
	 * 
	 * @param uploadVo
	 *           is the UploadVO of the file
	 * @return a collection of validation messages
	 */
	public Collection<String> validateSubjectFileFormat(UploadVO uploadVo) {
		java.util.Collection<String> validationMessages = null;
		try {
			InputStream inputStream = uploadVo.getFileUpload().getInputStream();
			String filename = uploadVo.getFileUpload().getClientFileName();
			fileFormat = filename.substring(filename.lastIndexOf('.') + 1).toUpperCase();
			delimiterCharacter = uploadVo.getUpload().getDelimiterType().getDelimiterCharacter();
			validationMessages = validateBiospecimenFileFormat(inputStream, fileFormat, delimiterCharacter);
		}
		catch (IOException e) {
			log.error(e.getMessage());
		}
		return validationMessages;
	}


	/**
	 * Validates the file in the default "matrix" file format assumed: SUBJECTUID,FIELD1,FIELD2,FIELDN... Where N is any number of columns
	 * 
	 * @param inputStream
	 *           is the input stream of the file
	 * @param fileFormat
	 *           is the file format (eg txt)
	 * @param delimChar
	 *           is the delimiter character of the file (eg comma)
	 * @return a collection of validation messages
	 */
	public Collection<String> validateLocationFileFormat(InputStream inputStream, String fileFormat, char delimChar) {
		java.util.Collection<String> validationMessages = null;

		try {
			// If Excel, convert to CSV for validation
			if (fileFormat.equalsIgnoreCase("XLS")) {
				Workbook w;
				try {
					w = Workbook.getWorkbook(inputStream);
					delimiterCharacter = ',';
					XLStoCSV xlsToCsv = new XLStoCSV(delimiterCharacter);
					inputStream = xlsToCsv.convertXlsToCsv(w);
					inputStream.reset();
					delimiterCharacter = ',';
				}
				catch (BiffException e) {
					log.error(e.getMessage());
				}
				catch (IOException e) {
					log.error(e.getMessage());
				}
			}
			validationMessages = validateLocationFileFormat(inputStream, inputStream.toString().length(), fileFormat, delimChar);
		}
		catch (FileFormatException ffe) {
			log.error(Constants.FILE_FORMAT_EXCEPTION + ffe);
		}
		catch (ArkBaseException abe) {
			log.error(Constants.ARK_BASE_EXCEPTION + abe);
		}
		return validationMessages;
	}

	/**
	 * Validates the file in the default "matrix" file format assumed: SUBJECTUID,FIELD1,FIELD2,FIELDN... Where N is any number of columns
	 * 
	 * @param inputStream
	 *           is the input stream of the file
	 * @param fileFormat
	 *           is the file format (eg txt)
	 * @param delimChar
	 *           is the delimiter character of the file (eg comma)
	 * @return a collection of validation messages
	 */
	public Collection<String> validateBiospecimenFileFormat(InputStream inputStream, String fileFormat, char delimChar) {
		java.util.Collection<String> validationMessages = null;

		try {
			// If Excel, convert to CSV for validation
			if (fileFormat.equalsIgnoreCase("XLS")) {
				Workbook w;
				try {
					w = Workbook.getWorkbook(inputStream);
					delimiterCharacter = ',';
					XLStoCSV xlsToCsv = new XLStoCSV(delimiterCharacter);
					inputStream = xlsToCsv.convertXlsToCsv(w);
					inputStream.reset();
					delimiterCharacter = ',';
				}
				catch (BiffException e) {
					log.error(e.getMessage());
				}
				catch (IOException e) {
					log.error(e.getMessage());
				}
			}
			validationMessages = validateBiospecimenMatrixFileFormat(inputStream, inputStream.toString().length(), fileFormat, delimChar);
		}
		catch (FileFormatException ffe) {
			log.error(Constants.FILE_FORMAT_EXCEPTION + ffe);
		}
		catch (ArkBaseException abe) {
			log.error(Constants.ARK_BASE_EXCEPTION + abe);
		}
		return validationMessages;
	}


	/**
	 * Validates the file in the default "matrix" file data assumed: BiospecimenUID,FIELD1,FIELD2,FIELDN... Where N is any number of columns
	 * 
	 * @param uploadVo
	 *           is the UploadVO of the file
	 * @return a collection of validation messages
	 */
	public Collection<String> validateLocationFileData(UploadVO uploadVo) {
		java.util.Collection<String> validationMessages = null;
		try {
			InputStream inputStream = uploadVo.getFileUpload().getInputStream();
			String filename = uploadVo.getFileUpload().getClientFileName();
			fileFormat = filename.substring(filename.lastIndexOf('.') + 1).toUpperCase();
			delimiterCharacter = uploadVo.getUpload().getDelimiterType().getDelimiterCharacter();

			// If Excel, convert to CSV for validation
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
			}

			validationMessages = validateLocationFileData(inputStream, fileFormat, delimiterCharacter);
		}
		catch (IOException e) {
			log.error(e.getMessage());
		}
		return validationMessages;
	}

	/**
	 * Validates the file in the default "matrix" file data assumed: BiospecimenUID,FIELD1,FIELD2,FIELDN... Where N is any number of columns
	 * 
	 * @param uploadVo
	 *           is the UploadVO of the file
	 * @return a collection of validation messages
	 */
	public Collection<String> validateBiospecimenFileData(UploadVO uploadVo) {
		java.util.Collection<String> validationMessages = null;
		try {
			InputStream inputStream = uploadVo.getFileUpload().getInputStream();
			String filename = uploadVo.getFileUpload().getClientFileName();
			fileFormat = filename.substring(filename.lastIndexOf('.') + 1).toUpperCase();
			delimiterCharacter = uploadVo.getUpload().getDelimiterType().getDelimiterCharacter();

			// If Excel, convert to CSV for validation
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
			}

			validationMessages = validateBiospecimenFileData(inputStream, fileFormat, delimiterCharacter);
		}
		catch (IOException e) {
			log.error(e.getMessage());
		}
		return validationMessages;
	}

	public Collection<String> validateBiospecimenFileData(InputStream inputStream, String fileFormat, char delimChar) {
		java.util.Collection<String> validationMessages = null;

		try {
			validationMessages = validateMatrixBiospecimenFileData(inputStream, inputStream.toString().length(), fileFormat, delimChar);
		}
		catch (FileFormatException ffe) {
			log.error(Constants.FILE_FORMAT_EXCEPTION + ffe);
		}
		catch (ArkBaseException abe) {
			log.error(Constants.ARK_BASE_EXCEPTION + abe);
		}
		return validationMessages;
	}


	public Collection<String> validateLocationFileData(InputStream inputStream, String fileFormat, char delimChar) {
		java.util.Collection<String> validationMessages = null;

		try {
			validationMessages = validateMatrixLocationFileData(inputStream, inputStream.toString().length(), fileFormat, delimChar);
		}
		catch (FileFormatException ffe) {
			log.error(Constants.FILE_FORMAT_EXCEPTION + ffe);
		}
		catch (ArkBaseException abe) {
			log.error(Constants.ARK_BASE_EXCEPTION + abe);
		}
		return validationMessages;
	}

	/**
	 * Validates the Biospecimen file in the default "matrix" file format assumed
	 * 
	 * @param fileInputStream
	 * @param inLength
	 * @param inFileFormat
	 * @param inDelimChr
	 * @return
	 * @throws FileFormatException
	 * @throws ArkBaseException
	 */
	public java.util.Collection<String> validateLocationFileFormat(InputStream fileInputStream, long inLength, String inFileFormat, char inDelimChr) throws FileFormatException,
			ArkBaseException {
		delimiterCharacter = inDelimChr;
		fileFormat = inFileFormat;
		row = 0;

		InputStreamReader inputStreamReader = null;
		CsvReader csvReader = null;
		try {
			inputStreamReader = new InputStreamReader(fileInputStream);
			csvReader = new CsvReader(inputStreamReader, delimiterCharacter);

			srcLength = inLength;
			if (srcLength <= 0) {
				throw new FileFormatException("The input size was not greater than 0.  Actual length reported: " + srcLength);
			}

			// Set field list (note 2th column to Nth column)
			// BiospecimenUID SITE F1 F2 FN
			// 0 1 2 3 N
			csvReader.readHeaders();

			srcLength = inLength - csvReader.getHeaders().toString().length();
			log.debug("Header length: " + csvReader.getHeaders().toString().length());
			String[] headerColumnArray = csvReader.getHeaders();

			Collection<String> biospecimenColumns = new ArrayList<String>();
			String[] biospecimenHeaderColumnArray = Constants.LOCATION_UPLOAD_TEMPLATE_HEADER;
			boolean headerError = false;
			for (int i = 0; i < biospecimenHeaderColumnArray.length; i++) {
				String colName = biospecimenHeaderColumnArray[i];
				biospecimenColumns.add(colName);
			}

			for (int i = 0; i < headerColumnArray.length; i++) {
				String colName = headerColumnArray[i];
				if (!biospecimenColumns.contains(colName)) {
					headerError = true;
					break;
				}
			}

			if (headerError) {
				// Invalid file format
				StringBuffer stringBuffer = new StringBuffer();
				stringBuffer.append("Error: The specified file does not appear to conform to the expected file format.\n");
				stringBuffer.append("The specified fileformat was: " + fileFormat + ".\n");
				stringBuffer.append("The specified delimiter type was: " + delimiterCharacter + ".\n");
				stringBuffer.append("The default format should be as follows:\n");

				// Column headers
				for (String column : Constants.LOCATION_UPLOAD_TEMPLATE_HEADER) {
					stringBuffer.append(column);
					stringBuffer.append(delimiterCharacter);
				}
				stringBuffer.deleteCharAt(stringBuffer.length()-1);
				stringBuffer.append("\n");

				fileValidationMessages.add(stringBuffer.toString());

				for (int i = 0; i < headerColumnArray.length; i++) {
					if (!biospecimenColumns.contains(headerColumnArray[i].toUpperCase())) {
						fileValidationMessages.add("Error: the column name " + headerColumnArray[i] + " is not a valid column name.");
					}
				}
			}

			row = 1;
		}
		catch (IOException ioe) {
			log.error("locationFile IOException stacktrace:", ioe);
			throw new ArkSystemException("Unexpected I/O exception whilst reading the Location data file");
		}
		catch (Exception ex) {
			log.error("locationFile Exception stacktrace:", ex);
			throw new ArkSystemException("Unexpected exception occurred when trying to process Location data file");
		}
		finally {
			// Clean up the IO objects
			
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

		return fileValidationMessages;
	}


	/**
	 * Validates the Biospecimen file in the default "matrix" file format assumed
	 * 
	 * @param fileInputStream
	 * @param inLength
	 * @param inFileFormat
	 * @param inDelimChr
	 * @return
	 * @throws FileFormatException
	 * @throws ArkBaseException
	 */
	public java.util.Collection<String> validateBiospecimenMatrixFileFormat(InputStream fileInputStream, long inLength, String inFileFormat, char inDelimChr) throws FileFormatException,
			ArkBaseException {
		delimiterCharacter = inDelimChr;
		fileFormat = inFileFormat;
		row = 0;

		InputStreamReader inputStreamReader = null;
		CsvReader csvReader = null;
		try {
			inputStreamReader = new InputStreamReader(fileInputStream);
			csvReader = new CsvReader(inputStreamReader, delimiterCharacter);

			srcLength = inLength;
			if (srcLength <= 0) {
				throw new FileFormatException("The input size was not greater than 0.  Actual length reported: " + srcLength);
			}

			// Set field list (note 2th column to Nth column)
			// BiospecimenUID DATE_COLLECTED F1 F2 FN
			// 0 1 2 3 N
			csvReader.readHeaders();

			srcLength = inLength - csvReader.getHeaders().toString().length();
			log.debug("Header length: " + csvReader.getHeaders().toString().length());
			String[] headerColumnArray = csvReader.getHeaders();

			Collection<String> biospecimenColumns = new ArrayList<String>();
			String[] biospecimenHeaderColumnArray = Constants.BIOSPECIMEN_TEMPLATE_HEADER;
			boolean headerError = false;
			for (int i = 0; i < biospecimenHeaderColumnArray.length; i++) {
				String colName = biospecimenHeaderColumnArray[i];
				biospecimenColumns.add(colName);
			}

			for (int i = 0; i < headerColumnArray.length; i++) {
				String colName = headerColumnArray[i];
				if (!biospecimenColumns.contains(colName)) {
					headerError = true;
					break;
				}
			}

			if (headerError) {
				// Invalid file format
				StringBuffer stringBuffer = new StringBuffer();
				stringBuffer.append("Error: The specified file does not appear to conform to the expected file format.\n");
				stringBuffer.append("The specified fileformat was: " + fileFormat + ".\n");
				stringBuffer.append("The specified delimiter type was: " + delimiterCharacter + ".\n");
				stringBuffer.append("The default format should be as follows:\n");

				// Column headers
				for (String column : Constants.BIOSPECIMEN_TEMPLATE_HEADER) {
					stringBuffer.append(column);
					stringBuffer.append(delimiterCharacter);
				}
				stringBuffer.deleteCharAt(stringBuffer.length()-1);
				stringBuffer.append("\n");

				fileValidationMessages.add(stringBuffer.toString());

				for (int i = 0; i < headerColumnArray.length; i++) {
					if (!biospecimenColumns.contains(headerColumnArray[i].toUpperCase())) {
						fileValidationMessages.add("Error: the column name " + headerColumnArray[i] + " is not a valid column name.");
					}
				}
			}

			row = 1;
		}
		catch (IOException ioe) {
			log.error("processMatrixBiospecimenFile IOException stacktrace:", ioe);
			throw new ArkSystemException("Unexpected I/O exception whilst reading the Biospecimen data file");
		}
		catch (Exception ex) {
			log.error("processMatrixBiospecimenFile Exception stacktrace:", ex);
			throw new ArkSystemException("Unexpected exception occurred when trying to process Biospecimen data file");
		}
		finally {
			// Clean up the IO objects
			
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

		return fileValidationMessages;
	}

	/**
	 * Validates the file in the default "matrix" file format assumed: BiospecimenUID,FIELD1,FIELD2,FIELDN...
		 * Where N is any number of columns
	 * 
	 * 
	 * 
	 * 
	 * 
	 * attempting to apply logic as seen: https://the-ark.atlassian.net/browse/ARK-790
	 * 
	 * THERE IS NO UPDATING

		For Biospecimen;
		if you have auto gen & you dont specific an id...then upload and generate biospec-id
		if you have auto gen & you do specific an id...then error message - don't upload
		if you DON'T have auto gen & you do specific an id...then
		...if something exists with that id already...THEN ERROR
		...else create the biospecimen with given id
		if you DON'T have auto gen & you dont specific an id...then error - don't upload
		
		For Biocollection;
		if you have auto gen & you dont specific an id...then upload and gen biocol
		if you have auto gen & you do specific an id
		...then look for existing biocol to point at
		...if exists then put biospec in that biolcollection
		...ELSE error
		if you DON'T have auto gen & you do specific an id
		...then if biocol already exists, use that existing biocol and tie biospec to it
		........however if biocol doesnt exist...create biocol with the given id
		if you DON'T have auto gen & you dont specific an id...then error

	 * 
	 * 
	 * 
	 * 
	 * @param fileInputStream
	 *           is the input stream of a file
	 * @param inLength
	 *           is the length of a file
	 * @throws FileFormatException
	 *            file format Exception
	 * @throws ArkBaseException
	 *            general ARK Exception
	 * @return a collection of data validation messages
	 */
	public java.util.Collection<String> validateMatrixBiospecimenFileData(InputStream fileInputStream, long inLength, String inFileFormat, char inDelimChr) throws FileFormatException,
			ArkSystemException {
		delimiterCharacter = inDelimChr;
		fileFormat = inFileFormat;
		
		HashMap<String, String> uniqueSubjectBiocollections = new HashMap<String, String>();
		HashSet<String> uniqueSubjectBiospecimens = new HashSet<String>();

		row = 1;

		InputStreamReader inputStreamReader = null;
		CsvReader csvReader = null;
		
		try {
			inputStreamReader = new InputStreamReader(fileInputStream);
			csvReader = new CsvReader(inputStreamReader, delimiterCharacter);
			
			String[] stringLineArray;

			srcLength = inLength;
			if (srcLength <= 0) {
				throw new FileFormatException("The input size was not greater than 0.  Actual length reported: " + srcLength);
			}


			// Set field list (note 1th column to Nth column)
			// BiospecimenUID F1 F2 FN
			// 0 1 2 N
			csvReader.readHeaders();

			srcLength = inLength - csvReader.getHeaders().toString().length();

			String[] fieldNameArray = csvReader.getHeaders();
			
			// Loop through all rows in file
			while (csvReader.readRecord()) {
				boolean insertThisRow = true;
				stringLineArray = csvReader.getValues();
				String subjectUID = null;
				String biospecimenUID = null;
				String biocollectionUID = null;
				String ageAtCollection = null;
				
				biospecimenUID = csvReader.get("BIOSPECIMENUID");
				subjectUID = csvReader.get("SUBJECTUID");
				BioCollection biocollection = null;// iLimsService.getBioCollectionByUID(biocollectionUID,study.getId());
				Biospecimen biospecimen = iLimsService.getBiospecimenByUid(biospecimenUID,study);
				Biospecimen parentBiospecimen = null;
				
				//PARENTID 
				if (csvReader.getIndex("PARENTID") > 0) {
					String parentBiospecimenID = csvReader.get("PARENTID");
					if(!parentBiospecimenID.isEmpty()){
						parentBiospecimen = new Biospecimen();
						parentBiospecimen = iLimsService.getBiospecimenByUid(parentBiospecimenID, study);
					
						if (parentBiospecimen == null) {
							StringBuilder errorString = new StringBuilder();
							errorString.append("Error: Row ");
							errorString.append(row);
							errorString.append(": SubjectUID: ");
							errorString.append(subjectUID);
							errorString.append(" The parent biospecimen ");
							errorString.append(parentBiospecimenID);
							errorString.append(" of BiospecimenUID: ");
							errorString.append(biospecimenUID);
							errorString.append(" does not exist in the database. Please check and try again");
							dataValidationMessages.add(errorString.toString());
							errorCells.add(new ArkGridCell(csvReader.getIndex("PARENTID"), row));
							insertThisRow = false;//drop out also?
						}else{
							if (csvReader.getIndex("AMOUNTUSED") > 0) {
								String parentBiospecimenAmtUsed = csvReader.get("AMOUNTUSED");
								if(parentBiospecimenAmtUsed.isEmpty()){
									StringBuilder errorString = new StringBuilder();
									errorString.append("Error: Row ");
									errorString.append(row);
									errorString.append(": SubjectUID: ");
									errorString.append(subjectUID);
									errorString.append(" The amount used ");
									errorString.append(parentBiospecimenID);
									errorString.append(" of BiospecimenUID: ");
									errorString.append(biospecimenUID);
									errorString.append(" is not specified. Please check and try again");
									dataValidationMessages.add(errorString.toString());
									errorCells.add(new ArkGridCell(csvReader.getIndex("AMOUNTUSED"), row));
									insertThisRow = false;//drop out also?
									log.error("Amt used is empty");
								}else{
									try{
										Double amtUsed = Double.parseDouble(parentBiospecimenAmtUsed);
										log.error("qty "+parentBiospecimen.getQuantity());
										log.error("amt used "+amtUsed);
										if(parentBiospecimen.getQuantity() < amtUsed){
											StringBuilder errorString = new StringBuilder();
											errorString.append("Error: Row ");
											errorString.append(row);
											errorString.append(": SubjectUID: ");
											errorString.append(subjectUID);
											errorString.append(" The amount used ");
											errorString.append(" of Biospecimen: ");
											errorString.append(biospecimenUID);
											errorString.append(" is greater than the current amount. Please check and try again.");
											dataValidationMessages.add(errorString.toString());
											errorCells.add(new ArkGridCell(csvReader.getIndex("AMOUNTUSED"), row));
											insertThisRow = false;//drop out also?
										}
									}catch(NumberFormatException ne){
										StringBuilder errorString = new StringBuilder();
										errorString.append("Error: Row ");
										errorString.append(row);
										errorString.append(": SubjectUID: ");
										errorString.append(subjectUID);
										errorString.append(" The age at collection ");
										errorString.append(parentBiospecimenAmtUsed);
										errorString.append(" of Biospecimen: ");
										errorString.append(biospecimenUID);
										errorString.append(" is not a valid number.");
										dataValidationMessages.add(errorString.toString());
										errorCells.add(new ArkGridCell(csvReader.getIndex("AMOUNTUSED"), row));
										insertThisRow = false;//drop out also?
									}
								}log.error("Amt used is not empty");
							}
							if(csvReader.getIndex("TRANSACTION") > 0){
								String bioTransaction = csvReader.get("TRANSACTION");
								if(bioTransaction.isEmpty()){
									StringBuilder errorString = new StringBuilder();
									errorString.append("Error: Row ");
									errorString.append(row);
									errorString.append(": SubjectUID: ");
									errorString.append(subjectUID);
									errorString.append(" The biotransaction ");
									errorString.append(" of BiospecimenUID: ");
									errorString.append(biospecimenUID);
									errorString.append(" is not specified. Please check and try again");
									dataValidationMessages.add(errorString.toString());
									errorCells.add(new ArkGridCell(csvReader.getIndex("TRANSACTION"), row));
									insertThisRow = false;//drop out also?
									log.error("Amt used is empty");
								}else{
									if(!bioTransaction.equalsIgnoreCase("Processed")||!bioTransaction.equalsIgnoreCase("Aliquoted")){
										StringBuilder errorString = new StringBuilder();
										errorString.append("Error: Row ");
										errorString.append(row);
										errorString.append(": SubjectUID: ");
										errorString.append(subjectUID);
										errorString.append(" The biotransaction ");
										errorString.append(bioTransaction.toString());
										errorString.append(" of Biospecimen: ");
										errorString.append(biospecimenUID);
										errorString.append(" is not a valid entry. Please check and try again.");
										dataValidationMessages.add(errorString.toString());
										errorCells.add(new ArkGridCell(csvReader.getIndex("TRANSACTION"), row));
										insertThisRow = false;//drop out also?
									}
								}
							}
							biocollection = parentBiospecimen.getBioCollection();
							ageAtCollection = String.valueOf(biocollection.getPatientAge());
						}
					}				
				} else {
					
					//BIOCOLLECTION
					if (biocollectionUID == null  || biocollectionUID.isEmpty() ) {
						if(study.getAutoGenerateBiocollectionUid()){
							//insertRows.add(row);								
						} else {
							StringBuilder errorString = new StringBuilder();
							errorString.append("Error: Row ");
							errorString.append(row);
							errorString.append(": SubjectUID: ");
							errorString.append(subjectUID);
							errorString.append(".  You have not specified a biocollection UID, yet your study is not set up" +
									" to auto generate biocollection UIDs.  Please specify a unique ID.");
							dataValidationMessages.add(errorString.toString());
							errorCells.add(new ArkGridCell(csvReader.getIndex("BIOCOLLECTIONUID"), row));
							insertThisRow = false;//drop out also?
						}
					} else {
						biocollection = iLimsService.getBioCollectionByUID(biocollectionUID,study.getId(), subjectUID);

						if(study.getAutoGenerateBiocollectionUid()){//ie; auto gen, id supplied.
							if(biocollection==null){
								StringBuilder errorString = new StringBuilder();
								errorString.append("Error: Row ");
								errorString.append(row);
								errorString.append(": SubjectUID: ");
								errorString.append(subjectUID);
								errorString.append(": BiocollectionUID: ");
								errorString.append(biocollectionUID);
								errorString.append(".  You have specified a non-existant Biocollection UID, yet your study is set up" +
									" to auto generate biocollection UIDs.  Check the Biocollection UID if you intended to relate it " +
									"to a Biocollection, otherwise remove the Biocollection UID if you wish to generate a new Biocollection");
								dataValidationMessages.add(errorString.toString());
								errorCells.add(new ArkGridCell(csvReader.getIndex("BIOCOLLECTIONUID"), row));
								insertThisRow = false;//drop out also?
							} else {
									//insertRows.add(row);
							}
						} else {//ie; not auto gen, id supplied.
							if(biocollection == null){
								//insertRows.add(row);	//this instance will need biocol created
							} else {
									//insertRows.add(row);  //this istance will use the provided biocol.
							}
						}
					}
					
					//AGEATCOLLECTION
					if (csvReader.getIndex("AGEATCOLLECTION") > 0) {
						ageAtCollection = csvReader.get("AGEATCOLLECTION");
						
						if (ageAtCollection.isEmpty()) {
							StringBuilder errorString = new StringBuilder();
							errorString.append("Error: Row ");
							errorString.append(row);
							errorString.append(": SubjectUID: ");
							errorString.append(subjectUID);
							errorString.append(" The age at collection) ");
							errorString.append(" of BiocollectionUID: ");
							errorString.append(biocollectionUID);
							errorString.append(" is required.");
							errorString.append(" Please enter a valid number.");
							dataValidationMessages.add(errorString.toString());
							errorCells.add(new ArkGridCell(csvReader.getIndex("AGEATCOLLECTION"), row));
							insertThisRow = false;//drop out also?
						}else {
							try{
								Integer.parseInt(ageAtCollection);
							}catch(NumberFormatException ne){
								StringBuilder errorString = new StringBuilder();
								errorString.append("Error: Row ");
								errorString.append(row);
								errorString.append(": SubjectUID: ");
								errorString.append(subjectUID);
								errorString.append(" The age at collection ");
								errorString.append(ageAtCollection);
								errorString.append(" of BiocollectionUID: ");
								errorString.append(biocollectionUID);
								errorString.append(" is not a valid number.");
								dataValidationMessages.add(errorString.toString());
								errorCells.add(new ArkGridCell(csvReader.getIndex("AGEATCOLLECTION"), row));
								insertThisRow = false;//drop out also?
							}
						}
					}
					
					//BIOCOLLECTIONNAME 
					
					//BIOCOLLECTIONDATE
					if (csvReader.getIndex("BIOCOLLECTIONDATE") > 0) {
						String bioCollectionDate = csvReader.get("BIOCOLLECTIONDATE");
						
						if (bioCollectionDate == null || bioCollectionDate.isEmpty()) {
							StringBuilder errorString = new StringBuilder();
							errorString.append("Error: Row ");
							errorString.append(row);
							errorString.append(": SubjectUID: ");
							errorString.append(subjectUID);
							errorString.append(" The collection date of BiospecimenUID:");
							errorString.append(biospecimenUID);
							errorString.append(" is required. Please enter a valid date.");
							dataValidationMessages.add(errorString.toString());
							errorCells.add(new ArkGridCell(csvReader.getIndex("BIOCOLLECTIONDATE"), row));
							insertThisRow = false;//drop out also?
						}else{
							try{
								simpleDateFormat.parse(bioCollectionDate);
							}catch(ParseException pex){
								StringBuilder errorString = new StringBuilder();
								errorString.append("Error: Row ");
								errorString.append(row);
								errorString.append(": SubjectUID: ");
								errorString.append(subjectUID);
								errorString.append(" The collection date ");
								errorString.append(bioCollectionDate);
								errorString.append(" of BiospecimenUID: ");
								errorString.append(biospecimenUID);
								errorString.append(" is not valid date. Please check and try again");
								dataValidationMessages.add(errorString.toString());
								errorCells.add(new ArkGridCell(csvReader.getIndex("BIOCOLLECTIONDATE"), row));
								insertThisRow = false;//drop out also?	
							}
						}						
					}
					
					//BIOCOLLECTIONTIME
					if (csvReader.getIndex("BIOCOLLECTIONTIME") > 0) {
						String biocollectionTime = csvReader.get("BIOCOLLECTIONTIME");
						
						if (biocollectionTime.isEmpty()) {
							//not Mandatory
						}else{
							try{
								Date date = simpleTimeFormat.parse(biocollectionTime.toString());
								//timeStamp = new Timestamp(date.getTime());
							}catch(ParseException pex){
								StringBuilder errorString = new StringBuilder();
								errorString.append("Error: Row ");
								errorString.append(row);
								errorString.append(": SubjectUID: ");
								errorString.append(subjectUID);
								errorString.append(" The biocollection time ");
								errorString.append(biocollectionTime);
								errorString.append(" of BiospecimenUID: ");
								errorString.append(biospecimenUID);
								errorString.append(" is not valid time. Please check and try again");
								dataValidationMessages.add(errorString.toString());
								errorCells.add(new ArkGridCell(csvReader.getIndex("BIOCOLLECTIONTIME"), row));
								insertThisRow = false;//drop out also?	
							}
						}						
					}
				}
					
				//SUBJECT
				LinkSubjectStudy linkSubjectStudy = (iArkCommonService.getSubjectByUIDAndStudy(subjectUID, study));
				if(linkSubjectStudy==null){
					StringBuilder errorString = new StringBuilder();
					errorString.append("Error: Row ");
					errorString.append(row);
					errorString.append(": SubjectUID: ");
					errorString.append(subjectUID);
					errorString.append(" does not exist.  Please check this Subject UID and try again.");
					dataValidationMessages.add(errorString.toString());
					errorCells.add(new ArkGridCell(csvReader.getIndex("SUBJECTUID"), row));
					insertThisRow = false;//drop out also?
					recordCount++;
					row++;
					break;
				}
				
				if(linkSubjectStudy.getStudy()==null){
					linkSubjectStudy.setStudy(study);
				}
				
				/*TODO ASAP 
				 * 
				 * once logic laid out
				 * ...make sure you just break out of while on the first error 
				 * ...change insertRow stuff to say rowIsOk = true/false
				 * ....once we pass all tests THEN if(rowIsOK) THEN insertRows.add(row)
				 * */	
				
				
				// Check for unique BiospecimenUIDs
				if(!study.getAutoGenerateBiospecimenUid()){
					if(!uniqueSubjectBiospecimens.contains(biospecimenUID)) {
						uniqueSubjectBiospecimens.add(biospecimenUID);
					}
					else {
						StringBuilder errorString = new StringBuilder();
						errorString.append("Error: Row ");
						errorString.append(row);
						errorString.append(": SubjectUID: ");
						errorString.append(subjectUID);
						errorString.append(".  BiospecimeUIDs must be unique. Please check the file and try again.");
						dataValidationMessages.add(errorString.toString());
						errorCells.add(new ArkGridCell(csvReader.getIndex("BIOSPECIMENUID"), row));
						insertThisRow = false;//drop out also?
					}
				}
				
				if (biospecimenUID == null || biospecimenUID.isEmpty() ) {
					if(study.getAutoGenerateBiospecimenUid()){
						//insertRows.add(row);								
					}
					else{
						StringBuilder errorString = new StringBuilder();
						errorString.append("Error: Row ");
						errorString.append(row);
						errorString.append(": SubjectUID: ");
						errorString.append(subjectUID);
						errorString.append(".  You have not specified a Biospecimen UID, yet your study is not set up" +
								" to auto generate Biospecimen UIDs.  Please specify a unique ID.");
						dataValidationMessages.add(errorString.toString());
						errorCells.add(new ArkGridCell(csvReader.getIndex("BIOSPECIMENUID"), row));
						insertThisRow = false;//drop out also?
					}
				}
				else {
					if(study.getAutoGenerateBiospecimenUid()){
						StringBuilder errorString = new StringBuilder();
						errorString.append("Error: Row ");
						errorString.append(row);
						errorString.append(": SubjectUID: ");
						errorString.append(subjectUID);
						errorString.append(": BIOSPECIMENUID: ");
						errorString.append(biospecimenUID);
						errorString.append(".  You have specified a Biospecimen UID, yet your study is set up" +
								" to auto generate Biospecimen UIDs.");
						dataValidationMessages.add(errorString.toString());
						errorCells.add(new ArkGridCell(csvReader.getIndex("BIOSPECIMENUID"), row));
						insertThisRow = false;//drop out also?
					}
					else{
						biospecimen = iLimsService.getBiospecimenByUid(biospecimenUID,study);
						if(biospecimen == null){
							insertRows.add(row);								
						}
						else{
							StringBuilder errorString = new StringBuilder();
							errorString.append("Error: Row ");
							errorString.append(row);
							errorString.append(": SubjectUID: ");
							errorString.append(subjectUID);
							errorString.append(": BiospecimenUID: ");
							errorString.append(biospecimenUID);
							errorString.append(". You have specified an existing BiospecimenUID. This uploader creates new " +
									"Biospecimens, but does not update existing Biospecimens. Please remove this row");
							dataValidationMessages.add(errorString.toString());
							errorCells.add(new ArkGridCell(csvReader.getIndex("BIOSPECIMENUID"), row));
							insertThisRow = false;//drop out also?
						}
					}
				}
				
				// Check for unique BiospecimenUIDs
				if(study.getAutoGenerateBiospecimenUid()){
					if(uniqueSubjectBiocollections.get(biocollectionUID) == null) {
						uniqueSubjectBiocollections.put(biocollectionUID, subjectUID);
					}
						else {
						if(!uniqueSubjectBiocollections.get(biocollectionUID).equals(subjectUID)) {
							StringBuilder errorString = new StringBuilder();
							errorString.append("Error: Row ");
							errorString.append(row);
							errorString.append(": SubjectUID: ");
							errorString.append(subjectUID);
							errorString.append(": Bicollection UID: ");
							errorString.append(biocollectionUID);
							errorString.append(". Is already assigned to SubjectUID: ");
							errorString.append(uniqueSubjectBiocollections.get(biocollectionUID));
							errorString.append(" Please amend the file and try again.");
							dataValidationMessages.add(errorString.toString());
							errorCells.add(new ArkGridCell(csvReader.getIndex("BIOCOLLECTIONUID"), row));
							insertThisRow = false;//drop out also?
						}
					}
				}
								
				//SAMPLETYPE 
				if (csvReader.getIndex("SAMPLETYPE") > 0) {
					String name = csvReader.get("SAMPLETYPE");
					BioSampletype sampleType = new BioSampletype();
					sampleType = iLimsService.getBioSampleTypeByName(name);
					if (sampleType == null) {
						StringBuilder errorString = new StringBuilder();
						errorString.append("Error: Row ");
						errorString.append(row);
						errorString.append(": SubjectUID: ");
						errorString.append(subjectUID);
						errorString.append(" The sample type ");
						errorString.append(name);
						errorString.append(" of BiospecimenUID: ");
						errorString.append(biospecimenUID);
						errorString.append(" do not match the details in the database. Please check and try again");
						dataValidationMessages.add(errorString.toString());
						errorCells.add(new ArkGridCell(csvReader.getIndex("SAMPLETYPE"), row));
						insertThisRow = false;//drop out also?
					}
				}
									
				//PROCESSDATE
				if (csvReader.getIndex("PROCESSDATE") > 0) {
					String processDate = csvReader.get("PROCESSDATE");
					log.error(processDate.toString());
					if(!csvReader.get("PARENTID").isEmpty()){
						if (processDate == null || processDate.isEmpty()) {
							StringBuilder errorString = new StringBuilder();
							errorString.append("Error: Row ");
							errorString.append(row);
							errorString.append(": SubjectUID: ");
							errorString.append(subjectUID);
							errorString.append(" The processing date of BiospecimenUID:");
							errorString.append(biospecimenUID);
							errorString.append(" is required. Please enter a valid date.");
							dataValidationMessages.add(errorString.toString());
							errorCells.add(new ArkGridCell(csvReader.getIndex("PROCESSDATE"), row));
							insertThisRow = false;//drop out also?
						}else{
							try{
								simpleDateFormat.parse(processDate);
							}catch(ParseException pex){
								StringBuilder errorString = new StringBuilder();
								errorString.append("Error: Row ");
								errorString.append(row);
								errorString.append(": SubjectUID: ");
								errorString.append(subjectUID);
								errorString.append(" The processing date ");
								errorString.append(processDate);
								errorString.append(" of BiospecimenUID: ");
								errorString.append(biospecimenUID);
								errorString.append(" is not valid date. Please check and try again");
								dataValidationMessages.add(errorString.toString());
								errorCells.add(new ArkGridCell(csvReader.getIndex("PROCESSDATE"), row));
								insertThisRow = false;//drop out also?	
							}
						}
					}
				}
				
				//PROCESSTIME
				if (csvReader.getIndex("PROCESSTIME") > 0) {
					String processTime = csvReader.get("PROCESSTIME");
					
					if (processTime == null || processTime.isEmpty()) {
						/*StringBuilder errorString = new StringBuilder();
						errorString.append("Error: Row ");
						errorString.append(row);
						errorString.append(": SubjectUID: ");
						errorString.append(subjectUID);
						errorString.append(" The processing time of BiospecimenUID:");
						errorString.append(biospecimenUID);
						errorString.append(" is empty. Please check and try again");
						dataValidationMessages.add(errorString.toString());
						errorCells.add(new ArkGridCell(csvReader.getIndex("PROCESSDATE"), row));
						insertThisRow = false;//drop out also?
*/					}else{
						try{
							
							Date date = simpleTimeFormat.parse(processTime.toString());
							//timeStamp = new Timestamp(date.getTime());
							//Date date = simpleDateFormat.parse(processTime);
							//timeStamp = new Timestamp(date.getTime());
						}catch(ParseException pex){
							StringBuilder errorString = new StringBuilder();
							errorString.append("Error: Row ");
							errorString.append(row);
							errorString.append(": SubjectUID: ");
							errorString.append(subjectUID);
							errorString.append(" The processing time ");
							errorString.append(processTime);
							errorString.append(" of BiospecimenUID: ");
							errorString.append(biospecimenUID);
							errorString.append(" is not valid time. Please check and try again");
							dataValidationMessages.add(errorString.toString());
							errorCells.add(new ArkGridCell(csvReader.getIndex("PROCESSTIME"), row));
							insertThisRow = false;//drop out also?	
						}
					}						
				}
				
				//COMMENTS
				
				//BARCODED
				if (csvReader.getIndex("BARCODED") > 0) {
					String barcoded = csvReader.get("BARCODED");
					
					if (barcoded != null || !barcoded.isEmpty()) {
						if (!DataConversionAndManipulationHelper.isSomethingLikeABoolean(barcoded)){
							StringBuilder errorString = new StringBuilder();
							errorString.append("Error: Row ");
							errorString.append(row);
							errorString.append(": SubjectUID: ");
							errorString.append(subjectUID);
							errorString.append(" The barcoded value ");
							errorString.append(barcoded);
							errorString.append(" of BiospecimenUID: ");
							errorString.append(biospecimenUID);
							errorString.append(" is not a valid boolean value.");
							errorString.append(" Please use true or false for this column.");
							dataValidationMessages.add(errorString.toString());
							errorCells.add(new ArkGridCell(csvReader.getIndex("BARCODED"), row));
							insertThisRow = false;//drop out also?
						}
					}						
				}
				
				
				//STOREDIN
				if (csvReader.getIndex("STOREDIN") > 0) {
					String storedIn = csvReader.get("STOREDIN");
								
					if (storedIn != null || !storedIn.isEmpty()) {
						BiospecimenStorage biospecimenStorage = iLimsService.getBiospecimenStorageByName(storedIn);
						if(biospecimenStorage==null){
							StringBuilder errorString = new StringBuilder();
							errorString.append("Error: Row ");
							errorString.append(row);
							errorString.append(": SubjectUID: ");
							errorString.append(subjectUID);
							errorString.append(" The storage name ");
							errorString.append(biospecimenStorage);
							errorString.append(" of BiospecimenUID: ");
							errorString.append(biospecimenUID);
							errorString.append(" is not a valid storage name.");
							errorString.append(" Please use a valid storage name.");
							dataValidationMessages.add(errorString.toString());
							errorCells.add(new ArkGridCell(csvReader.getIndex("STOREDIN"), row));
							insertThisRow = false;//drop out also?
						}
					}else{
						StringBuilder errorString = new StringBuilder();
						errorString.append("Error: Row ");
						errorString.append(row);
						errorString.append(": SubjectUID: ");
						errorString.append(subjectUID);
						errorString.append(" The storage name ");
						errorString.append(" of BiospecimenUID: ");
						errorString.append(biospecimenUID);
						errorString.append(" is required.");
						errorString.append(" Please use a valid storage name.");
						dataValidationMessages.add(errorString.toString());
						errorCells.add(new ArkGridCell(csvReader.getIndex("STOREDIN"), row));
						insertThisRow = false;//drop out also?
					}
				}						
							
				//ANTICOAGULANTTYPE
				if (csvReader.getIndex("ANTICOAGULANTTYPE") > 0) {
					String anticoag = csvReader.get("ANTICOAGULANTTYPE");
								
					if (anticoag != null || !anticoag.isEmpty()) {
						BiospecimenAnticoagulant biospecimenAnticoagulant = iLimsService.getBiospecimenAnticoagulantByName(anticoag);
						if(biospecimenAnticoagulant==null){
							StringBuilder errorString = new StringBuilder();
							errorString.append("Error: Row ");
							errorString.append(row);
							errorString.append(": SubjectUID: ");
							errorString.append(subjectUID);
							errorString.append(" The anticoagulant type ");
							errorString.append(anticoag);
							errorString.append(" of BiospecimenUID: ");
							errorString.append(biospecimenUID);
							errorString.append(" is not a valid anticoagulant type.");
							errorString.append(" Please use a valid anticoagulant type.");
							dataValidationMessages.add(errorString.toString());
							errorCells.add(new ArkGridCell(csvReader.getIndex("ANTICOAGULANTTYPE"), row));
							insertThisRow = false;//drop out also?
						}
					}else{
						StringBuilder errorString = new StringBuilder();
						errorString.append("Error: Row ");
						errorString.append(row);
						errorString.append(": SubjectUID: ");
						errorString.append(subjectUID);
						errorString.append(" The anticoagulant type");
						errorString.append(" of BiospecimenUID: ");
						errorString.append(biospecimenUID);
						errorString.append(" is required.");
						errorString.append(" Please use a valid anticoagulant type.");
						dataValidationMessages.add(errorString.toString());
						errorCells.add(new ArkGridCell(csvReader.getIndex("ANTICOAGULANTTYPE"), row));
						insertThisRow = false;//drop out also?
					}
				}		
				
				//STATUS
				if (csvReader.getIndex("STATUS") > 0) {
					String status = csvReader.get("STATUS");
								
					if (status != null || !status.isEmpty()) {
						 BiospecimenStatus biospecimenStatus = iLimsService.getBiospecimenStatusByName(status);
						if(biospecimenStatus==null){
							StringBuilder errorString = new StringBuilder();
							errorString.append("Error: Row ");
							errorString.append(row);
							errorString.append(": SubjectUID: ");
							errorString.append(subjectUID);
							errorString.append(" The biospecimen status ");
							errorString.append(status);
							errorString.append(" of BiospecimenUID: ");
							errorString.append(biospecimenUID);
							errorString.append(" is not a valid biospecimen status.");
							errorString.append(" Please use a valid biospecimen status.");
							dataValidationMessages.add(errorString.toString());
							errorCells.add(new ArkGridCell(csvReader.getIndex("STATUS"), row));
							insertThisRow = false;//drop out also?
						}
					}else{
						StringBuilder errorString = new StringBuilder();
						errorString.append("Error: Row ");
						errorString.append(row);
						errorString.append(": SubjectUID: ");
						errorString.append(subjectUID);
						errorString.append(" The biospecimen status");
						errorString.append(" of BiospecimenUID: ");
						errorString.append(biospecimenUID);
						errorString.append(" is required.");
						errorString.append(" Please use a valid biospecimen status.");
						dataValidationMessages.add(errorString.toString());
						errorCells.add(new ArkGridCell(csvReader.getIndex("STATUS"), row));
						insertThisRow = false;//drop out also?
					}
				}		
				
				//PROTOCOL
				/*if (csvReader.getIndex("PROTOCOL") > 0) {
					String protocol = csvReader.get("PROTOCOL");
								
					if (protocol!=null || !protocol.isEmpty()) {
						 BiospecimenProtocol biospecimenProtocol = iLimsService.getBiospecimenProtocolByName(protocol);
						if(biospecimenProtocol==null){
							StringBuilder errorString = new StringBuilder();
							errorString.append("Error: Row ");
							errorString.append(row);
							errorString.append(": SubjectUID: ");
							errorString.append(subjectUID);
							errorString.append(" The protocol ");
							errorString.append(protocol);
							errorString.append(" of BiospecimenUID: ");
							errorString.append(biospecimenUID);
							errorString.append(" is not a valid .");
							errorString.append(" Please use a valid protocol.");
							dataValidationMessages.add(errorString.toString());
							errorCells.add(new ArkGridCell(csvReader.getIndex("PROTOCOL"), row));
							insertThisRow = false;//drop out also?
						}
					}else{
						StringBuilder errorString = new StringBuilder();
						errorString.append("Error: Row ");
						errorString.append(row);
						errorString.append(": SubjectUID: ");
						errorString.append(subjectUID);
						errorString.append(" The protocol");
						errorString.append(" of BiospecimenUID: ");
						errorString.append(biospecimenUID);
						errorString.append(" is required.");
						errorString.append(" Please use a valid protocol.");
						dataValidationMessages.add(errorString.toString());
						errorCells.add(new ArkGridCell(csvReader.getIndex("PROTOCOL"), row));
						insertThisRow = false;//drop out also?
					}
				}*/		
				
				//These values are only checked for DNA samples
				if (csvReader.get("SAMPLETYPE")=="Nucleic Acid / DNA"){
					
					//GRADE
					if (csvReader.getIndex("GRADE") > 0) {
						String grade = csvReader.get("GRADE");
								
						if (grade!= null || !grade.isEmpty()) {
							BiospecimenGrade biospecimenGrade = iLimsService.getBiospecimenGradeByName(grade);
							
							if(biospecimenGrade==null){
								StringBuilder errorString = new StringBuilder();
								errorString.append("Error: Row ");
								errorString.append(row);
								errorString.append(": SubjectUID: ");
								errorString.append(subjectUID);
								errorString.append(" The grade ");
								errorString.append(biospecimenGrade);
								errorString.append(" of BiospecimenUID: ");
								errorString.append(biospecimenUID);
								errorString.append(" is not a valid grade.");
								errorString.append(" Please use a valid grade.");
								dataValidationMessages.add(errorString.toString());
								errorCells.add(new ArkGridCell(csvReader.getIndex("GRADE"), row));
								insertThisRow = false;//drop out also?
							}
						}else{
							StringBuilder errorString = new StringBuilder();
							errorString.append("Error: Row ");
							errorString.append(row);
							errorString.append(": SubjectUID: ");
							errorString.append(subjectUID);
							errorString.append(" The grade");
							errorString.append(" of BiospecimenUID: ");
							errorString.append(biospecimenUID);
							errorString.append(" is required.");
							errorString.append(" Please use a valid grade.");
							dataValidationMessages.add(errorString.toString());
							errorCells.add(new ArkGridCell(csvReader.getIndex("GRADE"), row));
							insertThisRow = false;//drop out also?
						}
					}
					
					//PURITY280
					if (csvReader.getIndex("PURITY280") > 0) {
						String purity280 = csvReader.get("PURITY280");
						
						if (purity280.isEmpty()) {
							StringBuilder errorString = new StringBuilder();
							errorString.append("Error: Row ");
							errorString.append(row);
							errorString.append(": SubjectUID: ");
							errorString.append(subjectUID);
							errorString.append(" The purity (260/280) ");
							errorString.append(" of BiospecimenUID: ");
							errorString.append(biospecimenUID);
							errorString.append(" is required.");
							errorString.append(" Please enter a valid number.");
							dataValidationMessages.add(errorString.toString());
							errorCells.add(new ArkGridCell(csvReader.getIndex("PURITY280"), row));
							insertThisRow = false;//drop out also?
						}else {
							try{
								Double.parseDouble(purity280);
							}catch(NumberFormatException ne){
								StringBuilder errorString = new StringBuilder();
								errorString.append("Error: Row ");
								errorString.append(row);
								errorString.append(": SubjectUID: ");
								errorString.append(subjectUID);
								errorString.append(" The quantity ");
								errorString.append(purity280);
								errorString.append(" of BiospecimenUID: ");
								errorString.append(biospecimenUID);
								errorString.append(" is not a valid number.");
								dataValidationMessages.add(errorString.toString());
								errorCells.add(new ArkGridCell(csvReader.getIndex("PURITY280"), row));
								insertThisRow = false;//drop out also?
							}
						}
					}
				
				//PURITY230
				if (csvReader.getIndex("PURITY230") > 0) {
						String purity230 = csvReader.get("PURITY230");
						
						if (purity230.isEmpty()) {
							StringBuilder errorString = new StringBuilder();
							errorString.append("Error: Row ");
							errorString.append(row);
							errorString.append(": SubjectUID: ");
							errorString.append(subjectUID);
							errorString.append(" The purity (260/230) ");
							errorString.append(" of BiospecimenUID: ");
							errorString.append(biospecimenUID);
							errorString.append(" is required.");
							errorString.append(" Please enter a valid number.");
							dataValidationMessages.add(errorString.toString());
							errorCells.add(new ArkGridCell(csvReader.getIndex("PURITY230"), row));
							insertThisRow = false;//drop out also?
						}else {
							try{
								Double.parseDouble(purity230);
							}catch(NumberFormatException ne){
								StringBuilder errorString = new StringBuilder();
								errorString.append("Error: Row ");
								errorString.append(row);
								errorString.append(": SubjectUID: ");
								errorString.append(subjectUID);
								errorString.append(" The quantity ");
								errorString.append(purity230);
								errorString.append(" of BiospecimenUID: ");
								errorString.append(biospecimenUID);
								errorString.append(" is not a valid number.");
								dataValidationMessages.add(errorString.toString());
								errorCells.add(new ArkGridCell(csvReader.getIndex("PURITY230"), row));
								insertThisRow = false;//drop out also?
							}
						}
					}
				
					//CONCENTRATION
					if (csvReader.getIndex("CONCENTRATION") > 0) {
						String concentrationString = csvReader.get("CONCENTRATION");
						if (concentrationString != null && !concentrationString.isEmpty()) {
							try{
								Double.parseDouble(concentrationString);
							}
							catch(NumberFormatException ne){
								StringBuilder errorString = new StringBuilder();
								errorString.append("Error: Row ");
								errorString.append(row);
								errorString.append(": SubjectUID: ");
								errorString.append(subjectUID);
								errorString.append(" The concentration ");
								errorString.append(concentrationString);
								errorString.append(" of BiospecimenUID: ");
								errorString.append(biospecimenUID);
								errorString.append(" is not a valid number.");
								dataValidationMessages.add(errorString.toString());
								errorCells.add(new ArkGridCell(csvReader.getIndex("CONCENTRATION"), row));
								insertThisRow = false;//drop out also?
							}
						
						}
					}
				}
					
				//QUALITY
				if (csvReader.getIndex("QUALITY") > 0) {
					String quality = csvReader.get("QUALITY");
								
					if (quality != null || !quality.isEmpty()) {
						BiospecimenQuality biospecimenQuality = iLimsService.getBiospecimenQualityByName(quality);
						if(biospecimenQuality==null){
							StringBuilder errorString = new StringBuilder();
							errorString.append("Error: Row ");
							errorString.append(row);
							errorString.append(": SubjectUID: ");
							errorString.append(subjectUID);
							errorString.append(" The biospecimen quality ");
							errorString.append(quality);
							errorString.append(" of BiospecimenUID: ");
							errorString.append(biospecimenUID);
							errorString.append(" is not a valid .");
							errorString.append(" Please use a valid biospecimen quality.");
							dataValidationMessages.add(errorString.toString());
							errorCells.add(new ArkGridCell(csvReader.getIndex("QUALITY"), row));
							insertThisRow = false;//drop out also?
						}
					}else{
						StringBuilder errorString = new StringBuilder();
						errorString.append("Error: Row ");
						errorString.append(row);
						errorString.append(": SubjectUID: ");
						errorString.append(subjectUID);
						errorString.append(" The biospecimen quality");
						errorString.append(" of BiospecimenUID: ");
						errorString.append(biospecimenUID);
						errorString.append(" is required.");
						errorString.append(" Please use a valid biospecimen quality.");
						dataValidationMessages.add(errorString.toString());
						errorCells.add(new ArkGridCell(csvReader.getIndex("QUALITY"), row));
						insertThisRow = false;//drop out also?
					}
				}
				
				//QUANTITY
				if (csvReader.getIndex("QUANTITY") > 0) {
					String quantityString = csvReader.get("QUANTITY");
					if (quantityString.isEmpty()) {
						StringBuilder errorString = new StringBuilder();
						errorString.append("Error: Row ");
						errorString.append(row);
						errorString.append(": SubjectUID: ");
						errorString.append(subjectUID);
						errorString.append(" The quantity ");
						errorString.append(" of BiospecimenUID: ");
						errorString.append(biospecimenUID);
						errorString.append(" is required.");
						errorString.append(" Please enter a valid number.");
						dataValidationMessages.add(errorString.toString());
						errorCells.add(new ArkGridCell(csvReader.getIndex("QUANTITY"), row));
						insertThisRow = false;//drop out also?
					}
					else {
						try{
							Double.parseDouble(quantityString);
						}
						catch(NumberFormatException ne){
							StringBuilder errorString = new StringBuilder();
							errorString.append("Error: Row ");
							errorString.append(row);
							errorString.append(": SubjectUID: ");
							errorString.append(subjectUID);
							errorString.append(" The quantity ");
							errorString.append(quantityString);
							errorString.append(" of BiospecimenUID: ");
							errorString.append(biospecimenUID);
							errorString.append(" is not a valid number.");
							dataValidationMessages.add(errorString.toString());
							errorCells.add(new ArkGridCell(csvReader.getIndex("QUANTITY"), row));
							insertThisRow = false;//drop out also?
						}
					}
				}
				
				//UNITS
				if (csvReader.getIndex("UNITS") > 0) {
					String name = csvReader.get("UNITS");
					Unit unit = iLimsService.getUnitByName(name);
					if (unit == null) {
						StringBuilder errorString = new StringBuilder();
						errorString.append("Error: Row ");
						errorString.append(row);
						errorString.append(": SubjectUID: ");
						errorString.append(subjectUID);
						errorString.append(" The unit ");
						errorString.append(name);
						errorString.append(" of BiospecimenUID: ");
						errorString.append(biospecimenUID);
						errorString.append(" do not match the details in the database. Please check and try again");
						dataValidationMessages.add(errorString.toString());
						errorCells.add(new ArkGridCell(csvReader.getIndex("UNITS"), row));
						insertThisRow = false;//drop out also?
					}
				}
				//TREATMENT
				if (csvReader.getIndex("TREATMENT") > 0) {
					String name = csvReader.get("TREATMENT");
					TreatmentType treatmentType = new TreatmentType(); 
					treatmentType = iLimsService.getTreatmentTypeByName(name);
					if (treatmentType == null) {
						StringBuilder errorString = new StringBuilder();
						errorString.append("Error: Row ");
						errorString.append(row);
						errorString.append(": SubjectUID: ");
						errorString.append(subjectUID);
						errorString.append(" The treatment ");
						errorString.append(name);
						errorString.append(" of BiospecimenUID: ");
						errorString.append(biospecimenUID);
						errorString.append(" do not match the details in the database. Please check and try again");
						dataValidationMessages.add(errorString.toString());
						errorCells.add(new ArkGridCell(csvReader.getIndex("TREATMENT"), row));
						insertThisRow = false;//drop out also?
					}
				}
				
				//SITE
				String site = csvReader.get("SITE");
				
				//FREEZER
				String freezer = csvReader.get("FREEZER");
				
				//SHELF
				String shelf = csvReader.get("SHELF");
				
				//RACK
				String rack = csvReader.get("RACK");
				
				//BOX
				String box = csvReader.get("BOX");
				
				//ROW
				String rowString = csvReader.get("ROW");
				
				//COLUMN
				String columnString = csvReader.get("COLUMN");
				
				if ((site == null && freezer == null && shelf == null && rack == null && box == null && rowString == null && columnString == null) ||
					(site.isEmpty() && freezer.isEmpty() && shelf == null && rack.isEmpty() && box.isEmpty() && rowString.isEmpty() && columnString.isEmpty())	
					){
						//ie; EVERYTHING IS EMPTY...in which case, we let you go ahead and don't specify where the biospec lives
						log.debug("EVERYTHING is empty so we still create it...we just don't put it somewhere");
				} else if ((site != null && freezer != null && shelf != null && rack != null && 
						box != null && rowString != null && columnString != null) &&
					(!site.isEmpty() && !freezer.isEmpty() && !shelf.isEmpty() && !rack.isEmpty() &&
						!box.isEmpty() && !rowString.isEmpty() && !columnString.isEmpty())	
				) {
					InvCell invCell = iInventoryService.getInvCellByLocationNames(site, freezer, shelf, rack, box, rowString, columnString);
					
					if (invCell == null) {
						StringBuilder errorString = new StringBuilder();
						errorString.append("Error: Row ");
						errorString.append(row);
						errorString.append(": SubjectUID: ");
						errorString.append(subjectUID);
						errorString.append(" The location details provided for BiospecimenUID: ");
						errorString.append(biospecimenUID);
						errorString.append(" do not match the details in the database. Please check and try again");
						dataValidationMessages.add(errorString.toString());
						errorCells.add(new ArkGridCell(csvReader.getIndex("SITE"), row));
						errorCells.add(new ArkGridCell(csvReader.getIndex("FREEZER"), row));
						errorCells.add(new ArkGridCell(csvReader.getIndex("SHELF"), row));
						errorCells.add(new ArkGridCell(csvReader.getIndex("RACK"), row));
						errorCells.add(new ArkGridCell(csvReader.getIndex("BOX"), row));
						errorCells.add(new ArkGridCell(csvReader.getIndex("ROW"), row));
						errorCells.add(new ArkGridCell(csvReader.getIndex("COLUMN"), row));
						insertThisRow = false;//drop out also?
					}else{
						//check ownership of the cell.  
						//if nothing is there - put this spec there
						if(invCell.getBiospecimen()==null) {
							//invCell.setBiospecimen(biospecimen); is what we will do next step
							//updateRows.add(row);
						} else if(invCell.getBiospecimen().getBiospecimenUid()==parentBiospecimen.getBiospecimenUid() && (parentBiospecimen.getQuantity()==Double.valueOf(csvReader.get("AMOUNTUSED")))){
							
						} else {
							StringBuilder errorString = new StringBuilder();
							errorString.append("Error: Row ");
							errorString.append(row);
							errorString.append(": biospecimen UID: ");
							errorString.append(biospecimenUID);
							errorString.append(" cannot be placed at this cell location as there is already something there (" + invCell.getBiospecimen().getBiospecimenUid() + ").  Please check this UID or location and try again.");
							dataValidationMessages.add(errorString.toString());
							errorCells.add(new ArkGridCell(csvReader.getIndex("ROW"), row));
							errorCells.add(new ArkGridCell(csvReader.getIndex("COLUMN"), row));
							insertThisRow = false;//drop out also?
						}
					}
					
				}
				else{
					//else you have incomplete info, and we aren't cool with that;
					StringBuilder errorString = new StringBuilder();
					errorString.append("Error: Row ");
					errorString.append(row);
					errorString.append(": SubjectUID: ");
					errorString.append(subjectUID);
					errorString.append(" The location details provided for BiospecimenUID: ");
					errorString.append(biospecimenUID);
					errorString.append(" are incomplete. Please ensure you either provide all of the following information \"SITE, FREEZER, RACK, BOX, ROW and COLUMN\" or none of these ");
					dataValidationMessages.add(errorString.toString());
					errorCells.add(new ArkGridCell(csvReader.getIndex("SITE"), row));
					errorCells.add(new ArkGridCell(csvReader.getIndex("FREEZER"), row));
					errorCells.add(new ArkGridCell(csvReader.getIndex("RACK"), row));
					errorCells.add(new ArkGridCell(csvReader.getIndex("BOX"), row));
					errorCells.add(new ArkGridCell(csvReader.getIndex("ROW"), row));
					errorCells.add(new ArkGridCell(csvReader.getIndex("COLUMN"), row));
					insertThisRow = false;//drop out also?
					
				}
			
				if(insertThisRow){
					int col = 0;
					String dateStr = new String();
	
					if (csvReader.getIndex("SAMPLEDATE") > 0 || csvReader.getIndex("SAMPLE_DATE") > 0) {
	
						if (csvReader.getIndex("SAMPLEDATE") > 0) {
							col = csvReader.getIndex("SAMPLEDATE");
						}
						else {
							col = csvReader.getIndex("SAMPLE_DATE");
						}
	
						try {
							dateStr = stringLineArray[col];
							if (dateStr != null && dateStr.length() > 0)
								simpleDateFormat.parse(dateStr);
						}
						catch (ParseException pex) {
							StringBuilder errorString = new StringBuilder();
							errorString.append("Error: Row ");
							errorString.append(row);
							errorString.append(": SubjectUID: ");
							errorString.append(subjectUID);
							errorString.append(" ");
							errorString.append(fieldNameArray[col]);
							errorString.append(": ");
							errorString.append(stringLineArray[col]);
							errorString.append(" is not in the valid date format of: ");
							errorString.append(Constants.DD_MM_YYYY.toLowerCase());
	
							dataValidationMessages.add(errorString.toString());
							errorCells.add(new ArkGridCell(col, row));
							insertThisRow = false;//drop out also?
						}
					}
				}

				//log.debug("\n");
				recordCount++;
				row++;
				if(insertThisRow){
					insertRows.add(row);
				}

			}

			if (dataValidationMessages.size() > 0) {
				log.debug("Validation messages: " + dataValidationMessages.size());
				for (Iterator<String> iterator = dataValidationMessages.iterator(); iterator.hasNext();) {
					String errorMessage = iterator.next();
					log.debug(errorMessage);
				}
			}
			else {
				log.debug("Validation is ok");
			}
		}
		catch (IOException ioe) {
			log.error("processMatrixBiospecimenFile IOException stacktrace:", ioe);
			throw new ArkSystemException("Unexpected I/O exception whilst reading the Biospecimen data file");
		}
		finally {
			// Clean up the IO objects
			//timer.stop();
			//log.debug("Total elapsed time: " + timer.getTime() + " ms or " + decimalFormat.format(timer.getTime() / 1000.0) + " s");
			//log.debug("Total file size: " + srcLength + " B or " + decimalFormat.format(srcLength / 1024.0 / 1024.0) + " MB");
			//if (timer != null)
			//	timer = null;
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

		for (Iterator<Integer> iterator = updateRows.iterator(); iterator.hasNext();) {
			Integer i = (Integer) iterator.next();
			dataValidationMessages.add("Data on row " + i.intValue() + " exists, please confirm update");
		}

		return dataValidationMessages;
	}
	
	
	public java.util.Collection<String> validateMatrixLocationFileData(InputStream fileInputStream, long inLength, String inFileFormat, char inDelimChr) throws FileFormatException,
	ArkSystemException {
		delimiterCharacter = inDelimChr;
		fileFormat = inFileFormat;
		
		HashMap<String, String> uniqueSubjectBiocollections = new HashMap<String, String>();
		HashSet<String> uniqueSubjectBiospecimens = new HashSet<String>();
		
		row = 1;
		
		InputStreamReader inputStreamReader = null;
		CsvReader csvReader = null;
		try {
			inputStreamReader = new InputStreamReader(fileInputStream);
			csvReader = new CsvReader(inputStreamReader, delimiterCharacter);
			
			String[] stringLineArray;
		
			srcLength = inLength;
			if (srcLength <= 0) {
				throw new FileFormatException("The input size was not greater than 0.  Actual length reported: " + srcLength);
			}
		
		
			// Set field list (note 1th column to Nth column)
			// BiospecimenUID F1 F2 FN
			// 0 1 2 N
			csvReader.readHeaders();
		
			srcLength = inLength - csvReader.getHeaders().toString().length();
		
			String[] fieldNameArray = csvReader.getHeaders();
			
			// Loop through all rows in file
			while (csvReader.readRecord()) {
				boolean updateThisRow = true;
				stringLineArray = csvReader.getValues();
				
				String biospecimenUID = csvReader.get("BIOSPECIMENUID");
				
				/*LinkSubjectStudy linkSubjectStudy = (iArkCommonService.getSubjectByUIDAndStudy(subjectUID, study));
				if(linkSubjectStudy==null){
					StringBuilder errorString = new StringBuilder();
					errorString.append("Error: Row ");
					errorString.append(row);
					errorString.append(": SubjectUID: ");
					errorString.append(subjectUID);
					errorString.append(" does not exist.  Please check this Subject UID and try again.");
					dataValidationMessages.add(errorString.toString());
					errorCells.add(new ArkGridCell(csvReader.getIndex("SUBJECTUID"), row));
					insertThisRow = false;//drop out also?
					recordCount++;
					row++;
					break;
				}*/
				
				Biospecimen biospecimen = null;//iLimsService.getBiospecimenByUid(biospecimenUID,study);
				
				biospecimen = iLimsService.getBiospecimenByUid(biospecimenUID,study);
				if(biospecimen == null){
					StringBuilder errorString = new StringBuilder();
					errorString.append("Error: Row ");
					errorString.append(row);
					errorString.append(": bioscpeim UID: ");
					errorString.append(biospecimenUID);
					errorString.append(" does not exist in study '" + study.getName() + "   Please check this Subject UID and try again.");
					dataValidationMessages.add(errorString.toString());
					errorCells.add(new ArkGridCell(csvReader.getIndex("SUBJECTUID"), row));
					updateThisRow = false;//drop out also?
					recordCount++;
					row++;
					break;								
				}
				else{
//default					updateThisRow = true;//drop out also?
				}
			
				if(updateThisRow){ //else why bother
					String site = csvReader.get("SITE");
					String freezer = csvReader.get("FREEZER");
					String shelf = csvReader.get("SHELF");
					String rack = csvReader.get("RACK");
					String box = csvReader.get("BOX");
					String rowString = csvReader.get("ROW");
					String columnString = csvReader.get("COLUMN");
					
					//if anything is empty
					if ((site != null && freezer != null && shelf != null && rack != null && 
							box != null && rowString != null && columnString != null) &&
						(!site.isEmpty() && !freezer.isEmpty() && !shelf.isEmpty() && !rack.isEmpty() &&
							!box.isEmpty() && !rowString.isEmpty() && !columnString.isEmpty())	
					) {
						InvCell invCell = iInventoryService.getInvCellByLocationNames(site, freezer, shelf, rack, box, rowString, columnString);
						if (invCell == null) {
							StringBuilder errorString = new StringBuilder();
							errorString.append("Error: Row ");
							errorString.append(row);
							errorString.append(".  The location details provided for BiospecimenUID: ");
							errorString.append(biospecimenUID);
							errorString.append(" do not match any location in the database. Please check and try again");
							dataValidationMessages.add(errorString.toString());
							errorCells.add(new ArkGridCell(csvReader.getIndex("SITE"), row));
							errorCells.add(new ArkGridCell(csvReader.getIndex("FREEZER"), row));
							errorCells.add(new ArkGridCell(csvReader.getIndex("SHELF"), row));
							errorCells.add(new ArkGridCell(csvReader.getIndex("RACK"), row));
							errorCells.add(new ArkGridCell(csvReader.getIndex("BOX"), row));
							errorCells.add(new ArkGridCell(csvReader.getIndex("ROW"), row));
							errorCells.add(new ArkGridCell(csvReader.getIndex("COLUMN"), row));
							updateThisRow = false;
						}
						else{
							//check ownership of the cell.  
							//if nothing is there - put this spec there
							if(invCell.getBiospecimen()==null){
								//invCell.setBiospecimen(biospecimen); is what we will do next step
								//updateRows.add(row);
							}
							else{
								StringBuilder errorString = new StringBuilder();
								errorString.append("Error: Row ");
								errorString.append(row);
								errorString.append(": biospecimen UID: ");
								errorString.append(biospecimenUID);
								errorString.append(" cannot be placed at this cell location as there is already something there (" + invCell.getBiospecimen().getBiospecimenUid() + ").  Please check this UID or location and try again.");
								dataValidationMessages.add(errorString.toString());
								errorCells.add(new ArkGridCell(csvReader.getIndex("SUBJECTUID"), row));
								updateThisRow = false;//drop out also?
							}
						}
					}
					else{
						//else you have incomplete info, and we aren't cool with that;
						StringBuilder errorString = new StringBuilder();
						errorString.append("Error: Row ");
						errorString.append(row);
						errorString.append(" The location details provided for BiospecimenUID: ");
						errorString.append(biospecimenUID);
						errorString.append(" are incomplete. Please ensure you either provide all of the following information \"SITE, FREEZER, RACK, BOX, ROW and COLUMN\" or none of these ");
						dataValidationMessages.add(errorString.toString());
						errorCells.add(new ArkGridCell(csvReader.getIndex("SITE"), row));
						errorCells.add(new ArkGridCell(csvReader.getIndex("FREEZER"), row));
						errorCells.add(new ArkGridCell(csvReader.getIndex("RACK"), row));
						errorCells.add(new ArkGridCell(csvReader.getIndex("BOX"), row));
						errorCells.add(new ArkGridCell(csvReader.getIndex("ROW"), row));
						errorCells.add(new ArkGridCell(csvReader.getIndex("COLUMN"), row));
						updateThisRow = false;//drop out also?
						
					}
				}
					
				//log.debug("\n");
				recordCount++;
				row++;
				if(updateThisRow){
					updateRows.add(row);
				}
		
			}
		
			if (dataValidationMessages.size() > 0) {
				log.debug("Validation messages: " + dataValidationMessages.size());
				for (Iterator<String> iterator = dataValidationMessages.iterator(); iterator.hasNext();) {
					String errorMessage = iterator.next();
					log.debug(errorMessage);
				}
			}
			else {
				log.debug("Validation is ok");
			}
		}
		catch (IOException ioe) {
			log.error("processMatrixBiospecimenFile IOException stacktrace:", ioe);
			throw new ArkSystemException("Unexpected I/O exception whilst reading the Biospecimen data file");
		}
		finally {
			// Clean up the IO objects
			//timer.stop();
			//log.debug("Total elapsed time: " + timer.getTime() + " ms or " + decimalFormat.format(timer.getTime() / 1000.0) + " s");
			//log.debug("Total file size: " + srcLength + " B or " + decimalFormat.format(srcLength / 1024.0 / 1024.0) + " MB");
			//if (timer != null)
			//	timer = null;
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
		
		for (Iterator<Integer> iterator = updateRows.iterator(); iterator.hasNext();) {
			Integer i = (Integer) iterator.next();
			dataValidationMessages.add("Data on row " + i.intValue() + " exists, please confirm update");
		}
		
		return dataValidationMessages;
		}
	
	
	
}
