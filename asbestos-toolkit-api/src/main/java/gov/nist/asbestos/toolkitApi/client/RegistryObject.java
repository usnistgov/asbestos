package gov.nist.asbestos.toolkitApi.client;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;


public abstract class RegistryObject extends MetadataObject implements IsSerializable, Serializable {

	private static final long serialVersionUID = 1L;
	public String status;
	public String statusX;
	public String statusDoc;

	public String title;
	public String titleX;
	public String titleDoc = "title";

	public String comments;
	public String commentsX;
	public String commentsDoc = "comments";

	public String patientId;
	public String patientIdX;
	public String patientIdDoc = "patientId";

	public String uniqueId;
	public String uniqueIdX;
	public String uniqueIdDoc = "uniqueId";


}
