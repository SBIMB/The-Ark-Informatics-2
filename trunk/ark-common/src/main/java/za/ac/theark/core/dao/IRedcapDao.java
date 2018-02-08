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
package za.ac.theark.core.dao;

import java.util.List;

import au.org.theark.core.vo.RedcapVO;
import za.ac.theark.core.model.study.entity.ArkRedcap;
import za.ac.theark.core.model.study.entity.RedcapContent;
import za.ac.theark.core.model.study.entity.RedcapContentFormat;
import za.ac.theark.core.model.study.entity.RedcapContentType;
import za.ac.theark.core.model.study.entity.RedcapSyncRecurrence;

/**
 * 
 * 
 * @author Freedom Mukomana
 * 
 * 
 */

public interface IRedcapDao {
	
	public List<ArkRedcap> getArkRedcapList();

	public ArkRedcap getArkRedcap(Long id);

	public List<RedcapContentType> getRedcapContentTypeList();
	
	public List<RedcapContent> getRedcapContentList();
	
	public List<RedcapContentFormat> getRedcapContentFormatList();

	public void deleteArkRedcap(ArkRedcap arkRedcap);

	public List<ArkRedcap> searchArkRedcap(ArkRedcap arkRedcap);

	public long getArkRedcapCount(ArkRedcap arkFunctionCriteria);

	public List<ArkRedcap> searchPageableArkRedcaps(ArkRedcap arkRedcapCriteria, int first, int count);

	public ArkRedcap getArkRedcapByName(String name);
	
	public void createOrUpdateArkRedcap(ArkRedcap arkRedcap);
	
	public List<RedcapSyncRecurrence> getRedcapSyncRecurrenceList();
}
