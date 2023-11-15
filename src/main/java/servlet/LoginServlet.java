package servlet;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import pojo.SharedContext;
import util.DBConnection;

/**
 * Servlet implementation class LoginServlet
 */
@WebServlet("/LoginServlet")
public class LoginServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Pattern alpha = Pattern.compile("^[a-zA-Z]+$");
	private static final Pattern password = Pattern.compile("^[a-zA-Z0-9]+$"); 
    /**
     * Default constructor. 
     */
    public LoginServlet() {
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
		Object otype = request.getParameter("service");
		if(otype == null) {
			response.getWriter().print(getError("No request type was provided!"));
			response.getWriter().flush();
			return;
		}
		String type = (String) otype;
		if(!(type.equals("login") || type.equals("query") || type.equals("register") || type.equals("heartbeat"))) {
			response.getWriter().print(getError("Invalid type provided!"));
			response.getWriter().flush();
			return;
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
		String pwd = "";
		if(type.equals("login") || type.equals("register")) {
			Object opwd = request.getParameter("password");
			if(opwd == null) {
				response.getWriter().print(getError("No password was provided!"));
				response.getWriter().flush();
				return;
			}
			pwd = (String) opwd;
			String pvalMsg = validatePassword(pwd);
			if(!pvalMsg.equals("")) {
				response.getWriter().print(pvalMsg);
				response.getWriter().flush();
				return;
			}
		}
		
		switch(type) {
			case "query" : {
				response.getWriter().print(doQuery(uname));
			} break;
			case "login" : {
				response.getWriter().print(doLogin(uname, pwd, request.getRemoteAddr()));
			} break;
			case "register" : {
				response.getWriter().print(doRegister(uname, pwd, request.getRemoteAddr()));
			} break;
			case "heartbeat" : {
				response.getWriter().print("boop");
			} break;
		}
		
		response.getWriter().flush();
	}
	
	private String getError(String message) {
		return "{ \"response\": \"error\", \"message\": \"" + message + "\" }";
	}
	
	private String doQuery(String uname) {
		boolean isValid = false;
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		try {
			DBConnection.getDBConnection();
			connection = DBConnection.connection;
			String selectSQL = "SELECT * FROM users WHERE UNAME = ?";
			preparedStatement = connection.prepareStatement(selectSQL);
			preparedStatement.setString(1, uname);

			ResultSet rs = preparedStatement.executeQuery();
			if(rs.next() != false) {
				isValid = true;
			}
			rs.close();
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
		return "{ \"response\": \"query\", \"valid\": \"" + isValid + "\" }"; 
	}
	private String doLogin(String uname, String pwd, String ip) {
		System.out.println(SharedContext.toStr());
		if(SharedContext.isLoggedIn(uname))
			if(!SharedContext.isAuthorized(uname, ip))
				return getLoginJson(false);
		
		boolean isValid = true;
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		try {
			DBConnection.getDBConnection();
			connection = DBConnection.connection;
			String selectSQL = "SELECT * FROM users WHERE UNAME = ? AND PWORD = ?";
			preparedStatement = connection.prepareStatement(selectSQL);
			preparedStatement.setString(1, uname);
			preparedStatement.setString(2, pwd);
			ResultSet rs = preparedStatement.executeQuery();
			if(rs.next() == false) {
				isValid = false;
			}
			rs.close();
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
		if(isValid) {
			SharedContext.authorize(uname, ip);
			SharedContext.heartbeat(uname, ip);
		}
		return getLoginJson(isValid);
	}
	private String getLoginJson(boolean success) {
		return "{ \"response\": \"login\", \"success\": \"" + success + "\" }";
	}
	private String doRegister(String uname, String pwd, String ip) {		
		boolean isValid = false;
		Connection connection = null;
		PreparedStatement selectStmt = null;
		PreparedStatement insertStmt = null;
		try {
			DBConnection.getDBConnection();
			connection = DBConnection.connection;
			String selectSQL = "SELECT * FROM users WHERE UNAME = ?";
			String insertSQL = "INSERT INTO users (UNAME, PWORD) VALUES (?, ?)";
			selectStmt = connection.prepareStatement(selectSQL);
			selectStmt.setString(1, uname);
			ResultSet rs1 = selectStmt.executeQuery();
			if(!rs1.next()) {
				isValid = true;
			}
			rs1.close();
			selectStmt.close();
			

			if(isValid) {
				insertStmt = connection.prepareStatement(insertSQL);
				insertStmt.setString(1, uname);
				insertStmt.setString(2,  pwd);
				boolean result = insertStmt.execute();
				insertStmt.close();
			}
			connection.close();
		} catch (SQLException se) {
			se.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (selectStmt != null)
					selectStmt.close();
				if (insertStmt != null)
					insertStmt.close();
			} catch (SQLException se2) {
			}
			try {
				if (connection != null)
					connection.close();
			} catch (SQLException se) {
				se.printStackTrace();
			}
		}
		return "{ \"response\": \"register\", \"success\": \"" + isValid + "\" }";
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
	
	private String validatePassword(String pwd) {
		if(password.matcher(pwd).matches()) {
			if(pwd.length() > 7) {
				if(pwd.length() < 21) {
					return "";
				} else {
					return getError("Passwords must be at most 20 characters long!");
				}
			} else {
				return getError("Passwords must be at least 8 characters long!");
			}
		} else {
			return getError("Passwords must contain alphanumeric characters only!");
		}
	}

}
