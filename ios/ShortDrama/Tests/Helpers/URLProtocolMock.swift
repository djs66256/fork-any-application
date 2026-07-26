import Foundation

/// A URLProtocol subclass for mocking network responses in tests.
final class URLProtocolMock: URLProtocol {

    /// Handler type: given a request, return a response + data or an error.
    typealias RequestHandler = (URLRequest) throws -> (HTTPURLResponse, Data)

    /// Shared handler for all URLProtocolMock instances.
    nonisolated(unsafe) static var handler: RequestHandler?

    override class func canInit(with request: URLRequest) -> Bool { true }

    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }

    override func startLoading() {
        guard let handler = URLProtocolMock.handler else {
            client?.urlProtocol(self, didFailWithError: URLError(.badServerResponse))
            return
        }

        do {
            let interceptedRequest = try materializedRequest(from: request)
            let (response, data) = try handler(interceptedRequest)
            client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
            client?.urlProtocol(self, didLoad: data)
            client?.urlProtocolDidFinishLoading(self)
        } catch {
            client?.urlProtocol(self, didFailWithError: error)
        }
    }

    override func stopLoading() {}

    private func materializedRequest(from request: URLRequest) throws -> URLRequest {
        guard request.httpBody == nil, let stream = request.httpBodyStream else {
            return request
        }

        var request = request
        request.httpBody = try readAllData(from: stream)
        return request
    }

    private func readAllData(from stream: InputStream) throws -> Data {
        stream.open()
        defer { stream.close() }

        let bufferSize = 1024
        var buffer = Array(repeating: UInt8(0), count: bufferSize)
        var data = Data()

        while stream.hasBytesAvailable {
            let readCount = stream.read(&buffer, maxLength: bufferSize)
            if readCount < 0 {
                throw stream.streamError ?? URLError(.cannotDecodeRawData)
            }
            if readCount == 0 {
                break
            }
            data.append(buffer, count: readCount)
        }

        return data
    }
}
