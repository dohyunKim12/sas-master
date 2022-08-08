package com.tmax.superobject.object;

import com.google.gson.Gson;
import com.tmax.superobject.annotation.ServiceParam;
import com.tmax.superobject.annotation.ServiceProperty;
import com.tmax.superobject.config.Global;
import com.tmax.superobject.listener.ChannelCompletionListener;
import com.tmax.superobject.listener.CompletionListener;
import com.tmax.superobject.logger.SuperAppDefaultLogger;
import com.tmax.superobject.servicemanager.ServiceManager;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.Pipe.SinkChannel;
import java.nio.channels.Pipe.SourceChannel;
import java.util.*;

public class TaskObject implements Runnable {
    public static enum ProcessContext {
        CONTROLLER, WORKER
    }

    public static enum ProcessState {
        BEFORE_SERVICE,
        AFTER_SERVICE,
        COMPLETE_SERVICE,
        WAIT,
        BEFORE_LISTENER,
        AFTER_LISTENER,
        TERMINATE
    }

    private static Logger logger = SuperAppDefaultLogger.getInstance().getLogger(TaskObject.class.getName());
    protected MessageObject requestMessage;
    protected MessageObject responseMessage;
    protected Class<? extends BodyObject> inDtoClass = null;
    protected Class<? extends BodyObject> outDtoClass = null;
    protected ClassLoader serviceClassLoader;
    protected EventLoopGroup eventManager;
    protected EventLoopGroup workerGroup;
    protected ServiceObject callee;
    protected List<CompletionListener> completionListeners = new ArrayList<>();
    protected TaskObject parentTask = null;
    protected int readyToComputeCount = 0;
    protected ProcessState state = ProcessState.BEFORE_SERVICE;
    protected ProcessContext context = ProcessContext.CONTROLLER;
    protected Pipe pipe = null;
    protected Channel inboundChannel = null;
    protected SourceChannel sourceChannel = null;
    protected SinkChannel sinkChannel = null;
    protected static final Map<String, LinkedList<TaskObject>> threadScheduler = new HashMap<>();
    protected static final Map<String, Integer> workingCount = new HashMap<>();

    public MessageObject getRequestMessage() {
        return requestMessage;
    }

    public Channel getInboundChannel() {
        return inboundChannel;
    }

    public void setInboundChannel(Channel inboundChannel) {
        this.inboundChannel = inboundChannel;
    }

    public void setState(ProcessState state) {
        this.state = state;
    }

    public int increaseComputeReady() {
        return ++readyToComputeCount;
    }

    public int decreaseComputeReady() {
        return --readyToComputeCount;
    }

    public TaskObject getParentTask() {
        return parentTask;
    }

    public void setParentTask(TaskObject parentTask) {
        this.parentTask = parentTask;
        if (this.parentTask != null)
            parentTask.increaseComputeReady();
    }

    public void handleOutboundResponse(BodyObject body) {
        callee.service(body);
    }

