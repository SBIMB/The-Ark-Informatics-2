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
package au.org.theark.report.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

//import mx4j.log.Log;







import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import au.org.theark.core.dao.IStudyDao;
import au.org.theark.core.exception.EntityNotFoundException;
import au.org.theark.core.model.lims.entity.BioSampletype;
import au.org.theark.core.model.pheno.entity.PhenoCollection;
import au.org.theark.core.model.report.entity.ReportOutputFormat;
import au.org.theark.core.model.report.entity.ReportTemplate;
import au.org.theark.core.model.study.entity.Address;
import au.org.theark.core.model.study.entity.ArkUser;
import au.org.theark.core.model.study.entity.Consent;
import au.org.theark.core.model.study.entity.ConsentOption;
import au.org.theark.core.model.study.entity.ConsentStatus;
import au.org.theark.core.model.study.entity.CustomFieldGroup;
import au.org.theark.core.model.study.entity.EthnicityType;
import au.org.theark.core.model.study.entity.LinkSubjectStudy;
import au.org.theark.core.model.study.entity.OtherID;
import au.org.theark.core.model.study.entity.Person;
import au.org.theark.core.model.study.entity.Phone;
import au.org.theark.core.model.study.entity.Study;
import au.org.theark.core.model.study.entity.StudyComp;
import au.org.theark.core.model.worktracking.entity.Researcher;
import au.org.theark.core.service.IArkCommonService;
import au.org.theark.core.vo.ArkUserVO;
import au.org.theark.lims.service.ILimsService;
import au.org.theark.report.model.dao.IReportDao;
import au.org.theark.report.model.vo.BiospecimenDetailsReportVO;
import au.org.theark.report.model.vo.BiospecimenNucleicAcidSummaryReportVO;
import au.org.theark.report.model.vo.BiospecimenSummaryReportVO;
import au.org.theark.report.model.vo.ConsentDetailsReportVO;
import au.org.theark.report.model.vo.CustomFieldDetailsReportVO;
import au.org.theark.report.model.vo.FieldDetailsReportVO;
import au.org.theark.report.model.vo.ResearcherCostResportVO;
import au.org.theark.report.model.vo.report.BiospecimenDetailsDataRow;
import au.org.theark.report.model.vo.report.BiospecimenNucleicAcidSummaryDataRow;
import au.org.theark.report.model.vo.report.BiospecimenSummaryDataRow;
import au.org.theark.report.model.vo.report.ConsentDetailsDataRow;
import au.org.theark.report.model.vo.report.CustomFieldDetailsDataRow;
import au.org.theark.report.model.vo.report.FieldDetailsDataRow;
import au.org.theark.report.model.vo.report.ResearcherCostDataRow;
import au.org.theark.report.model.vo.report.ResearcherDetailCostDataRow;
import au.org.theark.report.model.vo.report.StudyUserRolePermissionsDataRow;

@Transactional
@Service(Constants.REPORT_SERVICE)
public class ReportServiceImpl implements IReportService {

	private static Logger		log	= LoggerFactory.getLogger(ReportServiceImpl.class);

	private IArkCommonService	arkCommonService;
	private ILimsService		iLimsService;
	private IStudyDao			studyDao;
	private IReportDao			reportDao;

	public IReportDao getReportDao() {
		return reportDao;
	}

	@Autowired
	public void setReportDao(IReportDao reportDao) {
		this.reportDao = reportDao;
	}

	/* To access Hibernate Study Dao */
	public IStudyDao getStudyDao() {
		return studyDao;
	}

	@Autowired
	public void setStudyDao(IStudyDao studyDao) {
		this.studyDao = studyDao;
	}

	public IArkCommonService getArkCommonService() {
		return arkCommonService;
	}

	@Autowired
	public void setArkCommonService(IArkCommonService arkCommonService) {
		this.arkCommonService = arkCommonService;
	}
	
	public ILimsService getiLimsService() {
		return iLimsService;
	}

