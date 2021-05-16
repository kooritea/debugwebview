package moe.kooritea.debugwebview;

import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private String loadVConsole = "function loadJS(url, callback) {\n" +
            "  var script = document.createElement(\"script\"),\n" +
            "  fn = callback || function () {};\n" +
            "  script.type = \"text/javascript\";\n" +
            "  if (script.readyState) {\n" +
            "    script.onreadystatechange = function () {\n" +
            "      if (script.readyState == \"loaded\" || script.readyState == \"complete\") {\n" +
            "        script.onreadystatechange = null;\n" +
            "        fn();\n" +
            "      }\n" +
            "    };\n" +
            "  } else {\n" +
            "    script.onload = function () {\n" +
            "      fn();\n" +
            "    };\n" +
            "  }\n" +
            "  script.src = url;\n" +
            "  document.getElementsByTagName(\"head\")[0].appendChild(script);\n" +
            "}\n" +
            "loadJS(\"https://unpkg.com/vconsole/dist/vconsole.min.js\",function(){\n" +
            "  new VConsole();\n" +
            "})";

    private HashSet<String> classNameSet = new HashSet<>();
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(lpparam.isFirstApplication){
            if(hookCheck(lpparam.packageName)){
                this.hookSystemWebView(lpparam);
            }
        }
    }

    private void hookSystemWebView(final XC_LoadPackage.LoadPackageParam lpparam){
        try{
            final Class<?> webViewClazz = XposedHelpers.findClass("android.webkit.WebView",lpparam.classLoader);
            XposedBridge.hookAllConstructors(webViewClazz, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    XposedHelpers.callStaticMethod(webViewClazz, "setWebContentsDebuggingEnabled", true);
                    final WebView webview = (WebView)param.thisObject;
                    WebSettings webSettings = webview.getSettings();
                    webSettings.setJavaScriptEnabled(true);
                    if(hookCheck(webSettings.getClass().getName())){
                        XposedBridge.hookMethod(findMethod(webSettings.getClass(),"setJavaScriptEnabled"),new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                param.args[0] = true;
                            }
                        });
                    }
                }
            });
            XposedBridge.hookMethod(findMethod(webViewClazz,"setWebContentsDebuggingEnabled"),new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.args[0] = true;
                }
            });
            XposedBridge.log("[debug webview] setWebContentsDebuggingEnabled");
            XposedBridge.hookMethod(findMethod(webViewClazz,"setWebViewClient"),new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if(param.args[0] != null && hookCheck(param.args[0].getClass().getName())){
                        XposedBridge.hookMethod(findMethod(param.args[0].getClass(),"onPageFinished"),new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                final WebView webview = (WebView)param.args[0];
                                webview.evaluateJavascript("javascript:"+loadVConsole,new ValueCallback<String>() {
                                    @Override
                                    public void onReceiveValue(String value) {
                                        XposedBridge.log("[debug webview] inject vconsole");
                                    }
                                });
                            }
                        });
                    }
                }
            });
        }catch (Exception e){
            XposedBridge.log(e.getMessage());
        }
    }

    private Method findMethod(Class<?> clazz,String name){
        for(Method method : clazz.getMethods()){
            if(name.equals(method.getName())){
                return method;
            }
        }
        return null;
    }

    /**
     * 检查该类是否已经hook过，未hook的返回true
     * @param className
     * @return
     */
    private boolean hookCheck(String className){
        if(classNameSet.contains(className)){
            return false;
        }else{
            classNameSet.add(className);
            return true;
        }
    }
}
