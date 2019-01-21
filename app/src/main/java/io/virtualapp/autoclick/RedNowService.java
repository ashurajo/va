package io.virtualapp.autoclick;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class RedNowService extends AccessibilityService {
    private static final String TAG = "RedNowService";


    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {


        android.util.Log.e(TAG, "--------------start---------------------");
        int eventType = event.getEventType();
        android.util.Log.e(TAG, "get event = " + eventType);
        if(eventType==32){
            try {
                AccessibilityNodeInfo rootInfo = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    rootInfo = getRootInActiveWindow();
                }
                if (rootInfo != null) {
                    DFS(rootInfo);
                }
            } catch (Exception e) {
                Log.e("Exception:" + e.getMessage(), "true");
            }
        }

    }

    @Override
    public void onInterrupt() {

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

        final AccessibilityNodeInfo frameLayout = rootInfo;

        for (int i = 0; i < frameLayout.getChildCount(); i++) {
            Log.e(TAG,"-4-"+frameLayout.getChild(i).getClassName());
        }
    }

    private void performClick(AccessibilityNodeInfo targetInfo) {
        targetInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        targetInfo.performAction(AccessibilityNodeInfo.ACTION_SELECT);
        targetInfo.setText("zhaodaole");
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private boolean fill(String s) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode != null) {
            return findEditText(rootNode, s);
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
}