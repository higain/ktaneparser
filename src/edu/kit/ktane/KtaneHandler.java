package edu.kit.ktane;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by maxis on 17.05.2017.
 * TODO: Fenster auf eine Seite verschieben, damit Biofeedback angezeigt wird
 */
public class KtaneHandler {

//    /**
//     * Logger
//     */
//    final Logger logger = Logger.getLogger(getClass().getName());

    // brauch ich nicht private String analyticsFile = "C:/Users/maxis/AppData/LocalLow/Steel Crate Games/Keep Talking and Nobody Explodes/analytics/ktane.csv";
    private String logFile = "logs/ktane.log";
    // brauch ich nicht private String logDir = "C:/Program Files (x86)/Steam/steamapps/common/Keep Talking and Nobody Explodes/logs";
    private String ktaneStarter = "D:/workspace/IISM/ktane_start.vbs";
    private String ktaneExeLocation = "C:/Program Files (x86)/Steam/steamapps/common/Keep Talking and Nobody Explodes/ktane.exe";
    private String[] ktaneFromSteam = {"C:/Program Files (x86)/Steam/Steam.exe", "-applaunch", "341800"};
    private String[] missionIds1 = {"mod_iismkitpretest1_1bomb1", "mod_iismkitpretest1_1bomb2", "mod_iismkitpretest1_1bomb3",
            "mod_iismkitpretest1_1bomb4", "mod_iismkitpretest1_1bomb5", "mod_iismkitpretest1_1bomb6",
            "mod_iismkitpretest1_1bomb7", "mod_iismkitpretest1_1bomb8", "mod_iismkitpretest1_1bomb9",
            "mod_iismkitpretest1_1bomb10"};
    private String[] missionIds2 = {"mod_iismkitpretest2_2bomb1", "mod_iismkitpretest2_2bomb2", "mod_iismkitpretest2_2bomb3",
            "mod_iismkitpretest2_2bomb4", "mod_iismkitpretest2_2bomb5", "mod_iismkitpretest2_2bomb6",
            "mod_iismkitpretest2_2bomb7", "mod_iismkitpretest2_2bomb8", "mod_iismkitpretest2_2bomb9",
            "mod_iismkitpretest2_2bomb10"};
    private String[] seeds = {"11779", "13718", "49060", "59085", "27392", "1120", "59549", "44791", "643", "41167"};

    private String runningGame;
    private boolean run;
    public boolean experiment;
    private int numberOfRounds = 0;
    private Timestamp latestTs = new Timestamp(new Date().getTime());

    private String solvableModules, modules, solvedModules;
    private String timeLeft, bombState;
    private long strikes;

    private WindowHandler windowHandler;


    KtaneJsonHandler ktjshandler;

    public KtaneHandler() {
//        if (OS == null) {
//            OS = System.getProperty("os.name");
//        }
//
//        if (OS.startsWith("Windows")) {
//            logFile = "logs/ktane.log";                                             // New version with steam
//            ktaneFromSteam = new String[]{"C:/Program Files (x86)/Steam/Steam.exe", "-applaunch", "341800"};
////            ktaneExeLocation = "C:/Users/exp/Desktop/ktane/ktane.exe";
////                    System.getProperty("user.dir") +
////                            "/Exp_Implementation/src/edu/kit/exp/impl/groupmeter/server/res/game/ktaneWin/ktane.exe";
////            logFile = "C:/Users/exp/Desktop/ktane/logs/ktane.log";
//
//        } else if (OS.startsWith("Mac")) {
////            logFile = "/Users/michaelknierim/Desktop/game/logs/ktane.log";        // Old version without steam
//            logFile = "logs/ktane.log";                                             // New version with steam
//            ktaneFromSteam = new String[]{"/Applications/Steam.app/Contents/MacOS/steam_osx", "-applaunch", "341800"};
////            ktaneExeLocation =
////                    "/Users/michaelknierim/Desktop/game/ktaneMac.app/Contents/MacOS/Keep Talking and Nobody Explodes";
////                    System.getProperty("user.dir") +
////                            "/Exp_Implementation/src/edu/kit/exp/impl/groupmeter/server/res/game/ktaneMac.app/Contents/MacOS/Keep " +
////                            "Talking and Nobody Explodes";
//
//        } else {
//            logger.info("OS could not be determined. Therefore game exe was not set");
//        }
    }

    public void goStandalone(int numberOfGames) {
        while (numberOfGames > numberOfRounds) {
            System.out.println("Start game Nr. " + numberOfRounds + " of " + numberOfGames);
            go(numberOfRounds);
            numberOfRounds++;
        }

        System.out.println("Experiment zu ende!");
    }

