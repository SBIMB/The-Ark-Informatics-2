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
package za.ac.theark.admin.web.component.redcap;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PageableListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.theark.admin.web.component.ContainerForm;
import za.ac.theark.core.model.study.entity.ArkRedcap;
import za.ac.theark.core.service.IArkRedcapService;
import au.org.theark.core.service.IArkCommonService;
import au.org.theark.core.vo.ArkCrudContainerVO;
import au.org.theark.core.web.component.ArkCRUDHelper;
import au.org.theark.core.web.component.ArkDataProvider;
import au.org.theark.core.web.component.link.ArkBusyAjaxLink;


public class SearchResultsPanel extends Panel {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3963978624936971387L;

	protected transient Logger		log					= LoggerFactory.getLogger(SearchResultsPanel.class);

	@SpringBean(name = za.ac.theark.core.service.Constants.ARK_ADMIN_SERVICE)
	private IArkRedcapService<Void>	iAdminService;
	private ContainerForm			containerForm;
	private ArkCrudContainerVO		arkCrudContainerVo;
	
	@SpringBean(name = au.org.theark.core.Constants.ARK_COMMON_SERVICE)
	private IArkCommonService		iArkCommonService;

	public SearchResultsPanel(String id, ContainerForm containerForm, ArkCrudContainerVO arkCrudContainerVo) {
		super(id);
		this.containerForm = containerForm;
		this.arkCrudContainerVo = arkCrudContainerVo;
	}

	@SuppressWarnings("unchecked")
	public DataView<ArkRedcap> buildDataView(ArkDataProvider<ArkRedcap, IArkRedcapService> dataProvider) {
		DataView<ArkRedcap> dataView = new DataView<ArkRedcap>("arkRedcapList", dataProvider) {

			private static final long	serialVersionUID	= 2981419595326128410L;

			@Override
			protected void populateItem(final Item<ArkRedcap> item) {
				ArkRedcap arkRedcap = item.getModelObject();
				
				item.add(new CheckBox("arkRedcap.enabled",new Model<Boolean>(arkRedcap.getEnabled())));

				item.add(new Label("arkRedcap.id", arkRedcap.getId().toString()));
				item.add(buildLink(arkRedcap));

				if (arkRedcap.getDescription() != null) {
					// the ID here must match the ones in mark-up
					item.add(new Label("arkRedcap.description", arkRedcap.getDescription()));
				}
				else {
					item.add(new Label("arkRedcap.description", ""));
				}
				
				if (arkRedcap.getRedcapUrl() != null) {
					// the ID here must match the ones in mark-up
					item.add(new Label("arkRedcap.redcapUrl", arkRedcap.getRedcapUrl()));
				}
				else {
					item.add(new Label("arkRedcap.redcapUrl", ""));
				}
				
				if (arkRedcap.getType() != null) {
					// the ID here must match the ones in mark-up
					item.add(new Label("arkRedcap.type", arkRedcap.getType().getName()));
				}
				else {
					item.add(new Label("arkRedcap.type", ""));
				}
				
				if (arkRedcap.getContent() != null) {
					// the ID here must match the ones in mark-up
					item.add(new Label("arkRedcap.content", arkRedcap.getContent().getName()));
				}
				else {
					item.add(new Label("arkRedcap.content", ""));
				}
				
				if (arkRedcap.getFormat() != null) {
					// the ID here must match the ones in mark-up
					item.add(new Label("arkRedcap.format", arkRedcap.getFormat().getName()));
				}
				else {
					item.add(new Label("arkRedcap.format", ""));
				}

				if (arkRedcap.getStudy().getName() != null) {
					// the ID here must match the ones in mark-up
					item.add(new Label("arkRedcap.study", arkRedcap.getStudy().getName()));
				}
				else {
					item.add(new Label("arkRedcap.study", ""));
				}
				
				item.add(new AttributeModifier("class", new AbstractReadOnlyModel<String>() {
					private static final long	serialVersionUID	= 5761909841047153853L;

					@Override
					public String getObject() {
						return (item.getIndex() % 2 == 1) ? "even" : "odd";
					}
				}));
			}
		};
		return dataView;
	}

