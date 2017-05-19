public class Main {
    /*

http://svengoossens.nl/2015/12/25/ktane-hue/
https://github.com/LtHummus/KTANELog/blob/master/ktaneparser/ktaneparser.py

    - Performance Daten
    -- Neue Runde gestartet
    -- Leaderboard
    -- Anzahl Fehler
     */

    public static void main(String[] args) {
        KtaneHandler khandler = new KtaneHandler();
        khandler.experiment = true;
        khandler.go(2);

    }

}
