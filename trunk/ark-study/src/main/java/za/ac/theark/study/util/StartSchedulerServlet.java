/**
 * 
 */
package za.ac.theark.study.util;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;

import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * @author Freedom
 *
 */
@Component
public class StartSchedulerServlet extends HttpServlet{
	
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 3934862641683942436L;
	protected transient Logger		log					= LoggerFactory.getLogger(StartSchedulerServlet.class);
	
	/*@SpringBean(name="initJobsTimer")
	InitJobsTimer initJobsTimer;
	
	@SpringBean(name="initJobsScheduler")
	InitJobsScheduler initJobsScheduler;*/
	
	@Override
	public void init(ServletConfig cfg) throws javax.servlet.ServletException {
		super.init(cfg);
		ServletContext servletContext = this.getServletContext();
		WebApplicationContext webApplicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
		InitJobsScheduler initJobsScheduler = (InitJobsScheduler)webApplicationContext.getBean("initJobsScheduler");
		
		//Comment out to use timer instead of use scheduler
		initJobsScheduler.triggerService();
		log.info("About to initiliase jobs");
		initJobsScheduler.addJobs();
		log.info("Have initiliased jobs");
		
	}

	
}