    public void go(int gameId) {
        System.out.println("Aktuellster TS: " + latestTs);

        initialize();
        System.out.println("Starting Game from " + Arrays.toString(ktaneFromSteam));
        runningGame = startGame(ktaneFromSteam);
        schlafen(15000);
        while (!startMission(gameId)) {
            schlafen(200);
            System.out.println("Konnte mission nicht starten.");
        }
        // startMission(numberOfRounds);
        schlafen(5000);
        // used for log file parsing latestTs = new Timestamp(new Date().getTime());
        while (bombState == null) {
            parseJson(ktjshandler.fetchBombInfos());
            schlafen(200);
            System.out.println("Mission noch nicht fertig geladen.");
        }
        while (!(bombState.equals("Exploded"))) {
            parseJson(ktjshandler.fetchBombInfos());
            schlafen(500);
        }
        System.out.println("Spiel zu ende");
        bombState = null;
        System.out.println("Waiting for Process: " + runningGame);
        schlafen(3000);
        stopGame(runningGame);
        System.out.println("Shutting down game");
        schlafen(7000);
        strikes = 0;
        timeLeft = bombState = runningGame = solvableModules = modules = solvedModules = null;
        System.out.println("Experiment zu ende!");
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
        } catch (NullPointerException npe) {
            System.out.println("Cannot parse Json Object, is the game running?");
            System.out.println(npe.getLocalizedMessage());
        }
    }

    public void printBombStatus() {
        System.out.println("Solvable Modules: " + solvableModules + " " + "Solved Modules: " + solvedModules + " " +
                "Modules to solve: " + modules + " " + "Time left: " + timeLeft + " " + "Strikes: " + strikes +
                " " + "State: " + bombState);
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
        // System.out.println("Reading log file: " + logFile);
        ktjshandler = new KtaneJsonHandler(8085, "http://localhost:8085/");
    }

    public boolean stopGame(String pid) {
        String cmd = "taskkill /F /PID " + pid;
        try {
            Runtime.getRuntime().exec(cmd);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    public String startGame(String[] ktaneLocation) {
        try {
            // Runtime.getRuntime().exec(ktaneLocation);
            Process ktaneProcess = new ProcessBuilder(ktaneLocation).start();

            System.out.println("Process started");
            boolean started = false;
            String pid = "";
            while (!started) {
                try {
                    String line;
                    Process p = Runtime.getRuntime().exec
                            (System.getenv("windir") + "\\system32\\" + "tasklist.exe");
                    BufferedReader input =
                            new BufferedReader(new InputStreamReader(p.getInputStream()));
                    while ((line = input.readLine()) != null) {
                        if (line.contains("ktane.exe")) {
                            String[] tmparray = line.split(" ");
                            System.out.println(Arrays.toString(tmparray));
                            ArrayList<String> regline = new ArrayList<>();
                            for (String s : tmparray) {
                                if (s.length() > 1) {
                                    regline.add(s.replace(" ", ""));
                                }
                            }
                            System.out.println("Anzahl Elemente: " + regline.size());
                            System.out.println(Arrays.toString(regline.toArray()));
                            if (regline.size() > 2) {
                                System.out.println(regline.get(1));
                                pid = regline.get(1);
                            }
                            started = true;
                            windowHandler = new WindowHandler();
                            windowHandler.resizeWindow();
                            break;
                        }
                    }
                    input.close();
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }

            return pid;
        } catch (Exception e) {
            e.printStackTrace();
            schlafen(200);
            startGame(ktaneLocation);
            return null;
        }
    }

    public boolean startMission(int missionId) {
        boolean state = ktjshandler.startMission(missionIds1[missionId], "" + seeds[missionId]);
        return state;
    }


    /**
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

//    /**
//     * Writes the performance metrics of a current round to a csv corresponding to the experiment.
//     */
//    private void writePerformanceMetricsToCSV() {
//        DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy_HH-mm-ss-SSS");
//        final String fileSeparator = File.separator;
//        final String CSV_SEPARATOR = ",";
//        String dataLogFile;
//
//        try {
//            dataLogFile = "data/game" + fileSeparator + firstTs + "_gameData_log.csv";
////            dataLogFile = "data/metadata" + fileSeparator + LocalDateTime.now().format(dateTimeFormat) + "_expmetadata_log" +
////                    ".csv";
//
//            // Create subfolder if necessary
//            File targetFile = new File(dataLogFile);
//            File parent = targetFile.getParentFile();
//
//            // If path does not exist and can't be created, throw error.
//            if (!parent.exists() && !parent.mkdirs()) {
//                throw new IllegalStateException("Couldn't create dir: " + parent);
//            }
//
//            // If the file is created for the first time, write the header line too.
//            if (!targetFile.exists()) {
//                // Write to file
//                try (BufferedWriter bw = new BufferedWriter((new FileWriter(dataLogFile)))) {
//                    bw.write("lastLevelName" + CSV_SEPARATOR +
//                            "remainingTime" + CSV_SEPARATOR +
//                            "strikes");
//                }
//            }
//
//            // Write game data to file
//            try (BufferedWriter bw = new BufferedWriter((new FileWriter(dataLogFile, true)))) {
//                bw.newLine();
//                bw.write(lastLevelName + CSV_SEPARATOR +
//                        solvableModules + CSV_SEPARATOR +
//                        solvedModules + CSV_SEPARATOR +
//                        modules + CSV_SEPARATOR +
//                        timeLeft + CSV_SEPARATOR +
//                        strikes + CSV_SEPARATOR +
//                        bombState);
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}