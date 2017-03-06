package com.spectralogic.dsbrowser.gui.util;

import javafx.stage.Stage;

//Singleton Model class for getting Primary Stage
public class PrimaryStageModel {
    private static PrimaryStageModel stageInstance;
    private Stage primaryStage;

    private PrimaryStageModel() {
    }

    public static PrimaryStageModel getInstance() {
        if(stageInstance == null) {
            stageInstance = new PrimaryStageModel();
        }
        return stageInstance;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void setPrimaryStage(final Stage primaryStage) {
        this.primaryStage = primaryStage;
    }
}
