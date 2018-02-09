package za.ac.theark.study.util;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import za.ac.theark.core.service.IArkRedcapService;

@Service("startScheduler")
public class StartSchedulerListener implements ServletContextListener {
	
	protected transient Logger		log					= LoggerFactory.getLogger(StartSchedulerListener.class);
	//private StdSchedulerFactory factory = null;
	//public static final String QUARTZ_FACTORY_KEY = "org.quartz.impl.StdSchedulerFactory.KEY";
	//expressed in milliseconds
	 	
	private IArkRedcapService iArkAdminService;
	
	public void contextDestroyed(ServletContextEvent arg0) {
		// TODO Auto-generated method stub
		 try
		    {
		        StdSchedulerFactory.getDefaultScheduler().shutdown();
		    } catch (SchedulerException ex)
		    {
		        log.info("catch");
		    }
	}

	public void contextInitialized(ServletContextEvent sce) {
		
		// TODO Auto-generated method stub
		 System.out.println("THE APPLICATION STARTED");
		    ServletContext ctx = sce.getServletContext();
		    
		    /*JobDetail jobDetail = JobBuilder
		    		.newJob(UploadScheduler.class)
		    		.withIdentity("loadJobs")
		    		.build();
		    
		    List<ArkRedcap> con = iArkAdminService.getArkRedcapList();
		    
		    JobDataMap jobDataMap = new JobDataMap();
		    
		    jobDataMap.put("Conn", con);
		    
		    DateBuilder
		    	.newDate();
			Date dbBuilder = DateBuilder
		    	.futureDate(30, IntervalUnit.SECOND);
		    	
		    		

		    Trigger trigger = TriggerBuilder
		    		.newTrigger()
		    		.withIdentity("loadJobsTrigger")
		    		.startAt(dbBuilder)
		    		.build();


		        try {
		        factory = new StdSchedulerFactory();
		        ctx.setAttribute(QUARTZ_FACTORY_KEY, factory);
		        Scheduler scheduler= factory.getScheduler();
		        log.info("The scheduler is now starting...");
		        scheduler.start();
		        scheduler.scheduleJob(jobDetail, trigger);
		    } catch (SchedulerException e) {
		        e.printStackTrace();
		    }*/
		
		//Timer timer = new Timer();
		
		//InitJobs initJobs = new InitJobs();
		//timer.schedule(initJobs, 10*1000);
	}
}
