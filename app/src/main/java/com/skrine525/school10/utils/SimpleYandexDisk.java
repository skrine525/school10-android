package com.skrine525.school10.utils;

import android.os.FileUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SimpleYandexDisk {
    private OkHttpClient client;
    private String api_token;

    public SimpleYandexDisk(String api_token){
        this.client = new OkHttpClient();
        this.api_token = api_token;
    }

    public Response getUploadServerURL(String path) throws IOException {
        Request request = new Request.Builder().url("https://cloud-api.yandex.net/v1/disk/resources/upload?path=" + URLEncoder.encode(path, "UTF-8"))
                .addHeader("Authorization", api_token)
                .build();
        return client.newCall(request).execute();
    }

    public Response uploadFile(String href, File file, CountingFileRequestBody.ProgressListener listener) throws IOException {
        /*
        RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file", "name", RequestBody.create(MediaType.parse("application/*"), file))
                .build();
         */
        CountingFileRequestBody countingFileRequestBody = new CountingFileRequestBody(file, "application/*", listener);
        Request request =  new Request.Builder().url(href)
                .put(countingFileRequestBody)
                .build();
        return client.newCall(request).execute();
    }

    public Response makeDirectory(String path) throws IOException {
        Request request  = new Request.Builder().url("https://cloud-api.yandex.net/v1/disk/resources?path=" + URLEncoder.encode(path, "UTF-8"))
                .put(RequestBody.create(null, ""))
                .addHeader("Authorization", api_token)
                .build();
        return client.newCall(request).execute();
    }
}
