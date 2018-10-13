/** 
 * Класс должности со свойствами <b>depCode</b>, <b>depJob</b> и <b>description</b>.
 * @autor Анатолий Берелехис
*/
public class Job {

    private String depCode;
    private String depJob;
    private String description;
	
	public String getDepCode() {return depCode;}
	public String getDepJob() {return depJob;}
	public String getDescription() {return description;}

	/** 
     * Конструктор, в котором параметрами задаются все поля класса
     * @param connection - код департамента
	 * @param connection - название должности
	 * @param connection - описание должности
     */
    public Job(String depCode, String depJob, String description) {
        this.depCode = depCode;
        this.depJob = depJob;
        this.description = description;
    }

	@Override
    public String toString() {
        return "depcode=" + this.depCode + "\n"
			+ "depjob=" + this.depJob + "\n"
			+ "description=" + this.description + "\n";
    }
	
	@Override
    public boolean equals(Object object) {
        if (object == this)
            return true;
        if (object == null)
            return false;
        if (!(getClass() == object.getClass()))
            return false;
        else {
            Job job = (Job)object;
            if ((job.depCode.equals(this.depCode)) && (job.depJob.equals(this.depJob)))
                return true;
            else
                return false;
        }
    }

	/** 
     * Геттер для хеш-кода
     * @return возвращает хеш-код
     */
    public int getHashCode() {
        return this.depCode.hashCode()+this.depJob.hashCode();
    }
}