	@Autowired
	public void setiLimsService(ILimsService iLimsService) {
		this.iLimsService = iLimsService;
	}

	/* Service methods */
	public List<ReportTemplate> getReportsAvailableList(ArkUser arkUser, Study study) {
		List<ReportTemplate> result = reportDao.getReportsForUser(arkUser, study);
		return result;
	}

	public long getTotalSubjectCount(Study study) {
		return reportDao.getTotalSubjectCount(study);
	}

	public Map<String, Long> getSubjectStatusCounts(Study study) {
		return reportDao.getSubjectStatusCounts(study);
	}

	public Map<String, Long> getStudyConsentCounts(Study study) {
		return reportDao.getStudyConsentCounts(study);
	}

	public Map<String, Long> getStudyCompConsentCounts(Study study, StudyComp studyComp) {
		return reportDao.getStudyCompConsentCounts(study, studyComp);
	}

	public Long getWithoutStudyCompCount(Study study) {
		return reportDao.getWithoutStudyCompCount(study);
	}

	public List<ReportOutputFormat> getOutputFormats() {
		return reportDao.getOutputFormats();
	}

	public List<ConsentDetailsDataRow> getStudyLevelConsentDetailsList(ConsentDetailsReportVO cdrVO) {

		List<ConsentDetailsDataRow> consentDetailsList = new ArrayList<ConsentDetailsDataRow>();

		// Perform translation to report data source here...
		List<LinkSubjectStudy> tmpResults = reportDao.getStudyLevelConsentDetailsList(cdrVO);
		for (LinkSubjectStudy subject : tmpResults) {
			String subjectUID = subject.getSubjectUID();
//			Set<OtherID> list = subject.getPerson().getOtherIDs();
//			String otherID = "";
//			for(OtherID o : list) {
//				otherID += o.getOtherID_Source() + ": " + o.getOtherID() + "; ";
//			}
			String otherID = "WORLD!";
			String otherID_Source = "HELLO";
			String consentStatus = Constants.NOT_CONSENTED;
			ConsentStatus studyConsent = subject.getConsentStatus();
			if (studyConsent != null) {
				consentStatus = studyConsent.getName();
			}
			String subjectStatus = subject.getSubjectStatus().getName();
			Date dateOfEnrollment = subject.getDateOfEnrollment();
			Integer ageAtEnrollment = subject.getAgeAtEnrollment();
			
			Person p = subject.getPerson();
			EthnicityType ethnicityType = p.getEthnicityType();
			String ethnicity = Constants.UNKNOWN;
			if (ethnicityType!=null){
				ethnicity = ethnicityType.getName();
			}
			
			ConsentOption consentToUseDataOption = subject.getConsentToUseData(); 
			String consentToUseData = Constants.UNAVAILABLE;
			if(consentToUseDataOption!=null){
				consentToUseData = subject.getConsentToUseData().getName();
			}
			
			ConsentOption consentToShareDataOption = subject.getConsentToShareData(); 
			String consentToShareData = Constants.UNAVAILABLE;
			if(consentToShareDataOption!=null){
				consentToShareData = subject.getConsentToShareData().getName();
			}
			
			ConsentOption consentToUseBiospecimenOption = subject.getConsentToUseBiospecimen(); 
			String consentToUseBiospecimen = Constants.UNAVAILABLE;
			if(consentToUseBiospecimenOption!=null){
				consentToUseBiospecimen = subject.getConsentToUseBiospecimen().getName();
			}
			
			ConsentOption consentToShareBiospecimenOption = subject.getConsentToShareBiospecimen(); 
			String consentToShareBiospecimen = Constants.UNAVAILABLE;
			if(consentToShareBiospecimenOption!=null){
				consentToShareBiospecimen = subject.getConsentToShareBiospecimen().getName();
			}
			
			String sex = p.getGenderType().getName().substring(0, 1);
			Date consentDate = subject.getConsentDate();
			
			consentDetailsList.add(new ConsentDetailsDataRow(subjectUID, otherID_Source, otherID, subjectStatus, sex, dateOfEnrollment, ageAtEnrollment, ethnicity, consentDate, consentStatus, consentToUseData, consentToShareData, consentToUseBiospecimen,
					consentToShareBiospecimen));
		}

		return consentDetailsList;
	}

