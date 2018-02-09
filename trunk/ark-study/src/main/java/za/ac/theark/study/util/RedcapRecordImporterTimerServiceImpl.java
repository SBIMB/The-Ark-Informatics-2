package za.ac.theark.study.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.TimerTask;

import org.projectredcap.main.Config;
import org.projectredcap.main.ExportRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NestedRuntimeException;
import org.springframework.stereotype.Service;

import za.ac.theark.core.model.study.entity.ArkRedcap;
import za.ac.theark.core.service.IArkRedcapService;
import au.org.theark.core.model.study.entity.LinkSubjectStudy;
import au.org.theark.core.model.study.entity.Study;
import au.org.theark.core.service.IArkCommonService;
import au.org.theark.study.service.IStudyService;
import au.org.theark.study.util.DataUploader;

@Service("redcapRecordImporterTimerService")
public class RedcapRecordImporterTimerServiceImpl extends TimerTask implements IRedcapRecordImporterTimerService{
	
	protected transient Logger		log					= LoggerFactory.getLogger(RedcapRecordImporterTimerServiceImpl.class);
	
	//@SpringBean(name = za.ac.theark.admin.service.Constants.ARK_ADMIN_SERVICE)
	@Autowired
	public IArkRedcapService<?> iArkAdminService;
	
	@Autowired
	public IArkCommonService<?> iArkCommonService;
	
	@Autowired
	public IStudyService iStudyService;	
	
	private String site;
	
	public static void main(final String[] args)
	{
		//RedcapRecordImporterServiceImpl recordImporterServiceImpl = new RedcapRecordImporterServiceImpl();
		//recordImporterServiceImpl.getRecords();
	}
	
	public void initialise(){
		//RedcapRecordImporter acc = new RedcapRecordImporter();
		//acc.getRecords();
		//acc.getHeaders();
		//acc.formatRecords();
	
		
		if(iArkAdminService==null)
			log.info("IArkAdminService is null...");
			
			List<ArkRedcap> arkRedcapList = iArkAdminService.getArkRedcapList();
			//ArkRedcap arkRedcap = new ArkRedcap();
			List<List> subjectList = new ArrayList<List>();
			List<String> headers = new  ArrayList<String>();
			List<StringBuilder> uidsWhichNeedUpdating;
			List<StringBuilder> recordsStrBldr;
			Study study;
			
			for(ArkRedcap ar : arkRedcapList){
				recordsStrBldr = getRecords(ar);
				//recordsStrBldr = getRecords();
				headers = new ArrayList<String>(Arrays.asList("SUBJECTUID","SUBJECT_STATUS","DATE_OF_ENROLLMENT","AGE_AT_ENROLLMENT","OTHER_ID","ETHNICITY","GENDER","COMMENTS","CONSENT_DATE","CONSENT_STATUS","CONSENT_TO_USE_DATA","CONSENT_TO_USE_BIOSPECIMEN","CONSENT_TO_SHARE_DATA","CONSENT_TO_SHARE_BIOSPECIMEN","SUBSTUDYS"));
				subjectList = formatRecords(recordsStrBldr, ar);
				study = new Study(new Long(1));
				uidsWhichNeedUpdating = checkSubject(subjectList, study, iArkCommonService);
				DataUploader dataUploader = new DataUploader(iArkCommonService.getStudy(new Long(1)), iArkCommonService, iStudyService);
				dataUploader.uploadAndReportMatrixSubject(subjectList, headers, uidsWhichNeedUpdating);
			}			
	}
	
	@Override
	public void run() {
		initialise();
	}
	
	List<StringBuilder> getRecords(){
		
		Config c = new Config();
		
		c.setAPI_TOKEN("E8156C1701354FBEF43B9BEFE3C86855");
		c.setAPI_URL("https://redcap.core.wits.ac.za/redcap/redcap_v6.11.4/API/");
		c.setAPI_FORMAT("csv");
		c.setAPI_TYPE("flat");
		c.setAPI_CONTENT("record");
		c.setAPI_RAWORLABEL("label");
		c.setAPI_FIELDS("awi_number,sample_collection_date,age_at_collection,app_age_collection,country,ethnicity,kenya_ethnicity,ghana_ethnicity,burkina_faso_ethnicity,other_ethnicity,sex,date_blood_taken");
		ExportRecords er = new ExportRecords(c);
		return er.doPost();
		//SUBJECTUID	SUBJECT_STATUS			DATE_OF_ENROLLMENT		AGE_AT_ENROLLMENT						OTHER_ID	ETHNICITY																			GENDER	COMMENTS	CONSENT_DATE	CONSENT_STATUS	CONSENT_TO_USE_DATA	CONSENT_TO_USE_BIOSPECIMEN	CONSENT_TO_SHARE_DATA	CONSENT_TO_SHARE_BIOSPECIMEN	SUBSTUDYS
		//awi_number	-						sample_collection_date	age_at_collection,app_age_collection	-			ethnicity,kenya_ethnicity,ghana_ethnicity,burkina_faso_ethnicity,other_ethnicity	sex

	}
	
