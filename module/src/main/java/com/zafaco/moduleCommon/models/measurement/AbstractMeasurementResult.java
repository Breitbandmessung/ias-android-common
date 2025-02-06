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

package com.zafaco.moduleCommon.models.measurement;


import android.content.Context;

import com.zafaco.moduleCommon.interfaces.ResultInfo;

import org.json.JSONObject;

public abstract class AbstractMeasurementResult implements ResultInfo
{
    private static final String TAG = "AbstractMeasurementResult";

    protected final Context  ctx;
    protected       Cmd      cmd;
    protected       String   msg;
    protected       TestCase testCase;

    public AbstractMeasurementResult()
    {
        this(null);
    }

    public AbstractMeasurementResult(Context ctx)
    {
        this.ctx = ctx;
    }

    public Cmd getCmd()
    {
        return cmd;
    }

    public void setCmd(Cmd cmd)
    {
        this.cmd = cmd;
    }

    public String getMsg()
    {
        return msg;
    }

    public void setMsg(String msg)
    {
        this.msg = msg;
    }

    public TestCase getTestCase()
    {
        return testCase;
    }

    public void setTestCase(TestCase testCase)
    {
        this.testCase = testCase;
    }

    public abstract void addResults(String result);

    public abstract void addResults(JSONObject jsonObject);

    public abstract void addResult(ResultInfo resultInfo);

    public abstract void initMiscData();

    public enum TestCase
    {
        IP, INIT, RTT_UDP, DOWNLOAD, UPLOAD, TRACE_IAS, END
    }

    public enum Cmd
    {
        REPORT, ERROR, INFO, STARTED, FINISH, COMPLETED
    }

}
