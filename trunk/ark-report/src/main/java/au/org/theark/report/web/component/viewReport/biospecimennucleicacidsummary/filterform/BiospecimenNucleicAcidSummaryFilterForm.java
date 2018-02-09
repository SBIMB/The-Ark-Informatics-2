package au.org.theark.report.web.component.viewReport.biospecimennucleicacidsummary.filterform;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.protocol.http.WebApplication;
import org.wicketstuff.jasperreports.JRConcreteResource;
import org.wicketstuff.jasperreports.JRResource;
import org.wicketstuff.jasperreports.handlers.CsvResourceHandler;
import org.wicketstuff.jasperreports.handlers.PdfResourceHandler;

import au.org.theark.core.exception.EntityNotFoundException;
import au.org.theark.core.model.lims.entity.BioSampletype;
import au.org.theark.core.model.report.entity.ReportOutputFormat;
import au.org.theark.core.model.report.entity.ReportTemplate;
import au.org.theark.core.model.study.entity.Study;
import au.org.theark.report.model.vo.BiospecimenNucleicAcidSummaryReportVO;
import au.org.theark.report.web.Constants;
import au.org.theark.report.web.component.viewReport.biospecimennucleicacidsummary.BiospecimenNucleicAcidSummaryReportDataSource;
import au.org.theark.report.web.component.viewReport.form.AbstractReportFilterForm;

public class BiospecimenNucleicAcidSummaryFilterForm extends AbstractReportFilterForm<BiospecimenNucleicAcidSummaryReportVO> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private DropDownChoice<Study> studyDropDown;
	private DropDownChoice<BioSampletype> biospecimenTypeDropDown;
	private TextField<String> subjectUidTextField;

	public BiospecimenNucleicAcidSummaryFilterForm(String id,
			CompoundPropertyModel<BiospecimenNucleicAcidSummaryReportVO> model) {
		super(id, model);
		this.cpModel = model;
	}

	@Override
	protected void initialiseCustomFilterComponents() {
		this.subjectUidTextField=new TextField<String>(Constants.BIOSPECIMEN_SUMMARY_REPORT_SUBJECT_UID);
		initStudyDropDown();
		initBiospecimenTypeDropDown();
		addFilterFormComponents();
		addValidators();
	}
	
	private void initStudyDropDown() {
		List<Study> studies =null;
		try{
			studies= reportService.getStudyList();
		}catch(EntityNotFoundException ene){
			
			ene.printStackTrace();
		}
		ChoiceRenderer defaultChoiceRenderer = new ChoiceRenderer(au.org.theark.core.Constants.NAME, au.org.theark.core.Constants.ID);
		this.studyDropDown = new DropDownChoice(Constants.BIOSPECIMEN_SUMMARY_REPORT_STUDY, studies, defaultChoiceRenderer);
	}
	
	private void initBiospecimenTypeDropDown() {
		List<BioSampletype> biospecimenTypes =null;
		try{
			biospecimenTypes= reportService.getBiospecimenTypeList();
		}catch(EntityNotFoundException ene){
			ene.printStackTrace();
		}
		ChoiceRenderer defaultChoiceRenderer = new ChoiceRenderer(au.org.theark.core.Constants.NAME, au.org.theark.core.Constants.ID);
		this.biospecimenTypeDropDown = new DropDownChoice(Constants.BIOSPECIMEN_SUMMARY_REPORT_BIOSPECIMENTYPE, biospecimenTypes, defaultChoiceRenderer);
	}
	
	private void addFilterFormComponents() {
		this.add(studyDropDown);
		this.add(biospecimenTypeDropDown);
		this.add(subjectUidTextField);
	}

	private void addValidators() {
		this.studyDropDown.setRequired(true).setLabel(new StringResourceModel(Constants.ERROR_BIOSPECIMEN_SUMMARY_REPORT_STUDY_REQUIRED, studyDropDown, new Model<String>(Constants.BIOSPECIMEN_SUMMARY_REPORT_STUDY_TAG)));		
	}

	@Override
	protected void onGenerateProcess(AjaxRequestTarget target) {
		ReportTemplate reportTemplate = cpModel.getObject().getSelectedReportTemplate();
		ReportOutputFormat reportOutputFormat = cpModel.getObject().getSelectedOutputFormat();
		BioSampletype ReportBiospecimenType = cpModel.getObject().getBiospecimenType();

		// show report
		ServletContext context = ((WebApplication) getApplication()).getServletContext();
		File reportFile = null;

		reportFile = new File(context.getRealPath("/reportTemplates/" + reportTemplate.getTemplatePath()));
		JasperDesign design = null;
		JasperReport report = null;
		try {
			design = JRXmlLoader.load(reportFile);
			if (design != null) {
				if(ReportBiospecimenType.getSampletype() != "Nucleic Acid" )
					design.setName(au.org.theark.report.service.Constants.LIMS_BIOSPECIMEN_SUMMARY_REPORT_NAME); // set the output file name to match report title
				else{
					design.setName(au.org.theark.report.service.Constants.LIMS_NUCLEIC_ACID_SUMMARY_REPORT_NAME);
				}
				
				if (reportOutputFormat.getName().equals(au.org.theark.report.service.Constants.CSV_REPORT_FORMAT)) {
					design.setIgnorePagination(true); // don't paginate CSVs
				}
				report = JasperCompileManager.compileReport(design);
			}
		}
		catch (JRException e) {
			reportFile = null;
			e.printStackTrace();
		}
		
		final Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("baseDir", new File(context.getRealPath("/reportTemplates")));
		
		Study selectedStudy =  getModelObject().getStudy();
		parameters.put("studyName", selectedStudy.getName());
		
		BioSampletype selectedBiospecimenType = getModelObject().getBiospecimenType();
		parameters.put("bioSpecimenTypeName", selectedBiospecimenType.getName());
		
		BiospecimenNucleicAcidSummaryReportVO biospecimenNucleicAcidSummaryReportVO = new BiospecimenNucleicAcidSummaryReportVO();
		biospecimenNucleicAcidSummaryReportVO.setStudy(getModelObject().getStudy());
		biospecimenNucleicAcidSummaryReportVO.setSubjectUID(getModelObject().getSubjectUID());
		biospecimenNucleicAcidSummaryReportVO.setBiospecimenType(getModelObject().getBiospecimenType());
		
		BiospecimenNucleicAcidSummaryReportDataSource reportDS= new BiospecimenNucleicAcidSummaryReportDataSource(reportService, biospecimenNucleicAcidSummaryReportVO);
		
		JRResource reportResource = null;
		if (reportOutputFormat.getName().equals(au.org.theark.report.service.Constants.PDF_REPORT_FORMAT)) {
			final JRResource pdfResource = new JRConcreteResource<PdfResourceHandler>(new PdfResourceHandler());
			pdfResource.setJasperReport(report);
			pdfResource.setReportParameters(parameters).setReportDataSource(reportDS);
			reportResource = pdfResource;
		}
		else if (reportOutputFormat.getName().equals(au.org.theark.report.service.Constants.CSV_REPORT_FORMAT)) {
			final JRResource csvResource = new JRConcreteResource<CsvResourceHandler>(new CsvResourceHandler());
			csvResource.setJasperReport(report);
			csvResource.setReportParameters(parameters).setReportDataSource(reportDS);
			reportResource = csvResource;
		}
		if (reportResource != null) {
			reportOutputPanel.setReportResource(reportResource);
			reportOutputPanel.setVisible(true);
			target.add(reportOutputPanel);
		}
	}

}