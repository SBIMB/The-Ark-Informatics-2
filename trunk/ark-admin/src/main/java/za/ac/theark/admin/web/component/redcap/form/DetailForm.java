/*******************************************************************************
 * Copyright (c) 2011  University of Witwatersrand. All rights reserved.
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
package za.ac.theark.admin.web.component.redcap.form;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.rmi.CORBA.Stub;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.markup.html.form.DateTextField;
import org.apache.wicket.extensions.yui.calendar.DatePicker;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.theark.admin.model.vo.AdminVO;
import za.ac.theark.core.model.study.entity.ArkRedcap;
import za.ac.theark.core.model.study.entity.RedcapContentType;
import za.ac.theark.core.model.study.entity.RedcapContentFormat;
import za.ac.theark.core.model.study.entity.RedcapContent;
import za.ac.theark.core.model.study.entity.RedcapSyncRecurrence;
import za.ac.theark.core.service.IArkRedcapService;
import au.org.theark.admin.service.IAdminService;
import au.org.theark.admin.web.component.ContainerForm;
import au.org.theark.core.Constants;
import au.org.theark.core.model.study.entity.Study;
import au.org.theark.core.model.study.entity.YesNo;
import au.org.theark.core.security.ArkPermissionHelper;
import au.org.theark.core.service.IArkCommonService;
import au.org.theark.core.vo.ArkCrudContainerVO;
import au.org.theark.core.web.form.AbstractDetailForm;

public class DetailForm extends AbstractDetailForm<AdminVO> {


	private static final long	serialVersionUID	= -4117355038874906668L;

	protected transient Logger		log					= LoggerFactory.getLogger(DetailForm.class);

	@SpringBean(name = za.ac.theark.core.service.Constants.ARK_ADMIN_SERVICE)
	private IArkRedcapService<Void>	iArkAdminService;
	
	@SpringBean(name = au.org.theark.admin.service.Constants.ARK_ADMIN_SERVICE)
	private IAdminService<Void>	iAdminService;
	
	@SpringBean(name = au.org.theark.core.Constants.ARK_COMMON_SERVICE)
	private IArkCommonService<Void> iArkCommonService;

	private CheckBox									enabled;
	private TextField<String>							idTxtFld; 
	private TextField<String>							nameTxtFld; 
	private TextArea<String>							descriptionTxtAreaFld; 
	private TextField<String> 							redcapUrlTxtFld; 
	private TextField<String> 							tokenTxtFld; 
	private DropDownChoice<RedcapContentType> 			typeDropDown; 
	private DropDownChoice<RedcapContent> 				contentDropDown; 
	private DropDownChoice<RedcapContentFormat > 		formatDropDown; 
	private DropDownChoice<Study> 						studyDropDown; 
	private DropDownChoice<Study> 						subStudyDropDown;
	private DateTimeField								dateTimeField;
	private DatePicker									startDateDP;
	private DropDownChoice<RedcapSyncRecurrence>		syncRecurrenceDropDown;
	protected AjaxButton								testButton;
	
	/**
	 * Constructor
	 * @param id
	 * @param feedbackPanel
	 * @param containerForm
	 * @param arkCrudContainerVo
	 */
	public DetailForm(String id, FeedbackPanel feedbackPanel, ContainerForm containerForm, ArkCrudContainerVO arkCrudContainerVo) {
		super(id, feedbackPanel, containerForm, arkCrudContainerVo);
		this.containerForm = containerForm;
		arkCrudContainerVO = arkCrudContainerVo;
		setMultiPart(true);
	}
	
	@Override
	public void onBeforeRender() {
		super.onBeforeRender();
		// Do not allow deletes
		deleteButton.setEnabled(false);
	}

	public void initialiseDetailForm() {
		
		idTxtFld = new TextField<String>("arkRedcap.id");
		idTxtFld.setEnabled(false);
		
		nameTxtFld = new TextField<String>("arkRedcap.name") {

			private static final long	serialVersionUID	= 1L;

			@Override
			protected void onBeforeRender() {
				super.onBeforeRender();
				setEnabled(isNew());
			}
		};
		
		descriptionTxtAreaFld = new TextArea<String>("arkRedcap.description");
		
		redcapUrlTxtFld = new TextField<String>("arkRedcap.redcapUrl");
		
		tokenTxtFld = new TextField<String>("arkRedcap.token");
		
		initTypeDropDown();
		
		initContentDropDown();
		
		initFormatDropDown();
				
		initArkParentStudyDropDown();
		
		initArkSubStudyDropDown();
		
		initRedcapSyncOcurrence();
		
		initRedcapSyncStartDateTime();
		
		enabled = new CheckBox("arkRedcap.enabled");
		
		testButton = new AjaxButton(Constants.TEST, new StringResourceModel("page.test", this, null)) {

		/***
		 * 
		 **/
		private static final long serialVersionUID = -8144643942788032868L;

		@Override
		public boolean isVisible() {
			// calling super.isVisible() will allow an external setVisible() to override visibility
			return super.isVisible() && ArkPermissionHelper.isActionPermitted(Constants.TEST);
		}

		public void onSubmit(AjaxRequestTarget target, Form<?> form) {
			onTest(containerForm, target);
			target.add(arkCrudContainerVO.getDetailPanelContainer());
		}

		@Override
		protected void onError(AjaxRequestTarget target, Form<?> arg1) {
			testOnErrorProcess(target);	
		}	
	};
	
		attachValidators();
		
		addDetailFormComponents();
	}
	
	private void initTypeDropDown() {
		List<RedcapContentType> redcapContentTypeList = iArkAdminService.getRedcapContentTypeList();
		ChoiceRenderer<RedcapContentType> defaultChoiceRenderer = new ChoiceRenderer<RedcapContentType>("name");
		typeDropDown = new DropDownChoice<RedcapContentType>("arkRedcap.type", redcapContentTypeList, defaultChoiceRenderer);
	}
	
	private void initContentDropDown() {
		List<RedcapContent> redcapContentList = iArkAdminService.getRedcapContentList();
		ChoiceRenderer<RedcapContent> defaultChoiceRenderer = new ChoiceRenderer<RedcapContent>("name");
		contentDropDown = new DropDownChoice<RedcapContent>("arkRedcap.content",redcapContentList, defaultChoiceRenderer);
	}
	
	private void initFormatDropDown() {
		List<RedcapContentFormat> redcapFormatList = iArkAdminService.getRedcapContentFormatList();
		ChoiceRenderer<RedcapContentFormat> defaultChoiceRenderer = new ChoiceRenderer<RedcapContentFormat>("name");
		formatDropDown = new DropDownChoice<RedcapContentFormat>("arkRedcap.format", redcapFormatList, defaultChoiceRenderer);
	}
	
	private void initArkParentStudyDropDown() {
		List<Study> arkStudyList = iArkCommonService.getAllParentStudies();
		ChoiceRenderer<Study> defaultChoiceRenderer = new ChoiceRenderer<Study>("name");
		studyDropDown = new DropDownChoice<Study>("arkRedcap.study", arkStudyList, defaultChoiceRenderer){
			
			
			
			@Override
			protected boolean wantOnSelectionChangedNotifications() {
				// TODO Auto-generated method stub
				return true;
			}

			@Override
			protected void onSelectionChanged(Study study) {
				// TODO Auto-generated method stub
				super.onSelectionChanged(study);
				List<Study> arkSubStudyList = iArkCommonService.getAllSubStudiesList(study);
				if(!arkSubStudyList.isEmpty()){
					subStudyDropDown.setEnabled(true);
				}else{
					subStudyDropDown.setEnabled(false);
				}
				//ChoiceRenderer<Study> defaultChoiceRenderer = new ChoiceRenderer<Study>("name");
				//subStudyDropDown = new DropDownChoice<Study>("arkRedcap.study", arkSubStudyList, defaultChoiceRenderer);
				subStudyDropDown.setChoices(arkSubStudyList);
			}
		};
	}
	
	private void initArkSubStudyDropDown() {
		List<Study> arkSubStudyList = iArkCommonService.getAllStudies();
		ChoiceRenderer<Study> defaultChoiceRenderer = new ChoiceRenderer<Study>("name");
		subStudyDropDown = new DropDownChoice<Study>("arkRedcap.subStudy", arkSubStudyList, defaultChoiceRenderer);
		subStudyDropDown.setEnabled(false);
	}
	
	private void initRedcapSyncOcurrence(){
		List<RedcapSyncRecurrence> syncRedcapRecurrenceList = iArkAdminService.getRedcapSyncRecurrenceList();
		ChoiceRenderer<RedcapSyncRecurrence> defaultChoiceRenderer = new ChoiceRenderer<RedcapSyncRecurrence>("name");
		syncRecurrenceDropDown = new DropDownChoice<RedcapSyncRecurrence>("arkRedcap.redcapSyncRecurrence", syncRedcapRecurrenceList, defaultChoiceRenderer);
	}
	
	private void initRedcapSyncStartDateTime(){
		dateTimeField = new DateTimeField("arkRedcap.startDateTime"){
			
			 @Override 
	            protected DatePicker newDatePicker() { 
	                DatePicker datePicker = new DatePicker(){ 

	                   /* @Override 
	                    protected String getDatePattern() { 
	                        return "dd/MM/yyyy HH:mm:ss"; 
	                    } */

	                }; 

	                return datePicker; 
	            } 

			@Override
			protected boolean use12HourFormat()  {
				return false;
			}			
		};
	}
	
	/**
	 * Method to handle Test action, and handle for any errors, placing forcus on any component in error
	 * 
	 * @param target
	 */
	@SuppressWarnings("unchecked")
	protected void testOnErrorProcess(AjaxRequestTarget target) {
		boolean setFocusError = false;
		WebMarkupContainer wmc = arkCrudContainerVO.getDetailPanelContainer();
		for (Iterator<?> iterator = wmc.iterator(); iterator.hasNext();) {
			Component component = (Component) iterator.next();
			if (component instanceof FormComponent) {
				FormComponent<?> formComponent = (FormComponent<?>) component;

				if (!formComponent.isValid()) {
					if (!setFocusError) {
						// Place focus on field in error (for the first field in error)
						target.focusComponent(formComponent);
						setFocusError = true;
					}
				}
			}
		}

		processErrors(target);
	}

	@Override
	protected void attachValidators() {
		// Set required field here
		nameTxtFld.setRequired(true);
		redcapUrlTxtFld.setRequired(true);
		tokenTxtFld.setRequired(true);
		studyDropDown.setRequired(true);
	}
	
	/* (non-Javadoc)
	 * @see au.org.theark.core.web.form.AbstractDetailForm#addDetailFormComponents()
	 */
	@Override
	protected void addDetailFormComponents() {
		arkCrudContainerVO.getDetailPanelFormContainer().add(idTxtFld);
		arkCrudContainerVO.getDetailPanelFormContainer().add(nameTxtFld);
		arkCrudContainerVO.getDetailPanelFormContainer().add(descriptionTxtAreaFld);
		arkCrudContainerVO.getDetailPanelFormContainer().add(redcapUrlTxtFld);
		arkCrudContainerVO.getDetailPanelFormContainer().add(tokenTxtFld);
		arkCrudContainerVO.getDetailPanelFormContainer().add(typeDropDown);
		arkCrudContainerVO.getDetailPanelFormContainer().add(contentDropDown);
		arkCrudContainerVO.getDetailPanelFormContainer().add(formatDropDown);
		arkCrudContainerVO.getDetailPanelFormContainer().add(studyDropDown);
		arkCrudContainerVO.getDetailPanelFormContainer().add(subStudyDropDown);
		arkCrudContainerVO.getDetailPanelFormContainer().add(syncRecurrenceDropDown);
		arkCrudContainerVO.getDetailPanelFormContainer().add(dateTimeField);
		arkCrudContainerVO.getDetailPanelFormContainer().add(enabled);
				
		add(arkCrudContainerVO.getDetailPanelFormContainer());
		
		arkCrudContainerVO.getEditButtonContainer().add(testButton.setDefaultFormProcessing(false));
		
		add(arkCrudContainerVO.getEditButtonContainer());
	}

	protected void onTest(Form<AdminVO> containerForm, AjaxRequestTarget target) {
		//Test the RedCap connection
		AdminVO adminVO = containerForm.getModelObject();
		ArkRedcap arkRedcapTemp =  adminVO.getArkRedcap();
		
		log.info("ArkRedcap is null : "+adminVO.getArkRedcap().getId());
		
		log.info(adminVO.getArkRedcap().getId() + " "+
				arkRedcapTemp.getName()+" "+
				arkRedcapTemp.getDescription()//+" "+
				//arkRedcapTemp.getContent().getName()+" "+
				//arkRedcapTemp.getFormat().getName()
				);
		
		//Testing connection
		this.info(adminVO.getArkRedcap().testConnection());
		target.add(feedBackPanel);
	}
	
	protected void onSave(Form<AdminVO> containerForm, AjaxRequestTarget target) {
		// Save or update
		AdminVO adminVO = containerForm.getModelObject();
		log.info(adminVO.getArkRedcap().getEnabled() +" "+ adminVO.getArkRedcap().getRedcapSyncRecurrence().getName()+" "+ adminVO.getArkRedcap().getStartDateTime());
		
		adminVO.getArkRedcap().setStudy(iArkCommonService.getStudy((adminVO.getArkRedcap().getStudy().getId())));
		iAdminService.createOrUpdateArkRedcap(adminVO);
		//InitJobsScheduler initJobsScheduler
		this.info("Ark Redcap Connection: " + adminVO.getArkRedcap().getName() + " was created/updated successfully.");
		target.add(feedBackPanel);
		onSavePostProcess(target);
	}

	protected void onCancel(AjaxRequestTarget target) {
		containerForm.setModelObject(new AdminVO());
	}

	protected void onDeleteConfirmed(AjaxRequestTarget target, String selectionO) {
		// Delete
		iAdminService.deleteArkRedcap(containerForm.getModelObject());

		this.info("Ark Redcap connection: " + containerForm.getModelObject().getArkRedcap().getName() + " was deleted successfully.");
		editCancelProcess(target);
	}

	protected void processErrors(AjaxRequestTarget target) {
		target.add(feedBackPanel);
	}

	protected boolean isNew() {
		if (containerForm.getModelObject().getArkRedcap().getId() == null) {
			return true;
		}
		else {
			return false;
		}
	}
}
