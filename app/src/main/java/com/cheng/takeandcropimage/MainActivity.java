package com.cheng.takeandcropimage;

import android.Manifest;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyImageLoader.initImageLoader(this);
        setContentView(R.layout.activity_main);
        findViewById(R.id.take).setOnClickListener(this);
        findViewById(R.id.choice).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.take:
                takePhoto();
                break;
            case R.id.choice:
                choicePhoto();
                break;
        }
    }

    private String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private Uri takeImageUri;
    private Uri cropImageUri;

    private void takePhoto() {
        if (!UserPermissionActivity.checkPermission(this, permissions[0]) || !UserPermissionActivity.checkPermission(this, permissions[1])) {
            Intent intent = new Intent(this, UserPermissionActivity.class);
            intent.putExtra("permission", permissions);
            startActivityForResult(intent, 400);
        } else {
            try {
                if(!haveSDCard()){
                    Toast.makeText(this, "没有找到内存卡", Toast.LENGTH_SHORT).show();
                    return;
                }
                File takeImage = new File(ROOTPATH, "take_image.jpg");
                if (takeImage.exists()) takeImage.delete();
                takeImage.createNewFile();
                if (Build.VERSION.SDK_INT >= 24) {/**Android 7.0以上的方式**/
                    String authority = getPackageName() + ".fileProvider";
                    takeImageUri = android.support.v4.content.FileProvider.getUriForFile(this, authority, takeImage);
                } else {/**Android 7.0以前的方式**/
                    takeImageUri = Uri.fromFile(takeImage);
                }
                if(takeImageUri==null){
                    Toast.makeText(this, "获取拍照路径失败", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, takeImageUri);
                startActivityForResult(intent, 100);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "请检查权限是否开启，存储是否正常", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void choicePhoto() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(intent, 200);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 100 && resultCode == RESULT_OK) {
            startPhotoZoom(takeImageUri);
        } else if (requestCode == 200) {
            if(data != null){//判断手机系统版本号
                if(Build.VERSION.SDK_INT>=19){//4.4及以上系统使用这个方法处理图片
                    Uri uri = handlerImageOnKitKat(data);
                    startPhotoZoom(uri);
                }else{//4.4以下系统直接使用
                    startPhotoZoom(data.getData());
                }
            }else{
                Toast.makeText(this, "获取图片失败", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 300) {
            if (data != null) {
                //如果 return-data 参数为 true 则直接获取 bitmap
//                Bundle extras = data.getExtras()
//                if (extras != null) {
//                    Bitmap photo = extras.getParcelable<Bitmap>("data")
//                    Bitmap bitmap = compressImage(photo);
//                }
                try {
                    //获取裁剪后的图片
                    Bitmap photo = BitmapFactory.decodeStream(getContentResolver().openInputStream(cropImageUri));
                    //获取压缩后的图片
//                    Bitmap bitmap = compressImage(photo);
//                    String btb = bitmapToBase64(bitmap);
                    ((ImageView)findViewById(R.id.image_show)).setImageBitmap(photo);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } else if (requestCode == 400 && resultCode == RESULT_OK) {
            takePhoto();
        }
    }

    private Uri handlerImageOnKitKat(Intent data){
        String imagePath = "";
        Uri uri = data.getData();
        if (DocumentsContract.isDocumentUri(this, uri)) {
            //如果是document类型的Uri,则通过document id处理
            String docId = DocumentsContract.getDocumentId (uri);
            if ("com.android.providers.media.documents" == uri.getAuthority()) {
                String id = docId.split (":")[1];//解析出数字格式的id
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents" == uri.getAuthority()) {
                Uri contentUri = ContentUris.withAppendedId (Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            //如果是content类型的URI，则使用普通方式处理
            imagePath = getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            //如果是file类型的Uri,直接获取图片路径即可
            imagePath = uri.getPath();
        }
        if(imagePath==null || "".equals(imagePath)){
            return uri;
        }else{
            return Uri.fromFile(new File(imagePath));
        }
    }

    private String getImagePath(Uri uri, String selection){
        String path = "";
        //通过Uri和selection来获取真实的图片路径
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    /** 裁剪图片
     附加选项	数据类型	描述
     crop	String	发送裁剪信号
     aspectX	int	X方向上的比例
     aspectY	int	Y方向上的比例
     outputX	int	裁剪区的宽
     outputY	int	裁剪区的高
     scale	boolean	是否保留比例
     return-data	boolean	是否将数据保留在Bitmap中返回
     data	Parcelable	相应的Bitmap数据
     circleCrop	String	圆形裁剪区域？
     MediaStore.EXTRA_OUTPUT ("output")	URI	将URI指向相应的file:///...
     */
    private void startPhotoZoom(Uri uri) {
        File cropImage = new File(ROOTPATH, "crop_image.jpg");
        try {
            if (cropImage.exists()) cropImage.delete();
            cropImage.createNewFile();
            cropImageUri = Uri.fromFile(cropImage);
            if(cropImageUri==null){
                Toast.makeText(this, "获取图片路径失败", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent("com.android.camera.action.CROP");
            intent.setDataAndType(uri, "image/*");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {//添加这一句表示对目标应用临时授权该Uri所代表的文件
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            intent.putExtra("crop", "true");//设置在开启的Intent中设置显示的VIEW可裁剪
            intent.putExtra("scale", true);
            intent.putExtra("aspectX", 1);
            intent.putExtra("aspectY", 1);
            intent.putExtra("outputX", 300);
            intent.putExtra("outputY", 300);
            //        intent.putExtra("return-data", true)
            intent.putExtra("return-data", false);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cropImageUri);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
            intent.putExtra("noFaceDetection", true);
            startActivityForResult(intent, 300);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 质量压缩方法
     * @param image
     * @return
     */
    private Bitmap compressImage(Bitmap image) {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, bao);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        int options = 100;
        while (bao.toByteArray().length / 1024 > 100) {  //循环判断如果压缩后图片是否大于100kb,大于继续压缩
            bao.reset();//重置baos即清空baos
            //第一个参数 ：图片格式 ，第二个参数： 图片质量，100为最高，0为最差  ，第三个参数：保存压缩后的数据的流
            image.compress(Bitmap.CompressFormat.JPEG, options, bao);//这里压缩options%，把压缩后的数据存放到baos中
            options -= 10;//每次都减少10
        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(bao.toByteArray());//把压缩后的数据baos存放到ByteArrayInputStream中
        return BitmapFactory.decodeStream(isBm, null, null);
    }

    /**
     * @Title: bitmapToBase64
     * @Description: TODO(Bitmap 转换为字符串)
     * @param @param bitmap
     * @return String    返回类型
     */
    private String bitmapToBase64(Bitmap bitmap) {
        String result = "";// 要返回的字符串
        ByteArrayOutputStream baos = null;
        try {
            if (bitmap != null) {
                baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                baos.flush();
                baos.close();
                // 转换为字节数组
                byte[] byteArray = baos.toByteArray();
                // 转换为字符串
                result = Base64.encodeToString(byteArray, Base64.DEFAULT);
            } else {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return result;
    }

    /**
     * SD卡路径
     */
    public String ROOTPATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";

    /**
     * 判断SD是否存在
     * @return boolean
     */
    public boolean haveSDCard(){
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)){
            return true;
        }
        return false;
    }

}
