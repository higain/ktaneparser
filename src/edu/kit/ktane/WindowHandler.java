package edu.kit.ktane;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.WNDENUMPROC;
import com.sun.jna.win32.StdCallLibrary;

public class WindowHandler {
    public interface User32 extends StdCallLibrary {
        User32 INSTANCE = (User32) Native.loadLibrary("user32", User32.class);

        boolean EnumWindows(WinUser.WNDENUMPROC lpEnumFunc, Pointer arg);

        int GetWindowTextA(HWND hWnd, byte[] lpString, int nMaxCount);

        boolean MoveWindow(Pointer hWnd, int x, int y, int nWidth, int nHeight,
                           boolean bRepaint);

        boolean SetWindowPos(Pointer hWnd, Pointer hWndInsertAfter, int X, int Y, int cx,
                             int cy, int uFlags);
    }


    private User32 user32 = User32.INSTANCE;

    public void resizeWindow() {
        user32.EnumWindows(new WNDENUMPROC() {
            int count = 0;

            @Override
            public boolean callback(HWND hWnd, Pointer arg1) {
                byte[] windowText = new byte[512];
                user32.GetWindowTextA(hWnd, windowText, 512);
                String wText = Native.toString(windowText);
                // get rid of this if block if you want all windows regardless of whether
                // or not they have text
                if (!wText.contains("Keep Talking")) {
                    // User32 user32 = User32.INSTANCE;
                    // user32.
                    return true;
                }

                System.out.println("Found window with text " + hWnd + ", total " + ++count
                        + " Text: " + wText);
                moveWindow(hWnd.getPointer(), 0, -32, 1280, 1030);
                return true;
            }
        }, null);
    }

    public boolean moveWindow(Pointer hWnd, int x, int y, int nWidth,
                              int nHeight) {
        boolean bRepaint = true;
        return user32.MoveWindow(hWnd, x, y, nWidth, nHeight, bRepaint);
    }

    public void toBackground(boolean toBack) {
        user32.EnumWindows(new WNDENUMPROC() {
            int count = 0;

            @Override
            public boolean callback(HWND hWnd, Pointer arg1) {
                byte[] windowText = new byte[512];
                user32.GetWindowTextA(hWnd, windowText, 512);
                String wText = Native.toString(windowText);
                // get rid of this if block if you want all windows regardless of whether
                // or not they have text
                if (!wText.contains("Keep Talking")) {
                    // User32 user32 = User32.INSTANCE;
                    // user32.
                    return true;
                }

                System.out.println("Found window with text " + hWnd + ", total " + ++count
                        + " Text: " + wText);
                toBackground(hWnd.getPointer(), toBack);
                return true;
            }
        }, null);
    }

    // if toBack true: in Background schieben
    // if toBack false: in Foreground ziehen
    public boolean toBackground(Pointer hWnd, boolean toBack) {
        Pointer HWND_BOTTOM = null;
        if (toBack) {
            HWND_BOTTOM = new Pointer(1);
        }
        else {
            HWND_BOTTOM = new Pointer(-1);
        }

        return user32.SetWindowPos(hWnd, HWND_BOTTOM, 0, -32, 1280, 1030, 0);
    }

}
