import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Port;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by maxis on 17.05.2017.
 */
public class KtaneHandler {

    private String analyticsFile = "C:/Users/maxis/AppData/LocalLow/Steel Crate Games/Keep Talking and Nobody Explodes/analytics/ktane.csv";
    private String logFile = "logs/ktane.log";
    private String logDir = "C:/Program Files (x86)/Steam/steamapps/common/Keep Talking and Nobody Explodes/logs";
    private String ktaneExeLocation = "C:/Program Files (x86)/Steam/steamapps/common/Keep Talking and Nobody Explodes/ktane.exe";

    private Process runningGame;
    private boolean run;
    private int numberOfRounds = 0;
    private int bufferOffset = 0;
    private Timestamp latestTs = new Timestamp(new Date().getTime());

    public KtaneHandler() {
    }

    public void go() {
        initialize();
        System.out.println("Aktuellster TS: " + latestTs);

        runningGame = startGame(ktaneExeLocation);
        run = true;
        numberOfRounds++;
        System.out.println("Start game Nr. " + numberOfRounds);
        schlafen(10000);

        while (run) {
            System.out.println("Check for game end!");
            if (processFile()) {
            // if(false) {
                System.out.println("Spiel zu ende");
                run = false;
                runningGame.destroy();
            }
            schlafen(1000);
        }
    }

    public boolean schlafen(long mil) {
        try {
            Thread.sleep(mil);
            return true;
        } catch (InterruptedException ie) {
            System.out.println("Sleep Fehler");
            return false;
        }
    }

    public void initialize() {
        System.out.println("Reading log file: " + logFile);
    }


    public Process startGame(String ktaneLocation) {
        try {
            Process ktaneProcess = new ProcessBuilder(ktaneLocation).start();

            return ktaneProcess;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns true, if
     *
     * @return
     */
    public boolean processFile() {

        try {

            /*File source = new File(logFile);
            File dest = new File(logFile+"tmp");
            try {
                Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }*/

            List<String> tmp = new ArrayList<>();
            FileReader fr = new FileReader(logFile);
            BufferedReader br = new BufferedReader(fr);
            String ch;

            while ((ch = br.readLine()) != null) {
                // Throw out non-info or non-debug (such as tables, html code, etc)
                if (ch.startsWith("DEBUG") || ch.startsWith("INFO")) {
                    ch = ch.replace("DEBUG ", "");
                    ch = ch.replace(" INFO ", "");
                    ch = ch.replaceFirst(",", ".");
                    tmp.add(ch);
                }
            }
            br.close();
            System.out.println("Checking " + tmp.size() + " Log entries");
            for (int i = tmp.size() - 1; i >= 0; i--) {
                // Prüfung durchführen, solange die Daten aktueller als die letze Prüfung sind
                // System.out.println("Gelesener TS: " + tmp.get(i).substring(0, 23));
                long duration = (3 * 60 * 60 * 1000);
                Timestamp ts = Timestamp.valueOf(tmp.get(i).substring(0, 23));
                ts.setTime(ts.getTime() + duration);
                // System.out.println("Vergleichender TS: " + ts);
                if (ts.before(latestTs)) {
                    System.out.println("Hier waren wir schon!");
                    latestTs = Timestamp.valueOf(tmp.get(tmp.size() - 1).substring(0, 23));
                    System.out.println("Neuer latest TS: " + latestTs);
                    tmp.clear();
                    break;
                } else {
                    // Wenn OnRoundEnd() in der Log-Datei steht wird true zurück gegeben, da Spiel vorbei
                    if (tmp.get(i).contains("OnRoundEnd()")) {
                        System.out.println("Runde zu Ende!");
                        tmp.clear();
                        return true;
                    }
                }
            }
        } catch (FileNotFoundException fnfe) {
            System.out.println(logFile);
            System.out.println("Dateipfad falsch!");
            return false;
        } catch (IOException ioe) {
            System.out.println("IO-Fehler!");
            return false;
        }
        return false;
    }

}
