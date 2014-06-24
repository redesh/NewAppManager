package com.peter.appmanager;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.peter.appmanager.R;
import com.peter.appmanager.AppAdapter.AppInfo;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Application;
import android.app.ActivityManager.RecentTaskInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.Toast;

public class AppManager extends Application {
	
	public static final int ALL = 1;
	public static final int SHOW = 2;
	public static final String SETTING = "setting";
	
	@Override
	public void onCreate() {
		super.onCreate();
		Toast.makeText(getBaseContext(), "AppManager onCreate()", Toast.LENGTH_LONG).show();
	}

	
	/**
	 * 获取正在运行的应用信息
	 * 
	 * @return
	 */
	public List<AppInfo> getRunningAppInfos(Context context, int type) {
		ActivityManager mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		PackageManager pm = getPackageManager();
		List<ApplicationInfo> appList = pm.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
		// 正在运行的进程
		List<RunningAppProcessInfo> runningAppProcessInfos = mActivityManager.getRunningAppProcesses();
		// 正在运行的应用
		
		List<AppInfo> runningApps = new ArrayList<AppInfo>(runningAppProcessInfos.size());
		for (RunningAppProcessInfo runningAppInfo : runningAppProcessInfos) {
			ArrayList<ApplicationInfo> infos = getAppInfo(runningAppInfo.pkgList, appList);
			String topPackage = getTopApplication(context);
			for(ApplicationInfo applicationInfo: infos) {
				if(applicationInfo != null && !isSystemApp(applicationInfo) && !applicationInfo.packageName.equals(topPackage)) {
					AppInfo info = new AppInfo();
					info.packageName = applicationInfo.packageName;
					BitmapDrawable bitmapDrawable = (BitmapDrawable) applicationInfo.loadIcon(pm);
					info.appIcon = getRightSizeIcon(bitmapDrawable);
					info.appName = applicationInfo.loadLabel(pm).toString();
					if(type == ALL) {
						if(!containInfo(runningApps, info)) {
							runningApps.add(info);
						}
					}else if(type == SHOW) {
						boolean show = !context.getSharedPreferences(SETTING, MODE_PRIVATE).getBoolean(info.packageName, false);//默认都显示
						if(show) {
							if(!containInfo(runningApps, info)) {
								runningApps.add(info);
							}
						}
					}
				}
			}
		}
		return runningApps;
	}
	

/**
     *判断当前应用程序处于前台还是后台
     */
    public static String getTopApplication(final Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> tasks = am.getRunningTasks(1);
        if (!tasks.isEmpty()) {
            ComponentName topActivity = tasks.get(0).topActivity;
            return topActivity.getPackageName();
        }
        return "";  
     }
	
	private Drawable getRightSizeIcon(BitmapDrawable drawable) {
		Drawable rightDrawable = getResources().getDrawable(R.drawable.session_manager);
		int rightSize = rightDrawable.getIntrinsicWidth();
		Bitmap bitmap = drawable.getBitmap();
		int width = bitmap.getWidth();
		float widths = width;
		float scale = rightSize / widths;
		Matrix matrix = new Matrix();
		matrix.setScale(scale, scale);
		Bitmap bm = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
				bitmap.getHeight(), matrix, true);
		return new BitmapDrawable(getResources(), bm);
	}
	
	private boolean containInfo(List<AppInfo> infos, AppInfo info) {
		for(AppInfo af: infos) {
			if(af.packageName.equals(info.packageName)) {
				return true;
			}
		}
		return false;
	}
	
	public void getRecentTask() {
		ActivityManager mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		List<RecentTaskInfo> task = mActivityManager.getRecentTasks(Integer.MAX_VALUE, ActivityManager.RECENT_IGNORE_UNAVAILABLE);
		for(RecentTaskInfo info : task) {
			Log.i("peter", info.baseIntent.getComponent().getPackageName());
			invokeMethod("removeTask", mActivityManager, new Class[]{int.class, int.class}, new Object[]{info.id, 1});
		}
	}
	
    public void  invokeMethod(String MethodName,Object o, Class<?>[] c, Object []paras){
       try {
           Method method=o.getClass().getDeclaredMethod(MethodName,c);
           try {
               method.invoke(o,paras);//调用o对象的方法
           } catch (IllegalAccessException e) {
        	   e.printStackTrace();
           } catch (IllegalArgumentException e) {
        	   e.printStackTrace();
           } catch (InvocationTargetException e) {
        	   e.printStackTrace();
           }
       } catch (NoSuchMethodException e) {
    	   e.printStackTrace();
       } catch (SecurityException e) {
    	   e.printStackTrace();
       }
   }
	
	private boolean isSystemApp(ApplicationInfo appInfo) {
		if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0 ) {//system apps
			return true;
		}else {
			return false;
		}
	}
	
	/**
	 * 获取应用信息
	 * 
	 * @param name
	 * @return
	 */
	private ArrayList<ApplicationInfo> getAppInfo(String[] pkgList, List<ApplicationInfo> appList) {
        if (pkgList == null) {
            return null;
        }
        
        ArrayList<ApplicationInfo> infos = new ArrayList<ApplicationInfo>(pkgList.length);
        
        for(String pkg : pkgList) {
	        for (ApplicationInfo appinfo : appList) {
	            if (pkg.equals(appinfo.packageName)) {
	                 infos.add(appinfo);
	            }
	        }
        }
        return infos;
    }
	
	public void commandLineForceStop(String packageName) {
		List<String> commands = new ArrayList<String>();
		commands.add("su");
		commands.add("|");
		commands.add("am");
		commands.add("force-stop");
		commands.add(packageName);
		ProcessBuilder pb = new ProcessBuilder(commands);
		try {
			pb.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void commandLineKillAll() {
		List<String> commands = new ArrayList<String>();
		commands.add("su");
		commands.add("|");
		commands.add("am");
		commands.add("kill-all");
		ProcessBuilder pb = new ProcessBuilder(commands);
		try {
			pb.start();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
}
