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
            let (response, data) = try handler(request)
            client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
            client?.urlProtocol(self, didLoad: data)
            client?.urlProtocolDidFinishLoading(self)
        } catch {
            client?.urlProtocol(self, didFailWithError: error)
        }
    }

    override func stopLoading() {}
}
