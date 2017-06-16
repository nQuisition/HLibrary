/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nquisition.hlibrary.console;

import com.nquisition.hlibrary.ui.GalleryViewer;
import com.nquisition.hlibrary.ui.HConsoleStage;
import java.util.List;

/**
 *
 * @author Master
 */
public class HCommandGW extends HCommand
{
    public HCommandGW()
    {
        super("gw");
    }
    
    @Override
    public int execute(HConsole console, List<String> params)
    {
        HConsoleStage stage = console.getFocus();
        if(stage == null || !(stage instanceof GalleryViewer))
            return INVALID_TARGET;
        if(params == null || params.size() < 1)
            return INVALID_PARAMETER;
        GalleryViewer focus = (GalleryViewer)stage;
        if(params.get(0).equalsIgnoreCase("next"))
            focus.nextImage();
        return STATUS_OK;
    }
    
}
