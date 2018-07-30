package io.appetizer.anti_xposed;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by user on 2018/7/30.
 */

public class antiXposed {
    private static int FIND_XPOSED_FROM_PACKAGES = -1;
    private static int FIND_XPOSED_FROM_STACKTRACE = -2;
    private static int FIND_JAVA_METHOD_NATIVE = -3;
    private static int CHECK_XPOSED_FROM_XPOSEDHELPER_CACHE = -4;
    private static int CHECK_XPOSED_FROM_MAPS = -5;
    private static antiXposed instance = null;
    private Context context = null;

    static {
        System.loadLibrary("native-lib");
    }

    public static antiXposed getInstance() {
        if (instance == null) {
            instance = new antiXposed();
        }
        return instance;
    }

    public void setActivity(Activity activity) {
        if (this.context == null && activity != null) {
            this.context = activity.getApplicationContext();
        }
    }



    /*
    通过PackageManager查看安装列表
     */
    public int checkPackages(){
        PackageManager packageManager=context.getPackageManager();
        List<ApplicationInfo> appliacationInfoList=packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        for(ApplicationInfo item:appliacationInfoList ){
            if(item.packageName.equals("de.robv.android.xposed.installer")) {
                return FIND_XPOSED_FROM_PACKAGES;
            }
        }
        return 0;
    }

    /*
    在异常读取栈里面查找Xposed
     */
    public int checkStacktrace(){
        try {
            throw new Exception("Deteck hook");
        } catch (Exception e) {
            for (StackTraceElement item : e.getStackTrace()) {
                if (item.getClassName().equals("de.robv.android.xposed.XposedBridge")
                        && item.getMethodName().equals("main")) {
                    return FIND_XPOSED_FROM_STACKTRACE;
                }
                if (item.getClassName().equals("de.robv.android.xposed.XposedBridge")
                        && item.getMethodName().equals("handleHookedMethod")) {
                    return FIND_XPOSED_FROM_STACKTRACE;
                }
            }
        }
        return 0;
    }

    /*
    查看java 方法是不是变成 Native,只对Davilk有效，在Art上不改为native，直接运行时改linker
     */
    public int checkMethod(String className, String methodName, Class<?>... parameterTypes){
        try{
            Class clazz = Class.forName(className);
            Method method = clazz.getDeclaredMethod(methodName,parameterTypes);
            method.setAccessible(true);
            if(Modifier.isNative(method.getModifiers())){
                return FIND_JAVA_METHOD_NATIVE;
            }
        }
        catch (ClassNotFoundException e){
            Log.e("ClassNotFoundException","checkMethod");
            e.printStackTrace();
        }
        catch (NoSuchMethodException e){
            Log.e("NoSuchMethodException","checkMethod");
            e.printStackTrace();
        }
        return 0;
    }

    /*
    反射读取XposedHelper类字段
     */
    public int checkCache(String filedName,String sensitiveWord){
        String interName;
        Set keySet;
        try{
            ClassLoader classLoader = ClassLoader.getSystemClassLoader();
            Class clazz = classLoader.loadClass("de.robv.android.xposed.XposedHelpers");
            Constructor con = clazz.getDeclaredConstructor();
            con.setAccessible(true);
            Object object = con.newInstance();
            Field filed = object.getClass().getDeclaredField(filedName);
            filed.setAccessible(true);
            keySet = ((HashMap<String, Object>)filed.get(object)).keySet();
            if (!keySet.isEmpty()) {
                for (Object aKeySet: keySet) {
                    interName = aKeySet.toString().toLowerCase();
                    if(interName.contains(sensitiveWord)){
                        return CHECK_XPOSED_FROM_XPOSEDHELPER_CACHE;
                    }
                }
            }
        }
        catch (ClassNotFoundException e){
            Log.e("ClassNotFoundException","checkCache");
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            Log.e("IllegalAccessException","checkCache");
            e.printStackTrace();
        }
        catch (InstantiationException e) {
            Log.e("InstantiationException","checkCache");
            e.printStackTrace();
        }
        catch (NoSuchFieldException e) {
            Log.e("NoSuchFieldException","checkCache");
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            Log.e("NoSuchMethodException","checkCache");
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            Log.e("InvocationException","checkCache");
            e.printStackTrace();
        }
        return 0;
    }

    /*
    在native层查看 /proc/pid/maps ，看链接库里面有没有XposedBridge.jar
     */
    public native int checkMaps();

    /*
    在java层查看 /proc/pid/maps ，看链接库里面有没有XposedBridge.jar
     */
    public int check_java_maps(){
        try {
            String mapsFilename = "/proc/" + android.os.Process.myPid() + "/maps";
            BufferedReader reader = new BufferedReader(new FileReader(mapsFilename));
            String line;
            while((line = reader.readLine()) != null) {
                if(line.contains("XposedBridge.jar")) return CHECK_XPOSED_FROM_MAPS;
            }
            reader.close();
        }
        catch (Exception e) {
            Log.wtf("HookDetection", e.toString());
        }
        return 0;

    }



}
