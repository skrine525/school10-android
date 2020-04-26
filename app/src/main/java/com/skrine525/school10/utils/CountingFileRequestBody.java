package com.skrine525.school10.utils;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

public class CountingFileRequestBody extends RequestBody {

    private static final int SEGMENT_SIZE = 2048; // okio.Segment.SIZE

    private final File file;
    private final String contentType;
    private final ProgressListener listener;

    public CountingFileRequestBody(File file, String contentType, ProgressListener listener) {
        this.file = file;
        this.contentType = contentType;
        this.listener = listener;
    }

    @Override
    public long contentLength() {
        return file.length();
    }

    @Override
    public MediaType contentType() {
        return MediaType.parse(contentType);
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        Source source = null;
        try {
            source = Okio.source(file);
            long total = 0;
            long length = contentLength();
            long read;

            while ((read = source.read(sink.buffer(), SEGMENT_SIZE)) != -1) {
                total += read;
                sink.flush();
                this.listener.onProgress(total, length);

            }
        } finally {
            Util.closeQuietly(source);
        }
    }

    public interface ProgressListener {
        void onProgress(long bytesWritten, long contentLength);
    }

}