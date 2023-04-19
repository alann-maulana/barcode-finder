//
//  PDFtoBarcodeConversor.swift
//  iosbarcodefrompdf
//
//  Created by Rafael Aquila on 01/12/20.
//

import Foundation

class BarcodeScanner {
    
    var image: UIImage?
    var barcodesToFilter: [BarcodeFormatType]?
    func tryFindBarcodeFrom(uiImage: UIImage, barcodesToFilter: [BarcodeFormatType] = [BarcodeFormatType.any], singleResult: Bool = true) -> [String]?{
        self.image = uiImage
        self.barcodesToFilter = barcodesToFilter
        let rotationAttemptResult = tryRotateImage(uiImage, singleResult)
        return rotationAttemptResult
        
    }
    
    private func tryRotateImage(_ uiImage: UIImage, _ singleResult: Bool) -> [String]?{
        let attemptList = createAttemptList()
        for attempFunction in attemptList{
            if let barcode = attempFunction(uiImage, singleResult){
                if(!barcode.isEmpty){
                    return barcode
                }
            }
        }
        return nil;
    }
    
    private func createAttemptList() -> Array<((UIImage, Bool) ->[String]?)> {
        var attemptTransformList = Array<((UIImage, Bool) ->[String]?)>()
        attemptTransformList.append(decodeUnmodifiedImage)
        attemptTransformList.append(decodeRotated90DegreesImage)
        attemptTransformList.append(decodeRotated90DegreesImage)
        attemptTransformList.append(decodeRotated90DegreesImage)
        attemptTransformList.append(decodeCroppedPdf)
        
        return attemptTransformList
    }
    
    private func decodeRotated90DegreesImage(uiImage: UIImage, singleResult: Bool) ->[String]?{
        let rotated = uiImage.rotate(radians: .pi/2)
        self.image = rotated
        return getBarcodeFromImage(uiImage: uiImage, barcodesToFilter: self.barcodesToFilter!, singleResult: singleResult)
    }
    
    private func decodeUnmodifiedImage(uiImage: UIImage, singleResult: Bool) ->[String]?{
        return getBarcodeFromImage(uiImage: uiImage, barcodesToFilter: self.barcodesToFilter!, singleResult: singleResult)
        
    }
    
    private func decodeCroppedPdf(uiImage: UIImage, singleResult: Bool) ->[String]?{
        let cropped = uiImage.cropHalf()
        return getBarcodeFromImage(uiImage: cropped, barcodesToFilter: self.barcodesToFilter!, singleResult: singleResult)
        
    }
    
}

