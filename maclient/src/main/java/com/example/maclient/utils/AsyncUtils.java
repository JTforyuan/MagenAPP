package com.example.maclient.utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by JiaTang on 2018/2/24.
 */

public class AsyncUtils {
    private static final String SERVER_URL = "http://193.112.103.91/MagenServer/api/audio";

    public static String UploadWavFile(String filepath) {
        try {
            String response = NetUtils.sendPostFile(SERVER_URL, filepath);
            JSONObject json = new JSONObject(response);
            String state = json.getString("upload_state");
            if ( state.equals("successed") ) {
                final String gen_file = json.getString("generate_file");
                return gen_file;
            } else {
                return null;
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (JSONException je) {
            je.printStackTrace();
        }
        return null;
    }

    public static void downloadMidiFile(String paramsname) {
        String url = SERVER_URL.concat("/" + paramsname);
        NetUtils.downloadFile(url);
    }
}