	private Consent getConsentMatchingSearchCriteria(LinkSubjectStudy lss, StudyComp studyCompFromSearch, ConsentStatus consentStatusFromSearch){
		for(Consent consentForLSS : lss.getConsents()){
			if(consentForLSS!=null && consentForLSS.getStudyComp() != null && studyCompFromSearch!=null){
				
				if(consentForLSS.getStudyComp().getName().equalsIgnoreCase(studyCompFromSearch.getName())){
					if(consentStatusFromSearch==null){
						return consentForLSS;
					}
					else if(consentForLSS.getConsentStatus() != null){
						if(consentStatusFromSearch.getName().equalsIgnoreCase(consentForLSS.getConsentStatus().getName()));
					}
				}
			}
		}
		return null;
	}

	
	public List<ConsentDetailsDataRow> getStudyCompConsentDetailsList(ConsentDetailsReportVO cdrVO) {
		// LinkedHashMap maintains insertion order
		HashMap<Long, List<ConsentDetailsDataRow>> consentDetailsMap;
		List<ConsentDetailsDataRow> results = new ArrayList<ConsentDetailsDataRow>();

		// override the default initial capacity and make the loadFactor 1.0
		consentDetailsMap = new HashMap<Long, List<ConsentDetailsDataRow>>(studyDao.getConsentStatus().size(), (float) 1.0);

		boolean noConsentDateCriteria = (cdrVO.getConsentDate() == null);
		boolean noConsentStatusCriteria = (cdrVO.getConsentStatus() == null);
		if (noConsentDateCriteria && noConsentStatusCriteria) {
			// This means that we can't do a query with LinkSubjectStudy inner join Consent
			// - so better to just iterate through the subjects that match the initial subject criteria
			Study study = cdrVO.getLinkSubjectStudy().getStudy();
			//List<LinkSubjectStudy> subjectList = reportDao.getSubjects(cdrVO);		//THIS GOES AND GETS ALL THE SUBJECTS REGARDLESS OF CONSENT
			List<LinkSubjectStudy> subjectList = reportDao.getSubjectsMatchingComponentConsent(cdrVO);		//THIS GOES AND GETS ALL THE SUBJECTS REGARDLESS OF CONSENT

			for (LinkSubjectStudy subject : subjectList) {
				/*Consent consentCriteria = new Consent();
				consentCriteria.setStudy(study);
				consentCriteria.setLinkSubjectStudy(subject);
				consentCriteria.setStudyComp(cdrVO.getStudyComp());
				consentCriteria.setConsentDate(cdrVO.getConsentDate());
				consentCriteria.setConsentStatus(cdrVO.getConsentStatus());
				// reportDao.getStudyCompConsent(..) ignores consentDate and consentStatus
*/
				//Consent consentResult = reportDao.getStudyCompConsent(consentCriteria);
				Consent consentResult = getConsentMatchingSearchCriteria(subject, cdrVO.getStudyComp(), cdrVO.getConsentStatus());

				ConsentDetailsDataRow cddr = new ConsentDetailsDataRow();
				if (consentResult == null) {
					populateConsentDetailsDataRow(cddr, study, subject, null);
					Long key = null;
					if (!consentDetailsMap.containsKey(key)) {
						consentDetailsMap.put(key, new ArrayList<ConsentDetailsDataRow>());
					}
					consentDetailsMap.get(key).add(cddr);
				}
				else {
					populateConsentDetailsDataRow(cddr, study, subject, consentResult);
					Long key = consentResult.getConsentStatus().getId();
					if (!consentDetailsMap.containsKey(key)) {
						consentDetailsMap.put(key, new ArrayList<ConsentDetailsDataRow>());
					}
					consentDetailsMap.get(key).add(cddr);
				}
				log.info("Subject: " + subject.getSubjectUID());
			}
			for (Long key : consentDetailsMap.keySet()) {
				results.addAll(consentDetailsMap.get(key));
			}
		}
		else {
			// Perform a consentStatus and/or consentDate constrained lookup
			// based on LinkSubjectStudy inner join Consent (this is because if either
			// of these constraints are applied, there will be no such Consent record
			// for the "Not Consented" state)
			List<ConsentDetailsDataRow> consents = reportDao.getStudyCompConsentList(cdrVO);
			if (consents != null) {
				for (ConsentDetailsDataRow cddr : consents) {
					Study study = cdrVO.getLinkSubjectStudy().getStudy();
					populateConsentDetailsDataRow(cddr, study, null, null);
					results.add(cddr);
				}
			}
		}

		return results;
	}

