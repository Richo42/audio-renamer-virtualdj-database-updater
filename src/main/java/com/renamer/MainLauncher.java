package com.renamer;

public class MainLauncher {
    public static void main(String[] args) {
        try {
            MainApp.main(args);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR FATAL");
            try {
                Thread.sleep(15000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }
}