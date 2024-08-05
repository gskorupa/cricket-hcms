package org.cricketmsf.hcms.application.out;

import java.io.IOException;

public class SystemCommands {
    
    public static String runPullCommand(String cmd){
        try {
           Process p= Runtime.getRuntime().exec("git pull");
           p.waitFor();
           return ""+p.exitValue();
        } catch (IOException|InterruptedException e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }
}
