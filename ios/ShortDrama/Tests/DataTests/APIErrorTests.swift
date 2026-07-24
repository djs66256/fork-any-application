import Foundation
import Testing
@testable import ShortDrama

struct APIErrorTests {

    @Test("T-07: Same server errors are equal")
    func testServerErrorEqual() {
        let a = APIError.server(code: 500, message: "boom")
        let b = APIError.server(code: 500, message: "boom")
        #expect(a == b)
    }

    @Test("T-08: Different server messages are not equal")
    func testServerErrorNotEqual() {
        let a = APIError.server(code: 500, message: "a")
        let b = APIError.server(code: 500, message: "b")
        #expect(a != b)
    }

    @Test("T-08: Different server codes are not equal")
    func testServerErrorDifferentCode() {
        let a = APIError.server(code: 500, message: "boom")
        let b = APIError.server(code: 400, message: "boom")
        #expect(a != b)
    }

    @Test("T-07: Same notImplemented errors are equal")
    func testNotImplementedEqual() {
        let a = APIError.notImplemented("not ready")
        let b = APIError.notImplemented("not ready")
        #expect(a == b)
    }

    @Test("T-07: invalidURL equals itself")
    func testInvalidURLEqual() {
        #expect(APIError.invalidURL == APIError.invalidURL)
    }

    @Test("T-07: invalidResponse equals itself")
    func testInvalidResponseEqual() {
        #expect(APIError.invalidResponse == APIError.invalidResponse)
    }

    @Test("T-07: cancelled equals itself")
    func testCancelledEqual() {
        #expect(APIError.cancelled == APIError.cancelled)
    }

    @Test("T-08: Different error types are not equal")
    func testDifferentTypesNotEqual() {
        #expect(APIError.invalidURL != APIError.invalidResponse)
    }

    @Test("T-07: decodingFailed equals itself")
    func testDecodingFailedEqual() {
        let error = NSError(domain: "test", code: 1)
        let a = APIError.decodingFailed(error)
        let b = APIError.decodingFailed(error)
        #expect(a == b)
    }

    @Test("T-07: network equals itself")
    func testNetworkEqual() {
        let error = NSError(domain: "test", code: 2)
        let a = APIError.network(underlying: error)
        let b = APIError.network(underlying: error)
        #expect(a == b)
    }

    @Test("T-05: server errorDescription returns the message")
    func testServerErrorDescription() {
        let error = APIError.server(code: 500, message: "boom")
        #expect(error.errorDescription == "boom")
    }

    @Test("T-06: notImplemented errorDescription returns the message")
    func testNotImplementedErrorDescription() {
        let error = APIError.notImplemented("not ready")
        #expect(error.errorDescription == "not ready")
    }

    @Test("invalidURL errorDescription is non-empty")
    func testInvalidURLErrorDescription() {
        #expect(APIError.invalidURL.errorDescription != nil)
    }

    @Test("invalidResponse errorDescription is non-empty")
    func testInvalidResponseErrorDescription() {
        #expect(APIError.invalidResponse.errorDescription != nil)
    }

    @Test("cancelled errorDescription is non-empty")
    func testCancelledErrorDescription() {
        #expect(APIError.cancelled.errorDescription != nil)
    }
}
