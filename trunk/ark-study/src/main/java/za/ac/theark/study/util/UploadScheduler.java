/**
 * 
 */
package za.ac.theark.study.util;

import java.util.List;

import org.apache.wicket.spring.injection.annot.SpringBean;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import za.ac.theark.core.model.study.entity.ArkRedcap;
import za.ac.theark.core.service.IArkRedcapService;
import za.ac.theark.study.util.REDCapJob;
import za.ac.theark.study.util.UploadScheduler;

/**
 * @author Freedom
 *
 */
@Service("uploadScheduler")
public class UploadScheduler implements Job {
	
	protected transient Logger		log					= LoggerFactory.getLogger(UploadScheduler.class);
	
	private UploadJobs uploadJobs;
	ApplicationContext applicationContext;
	IArkRedcapService iArkAdminService;
	
	public UploadJobs getUploadJobs() {
		return uploadJobs;
	}

	/*@Autowired
	public void setUploadJobs(UploadJobs uploadJobs) {
		this.uploadJobs = uploadJobs;
	}*/

	public void execute(JobExecutionContext context)
			throws JobExecutionException {
		try{
			StdSchedulerFactory s = new StdSchedulerFactory();
			Scheduler scheduler = s.getScheduler("MyQuartzScheduler");
			
			 List<ArkRedcap> con = (List<ArkRedcap>)context.getJobDetail().getJobDataMap().get("iArkAdminService");
			 
			 for(ArkRedcap ar : con){
				 
			 }
		
			UploadJobs(scheduler);
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}	
	
	private void UploadJobs(Scheduler scheduler) {
		//iArkAdminService = (IArkAdminService<?>) ac.getBean("iArkAdminService");
			
		try{
    		
			List<ArkRedcap> connections = iArkAdminService.getArkRedcapList();
			
    		for(ArkRedcap ar : connections){
    			JobKey jk = new JobKey(ar.getId().toString());
    			
    			JobDetail job = JobBuilder.newJob(REDCapJob.class)
    					.withIdentity(jk)
    					.build();
    			
    			job.getJobDataMap().put("arkRedcap", ar);
    			
    			Trigger t = TriggerBuilder
    				.newTrigger()
    				.withIdentity(ar.getId().toString())
    				.withSchedule(
    						CronScheduleBuilder.cronSchedule("0/5 * * * * ?")
    						)
    				.build();
    			scheduler.scheduleJob(job, t);
    			log.info("Job " + job.getKey() + " has been scheduled.");
    		}
    		
    	}catch(Exception ex){
    		ex.printStackTrace();
    	}
    }
}
