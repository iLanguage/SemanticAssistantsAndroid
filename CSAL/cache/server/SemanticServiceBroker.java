
package info.semanticsoftware.semassist.server;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;
import net.java.dev.jaxb.array.StringArray;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.1.7-b01-
 * Generated source version: 2.1
 * 
 */
@WebService(name = "SemanticServiceBroker", targetNamespace = "http://server.semassist.semanticsoftware.info/")
@SOAPBinding(style = SOAPBinding.Style.RPC)
@XmlSeeAlso({
    net.java.dev.jaxb.array.ObjectFactory.class,
    info.semanticsoftware.semassist.server.ObjectFactory.class
})
public interface SemanticServiceBroker {


    /**
     * 
     * @return
     *     returns info.semanticsoftware.semassist.server.ServiceInfoForClientArray
     */
    @WebMethod
    @WebResult(partName = "return")
    public ServiceInfoForClientArray getAvailableServices();

    /**
     * 
     * @return
     *     returns java.lang.String
     */
    @WebMethod
    @WebResult(partName = "return")
    public String getRunningServices();

    /**
     * 
     * @return
     *     returns java.lang.String
     */
    @WebMethod
    @WebResult(partName = "return")
    public String getThreadPoolQueueStatus();

    /**
     * 
     * @param ctx
     * @return
     *     returns info.semanticsoftware.semassist.server.ServiceInfoForClientArray
     */
    @WebMethod
    @WebResult(partName = "return")
    public ServiceInfoForClientArray recommendServices(
        @WebParam(name = "ctx", partName = "ctx")
        UserContext ctx);

    /**
     * 
     * @param connID
     * @param literalDocs
     * @param documents
     * @param gateParams
     * @param userCtx
     * @param serviceName
     * @return
     *     returns java.lang.String
     */
    @WebMethod
    @WebResult(partName = "return")
    public String invokeService(
        @WebParam(name = "serviceName", partName = "serviceName")
        String serviceName,
        @WebParam(name = "documents", partName = "documents")
        UriList documents,
        @WebParam(name = "literalDocs", partName = "literalDocs")
        StringArray literalDocs,
        @WebParam(name = "connID", partName = "connID")
        long connID,
        @WebParam(name = "gateParams", partName = "gateParams")
        GateRuntimeParameterArray gateParams,
        @WebParam(name = "userCtx", partName = "userCtx")
        UserContext userCtx);

    /**
     * 
     * @param resultFileUrl
     * @return
     *     returns java.lang.String
     */
    @WebMethod
    @WebResult(partName = "return")
    public String getResultFile(
        @WebParam(name = "resultFileUrl", partName = "resultFileUrl")
        String resultFileUrl);

    /**
     * 
     * @param arg0
     * @return
     *     returns java.lang.Object
     */
    @WebMethod
    @WebResult(partName = "return")
    public Object loadGateApp(
        @WebParam(name = "arg0", partName = "arg0")
        String arg0);

}
