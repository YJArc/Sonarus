package com.yjarc.sonarus.UIHelper;


public class ColorizeText {

    public String ColorizeTexts(String Text){
        int colorchoice = Text.hashCode() % 2;

        switch (colorchoice){
            case 1:
                return "<font color=\"#0000FF\">" + Text + "</font><br><br>";
            case 2:
                return "<font color=\"#00FF00\">" + Text + "</font><br><br>";
            default:
                return "<font color=\"#FF0000\">" + Text + "</font><br><br>";
        }

    }
}
