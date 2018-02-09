package za.ac.theark.study.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import org.projectredcap.main.Config;
import org.projectredcap.main.ExportRecords;
import org.projectredcap.main.ExportReports;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import za.ac.theark.core.model.study.entity.ArkRedcap;
import za.ac.theark.core.service.IArkRedcapService;
import au.org.theark.core.model.study.entity.DelimiterType;
import au.org.theark.core.model.study.entity.FileFormat;
import au.org.theark.core.model.study.entity.LinkSubjectStudy;
import au.org.theark.core.model.study.entity.Payload;
import au.org.theark.core.model.study.entity.Study;
import au.org.theark.core.model.study.entity.Upload;
import au.org.theark.core.service.IArkCommonService;
import au.org.theark.study.job.StudyDataUploadExecutor;
import au.org.theark.study.service.IStudyService;
import au.org.theark.study.util.DataUploader;
import au.org.theark.study.util.SubjectUploadReport;

/**
 * @author Freedom Mukomana
 *
 */

@Service("redcapRecordImporterSchService")
public class RedcapRecordImporterSchServiceImpl implements IRedcapRecordImporterSchService{
	
	protected transient Logger		log					= LoggerFactory.getLogger(RedcapRecordImporterSchServiceImpl.class);
	
	
	public IArkRedcapService<?> iArkAdminService;

	public IArkCommonService<?> iArkCommonService;
	
