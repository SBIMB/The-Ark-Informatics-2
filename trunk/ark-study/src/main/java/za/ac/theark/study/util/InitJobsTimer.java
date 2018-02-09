package za.ac.theark.study.util;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import za.ac.theark.core.model.study.entity.ArkRedcap;
import za.ac.theark.core.service.IArkRedcapService;

@Service("initJobsTimer")
public class InitJobsTimer extends TimerTask{
	
		private final static long fONCE_PER_DAY = 5*1000;//1000*60*60*24;
	private final static int fONE_DAY = 7;
	private final static int fFOUR_AM = 0;
	private final static int fZERO_MINUTES = 40;
	private Timer timer = new Timer();
	
	@Autowired
	IArkRedcapService iArkAdminService;
	
	@Autowired
	RedcapRecordImporterTimerServiceImpl RedcapRecordImporterTimerService;

	@Override
	public void run() {
		/*RedcapRecordImporter rri = new RedcapRecordImporter();
		rri.setiArkAdminService(iArkAdminService);
		timer.schedule(rri, 30*1000);*/
		timer.scheduleAtFixedRate(RedcapRecordImporterTimerService, getTomorrowMorning4am(), fONCE_PER_DAY);
	}
	
	public void triggerService(){
		Timer timer = new Timer();
		timer.schedule(RedcapRecordImporterTimerService, 30*1000);
		//addJobs();
	}
	
	void addJobs(){
		List<ArkRedcap> connections = (List<ArkRedcap>) iArkAdminService.getArkRedcapList();
		
		for(ArkRedcap arkRedcap : connections){
			Timer timer = new Timer();
			timer.schedule(RedcapRecordImporterTimerService, 30*1000);
		}
	}
	
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
}
