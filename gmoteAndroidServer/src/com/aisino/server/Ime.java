/**
 * Copyright (C) 2009 Aisino Corporation Inc.
 *
 * No.18A, Xingshikou street, Haidian District,Beijing
 * All rights reserved.
 * 
 * This software is the confidential and proprietary information of 
 * Aisino Corporation Inc. ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in 
 * accordance with the terms of the license agreement you entered into
 * with Aisino.
 */

package com.aisino.server;

import java.util.List;

import com.android.internal.view.IInputMethodManager;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.PrintStreamPrinter;
import android.util.Printer;
import android.view.inputmethod.InputMethodInfo;

public final class Ime {
  private String IME_ID = "com.aisino.server/.GmoteServer";
  IInputMethodManager mImm;


    private String[] mArgs;
    private int mNextArg;
    private String mCurArgData;

    private static final String IMM_NOT_RUNNING_ERR =
        "Error: Could not access the Input Method Manager.  Is the system running?";

    public void run() {
        mImm = IInputMethodManager.Stub.asInterface(ServiceManager.getService("input_method"));
        if (mImm == null) {
            System.err.println(IMM_NOT_RUNNING_ERR);
            return;
        }
    }


    public void enableAisinoInput(final boolean all) {

              List<InputMethodInfo> methods;
              if (!all) {
                  try {
                      methods = mImm.getEnabledInputMethodList();
                  } catch (RemoteException e) {
                      System.err.println(e.toString());
                      System.err.println(IMM_NOT_RUNNING_ERR);
                      return;
                  }
              } else {
                  try {
                      methods = mImm.getInputMethodList();
                  } catch (RemoteException e) {
                      System.err.println(e.toString());
                      System.err.println(IMM_NOT_RUNNING_ERR);
                      return;
                  }
              }

              if (methods != null) {
                  for (int i=0; i<methods.size(); i++) {
                      InputMethodInfo imi = methods.get(i);
                      String ids = imi.getId();
                      if(ids.contains("aisino")&&all){
                          runSetEnabled(IME_ID, true);
                          runSet(IME_ID);
                          break;
                      }else if(!all){
                          runSetEnabled(IME_ID, false);
                          runSet(ids);
                          break;
                      }
                  }
              }
    }

    private void runSetEnabled(String id,boolean state) {
        if (id == null) {
            System.err.println("Error: no input method ID specified");
            return;
        }

        try {
            boolean res = mImm.setInputMethodEnabled(id, state);
            if (state) {
                System.out.println("Input method " + id + ": "
                        + (res ? "already enabled" : "now enabled"));
            } else {
                System.out.println("Input method " + id + ": "
                        + (res ? "now disabled" : "already disabled"));
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            return;
        } catch (RemoteException e) {
            System.err.println(e.toString());
            System.err.println(IMM_NOT_RUNNING_ERR);
            return;
        }
    }

    private void runSet(String id) {
        if (id == null) {
            System.err.println("Error: no input method ID specified");
            return;
        }

        try {
            mImm.setInputMethod(null, id);
            System.out.println("Input method " + id + " selected");
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            return;
        } catch (RemoteException e) {
            System.err.println(e.toString());
            System.err.println(IMM_NOT_RUNNING_ERR);
            return;
        }
    }



}