	public IStudyService iStudyService;	
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException{
		
		//Declaring variables
		List<List> subjectList = new ArrayList<List>();
		List<String> headers = new  ArrayList<String>();
		List<String> uidsWhichNeedUpdating;
		List<StringBuilder> recordsStrBldr;
		Date date = new Date(System.currentTimeMillis());
		Study study;
		//DataREDCapUploader dataREDCapUploader;
		//DataUploader dataUploader;
		StringBuffer report;
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		log.info("/tmp/"+sdf.format(date)+"upload.csv");
		
		File file; //= new File("/tmp/"+sdf.format(date)+"upload.csv");
				
		//Generate an upload report object
		UploadREDCapReport uploadREDCapReport = new UploadREDCapReport();	
		
		Upload upload = new Upload();
		
		Payload payload;
				
		try{
			//Retrieve autowired services from JobExecutionContext
			iArkAdminService = (IArkRedcapService<?>)context.getScheduler().getContext().get("arkAdminService");
			iArkCommonService = (IArkCommonService<?>) context.getScheduler().getContext().get("arkCommonService");
			iStudyService = (IStudyService) context.getScheduler().getContext().get("studyService");
			
			//Gets Redcap connection from JobDataMap 
			JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
			ArkRedcap ar = (ArkRedcap)jobDataMap.get("arkRedcap");
			log.warn("ArkREDCap Status " + ar.getId() + " " + ar.getName().toString() + " " +ar.getStudy().getId() + " " +ar.getSubStudy().getId() + " " + ar.getRedcapReportID() + " " + ar.getEnabled());
			file = new File("/tmp/"+sdf.format(date)+ar.getName()+"upload.csv");
			
			file.createNewFile();
			
			//Create an InputStream for the csvFile
			InputStream inputStream = new FileInputStream(file);
			
			//Calls the method that import data from Redcap
			recordsStrBldr = getRecords(ar);
			
			subjectList = formatRecords(recordsStrBldr, ar);
			
			study = ar.getStudy();
			log.info("RedXXX " + ar.getName() + " has been scheduled to start at " + ar.getStartDateTime()+" for "+ study.getName() +" study");
			uidsWhichNeedUpdating = checkSubject(subjectList, study, iArkCommonService);
			
			
			ListToCSV(file, subjectList);
			
			byte[] bFile = new byte[(int) file.length()];
			
			inputStream.read(bFile);
			
			payload = iArkCommonService.createPayload(bFile);
			
			MessageDigest md5Digest = MessageDigest.getInstance("MD5");
			String checksum = getFileChecksum(md5Digest, inputStream);
			

			String strFileFormat = file.getName().substring(file.getName().lastIndexOf('.') + 1).toUpperCase();
			FileFormat fileFormat = iArkCommonService.getFileFormatByName(strFileFormat.toUpperCase());
			
			//report = dataREDCapUploader.uploadAndReportMatrixSubject(subjectList, headers, uidsWhichNeedUpdating);
			//report = dataREDCapUploader.uploadAndReportMatrixSubject(inputStream, headers, uidsWhichNeedUpdating);
			char inDelimChr = ',';
						
			//Prepare report
			upload.setStartTime(date);
			upload.setUploadMethod(iArkCommonService.getUploadMethod(new Long(2)));
			upload.setArkFunction(iArkCommonService.getArkFunctionById(new Long(73)));
			upload.setChecksum(checksum);
			upload.setPayload(payload);
			DelimiterType delimiterType = iArkCommonService.getDelimiterType(new Long(1));
			upload.setDelimiterType(delimiterType);
			upload.setFileFormat(fileFormat);
			upload.setFilename(file.getName());
			upload.setStudy(study);
			upload.setUploadType(iArkCommonService.getDefaultUploadType());
		
			//Create an upload
			iArkCommonService.createUpload(upload);
			
			//InputStream fileInputStream, long inLength, String inFileFormat, char inDelimChr, List<String> uidsWhichNeedUpdating
			//String strBuf = dataUploader.uploadAndReportMatrixSubjectFile(new FileInputStream(file), file.length(), strFileFormat, inDelimChr, uidsWhichNeedUpdating).toString(); 
			//uploadREDCapReport.setReport(report);
			
			//iArkCommonService.updateUpload(upload);
			
			SubjectUploadReport subjectUploadReport = new SubjectUploadReport();
			subjectUploadReport.appendDetails(upload, study.getName().toString(), "arkadmina@ark.core.wits.ac.za");
			
			/*IArkCommonService iArkCommonService,
			IStudyService iStudyService,
			InputStream inputStream,
			Long uploadId,
			Long studyId,
			String fileFormat,
			char delimiter,
			long size, String report, List<String> uidsToUpload*/
			
			log.info("Study Details "+study.getId());
			
			StudyDataUploadExecutor task = new StudyDataUploadExecutor(iArkCommonService, iStudyService, new FileInputStream(file), upload.getId(),
					study.getId(), strFileFormat, inDelimChr, file.length(), subjectUploadReport.getReport().toString(), uidsWhichNeedUpdating);
			task.run();
			
			inputStream.close();
			
		}catch(Exception ex){
			ex.printStackTrace();
		}			
	}
	
	private void insertHeaders(File file){
		try{
			
			String header = "SUBJECTUID,SUBJECT_STATUS,DATE_OF_ENROLLMENT,AGE_AT_ENROLLMENT, OTHER_ID, ETHNICITY, GENDER,COMMENTS,CONSENT_DATE,CONSENT_STATUS,CONSENT_TO_USE_DATA,CONSENT_TO_USE_BIOSPECIMEN,CONSENT_TO_SHARE_DATA,CONSENT_TO_SHARE_BIOSPECIMEN,SUBSTUDYS";
					
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(header);
			bw.close();
		}catch(Exception ex){
			ex.printStackTrace();
		}		
	}
	
	private String getFileChecksum(MessageDigest digest, InputStream inputStream) throws IOException
	{
	     //Create byte array to read data in chunks
	    byte[] byteArray = new byte[1024];
	    int bytesCount = 0; 
			
	    //Read file data and update in message digest
	    while ((bytesCount = inputStream.read(byteArray)) != -1) {
	        digest.update(byteArray, 0, bytesCount);
	    };
	     
	    //Get the hash's bytes
	    byte[] bytes = digest.digest();
	     
	    //This bytes[] has bytes in decimal format;
	    //Convert it to hexadecimal format
	    StringBuilder sb = new StringBuilder();
	    for(int i=0; i< bytes.length ;i++)
	    {
	        sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
	    }
	     
	    //return complete hash
	   return sb.toString();
	}
	