	protected void populateConsentDetailsDataRow(ConsentDetailsDataRow consentRow, Study study, LinkSubjectStudy subject, Consent consent) {
		String consentStatus = Constants.NOT_CONSENTED;
		consentRow.setConsentToUseData(Constants.UNAVAILABLE);
		consentRow.setConsentToShareData(Constants.UNAVAILABLE);
		consentRow.setConsentToUseBiospecimen(Constants.UNAVAILABLE);
		consentRow.setConsentToShareBiospecimen(Constants.UNAVAILABLE);
		
		if (consent != null && consent.getConsentStatus() != null) {
			consentStatus = consent.getConsentStatus().getName();
			consentRow.setConsentStatus(consentStatus); // set ConsentStatus with override from Consent arg
			consentRow.setConsentDate(consent.getConsentDate()); // set ConsentDate with override from Consent arg
			
			ConsentOption consentToUseData = subject.getConsentToUseData();
			if (consentToUseData != null){
				consentRow.setConsentToUseData(consentToUseData.getName());
			}
			
			ConsentOption consentToShareData = subject.getConsentToShareData();
			if (consentToShareData != null){
				consentRow.setConsentToShareData(consentToShareData.getName());
			}
						
			ConsentOption consentToUseBiospecimen = subject.getConsentToUseBiospecimen();
			if (consentToUseBiospecimen != null){
				consentRow.setConsentToUseBiospecimen(consentToUseBiospecimen.getName());
			}
						
			ConsentOption consentToShareBiospecimen = subject.getConsentToShareBiospecimen();
			if (consentToShareBiospecimen != null){
				consentRow.setConsentToShareBiospecimen(consentToShareBiospecimen.getName());
			}
		}
		else if (consentRow.getConsentStatus() == null || consentRow.getConsentStatus().isEmpty()) {
			consentRow.setConsentStatus(consentStatus); // set ConsentStatus to Not Consented if not set
		}

		try {
			if (subject == null) {
				// no subject was passed in, retrieve from DB via subjectUID
				if(consent == null) {
					subject = studyDao.getSubjectByUID(consentRow.getSubjectUID(), study);
				}
				else {
					subject = studyDao.getSubjectByUID(consentRow.getSubjectUID(), consent.getStudy());
				}
			}
			else {
				// set subjectUID if subject passed in
				consentRow.setSubjectUID(subject.getSubjectUID());
			}
			String subjectStatus = subject.getSubjectStatus().getName();
			consentRow.setSubjectStatus(subjectStatus); // set SubjectStatus
			Person p = subject.getPerson();
			
			Date dateOfEnrollment = subject.getDateOfEnrollment();
			consentRow.setDateOfEnrollment(dateOfEnrollment); // set Date of Enrollment
			
			Integer ageAtEnrollment = subject.getAgeAtEnrollment();
			consentRow.setAgeAtEnrollment(ageAtEnrollment); // set Age at enrollment
			
			String ethnicity = p.getEthnicityType().getName();
			consentRow.setEthnicity(ethnicity);; // set Ethnicity
			
			Date consentDate = subject.getConsentDate();
			consentRow.setConsentDate(consentDate);	// set Consent Date	
		}
		catch (EntityNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public List<FieldDetailsDataRow> getPhenoFieldDetailsList(FieldDetailsReportVO fdrVO) {
		return reportDao.getPhenoFieldDetailsList(fdrVO);
	}

	public List<CustomFieldDetailsDataRow> getPhenoCustomFieldDetailsList(CustomFieldDetailsReportVO fdrVO) {
		return reportDao.getPhenoCustomFieldDetailsList(fdrVO);
	}

	public List<PhenoCollection> getPhenoCollectionList(Study study) {
		return reportDao.getPhenoCollectionList(study);
	}

	public List<StudyUserRolePermissionsDataRow> getStudyUserRolePermissions(Study study) {
		return reportDao.getStudyUserRolePermissions(study);
	}

	public List<CustomFieldGroup> getQuestionnaireList(Study study) {
		return reportDao.getQuestionnaireList(study);
	}

	public List<ConsentDetailsDataRow> getStudyLevelConsentDetailsDataRowList(ConsentDetailsReportVO cdrVO) {
		return reportDao.getStudyLevelConsentDetailsDataRowList(cdrVO);
	}
	
	public List<ConsentDetailsDataRow> getStudyLevelConsentOtherIDDetailsDataRowList(ConsentDetailsReportVO cdrVO) {
		return reportDao.getStudyLevelConsentOtherIDDetailsDataRowList(cdrVO);
	}
	
	public List<ResearcherCostDataRow> getResearcherBillableItemTypeCostData(
				ResearcherCostResportVO researcherCostResportVO) {
			// TODO Auto-generated method stub
			return reportDao.getResearcherBillableItemTypeCostData(researcherCostResportVO);
	}

	public List<Researcher> searchResearcherByStudyId(final Long studyId) {
		// TODO Auto-generated method stub
		return reportDao.searchResearcherByStudyId(studyId);
	}

	public List<ResearcherDetailCostDataRow> getBillableItemDetailCostData(
			ResearcherCostResportVO researcherCostResportVO) {
		// TODO Auto-generated method stub
		return reportDao.getBillableItemDetailCostData(researcherCostResportVO);
	}

	public List<Study> getStudyList() throws EntityNotFoundException {
		
		Study searchStudyCriteria = new Study();
		Subject currentUser = SecurityUtils.getSubject();
		ArkUser arkUser = arkCommonService.getArkUser(currentUser.getPrincipal().toString());
		ArkUserVO arkUserVo = new ArkUserVO();
		arkUserVo.setArkUserEntity(arkUser);
		arkUserVo.setStudy(searchStudyCriteria);
		return arkCommonService.getStudyListForUser(arkUserVo);
	}
	
	public List<BioSampletype> getBiospecimenTypeList() throws EntityNotFoundException {
		return iLimsService.getBioSampleTypes();
	}

	public List<BiospecimenSummaryDataRow> getBiospecimenSummaryData(
			BiospecimenSummaryReportVO biospecimenSummaryReportVO) {
		// TODO Auto-generated method stub
		return reportDao.getBiospecimenSummaryData(biospecimenSummaryReportVO);
	}
	
	public List<BiospecimenNucleicAcidSummaryDataRow> getBiospecimenNucleicAcidSummaryData(
			BiospecimenNucleicAcidSummaryReportVO biospecimenNucleicAcidSummaryReportVO) {
		// TODO Auto-generated method stub
		return reportDao.getBiospecimenNucleicAcidSummaryData(biospecimenNucleicAcidSummaryReportVO);
	}

	public List<BiospecimenDetailsDataRow> getBiospecimenDetailsData(
			BiospecimenDetailsReportVO biospecimenDetailReportVO) {
		// TODO Auto-generated method stub
		return reportDao.getBiospecimenDetailsData(biospecimenDetailReportVO);
	}
	
	

}
