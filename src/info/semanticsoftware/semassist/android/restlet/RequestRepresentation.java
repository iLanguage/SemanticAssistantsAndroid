package info.semanticsoftware.semassist.android.restlet;

import info.semanticsoftware.semassist.android.prefs.PrefUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RequestRepresentation {

	private String serviceName;
	private String input;
	private Map<String, String> params = new HashMap<String, String>();

	public RequestRepresentation(String iServiceName, Map<String,String> iParams, String input){
		this.serviceName = iServiceName;
		this.params = iParams;
		this.input = input;
	}

	public String getXML(){
		PrefUtils prefUtil = PrefUtils.getInstance();

		StringBuffer buffer = new StringBuffer();
		buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		buffer.append("<invocation>");
		String username = prefUtil.getUsername();
		if(username != null){
			if(username.indexOf("@") > -1){
				username = username.substring(0, username.indexOf("@"));
			}
			buffer.append("<username>").append(username).append("</username>");
		}

		buffer.append("<serviceName>").append(serviceName).append("</serviceName>");
		if(params !=null){
			Set<String> paramNames = params.keySet();
			for(String name:paramNames){
				buffer.append("<param>");
					buffer.append("<name>").append(name).append("</name>");
					buffer.append("<value>");
					buffer.append(params.get(name));
					buffer.append("</value>");
				buffer.append("</param>");
			}
		}

		buffer.append("<input><![CDATA[");
		buffer.append(input);
		buffer.append("]]></input>");

		buffer.append("</invocation>");
		return buffer.toString();
	}
}
