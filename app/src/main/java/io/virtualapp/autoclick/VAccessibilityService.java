package io.virtualapp.autoclick;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

/**
 * 无障碍操作;
 * 获取需要操作的app相关信息，具体到各个界面布局的节点，
 * 反编译apk后，可获取对应控件的id，根据id查找控件，设置相关事件
 *AccessibilityEvent根据不同过的类型，通过getClassName()会得到不同的类型，
 * 可能是android.widget.Button也可能是对应界面activity的全类名
 *
 */
public class VAccessibilityService extends AccessibilityService {
    private static final String TAG = "auto";
    private static final String TAG1 = "start";
    //响应某个应用的时间，包名为应用的包名；可以用String[]对象传入多包。如果不设置，默认响应所有应用的事件。
    String[] PACKAGES = {"me.kaede.androidmvppattern"};
    // 爱壁纸 首页com.adesk.picasso.view.HomeActivity
    // 登陆界面com.adesk.picasso.view.user.LoginActivity
    // 注册界面com.adesk.picasso.view.user.UserRegisterActivity
    //list-----qwe123

    @Override
    protected void onServiceConnected() {
        Log.i(TAG, "config success!");
        AccessibilityServiceInfo accessibilityServiceInfo = new AccessibilityServiceInfo();
        accessibilityServiceInfo.packageNames = PACKAGES;
        accessibilityServiceInfo.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        accessibilityServiceInfo.feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN;
        accessibilityServiceInfo.notificationTimeout = 1000;
        setServiceInfo(accessibilityServiceInfo);

    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        Log.e(TAG, "------start------"+event.getPackageName()+"----"+event.getClassName().toString()+"----");
        if((event.getPackageName()+"").equals("me.kaede.androidmvppattern")){
//            Intent intent = new Intent();
//            intent.setClassName("me.kaede.androidmvppattern", "me.kaede.mvp.login.LoginActivity");
//            startActivity(intent);
        }
        //当eventType==32（TYPE_WINDOW_STATE_CHANGED）时，event.getClassName()获取当前activity的包名；
        // 当eventType==TYPE_WINDOW_CONTENT_CHANGED时，event.getClassName()获取当前布局的最外层布局
        int eventType = event.getEventType();
        String eventText = "";
        switch (eventType) {
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                eventText = "TYPE_VIEW_CLICKED";
                break;
            case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                eventText = "TYPE_VIEW_FOCUSED";
                break;
            case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED:
                eventText = "TYPE_VIEW_LONG_CLICKED";
                break;
            case AccessibilityEvent.TYPE_VIEW_SELECTED:
                eventText = "TYPE_VIEW_SELECTED";
                break;
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                eventText = "TYPE_VIEW_TEXT_CHANGED";
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                eventText = "TYPE_WINDOW_STATE_CHANGED";
                break;
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                eventText = "TYPE_NOTIFICATION_STATE_CHANGED";
                break;
            case AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_END:
                eventText = "TYPE_TOUCH_EXPLORATION_GESTURE_END";
                break;
            case AccessibilityEvent.TYPE_ANNOUNCEMENT:
                eventText = "TYPE_ANNOUNCEMENT";
                break;
            case AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START:
                eventText = "TYPE_TOUCH_EXPLORATION_GESTURE_START";
                break;
            case AccessibilityEvent.TYPE_VIEW_HOVER_ENTER:
                eventText = "TYPE_VIEW_HOVER_ENTER";
                break;
            case AccessibilityEvent.TYPE_VIEW_HOVER_EXIT:
                eventText = "TYPE_VIEW_HOVER_EXIT";
                break;
            case AccessibilityEvent.TYPE_VIEW_SCROLLED:
                eventText = "TYPE_VIEW_SCROLLED";
                break;
            case AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED:
                eventText = "TYPE_VIEW_TEXT_SELECTION_CHANGED";
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED: //2048
                eventText = "TYPE_WINDOW_CONTENT_CHANGED";
                break;
        }
        eventText = eventText + ":" + eventType;
        Log.e(TAG, eventText);
        Log.e(TAG, "========END=======");

        if(eventType==32){
            if(event!=null){
                Log.e(TAG, "---type-32------"+event.getPackageName()+"----"+event.getClassName().toString()+"----");
            }
            try {
                AccessibilityNodeInfo rootInfo = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                   // rootInfo = getRootInActiveWindow(); //or this is ok 
                    rootInfo = event.getSource();
                }
                if (rootInfo != null) {
                    DFS(rootInfo);
                }
            } catch (Exception e) {
                Log.e("AutoClick", "e is  "+ e.toString());
            }
        }
    }

    @Override
    public void onInterrupt() {
        Log.e(TAG, "onInterrupt" );
    }

    /**
     * 深度优先遍历寻找目标节点
     */
    @SuppressLint("NewApi")
    private void DFS(AccessibilityNodeInfo rootInfo) {
        if (rootInfo == null || TextUtils.isEmpty(rootInfo.getClassName())) {
            return;
        }

//        if (!"android.widget.GridView".equals(rootInfo.getClassName())) {
//            Log.e(TAG,"-1-"+rootInfo.getClassName().toString());
//            if("android.widget.Button".equals(rootInfo.getClassName())){
//                Log.e(TAG,"-4-"+rootInfo.getClassName().toString());
//                final AccessibilityNodeInfo frameLayoutInfo = rootInfo.getChild(0);
//                if(rootInfo.getChild(0).getText().equals("BUTTON1")){
//                    performClick(frameLayoutInfo);
//                    return;
//                }
//            }
//            for (int i = 0; i < rootInfo.getChildCount(); i++) {
//                DFS(rootInfo.getChild(i));
//            }
//        } else {
//            Log.e(TAG,"-2-"+"==find gridView==");
//            final AccessibilityNodeInfo GridViewInfo = rootInfo;
//            for (int i = 0; i < GridViewInfo.getChildCount(); i++) {
//                final AccessibilityNodeInfo frameLayoutInfo = GridViewInfo.getChild(i);
//                //细心的同学会发现，我代码里的遍历的逻辑跟View树里显示的结构不一样，
//                //快照显示的FrameLayout下明明该是LinearLayout，我这里却是TextView，
//                //这个我也不知道，实际调试出来的就是这样……所以大家实操过程中也要注意了
//                final AccessibilityNodeInfo childInfo = frameLayoutInfo.getChild(0);
//                String text = childInfo.getText().toString();
//                if (text.equals("BUTTON1")) {
//                    performClick(frameLayoutInfo);
//                } else {
//                    Log.e(TAG,"-3-"+text);
//                }
//            }
//        }
//--------------------
//        synchronized (rootInfo) {
//            // username
//           List<AccessibilityNodeInfo>   edit_name =  rootInfo.findAccessibilityNodeInfosByViewId("cn.leancloud.rednow:id/username");
//           if(edit_name!=null&&edit_name.size()>0){
//            Log.e(TAG,"find----------------"+edit_name.get(0).getClassName());
//               fill("liuhengpu",edit_name.get(0));
//           }
//            // password
//            List<AccessibilityNodeInfo>   edit_password =  rootInfo.findAccessibilityNodeInfosByViewId("cn.leancloud.rednow:id/password");
//            if(edit_password!=null&&edit_password.size()>0){
//                Log.e(TAG,"find----------------"+edit_password.get(0).getClassName());
//                fill("123456",edit_password.get(0));
//            }
//
//            for (int i = 0; i < rootInfo.getChildCount(); i++) {
//                Log.e(TAG, "-4-" + rootInfo.getChild(i).getClassName());
//                final AccessibilityNodeInfo childInfo_button = rootInfo.getChild(i);
//                String child_text = childInfo_button.getClassName() + "";
//
//                final AccessibilityNodeInfo childInfo_edit = rootInfo.getChild(i);
//                String child_edit = childInfo_button.getClassName() + "";
//                if (("android.widget.EditText").equals(child_edit)) {
//
//                }
//
//                if (("android.widget.Button").equals(child_text)) {
//                    Log.e(TAG, "-5-" + childInfo_button.getText().toString());
//                    if (childInfo_button.getText().toString().equals("BUTTON1")) {
//                        if (fill("i get somethings")) {
//                            performClick(childInfo_button);
//                        }
//
//                    }
//                }
//
//            }
//
//        }

//--------------------
    }

    private void performClick(AccessibilityNodeInfo targetInfo) {
        targetInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        targetInfo.performAction(AccessibilityNodeInfo.ACTION_SELECT);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private boolean fill(String s) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode != null) {
            return findEditText(rootNode, s);
        }
        return false;
    }
    private boolean fill(String s,AccessibilityNodeInfo rootNode) {
        if (rootNode != null) {
            return findEditTextNew(rootNode, s);
        }
        return false;
    }

    private boolean findEditText(AccessibilityNodeInfo rootNode, String content) {
        int count = rootNode.getChildCount();

        Log.d("maptrix", "root class=" + rootNode.getClassName() + ","+ rootNode.getText()+","+count);
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo nodeInfo = rootNode.getChild(i);
            if (nodeInfo == null) {
                Log.d("maptrix", "nodeinfo = null");
                continue;
            }

            Log.d("maptrix", "class=" + nodeInfo.getClassName());
            Log.e("maptrix", "ds=" + nodeInfo.getContentDescription());

            if ("android.widget.EditText".equals(nodeInfo.getClassName())) {
                Log.i("maptrix", "==================");
                Bundle arguments = new Bundle();
                arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                        AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
                arguments.putBoolean(AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN,
                        true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    nodeInfo.performAction(AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY,
                            arguments);
                }
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                ClipData clip = ClipData.newPlainText("label", content);
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clipboardManager.setPrimaryClip(clip);
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE);
                return true;
            }

            if (findEditText(nodeInfo, content)) {
                return true;
            }
        }

        return false;
    }

    private boolean findEditTextNew(AccessibilityNodeInfo rootNode, String content) {

        Log.d("maptrix", "root class=" + rootNode.getClassName() + ","+ rootNode.getText()+",");
            AccessibilityNodeInfo nodeInfo = rootNode;

            Log.d("maptrix", "class=" + nodeInfo.getClassName());
            Log.e("maptrix", "ds=" + nodeInfo.getContentDescription());

            if(rootNode.getText()==null){

            if ("android.widget.EditText".equals(nodeInfo.getClassName())) {
                Log.i("maptrix", "==================");
                Bundle arguments = new Bundle();
                arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                        AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
                arguments.putBoolean(AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN,
                        true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    nodeInfo.performAction(AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY,
                            arguments);
                }
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                ClipData clip = ClipData.newPlainText("label", content);
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clipboardManager.setPrimaryClip(clip);
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE);

                return true;
            }
            }
        return false;

    }


  // 1、获得当前应用包名
    public static String getRunningActivityName(Context context) {
        ActivityManager activityManager=(ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
//完整类名
        String runningActivity=activityManager.getRunningTasks(1).get(0).topActivity.getClassName();
        String contextActivity = runningActivity.substring(runningActivity.lastIndexOf(".")+1);
        return contextActivity;
    }

//2、获得当前activity的名字
    public static String getAppPackageName(Context context){
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> taskInfo = activityManager.getRunningTasks(1);
        ComponentName componentInfo = taskInfo.get(0).topActivity;
        Log.d(TAG, "当前应用:" + componentInfo.getPackageName());
        return componentInfo.getPackageName();
    }

}