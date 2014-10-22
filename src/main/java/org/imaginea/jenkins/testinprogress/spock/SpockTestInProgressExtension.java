package org.imaginea.jenkins.testinprogress.spock;

import org.jenkinsci.testinprogress.messagesender.IMessageSenderFactory;
import org.jenkinsci.testinprogress.messagesender.MessageSender;
import org.jenkinsci.testinprogress.messagesender.SocketMessageSenderFactory;
import org.spockframework.runtime.AbstractRunListener;
import org.spockframework.runtime.extension.IGlobalExtension;
import org.spockframework.runtime.model.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Created by varunm on 11/3/14.
 */

public class SpockTestInProgressExtension extends AbstractRunListener implements IGlobalExtension {

    private static MessageSender messageSender;
    private static IMessageSenderFactory messageSenderFactory = new SocketMessageSenderFactory();
    private Map<String, String> testIds = new HashMap<String, String>();
    private AtomicLong atomicLong = new AtomicLong(0);
    private long startTime;
    private static boolean initialized = false;
    private static List<SpecInfo> specInfos = new ArrayList<SpecInfo>();
    private static int totalTests = 0;
    private String suiteId = "";
    private String suiteName = "Test Suite";
    private boolean iterationFailed = false;
    private String iterationTrace ="";
    private static boolean pluginEnabled = true;

    @Override
    public void visitSpec(SpecInfo specInfo){
        try {
            pluginEnabled = Boolean.parseBoolean(System.getProperty("testinprogress.enabled", "true"));

            init();
            totalTests += specInfo.getFeatures().size();
            specInfo.addListener(this);
            sendTestTree(specInfo);
        } catch (Exception e) {

        }
    }

    private void init(){
        if(!initialized && pluginEnabled){
            messageSender = messageSenderFactory.getMessageSender();
            try{
                messageSender.init();
            }catch(Exception e){
                e.printStackTrace();
            }
            messageSender.testRunStarted(0);
            String id = addTagAndGetTestId(this.suiteName);
            messageSender.testTree(id, this.suiteName, true, 100);
            this.suiteId = id;
            initialized = true;
        }
    }

    public void beforeSpec(SpecInfo spec) {

    }

    private void sendTestTree(NodeInfo nodeInfo) {
        String id = getTestId(nodeInfo);
        String name = "";
        String parentName = "";
        String parentId = "";
        boolean isClass = false;
        int childrenCount = 1;
        if(SpecInfo.class.isAssignableFrom(nodeInfo.getClass())){
            isClass = true;
            childrenCount = ((SpecInfo)nodeInfo).getFeatures().size();
            name = ((SpecInfo) nodeInfo).getReflection().getName();
            parentId = this.suiteId;
            parentName = this.suiteName;
        } else if(FeatureInfo.class.isAssignableFrom(nodeInfo.getClass())){
            name = getTestMethodName(nodeInfo);
            parentName = ((FeatureInfo)nodeInfo).getFeatureMethod().getReflection().getDeclaringClass().getName();
            parentId = getTestId(((FeatureInfo)nodeInfo).getParent());
        } else if(IterationInfo.class.isAssignableFrom(nodeInfo.getClass())){
            name = getTestMethodName(nodeInfo);
            parentName = ((IterationInfo)nodeInfo).getParent().getFeatureMethod().getReflection().getDeclaringClass().getName();
            parentId = getTestId(((IterationInfo)nodeInfo).getParent());
        } else if(MethodInfo.class.isAssignableFrom(nodeInfo.getClass())){
            name = getTestMethodName(nodeInfo);
            parentName = ((MethodInfo)nodeInfo).getReflection().getDeclaringClass().getName();
            parentId = getTestId(((MethodInfo)nodeInfo).getParent());
        }


        messageSender.testTree(id, name, parentId, parentName, isClass, childrenCount);
        /*System.out.println("Test tree sent: "+id+"-"+name+"-"+isClass+"-"+childrenCount);*/
        if(SpecInfo.class.isAssignableFrom(nodeInfo.getClass())){
            for (FeatureInfo feature : ((SpecInfo)nodeInfo).getFeatures()){
                sendTestTree(feature);
            }
        }
    }

    private String getTestMethodName(NodeInfo nodeInfo){
        FeatureInfo featureInfo=null;
        String methodName = "";
        MethodInfo methodInfo=null;
        if(FeatureInfo.class.isAssignableFrom(nodeInfo.getClass())){
            featureInfo = (FeatureInfo)nodeInfo;
            methodInfo = featureInfo.getFeatureMethod();

        } else if(IterationInfo.class.isAssignableFrom(nodeInfo.getClass())){
            featureInfo = ((IterationInfo)nodeInfo).getParent();
            methodName = ((IterationInfo)nodeInfo).getName();
            methodInfo = featureInfo.getFeatureMethod();

        } else if(MethodInfo.class.isAssignableFrom(nodeInfo.getClass())){
            methodInfo = (MethodInfo)nodeInfo;
            methodName = ((MethodInfo)nodeInfo).getName();
        }

        methodName = (methodName.contentEquals("")) ? methodInfo.getName() : methodName;
        String className = methodInfo.getReflection().getDeclaringClass().getName();
        String name = methodName +"("+className+")";
        return name;
    }

