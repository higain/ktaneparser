package edu.kit.ktane;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.awt.*;
import java.awt.event.InputEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Created by maxis on 17.05.2017.
 * TODO: Log Ausgabe nur bei Events: - Modul entschärft, Strike erhalten, Bombe entschärft
 * TODO: Zeit, die pro Modul gebraucht wird (log wann Modul aktiviert wird)
 */
public class KtaneHandler {

    /**
     * Logger
     */
    final Logger logger = Logger.getLogger(getClass().getName());

    private String logFile = "logs/ktane.log";
    private String ktaneStarter = "D:/workspace/IISM/ktane_start.vbs";
    private String ktaneExeLocation = "C:/Program Files (x86)/Steam/steamapps/common/Keep Talking and Nobody Explodes/ktane.exe";
    private String[] ktaneFromSteam = {"C:/Program Files (x86)/Steam/Steam.exe", "-applaunch", "341800"};
    private String[] missionidslgbfpt1 = {"mod_lgbfpt1_1bomb1", "mod_lgbfpt1_1bomb2", "mod_lgbfpt1_1bomb3",
            "mod_lgbfpt1_1bomb4", "mod_lgbfpt1_1bomb5", "mod_lgbfpt1_1bomb6",
            "mod_lgbfpt1_1bomb7", "mod_lgbfpt1_1bomb8", "mod_lgbfpt1_1bomb9",
            "mod_lgbfpt1_1bomb10"};
    private String[] missionidslgbfpt2 = {"mod_lgbfpt2_2bomb1", "mod_lgbfpt2_2bomb2", "mod_lgbfpt2_2bomb3",
            "mod_lgbfpt2_2bomb4", "mod_lgbfpt2_2bomb5", "mod_lgbfpt2_2bomb6",
            "mod_lgbfpt2_2bomb7", "mod_lgbfpt2_2bomb8", "mod_lgbfpt2_2bomb9",
            "mod_lgbfpt2_2bomb10"};
    private String[] missionidsgfpt2 = {"mod_gfpt2_3bomb1", "mod_gfpt2_3bomb2", "mod_gfpt2_3bomb3",
            "mod_gfpt2_3bomb4", "mod_gfpt2_3bomb5", "mod_gfpt2_3bomb6",
            "mod_gfpt2_3bomb7", "mod_gfpt2_3bomb8", "mod_gfpt2_3bomb9",
            "mod_gfpt2_3bomb10", "mod_gfpt2_3bomb11", "mod_gfpt2_3bomb12"};
    private String[] missionidlgbfpt3 = {"mod_lgbfpt3_4bomb1", "mod_lgbfpt3_4bomb2", "mod_lgbfpt3_4bomb3",
            "mod_lgbfpt3_4bomb4", "mod_lgbfpt3_4bomb5", "mod_lgbfpt3_4bomb6",
            "mod_lgbfpt3_4bomb7", "mod_lgbfpt3_4bomb8", "mod_lgbfpt3_4bomb9",
            "mod_lgbfpt3_4bomb10", "mod_lgbfpt3_4bomb11", "mod_lgbfpt3_4bomb12"};
    private String[] seeds = {"11778", "13718", "49060", "59085", "27392", "1120", "59549", "44791", "643", "41167", "76218", "6863"};

    static private String runningGame;
    private boolean isRoundRunning = false;
    public boolean experiment;
    private int numberOfRounds = 0;

    // Some data storage variables.
    private Long gameRoundInitiationTimestamp, experimentInitialisationTimestamp;
    private Timestamp latestTs = new Timestamp(System.currentTimeMillis());
    private Timestamp bombStartTimestamp, bombEndTimestamp;
    private String runningClientID;

    //    private LinkedList<GameData> gameDataEvents = new LinkedList();
    private ArrayList<String> sessionEventList, gameEventList;
    private String solvableModules, modules, solvedModules;
    private String timeLeft, bombState;
    private long strikes;
    private int countSolvedModules = 0;
    private ArrayList<String> tmpSolved, allModules;
    private long tmpStrikes = 0;

    WindowHandler windowHandler;
    KtaneJsonHandler ktjshandler;
    static Process ktaneProcess;
    private String currentBombId;