	List getRecords(ArkRedcap arkRedcap){
		
		Config c = new Config();
		
		
		
		c.setAPI_TOKEN(arkRedcap.getToken());//"E8156C1701354FBEF43B9BEFE3C86855"
		c.setAPI_URL(arkRedcap.getRedcapUrl());//"https://redcap.core.wits.ac.za/redcap/redcap_v6.9.5/API/"
		c.setAPI_FORMAT(arkRedcap.getFormat().getName());//"csv"
		c.setAPI_TYPE(arkRedcap.getType().getName());//"flat"
		c.setAPI_CONTENT(arkRedcap.getContent().toString());//"record"
		c.setAPI_RAWORLABEL("label");
		c.setAPI_FIELDS("awi_number,sample_collection_date,age_at_collection,app_age_collection,sex,country,ethnicity,kenya_ethnicity,ghana_ethnicity,burkina_faso_ethnicity,other_ethnicity,site");
		ExportRecords er = new ExportRecords(c);
		return er.doPost();
	}
	
	List<List> formatRecords(List<StringBuilder> recordsStrBldr, ArkRedcap ar){
		
		List<StringBuilder> subject = new ArrayList<StringBuilder>();
		
		List<List> subjectList = new ArrayList<List>();
		List<String> tempHeader = new ArrayList<String>(Arrays.asList(recordsStrBldr.get(0).toString().split(",")));
				
		recordsStrBldr.remove(0);
		
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
		StringBuilder subStudies;
		
		for(StringBuilder sb : recordsStrBldr){
		List<StringBuilder> subjectTemp = new ArrayList<StringBuilder>();
			for(String s : sb.toString().split(",")){
				//log.warn(s.toString());
				subjectTemp.add(new StringBuilder(s));	
			}
			
			awiNum = subjectTemp.get(tempHeader.indexOf("awi_number"));
			subjectStatus = new StringBuilder("Current");
			dateOfEnrollment = subjectTemp.get(tempHeader.indexOf("sample_collection_date"));
			ageAtEnrollment = getAge(subjectTemp.get(tempHeader.indexOf("age_at_collection")), subjectTemp.get(tempHeader.indexOf("app_age_collection")));
			otherID = new StringBuilder();
			ethnicity = getEthnicity(subjectTemp, tempHeader);
			gender = subjectTemp.get(tempHeader.indexOf("sex"));
			comments = new StringBuilder();
			consentDate = subjectTemp.get(tempHeader.indexOf("sample_collection_date"));
			consentStatus = new StringBuilder("Consented");
			consentToUseData = new StringBuilder("Yes");
			consentToShareData = new StringBuilder("Yes");
			consentToUseBiospecimen = new StringBuilder("Yes");
			consentToShareBiospecimen = new StringBuilder("Yes");
			subStudies = getSubStudy(subjectTemp, tempHeader);
			
			subject = new ArrayList<StringBuilder>(Arrays.asList(awiNum,subjectStatus,dateOfEnrollment,ageAtEnrollment,otherID,ethnicity,gender,comments,consentDate,consentStatus,consentToUseData,consentToShareData,consentToUseBiospecimen,consentToShareBiospecimen,subStudies));		
			subjectList.add(subject);	
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
	
	List<StringBuilder> checkSubject(List<List> subjectList, Study study, IArkCommonService<?> iArkCommonService){
		
		List<StringBuilder> subjectToUpdate = new ArrayList<StringBuilder>();
		LinkSubjectStudy subject = new LinkSubjectStudy();
		List<StringBuilder> iDsToUpdateList = new ArrayList<StringBuilder>();
		List<Long> iDsToInsertList = new ArrayList<Long>();
		
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
