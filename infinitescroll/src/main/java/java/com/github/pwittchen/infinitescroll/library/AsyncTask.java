/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package java.com.github.pwittchen.infinitescroll.library;

import ohos.eventhandler.EventHandler;
import ohos.eventhandler.EventRunner;
import ohos.eventhandler.InnerEvent;
import ohos.hiviewdfx.HiLog;
import ohos.hiviewdfx.HiLogLabel;
import ohos.os.ProcessManager;
import java.util.ArrayDeque;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AsyncTask.
 *
 * @param <Params> Params
 * @param <Progress> Progress
 * @param <Result> Result
 */

public abstract class AsyncTask<Params, Progress, Result> {

    /**
     * To print logInfo.
     */
    private static final String LOG_TAG = "AsyncTask";

    /**
     * LABEL_LOG.
     */
    public static final HiLogLabel LABEL_LOG = new HiLogLabel(3, 0xD000F00, "[InfiniteScroll] ");


    // We keep only a single pool thread around all the time.
    // We let the pool grow to a fairly large number of threads if necessary,
    // but let them time out quickly. In the unlikely case that we run out of threads,
    // we fall back to a simple unbounded-queue executor.
    // This combination ensures that:
    // 1. We normally keep few threads (1) around.
    // 2. We queue only after launching a significantly larger, but still bounded, set of threads.
    // 3. We keep the total number of threads bounded, but still allow an unbounded set
    // of tasks to be queued.

    /**
     * To define CORE_POOL_SIZE.
     */
    private static final int CORE_POOL_SIZE = 1;

    /**
     * MAXIMUM_POOL_SIZE.
     */
    private static final int MAXIMUM_POOL_SIZE = 20;

    /**
     * BACKUP_POOL_SIZE.
     */
    private static final int BACKUP_POOL_SIZE = 5;

    /**
     * KEEP_ALIVE_SECONDS.
     */
    private static final int KEEP_ALIVE_SECONDS = 3;

    /**
     * THREAD_PRIORITY_BACKGROUND.
     */
    public static final int THREAD_PRIORITY_BACKGROUND = 10;

