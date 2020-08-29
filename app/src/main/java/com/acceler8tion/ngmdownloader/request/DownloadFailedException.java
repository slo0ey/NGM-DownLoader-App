package com.acceler8tion.ngmdownloader.request;

import java.io.IOException;

public class DownloadFailedException extends IOException {

    public DownloadFailedException(String message) {
        super(message);
    }

}
