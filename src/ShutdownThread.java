/**
 * Created by maxis on 06.06.2017.
 */

public class ShutdownThread extends Thread {

    private Process ktaneProcess;

    public ShutdownThread(Process ktaneProcess) {
        this.ktaneProcess = ktaneProcess;
    }
    public void run() {
        ktaneProcess.destroy();
        System.out.println("Programm wird geschlossen.");

    }
}
