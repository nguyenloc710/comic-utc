package vn.edu.utc.comic.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.io.IOException;
import org.apache.catalina.connector.ClientAbortException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.common.i18n.MessageService;

/**
 * Kiểm tra cách GlobalExceptionHandler phân loại lỗi, dùng một controller giả chỉ biết ném ngoại lệ.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.setUseCodeAsDefaultMessage(true);
        mockMvc = MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(new GlobalExceptionHandler(new MessageService(messageSource)))
                .build();
    }

    @Test
    void clientDisconnect_isNotTreatedAsServerError_andRendersNothing() throws Exception {
        MvcResult result = mockMvc.perform(get("/client-abort")).andReturn();

        // Không dựng trang lỗi (kết nối đã đóng) và không đổi thành 500
        assertThat(result.getModelAndView()).isNull();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentAsString()).isEmpty();
    }

    @Test
    void unexpectedError_onPage_rendersErrorPageWithStatus500() throws Exception {
        mockMvc.perform(get("/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name(ViewConstants.ERROR_PAGE));
    }

    @Test
    void unexpectedError_underApi_returnsJsonWithoutInternalDetails() throws Exception {
        mockMvc.perform(get("/api/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("error.internal"));
    }

    @Test
    void businessError_keepsItsOwnStatusAndMessageKey() throws Exception {
        mockMvc.perform(get("/api/business"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("GENRE_IN_USE"));
    }

    @Controller
    static class FailingController {

        @GetMapping("/client-abort")
        public String abort() throws IOException {
            throw new ClientAbortException("Trình duyệt đã đóng kết nối");
        }

        @GetMapping({"/unexpected", "/api/unexpected"})
        public String unexpected() {
            throw new IllegalStateException("chi tiết nội bộ không được lộ ra ngoài");
        }

        @GetMapping("/api/business")
        public String business() {
            throw new ApiException(ErrorCode.GENRE_IN_USE, 3);
        }
    }
}
