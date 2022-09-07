package org.apache.coyote.http11.handler;

import static org.apache.coyote.http11.HttpStatus.FOUND;
import static org.apache.coyote.http11.HttpStatus.NOT_FOUND;
import static org.apache.coyote.http11.HttpStatus.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Queue;
import nextstep.jwp.handler.ServletAdvice;
import org.apache.coyote.http11.HttpHeader;
import org.apache.coyote.http11.request.HttpRequest;
import org.apache.coyote.http11.request.HttpRequestLine;
import org.apache.coyote.http11.response.ResponseEntity;
import org.apache.coyote.http11.response.file.FileHandler;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class HttpFrontServletTest {

    private HttpRequest getHttpRequest(final String rawRequestLine, final String requestBody) {
        final HttpRequestLine requestLine = HttpRequestLine.of(rawRequestLine);
        final Queue<String> rawRequest = new LinkedList<>();
        rawRequest.add("name: eve");
        final HttpHeader httpHeader = HttpHeader.of(rawRequest);

        return HttpRequest.of(requestLine, httpHeader, requestBody);
    }

    private final HttpFrontServlet httpFrontServlet = new HttpFrontServlet(
            RequestServletMapping.init(), ServletAdvice.init());
    @Nested
    @DisplayName("handle 메소드는")
    class Handle {


        @Test
        @DisplayName("static file uri를 반환하는 핸들러가 실행되면 해당 파일 정보가 포함된 ResponseEntity를 반환한다.")
        void success_file() throws IOException {
            // given
            final String rawRequestLine = "GET /login HTTP/1.1";
            final HttpRequest httpRequest = getHttpRequest(rawRequestLine, "");

            // when
            final ResponseEntity response = httpFrontServlet.service(httpRequest);

            // then
            final URL url = FileHandler.class.getClassLoader().getResource("static/login.html");
            final Path path = Path.of(url.getPath());
            final byte[] fileBytes = Files.readAllBytes(path);

            assertAll(() -> {
                assertThat(response).extracting("httpStatus", "body")
                        .containsExactly(OK, new String(fileBytes));
                assertThat(response.getHttpHeader().getHeader("Content-Type")).isEqualTo(Files.probeContentType(path));
            });
        }

        @Test
        @DisplayName("body를 반환하는 핸들러가 실행되면 해당 body 정보가 포함된 ResponseEntity를 반환한다.")
        void success_body() throws IOException {
            // given
            final String rawRequestLine = "POST /login?account=gugu&password=password HTTP/1.1";
            final HttpRequest httpRequest = getHttpRequest(rawRequestLine, "");

            // when
            final ResponseEntity response = httpFrontServlet.service(httpRequest);

            // then
            assertAll(() -> {
                assertThat(response).extracting("httpStatus", "body")
                        .containsExactly(FOUND, "");
                assertThat(response.getHttpHeader().getHeader("Location")).isEqualTo("/index.html");
            });
        }

        @Test
        @DisplayName("입력 받은 path와 매핑되는 핸들러가 없다면 Redirect Not Found ResponseEntity를 반환한다.")
        void success_notFountResponse() throws IOException {
            // given
            final String rawRequestLine = "GET /wrong HTTP/1.1";
            final HttpRequest httpRequest = getHttpRequest(rawRequestLine, "");

            // when
            final ResponseEntity response = httpFrontServlet.service(httpRequest);

            // then
            assertAll(() -> {
                assertThat(response).extracting("httpStatus", "body")
                        .containsExactly(FOUND, "");
                assertThat(response.getHttpHeader().getHeader("Location")).isEqualTo(
                        NOT_FOUND.getStatusCode() + ".html");
            });
        }
        @Test
        @DisplayName("매핑한 핸들러 실행 도중 IllegalArgumentException 예외가 발생하면 Redirect Not Found ResponseEntity를 반환한다.")
        void success_ServerErrorResponse() throws IOException {
            // given
            final String rawRequestLine = "POST /login?account=wrong&password=password HTTP/1.1";
            final HttpRequest httpRequest = getHttpRequest(rawRequestLine, "");

            // when
            final ResponseEntity response = httpFrontServlet.service(httpRequest);

            // then
            assertAll(() -> {
                assertThat(response).extracting("httpStatus", "body")
                        .containsExactly(FOUND, "");
                assertThat(response.getHttpHeader().getHeader("Location")).isEqualTo(
                        NOT_FOUND.getStatusCode() + ".html");
            });
        }

    }
}