package com.xinwa.pdftest.download;

import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Streaming;
import retrofit2.http.Url;
import rx.Observable;

/**
 * Created by xinwa on 2016/07/17.
 */
public interface DownloadApi {
    @Streaming
    @GET
    Observable<ResponseBody> downloadFile(@Url String fileUrl);


    /**
     * @param startPosition 断点续传开始的位置
     * */
    @Streaming
    @GET
    Observable<ResponseBody>downloadSoFile(@Url String fileUrl, @Header("Range") String startPosition);
}
