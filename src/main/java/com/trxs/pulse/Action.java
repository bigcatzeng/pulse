package com.trxs.pulse;

import com.trxs.pulse.data.TimerMessage;

public class Action extends TimerMessage
{
    private String requestURL;
    private String parameters;

    public String getRequestURL() {
        return requestURL;
    }

    public void setRequestURL(String requestURL) {
        this.requestURL = requestURL;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }
}
