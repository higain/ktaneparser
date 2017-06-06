import java.io.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by maxis on 17.05.2017.
 * TODO: Fenster auf eine Seite verschieben, damit Biofeedback angezeigt wird
 */
public class KtaneHandler {

    // brauch ich nicht private String analyticsFile = "C:/Users/maxis/AppData/LocalLow/Steel Crate Games/Keep Talking and Nobody Explodes/analytics/ktane.csv";
    private String logFile = "logs/ktane.log";
    // brauch ich nicht private String logDir = "C:/Program Files (x86)/Steam/steamapps/common/Keep Talking and Nobody Explodes/logs";
    private String ktaneStarter = "D:/workspace/IISM/ktane_start.vbs";
    private String ktaneExeLocation = "C:/Program Files (x86)/Steam/steamapps/common/Keep Talking and Nobody Explodes/ktane.exe";
    private String[] ktaneFromSteam = {"C:/Program Files (x86)/Steam/Steam.exe", "-applaunch", "341800"};
    private String[] missionIds = {"firsttime", "mission", "mission 1","mission 2"};

    private Process runningGame;
    private boolean run;
    public boolean experiment;
    private int numberOfRounds = 0;
    private Timestamp latestTs = new Timestamp(new Date().getTime());

    private String solvableModules, modules, solvedModules;
    private String timeLeft, bombState;
    private long strikes;

    private String[] bombList = {"abc", "def"};

    KtaneJsonHandler ktjshandler;

    public KtaneHandler() {
    }

    public void go(int numberOfGames) {
        initialize();
        System.out.println("Aktuellster TS: " + latestTs);

        while(numberOfGames>=numberOfRounds) {
            System.out.println("Starting Game from " + Arrays.toString(ktaneFromSteam));
            runningGame = startGame(ktaneFromSteam);
            ShutdownThread shutdownHandler = new ShutdownThread(runningGame);
            Runtime.getRuntime().addShutdownHook(shutdownHandler);
            schlafen(20000);
            startMission(missionIds[0]);
            schlafen(10000);
            run = true;
            numberOfRounds++;
            System.out.println("Start game Nr. " + numberOfRounds);
            latestTs = new Timestamp(new Date().getTime());

            while (run) {
                // System.out.println(ktjshandler.fetchBombInfos());
                parseJson(ktjshandler.fetchBombInfos());

                if (processFile()) {
                    System.out.println("Spiel zu ende");
                    schlafen(1000);
                    run = false;
                    runningGame.destroy();
                }
                schlafen(500);
            }
        }
    }

    public void parseJson(JSONObject jobj) {
        try {
            solvableModules = ((JSONArray) jobj.get("SolvableModules")).toString(); // .replace("\"", "").replace("[", "").replace("]", "").split(",");
            modules = ((JSONArray) jobj.get("Modules")).toString(); //.replace("\"", "").replace("[", "").replace("]", "").split(",");
            solvedModules = ((JSONArray) jobj.get("SolvedModules")).toString(); //.replace("\"", "").replace("[", "").replace("]", "").split(",");
            timeLeft = (String) jobj.get("Time");
            strikes = ((Long) jobj.get("Strikes"));
            bombState = (String) jobj.get("BombState");

            printBombStatus();
        }
        catch (NullPointerException npe) {
            System.out.println("Cannot parse Json Object, is the game running?");
            System.out.println(npe.getLocalizedMessage());
        }
    }

    public void printBombStatus() {
        System.out.println("Solvable Modules: "+solvableModules);
        System.out.println("Solved Modules: "+solvedModules);
        System.out.println("Modules to solve: " + modules);
        System.out.println("Time left: "+timeLeft);
        System.out.println("Strikes: "+strikes);
        System.out.println("State: "+bombState);
        System.out.println();

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
        ktjshandler = new KtaneJsonHandler(8085, "http://localhost:8085/");
    }


    public Process startGame(String[] ktaneLocation) {
        try {
            // Runtime.getRuntime().exec(ktaneLocation);
            Process ktaneProcess = new ProcessBuilder(ktaneLocation).start();

            return ktaneProcess;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void startMission(String missionId) {
        int randomNum = ThreadLocalRandom.current().nextInt(1234, 99998 + 1);
        boolean state = ktjshandler.startMission(missionId, ""+randomNum);
        System.out.println(state);
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
                if (ch.startsWith("DEBUG") || ch.startsWith(" INFO")) {
                    ch = ch.replace("DEBUG ", "");
                    ch = ch.replace(" INFO ", "");
                    ch = ch.replaceFirst(",", ".");
                    tmp.add(ch);
                    // System.out.println(ch);
                }
            }
            br.close();
            // System.out.println("Checking " + tmp.size() + " Log entries");
            int counter = 0;
            boolean abbruch = false;
            outerloop:
            for (int i = tmp.size() - 1; i >= 0; i--) {
                // Prüfung durchführen, solange die Daten aktueller als die letze Prüfung sind
                // System.out.println("Gelesener TS: " + tmp.get(i).substring(0, 23));
                long duration = (2 * 60 * 60 * 1000);
                Timestamp ts = Timestamp.valueOf(tmp.get(i).substring(0, 23));
                ts.setTime(ts.getTime() + duration);
                // System.out.println("Vergleichender TS: " + ts);
                if (ts.equals(latestTs) || ts.before(latestTs)) {
                    // System.out.println("Hier waren wir schon!");
                    Timestamp tsmid = Timestamp.valueOf(tmp.get(tmp.size() - 1).substring(0, 23));
                    tsmid.setTime(tsmid.getTime() + duration);
                    latestTs = tsmid;
                    // System.out.println("Neuer latest TS: " + latestTs);
                    abbruch = true;
                    break outerloop;
                } else {
                    counter++;
                    // Wenn OnRoundEnd() in der Log-Datei steht wird true zurück gegeben, da Spiel vorbei
                    if (tmp.get(i).contains("OnRoundEnd()")) {
                        System.out.println("Runde zu Ende!");
                        tmp.clear();
                        return true;
                    }
                }
            }
            if (!abbruch) {
                long duration = (2 * 60 * 60 * 1000);
                Timestamp tsend = Timestamp.valueOf(tmp.get(tmp.size() - 1).substring(0, 23));
                tsend.setTime(tsend.getTime() + duration);

                latestTs = tsend;
                System.out.println("Neuer latest TS: " + latestTs);
            }
            if (counter > 0) {
                System.out.println(counter + " Einträge neu gelesen");
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
