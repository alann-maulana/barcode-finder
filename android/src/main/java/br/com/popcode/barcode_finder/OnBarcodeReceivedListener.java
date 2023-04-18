package br.com.popcode.barcode_finder;

import java.util.List;

public interface OnBarcodeReceivedListener {
    void onBarcodeFound(List<String> code);

    void onBarcodeNotFound();

    void onOutOfMemory();
}
