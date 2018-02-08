/*******************************************************************************
 * Copyright (c) 2015  University of Witwatersrand. All rights reserved.
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
package za.ac.theark.core.model.study.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.projectredcap.main.Config;
import org.projectredcap.main.Connection;

import au.org.theark.core.Constants;
import au.org.theark.core.model.study.entity.Study;
import au.org.theark.core.model.study.entity.YesNo;

/**
 * 
 * 
 * @author Freedom Mukomana
 * 
 * 
 */

@Entity
@Table(name = "ARK_REDCAP", schema = Constants.STUDY_SCHEMA)
public class ArkRedcap implements Serializable {

	private static final long serialVersionUID = 1L;
	private Long id;
	private String name;
	private String description;
	private String redcapUrl;
	private String token;
	private Integer redcapReportID; 
	private RedcapContentType type;
	private RedcapContent content;
	private RedcapContentFormat format;
	private Study study;
	private Study subStudy;
	private RedcapSyncRecurrence redcapSyncRecurrence;
	private Date startDateTime;
	private Boolean enabled;
	// private ArkModuleREDCap arkModuleREDCap;

	@Id
	@SequenceGenerator(name = "redcap_generator", sequenceName = "REDCAP_SEQUENCE")
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "redcap_generator")
	@Column(name = "ID", unique = true, nullable = false, precision = 22, scale = 0)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "NAME")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(name = "DESCRIPTION")
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "STUDY_ID")
	public Study getStudy() {
		return study;
	}

	public void setStudy(Study study) {
		this.study = study;
	}

	@Column(name = "REDCAP_URL")
	public String getRedcapUrl() {
		return redcapUrl;
	}

	public void setRedcapUrl(String redcapUrl) {
		this.redcapUrl = redcapUrl;
	}

	@Column(name = "TOKEN")
	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
	
	@Column(name = "REPORT_ID")
	public Integer getRedcapReportID() {
		return redcapReportID;
	}

	public void setRedcapReportID(Integer redcapReportID) {
		this.redcapReportID = redcapReportID;
	}

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "REDCAP_CONTENT_TYPE_ID")
	public RedcapContentType getType() {
		return type;
	}

	public void setType(RedcapContentType type) {
		this.type = type;
	}

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "CONTENT_ID")
	public RedcapContent getContent() {
		return content;
	}

	public void setContent(RedcapContent content) {
		this.content = content;
	}

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "REDCAP_CONTENT_FORMAT_ID")
	public RedcapContentFormat getFormat() {
		return format;
	}

	public void setFormat(RedcapContentFormat format) {
		this.format = format;
	}
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "REDCAP_SYNC_RECURRENCE_ID")
	public RedcapSyncRecurrence getRedcapSyncRecurrence() {
		return redcapSyncRecurrence;
	}

	public void setRedcapSyncRecurrence(RedcapSyncRecurrence redcapSyncRecurrence) {
		this.redcapSyncRecurrence = redcapSyncRecurrence;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "START_DATE_TIME")
	public Date getStartDateTime() {
		return startDateTime;
	}

	public void setStartDateTime(Date startDateTime) {
		this.startDateTime = startDateTime;
	}
	
	@Column(name = "ENABLED_ID", precision = 1, scale = 0)
	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ArkRedcap other = (ArkRedcap) obj;
		if (format == null){
			if(other.format != null)
				return false;
		}else if(!format.equals(other.format))
				return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (redcapUrl == null) {
			if (other.redcapUrl != null)
				return false;
		} else if (!redcapUrl.equals(other.redcapUrl))
			return false;
		if (redcapReportID == null) {
			if (other.redcapReportID != null)
				return false;
		} else if (!redcapReportID.equals(other.redcapReportID))
			return false;
		if (token == null) {
			if (other.token != null)
				return false;
		} else if (!token.equals(other.token))
			return false;
		if (content == null){
			if(other.content != other.content)
				return false;
		}else if(!content.equals(other.content)){
			return false;
		}
				
		return true;
	}

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "SUBSTUDY_ID")
	public Study getSubStudy() {
		return subStudy;
	}

	public void setSubStudy(Study subStudy) {
		this.subStudy = subStudy;
	}
	
	public int testConnection(){
		Config c = new Config();
		
		c.setAPI_TOKEN(this.getToken());
		c.setAPI_URL(this.getRedcapUrl());
		c.setAPI_FORMAT(this.getFormat().getName());
		c.setAPI_TYPE(this.getType().getName());
		c.setAPI_CONTENT(this.getContent().toString());
		c.setAPI_REPORT_ID(this.getRedcapReportID());
		c.setAPI_RAWORLABEL("label");
		
		Connection conn = new Connection();
		
		conn.configure(c);
				
		return conn.test();		
	}
}
