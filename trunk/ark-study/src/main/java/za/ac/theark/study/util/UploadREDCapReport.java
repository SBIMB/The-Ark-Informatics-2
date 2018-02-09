/**
 * 
 */
package za.ac.theark.study.util;

import org.apache.shiro.SecurityUtils;

import au.org.theark.core.model.study.entity.Upload;
import au.org.theark.core.util.UploadReport;

/**
 * @author Freedom
 *
 */
public class UploadREDCapReport extends UploadReport{

	/**
	 * 
	 */
	public UploadREDCapReport() {
		super();
	}
	
	@Override
	public void appendDetails(Upload upload) {
		append("Study: ");
		appendAndNewLine(upload.getStudy().getName());
		append("UserID: ");
		appendAndNewLine(SecurityUtils.getSubject().getPrincipal().toString());
		append("Filename: ");
		appendAndNewLine(upload.getFilename());
		append("File Format: ");
		appendAndNewLine(upload.getFileFormat().getName());
		append("Delimiter Type: ");
		appendAndNewLine(upload.getDelimiterType().getName());
	}
	

}
