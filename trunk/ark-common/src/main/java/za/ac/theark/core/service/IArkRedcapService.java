/*******************************************************************************
 * Copyright (c) 2011  University of Witswatersrand. All rights reserved.
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

import java.util.List;

import au.org.theark.core.vo.RedcapVO;
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

public interface IArkRedcapService<T> {
	
	public ArkRedcap getArkRedcap(Long id);
	
	public ArkRedcap getArkRedcapByName(String name);
	
	public List<ArkRedcap> getArkRedcapList();
	
	public List<RedcapContentFormat> getRedcapContentFormatList();
	
	public List<RedcapContent> getRedcapContentList();
	
	public List<RedcapContentType> getRedcapContentTypeList();
	
	public List<ArkRedcap> searchArkRedcap(ArkRedcap arkRedap);
	
	public long getArkRedcapCount(ArkRedcap arkRedcapCriteria);
	
	public List<ArkRedcap> searchPageableArkRedcaps(ArkRedcap arkRedcapCriteria, int first, int count);
	
	public List<RedcapSyncRecurrence> getRedcapSyncRecurrenceList();
}
