import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';

class CapturedRequest {
  final String method;
  final String path;
  final dynamic body;

  const CapturedRequest({
    required this.method,
    required this.path,
    required this.body,
  });
}

typedef FakeDioHandler =
    FutureOr<ResponseBody> Function(RequestOptions options, dynamic body);

class FakeDioAdapter implements HttpClientAdapter {
  final FakeDioHandler handler;
  final List<CapturedRequest> requests = [];

  FakeDioAdapter(this.handler);

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    final body = _readBody(options);
    requests.add(
      CapturedRequest(method: options.method, path: options.path, body: body),
    );
    return handler(options, body);
  }

  @override
  void close({bool force = false}) {}
}

ResponseBody jsonResponse(dynamic data, {int statusCode = 200}) {
  return ResponseBody.fromString(
    jsonEncode(data),
    statusCode,
    headers: {
      Headers.contentTypeHeader: [Headers.jsonContentType],
    },
  );
}

ResponseBody textResponse(String data, {int statusCode = 200}) {
  return ResponseBody.fromString(data, statusCode);
}

dynamic _readBody(RequestOptions options) {
  final data = options.data;
  if (data != null) {
    return data;
  }
  return null;
}