    private synchronized String getTestId(NodeInfo nodeInfo) {
        String key = "";
        if(SpecInfo.class.isAssignableFrom(nodeInfo.getClass())){
            key = ((SpecInfo)nodeInfo).getName();
        }else if(FeatureInfo.class.isAssignableFrom(nodeInfo.getClass())){
            String className = ((FeatureInfo)nodeInfo).getFeatureMethod().getReflection().getDeclaringClass().getName();
            String methodName = ((FeatureInfo)nodeInfo).getFeatureMethod().getName();
            key = className+"-"+methodName;
        } else if(IterationInfo.class.isAssignableFrom(nodeInfo.getClass())){
            String className = ((IterationInfo)nodeInfo).getParent().getFeatureMethod().getReflection().getDeclaringClass().getName();
            String methodName = ((IterationInfo)nodeInfo).getName();
            key = className+"-"+methodName;
        } else if(MethodInfo.class.isAssignableFrom(nodeInfo.getClass())){
            String className = ((MethodInfo)nodeInfo).getReflection().getDeclaringClass().getName();
            String methodName = ((MethodInfo)nodeInfo).getName();
            key = className+"-"+methodName;
        }
        /*System.out.println("Key is:-"+key);*/
        String test = testIds.get(key);
        if (test == null) {
            test = Long.toString(atomicLong.incrementAndGet());
            testIds.put(key, test);
        }
        return test;
    }

    private synchronized String addTagAndGetTestId(String key){
        String test = Long.toString(atomicLong.incrementAndGet());
        testIds.put(key, test);
        return test;
    }

    public void beforeFeature(FeatureInfo feature) {
        if (pluginEnabled) {
            String name = getTestMethodName(feature);
            if (!(feature.isParameterized() && feature.isReportIterations())) {
                String id = getTestId(feature);
                messageSender.testStarted(id, name, false);
            }
        }
    }
    public void beforeIteration(IterationInfo iteration) {
        if (pluginEnabled) {
            FeatureInfo feature = iteration.getParent();
            if ((feature.isParameterized() && feature.isReportIterations())) {
                String name = getTestMethodName(iteration);
                String id = getTestId(iteration);
                String parentId = getTestId(iteration.getParent());
                String parentName = getTestMethodName(iteration.getParent());
                messageSender.testTree(id, name, parentId, parentName, false, 1);
                messageSender.testStarted(id, name, false);
            }
        }
    }

    public void afterIteration(IterationInfo iteration) {
        if (pluginEnabled) {
            FeatureInfo feature = iteration.getParent();
            String name = getTestMethodName(iteration);
            String id = getTestId(iteration);
            if ((feature.isParameterized() && feature.isReportIterations())) {
                if (iterationFailed) {
                    messageSender.testError(id, name, iterationTrace);
                    iterationFailed = false;
                    iterationTrace = "";
                }
                messageSender.testEnded(id, name, false);
            }
        }
    }

    public void afterFeature(FeatureInfo feature) {
        if (pluginEnabled) {
            String name = getTestMethodName(feature);
            String id = getTestId(feature);
            messageSender.testEnded(id, name, false);
        }
    }

    public void error(ErrorInfo error) {
        if (pluginEnabled) {
            boolean sendError = true;
            NodeInfo nodeInfo;
            String trace = getTrace(error.getException());
            MethodInfo methodInfo = error.getMethod();
            MethodKind kind = methodInfo.getKind();
            if (kind.isFixtureMethod()){
                sendTestTree(methodInfo);
                nodeInfo = methodInfo;
                String name = getTestMethodName(nodeInfo);
                String id = getTestId(nodeInfo);
                messageSender.testStarted(id, name, false);
            } else {
                FeatureInfo feature = error.getMethod().getFeature();
                nodeInfo = feature;
                if (feature.isParameterized() && feature.isReportIterations()) {
                    sendError = false;
                    iterationFailed = true;
                    iterationTrace = trace;
                }
            }
            String name = getTestMethodName(nodeInfo);
            String id = getTestId(nodeInfo);

             if (sendError) {
                messageSender.testError(id, name, trace);
            }
        }
    }

    private String getTrace(Throwable e){
        StringWriter stringWriter= new StringWriter();
        PrintWriter writer= new PrintWriter(stringWriter);
        e.printStackTrace(writer);
        StringBuffer buffer= stringWriter.getBuffer();
        return buffer.toString();
    }



    public void specSkipped(SpecInfo spec) {
        if (pluginEnabled) {
            init();

            for (FeatureInfo feature : spec.getFeatures()) {
                featureSkipped(feature);
            }
        }
    }

    public void featureSkipped(FeatureInfo feature) {
        if (pluginEnabled) {
            String name = getTestMethodName(feature);
            String id = getTestId(feature);
            messageSender.testStarted(id, name, true);
            messageSender.testEnded(id, name, true);
        }
    }
}