	public void ListToCSV(File file, List<List> subjectList) throws IOException{
		
		Writer writer = new FileWriter(file);
		BufferedWriter bufferedWriter = new BufferedWriter(writer);
		String header = "SUBJECTUID,SUBJECT_STATUS,DATE_OF_ENROLLMENT,AGE_AT_ENROLLMENT, OTHER_ID, ETHNICITY, GENDER,COMMENTS,CONSENT_DATE,CONSENT_STATUS,CONSENT_TO_USE_DATA,CONSENT_TO_USE_BIOSPECIMEN,CONSENT_TO_SHARE_DATA,CONSENT_TO_SHARE_BIOSPECIMEN,SUBSTUDYS";
		//List<String> tempHeader = new ArrayList<String>(Arrays.asList(subjectList.get(0).toString().split(",")));
		bufferedWriter.write(header);
				
		for(List l : subjectList){
			//bufferedWriter.append("\n");
			StringBuilder subject = new StringBuilder();
						
			for(StringBuilder sb : (StringBuilder[]) l.toArray(new StringBuilder[l.size()])){
					subject.append(sb.toString());
					subject.append(",");					
				}
			
			subject.deleteCharAt(subject.length()-1);
			
			
		
			bufferedWriter.write("\n"+subject.toString());
			
			//log.info(subject.toString());
			}
						
		bufferedWriter.flush();	
		bufferedWriter.close();	
		
		//BufferedReader br = new BufferedReader(new FileReader(file));
		
		/*while(br.read()!=-1){
			log.info(br.readLine());
		}
		
		if(true){
			throw new AssertionError();
		}*/
	}
	
	/**
	 * The function imports data from Redcap
	 * @param arkRedcap
	 ** @return a list of strings
	 */
	List getRecords(ArkRedcap arkRedcap){
		
		Config c = new Config();
				
		c.setAPI_TOKEN(arkRedcap.getToken());
		c.setAPI_URL(arkRedcap.getRedcapUrl());
		c.setAPI_FORMAT(arkRedcap.getFormat().getName());
		c.setAPI_CONTENT(arkRedcap.getContent().toString());
		c.setAPI_RAWORLABEL("label");
		if(arkRedcap.getRedcapReportID()!=null){
			c.setAPI_REPORT_ID(arkRedcap.getRedcapReportID());
			log.warn("Report ID : "+arkRedcap.getRedcapReportID());
		}else{
			//c.setAPI_TYPE(arkRedcap.getType().getName());
		}
		//c.setAPI_FIELDS("awi_number,sample_collection_date,age_at_collection,app_age_collection,sex,country,ethnicity,kenya_ethnicity,ghana_ethnicity,burkina_faso_ethnicity,other_ethnicity,site");
		//ExportRecords er = new ExportRecords(c);
		ExportReports er = new ExportReports(c);
		//log.info("XXX"+er.doPost());
		return er.doPost();
	}
	
