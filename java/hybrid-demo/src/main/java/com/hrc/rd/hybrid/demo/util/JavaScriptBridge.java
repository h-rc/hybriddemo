package com.hrc.rd.hybrid.demo.util;

public class JavaScriptBridge {
    
    private final JavaScriptCallback callback;
    
    public JavaScriptBridge(JavaScriptCallback callback) {
        this.callback = callback;
    }
    
    public void logMsg(String msg) {
        System.out.println("JSBridge MSG LOG:");
        System.out.println(msg);
    }
    
    public void callForm(String formName) {
        callback.callForm(formName);
    }

    public void callFormForResult(String formName) {
        callback.callFormForResult(formName);
    }

    public void closeForm() {
        callback.closeForm();
    }

    public void saveFile(String bytes, String fileName, String extension) {
        callback.saveFile(bytes, fileName, extension);
    }

    public void showError(String error) {
        callback.showError(error);
    }

    public void passParameters(String parameters) {
        callback.passParameters(parameters);
    }

    public void callFormsProcedure(String procedureName, String parametersJson) {
        callback.callFormsProcedure(procedureName, parametersJson);
    }
}
