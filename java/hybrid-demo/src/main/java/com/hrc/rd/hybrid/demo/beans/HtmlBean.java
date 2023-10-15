package com.hrc.rd.hybrid.demo.beans;

import com.hrc.rd.hybrid.demo.util.JavaScriptBridge;
import com.hrc.rd.hybrid.demo.util.JavaScriptCallback;
import com.sun.javafx.webkit.WebConsoleListener;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import oracle.forms.properties.ID;
import oracle.forms.ui.CustomEvent;

public class HtmlBean extends JavaBean implements JavaScriptCallback {
    
    public static final Double ZOOM_FACTOR = 1.1;
    public static final ID DISPLAY_URL = ID.registerProperty("DISPLAY_URL");
    public static final ID SET_PARAMETERS_EVENT = ID.registerProperty("SET_PARAMETERS_EVENT");
    
    public static final ID CLOSE_FORM_EVENT = ID.registerProperty("CLOSE_FORM_EVENT");
    public static final ID CALL_FORM_EVENT = ID.registerProperty("CALL_FORM_EVENT");
    public static final ID CALL_FORM_EVENT_PARAM = ID.registerProperty("CALL_FORM_EVENT_PARAM");
    public static final ID CALL_FORM_FOR_RESULT_EVENT = ID.registerProperty("CALL_FORM_FOR_RESULT_EVENT");
    public static final ID ERROR_EVENT = ID.registerProperty("ERROR_EVENT");
    public static final ID ERROR_EVENT_PARAM = ID.registerProperty("ERROR_EVENT_PARAM");
    public static final ID GET_PARAMETERS_EVENT = ID.registerProperty("GET_PARAMETERS_EVENT");
    public static final ID GET_PARAMETERS_EVENT_PARAM = ID.registerProperty("GET_PARAMETERS_EVENT_PARAM");
    public static final ID CALL_FORMS_PROCEDURE = ID.registerProperty("CALL_FORMS_PROCEDURE");
    public static final ID CALL_FORMS_PROCEDURE_PARAM1 = ID.registerProperty("CALL_FORMS_PROCEDURE_PARAM1");
    public static final ID CALL_FORMS_PROCEDURE_PARAM2 = ID.registerProperty("CALL_FORMS_PROCEDURE_PARAM2");
    
    public static final String FIRST_LOAD_URL = "https://google.com";
    
    public WebEngine htmlEngine;
    private WebView browser;
    private JavaScriptBridge javaScriptBridge;
    
    public HtmlBean() {
        super();
        constructGui();
    }
    
    public final void constructGui() {
        final JFXPanel panel = new JFXPanel();
        Platform.setImplicitExit(false);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                browser = new WebView();
                browser.setPrefSize(50, 50);
                HBox.setHgrow(browser, Priority.ALWAYS);
                VBox.setVgrow(browser, Priority.ALWAYS);
                htmlEngine = browser.getEngine();
                StackPane root = new StackPane(browser);
                Scene scene = new Scene(root);
                panel.setScene(scene);
            }
        });
        add(panel);
    }
    
    @Override
    public boolean setProperty(ID pKey, Object pValue) {
        if (pKey == DISPLAY_URL) {
            System.out.println("DISPLAY_URL:");
            System.out.println(pValue.toString());
            loadUrl(pValue.toString());
            return true;
        }
        if (pKey == SET_PARAMETERS_EVENT) {
            setParametersEvent(pValue.toString());
            return true;
        }
        return super.setProperty(pKey, pValue);
    }
    
    private void loadUrl(final String url) {
        javaScriptBridge = new JavaScriptBridge(this);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                htmlEngine.getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
                    @Override
                    public void changed(ObservableValue<? extends Worker.State> ov, Worker.State t, Worker.State t1) {
                        if (t1 == Worker.State.SUCCEEDED) {
                            JSObject jsobj = (JSObject) htmlEngine.executeScript("window");
                            jsobj.setMember("forms2web", javaScriptBridge);
                        }
                    }
                });

                browser.addEventFilter(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
                                   public void handle(KeyEvent e) {
                                       if ((e.getCode() == KeyCode.ADD || e.getCode() == KeyCode.EQUALS
                                            || e.getCode() == KeyCode.PLUS) && e.isControlDown()) {
                                           zoomIn();
                                       } else if ((e.getCode() == KeyCode.SUBTRACT || e.getCode() == KeyCode.MINUS) && e.isControlDown()) {
                                           zoomOut();
                                       }
                                   }
                               });

                browser.setOnScroll(new EventHandler<ScrollEvent>() {
                    @Override
                    public void handle(ScrollEvent e) {
                        if (e.isControlDown()) {
                            double deltaY = e.getDeltaY();
                            if (deltaY < 0) {
                                zoomOut();
                            } else {
                                zoomIn();
                            }
                        }
                    }
                });

                WebConsoleListener.setDefaultListener((browser, message, lineNumber, sourceId) -> {
                    showError("Received exception " + message + "[at " + lineNumber + "]");
                });
                
                htmlEngine.load(url);
            }
        }
        );
    }
    
    private void zoomIn() {
        browser.setZoom(browser.getZoom() * ZOOM_FACTOR);
    }

    private void zoomOut() {
        browser.setZoom(browser.getZoom() / ZOOM_FACTOR);
    }
    
    public void setParametersEvent(String parameters) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                String tmp = parameters;
                tmp = tmp.replace("\n", "");
                tmp = tmp.replace("\r", "");
                htmlEngine.executeScript("setParametersE([{ name: \"parameters\", value: \"" + tmp + "\"}]);");
            }
        });
    }
    
    @Override
    public void closeForm() {
        CustomEvent cs = new CustomEvent(getHandler(), CLOSE_FORM_EVENT);
        dispatchCustomEvent(cs);
    }

    @Override
    public void callForm(String formName) {
        CustomEvent cs = new CustomEvent(getHandler(), CALL_FORM_EVENT);
        cs.setProperty(CALL_FORM_EVENT_PARAM, formName);
        dispatchCustomEvent(cs);
    }

    @Override
    public void callFormForResult(String formName) {
        CustomEvent cs = new CustomEvent(getHandler(), CALL_FORM_FOR_RESULT_EVENT);
        cs.setProperty(CALL_FORM_EVENT_PARAM, formName);
        dispatchCustomEvent(cs);
    }

    @Override
    public void saveFile(String bytes, String fileName, String extension) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public void showError(String error) {
        CustomEvent cs = new CustomEvent(getHandler(), ERROR_EVENT);
        cs.setProperty(ERROR_EVENT_PARAM, error);
        dispatchCustomEvent(cs);
    }

    @Override
    public void passParameters(String parameters) {
        CustomEvent cs = new CustomEvent(getHandler(), GET_PARAMETERS_EVENT);
        cs.setProperty(GET_PARAMETERS_EVENT_PARAM, parameters);
        dispatchCustomEvent(cs);
    }
    
    @Override
    public void callFormsProcedure(String procedureName, String parametersJson) {
        CustomEvent cs = new CustomEvent(getHandler(), CALL_FORMS_PROCEDURE);
        cs.setProperty(CALL_FORMS_PROCEDURE_PARAM1, procedureName);
        cs.setProperty(CALL_FORMS_PROCEDURE_PARAM2, parametersJson);
        dispatchCustomEvent(cs);
    }
}
