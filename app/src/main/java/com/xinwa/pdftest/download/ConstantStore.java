package com.xinwa.pdftest.download;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;

import okhttp3.ResponseBody;

/**
 * Created by xinwa on 2016/07/20.
 */
public class ConstantStore {
    private Context context;
    private SharedPreferences pm;
    private SharedPreferences.Editor editor;

    private ConstantStore(Context context){
        this.context = context;
        pm = context.getSharedPreferences("pdf_data",Context.MODE_PRIVATE);
        editor = pm.edit();
    }

    public static ConstantStore getConstantStore(Context context){

        return new ConstantStore(context);
    }

    public void putLong(String name,Long value){
        editor.putLong(name,value);
        editor.commit();
    }

    public void saveSoFileStartPosition(String fileName,Long value){
        editor.putLong(fileName,value);
        editor.commit();
    }

    public void saveSoFileSize(String fileName, ResponseBody body){
        editor.putLong(fileName+"_size",body.contentLength());
        editor.commit();
    }

    public Long getStartPosition(String fileName){
       return pm.getLong(fileName,0);
    }

    public Long getSoFileSize(String fileName){
        return pm.getLong(fileName+".so_size",0);
    }

    //是否pdf文件下载完整
    public static boolean isPdfSoFileDownloaded(Context context,ConstantStore store){
        File jnipdfium = new File(context.getCacheDir()+File.separator+"libjniPdfium.so");
        File modpdfium = new File(context.getCacheDir()+File.separator+"libmodpdfium.so");

        if(!jnipdfium.exists() || !modpdfium.exists()){
            return false;
        }else{

//            ConstantStore store = ConstantStore.getConstantStore(context);
            long jnipdfium_size = store.getSoFileSize("libjniPdfium");
            long modpdfium_size = store.getSoFileSize("libmodpdfium");
//            Log.e("jni",jnipdfium_size+"");
//            Log.e("mod",modpdfium_size+"");
//            Log.e("jniLength",jnipdfium.length()+"");
//            Log.e("modLebgth",modpdfium.length()+"");
            if(jnipdfium.length() < jnipdfium_size || modpdfium.length() <modpdfium_size){
                return false;
            }
            return true;
        }
    }

    //根据已经下载文件的大小来给startPosition赋值
    public  long getSoFileStartPosition(String fileName){
        File file = new File(context.getCacheDir()+File.separator+fileName);
        return file.length();
    }
}
