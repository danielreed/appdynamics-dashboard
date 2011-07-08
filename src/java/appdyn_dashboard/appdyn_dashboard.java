package appdyn_dashboard;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.NodeList;

import org.xml.sax.InputSource;

@ManagedBean
@SessionScoped
public class appdyn_dashboard {

    HashMap nodes = new HashMap();
    HashMap values = new HashMap();
    HashMap problems = new HashMap();

    public appdyn_dashboard() {
    }

    public HashMap getNodes() {
        return nodes;
    }

    public void initNodes() throws Exception {

        String getrestxml = restGet("/controller/rest/applications/137/nodes", "");

        if (getrestxml != null) {

            for (int i = 0; i < parseXML(getrestxml, "/nodes/node/type").size(); i++) {
               
                nodes.put(parseXML(getrestxml, "/nodes/node/id").get(i),
                        parseXML(getrestxml, "/nodes/node/type").get(i) + "," +
                        parseXML(getrestxml, "/nodes/node/name").get(i) + "," + 
                        parseXML(getrestxml, "/nodes/node/machineName").get(i) + "," +
                        parseXML(getrestxml, "/nodes/node/machineAgentVersion").get(i) + "," +
                        parseXML(getrestxml, "/nodes/node/appAgentVersion").get(i));
                
            }

        }

    }

    public HashMap getValues() {
        return values;
    }

    public void initValues() throws Exception {

        String getrestxml = restGet("/controller/rest/applications/137/metric-data", "metric-path=Business Transaction Performance|Business Transactions|kfs|*|Individual Nodes|kfsServer1|Average Response Time (ms)&time-range-type=BEFORE_NOW&duration-in-mins=15");

        if (getrestxml != null) {
            
            for (int i = 0; i < parseXML(getrestxml, "/metric-datas/metric-data/metricPath").size(); i++) {
                values.put(parseXML(getrestxml, "/metric-datas/metric-data/metricPath").get(i), parseXML(getrestxml, "/metric-datas/metric-data/frequency").get(i));
            }

        }

    }

    public HashMap getProblems() {
        return problems;
    }

    public void initProblems() throws Exception {

        String getrestxml = restGet("/controller/rest/applications/137/problems/policy-violations", "time-range-type=BEFORE_NOW&duration-in-mins=10080");       
        
        if (getrestxml != null) {
            
            for (int i = 0; i < parseXML(getrestxml, "/policy-violations/policy-violation/id").size(); i++) {
                           
                //find the start time in ms of the event, convert to a string and then a long
                Object start = parseXML(getrestxml, "/policy-violations/policy-violation/startTimeInMillis").get(i).toString();
                String sstart = start.toString();
                long lstart = Long.parseLong(sstart.trim());
                
                //find the end time in ms of the event, convert to a string and then a long
                Object end = parseXML(getrestxml, "/policy-violations/policy-violation/endTimeInMillis").get(i).toString();
                String send = end.toString();
                long lend = Long.parseLong(send.trim());
                
                //subtract the end from the start, divide by 1000 ms, and again by 60 secs
                //to find the duration of the event in mins
                long math = (((lend - lstart)/1000)/60);              
                                
                //split the long name "CPU % Busy- warning rule" and take the first part
                String name = parseXML(getrestxml, "/policy-violations/policy-violation/name").get(i).toString();
                String names[] = name.split("-");
                
                problems.put(parseXML(getrestxml, "/policy-violations/policy-violation/id").get(i),
                        names[0] + "," +
                        parseXML(getrestxml, "/policy-violations/policy-violation/incidentStatus").get(i) + "," + 
                        parseXML(getrestxml, "/policy-violations/policy-violation/severity").get(i) + "," +
                        math);
            }

        }

    }

    public String restGet (String path1, String path2) throws URISyntaxException, ClientProtocolException, IOException {

        String restget = "";
        DefaultHttpClient httpclient = new DefaultHttpClient(); //default implementation of the HttpClient interface

        try {

            //maintain a set of user credentials used for auth scope (host, port, user, pass)                       
            httpclient.getCredentialsProvider().setCredentials(
                new AuthScope(" .saas.appdynamics.com", AuthScope.ANY_PORT),
                new UsernamePasswordCredentials(" @ ", " "));

            //assemble a new URI
            URI uri = new URI("http", " .saas.appdynamics.com", path1, path2, null);

            //execute a new GET request with the URI
            HttpGet httpget = new HttpGet(uri);
            HttpResponse response = httpclient.execute(httpget);

            //consume the HTTP entity content
            HttpEntity entity = response.getEntity();  //InputStream            

            //non-null entity should be assigned to restget
            if (entity != null) {
                restget = EntityUtils.toString(entity);
            }

            EntityUtils.consume(entity); //release the connection
            
        } finally {
            httpclient.getConnectionManager().shutdown();  //shutdown the connection manager
        }

        return restget;
    }

    public List parseXML (String rawxml, String evalexp) throws XPathExpressionException, IOException {

        List parsedlist = new ArrayList();

        if (rawxml != null) {
            InputStream is = new ByteArrayInputStream(rawxml.getBytes());
            InputSource inputSource = new InputSource();
            inputSource.setByteStream(is);
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            
            NodeList nl = (NodeList) xpath.evaluate(evalexp, inputSource, XPathConstants.NODESET);
            for (int i = 0; i < nl.getLength(); i++) {
                parsedlist.add(nl.item(i).getTextContent());
            }

            is.close(); //close the inputstream
        }

        return parsedlist;  //return the list
    }
}
