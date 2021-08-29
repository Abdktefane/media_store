import 'dart:async';
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter/services.dart';

class MediaStore {
  static const MethodChannel _channel =
      const MethodChannel('net.amond/media_store');

  /// save image to Gallery
  /// imageBytes can't null
  static Future saveImage(Uint8List imageBytes) async {
    assert(imageBytes != null);
    final result =
        await _channel.invokeMethod('saveImageToGallery', imageBytes);
    return result;
  }

  /// Save the PNG，JPG，JPEG image or video located at [file] to the local device media gallery.
  static Future saveFile({required File file, required String name}) async {
    final result = await _channel.invokeMethod('saveFileToGallery', {
      'path': file.path,
      'name': name,
    });
    return result;
  }
}
