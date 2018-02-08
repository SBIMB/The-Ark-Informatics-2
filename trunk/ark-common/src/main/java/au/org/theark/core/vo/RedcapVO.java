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
package au.org.theark.core.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import au.org.theark.core.model.study.entity.ArkFunction;
import au.org.theark.core.model.study.entity.ArkModule;
import au.org.theark.core.model.study.entity.ArkModuleFunction;
import au.org.theark.core.model.study.entity.ArkModuleRole;
import za.ac.theark.core.model.study.entity.ArkRedcap;
import au.org.theark.core.model.study.entity.ArkRole;
import au.org.theark.core.model.study.entity.ArkRolePolicyTemplate;
import au.org.theark.core.model.study.entity.Study;

/**
 * @author cellis
 * 
 */
public class RedcapVO implements Serializable {

	private static final long					serialVersionUID	= -3939245546324873647L;

	private ArkRedcap							arkRedcap;
	

	public RedcapVO() {
		
		this.arkRedcap = new ArkRedcap();
	}

	
	
	/**
	 * @param arkRedcap the arkRedcap to set
	 */
	public ArkRedcap getArkRedcap() {
		return arkRedcap;
	}

	public void setArkRedcap(ArkRedcap arkRedcap) {
		this.arkRedcap = arkRedcap;
	}

}