	/**
	 * The function imports data from Redcap
	 * @param recordsStrBldr, arkRedcap
	 ** @return a list of subjects (the data of the subject is stored as String in a list)
	 */
	private List<List> formatRecords(List<StringBuilder> recordsStrBldr, ArkRedcap ar){
		StringBuilder awiNum;		
		StringBuilder subjectStatus;
		StringBuilder dateOfEnrollment;
		StringBuilder ageAtEnrollment;
		StringBuilder otherID;
		StringBuilder ethnicity;
		StringBuilder gender;
		StringBuilder comments;
		StringBuilder consentDate;
		StringBuilder consentStatus;
		StringBuilder consentToUseData;
		StringBuilder consentToShareData;
		StringBuilder consentToUseBiospecimen;
		StringBuilder consentToShareBiospecimen;
		StringBuilder subStudy;
		
		List<StringBuilder> subject = new ArrayList<StringBuilder>();
		List<List> subjectList = new ArrayList<List>();
		
		log.warn("File header : "+recordsStrBldr.get(0).toString());
		List<String> tempHeader = new ArrayList<String>(Arrays.asList(recordsStrBldr.get(0).toString().split(",")));
		recordsStrBldr.remove(0);
				
		for(StringBuilder sb : recordsStrBldr){
			List<StringBuilder> subjectTemp = new ArrayList<StringBuilder>();
		
				for(String s : sb.toString().split(",")){
					//log.warn(s.toString());
					subjectTemp.add(new StringBuilder(s));	
				}
				
			//log.warn("Temporary Header Size : " + tempHeader); 
			log.warn(subjectList.size()+" Subject Temp : " + subjectTemp);
			
				awiNum = subjectTemp.get(tempHeader.indexOf("awi_number"));
				subjectStatus = new StringBuilder("Current");
				dateOfEnrollment = subjectTemp.get(tempHeader.indexOf("sample_collection_date"));
				ageAtEnrollment = getAge(subjectTemp.get(tempHeader.indexOf("age_at_collection")), subjectTemp.get(tempHeader.indexOf("app_age_collection")));
				otherID = new StringBuilder();
				ethnicity = new StringBuilder("African");
				gender = subjectTemp.get(tempHeader.indexOf("sex"));
				comments = new StringBuilder();
				consentDate = subjectTemp.get(tempHeader.indexOf("sample_collection_date"));
				consentStatus = new StringBuilder("Consented");
				consentToUseData = new StringBuilder("Yes");
				consentToShareData = new StringBuilder("Yes");
				consentToUseBiospecimen = new StringBuilder("Yes");
				consentToShareBiospecimen = new StringBuilder("Yes");
			
				subStudy = new StringBuilder(ar.getSubStudy().getId().toString());
			
				subject = new ArrayList<StringBuilder>(Arrays.asList(awiNum,subjectStatus,dateOfEnrollment,ageAtEnrollment,otherID,ethnicity,gender,comments,consentDate,consentStatus,consentToUseData,consentToShareData,consentToUseBiospecimen,consentToShareBiospecimen,subStudy));
				if(!awiNum.toString().contains("-1")){
					subjectList.add(subject);	
				}
		}
		
		return subjectList; 
	}
	
	private StringBuilder getAge(StringBuilder age, StringBuilder appAge){
		StringBuilder newAge;
		if(age==null || age.length()==0){
			age=appAge;
		}
		newAge = new StringBuilder(age);
		return newAge;
	}
	
	List<String> createHeaders(List<StringBuilder> recordsStrBldr){
		List<String> header = new ArrayList<String>();
		for(String s : recordsStrBldr.get(0).toString().split(",")){
			if(header.size()<8)
				header.add(s);
		}
		
		header.add("status");
		header.add("comments");
		header.add("consent_date");
		header.add("consent_status");
		header.add("consent_to_use_data");
		header.add("consent_to_share_data");
		header.add("consent_to_use_biospecimen");
		header.add("consent_to_share_biospecimen");
		header.add("substudies");
		
		return header;
	}
	
	/*List<StringBuilder> checkSubject(List<List> subjectList, Study study, IArkCommonService<?> iArkCommonService){
		
		new ArrayList<StringBuilder>();
		LinkSubjectStudy subject = new LinkSubjectStudy();
		List<StringBuilder> iDsToUpdateList = new ArrayList<StringBuilder>();
		new ArrayList<Long>();
		
		try{
			log.info("Records downloaded " + subjectList.size());
		for(List<StringBuilder> sb : subjectList){
			subject = iArkCommonService.getSubjectByUID(sb.get(0).toString(),study);
			log.info(" ID - "+subject.getSubjectUID());
			if(subject!=null)
				iDsToUpdateList.add(new StringBuilder(subject.getSubjectUID().toString()));
		}
		}catch(Exception ex){
			
		}
		return iDsToUpdateList;
	}*/
	
