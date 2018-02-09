package za.ac.theark.study.util;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import za.ac.theark.core.model.study.entity.ArkRedcap;
import za.ac.theark.core.service.IArkRedcapService;

@Service("uploadJobs")
public class UploadJobs {
	
	//@SpringBean(name = za.ac.theark.admin.service.Constants.ARK_ADMIN_SERVICE)
	private IArkRedcapService<?> iArkAdminService;
	protected transient Logger		log					= LoggerFactory.getLogger(UploadJobs.class);
	
	public UploadJobs() {
		super();
		log.info("Uploadjobs has been created");
	}

	protected List<ArkRedcap> getJobs(){
		
		return iArkAdminService.getArkRedcapList();
		
	}

	public IArkRedcapService<?> getiArkAdminService() {
		return iArkAdminService;
	}

	@Autowired
	public void setiArkAdminService(IArkRedcapService<?> iArkAdminService) {
		this.iArkAdminService = iArkAdminService;
	}
	

}