	@SuppressWarnings("unchecked")
	public PageableListView<ArkRedcap> buildPageableListView(IModel iModel, final WebMarkupContainer searchResultsContainer) {
		PageableListView<ArkRedcap> pageableListView = new PageableListView<ArkRedcap>("arkRedcapList", iModel, iArkCommonService.getRowsPerPage()) {

			private static final long	serialVersionUID	= 3350183112731574263L;

			@Override
			protected void populateItem(final ListItem<ArkRedcap> item) {

				ArkRedcap arkRedcap = item.getModelObject();
				
				item.add(new Label("arkRedcap.id", arkRedcap.getId().toString()));
				
				/*item.add(buildLink(arkRedcap));*/

				/*if (arkRedcap.getName() != null) {
					// the ID here must match the ones in mark-up
					item.add(new Label("arkRedcap.name", arkRedcap.getName()));
				}
				else {
					item.add(new Label("arkRedcap.name", ""));
				}*/

				/*if (arkRedcap.getDescription() != null) {
					// the ID here must match the ones in mark-up
					item.add(new Label("arkRedcap.description", arkRedcap.getDescription()));
				}
				else {
					item.add(new Label("arkRedcap.description", ""));
				}*/

				if (arkRedcap.getContent() != null) {
					// the ID here must match the ones in mark-up
					item.add(new Label("arkRedcap.description", arkRedcap.getDescription()));
				}
				else {
					item.add(new Label("arkRedcap.description", ""));
				}
				
				if (arkRedcap.getRedcapUrl() != null) {
					// the ID here must match the ones in mark-up
					item.add(new Label("arkRedcap.redcapUrl", arkRedcap.getRedcapUrl()));
				}
				else {
					item.add(new Label("arkRedcap.redcapUrl", ""));
				}	
				
				if (arkRedcap.getType().getName() != null) {
					// the ID here must match the ones in mark-up
					item.add(new Label("arkRedcap.type", arkRedcap.getType().getName()));
				}
				else {
					item.add(new Label("arkRedcap.type", ""));
				}				
				
				if (arkRedcap.getContent() != null) {
					// the ID here must match the ones in mark-up
					item.add(new Label("arkRedcap.content", arkRedcap.getContent().getName()));
				}
				else {
					item.add(new Label("arkRedcap.content", ""));
				}
				
				if (arkRedcap.getFormat().getName() != null) {
					// the ID here must match the ones in mark-up
					item.add(new Label("arkRedcap.format", arkRedcap.getFormat().getName()));
				}
				else {
					item.add(new Label("arkRedcap.format", ""));
				}			
				
				if (arkRedcap.getStudy().getName() != null) {
					// the ID here must match the ones in mark-up
					item.add(new Label("arkRedcap.study", arkRedcap.getStudy().getName()));
				}
				else {
					item.add(new Label("arkRedcap.study", ""));
				}
				

				item.add(new AttributeModifier("class", new AbstractReadOnlyModel<String>() {
					private static final long	serialVersionUID	= 1938679383897533820L;

					@Override
					public String getObject() {
						return (item.getIndex() % 2 == 1) ? "even" : "odd";
					}
				}));

			}
		};
		return pageableListView;
	}

	private AjaxLink<ArkRedcap> buildLink(final ArkRedcap arkRedcap) {
		ArkBusyAjaxLink<ArkRedcap> link = new ArkBusyAjaxLink<ArkRedcap>("link") {

			private static final long	serialVersionUID	= 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				Long id = arkRedcap.getId();
				ArkRedcap ArkRedcap = iAdminService.getArkRedcap(id);
				containerForm.getModelObject().setArkRedcap(ArkRedcap);

				ArkCRUDHelper.preProcessDetailPanelOnSearchResults(target, arkCrudContainerVo);
				// Refresh base container form to remove any feedBack messages
				target.add(containerForm);
			}
		};

		// Add the label for the link
		Label linkLabel = new Label("arkRedcap.name", arkRedcap.getName());
		link.add(linkLabel);
		return link;
	}
}
