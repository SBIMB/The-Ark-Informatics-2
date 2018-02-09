package za.ac.theark.study.util;

import java.util.List;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import za.ac.theark.core.model.study.entity.ArkRedcap;
import za.ac.theark.core.service.IArkRedcapService;

public class JobScheduler {
	
	@Autowired
	IArkRedcapService iArkAdminService;
	
	public void addJobs(){
	
	List<ArkRedcap> connections = (List<ArkRedcap>) iArkAdminService.getArkRedcapList();
	
		for(ArkRedcap ar : connections){
		
		}
	}

}
