import java.util.HashSet;
import java.util.Iterator;
import java.io.UnsupportedEncodingException;
import java.io.OutputStreamWriter;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.sql.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** 
 * Класс множества должностей.
 * @autor Анатолий Берелехис
*/
public class Jobs extends HashSet {
		
	/** 
     * Конструктор по умолчанию
     */
	public Jobs() {
		
	}
	
	/** 
     * Конструктор - загрузка множества должностей их базы данных
     * @param connection - соединение с базой данных
     */
	public Jobs(Connection connection) {
		
		Main.logger.debug("Creating jobs set from data base begin");
		
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM JOB");
            while(resultSet.next())
            {
                Job job = new Job(
					resultSet.getString(2),
					resultSet.getString(3),
					resultSet.getString(4)
				);
                this.add(job);
            }
            statement.close();
			Main.logger.info("Successfull creating set of jobs from data base");
        }
        catch(SQLException e)
        {
            Main.logger.fatal("Creating jobs set from data base failed, transaction has aborted!");
			try {
				connection.rollback();
			}
			catch(SQLException ex) {
			}
			System.exit(1);
        }
	}
	
	/** 
     * Конструктор - загрузка множества должностей их файла
     * @param fileName - имя файла
     */
	public Jobs(String fileName) {
		
		Main.logger.debug("Creating jobs set from file begin");
        try{
            DocumentBuilder xml = 
				DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = xml.parse(new File(fileName));
            Element rootel = doc.getDocumentElement();
            Node node1 = rootel.getFirstChild();
            do {
                if (!"root".equals(node1.getNodeName())) {
                    Node node2 = node1.getFirstChild();
                    String depcode = "", depjob = "", desc = "";
                    int id = 0;
                    while (node2 != null) {
                        if (
							"#text".equals(node2.getNodeName()) || 
							"id".equals(node2.getNodeName())
						) {
                            node2 = node2.getNextSibling();
                            continue;
						}
                        if (
							"depcode".equals(node2.getNodeName()) && 
							!"".equals(node2.getTextContent())
						) { 
							depcode = node2.getTextContent();
							node2 = node2.getNextSibling();
                            continue;
						}
                        if (
							"depjob".equals(node2.getNodeName()) && 
							node2.getTextContent() != ""
						) {
							depjob = node2.getTextContent();
							node2 = node2.getNextSibling();
                            continue;
						}
                        if (
							"description".equals(node2.getNodeName()) && 
							!"".equals(node2.getTextContent())
						) 
							desc = node2.getTextContent();
							
                        Job job = new Job(depcode,depjob,desc);
                        if (!(this.contains(job))) 
							this.add(job);
                        else {
                            this.clear();
                            return;
                        }
                        node2 = node2.getNextSibling();
					}
				
                    node1 = node1.getNextSibling();
                }
            }
			while(node1 != null);
			
			if (this.hasDuplicate()) {
				Main.logger.fatal("There are many duplicates in the file!");
				System.exit(1);
			}
			Main.logger.info("Successfull creating set of jobs from file");
        }
		
		catch(Exception e) {
            Main.logger.fatal("Creating jobs set from file failed!");
			System.exit(1);
        }
    }
	
	/** 
     * Функция, проверяющая множества должностей на записи с одинаковым "depCode" и "depJob"
     * @return возвращает true, если есть дубли, и false, если их нет
     */
	private boolean hasDuplicate() {
		Iterator<Job> iterator1 = this.iterator();
		while(iterator1.hasNext()) {
			Job job = iterator1.next();
			int duplicateCount = 0;
			Iterator<Job> iterator2 = this.iterator();
			while (iterator2.hasNext()) {
				if (job.equals(iterator2.next()))
					duplicateCount++;
			}
			if (duplicateCount > 1)
				return true;
		}
		return false;
	}
	
	/** 
     * Функция, выгружающая данные из базы в файл
     * @param fileName - имя файла
     */
	public void unload(String fileName) 
			throws UnsupportedEncodingException, FileNotFoundException, IOException {
        
		Main.logger.debug("Unloading from data base to file begin");
		try {
            OutputStreamWriter writer = new OutputStreamWriter(
				new BufferedOutputStream(
						new FileOutputStream(fileName), 1024*100
				), 
				"windows-1251"
			);

            writer.write("<?xml version=\"1.0\" encoding=\"windows-1251\"?>\n");
            Iterator<Job> it = this.iterator();
            writer.write("<root>\n");
            while(it.hasNext()) {
                Job job = it.next();
                writer.write("<worker>\n");
                writer.write("\t<depcode>" + job.getDepCode() + "</depcode>\n");
                writer.write("\t<depjob>" + job.getDepJob() + "</depjob>\n");
                writer.write(
					"\t<description>" + 
					job.getDescription() + 
					"</description>\n"
				);
                writer.write("</worker>\n");
            }
            writer.write("</root>\n");
            writer.flush();
            writer.close();
			
			Main.logger.info("Successfull unloading from data base to file");
		}
        catch(UnsupportedEncodingException e) {
            Main.logger.debug("Unloading from data base to file failed!");
			System.exit(1);
        }
        catch(FileNotFoundException e) {
            Main.logger.debug("Unloading from data base to file failed!");
			System.exit(1);
        }
        catch(IOException e) {
            Main.logger.debug("Unloading from data base to file failed!");
			System.exit(1);
        }
    }
	
	/** 
     * Аналог операции "xor" из теории множеств в математике
     * @param arg - множество-параметр для операции "xor"
	 * @return возвращает получившееся множество в результате операции
     */
	public Jobs xor(Jobs arg) {
		Jobs result = new Jobs();
		Iterator<Job> thisIterator = this.iterator();
		while (thisIterator.hasNext()) {
			Job thisJob = (Job)thisIterator.next();
			Iterator<Job> argIterator = arg.iterator();
			boolean isContains = false;
			while(argIterator.hasNext()) {
				Job argJob = argIterator.next();
				if (thisJob.equals(argJob))
					isContains = true;
			}
			if (!isContains) 
				result.add(thisJob);
		}
		return result;
	}
	
	/** 
     * Аналог операции "пересечение" из теории множеств в математике
     * @param arg - множество-параметр для операции "xor"
	 * @return возвращает получившееся множество в результате операции
     */
	public Jobs intersection(Jobs arg) {
		Jobs result = new Jobs();
		Iterator<Job> thisIterator = this.iterator();
		while (thisIterator.hasNext()) {
			Job thisJob = (Job)thisIterator.next();
			Iterator<Job> argIterator = arg.iterator();
			boolean isContains = false;
			while(argIterator.hasNext()) {
				Job argJob = argIterator.next();
				if (thisJob.equals(argJob))
					isContains = true;
			}
			if (isContains) 
				result.add(thisJob);
		}
		return result;
	}
}