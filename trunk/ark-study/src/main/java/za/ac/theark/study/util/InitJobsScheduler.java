package za.ac.theark.study.util;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.hibernate.hql.internal.classic.GroupByParser;
import org.jfree.chart.renderer.category.GroupedStackedBarRenderer;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import au.org.theark.core.service.IArkCommonService;
import au.org.theark.study.service.IStudyService;
import za.ac.theark.core.model.study.entity.ArkRedcap;
import za.ac.theark.core.service.IArkRedcapService;

@Service("initJobsScheduler")
public class InitJobsScheduler extends TimerTask {
	
	protected transient Logger		log					= LoggerFactory.getLogger(InitJobsScheduler.class);
	
	private final static long fONCE_PER_DAY = 5*1000;//1000*60*60*24;
	private final static int fONE_DAY = 7;
	private final static int fFOUR_AM = 0;
	private final static int fZERO_MINUTES = 40;
	private Timer timer = new Timer();
	
	@Autowired
	IArkRedcapService iArkAdminService;
	
	@Autowired
	public IArkCommonService<?> iArkCommonService;
	
	@Autowired
	public IStudyService iStudyService;	
	
	@Autowired IArkRedcapService<?> iArkRedcapService;
	
	
	@Autowired
	RedcapRecordImporterSchServiceImpl redcapRecordImporterSchServiceImpl;

	/*@Override
	public void run() {
		RedcapRecordImporter rri = new RedcapRecordImporter();
		rri.setiArkAdminService(iArkAdminService);
		timer.schedule(rri, 30*1000);
		timer.scheduleAtFixedRate(RedcapRecordImporterService, getTomorrowMorning4am(), fONCE_PER_DAY);
	}
	*/
	
	public InitJobsScheduler() {
		super();
		
		timer.schedule(this, Long.parseLong("10000"));
		log.info("Timer set?");
	}
	
	public void triggerService(){
		Timer timer = new Timer();
		//timer.schedule(RedcapRecordImporterSchServiceImpl, 30*1000);
		addJobs();
	}
	
	protected void addJobs(){
		List<ArkRedcap> connections = (List<ArkRedcap>) iArkAdminService.getArkRedcapList();
		
		try{
			StdSchedulerFactory s = new StdSchedulerFactory();
			Scheduler scheduler = s.getScheduler("MyQuartzScheduler");
			
			scheduler.getContext().put("arkAdminService", iArkAdminService);
			scheduler.getContext().put("arkCommonService", iArkCommonService);
			scheduler.getContext().put("studyService", iStudyService);
			scheduler.getContext().put("arkRedcapService", iArkRedcapService);
			
			if (scheduler.isInStandbyMode() == true){
				scheduler.start();
			}
			
			log.info("Is the scheduler running? " + scheduler.isStarted());
			
			log.info("Adding jobs ");
    		for(ArkRedcap ar : connections){
    			log.warn("ArkREDCap Status" + ar.getId() + " " + ar.getName().toString() + " " +ar.getStudy().getId() + " " +ar.getSubStudy().getId() + " " + ar.getEnabled());
    			if(ar.getEnabled()==true){
    				JobKey jk = new JobKey(ar.getId().toString());
    			
    				JobDetail job = JobBuilder.newJob(RedcapRecordImporterSchServiceImpl.class)
    					.withIdentity(jk)
    					.build();
    				
    				job.getJobDataMap().put("arkRedcap", ar);
    			
    				Trigger t = TriggerBuilder
    						.newTrigger()
    						.startNow()
    						.withIdentity(ar.getId().toString())
    						.withSchedule(SimpleScheduleBuilder.simpleSchedule()
    							.withIntervalInHours(3)
    							.repeatForever()
    						//CronScheduleBuilder.cronSchedule("0/5 * * * * ?")
    						)
    						.build();
    			
    				scheduler.scheduleJob(job, t);
    			}
    		}
    		
    	}catch(Exception ex){
    		ex.printStackTrace();
    	}
    }

	/*protected void addJob(ArkRedcap ar){
		int interval = 0;
				
		try{
			StdSchedulerFactory s = new StdSchedulerFactory();
			Scheduler scheduler = s.getDefaultScheduler();
			
			scheduler.getContext().put("arkAdminService", iArkAdminService);
			scheduler.getContext().put("arkCommonService", iArkCommonService);
			scheduler.getContext().put("studyService", iStudyService);
			scheduler.getContext().put("arkRedcapService", iArkRedcapService);
			
			if (scheduler.isInStandbyMode() == true){
				scheduler.start();
			}
			
			log.info("Is the scheduler running? " + s.getDefaultScheduler().isStarted());
			
			log.info("Adding jobs");
    		
			JobKey jk = new JobKey(ar.getId().toString());
    			
    		JobDetail job = JobBuilder.newJob(RedcapRecordImporterSchServiceImpl.class)
    				.withIdentity(jk)
    				.build();
    			
    		job.getJobDataMap().put("arkRedcap", ar);
    		
    		//get recurrence
    		switch (ar.getRedcapSyncRecurrence().getName().toUpperCase()){
    			case "DAILY" : 
    				interval = 24;
    				break;
    			case "WEEKLY" : 
    				interval = 168;
    				break;	
    			case "FORTNIGHT" : 
    				interval = 336;
    				break;
    			case "MONTHLY" : 
    				interval = 730;
    				break;
    		}
    		
    		
    			
    		Trigger t = TriggerBuilder
    			.newTrigger()
    			.withIdentity(ar.getId().toString())
    			//.startAt(ar.getStartDateTime())
    			.withSchedule(SimpleScheduleBuilder.simpleSchedule()
    						//.withIntervalInHours(interval)
    					.withIntervalInMinutes(3)
    						.repeatForever()
    						//CronScheduleBuilder.cronSchedule("0/5 * * * * ?")
    					)
    			.build();
    			
    		scheduler.scheduleJob(job, t);
    		log.info("Job " + job.getKey() + " has been scheduled.");
    	  		
    	}catch(Exception ex){
    		ex.printStackTrace();
    	}
    }*/
	
	private static Date getTomorrowMorning4am(){
	    Calendar tomorrow = new GregorianCalendar();
	    tomorrow.add(Calendar.DATE, fONE_DAY);
	    Calendar result = new GregorianCalendar(
	      tomorrow.get(Calendar.YEAR),
	      tomorrow.get(Calendar.MONTH),
	      tomorrow.get(Calendar.DATE),
	      fFOUR_AM,
	      fZERO_MINUTES
	    );
	   return result.getTime();
	  }

	@Override
	public void run() {
		addJobs();
		log.info("Timer excuted ");
		StdSchedulerFactory s = new StdSchedulerFactory();
		log.info("Listing Jobs");
		try {
			Scheduler sc = s.getScheduler("MyQuartzScheduler");
			for(String group : sc.getJobGroupNames()){
				log.info("Got group, with " + sc.getJobKeys(GroupMatcher.anyJobGroup()).size());
				for(JobKey jk : sc.getJobKeys(GroupMatcher.anyJobGroup())){
					log.info("Job x" + jk.getGroup() +" "+ jk.getName());
				}
			}
		}
		catch (SchedulerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			timer = null;
		}
		
		
		
	}
}
