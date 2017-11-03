package com.yjarc.sonarus.RecylerViewAdapters;


public class StatusObject {
    public String imgURI, username, status, uID;
    public long timestamp;

    public StatusObject (String uID, String username, String image, String status, long timestamp){
        this.uID = uID;
        this.username = username;
        this.imgURI = image;
        this.status = status;
        this.timestamp = timestamp;
    }

}
