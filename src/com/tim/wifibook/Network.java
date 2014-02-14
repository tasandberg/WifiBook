package com.tim.wifibook;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Created by tim on 1/15/14.
 */
public class Network {
    private String mName;
    private UUID mId;
    private boolean mActive;
    private static final String JSON_ID = "id";
    private static final String JSON_NAME = "name";
    private static final String JSON_SELECTED = "isSelected";



    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put(JSON_ID, mId.toString());
        json.put(JSON_NAME, mName);
        json.put(JSON_SELECTED, mActive);
        return json;
    }

    Network(String name){
        mName = name;
        mId= UUID.randomUUID();
        mActive = true;
    }

    public boolean isActive() {
        return mActive;
    }

    public void setActive(boolean isSelected) {
        mActive = isSelected;
    }


    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public UUID getId() {
        return mId;
    }


    @Override
    public String toString() {
        return mName;
    }
}
