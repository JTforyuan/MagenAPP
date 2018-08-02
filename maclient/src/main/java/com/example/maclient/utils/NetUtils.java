package com.example.maclient.utils;

import android.os.Environment;
import android.renderscript.ScriptGroup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by JiaTang on 2018/2/9.
 */

public class NetUtils {

    private static final String TAG = "NetUtils";

    static String result = "";
    private static OkHttpClient client = new OkHttpClient();

    /**
     * 发送GET请求
     *
     * @param urlString 请求的URL地址
     * @return 请求得到的字符串
     */
    public static String sendGet(String urlString) throws IOException {

        Request request = new Request.Builder()
                .get()
                .url(urlString)
                .build();
        Response response = client.newCall(request).execute();

        return response.body().string();
    }

    /**
     * 发送POST请求，@params 和 @values 应一一对应
     *
     * @param urlString 请求的URL地址
     * @param params POST请求参数
     * @param values POST请求参数值
     *
     * @return 请求得到的结果字符串
     */
    public static String sendPost(String urlString, List<String> params, List<String> values)
            throws IOException {

        FormBody.Builder builder = new FormBody.Builder();
        if(params != null && values != null) {
            for (int i = 0; i < params.size(); i++) {
                builder.add(params.get(i), values.get(i));
            }
        }
        Request request = new Request.Builder()
                .post(builder.build())
                .url(urlString)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    /**
     * 利用Post请求进行文件上传
     *
     * @param url URL
     * @param filePath 需要上传的文件路径
     */
    public static String sendPostFile(String url, String filePath) throws IOException
    {

        File file = new File(filePath);
        String filename = filePath.substring(filePath.lastIndexOf("/") + 1);
        // 构建请求体

        RequestBody filebody = RequestBody.create(MediaType.parse("audio/wav"), file);
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("filename", filename, filebody)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    public static void downloadFile(final String url) {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream is = null;
                int len = 0;
                byte[] buffer = new byte[1024];
                FileOutputStream fos = null;

                File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                        , "maclient/gen_midi");
                if ( !dir.exists()) {
                    dir.mkdirs();
                }

                String filename = url.substring(url.lastIndexOf("/") + 1);
                File downloadFile = new File(dir.getAbsolutePath(), filename);

                try {
                    is = response.body().byteStream();
                    long total = response.body().contentLength();
                    fos = new FileOutputStream(downloadFile);
                    while ( (len = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }

                    fos.flush();

                } catch (Exception e ) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (is != null)
                            is.close();
                        if (fos != null)
                            fos.close();
                    } catch (IOException ioe) {

                    }
                }
            }
        });
    }
}