    public KtaneHandler(Long experimentInitiationTimestamp, Long gameRoundInitiationTimestamp, String clientID) {
        // Time the whole experiment was set up (should not change during rounds)
        this.experimentInitialisationTimestamp = experimentInitiationTimestamp;

        // Time the game was started in brownie (this is not the TS for when users start defusing).
        this.gameRoundInitiationTimestamp = gameRoundInitiationTimestamp;

        // The name of the client pc that the game is running on.
        this.runningClientID = clientID;

        // Prepare the file writer
        // GameData.prepareGameDataFile(experimentInitiationTimestamp);

        // Start the game and initialize the JSON game parser.
        initializeJSONHandler();
        tmpSolved = new ArrayList<>();

        // Initialize WindowHandler for back- and foreground management
        windowHandler = new WindowHandler();

    }

    public void startupGame() {
        // TODO: Start game the first time, then only check if its running
        if (runningGame != null) {
            System.out.println("Game pid: " + runningGame);
        }
        if (runningGame == null || runningGame.equals("")) {
            runningGame = startGame(ktaneFromSteam);
            System.out.println("Game pid: " + runningGame);
            System.out.println("Starting Game from " + Arrays.toString(ktaneFromSteam));
            windowHandler = new WindowHandler();
            schlafen(10000);    // TODO: Not sure if needed...

            // Intro-Screen
            windowHandler.toBackground(false);
            try {
                Robot robot = new Robot();
                robot.mouseMove(300, 300);
                robot.mousePress(InputEvent.BUTTON1_MASK);
                robot.mouseRelease(InputEvent.BUTTON1_MASK);
                System.out.println("Mouse press bei 300, 300");
            } catch (AWTException e) {
                System.out.println("Mouse click failed!");
            }
            schlafen(1500);
            // Enable Mods
            windowHandler.toBackground(false);
            try {
                Robot robot = new Robot();
                robot.mouseMove(830, 900
                );
                robot.mousePress(InputEvent.BUTTON1_MASK);
                robot.mouseRelease(InputEvent.BUTTON1_MASK);
                System.out.println("Mouse press bei 800, 900");
            } catch (AWTException e) {
                System.out.println("Mouse click failed!");
            }
        }
    }

    /**
     * Takes number of games which should be played and executes from 1 to numberOfGames until finished
     *
     * @param numberOfGames Number of games which should be played
     */
    public void goStandalone(int numberOfGames) {
        while (numberOfGames > numberOfRounds) {
            System.out.println("Start game Nr. " + numberOfRounds + " of " + numberOfGames);
            go(numberOfRounds);
            numberOfRounds++;
        }

        System.out.println("Experiment zu Ende!");
    }