	List<String> checkSubject(List<List> subjectList, Study study, IArkCommonService<?> iArkCommonService){
		
		LinkSubjectStudy subject = new LinkSubjectStudy();
		List<String> iDsToUpdateList = new ArrayList<String>();
		new ArrayList<Long>();
		
		
		log.info("Records downloaded " + subjectList.size());
		int i = 1;
		for(List<StringBuilder> sb : subjectList){
			subject=null;
			try{
				subject = iArkCommonService.getSubjectByUID(sb.get(0).toString(),study);
			}catch(Exception ex){
				ex.printStackTrace();				
			}	
			
			if(subject!=null){
				log.info(i+" Subject : "+sb.get(0).toString()+" "+ subject.getSubjectUID().toString() +" Study Name : "+study.getName());
				iDsToUpdateList.add(subject.getSubjectUID().toString());
				log.info("Added for update");
			}
				//log.info(subject.getSubjectUID().toString());
				i++;
		}
		
		log.info(" Records to be updated - "+iDsToUpdateList.size());
		for(String s : iDsToUpdateList)
			log.info(s.toString());
		/*if(true)
			throw new IllegalAccessError();*/
		return iDsToUpdateList;
	}
	
	StringBuilder getEthnicity(List<StringBuilder> record, List<String> tempHeader){
		
		switch (record.get(tempHeader.indexOf("country")).toString()) {
			case "South Africa":
				if(record.get(tempHeader.indexOf("ethnicity")).toString()=="Other"){
					return record.get(tempHeader.indexOf("other_ethnicity"));
				}else{
					return record.get(tempHeader.indexOf("ethnicity"));
				}
			case "Kenya":	
				if(record.get(tempHeader.indexOf("kenya_ethnicity")).toString()=="Other"){
					return record.get(tempHeader.indexOf("other_ethnicity"));
				}else{
					return record.get(tempHeader.indexOf("kenya_ethnicity"));
				}
			case "Ghana":
				if(record.get(tempHeader.indexOf("ghana_ethnicity")).toString()=="Other"){
					return record.get(tempHeader.indexOf("other_ethnicity"));
				}else{
					return record.get(tempHeader.indexOf("ghana_ethnicity"));
				}
			case "Burkina Faso":
				if(record.get(tempHeader.indexOf("burkina_faso_ethnicity")).toString()=="Other"){
					return record.get(tempHeader.indexOf("other_ethnicity"));
				}else{
					return record.get(tempHeader.indexOf("burkina_faso_ethnicity"));
				}	
		}
		return new StringBuilder(); 		
	}
	
	private StringBuilder getSubStudy(List<StringBuilder> record, List<String> tempHeader){
		Study study = iArkCommonService.getStudy(new Long(1));
		Hashtable<String, Study> subStudies = iArkCommonService.getAllSubStudiesHashTable(study);
		
		StringBuilder subStudiesStr = new StringBuilder();
		switch (record.get(tempHeader.indexOf("site")).toString()) {
			case "Agincourt":
				subStudiesStr.append(subStudies.get("Agincourt").getId()+",");
			case "Dikgale":	
				subStudiesStr.append(subStudies.get("Dikgale").getId()+",");
			case "Nairobi":
				subStudiesStr.append(subStudies.get("Nairobi").getId()+",");
			case "Nanoro":
				subStudiesStr.append(subStudies.get("Nanoro").getId()+",");
			case "Navrongo":
				subStudiesStr.append(subStudies.get("Navrongo").getId()+",");	
			case "SOWETO":
				subStudiesStr.append(subStudies.get("SOWETO").getId());	
		}		
		return subStudiesStr;
	}
}
