/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.ml.job.process;

import org.elasticsearch.common.CheckedConsumer;
import org.elasticsearch.xpack.ml.job.process.autodetect.params.FlushJobParams;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.time.ZonedDateTime;

/**
 * Interface representing common functions of ML native C++ processes
 */
public interface MlProcess extends Closeable {

    /**
     * Restore state by calling the given restore function
     * @param restorer a function that will restore state to the stream passed to it
     */
    void restoreState(CheckedConsumer<OutputStream, IOException> restorer);

    /**
     * Is the process ready to receive data?
     * @return {@code true} if the process is ready to receive data
     */
    boolean isReady();

    /**
     * Write the record to autodetect. The record parameter should not be encoded
     * (i.e. length encoded) the implementation will appy the corrrect encoding.
     *
     * @param record Plain array of strings, implementors of this class should
     *               encode the record appropriately
     * @throws IOException If the write failed
     */
    void writeRecord(String[] record) throws IOException;

    /**
     * Flush the job pushing any stale data into autodetect.
     * Every flush command generates a unique flush Id which will be output
     * in a flush acknowledgment by the autodetect process once the flush has
     * been processed.
     *
     * @param params Parameters describing the controls that will accompany the flushing
     *               (e.g. calculating interim results, time control, etc.)
     * @return The flush Id
     * @throws IOException If the flush failed
     */
    String flushJob(FlushJobParams params) throws IOException;

    /**
     * Flush the output data stream
     */
    void flushStream() throws IOException;

    /**
     * Kill the process.  Do not wait for it to stop gracefully.
     */
    void kill() throws IOException;

    /**
     * The time the process was started
     * @return Process start time
     */
    ZonedDateTime getProcessStartTime();

    /**
     * Returns true if the process still running.
     * Methods such as {@link #flushJob(FlushJobParams)} are essentially
     * asynchronous the command will be continue to execute in the process after
     * the call has returned. This method tests whether something catastrophic
     * occurred in the process during its execution.
     * @return True if the process is still running
     */
    boolean isProcessAlive();

    /**
     * Check whether autodetect terminated given maximum 45ms for termination
     *
     * Processing errors are highly likely caused by autodetect being unexpectedly
     * terminated.
     *
     * Workaround: As we can not easily check if autodetect is alive, we rely on
     * the logPipe being ended. As the loghandler runs in another thread which
     * might fall behind this one, we give it a grace period of 45ms.
     *
     * @return false if process has ended for sure, true if it probably still runs
     */
    boolean isProcessAliveAfterWaiting();

    /**
     * Read any content in the error output buffer.
     * @return An error message or empty String if no error.
     */
    String readError();
}