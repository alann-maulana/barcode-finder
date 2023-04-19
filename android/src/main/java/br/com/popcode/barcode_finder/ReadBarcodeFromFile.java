package br.com.popcode.barcode_finder;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.GenericMultipleBarcodeReader;
import com.google.zxing.multi.MultipleBarcodeReader;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ReadBarcodeFromFile extends AsyncTask<Object, Void, List<String>> {

    private static final int NUMBER_OF_ATTEMPTS = 4;
    private static final double PORCENTAGEM_ESCALA = 0.05;

    @SuppressLint("StaticFieldLeak")
    private final Context context;
    private final Uri filePath;
    private boolean outOfMemoryError = false;
    private final OnBarcodeReceivedListener listener;
    private final EntryType entryType;
    private final ArrayList barcodeFormats;

    ReadBarcodeFromFile(OnBarcodeReceivedListener listener, Context context, Uri filePath, EntryType entryType, ArrayList barcodeFormats) {
        this.listener = listener;
        this.context = context;
        this.filePath = filePath;
        this.entryType = entryType;
        this.barcodeFormats = barcodeFormats;
    }

    @Override
    protected void onPreExecute() {
    }

    private Bitmap resizeImage(Bitmap bitmap, int tryNumber){
        int width = (int) (bitmap.getWidth() * (1-tryNumber * PORCENTAGEM_ESCALA));
        int height = (int) (bitmap.getHeight() * (1-tryNumber * PORCENTAGEM_ESCALA));
        bitmap = Bitmap.createScaledBitmap(bitmap, width,height, false);
        return bitmap;
    }

    @Override
    protected List<String> doInBackground(Object... objects) {
        boolean singleResult = true;
        if (objects.length == 1) {
            if (objects[0] instanceof Boolean) {
                singleResult = (Boolean) objects[0];
            }
        }

        if (filePath != null) {
            int tryNumber = 1;
            while (tryNumber <= NUMBER_OF_ATTEMPTS && !outOfMemoryError) {
                Bitmap bitmap;
                if (entryType == EntryType.PDF) {
                    bitmap = generateImageFromPdf(filePath, 0, tryNumber);
                } else {
                    bitmap = BitmapFactory.decodeFile(filePath.getPath());
                    bitmap = resizeImage(bitmap, tryNumber);
                }
                if (bitmap != null) {
                    List<String> code = scanImage(bitmap, new MultiFormatReader(), singleResult);
                    if (code != null && !code.isEmpty()) {
                        return code;
                    }
                }
                tryNumber++;
            }
        }
        return new ArrayList<>();
    }

    @Override
    protected void onPostExecute(List<String> result) {
        if (!outOfMemoryError) {
            if (result != null && !result.isEmpty()) {
                listener.onBarcodeFound(result);
            } else {
                listener.onBarcodeNotFound();
            }
        } else {
            listener.onOutOfMemory();
        }
    }

    private Bitmap generateImageFromPdf(Uri assetFileName, int numeroPagina, int tryNumber) {
        PdfiumCore pdfiumCore = new PdfiumCore(context);
        try {
            ParcelFileDescriptor parcelFileDescriptor;
            ContentResolver contentResolver = context.getApplicationContext().getContentResolver();
            parcelFileDescriptor = contentResolver.openFileDescriptor(assetFileName, "r", null);
            PdfDocument pdfDocument = pdfiumCore.newDocument(parcelFileDescriptor);
            pdfiumCore.openPage(pdfDocument, numeroPagina);
            int width = pdfiumCore.getPageWidthPoint(pdfDocument, numeroPagina) * tryNumber;
            int height = pdfiumCore.getPageHeightPoint(pdfDocument, numeroPagina) * tryNumber;
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            pdfiumCore.renderPageBitmap(pdfDocument, bmp, numeroPagina, 0, 0, width, height);
            pdfiumCore.closeDocument(pdfDocument);
            return bmp;
        } catch (OutOfMemoryError e) {
            Log.e("BarcodeTest", "Error Out of memory", e);
            outOfMemoryError = true;
            return null;
        } catch (Exception e) {
            Log.e("Bitmap PDF", e.getMessage());
        }
        return null;
    }

    private List<String> scanImage(Bitmap bMap, Reader reader, boolean singleResult) {
        List<String> contents = new ArrayList<>();
        try {
            if (!outOfMemoryError) {
                int[] intArray = new int[bMap.getWidth() * bMap.getHeight()];
                bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(), bMap.getHeight());
                LuminanceSource source = new RGBLuminanceSource(bMap.getWidth(), bMap.getHeight(), intArray);
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
                hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
                if (singleResult) {
                    Result result = reader.decode(bitmap, hints);
                    if (this.barcodeFormats.isEmpty() || this.barcodeFormats.contains(result.getBarcodeFormat().toString())) {
                        contents.add(result.getText());
                    }
                } else {
                    MultipleBarcodeReader multipleReader = new GenericMultipleBarcodeReader(reader);
                    Result[] results = multipleReader.decodeMultiple(bitmap, hints);
                    for (Result result : results) {
                        if (this.barcodeFormats.isEmpty() || this.barcodeFormats.contains(result.getBarcodeFormat().toString())) {
                            contents.add(result.getText());
                        }
                    }
                }
            }
        } catch (OutOfMemoryError e) {
            Log.e("BarcodeTest", "Error Out of memory", e);
            outOfMemoryError = true;
            return null;
        } catch (Exception e) {
            Log.e("BarcodeTest", "Error decoding barcode", e);
        }
        return contents;
    }
}