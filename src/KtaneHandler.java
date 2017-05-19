import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Port;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
    private String logFile = "C:/Program Files (x86)/Steam/steamapps/common/Keep Talking and Nobody Explodes/logs/ktane.log";
    private String logDir = "C:/Program Files (x86)/Steam/steamapps/common/Keep Talking and Nobody Explodes/logs";
    private String ktaneExeLocation = "C:/Program Files (x86)/Steam/steamapps/common/Keep Talking and Nobody Explodes/ktane.exe";

    private Process runningGame;
    private boolean run;
    private int numberOfRounds = 0;
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
            if (processFile(logFile)) {
                System.out.println("Spiel zu ende");
                run = false;
                runningGame.destroy();
            }
            schlafen(200);
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
     * @param fileLoc
     * @return
     */
    public boolean processFile(String fileLoc) {

        // Watch-Service

        System.out.println("Bis hier 1");
        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();
            Path watchingPath = Paths.get(logDir);
            watchingPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);

            WatchKey key = null;
            try {
                System.out.println("Bis hier 2");
                key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    System.out.println("Bis hier 3");
                    if (event.context().toString().equals(logDir)) {
                        System.out.println("Bis hier 4");
                        //Your file has changed, do something interesting with it.

                        try {
                            System.out.println("Bis hier 5");
                            List<String> tmp = new ArrayList<>();
                            FileReader fr = new FileReader(fileLoc);
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
                            System.out.println("Checking " + tmp.size() + "Log entries");
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
                            System.out.println("Dateipfad falsch!");
                            return false;
                        } catch (IOException ioe) {
                            System.out.println("IO-Fehler!");
                            return false;
                        }
                        return false;
                    }
                }
            } catch (InterruptedException e) {
                //Failed to watch for result file cahnges
                //you might want to log this.
                run = false;
            } catch (ClosedWatchServiceException e1) {
                run = false;
            }

            boolean reset = key.reset();
            if (!reset) {
                run = false;
            }
        } catch (Exception e) {

        }
        return false;
    }

}
