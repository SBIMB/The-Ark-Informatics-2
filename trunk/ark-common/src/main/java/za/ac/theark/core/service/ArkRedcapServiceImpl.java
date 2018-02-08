/*******************************************************************************
 * Copyright (c) 2011  University of Western Australia. All rights reserved.
 * 
 * This file is part of The Ark.
 * 
 * The Ark is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * 
 * The Ark is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package za.ac.theark.core.service;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import au.org.theark.core.vo.RedcapVO;
import za.ac.theark.core.dao.IRedcapDao;
import za.ac.theark.core.model.study.entity.ArkRedcap;
import za.ac.theark.core.model.study.entity.RedcapContent;
import za.ac.theark.core.model.study.entity.RedcapContentFormat;
import za.ac.theark.core.model.study.entity.RedcapContentType;
import za.ac.theark.core.model.study.entity.RedcapSyncRecurrence;

/**
 * The implementation of IAdminService. We want to auto-wire and hence use the @Service annotation.
 * 
 * @author Freedom Mukomana
 * 
 * 
 */
@Transactional
@Service(za.ac.theark.core.service.Constants.ARK_ADMIN_SERVICE)
public class ArkRedcapServiceImpl<T> implements IArkRedcapService<T> {
	
	public ArkRedcapServiceImpl() {
		super();
		log.info("ArkAdminService has been created.");
	}

	protected transient Logger		log					= LoggerFactory.getLogger(ArkRedcapServiceImpl.class);
	private IRedcapDao	iRedcapDao;

	
	
	
	public IRedcapDao getRedcapDao() {
		return iRedcapDao;
	}

	@Autowired
	public void setRedCapDao(IRedcapDao iRedcapDao) {
		log.info("RedCapDAO Wired");
		this.iRedcapDao = iRedcapDao;
	}

	@Override
	public ArkRedcap getArkRedcapByName(String name) {
		return iRedcapDao.getArkRedcapByName(name);
	}
	
	@Override
	public List<ArkRedcap> getArkRedcapList() {
		
			log.info("ArkAdminService is null");
			
			/*if(true)
				throw new IllegalAccessError();*/
			
		return iRedcapDao.getArkRedcapList();
	}

	@Override
	public ArkRedcap getArkRedcap(Long id) {
		return iRedcapDao.getArkRedcap(id);
	}

	@Override
	public List<ArkRedcap> searchArkRedcap(ArkRedcap arkRedap) {
		return iRedcapDao.searchArkRedcap(arkRedap);
	}

	@Override
	public List<RedcapContentType> getRedcapContentTypeList() {
		return iRedcapDao.getRedcapContentTypeList();
	}
	
	@Override
	public List<RedcapContent> getRedcapContentList() {
		return iRedcapDao.getRedcapContentList();
	}

	@Override
	public List<RedcapContentFormat> getRedcapContentFormatList() {
		return iRedcapDao.getRedcapContentFormatList();
	}

	@Override
	public long getArkRedcapCount(ArkRedcap arkRedcapCriteria) {
		return iRedcapDao.getArkRedcapCount(arkRedcapCriteria);
	}

	@Override
	public List<ArkRedcap> searchPageableArkRedcaps(ArkRedcap arkRedcapCriteria, int first, int count) {
		return iRedcapDao.searchPageableArkRedcaps(arkRedcapCriteria, first, count);
	}

	@Override
	public List<RedcapSyncRecurrence> getRedcapSyncRecurrenceList() {
		return iRedcapDao.getRedcapSyncRecurrenceList();
	}
	
	
}