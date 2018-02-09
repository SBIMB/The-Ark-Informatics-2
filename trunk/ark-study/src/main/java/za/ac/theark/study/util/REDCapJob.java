package za.ac.theark.study.util;

import org.apache.wicket.spring.injection.annot.SpringBean;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import za.ac.theark.core.model.study.entity.ArkRedcap;
import za.ac.theark.core.service.IArkRedcapService;

public class REDCapJob implements Job {
	
	@SpringBean(name = za.ac.theark.core.service.Constants.ARK_ADMIN_SERVICE)
	IArkRedcapService<?> iArkAdminService;
	
	@Autowired
	RedcapRecordImporterSchServiceImpl redcapRecordImporterSchService;
	
	public static Long connectionID = 0L;

	@Override
	public void execute(JobExecutionContext context)
			throws JobExecutionException {
		ArkRedcap arkRedcap = (ArkRedcap)context.getJobDetail().getJobDataMap().get("connection");
		
		redcapRecordImporterSchService.getRecords(arkRedcap);
		//acc.getHeaders();
		//acc.formatRecords();

	}

}
