package au.org.theark.report.web.component.viewReport.biospecimennucleicacidsummary;

import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;

import au.org.theark.report.model.vo.BiospecimenNucleicAcidSummaryReportVO;
import au.org.theark.report.model.vo.BiospecimenSummaryReportVO;
import au.org.theark.report.web.component.viewReport.ReportOutputPanel;
import au.org.theark.report.web.component.viewReport.biospecimennucleicacidsummary.filterform.BiospecimenNucleicAcidSummaryFilterForm;

public class ReportFilterPanel extends Panel {

	private static final long	serialVersionUID	= 1L;

	AjaxButton						generateButton;

	public ReportFilterPanel(String id) {
		super(id);
	}

	public void initialisePanel(CompoundPropertyModel<BiospecimenNucleicAcidSummaryReportVO> cpModel, FeedbackPanel feedbackPanel, ReportOutputPanel reportOutputPanel) {
		BiospecimenNucleicAcidSummaryFilterForm biospecimenNucleicAcidSummaryFilterForm = new BiospecimenNucleicAcidSummaryFilterForm("filterForm", cpModel);
		biospecimenNucleicAcidSummaryFilterForm.initialiseFilterForm(feedbackPanel, reportOutputPanel);
		add(biospecimenNucleicAcidSummaryFilterForm);
	}

}
