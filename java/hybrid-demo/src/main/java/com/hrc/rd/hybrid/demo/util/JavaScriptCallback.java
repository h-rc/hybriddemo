package com.hrc.rd.hybrid.demo.util;

public interface JavaScriptCallback {
    
    public void closeForm();

    public void callForm(String formName);

    public void callFormForResult(String formName);

    public void saveFile(String bytes, String fileName, String extension);

    public void showError(String error);

    public void passParameters(String parameters);

    public void callFormsProcedure(String procedureName, String parametersJson);
}
