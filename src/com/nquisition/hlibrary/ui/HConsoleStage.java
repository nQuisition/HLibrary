/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nquisition.hlibrary.ui;

import com.nquisition.hlibrary.HLibrary;
import javafx.beans.value.ObservableValue;
import javafx.stage.Stage;

/**
 *
 * @author Master
 */
public class HConsoleStage extends Stage
{
    public HConsoleStage()
    {
        this.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> arg0,
             Boolean oldPropertyValue,
             Boolean newPropertyValue) ->
        {
            if (newPropertyValue)
            {
                HLibrary.changeConsoleFocus(this, true);
            }
            else
            {
                //HLibrary.changeConsoleFocus(this, false);
            }
        });
    }
}
