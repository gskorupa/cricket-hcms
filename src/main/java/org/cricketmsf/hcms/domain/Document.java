package org.cricketmsf.hcms.domain;

import java.util.HashMap;

public class Document {

    public String path="";
    public String name="";
    public String content="";
    public long updateTimestamp=0;
    public HashMap<String, String> metadata = new HashMap<>();

    public Document() {
    } 

}