    /**
     * Takes one bomb-id (from 1-10/12) and executes this one only
     *
     * @param gameId Which predefined bomb should be played
     */
    public Boolean go(int gameId) {
        currentBombId = missionidlgbfpt3[gameId];

        // Reset game metrics.
        resetVariables();

        System.out.println("Aktuellster TS: " + latestTs);

        // Set flag that round is starting.
        isRoundRunning = true;
        // schlafen(3000);

        // Start the mission

        while (!startMission(gameId)) {
            schlafen(200);
            System.out.println("Konnte mission nicht starten.");
        }

        // Wait briefly before showing the window, so that users only see the loading screen.
        schlafen(750);

        // Move window to the foreground
        windowHandler.toBackground(false);
        System.out.println("Moving to Foreground");

        schlafen(5000);     // Short break until mission has started.

        // Analyze bomb state continuously
        while (bombState == null || bombState.equals("")) {
            parseJson(ktjshandler.fetchBombInfos());
            logDiffs();
            schlafen(1000);      // TODO: Consider removing this, if events are not parsed directly from log.
            System.out.println("Mission noch nicht fertig geladen.");
        }
        System.out.println("Mission running, analyze it!");
        bombStartTimestamp = new Timestamp(System.currentTimeMillis());
        System.out.println("Start at " + bombStartTimestamp);
        try {
            sessionEventList.add(bombStartTimestamp+";"+currentBombId+";START;Die Mission wurde gestartet;");
            gameEventList.add("Start;" + bombStartTimestamp);
        }
        catch(NullPointerException npe) {
            System.out.println("Konnte Timestamp nicht setzen.");
            System.out.println(npe);

        }

        try {
            System.out.println("BombState: " + bombState);
        } catch (NullPointerException npe) {
            System.out.println("BombState is null");
        } catch (Exception e) {
            System.out.println(e);
        }

        boolean runWorker = true;
        while (runWorker) {
            if ((bombState.equals("Exploded")) || bombState.equals("Defused")) {
                bombEndTimestamp = new Timestamp(System.currentTimeMillis());
                runWorker = false;
            }
            parseJson(ktjshandler.fetchBombInfos());
            logDiffs();
            schlafen(200);
        }

        // Letztes Mal ausführen, um alles zu loggen
        parseJson(ktjshandler.fetchBombInfos());
        logDiffs();
        if (bombState.equals("Exploded") && !timeLeft.equals("00.00")) {
            Timestamp tempstamp = new Timestamp(System.currentTimeMillis());
            sessionEventList.add(tempstamp+";"+currentBombId+";STRIKE;Got strike nr " + ((int) strikes + 1) + " with " + timeLeft + " seconds remaining;"+timeLeft);
            System.out.println(sessionEventList.get(sessionEventList.size() - 1));
        }

        schlafen(1000);
        // System.out.println("Runde zu ende");
        Timestamp tempstamp = new Timestamp(System.currentTimeMillis());
        sessionEventList.add(tempstamp+";"+currentBombId+";"+bombState.toUpperCase()+";Bomb " + bombState + " with " + timeLeft + " seconds remaining;"+timeLeft);
        tempstamp.setTime(System.currentTimeMillis());
        sessionEventList.add(tempstamp+";"+currentBombId+";END;"+"Runde beendet.;"+timeLeft);
        logGameEvents();

        System.out.println(sessionEventList.get(sessionEventList.size() - 1));
        // TODO: Write game data to file
        try {
            GameData.writeGameDataToFile(sessionEventList, "_sessionLog", experimentInitialisationTimestamp, this.runningClientID);
            GameData.writeGameDataToFile(gameEventList, "_gameLog", experimentInitialisationTimestamp, this.runningClientID);

        } catch (Exception e) {
            System.out.println("Error!");
            System.out.println(e);
        }

        // Click Worker to return the screen to the main menu.
        try {
            int sleep = 7500;
            Robot robot = new Robot();
            int x = 0;
            int y = 0;
            int waitX = 600;
            int waitY = 300;

            if (bombState.equals("Exploded")) {
                x = 690;
                y = 750;
            } else if (bombState.equals("Defused")) {
                x = 750;
                y = 750;
            }
            while (sleep >= 0) {
                // robot.mouseMove(9999, 9999);
                robot.mouseMove(waitX, waitY);
                schlafen(1);
                sleep = sleep - 1;
            }
            sleep = 200;
            while (!timeLeft.equals("")) {
                robot.mouseMove(x, y);
                robot.mousePress(InputEvent.BUTTON1_MASK);
                robot.mouseRelease(InputEvent.BUTTON1_MASK);
                parseJson(ktjshandler.fetchBombInfos());
                schlafen(1);
            }
            windowHandler.toBackground(false);
        } catch (AWTException e) {
            System.out.println("Mouse click failed!");
        }

        schlafen(200);

//        stopGame();

        // Reset game metrics.
        strikes = 0;
        timeLeft = bombState = solvableModules = modules = solvedModules = null;

        // Set flag that round is over.
        isRoundRunning = false;

        // Move window to the background
        windowHandler.toBackground(true);
        System.out.println("Moving to Background");

        return isRoundRunning;
    }

    public void logGameEvents() {
        gameEventList.add(1, ("End;"+bombEndTimestamp));
        gameEventList.add("Bomb Status;"+bombState);
        gameEventList.add("Time Remaining;"+timeLeft);
        gameEventList.add("Nr of Strikes;" + strikes);
        gameEventList.add("Nr of solved Modules;"+ countSolvedModules);
    }


