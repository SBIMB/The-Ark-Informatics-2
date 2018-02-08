package za.ac.theark.core.api;

import java.util.List;

import au.org.theark.core.model.lims.entity.Biospecimen;
import au.org.theark.core.model.study.entity.LinkSubjectStudy;

public interface IArkCommonAPI {
	void importParticipant(LinkSubjectStudy lss);
	void importParticipantList(List<LinkSubjectStudy> LSSList);
	void importBiospecimen(Biospecimen b);
	void importBiospecimenList(List<Biospecimen> bioList);
}
