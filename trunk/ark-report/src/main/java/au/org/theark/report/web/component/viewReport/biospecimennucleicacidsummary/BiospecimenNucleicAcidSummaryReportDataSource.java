package au.org.theark.report.web.component.viewReport.biospecimennucleicacidsummary;

import java.io.Serializable;
import java.util.List;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import au.org.theark.report.model.vo.BiospecimenNucleicAcidSummaryReportVO;
import au.org.theark.report.model.vo.BiospecimenSummaryReportVO;
import au.org.theark.report.model.vo.report.BiospecimenNucleicAcidSummaryDataRow;
import au.org.theark.report.model.vo.report.BiospecimenSummaryDataRow;
import au.org.theark.report.service.IReportService;

public class BiospecimenNucleicAcidSummaryReportDataSource implements Serializable,
		JRDataSource {
	
	
	private static final long				serialVersionUID	= 1L;

	private List<BiospecimenNucleicAcidSummaryDataRow>          	data			= null;

	private int									index			= -1;
	
	public BiospecimenNucleicAcidSummaryReportDataSource(final IReportService reportService, final BiospecimenNucleicAcidSummaryReportVO  biospecimenNucleicAcidSummaryReportVo) {
		data =  reportService.getBiospecimenNucleicAcidSummaryData(biospecimenNucleicAcidSummaryReportVo);
	}

	public Object getFieldValue(JRField field) throws JRException {
		Object value = null;
		String fieldName = field.getName();
		if ("studyName".equalsIgnoreCase(fieldName)) {
			value = data.get(index).getStudyName();
		}
		else if ("subjectUId".equalsIgnoreCase(fieldName)) {
			value = data.get(index).getSubjectUId();
		}
		else if ("biospecimenId".equalsIgnoreCase(fieldName)) {
			value = data.get(index).getBiospecimenId();
		}
		else if ("parentId".equalsIgnoreCase(fieldName)) {
			value = data.get(index).getParentId();
		}
		else if ("sampleType".equalsIgnoreCase(fieldName)) {
			value = data.get(index).getSampleType();
		}
		else if ("quantity".equalsIgnoreCase(fieldName)) {
			value = data.get(index).getQuantity();
		}
		else if ("biospecimenUid".equalsIgnoreCase(fieldName)) {
			value = data.get(index).getBiospecimenUid();
		}
		else if ("concentration".equalsIgnoreCase(fieldName)) {
			value = data.get(index).getConcentration();
		}
		else if ("purity260230".equalsIgnoreCase(fieldName)) {
			value = data.get(index).getPurity260230();
		}
		else if ("purity260280".equalsIgnoreCase(fieldName)) {
			value = data.get(index).getPurity260280();
		}
		
		else if ("amountDNA".equalsIgnoreCase(fieldName)) {
			value = data.get(index).getAmountDNA();
		}
		
		else if ("grade".equalsIgnoreCase(fieldName)) {
			value = data.get(index).getGrade();
		}
		
		return value;
	}

	public boolean next() throws JRException {
		index++;
		// Need to return false for when (index == data.size())
		// so as to stop the current report from consuming any more data.
		// However, when another report attempts to consume data it will
		// have advanced the index and thus we can reset it automatically
		if (index > data.size()) {
			index = 0;
		}
		return (index < data.size());
	}

}