    public void logDiffs() {
        try {
            Timestamp tempstamp = new Timestamp(System.currentTimeMillis());
            // Diff Strikes
            if (tmpStrikes != strikes) {
                sessionEventList.add(tempstamp+";"+currentBombId+";STRIKE;Got strike nr " + strikes + " with " + timeLeft + " seconds remaining;"+timeLeft);
                System.out.println(sessionEventList.get(sessionEventList.size() - 1));
                tmpStrikes = strikes;
            }

            // Diff Modules
            // Working copies of Strings:
            String wcSolved = solvedModules.replaceAll("\"", "").replaceAll("\\[", "").replaceAll("\\]", "");
            ArrayList<String> solvedLive;

            // Processing starts, as soon as one module is solved
            if (wcSolved.length() > 0) {
                solvedLive = new ArrayList<>(Arrays.asList(wcSolved.split(",")));
                // If arrays differ in size, a new module has been solved. The existing elements are removed from the local List and therefore leave the new module
                // System.out.println(solvedLive.toString());
                if (tmpSolved == null) {
                    tmpSolved = new ArrayList<>();
                }
                // System.out.println("SopulvedLive: " + solvedLive.size() + "TmpSolved: " + tmpSolved.size());
                if ((solvedLive.size() != tmpSolved.size()) || tmpSolved.toString().equals("[]")) {
                    ArrayList<String> tmpNewMod = new ArrayList<>(solvedLive);
                    ArrayList<String> tmpRemaining = new ArrayList<>(allModules);
                    // Get the newly solved Module
                    tmpNewMod.removeAll(tmpSolved);
                    // Get remaining Modules
                    tmpRemaining.removeAll(solvedLive);

                    sessionEventList.add(tempstamp+";"+currentBombId+";SOLVED MODULE;Solved Module " + tmpNewMod.get(0) + " with " + timeLeft + " seconds remaining. " +
                            "The remaining Modules are: " + tmpRemaining.toString()+";"+timeLeft);
                    System.out.println(sessionEventList.get(sessionEventList.size() - 1));
                    countSolvedModules = countSolvedModules+1;

                    // Update temp variables
                    tmpSolved.clear();
                    tmpSolved.addAll(solvedLive);
                }

            }
        }
        catch(Exception e) {
            logger.info(e.toString());
            System.out.println("Fehler in logDiff: " + e);
        }
    }

    public void parseJson(JSONObject jobj) {
        try {
            solvableModules = ((JSONArray) jobj.get("SolvableModules")).toString(); // .replace("\"", "").replace("[", "").replace("]", "").split(",");
            if (!solvableModules.equals("[]") && (allModules == null)) {
                allModules = new ArrayList<>(Arrays.asList(solvableModules.replaceAll("\"", "").replaceAll("\\[", "").replaceAll("\\]", "").split(",")));
                System.out.println("Set all Modules: " + allModules.toString());
            }
            modules = ((JSONArray) jobj.get("Modules")).toString(); //.replace("\"", "").replace("[", "").replace("]", "").split(",");
            solvedModules = ((JSONArray) jobj.get("SolvedModules")).toString(); //.replace("\"", "").replace("[", "").replace("]", "").split(",");
            timeLeft = (String) jobj.get("Time");
            strikes = ((Long) jobj.get("Strikes"));
            bombState = (String) jobj.get("BombState");

            if (!bombState.equals("") && timeLeft.equals("")) {
                bombState = "";
            }

            // printBombStatus();
        } catch (NullPointerException npe) {
            System.out.println("Cannot parse Json Object, is the game running?");
            System.out.println(npe.getLocalizedMessage());
        }
    }

