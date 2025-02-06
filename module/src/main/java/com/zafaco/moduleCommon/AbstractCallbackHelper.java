/*
 *     Copyright (C) 2016-2025 zafaco GmbH
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License version 3
 *     as published by the Free Software Foundation.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.zafaco.moduleCommon;

import android.content.Context;

import com.zafaco.moduleCommon.interfaces.BaseInterface;
import com.zafaco.moduleCommon.interfaces.GenericInterface;
import com.zafaco.moduleCommon.interfaces.MeasurementListener;
import com.zafaco.moduleCommon.interfaces.ResultInfo;
import com.zafaco.moduleCommon.models.measurement.AbstractMeasurementResult;

import org.json.JSONObject;

public abstract class AbstractCallbackHelper
{

    private static final String TAG = "CALLBACK_HELPER";

    protected       BaseInterface             callbackInterface;
    protected       AbstractMeasurementResult measurementResult;
    protected final Context                   ctx;


    public AbstractCallbackHelper(Context ctx, BaseInterface callbackInterface)
    {
        this(ctx, callbackInterface, null);
    }

    public AbstractCallbackHelper(Context ctx, BaseInterface callbackInterface, AbstractMeasurementResult measurementResult)
    {
        this.ctx               = ctx;
        this.callbackInterface = callbackInterface;
        this.measurementResult = measurementResult;
    }

    protected void publishResults()
    {
        String console = "cmd: " + measurementResult.getCmd() + ", test_case: " + measurementResult.getTestCase() + ", msg: " + measurementResult.getMsg();
        if (callbackInterface instanceof MeasurementListener)
        {
            switch (measurementResult.getCmd())
            {
                case REPORT:
                {
                    ((MeasurementListener) callbackInterface).onReport(measurementResult);
                    break;
                }
                case ERROR:
                {
                    ((MeasurementListener) callbackInterface).onError(measurementResult);
                    break;
                }
                case INFO:
                {
                    ((MeasurementListener) callbackInterface).onInfo(measurementResult);
                    break;
                }
                case STARTED:
                {
                    ((MeasurementListener) callbackInterface).onStarted(measurementResult);
                    break;
                }
                case FINISH:
                {
                    ((MeasurementListener) callbackInterface).onFinished(measurementResult);
                    break;
                }
                case COMPLETED:
                {
                    ((MeasurementListener) callbackInterface).onCompleted(measurementResult);
                    break;
                }

            }
            ((MeasurementListener) callbackInterface).onConsoleMessage(console);
        } else if (callbackInterface instanceof GenericInterface)
        {
            ((GenericInterface) callbackInterface).reportCallback(measurementResult.toJson());
            ((GenericInterface) callbackInterface).consoleCallback(console);
        }
    }

    public void createMessage(JSONObject result)
    {
        measurementResult.addResults(result);
        publishResults();
    }

    public void createMessage(String result)
    {
        measurementResult.addResults(result);
        if (measurementResult.getCmd() == AbstractMeasurementResult.Cmd.ERROR)
        {
            createErrorMessage();
            return;
        } else if (measurementResult.getCmd() == AbstractMeasurementResult.Cmd.COMPLETED)
        {
            createCompletedMessage();
            return;
        } else if (measurementResult.getCmd() == AbstractMeasurementResult.Cmd.STARTED)
        {
            createStartMessage(measurementResult.getTestCase(), measurementResult.getMsg());
            return;
        }
        publishResults();
    }

    public abstract void initData();

    protected void createStartMessage(AbstractMeasurementResult.TestCase testCase, String message)
    {
        measurementResult.setCmd(AbstractMeasurementResult.Cmd.STARTED);
        measurementResult.setMsg(message);
        measurementResult.setTestCase(testCase);
        publishResults();
    }

    public void createInfoMessage(AbstractMeasurementResult.TestCase testCase, String message)
    {
        measurementResult.setCmd(AbstractMeasurementResult.Cmd.INFO);
        measurementResult.setMsg(message);
        measurementResult.setTestCase(testCase);
        publishResults();
    }

    public void createReportMessage(AbstractMeasurementResult.TestCase testCase, ResultInfo resultInfo)
    {
        measurementResult.setCmd(AbstractMeasurementResult.Cmd.REPORT);
        measurementResult.setTestCase(testCase);
        measurementResult.addResult(resultInfo);
        measurementResult.setMsg("measurement report");

        publishResults();
    }

    public void createReportMessage(AbstractMeasurementResult.TestCase testCase, String msg, ResultInfo resultInfo)
    {
        measurementResult.setMsg(msg);
        createReportMessage(testCase, resultInfo);
    }


    public void createFinishMessage(AbstractMeasurementResult.TestCase testCase, String message)
    {
        measurementResult.setCmd(AbstractMeasurementResult.Cmd.FINISH);
        measurementResult.setMsg(message);
        measurementResult.setTestCase(testCase);
        publishResults();
    }

    public void createCompletedMessage()
    {
        measurementResult.setCmd(AbstractMeasurementResult.Cmd.COMPLETED);
        measurementResult.setMsg("all measurements completed");
        publishResults();
    }

    public void createErrorMessage(String msg)
    {
        measurementResult.setCmd(AbstractMeasurementResult.Cmd.ERROR);
        if (msg != null)
            measurementResult.setMsg(msg);
        else
        {
            measurementResult.setMsg("error");
        }
        createErrorMessage();
    }

    public void createErrorMessage()
    {
        publishResults();
    }

    public AbstractMeasurementResult getMeasurementResult()
    {
        return measurementResult;
    }
}
