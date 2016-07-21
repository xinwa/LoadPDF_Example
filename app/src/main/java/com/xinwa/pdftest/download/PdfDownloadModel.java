package com.xinwa.pdftest.download;


import okhttp3.ResponseBody;
import rx.Observable;
import rx.schedulers.Schedulers;

/**
 * Created by xinwa on 2016/07/17.
 */
public class PdfDownloadModel {

    public PdfDownloadModel(){

    }
    private static DownloadApi mDownloadApi = DownloadEngine.getInstance().create(DownloadApi.class);

    public Observable<ResponseBody> downloadFile(String url){

        return mDownloadApi.downloadFile(url)
                .subscribeOn(Schedulers.io());
    }

    public Observable<ResponseBody> downloadSoFile(String url,String startPosition){

        return mDownloadApi.downloadSoFile(url,startPosition)
                .subscribeOn(Schedulers.io());
    }

}
