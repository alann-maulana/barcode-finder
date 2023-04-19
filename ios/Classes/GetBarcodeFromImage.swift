//
//  GetBarcodeFromImage.swift
//  barcode_finder
//
//  Created by Rafael Aquila on 25/12/20.
//

import Foundation

func getBarcodeFromImage(uiImage: UIImage, barcodesToFilter: [BarcodeFormatType] = [BarcodeFormatType.any], singleResult: Bool = true) -> [String]?{
    let scanners = [scanUsingZxing, scanUsingZBar]
    for scanner in scanners{
        if let barcodeFound = scanner(uiImage, barcodesToFilter, singleResult){
            return barcodeFound
        }
    }
    return nil
}


private func scanUsingZxing(_ image: UIImage, barcodesToFilter: [BarcodeFormatType], singleResult: Bool = true) ->[String]?{
    let result: [String]? = zxingScanImage(image, barcodesToFilter: barcodesToFilter)
    if let barcode = result{
        if(!barcode.isEmpty){
            return barcode
        }
    }
    return nil
}

private func scanUsingZBar(_ image: UIImage, barcodesToFilter: [BarcodeFormatType], singleResult: Bool = true) ->[String]?{
    let result: String? = zbarScanImage(image, barcodesToFilter: barcodesToFilter)
    if let barcode = result{
        if(!barcode.isEmpty){
            return [barcode]
        }
    }
    return nil
}