    public void printBombStatus() {
        System.out.println("sm: " + solvableModules + " " + "sdm: " + solvedModules + " " +
                "all: " + modules + " " + "t: " +
                timeLeft + " " + "st: " + strikes +
                " " + "s: " + bombState);
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

    public void resetVariables() {
        strikes = 0;
        timeLeft = null;
        bombState = null;
        solvableModules = null;
        modules = null;
        solvedModules = null;
        tmpSolved = null;
    }

    public void initializeJSONHandler() {
        ktjshandler = new KtaneJsonHandler(8085, "http://localhost:8085/");
        sessionEventList = new ArrayList<>();
        gameEventList = new ArrayList<>();
    }

    public boolean stopGame() {
        if (runningGame != null && !runningGame.equals("")) {
            String cmd = "taskkill /F /PID " + runningGame;
            try {
                Runtime.getRuntime().exec(cmd);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }

    public String startGame(String[] ktaneLocation) {
        try {
            // Runtime.getRuntime().exec(ktaneLocation);
            ktaneProcess = new ProcessBuilder(ktaneLocation).start();

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
        boolean state = ktjshandler.startMission(missionidlgbfpt3[missionId], "" + seeds[missionId]);
        return state;
    }

    /**
     * Check if the Ktane game is running in the background.
     *
     * @return
     */
    public boolean isGameStarted() {
        return ktaneProcess.isAlive();
    }

    /**
     * Check if a bomb round is running in the background.
     *
     * @return
     */
    public boolean isRoundRunning() {
        return isRoundRunning;
    }

    //    /**
//     * @return
//     */
//    public boolean processFile() {
//
//        try {
//
//            /*File source = new File(logFile);
//            File dest = new File(logFile+"tmp");
//            try {
//                Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }*/
//
//            List<String> tmp = new ArrayList<>();
//            FileReader fr = new FileReader(logFile);
//            BufferedReader br = new BufferedReader(fr);
//            String ch;
//
//            while ((ch = br.readLine()) != null) {
//                // Throw out non-info or non-debug (such as tables, html code, etc)
//                if (ch.startsWith("DEBUG") || ch.startsWith(" INFO")) {
//                    ch = ch.replace("DEBUG ", "");
//                    ch = ch.replace(" INFO ", "");
//                    ch = ch.replaceFirst(",", ".");
//                    tmp.add(ch);
//                    // System.out.println(ch);
//                }
//            }
//            br.close();
//            // System.out.println("Checking " + tmp.size() + " Log entries");
//            int counter = 0;
//            boolean abbruch = false;
//            outerloop:
//            for (int i = tmp.size() - 1; i >= 0; i--) {
//                // Prüfung durchführen, solange die Daten aktueller als die letze Prüfung sind
//                // System.out.println("Gelesener TS: " + tmp.get(i).substring(0, 23));
//                long duration = (2 * 60 * 60 * 1000);
//                Timestamp ts = Timestamp.valueOf(tmp.get(i).substring(0, 23));
//                ts.setTime(ts.getTime() + duration);
//                // System.out.println("Vergleichender TS: " + ts);
//                if (ts.equals(latestTs) || ts.before(latestTs)) {
//                    // System.out.println("Hier waren wir schon!");
//                    Timestamp tsmid = Timestamp.valueOf(tmp.get(tmp.size() - 1).substring(0, 23));
//                    tsmid.setTime(tsmid.getTime() + duration);
//                    latestTs = tsmid;
//                    // System.out.println("Neuer latest TS: " + latestTs);
//                    abbruch = true;
//                    break outerloop;
//                } else {
//                    counter++;
//                    // Wenn OnRoundEnd() in der Log-Datei steht wird true zurück gegeben, da Spiel vorbei
//                    if (tmp.get(i).contains("OnRoundEnd()")) {
//                        System.out.println("Runde zu Ende!");
//                        tmp.clear();
//                        return true;
//                    }
//                }
//            }
//            if (!abbruch) {
//                long duration = (2 * 60 * 60 * 1000);
//                Timestamp tsend = Timestamp.valueOf(tmp.get(tmp.size() - 1).substring(0, 23));
//                tsend.setTime(tsend.getTime() + duration);
//
//                latestTs = tsend;
//                System.out.println("Neuer latest TS: " + latestTs);
//            }
//            if (counter > 0) {
//                System.out.println(counter + " Einträge neu gelesen");
//            }
//        } catch (FileNotFoundException fnfe) {
//            System.out.println(logFile);
//            System.out.println("Dateipfad falsch!");
//            return false;
//        } catch (IOException ioe) {
//            System.out.println("IO-Fehler!");
//            return false;
//        }
//        return false;
//    }
}
