package woyou.aidlservice.jiuiv5;

import woyou.aidlservice.jiuiv5.ICallback;

interface IWoyouService {
    void printerInit(in ICallback callback);
    void printText(String text, ICallback callback);
    void lineWrap(int n, ICallback callback);
}
