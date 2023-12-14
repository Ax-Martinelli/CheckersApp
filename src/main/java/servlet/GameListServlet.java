package servlet;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import pojo.SharedContext;
import util.DBConnection;

/**
 * Servlet implementation class GameListServlet
 */
public class GameListServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Pattern alpha = Pattern.compile("^[a-zA-Z]+$");
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GameListServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("application/json");
		response.addHeader("Access-Control-Allow-Origin", "*");
		Object otype = request.getParameter("service");
		if(otype == null) {
			response.getWriter().print(getError("No request type was provided!"));
			response.getWriter().flush();
			return;
		}
		String type = (String) otype;
		if(!((type.equals("list") || type.equals("addpub") || type.equals("whoami") || type.equals("accept")))) {
			response.getWriter().print(getError("Invalid type provided!"));
			response.getWriter().flush();
			return;
		}
		Object ogameId = request.getParameter("gameId");
		int gameId = -1;
		if(type.equals("accept")) {
			if(ogameId == null) {
				response.getWriter().print(getError("No gameid was provided!"));
				response.getWriter().flush();
				return;
			}
			gameId = Integer.parseInt((String) ogameId);
		}
		Object ouname = request.getParameter("name");
		if(ouname == null) {
			response.getWriter().print(getError("No username was provided!"));
			response.getWriter().flush();
			return;
		}
		String uname = (String) ouname;
		String valMsg = validateUsername(uname);
		
		if(!valMsg.equals("")) {
			response.getWriter().print(valMsg);
			response.getWriter().flush();
			return;
		}
		
		switch(type) {
			case "list" : {
				response.getWriter().print(getList(uname, request.getRemoteAddr()));
			} break;
			case "addpub" : {
				response.getWriter().print(addGame(uname, request.getRemoteAddr()));
			} break;
			case "whoami" : {
				response.getWriter().print("{ \"response\": \"" + SharedContext.whois(request.getRemoteAddr()) + "\" }");
			} break;
			case "accept" : {
				response.getWriter().print(acceptGame(gameId, request.getRemoteAddr()));
			} break;
		}
		
		response.getWriter().flush();
	}
	
	private String getError(String message) {
		return "{ \"response\": \"error\", \"message\": \"" + message + "\" }";
	}

	private String validateUsername(String uname) {
		if(alpha.matcher(uname).matches()) {
			if(uname.length() > 3) {
				if(uname.length() < 21) {
					return "";
				} else {
					return getError("Usernames must be at most 20 characters long!");
				}
			} else {
				return getError("Usernames must be at least 4 characters long!");
			}
		} else {
			return getError("Usernames must contain alphabetic characters only!");
		}
	}
	
	private String getList(String uname, String ip) {
		List<String> packedResults = new ArrayList<String>();
		List<String> activeGames = new ArrayList<String>();
		if(SharedContext.isAuthorized(uname, ip)) {
			Connection connection = null;
			PreparedStatement freeStmt = null;
			PreparedStatement activeStmt = null;
			ResultSet freeRs = null;
			ResultSet activeRs = null;
			try {
				DBConnection.getDBConnection();
				connection = DBConnection.connection;
				freeStmt = connection.prepareStatement("SELECT * FROM matches WHERE STATUS = 0");
				freeStmt.executeQuery();
				freeRs = freeStmt.getResultSet();
				System.out.println("Attempting thing...");
				while(freeRs.next()) {
					String reqUser = freeRs.getString("REQUSER");
					String reqDate = freeRs.getTimestamp("REQDATE").toString();
					int id = freeRs.getInt("id");
					String packed = String.format("{ \"id\": \"%d\", \"requser\": \"%s\", \"reqdate\": \"%s\" }", id, reqUser, reqDate);
					System.out.println(packed);
					packedResults.add(packed);
				}
				freeRs.close();
				freeStmt.close();
				
				activeStmt = connection.prepareStatement("SELECT * FROM matches WHERE STATUS = 1 AND (REQUSER = ? OR ACCUSER = ?)");
				activeStmt.setString(1, uname);
				activeStmt.setString(2, uname);
				activeStmt.executeQuery();
				activeRs = activeStmt.getResultSet();
				while(activeRs.next()) {
					String reqUser = activeRs.getString("REQUSER");
					String accUser = activeRs.getString("ACCUSER");
					String diffUser = reqUser.equals(uname) ? accUser : reqUser;
					String accDate = activeRs.getTimestamp("ACCDATE").toString();
					int id = activeRs.getInt("id");
					String packed = String.format("{ \"id\": \"%d\", \"opponent\": \"%s\", \"accdate\": \"%s\" }", id, diffUser, accDate);
					System.out.println(packed);
					activeGames.add(packed);
				}
				activeRs.close();
				activeStmt.close();
				connection.close();
			} catch (SQLException se) {
				se.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					if (freeStmt != null)
						freeStmt.close();
					if (activeStmt != null)
						activeStmt.close();
				} catch (SQLException se2) {
				}
				try {
					if (connection != null)
						connection.close();
				} catch (SQLException se) {
					se.printStackTrace();
				}
			}
			StringBuilder sb = new StringBuilder("{ \"response\": { \"public\": [");
			for(int i = 0; i < packedResults.size() - 1; i++) {
				sb.append(packedResults.get(i)).append(", ");
			}
			if(packedResults.size() > 0) sb.append(packedResults.get(packedResults.size() - 1));
			
			sb.append("], \"private\": [");
			for(int i = 0; i < activeGames.size() - 1; i++) {
				sb.append(activeGames.get(i)).append(", ");
			}
			if(activeGames.size() > 0) sb.append(activeGames.get(activeGames.size() - 1));
			sb.append("] } }");
			return sb.toString(); 
		} else {
			return getError("You are not authorized!");
		}
	}
	

	private String addGame(String uname, String ip) {
		if(SharedContext.isAuthorized(uname, ip)) {
			Connection connection = null;
			PreparedStatement preparedStatement = null;
			boolean success = false;
			try {
				DBConnection.getDBConnection();
				connection = DBConnection.connection;
				String selectSQL = "INSERT INTO matches (STATUS, REQUSER, REQDATE) VALUES (0, ?, CURRENT_TIMESTAMP())";
				preparedStatement = connection.prepareStatement(selectSQL);
				preparedStatement.setString(1, uname);
				
				success = preparedStatement.execute();

				preparedStatement.close();
				connection.close();
			} catch (SQLException se) {
				se.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					if (preparedStatement != null)
						preparedStatement.close();
				} catch (SQLException se2) {
				}
				try {
					if (connection != null)
						connection.close();
				} catch (SQLException se) {
					se.printStackTrace();
				}
			}
			StringBuilder sb = new StringBuilder("{ \"response\": { \"success\": ");

			sb.append("\"").append(success).append("\" } }");
			return sb.toString(); 
		} else {
			return getError("You are not authorized!");
		}
	}
	
	private String acceptGame(int gameId, String ip) {
		if(SharedContext.isAuthorized(SharedContext.whois(ip), ip)) {
			Connection connection = null;
			PreparedStatement preparedStatement = null;
			boolean success = false;
			try {
				DBConnection.getDBConnection();
				connection = DBConnection.connection;
				String selectSQL = "UPDATE matches SET ACCUSER = ?, ACCDATE = CURRENT_TIMESTAMP(), STATUS = 1 WHERE id = ?";
				preparedStatement = connection.prepareStatement(selectSQL);
				preparedStatement.setString(1, SharedContext.whois(ip));
				preparedStatement.setInt(2, gameId);
				
				success = preparedStatement.execute();

				preparedStatement.close();
				connection.close();
			} catch (SQLException se) {
				se.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					if (preparedStatement != null)
						preparedStatement.close();
				} catch (SQLException se2) {
				}
				try {
					if (connection != null)
						connection.close();
				} catch (SQLException se) {
					se.printStackTrace();
				}
			}
			StringBuilder sb = new StringBuilder("{ \"response\": { \"success\": ");

			sb.append("\"").append(success).append("\" } }");
			return sb.toString(); 
		} else {
			return getError("You are not authorized!");
		}
	}
	
}
