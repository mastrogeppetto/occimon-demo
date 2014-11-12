package systemSpecification;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;

public class SystemSpecification {

	public final static String SENSOR="http://schemas.ogf.org/occi/monitoring#sensor";
	public final static String COLLECTOR="http://schemas.ogf.org/occi/monitoring#collector";
	public final static String COMPUTE="http://schemas.ogf.org/occi/infrastructure#compute";
	
	
//	public HashMap<String, Object> OCCIResource = new HashMap<String,Object>();
//	public HashMap<String, Object> OCCILink = new HashMap<String,Object>();
	private String url = null;

	public static void main (String[] args) throws FileNotFoundException, IOException, ParseException {
		SystemSpecification x = new SystemSpecification("http://127.0.0.1:6789/");
//		System.out.println(x.OCCIResource);
//		List<String> r = x.getResourcesByKind(SENSOR);
//		System.out.println(r);
//		r = x.getLinksByKind(COLLECTOR);
//		System.out.println(r);
//		JSONObject l=x.getLinkById("urn:uuid:23456789-def0");
//		System.out.println(l);
//		l=x.getResourceById("urn:uuid:1111");
//		System.out.println(l);
		JSONObject at=x.getAttributesById("urn:uuid:1111");
		System.out.println(at);
		at=x.getAttributesById("urn:uuid:23456789-def0");
		System.out.println(at);
		at=x.getAttributesById("urn:uuid:2222");
		System.out.println(at);
		HashMap<String, JSONObject> m=x.getMixinsById("http://example.com/occi/monitoring","urn:uuid:23456789-def0");
		System.out.println(m);
		System.out.println(x.inputPipes("http://example.com/occi/monitoring","urn:uuid:1111"));
	}

	public SystemSpecification(String url) throws IOException, ParseException,
	FileNotFoundException {
		this.url=url;
	}

	public List<String> getLinksByTarget(String id) throws IOException, ParseException {
		String descr = Jsoup.connect(url+id).ignoreContentType(true).get().text();
		JSONObject entity = (JSONObject) (new JSONParser()).parse(descr);
		List<String> result = new LinkedList<String>();
		for ( Object link: (JSONArray) entity.get("links") ) {
			result.add((String) link);
		}
		return result;
	}

	
	public String getKindById(String id) throws ParseException, IOException {
		JSONObject entity = getById(id);
		String kind = ((String) entity.get("kind")).split("#")[1];
		return kind;
	}
	
	public JSONObject getById(String id) throws ParseException, IOException {
		String descr = Jsoup.connect(url+id).ignoreContentType(true).get().text();
		JSONObject obj = (JSONObject) (new JSONParser()).parse(descr);
		return (JSONObject) obj;
	}

//	public JSONObject getLinkById(String id) {
//		return (JSONObject) OCCILink.get(id);
//	}
	
	public JSONObject getAttributesById(String id) throws IOException, ParseException {
		String descr = Jsoup.connect(url+id).ignoreContentType(true).get().text();
		JSONObject entity = (JSONObject) (new JSONParser()).parse(descr);
		if ( entity == null ) return null;
		String kind = ((String) entity.get("kind")).split("#")[1];
		JSONObject attr = (JSONObject) entity.get("attributes");
		List<String> path = Arrays.asList("occi",kind);
		for ( String s: path ) {
			attr=(JSONObject) attr.get(s);
			if ( attr == null) return null;
		}
		return attr;
	}

	public HashMap<String,JSONObject> getMixinsById(String url, String id) throws ParseException, IOException {
		HashMap <String, JSONObject> result = new HashMap<String,JSONObject>();
		JSONObject entity=getById(id);
		if ( entity == null ) return null;
		// compute path list from url
		List<String> path = new LinkedList<String>();
		List<String> p = Arrays.asList(url.split("/"));
		int i=0;
		for ( String x: p ) {
			if ( i == 2) {
				List<String> d = Arrays.asList(x.split("\\."));
				Collections.reverse(d);
				for (String y: d) {
					path.add(y);
				}
			}
			if ( i>2 ) {
				path.add(x);
			}
			i++;
		}
		JSONObject attr = (JSONObject) entity.get("attributes");
		// Find a mixin element for provider
		for ( String s: path ) {
			attr=(JSONObject) attr.get(s);
			if ( attr == null) return null;
		}
		for ( Object m: attr.keySet() ) {
			result.put((String) m, (JSONObject) attr.get(m) );
		}
		return result;
	}
	
	public HashMap<String, PipedInputStream> inputPipes(String url, String scopeId) throws ParseException, IOException {
		HashMap<String, PipedInputStream> result = new HashMap<>();
		List<String> inScope=getLinksByTarget(scopeId);
		inScope.add(scopeId);
		for ( String id: inScope ) {
			HashMap<String, JSONObject> m = getMixinsById(url, id);
			for ( String mixinName: m.keySet() ) {
//				System.out.println(mixinName);
				JSONObject attrs = (JSONObject) m.get(mixinName);
				for ( Object name: attrs.keySet() ) {
					if ( ((String) name).matches("in.*") ) {
//						System.out.println(id+" "+mixinName+" "+name+" "+attrs.get(name));
						result.put((String) attrs.get(name), new PipedInputStream());
					}
				}
			}
		}
		return result;
	}
	
	public HashMap<String, PrintWriter> outputPipes(String url, String scopeId, HashMap<String, PipedInputStream> pipeInMap) throws ParseException, IOException {
		HashMap<String, PrintWriter> result = new HashMap<>();
		List<String> outScope=getLinksByTarget(scopeId);
		outScope.add(scopeId);
		for ( String id: outScope ) {
			HashMap<String, JSONObject> m = getMixinsById(url, id);
			for ( String mixinName: m.keySet() ) {
				JSONObject attrs = (JSONObject) m.get(mixinName);
				for ( Object name: attrs.keySet() ) {
					if ( ((String) name).matches("out.*") ) {
						try {
							PrintWriter pw = 
									new PrintWriter(
											new BufferedWriter(
													new OutputStreamWriter(
															new PipedOutputStream(
																	pipeInMap.get(attrs.get(name))))),true);
							result.put((String) attrs.get(name), pw);
						} catch (NullPointerException | IOException e) {
							System.err.println("Error connecting pipe '"+name+"' in mixin '"+mixinName+"'");
							e.printStackTrace();
							System.exit(0);
						}
					}
				}
			}
		}
		return result;
	}
}