    public TaskObject(ServiceObject serviceObject, MessageObject requestMessage, ClassLoader serviceClassLoader,
                      EventLoopGroup eventManager, EventLoopGroup workerGroup) {
        this.callee = serviceObject;
        this.requestMessage = requestMessage;
        this.serviceClassLoader = serviceClassLoader;
        this.eventManager = eventManager;
        this.workerGroup = workerGroup;
        try {
            this.pipe = Pipe.open();
            this.sinkChannel = pipe.sink();
            this.sourceChannel = pipe.source();
            sourceChannel.configureBlocking(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static TaskObject newInstanceFromMessageObject(MessageObject messageObject)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
            NoSuchMethodException, SecurityException {
        TaskObject taskObject = null;
        try {
            Class<?> clazz = Global.getInstance().getServiceClassLoader()
                    .loadClass(messageObject.header().targetServiceName());
            ServiceObject serviceObject = (ServiceObject) clazz.getDeclaredConstructor().newInstance();
            if (serviceObject instanceof HandlerServiceObject) {
                taskObject = new TaskObject(serviceObject, messageObject,
                        Global.getInstance().getServiceClassLoader(),
                        ServiceManager.getInstance().getEventManager(),
                        ServiceManager.getInstance().getEventManager());
            } else if (serviceObject instanceof ServiceObject) {
                taskObject = new TaskObject(serviceObject, messageObject,
                        Global.getInstance().getServiceClassLoader(),
                        ServiceManager.getInstance().getEventManager(),
                        ServiceManager.getInstance().getWorker());
            }
        } catch (ClassNotFoundException e) {
            taskObject = new OutboundTaskObject(messageObject,
                    Global.getInstance().getServiceClassLoader(),
                    ServiceManager.getInstance().getEventManager().next(),
                    ServiceManager.getInstance().getWorker());
        }
        return taskObject;
    }

    public void addCompletionListener(CompletionListener listener) {
        this.completionListeners.add(listener);
    }

    @Override
    public void run() {
        Global.currentTask.set(this);
        Global.afterService.set(new ArrayList<TaskObject>());
        try {
            switch (context) {
                case CONTROLLER:
                    logger.info("process ["
                            + requestMessage.header().targetServiceName()
                            + "] in controller context ["
                            + Thread.currentThread().getName()
                            + "], state is ["
                            + state + "]");
                    processInEventManager();
                    break;
                case WORKER:
                    logger.info("process ["
                            + requestMessage.header().targetServiceName()
                            + "] in worker context ["
                            + Thread.currentThread().getName()
                            + "], state is ["
                            + state + "]");
                    processInWorkerThread();
                    break;
                default:
                    break;
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        } finally {
            Global.currentTask.remove();
            Global.afterService.remove();
        }
    }

    protected void processInEventManager() throws ClassNotFoundException, NoSuchMethodException, SecurityException {
        switch (state) {
            case BEFORE_SERVICE:
                routeService();
                break;
            case AFTER_SERVICE:
                LinkedList<TaskObject> queue = threadScheduler.get(requestMessage.header().targetServiceName());
                if (queue != null) {
                    if (!queue.isEmpty()) {
                        workerGroup.execute(queue.poll());
                    } else {
                        int currentCount = workingCount.putIfAbsent(requestMessage.header().targetServiceName(), 0);
                        workingCount.put(requestMessage.header().targetServiceName(), currentCount - 1);
                    }
                }
                execInWorkerThread();
                break;
            case BEFORE_LISTENER:
                execChannelCompletionListener();
                execInWorkerThread();
                break;
            case AFTER_LISTENER:
                if (parentTask != null && parentTask.decreaseComputeReady() == 0 && parentTask.isCompleted()) {
                    parentTask.state = ProcessState.AFTER_SERVICE;
                    workerGroup.execute(parentTask);
                }
                state = ProcessState.TERMINATE;
                this.awake();
                execInEventManager();
                break;
            case COMPLETE_SERVICE:
                // do same thing in before service
                execInWorkerThread();
                break;
            case WAIT:
                // do nothing
                break;
            case TERMINATE:
                break;
            default:
                logger.info("unreachable state : " + state);
                break;
        }
    }

    protected void processInWorkerThread()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        if (serviceClassLoader == null) {
            serviceClassLoader = Thread.currentThread().getContextClassLoader();
        }

        switch (state) {
            case BEFORE_SERVICE:
                execService();
                break;
            case AFTER_SERVICE:
                checkWait();
                break;
            case BEFORE_LISTENER:
                execGenericCompletionListener();
                break;
            case AFTER_LISTENER:
                break;
            case COMPLETE_SERVICE:
                execComplete();
                break;
            case WAIT:
            default:
                logger.info("unreachable state : " + state);
                break;
        }
        execInEventManager();
    }

    protected void execInEventManager() {
        context = ProcessContext.CONTROLLER;
        eventManager.execute(this);
    }

    protected void execInWorkerThread() {
        context = ProcessContext.WORKER;
        workerGroup.execute(this);
    }

    protected void routeService() throws ClassNotFoundException, NoSuchMethodException, SecurityException {
        Class<?> clazz = serviceClassLoader.loadClass(requestMessage.header().targetServiceName());

        if (clazz.isAnnotationPresent(ServiceParam.class)) {
            logger.info("annotation detected");
            ServiceParam serviceParam = clazz.getAnnotation(ServiceParam.class);

            inDtoClass = serviceParam.inputClass();
            outDtoClass = serviceParam.outputClass();

            Gson gson = Global.gson.get();
            BodyObject customInDto = (BodyObject) gson.fromJson(requestMessage.body().getJsonObject(), inDtoClass);
            customInDto.setJsonObject(requestMessage.body().getJsonObject());
            customInDto.setByteBuffer(requestMessage.body().getByteBuffer());
            requestMessage.setBody(customInDto);
        }

        Method method = clazz.getDeclaredMethod("service", BodyObject.class);
        if (method.isAnnotationPresent(ServiceProperty.class)) {
            ServiceProperty serviceProperty = method.getAnnotation(ServiceProperty.class);
            int numThreads = serviceProperty.numThreads();
            int currentCount = workingCount.putIfAbsent(requestMessage.header().targetServiceName(), 0);
            LinkedList<TaskObject> queue = threadScheduler
                    .putIfAbsent(requestMessage.header().targetServiceName(), new LinkedList<TaskObject>());
            if (numThreads >= currentCount) {
                queue.add(this);
            } else {
                workingCount.put(requestMessage.header().targetServiceName(), currentCount + 1);
                execInWorkerThread();
            }
        } else {
            execInWorkerThread();
        }
    }

    protected void execService() throws ClassNotFoundException, InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        callee.service(requestMessage.body());
        if (!Global.afterService.get().isEmpty()) {
            for (TaskObject obj : Global.afterService.get()) {
                eventManager.execute(obj);
            }
            Global.afterService.get().clear();
        }
        state = ProcessState.AFTER_SERVICE;
    }

    protected void checkWait() {
        if (callee.completed()) {
            // when completed in service() method
            HeaderObject header = new DefaultHeaderObject();
            BodyObject body = callee.getReply();
            responseMessage = new MessageObject(header, body);
            state = ProcessState.COMPLETE_SERVICE;
        } else if (readyToComputeCount == 0) {
            // need to be call complete() method
            state = ProcessState.COMPLETE_SERVICE;
        } else {
            // or wait
            state = ProcessState.WAIT;
        }
    }

    protected void execComplete() {
        callee.completeService();
        HeaderObject header = new DefaultHeaderObject();
        BodyObject body = callee.getReply();
        body.setBodyClass(outDtoClass);
        responseMessage = new MessageObject(header, body);
        state = ProcessState.BEFORE_LISTENER;
    }

    protected void execGenericCompletionListener() {
        if (completionListeners != null) {
            for (CompletionListener listener : completionListeners) {
                if (!(listener instanceof ChannelCompletionListener)) {
                    if (this.parentTask != null) {
                        TaskObject tmp = Global.currentTask.get();

                        // in completion listener context, current task should be parent task, when
                        // service is called in completion listener
                        Global.currentTask.set(this.parentTask);
                        listener.afterCompletion(responseMessage.body());
                        Global.currentTask.set(tmp);
                    } else {
                        listener.afterCompletion(responseMessage.body());
                    }
                }
            }
        }
        state = ProcessState.AFTER_LISTENER;
    }

    protected void execChannelCompletionListener() {
        if (completionListeners != null) {
            for (CompletionListener listener : completionListeners) {
                if (listener instanceof ChannelCompletionListener)
                    listener.afterCompletion(responseMessage.body());
            }
        }
    }

    public void sync() {
        try {
            logger.info("call sync : " + requestMessage.header().targetServiceName());
            if (sourceChannel.isOpen()) {
                sourceChannel.read(ByteBuffer.allocate(1));
                sourceChannel.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void awake() {
        try {
            logger.info("call wake : " + requestMessage.header().targetServiceName());
            if (sinkChannel.isOpen())
                sinkChannel.write(ByteBuffer.allocate(1));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public MessageObject getReply() {
        sync();
        return responseMessage;
    }

    public boolean isCompleted() {
        return this.callee.completed();
    }

    public void terminate() {
        try {
            if (sinkChannel.isOpen()) {
                sinkChannel.close();
            }
            if (sourceChannel.isOpen()) {
                sourceChannel.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
