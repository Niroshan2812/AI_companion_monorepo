package com.pm.javagateway.core.inference;

public class InferenceContext {
    private String systemPromptContext;
    private String rlAction;

    public InferenceContext(String systemPromptContext, String rlAction) {
        this.systemPromptContext = systemPromptContext;
        this.rlAction = rlAction;
    }

    public String getSystemPromptContext() {
        return systemPromptContext;
    }

    public String getRlAction() {
        return rlAction;
    }

}
