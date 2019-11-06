package ppex.utils.tpool;

public class DisruptorHandler {
    private ITask task;

    public void setTask(ITask task) {
        this.task = task;
    }
    public void execute(){
        try {
            this.task.execute();;
            this.task = null;
        }catch (Throwable t){
            t.printStackTrace();
        }
    }
}
