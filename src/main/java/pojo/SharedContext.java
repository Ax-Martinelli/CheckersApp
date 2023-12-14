package pojo;

import java.util.HashMap;
import java.util.Map;

public class SharedContext {
	private static Map<String, Boolean> authorizedUsers = new HashMap<String, Boolean>();
	private static Map<String, String> ipContext = new HashMap<String, String>();
	
	private SharedContext() {

	}
	
	public static void deauthorize() {
		for(String user : authorizedUsers.keySet()) {
			boolean heartbeat = authorizedUsers.get(user).booleanValue();
			String lastIp = ipContext.get(user);
			authorizedUsers.remove(user);
			ipContext.remove(user);
			if(heartbeat) {
				authorizedUsers.put(user, false);
				ipContext.put(user, lastIp);
			}
		}
	}
	
	public static String whois(String ip) {
		String response = "error";
		for(String key : ipContext.keySet()) {
			if(ipContext.get(key).equals(ip)) {
				response = key;
			}
		}
		return response;
	}
	
	public static void heartbeat(String username, String ip) {
		if(isAuthorized(username, ip)) {
			authorizedUsers.remove(username);
			authorizedUsers.put(username, true);
		}
	}
	
	public static void authorize(String username, String ip) {
		authorizedUsers.put(username, false);
		ipContext.put(username, ip);
	}
	
	public static boolean isAuthorized(String username, String ip) {
		if(authorizedUsers.containsKey(username)) {
			return ipContext.get(username).equals(ip);
		}
		return false;
	}
	
	public static boolean isLoggedIn(String username) {
		return authorizedUsers.containsKey(username);
	}
	
	public static String toStr() {
		return "auths: " + authorizedUsers.toString() + "\nips: " + ipContext.toString();
	}
}
