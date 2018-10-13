import java.util.HashSet;
import java.util.Properties;
import java.util.ArrayList;
import java.util.Set;
import java.util.Iterator;
import java.util.Enumeration;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.FileInputStream;
import java.sql.*;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/** 
 * Основной класс приложения со свойствами <b>logger</b> и <b>connection</b>.
 * @autor Анатолий Берелехис
*/
public class Main {
	
	public static final Logger logger = Logger.getLogger(Main.class);
	private static Connection connection = null;
	
	/** 
     * Основная функция приложения
     * @param args - фргументы командной строки (1й - тип операции, 2й - имя текстового файла)
     */
	public static void main(String[] args) throws UnsupportedEncodingException,
			FileNotFoundException, IOException, SQLException{	
			
		setLog4jConfig();
		logger.debug("Program begin");
		
		ArrayList<String> connectionParameters = getConnectionParameters();
		try {
			connection = DriverManager.getConnection(
				connectionParameters.get(0), 
				connectionParameters.get(1), 
				connectionParameters.get(2)
			);
		}
		catch (SQLException e) {
			logger.fatal("Invalid connection parameters, transaction has aborted!");
			connection.rollback();
			System.exit(1);
		}
		Jobs jobsFromDb = new Jobs(connection);
		
		try {
			if (args[0].equals("unload"))
				jobsFromDb.unload("data/" + args[1] + ".xml");
			
			else if (args[0].equals("sync")) {
				Jobs jobsFromFile = new Jobs("data/" + args[1] + ".xml");	
				Jobs toRemove = jobsFromDb.xor(jobsFromFile);
				Jobs toAdd = jobsFromFile.xor(jobsFromDb);	
				Jobs toUpdate = jobsFromFile.intersection(jobsFromDb);
				
				connection.setAutoCommit(false);
				removeFromDb(connection, toRemove);
				addToDb(connection, toAdd);
				updateDb(connection, toUpdate);
				connection.commit();
			}
				
			else  {
				logger.fatal("Invalid command!");
				System.exit(1);
			}
		}
		catch (IndexOutOfBoundsException e) {
			logger.fatal("Too few arguments!");
			System.exit(1);
		}
		
		connection.close();
	}
	
	/** 
     * Настраивает конфигурацию для логгера
     */
	private static void setLog4jConfig() {
		
		String log4jPropertyFile = "data/config.properties";
		Properties properties = new Properties();
		
		try {
			properties.load(new FileInputStream(log4jPropertyFile));
			PropertyConfigurator.configure(properties);
		} 
		catch (IOException e) {
			logger.error("Сonfiguration failed!");
		}
	}
	
	/** 
     * Считывает из конфига свойств параметры для соединения с базой данных
     * @return возвращает массив параметров соединения с базой данных
     */
	private static ArrayList<String> getConnectionParameters() {
		
		Properties properties = new Properties();
		String settingsFileName = "data/config.properties";
		
		logger.debug("Try connect to config");
		
        try {
            FileReader fileReader = new FileReader(settingsFileName);
            properties.load(fileReader);
        }
        catch(FileNotFoundException e) {
            logger.fatal("File " + settingsFileName + " not found!");
			System.exit(1);
        }
        catch(IOException e) {
            logger.fatal("File " + settingsFileName + " not found!");
			System.exit(1);
        }
		
		ArrayList<String> connectionParameters = new ArrayList<String>();
		connectionParameters.add(properties.getProperty("jdbc.url"));
		connectionParameters.add(properties.getProperty("jdbc.username"));
		connectionParameters.add(properties.getProperty("jdbc.password"));
		
		logger.debug("Successfull connect to config");
		
		return connectionParameters;
	}
	
	/** 
     * Удаляет данные из базы
     * @param connection - соединение с базой данных
     * @param jobs - множство должностей, которое нужно удалить из базы данных
     */
	private static void removeFromDb(Connection connection, Jobs jobs) {
		
		logger.debug("Removing from data base begin");
		
		if (jobs.isEmpty()) {
			logger.warn("The set to remove is empty");
			return;
		}
		
        try {
			PreparedStatement statement = null;
			Iterator<Job> it = jobs.iterator();
			while (it.hasNext()) {
				Job job = it.next();
				String sql = "DELETE FROM JOB WHERE DEPCODE = ? AND DEPJOB = ?";
				statement = connection.prepareStatement(sql);
				statement.setString(1, job.getDepCode()); 
				statement.setString(2, job.getDepJob()); 
				statement.executeUpdate();
			}
            statement.close();
			logger.info("Successfull removing from data base");
        }
        catch(SQLException e) {
			logger.fatal("Removing from data base failed, transaction has aborted!");
			try {
				connection.rollback();
			}
			catch(SQLException ex) {
			}
			System.exit(1);
        }
	}
	
	/** 
     * Обновляет данные в базе
     * @param connection - соединение с базой данных
     * @param jobs - множство должностей, которое нужно обновить в базе данных
     */
	private static void updateDb(Connection connection, Jobs jobs) {
		
		logger.debug("Updating data base begin");
		
		if (jobs.isEmpty()) {
			logger.warn("The set to update is empty");
			return;
		}
        try {
			PreparedStatement statement = null;
			Iterator<Job> it = jobs.iterator();
			while (it.hasNext()) {
				Job job = it.next();
				String sql = "UPDATE JOB SET DESCRIPTION = ? WHERE DEPCODE = ? AND DEPJOB = ?";
				statement = connection.prepareStatement(sql);
				statement.setString(1, job.getDescription()); 
				statement.setString(2, job.getDepCode()); 
				statement.setString(3, job.getDepJob()); 
				statement.executeUpdate();
			}
            statement.close();
			logger.info("Successfull updating from data base");
        }
        catch(SQLException e) {
			logger.fatal("Updating data base failed, transaction has aborted!");
			try {
				connection.rollback();
			}
			catch(SQLException ex) {
			}
			System.exit(1);
        }
	}
	
	/** 
     * Добавляет данные в базу
     * @param connection - соединение с базой данных
     * @param jobs - множство должностей, которое нужно добавить в базу данных
     */
	private static void addToDb(Connection connection, Jobs jobs) {
		
		logger.debug("Adding to data base begin");
		
		if (jobs.isEmpty()) {
			logger.warn("The set to add is empty");
			return;
		}
        try {
			PreparedStatement statement = null;
			Iterator<Job> it = jobs.iterator();
			while (it.hasNext()) {
				Job job = it.next();
				String sql = "INSERT INTO JOB " +
					"(DEPCODE, DEPJOB, DESCRIPTION) VALUES (?, ?, ?)";
				statement = connection.prepareStatement(sql);
				statement.setString(1, job.getDepCode()); 
				statement.setString(2, job.getDepJob()); 
				statement.setString(3, job.getDescription()); 
				statement.executeUpdate();
			}
            statement.close();
			logger.info("Successfull adding to data base");
        }
        catch(SQLException e)
        {
			logger.fatal("Adding to data base failed, transaction has aborted!");
			try {
				connection.rollback();
			}
			catch(SQLException ex) {
			}
			System.exit(1);
        }
	}
}
