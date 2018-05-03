package com.abhishek.oneminvideo;

public class AndroidUtilities {

    public static int dp(float value) {
        return (value==0)? 0 : (int) Math.ceil(1 * value);
    }

    public static float[] getRange(float start,float end,float duration) {
        float[] a = {start*duration,end*duration};
        return a;
    }

}