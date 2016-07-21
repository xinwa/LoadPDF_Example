package com.xinwa.pdftest.download;


import com.xinwa.pdftest.download.progress.ProgressHelper;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;



/**
 * Created by xinwa on 2016/07/20.
 */
public class DownloadEngine {

    private static final String ENDPOINT = "http://192.168.1.232:8080/wenwen/zebra_lib/";
    public OkHttpClient okHttpClient;

    private Retrofit retrofit;

    public <T> T create(final Class<T> service) {
        return retrofit.create(service);
    }

    private DownloadEngine(){

        okHttpClient = ProgressHelper.addProgress(null).build();

        retrofit= new Retrofit.Builder()
                .baseUrl(ENDPOINT)
                .client(okHttpClient)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();
    }

    private static class SingletonHolder {
        private static DownloadEngine INSTANCE = new DownloadEngine();
    }

    public static DownloadEngine getInstance(){
        return SingletonHolder.INSTANCE;
    }
}
