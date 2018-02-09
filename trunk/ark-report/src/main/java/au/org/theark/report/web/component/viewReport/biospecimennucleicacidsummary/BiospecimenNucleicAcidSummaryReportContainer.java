package au.org.theark.report.web.component.viewReport.biospecimennucleicacidsummary;

import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;

import au.org.theark.core.model.report.entity.ReportTemplate;
import au.org.theark.report.model.vo.BiospecimenNucleicAcidSummaryReportVO;
import au.org.theark.report.model.vo.BiospecimenSummaryReportVO;
import au.org.theark.report.web.component.viewReport.AbstractSelectedReportContainer;
import au.org.theark.report.web.component.viewReport.ReportOutputPanel;


public class BiospecimenNucleicAcidSummaryReportContainer extends
		AbstractSelectedReportContainer<BiospecimenNucleicAcidSummaryReportVO> {

	public BiospecimenNucleicAcidSummaryReportContainer(String id) {
		super(id);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void initialiseCPModel() {
		BiospecimenNucleicAcidSummaryReportVO grvVO = new BiospecimenNucleicAcidSummaryReportVO();
		cpModel = new CompoundPropertyModel<BiospecimenNucleicAcidSummaryReportVO>(grvVO);		
	}
	
	public void initialisePanel(FeedbackPanel feedbackPanel, ReportTemplate reportTemplate) {
		cpModel.getObject().setSelectedReportTemplate(reportTemplate);
		ReportFilterPanel rfp = new ReportFilterPanel("reportFilterPanel");
		// Initialise output panel without link
		ReportOutputPanel reportOutputPanel = new ReportOutputPanel("reportOutputPanel");
		reportOutputPanel.initialisePanel();
		reportOutputPanel.setOutputMarkupPlaceholderTag(true);

		rfp.initialisePanel(cpModel, feedbackPanel, reportOutputPanel);
		add(rfp);
		add(reportOutputPanel);
	}
	
}
