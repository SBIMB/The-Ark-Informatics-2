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
package za.ac.theark.core.dao;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import au.org.theark.core.dao.HibernateSessionDao;
import za.ac.theark.core.model.study.entity.ArkRedcap;
import za.ac.theark.core.model.study.entity.RedcapContent;
import za.ac.theark.core.model.study.entity.RedcapContentFormat;
import za.ac.theark.core.model.study.entity.RedcapContentType;
import za.ac.theark.core.model.study.entity.RedcapSyncRecurrence;

/**
 * The implementation of IAdminDao. We want to auto-wire and hence use the @Repository annotation.
 * 
 * @author Freedom Mukomana
 * 
 * 
 */

@SuppressWarnings("unchecked")
@Repository(za.ac.theark.core.dao.Constants.ARK_ADMIN_SERVICE)
public class RedcapDao extends HibernateSessionDao implements IRedcapDao {
	static Logger	log	= LoggerFactory.getLogger(RedcapDao.class);

	public List<ArkRedcap> getArkRedcapList() {
		Criteria criteria = getSession().createCriteria(ArkRedcap.class);
		return criteria.list();
	}

	public ArkRedcap getArkRedcap(Long id) {
		Criteria criteria = getSession().createCriteria(ArkRedcap.class);
		criteria.add(Restrictions.eq("id", id));
		return (ArkRedcap) criteria.uniqueResult();
	}

	public List<RedcapContentType> getRedcapContentTypeList() {
		Criteria criteria = getSession().createCriteria(RedcapContentType.class);
		return criteria.list();
	}
	
	public List<RedcapContent> getRedcapContentList() {
		Criteria criteria = getSession().createCriteria(RedcapContent.class);
		return criteria.list();
	}

	public List<RedcapContentFormat> getRedcapContentFormatList() {
		Criteria criteria = getSession().createCriteria(RedcapContentFormat.class);
		return criteria.list();
	}

	public void createOrUpdateArkRedcap(ArkRedcap arkRedcap) {
		log.info("Inside RedCap DAO");
		getSession().saveOrUpdate(arkRedcap);
	}

	public void deleteArkRedcap(ArkRedcap arkRedcap) {
		getSession().delete(arkRedcap);
	}

	public List<ArkRedcap> searchArkRedcap(ArkRedcap arkRedcap) {
		Criteria criteria = getSession().createCriteria(ArkRedcap.class);
		if (arkRedcap.getId() != null) {
			criteria.add(Restrictions.eq("id", arkRedcap.getId()));
		}

		if (arkRedcap.getName() != null) {
			criteria.add(Restrictions.ilike("name", arkRedcap.getName(), MatchMode.ANYWHERE));
		}
		return criteria.list();
	}

	public long getArkRedcapCount(ArkRedcap arkRedcapCriteria) {
		Criteria criteria = buildArkRedcapCriteria(arkRedcapCriteria);
		criteria.setProjection(Projections.rowCount());
		Long totalCount = (Long) criteria.uniqueResult();
		return totalCount;
	}

	public List<ArkRedcap> searchPageableArkRedcaps(ArkRedcap arkRedcapCriteria, int first, int count) {
		Criteria criteria = buildArkRedcapCriteria(arkRedcapCriteria);
		criteria.setFirstResult(first);
		criteria.setMaxResults(count);
		List<ArkRedcap> list = criteria.list();

		return list;
	}

	protected Criteria buildArkRedcapCriteria(ArkRedcap arkRedcapCriteria) {
		Criteria criteria = getSession().createCriteria(ArkRedcap.class);

		if (arkRedcapCriteria.getId() != null)
			criteria.add(Restrictions.eq("id", arkRedcapCriteria.getId()));

		if (arkRedcapCriteria.getName() != null)
			criteria.add(Restrictions.ilike("name", arkRedcapCriteria.getName(), MatchMode.ANYWHERE));

		if (arkRedcapCriteria.getContent() != null)
			criteria.add(Restrictions.eq("content", arkRedcapCriteria.getContent()));

		return criteria;
	}

	public ArkRedcap getArkRedcapByName(String name) {
		Criteria criteria = getSession().createCriteria(ArkRedcap.class);
		criteria.add(Restrictions.eq("name", name));
		return (ArkRedcap) criteria.list().get(0);
	}

	
	public List<RedcapSyncRecurrence> getRedcapSyncRecurrenceList() {
		Criteria criteria = getSession().createCriteria(RedcapSyncRecurrence.class);
		for(RedcapSyncRecurrence rsr:(List<RedcapSyncRecurrence>)criteria.list()){
			log.info(rsr.getName());
		}
		return criteria.list();
	}
	
	
}
