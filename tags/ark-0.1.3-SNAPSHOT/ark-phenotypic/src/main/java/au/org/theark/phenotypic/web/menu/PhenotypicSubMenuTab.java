package au.org.theark.phenotypic.web.menu;

import java.util.ArrayList;
import java.util.List;

import org.apache.shiro.subject.Subject;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.theark.core.model.study.entity.ArkFunction;
import au.org.theark.core.model.study.entity.ArkModule;
import au.org.theark.core.service.IArkCommonService;
import au.org.theark.core.web.component.ArkAjaxTabbedPanel;
import au.org.theark.core.web.component.menu.AbstractArkTabPanel;
import au.org.theark.phenotypic.web.Constants;
import au.org.theark.phenotypic.web.component.field.FieldContainerPanel;
import au.org.theark.phenotypic.web.component.fieldData.FieldDataContainerPanel;
import au.org.theark.phenotypic.web.component.fieldDataUpload.FieldDataUploadContainerPanel;
import au.org.theark.phenotypic.web.component.fieldUpload.FieldUploadContainerPanel;
import au.org.theark.phenotypic.web.component.phenoCollection.PhenoCollectionContainerPanel;
import au.org.theark.phenotypic.web.component.summaryModule.SummaryContainerPanel;

@SuppressWarnings( { "serial", "unused" })
public class PhenotypicSubMenuTab extends AbstractArkTabPanel
{
	@SpringBean(name = au.org.theark.core.Constants.ARK_COMMON_SERVICE)
	private IArkCommonService<Void>	iArkCommonService;

	private transient Logger			log					= LoggerFactory.getLogger(PhenotypicSubMenuTab.class);
	private transient Subject			currentUser;
	private transient Long				studyId;
	private WebMarkupContainer			arkContextMarkup;
	private List<ITab>					moduleSubTabsList	= new ArrayList<ITab>();

	public PhenotypicSubMenuTab(String id, WebMarkupContainer arkContextMarkup)
	{
		super(id);
		this.arkContextMarkup = arkContextMarkup;
		buildTabs(arkContextMarkup);
	}

	public void buildTabs(final WebMarkupContainer arkContextMarkup)
	{
		ArkModule arkModule = iArkCommonService.getArkModuleByName(au.org.theark.core.Constants.ARK_MODULE_PHENOTYPIC);
		List<ArkFunction> arkFunctionList = iArkCommonService.getModuleFunction(arkModule);
		for (final ArkFunction menuArkFunction : arkFunctionList)
		{
			AbstractTab tab = new AbstractTab(new StringResourceModel(menuArkFunction.getResourceKey(), this, null))
			{
				@Override
				public Panel getPanel(final String panelId)
				{
					return panelToReturn(menuArkFunction, panelId);
				}
			};
			moduleSubTabsList.add(tab);
		}

		ArkAjaxTabbedPanel moduleTabbedPanel = new ArkAjaxTabbedPanel(Constants.PHENOTYPIC_SUBMENU, moduleSubTabsList);
		add(moduleTabbedPanel);
	}

	protected Panel panelToReturn(final ArkFunction arkFunction, String panelId)
	{
		Panel panelToReturn = null;

		// Clear cache to determine permissions
		processAuthorizationCache(au.org.theark.core.Constants.ARK_MODULE_PHENOTYPIC, arkFunction);

		if (arkFunction.getName().equalsIgnoreCase(au.org.theark.core.Constants.FUNCTION_KEY_VALUE_PHENO_SUMMARY))
		{
			panelToReturn = new SummaryContainerPanel(panelId);
		}
		else if (arkFunction.getName().equalsIgnoreCase(au.org.theark.core.Constants.FUNCTION_KEY_VALUE_DATA_DICTIONARY))
		{
			panelToReturn = new FieldContainerPanel(panelId);
		}
		else if (arkFunction.getName().equalsIgnoreCase(au.org.theark.core.Constants.FUNCTION_KEY_VALUE_DATA_DICTIONARY_UPLOAD))
		{
			panelToReturn = new FieldUploadContainerPanel(panelId);
		}
		else if (arkFunction.getName().equalsIgnoreCase(au.org.theark.core.Constants.FUNCTION_KEY_VALUE_PHENO_COLLECTION))
		{
			panelToReturn = new PhenoCollectionContainerPanel(panelId, arkContextMarkup);
		}
		else if (arkFunction.getName().equalsIgnoreCase(au.org.theark.core.Constants.FUNCTION_KEY_VALUE_FIELD_DATA))
		{
			panelToReturn = new FieldDataContainerPanel(panelId);
		}
		else if (arkFunction.getName().equalsIgnoreCase(au.org.theark.core.Constants.FUNCTION_KEY_VALUE_FIELD_DATA_UPLOAD))
		{
			panelToReturn = new FieldDataUploadContainerPanel(panelId);
		}

		return panelToReturn;
	}
}