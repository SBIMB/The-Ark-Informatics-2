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
package au.org.theark.core.model.study.entity;

import javax.persistence.*;

@Entity
@Table(name = "USER_ROLES", schema = "ETA")
public class UserRoles implements java.io.Serializable {

	// Fields

	private long		id;
	private EtaUser	etaUser;
	private Role		role;

	// Constructors

	/** default constructor */
	public UserRoles() {
	}

	/** minimal constructor */
	public UserRoles(long id) {
		this.id = id;
	}

	/** full constructor */
	public UserRoles(long id, EtaUser etaUser, Role role) {
		this.id = id;
		this.etaUser = etaUser;
		this.role = role;
	}

	// Property accessors
	@Id
	@Column(name = "ID", unique = true, nullable = false, precision = 22, scale = 0)
	public long getId() {
		return this.id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "USER_ID")
	public EtaUser getEtaUser() {
		return this.etaUser;
	}

	public void setEtaUser(EtaUser etaUser) {
		this.etaUser = etaUser;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ROLE_ID")
	public Role getRole() {
		return this.role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

}