    /**
     * ThreadFactory.
     */
    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);
        public Thread newThread(Runnable r) {
            return new Thread(r, "AsyncTask #" + mCount.getAndIncrement());
        }
    };

    /**
     * Initialization protected by sRunOnSerialPolicy lock.
     */
    private static ThreadPoolExecutor sBackupExecutor;

    /**
     * BackupExecutorQueue.
     */
    private static LinkedBlockingQueue<Runnable> sBackupExecutorQueue;

    /**
     * RejectedExecutionHandler.
     */
    private static final RejectedExecutionHandler sRunOnSerialPolicy =
            new RejectedExecutionHandler() {
                public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                    HiLog.warn(LABEL_LOG, LOG_TAG, "Exceeded ThreadPoolExecutor pool size");
                    // As a last ditch fallback, run it on an executor with an unbounded queue.
                    // Create this executor lazily, hopefully almost never.
                    synchronized (this) {
                        if (sBackupExecutor == null) {
                            sBackupExecutorQueue = new LinkedBlockingQueue<Runnable>();
                            sBackupExecutor = new ThreadPoolExecutor(
                                    BACKUP_POOL_SIZE, BACKUP_POOL_SIZE, KEEP_ALIVE_SECONDS,
                                    TimeUnit.SECONDS, sBackupExecutorQueue, sThreadFactory);
                            sBackupExecutor.allowCoreThreadTimeOut(true);
                        }
                    }
                    sBackupExecutor.execute(r);
                }
            };

    /**
     * An {@link Executor} that can be used to execute tasks in parallel.
     * Using a single thread pool for a general purpose results in suboptimal behavior
     * for different tasks. Small, CPU-bound tasks benefit from a bounded pool and queueing, and
     * long-running blocking tasks, such as network operations, benefit from many threads. Use or
     * create an {@link Executor} configured for your use case.
     */

    public static final Executor THREAD_POOL_EXECUTOR;

    static {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(), sThreadFactory);
        threadPoolExecutor.setRejectedExecutionHandler(sRunOnSerialPolicy);
        THREAD_POOL_EXECUTOR = threadPoolExecutor;
    }

    /**
     * An {@link Executor} that executes tasks one at a time in serial
     * order.  This serialization is global to a particular process.
     * Globally serializing tasks results in excessive queuing for unrelated operations.
     */

    public static final Executor SERIAL_EXECUTOR = new SerialExecutor();

    /**
     * MESSAGE_POST_RESULT.
     */
    private static final int MESSAGE_POST_RESULT = 0x1;

    /**
     * MESSAGE_POST_PROGRESS.
     */
    private static final int MESSAGE_POST_PROGRESS = 0x2;

    /**
     * DefaultExecutor.
     */
    private static volatile Executor sDefaultExecutor = SERIAL_EXECUTOR;

    /**
     * EventHandler.
     */
    private static InternalEventHandler sEventHandler;

    /**
     * WorkerRunnable.
     */
    private final WorkerRunnable<Params, Result> mWorker;

    /**
     * FutureTask.
     */
    private final FutureTask<Result> mFuture;

    /**
     * Indicates that the task has not been executed yet.
     */
    private volatile Status mStatus = Status.PENDING;

    /**
     * AtomicBoolean.
     */
    private final AtomicBoolean mCancelled = new AtomicBoolean();

    /**
     * TaskInvoked.
     */
    private final AtomicBoolean mTaskInvoked = new AtomicBoolean();

    /**
     * EventHandler to handle Events.
     */
    private final EventHandler mEventHandler;

    /**
     * SerialExecutor.
     */
    private static class SerialExecutor implements Executor {

        /**
         * Tasks.
         */
        private final ArrayDeque<Runnable> mTasks = new ArrayDeque<Runnable>();

        /**
         * Active.
         */
        private Runnable mActive;

        /**
         * execute.
         *
         * @param r r
         */
        public synchronized void execute(final Runnable r) {
            mTasks.offer(new Runnable() {
                public void run() {
                    try {
                        r.run();
                    } finally {
                        scheduleNext();
                    }
                }
            });
            if (mActive == null) {
                scheduleNext();
            }
        }

        /**
         * scheduleNext.
         */
        protected synchronized void scheduleNext() {
            if ((mActive = mTasks.poll()) != null) {
                THREAD_POOL_EXECUTOR.execute(mActive);
            }
        }
    }

    /**
     * Indicates the current status of the task. Each status will be set only once
     * during the lifetime of a task.
     */
    public enum Status {
        /**
         * Indicates that the task has not been executed yet.
         */
        PENDING,
        /**
         * Indicates that the task is running.
         */
        RUNNING,
        /**
         * Indicates that {@link AsyncTask#onPostExecute} has finished.
         */
        FINISHED,
    }

    /**
     * To get the MainEventHandler.
     *
     * @return EventHandler
     */
    private static EventHandler getMainEventHandler() {
        synchronized (AsyncTask.class) {
            if (sEventHandler == null) {
                sEventHandler = new InternalEventHandler(EventRunner.getMainEventRunner());
            }
            return sEventHandler;
        }
    }

    /**
     * To get EventHandler.
     *
     * @return EventHandler
     */
    private EventHandler getEventHandler() {
        return mEventHandler;
    }

    /**
     * setDefaultExecutor'.
     *
     * @param exec exec
     * @hide
     */
    public static void setDefaultExecutor(Executor exec) {
        sDefaultExecutor = exec;
    }

    /**
     * Creates a new asynchronous task. This constructor must be invoked on the UI thread.
     */
    public AsyncTask() {
        this((EventRunner) null);
    }

    /**
     * Creates a new asynchronous task. This constructor must be invoked on the UI thread.
     *
     * @param EventHandler EventHandler
     * @hide
     */
    public AsyncTask(EventHandler EventHandler) {
        this(EventHandler != null ? EventHandler.getEventRunner() : null);
    }

    /**
     * Creates a new asynchronous task. This constructor must be invoked on the UI thread.
     *
     * @param  callbackLooper callbackLooper
     * @hide
     */
    public AsyncTask(EventRunner callbackLooper) {
        mEventHandler = callbackLooper == null || callbackLooper == EventRunner.getMainEventRunner()
                ? getMainEventHandler()
                : new EventHandler(callbackLooper);
        mWorker = new WorkerRunnable<Params, Result>() {
            public Result call() throws Exception {
                mTaskInvoked.set(true);
                Result result = null;
                try {
                    ProcessManager.setThreadPriority(THREAD_PRIORITY_BACKGROUND);
                    //noinspection unchecked
                    result = doInBackground(mParams);
                    //todo
//                    Binder.flushPendingCommands();
                } catch (Throwable tr) {
                    mCancelled.set(true);
                    throw tr;
                } finally {
                    postResult(result);
                }
                return result;
            }
        };

        mFuture = new FutureTask<Result>(mWorker) {
            @Override
            protected void done() {
                try {
                    postResultIfNotInvoked(get());
                } catch (InterruptedException e) {
                    HiLog.warn(LABEL_LOG, LOG_TAG, e);
                } catch (ExecutionException e) {
                    throw new RuntimeException("An error occurred while executing doInBackground()",
                            e.getCause());
                } catch (CancellationException e) {
                    postResultIfNotInvoked(null);
                }
            }
        };
    }

    /**
     * postResultIfNotInvoked.
     *
     * @param result result
     */
    private void postResultIfNotInvoked(Result result) {
        final boolean wasTaskInvoked = mTaskInvoked.get();
        if (!wasTaskInvoked) {
            postResult(result);
        }
    }

    /**
     * To post result.
     *
     * @param result result
     * @return Result
     */
    private Result postResult(Result result) {
        InnerEvent innerEvent = InnerEvent.get(MESSAGE_POST_RESULT, new AsyncTaskResult<Result>(this, result));
        getEventHandler().sendEvent(innerEvent);
        return result;
    }

    /**
     * Returns the current status of this task.
     *
     * @return The current status.
     */
    public final Status getStatus() {
        return mStatus;
    }

    /**
     * Override this method to perform a computation on a background thread. The
     * specified parameters are the parameters passed to {@link #execute}
     * by the caller of this task.
     * This will normally run on a background thread. But to better
     * support testing frameworks, it is recommended that this also tolerates
     * direct execution on the foreground thread, as part of the {@link #execute} call.
     * This method can call {@link #publishProgress} to publish updates
     * on the UI thread.
     *
     * @param params The parameters of the task.
     * @return A result, defined by the subclass of this task.
     * @see #onPreExecute()
     * @see #onPostExecute
     * @see #publishProgress
     */
    protected abstract Result doInBackground(Params... params);

    /**
     * Runs on the UI thread before {@link #doInBackground}.
     * Invoked directly by {@link #execute} or {@link #executeOnExecutor}.
     * The default version does nothing.
     *
     * @see #onPostExecute
     * @see #doInBackground
     */
    protected void onPreExecute() {
    }

    /**
     * <p>Runs on the UI thread after {@link #doInBackground}. The
     * specified result is the value returned by {@link #doInBackground}.
     * To better support testing frameworks, it is recommended that this be
     * written to tolerate direct execution as part of the execute() call.
     * The default version does nothing.</p>
     *
     * <p>This method won't be invoked if the task was cancelled.</p>
     *
     * @param result The result of the operation computed by {@link #doInBackground}.
     * @see #onPreExecute
     * @see #doInBackground
     * @see #onCancelled(Object)
     */
    @SuppressWarnings({"UnusedDeclaration"})
    protected void onPostExecute(Result result) {
    }

    /**
     * Runs on the UI thread after {@link #publishProgress} is invoked.
     * The specified values are the values passed to {@link #publishProgress}.
     * The default version does nothing.
     *
     * @param values The values indicating progress.
     * @see #publishProgress
     * @see #doInBackground
     */
    @SuppressWarnings({"UnusedDeclaration"})
    protected void onProgressUpdate(Progress... values) {
    }

    /**
     * <p>Runs on the UI thread after {@link #cancel(boolean)} is invoked and
     * {@link #doInBackground(Object[])} has finished.</p>
     *
     * <p>The default implementation simply invokes {@link #onCancelled()} and
     * ignores the result. If you write your own implementation, do not call
     * <code>super.onCancelled(result)</code>.</p>
     *
     * @param result The result, if any, computed in
     *               {@link #doInBackground(Object[])}, can be null
     * @see #cancel(boolean)
     * @see #isCancelled()
     */
    @SuppressWarnings({"UnusedParameters"})
    protected void onCancelled(Result result) {
        onCancelled();
    }

    /**
     * <p>Applications should preferably override {@link #onCancelled(Object)}.
     * This method is invoked by the default implementation of
     * {@link #onCancelled(Object)}.
     * The default version does nothing.</p>
     *
     * <p>Runs on the UI thread after {@link #cancel(boolean)} is invoked and
     * {@link #doInBackground(Object[])} has finished.</p>
     *
     * @see #onCancelled(Object)
     * @see #cancel(boolean)
     * @see #isCancelled()
     */
    protected void onCancelled() {
    }

    /**
     * Returns <tt>true</tt> if this task was cancelled before it completed
     * normally. If you are calling {@link #cancel(boolean)} on the task,
     * the value returned by this method should be checked periodically from
     * {@link #doInBackground(Object[])} to end the task as soon as possible.
     *
     * @return <tt>true</tt> if task was cancelled before it completed
     * @see #cancel(boolean)
     */
    public final boolean isCancelled() {
        return mCancelled.get();
    }

    /**
     * <p>Attempts to cancel execution of this task.  This attempt will
     * fail if the task has already completed, already been cancelled,
     * or could not be cancelled for some other reason. If successful,
     * and this task has not started when <tt>cancel</tt> is called,
     * this task should never run. If the task has already started,
     * then the <tt>mayInterruptIfRunning</tt> parameter determines
     * whether the thread executing this task should be interrupted in
     * an attempt to stop the task.</p>
     *
     * <p>Calling this method will result in {@link #onCancelled(Object)} being
     * invoked on the UI thread after {@link #doInBackground(Object[])} returns.
     * Calling this method guarantees that onPostExecute(Object) is never
     * subsequently invoked, even if <tt>cancel</tt> returns false, but
     * {@link #onPostExecute} has not yet run.  To finish the
     * task as early as possible, check {@link #isCancelled()} periodically from
     * {@link #doInBackground(Object[])}.</p>
     *
     * <p>This only requests cancellation. It never waits for a running
     * background task to terminate, even if <tt>mayInterruptIfRunning</tt> is
     * true.</p>
     *
     * @param mayInterruptIfRunning <tt>true</tt> if the thread executing this
     *                              task should be interrupted; otherwise, in-progress tasks are allowed
     *                              to complete.
     * @return <tt>false</tt> if the task could not be cancelled,
     * typically because it has already completed normally;
     * <tt>true</tt> otherwise
     * @see #isCancelled()
     * @see #onCancelled(Object)
     */
    public final boolean cancel(boolean mayInterruptIfRunning) {
        mCancelled.set(true);
        return mFuture.cancel(mayInterruptIfRunning);
    }

    /**
     * Waits if necessary for the computation to complete, and then
     * retrieves its result.
     *
     * @return The computed result.
     * @throws CancellationException If the computation was cancelled.
     * @throws ExecutionException    If the computation threw an exception.
     * @throws InterruptedException  If the current thread was interrupted
     *                               while waiting.
     */
    public final Result get() throws InterruptedException, ExecutionException {
        return mFuture.get();
    }

    /**
     * Waits if necessary for at most the given time for the computation
     * to complete, and then retrieves its result.
     *
     * @param timeout Time to wait before cancelling the operation.
     * @param unit    The time unit for the timeout.
     * @return The computed result.
     * @throws CancellationException If the computation was cancelled.
     * @throws ExecutionException    If the computation threw an exception.
     * @throws InterruptedException  If the current thread was interrupted
     *                               while waiting.
     * @throws TimeoutException      If the wait timed out.
     */
    public final Result get(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {
        return mFuture.get(timeout, unit);
    }

    /**
     * Executes the task with the specified parameters. The task returns
     * itself (this) so that the caller can keep a reference to it.
     *
     * <p>Note: this function schedules the task on a queue for a single background
     * thread or pool of threads depending on the platform version.  When first
     * introduced, AsyncTasks were executed serially on a single background thread.
     * to a pool of threads allowing multiple tasks to operate in parallel. Starting
     * executed on a single thread to avoid common application errors caused
     * by parallel execution.  If you truly want parallel execution, you can use
     * the {@link #executeOnExecutor} version of this method
     * with {@link #THREAD_POOL_EXECUTOR}; however, see commentary there for warnings
     * on its use.
     *
     * <p>This method must be invoked on the UI thread.
     *
     * @param params The parameters of the task.
     * @return This instance of AsyncTask.
     * @throws IllegalStateException If {@link #getStatus()} returns either
     *                               {@link Status#RUNNING} or {@link Status#FINISHED}.
     * @see #executeOnExecutor(Executor, Object[])
     * @see #execute(Runnable)
     */
    public final AsyncTask<Params, Progress, Result> execute(Params... params) {
        return executeOnExecutor(sDefaultExecutor, params);
    }

    /**
     * Executes the task with the specified parameters. The task returns
     * itself (this) so that the caller can keep a reference to it.
     *
     * <p>This method is typically used with {@link #THREAD_POOL_EXECUTOR} to
     * allow multiple tasks to run in parallel on a pool of threads managed by
     * AsyncTask, however you can also use your own {@link Executor} for custom
     * behavior.
     *
     * <p><em>Warning:</em> Allowing multiple tasks to run in parallel from
     * a thread pool is generally <em>not</em> what one wants, because the order
     * of their operation is not defined.  For example, if these tasks are used
     * to modify any state in common (such as writing a file due to a button click),
     * there are no guarantees on the order of the modifications.
     * Without careful work it is possible in rare cases for the newer version
     * of the data to be over-written by an older one, leading to obscure data
     * loss and stability issues.  Such changes are best
     * executed in serial; to guarantee such work is serialized regardless of
     * platform version you can use this function with {@link #SERIAL_EXECUTOR}.
     *
     * <p>This method must be invoked on the UI thread.
     *
     * @param exec   The executor to use.  {@link #THREAD_POOL_EXECUTOR} is available as a
     *               convenient process-wide thread pool for tasks that are loosely coupled.
     * @param params The parameters of the task.
     * @return This instance of AsyncTask.
     * @throws IllegalStateException If {@link #getStatus()} returns either
     *                               {@link Status#RUNNING} or {@link Status#FINISHED}.
     * @see #execute(Object[])
     */
    public final AsyncTask<Params, Progress, Result> executeOnExecutor(Executor exec,
                                                                       Params... params) {
        if (mStatus != Status.PENDING) {
            switch (mStatus) {
                case RUNNING:
                    throw new IllegalStateException("Cannot execute task:"
                            + " the task is already running.");
                case FINISHED:
                    throw new IllegalStateException("Cannot execute task:"
                            + " the task has already been executed "
                            + "(a task can be executed only once)");
            }
        }

        mStatus = Status.RUNNING;
        onPreExecute();
        mWorker.mParams = params;
        exec.execute(mFuture);
        return this;
    }

    /**
     * Convenience version of {@link #execute(Object...)} for use with
     * a simple Runnable object. See {@link #execute(Object[])} for more
     * information on the order of execution.
     *
     * @param runnable runnable
     * @see #execute(Object[])
     * @see #executeOnExecutor(Executor, Object[])
     */
    public static void execute(Runnable runnable) {
        sDefaultExecutor.execute(runnable);
    }

    /**
     * This method can be invoked from {@link #doInBackground} to
     * publish updates on the UI thread while the background computation is
     * still running. Each call to this method will trigger the execution of
     * {@link #onProgressUpdate} on the UI thread.
     * {@link #onProgressUpdate} will not be called if the task has been
     * canceled.
     *
     * @param values The progress values to update the UI with.
     * @see #onProgressUpdate
     * @see #doInBackground
     */
    protected final void publishProgress(Progress... values) {
        if (!isCancelled()) {
            InnerEvent innerEvent = InnerEvent.get(MESSAGE_POST_PROGRESS, new AsyncTaskResult<Progress>(this, values));
            getEventHandler().sendEvent(innerEvent);
        }
    }

    /**
     * finish.
     *
     * @param result result
     */
    private void finish(Result result) {
        if (isCancelled()) {
            onCancelled(result);
        } else {
            onPostExecute(result);
        }
        mStatus = Status.FINISHED;
    }

    /**
     * InternalEventHandler to handle Internal Events.
     */
    private static class InternalEventHandler extends EventHandler {

        /**
         * InternalEventHandler.
         *
         * @param eventRunner eventRunner
         */
        public InternalEventHandler(EventRunner eventRunner) {
            super(eventRunner);
        }

        @SuppressWarnings({"unchecked", "RawUseOfParameterizedType"})
        @Override
        public void processEvent(InnerEvent msg) {
            AsyncTaskResult<?> result = (AsyncTaskResult<?>) msg.object;
            switch (msg.eventId) {
                case MESSAGE_POST_RESULT:
                    // There is only one result
                    result.mTask.finish(result.mData[0]);
                    break;
                case MESSAGE_POST_PROGRESS:
                    result.mTask.onProgressUpdate(result.mData);
                    break;
            }
        }
    }

    /**
     * WorkerRunnable.
     *
     * @param <Params> params
     * @param <Result> Result
     */
    private abstract static class WorkerRunnable<Params, Result> implements Callable<Result> {

        /**
         * Params.
         */
        Params[] mParams;
    }

    /**
     * AsyncTaskResult.
     *
     * @param <Data> Data
     */
    @SuppressWarnings({"RawUseOfParameterizedType"})
    private static class AsyncTaskResult<Data> {

        /**
         * AsyncTask.
         */
        private final AsyncTask mTask;

        /**
         * Data.
         */
        private final Data[] mData;

        /**
         * AsyncTaskResult.
         *
         * @param task task
         * @param data data
         */
        AsyncTaskResult(AsyncTask task, Data... data) {
            mTask = task;
            mData = data;
        }
    }
}