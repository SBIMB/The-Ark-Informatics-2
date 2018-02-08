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
package au.org.theark.admin.service;

import java.util.List;

import au.org.theark.admin.model.vo.AdminVO;
import au.org.theark.admin.model.vo.ArkRoleModuleFunctionVO;
import au.org.theark.core.model.study.entity.ArkFunction;
import au.org.theark.core.model.study.entity.ArkFunctionType;
import au.org.theark.core.model.study.entity.ArkModule;
import au.org.theark.core.model.study.entity.ArkModuleFunction;
import au.org.theark.core.model.study.entity.ArkModuleRole;
import au.org.theark.core.model.study.entity.ArkPermission;
import au.org.theark.core.model.study.entity.ArkRole;
import au.org.theark.core.model.study.entity.ArkRolePolicyTemplate;

public interface IAdminService<T> {
	
	//ArkRoleModuleFunctionVO
	
	public long getArkRoleModuleFunctionVOCount(ArkRoleModuleFunctionVO arkRoleModuleFunctionVO);
	
	public List<ArkRoleModuleFunctionVO> searchPageableArkRoleModuleFunctionVO(ArkRoleModuleFunctionVO arkRoleModuleFunctionVo, int first, int count);
	
	public List<ArkRoleModuleFunctionVO> getArkRoleModuleFunctionVoList(ArkRole arkRole);
	
	
	//Ark Function
	
	public ArkFunction getArkFunction(Long id);
	
	public List<ArkFunction> getArkFunctionList();
	
	public List<ArkFunction> getArkFunctionListByArkModule(ArkModule arkModule);
	
	public void createOrUpdateArkFunction(AdminVO adminVo);
	
	public void deleteArkFunction(AdminVO adminVo);
	
	public long getArkFunctionCount(ArkFunction arkFunctionCriteria);
	
	
	
	//ArkFunctionType
	
	public List<ArkFunctionType> getArkFunctionTypeList();
	
	
	//ArkModule
	
	public ArkModule getArkModule(Long id);
	
	public List<ArkModule> getArkModuleList(ArkRole arkRole);
	
	public List<ArkModule> getArkModuleList();
	
	public void createOrUpdateArkModule(AdminVO adminVo);

	public void deleteArkModule(AdminVO adminVo);

	public List<ArkModule> searchArkModule(ArkModule arkModule);

	public long getArkModuleCount(ArkModule arkModuleCriteria);

	public List<ArkModule> searchPageableArkModules(ArkModule arkModuleCriteria, int first, int count);	
	
		
	//ArkModuleFunction
		
	public ArkModuleFunction getArkModuleFunction(Long id);
	
	public long getArkModuleFunctionCount(ArkModuleFunction arkModuleFunction);
	
	public void createOrUpdateArkModuleFunction(AdminVO modelObject);
	
	public List<ArkFunction> searchArkFunction(ArkFunction arkFunction);
	
	public List<ArkModuleFunction> searchPageableArkModuleFunctions(ArkModuleFunction arkModuleFunctionCriteria, int first, int count);
	
	public List<ArkFunction> searchPageableArkFunctions(ArkFunction arkFunctionCriteria, int first, int count);
	
	
	//ArkModuleRole
	
	public ArkModuleRole getArkModuleRole(Long id);
	
	public long getArkModuleRoleCount(ArkModuleRole arkModuleRole);
	
	public List<ArkModuleRole> searchPageableArkModuleRoles(ArkModuleRole arkModulRoleCriteria, int first, int count);
	
	public void createArkModuleRole(AdminVO modelObject);
	
	public void updateArkModuleRole(AdminVO modelObject);
	
	public List<ArkModule> getArkModuleListByArkRole(ArkRole arkRole);
	
	
	//ArkPermission
	
	public ArkPermission getArkPermissionByName(String name);
	
	
	//ArkRole
	
	public ArkRole getArkRole(Long id);
	
	public ArkRole getArkRoleByName(String name);
	
	public long getArkRoleCount(ArkRole arkRoleCriteria);
	
	public List<ArkRole> getArkRoleList();
	
	public void createOrUpdateArkRole(AdminVO modelObject);
	
	public List<ArkRole> searchPageableArkRoles(ArkRole arkRoleCriteria, int first, int count);
	
	public List<ArkRole> getArkRoleListByArkModule(ArkModule arkModule);
	
	
	//ArkRolePolicyTemplate
	
	/**
	 * Create a new arkRolePolicyTemplate, via the reference AdminVO object
	 * 
	 * @param adminVo
	 */
	public void createArkRolePolicyTemplate(AdminVO adminVo);

	/**
	 * Update an arkRolePolicyTemplate, via the reference AdminVO object
	 * 
	 * @param adminVo
	 */
	public void updateArkRolePolicyTemplate(AdminVO adminVo);

	/**
	 * Delete an arkRolePolicyTemplate entity, via the reference AdminVO object
	 * 
	 * @param adminVo
	 */
	public void deleteArkRolePolicyTemplate(AdminVO adminVo);

	/**
	 * Create or update an arkRolePolicyTemplate entity
	 * 
	 * @param arkRolePolicyTemplate
	 */
	public void createOrUpdateArkRolePolicyTemplate(AdminVO adminVo);

	public ArkRolePolicyTemplate getArkRolePolicyTemplate(Long id);
	
	public List<ArkRolePolicyTemplate> getArkRolePolicyTemplateList(ArkRolePolicyTemplate arkRolePolicyTemplate);
	
	
	public void createOrUpdateArkRedcap(AdminVO adminVO);
	
	public void deleteArkRedcap(AdminVO adminVO);
}
