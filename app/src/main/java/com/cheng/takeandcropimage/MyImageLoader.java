package com.cheng.takeandcropimage;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.decode.BaseImageDecoder;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener;
import com.nostra13.universalimageloader.utils.StorageUtils;
import java.io.File;

/**
 * 重构 ImageLoader 类
 */
public class MyImageLoader {

    private MyImageLoader() {

    }

    /**
     * 初始化ImageLoader
     */
    public static void initImageLoader(Context context) {
        File cacheDir = StorageUtils.getCacheDirectory(context);
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
                .threadPoolSize(3)  //线程池内加载的数量
                .threadPriority(Thread.NORM_PRIORITY - 2) // default
                .denyCacheImageMultipleSizesInMemory()
                .diskCacheFileNameGenerator(new Md5FileNameGenerator()) // 文件名生成
                .diskCache(new UnlimitedDiskCache(cacheDir)) // default
                .tasksProcessingOrder(QueueProcessingType.FIFO) // default
                .memoryCache(new LruMemoryCache(5 * 1024 * 1024))
                .memoryCacheSize(5 * 1024 * 1024)
                .memoryCacheSizePercentage(13) // default
                .diskCacheSize(50 * 1024 * 1024)
                .diskCacheFileCount(500)//文件数量
                .imageDownloader(new BaseImageDownloader(context))
//                .imageDownloader(new AuthImageDownloader(context))
                .imageDecoder(new BaseImageDecoder(false)) // default
                .defaultDisplayImageOptions(DisplayImageOptions.createSimple()) // default
                .writeDebugLogs()
                .build();
        ImageLoader.getInstance().init(config);
    }

    /**
     * 获取ImageLoader对象
     *
     * @return
     */
    public static ImageLoader getImageLoder() {
        return ImageLoader.getInstance();
    }

    /**
     * 带有全部参数的 displayImage 方法
     *
     * @param uri
     * @param imageAware
     * @param options
     * @param listener
     * @param progressListener
     */
    public static void displayImage(String uri, ImageAware imageAware, DisplayImageOptions options, ImageLoadingListener listener, ImageLoadingProgressListener progressListener) {
        getImageLoder().displayImage(uri, imageAware, options, listener, progressListener);
    }

    /**
     * 带有全部参数的 displayImage 方法
     *
     * @param uri
     * @param imageView
     * @param options
     * @param listener
     * @param progressListener
     */
    public static void displayImage(String uri, ImageView imageView, DisplayImageOptions options, ImageLoadingListener listener, ImageLoadingProgressListener progressListener) {
        displayImage(uri, new ImageViewAware(imageView), options, listener, progressListener);
    }

    /**
     * 带有初始化设置 和 加载失败时的图片 displayImage
     * 不进行剪切
     *
     * @param uri
     * @param imageView
     */
    public void displayImageNoSplit(String uri, ImageView imageView, int imageOnLoading, int imageOnFail) {
        displayImage(uri, new ImageViewAware(imageView), getOptionWithEmpty(imageOnLoading, imageOnFail), null, null);
    }

    public static int defaultImage = R.mipmap.ic_launcher;

    //不需要参数直接使用
    public static final DisplayImageOptions ImageOptions = new DisplayImageOptions.Builder()
            .showImageOnLoading(defaultImage)
            .showImageForEmptyUri(R.mipmap.ic_launcher)
            .showImageOnFail(R.mipmap.ic_launcher)
            .cacheInMemory(true)
            .cacheOnDisk(true)
            .considerExifParams(true)
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .build();

    //不缓存
    public static DisplayImageOptions getOptionNoCache(int defaultImage) {
        DisplayImageOptions ImageOptions = new DisplayImageOptions.Builder()
                .showImageOnLoading(defaultImage)
                .showImageForEmptyUri(defaultImage)
                .showImageOnFail(defaultImage)
                .cacheInMemory(false)
                .cacheOnDisk(false)
                .considerExifParams(false)
                .bitmapConfig(Bitmap.Config.ARGB_8888)
                .build();
        return ImageOptions;
    }

    //带默认加载图片
    public static DisplayImageOptions getOptionWithLoding(int defaultImage) {
        DisplayImageOptions ImageOptions = new DisplayImageOptions.Builder()
                .showImageOnLoading(defaultImage)
                .showImageForEmptyUri(R.mipmap.ic_launcher)
                .showImageOnFail(R.mipmap.ic_launcher)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .considerExifParams(true)
                .bitmapConfig(Bitmap.Config.ARGB_8888)
                .build();
        return ImageOptions;
    }

    //带默认加载图片和默认失败图片
    public static DisplayImageOptions getOptionWithEmpty(int defaultImage, int imageOnFail) {
        DisplayImageOptions ImageOptions = new DisplayImageOptions.Builder()
                .showImageOnLoading(defaultImage)
                .showImageForEmptyUri(imageOnFail)
                .showImageOnFail(imageOnFail)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .considerExifParams(true)
                .bitmapConfig(Bitmap.Config.ARGB_8888)
                .build();
        return ImageOptions;
    }

    public static DisplayImageOptions getRoundBitmapOption(int defaultImage) {
        DisplayImageOptions ImageOptions = new DisplayImageOptions.Builder()
                .showImageOnLoading(defaultImage)
                .showImageForEmptyUri(defaultImage)
                .showImageOnFail(defaultImage)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .considerExifParams(true)
                .bitmapConfig(Bitmap.Config.ARGB_8888)
                .displayer(new RoundedBitmapDisplayer(8))
                .build();
        return ImageOptions;
    }

}
