package com.xinwa.pdftest;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.ScrollBar;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.shockwave.pdfium.PdfDocument;
import com.xinwa.pdftest.download.ConstantStore;
import com.xinwa.pdftest.download.PdfDownloadModel;
import com.xinwa.pdftest.download.progress.DownloadProgressHandler;
import com.xinwa.pdftest.download.progress.ProgressHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.ResponseBody;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements OnPageChangeListener, OnLoadCompleteListener {
    @Bind(R.id.pdfView)
    PDFView pdfView;
    @Bind(R.id.scrollBar)
    ScrollBar scrollBar;
    @Bind(R.id.btnCheck)
    Button btnCheck;
    @Bind(R.id.btnDeleteSoFile)
    Button btnDeleteSoFile;
    @Bind(R.id.btnDeletePdf)
    Button btnDeletePdf;
    @Bind(R.id.btnStart)
    Button btnStart;

    private ProgressDialog progressDialog;

    Integer pageNumber = 1;
    private long startPosition;

    private static String TAG = "PDfInfo";
    private String pdfFileName = "pdf_temp.pdf";

    private ConstantStore store;
    private File tempFile;

    private String pdfUrl = "http://xyw.bit.edu.cn/docs/20120614160010185.pdf";

    private Subscription subscription;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        checkSoFileExist();
    }

    //这边检测soFile是否完整
    private void checkSoFileExist(){
        store = ConstantStore.getConstantStore(this);

        if(ConstantStore.isPdfSoFileDownloaded(this,store)){
            Log.e("info","so文件存在");
            initData();
        }else{
            Log.e("info","so文件不存在");
            initSoFileData();
        }
    }


    private void initData() {
        File jnipdfium = new File(getCacheDir()+File.separator+"libjniPdfium.so");
        File modpdfium = new File(getCacheDir()+File.separator+"libmodpdfium.so");

        System.load(modpdfium.getAbsolutePath());
        System.load(jnipdfium.getAbsolutePath());

        //download pdf file
         new PdfDownloadModel().downloadFile(pdfUrl)
                .flatMap(new Func1<ResponseBody, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(ResponseBody responseBody) {
                        boolean ret = writeResponseBodyToDisk(responseBody);
                        return Observable.just(ret);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        showLoadProgressDialog();
                    }
                })
                .doOnTerminate(new Action0() {
                    @Override
                    public void call() {
                        progressDialog.dismiss();
                    }
                })
                .subscribe(new Subscriber<Boolean>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        showError(e);
                    }

                    @Override
                    public void onNext(Boolean isSuccess) {
                        showResult(isSuccess);
                    }
                });
    }

    private void initSoFileData(){
        //问题1：当用户退出应用到后台后，retrofit是否会停止下载，如果停止下载是否会调用onError方法
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setProgressNumberFormat("%1d KB/%2d KB");
        dialog.setTitle("下载");
        dialog.setMessage("正在下载，请稍后...");
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setCancelable(false);
        dialog.setButton("暂停下载", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                subscription.unsubscribe();
            }
        });
        dialog.show();

        startPosition = 0L;
        //目前我pdf存放的位置,需要注意的一点是libjniPdfium.so 中的P是大写字母，后面那个是小写字母
        String[] strs = {"libjniPdfium.so", "libmodpdfium.so"};

        //设置回调接口，该接口运行与主线程，在下载pdf文件令progressHandler为null，不然又会触发
        //下载进度的显示
        ProgressHelper.setProgressHandler(new DownloadProgressHandler() {
            @Override
            protected void onProgress(long bytesRead, long contentLength, boolean done) {
                dialog.setMax((int) (contentLength/1024));
                dialog.setProgress((int) (bytesRead/1024));
                if(done){
                    dialog.dismiss();
                }
            }
        });

        subscription = Observable.from(strs).subscribe(new Action1<String>() {
            @Override
            public void call(final String s) {
                //不同的SoFile 他们的startPosition是不同的
                startPosition = store.getSoFileStartPosition(s);
                new PdfDownloadModel().downloadSoFile(s,"bytes="+startPosition+"-")
                        .flatMap(new Func1<ResponseBody, Observable<Boolean>>() {
                            @Override
                            public Observable<Boolean> call(ResponseBody responseBody) {

                                boolean ret = RandomWriteResponseBodyToDisk(responseBody,s,startPosition);
                                return Observable.just(ret);
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnTerminate(new Action0() {//Obeservable结束前触发
                            @Override
                            public void call() {
                            }
                        })
                        .subscribe(new Subscriber<Boolean>() {
                            @Override
                            public void onCompleted() {

                            }

                            @Override
                            public void onError(Throwable e) {
                                e.printStackTrace();
                                Log.e("info", "请求错误");
                            }

                            @Override
                            public void onNext(Boolean isSuccess) {
                                //主要是因为soFile有俩个，必须全部下载下来之后在去下载pdf文件
                                if(isSuccess && ConstantStore.isPdfSoFileDownloaded(MainActivity.this,store)){
                                    initData();
                                }
                            }
                        });
            }
        });
    }

    private boolean writeResponseBodyToDisk(ResponseBody body) {
        try {
            // todo change the file location/name according to your needs
            tempFile = new File(getCacheDir() + File.separator + "pdf_temp.pdf");

            InputStream inputStream = null;
            OutputStream outputStream = null;

            if (!tempFile.exists()) {
                tempFile.createNewFile();
            }

            try {
                byte[] fileReader = new byte[1024 * 256]; //4k

                long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;

                inputStream = body.byteStream();
                outputStream = new FileOutputStream(tempFile);

                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }

                    outputStream.write(fileReader, 0, read);

                    fileSizeDownloaded += read;

                    Log.d(TAG, "file download: " + fileSizeDownloaded + " of " + fileSize);
                }

                outputStream.flush();

                return true;
            } catch (IOException e) {
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            return false;
        }
    }

    //断点续传，增加位置参数
    private boolean RandomWriteResponseBodyToDisk(ResponseBody body,String fileName,long position) {
        try {
            // todo change the file location/name according to your needs
            Log.e("info","开始保存大小");
            store.saveSoFileSize(fileName,body);

            InputStream inputStream = null;

            RandomAccessFile file = new RandomAccessFile(getCacheDir()+ File.separator+fileName, "rwd");
            file.seek(position);
            try {
                byte[] fileReader = new byte[1024 * 256]; //4k

                long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;

                inputStream = body.byteStream();

                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }
                    file.write(fileReader, 0, read);

                    fileSizeDownloaded += read;

                    Log.d(TAG, "file download: " + fileSizeDownloaded + " of " + fileSize);
                }

                return true;
            } catch (IOException e) {
                return false;
            } finally {
                file.close();
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        } catch (IOException e) {
            return false;
        }

    }


    private void loadPDFFile(File file) {
        pdfView.fromFile(file)
                .enableSwipe(true)
                .enableDoubletap(true)
                .swipeVertical(true)
                .defaultPage(1)
                .showMinimap(false)
                .load();
    }

    public void showError(Throwable e) {
        e.printStackTrace();
        Toast.makeText(this, "无法打开pdf文件,showErrot",Toast.LENGTH_SHORT).show();
        finish();
    }

    public void showResult(boolean isSuccess) {
        if (isSuccess && null != tempFile && tempFile.exists()) {
            loadPDFFile(tempFile);
        } else {
            Toast.makeText(this, "无法打开pdf文件",Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.btnCheck)
    public void btnCheckSoFile(){
        store = ConstantStore.getConstantStore(this);

        if(ConstantStore.isPdfSoFileDownloaded(this,store)){
            Log.e("info","so文件存在");
            Toast.makeText(this,"soFile存在",Toast.LENGTH_SHORT).show();
        }else{
            Log.e("info","so文件不存在");
            Toast.makeText(this,"soFile不存在",Toast.LENGTH_SHORT).show();
        }
    }
    @OnClick(R.id.btnDeleteSoFile)
    public void btnDeleteSoFile(){
        File jnipdfium = new File(getCacheDir()+File.separator+"libjniPdfium.so");
        File modpdfium = new File(getCacheDir()+File.separator+"libmodpdfium.so");

        jnipdfium.delete();
        modpdfium.delete();
    }

    @OnClick(R.id.btnDeletePdf)
    public void btnDeletePdf(){
        File file = new File(getCacheDir() + File.separator + "pdf_temp.pdf");
        file.delete();
    }


    @OnClick(R.id.btnStart)
    public void btnStart(){
        initSoFileData();
    }


    private void showLoadProgressDialog(){
        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.show();
    }

    @Override
    public void onPageChanged(int page, int pageCount) {
        pageNumber = page;
        setTitle(String.format("%s %s / %s", pdfFileName, page, pageCount));
    }

    @Override
    public void loadComplete(int nbPages) {
        PdfDocument.Meta meta = pdfView.getDocumentMeta();
        printBookmarksTree(pdfView.getTableOfContents(), "-");

        Log.e(TAG, "title = " + meta.getTitle());
        Log.e(TAG, "author = " + meta.getAuthor());
        Log.e(TAG, "subject = " + meta.getSubject());
        Log.e(TAG, "keywords = " + meta.getKeywords());
        Log.e(TAG, "creator = " + meta.getCreator());
        Log.e(TAG, "producer = " + meta.getProducer());
        Log.e(TAG, "creationDate = " + meta.getCreationDate());
        Log.e(TAG, "modDate = " + meta.getModDate());

    }

    public void printBookmarksTree(List<PdfDocument.Bookmark> tree, String sep) {
        for (PdfDocument.Bookmark b : tree) {

            Log.e(TAG, String.format("%s %s, p %d", sep, b.getTitle(), b.getPageIdx()));

            if (b.hasChildren()) {
                printBookmarksTree(b.getChildren(), sep + "-");
            }
        }
    }


}


