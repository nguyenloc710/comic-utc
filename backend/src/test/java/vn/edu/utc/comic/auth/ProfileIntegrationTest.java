package vn.edu.utc.comic.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import javax.imageio.ImageIO;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import vn.edu.utc.comic.common.config.StorageProperties;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.common.security.AppUserPrincipal;
import vn.edu.utc.comic.support.AbstractIntegrationTest;
import vn.edu.utc.comic.user.entity.UserAccount;
import vn.edu.utc.comic.user.enums.Role;

class ProfileIntegrationTest extends AbstractIntegrationTest {

    private static final String NEW_PASSWORD = "MatKhauMoi456";

    @Autowired
    private StorageProperties storageProperties;

    @Test
    void profilePages_requireLogin() throws Exception {
        mockMvc.perform(get("/me/profile")).andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(get("/me/password")).andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void profilePage_showsAccountAndPrefilledForm() throws Exception {
        UserAccount account = createAccount(Role.USER);

        mockMvc.perform(get("/me/profile").with(user(principal(account))))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.ME_PROFILE))
                .andExpect(content().string(Matchers.containsString(account.getEmail())))
                .andExpect(content().string(Matchers.containsString("value=\"" + account.getDisplayName() + "\"")));
    }

    @Test
    void updateProfile_savesFields_storesAvatar_andRemovesReplacedAvatar() throws Exception {
        UserAccount account = createAccount(Role.USER);

        mockMvc.perform(profileUpdate(account, "  Tên Mới  ", "Thích truyện tu tiên", pngAvatar()))
                .andExpect(redirectedUrl("/me/profile"));

        UserAccount saved = reload(account);
        assertThat(saved.getDisplayName()).isEqualTo("Tên Mới");
        assertThat(saved.getBio()).isEqualTo("Thích truyện tu tiên");
        assertThat(saved.getAvatarPath()).startsWith("avatars/" + account.getId() + "/").endsWith(".png");
        Path firstAvatar = storedFile(saved.getAvatarPath());
        assertThat(firstAvatar).isRegularFile();

        mockMvc.perform(profileUpdate(account, "Tên Mới", "", pngAvatar()))
                .andExpect(redirectedUrl("/me/profile"));

        UserAccount replaced = reload(account);
        assertThat(replaced.getBio()).isNull();
        assertThat(storedFile(replaced.getAvatarPath())).isRegularFile();
        assertThat(firstAvatar).doesNotExist();
    }

    @Test
    void updateProfile_withoutChoosingAFile_keepsCurrentAvatar() throws Exception {
        UserAccount account = createAccount(Role.USER);
        mockMvc.perform(profileUpdate(account, "Có Ảnh", "", pngAvatar()));
        String avatarPath = reload(account).getAvatarPath();
        MockMultipartFile noFileChosen = new MockMultipartFile("avatar", "", "application/octet-stream", new byte[0]);

        mockMvc.perform(profileUpdate(account, "Đổi Tên Thôi", "", noFileChosen))
                .andExpect(redirectedUrl("/me/profile"));

        assertThat(reload(account).getAvatarPath()).isEqualTo(avatarPath);
        assertThat(storedFile(avatarPath)).isRegularFile();
    }

    @Test
    void updateProfile_rejectsFakeImage_andSavesNothing() throws Exception {
        UserAccount account = createAccount(Role.USER);
        MockMultipartFile fake = new MockMultipartFile("avatar", "anh.png", "image/png",
                "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(profileUpdate(account, "Tên Không Được Lưu", "", fake))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.ME_PROFILE))
                .andExpect(model().attributeHasFieldErrorCode(ViewConstants.ATTR_FORM, "avatar",
                        "error.image.type.not.allowed"));

        UserAccount unchanged = reload(account);
        assertThat(unchanged.getDisplayName()).isEqualTo(account.getDisplayName());
        assertThat(unchanged.getAvatarPath()).isNull();
    }

    @Test
    void changePassword_reportsEveryProblemAtOnce_andKeepsOldPassword() throws Exception {
        UserAccount account = createAccount(Role.USER);

        mockMvc.perform(passwordChange(account, "khong-dung", "yeu", "khac"))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.ME_PASSWORD))
                .andExpect(model().attributeHasFieldErrors(ViewConstants.ATTR_FORM,
                        "currentPassword", "newPassword", "confirmPassword"));

        mockMvc.perform(formLogin("/login").user(account.getUsername()).password(ACCOUNT_PASSWORD))
                .andExpect(authenticated());
    }

    @Test
    void changePassword_rejectsReusingCurrentPassword() throws Exception {
        UserAccount account = createAccount(Role.USER);

        mockMvc.perform(passwordChange(account, ACCOUNT_PASSWORD, ACCOUNT_PASSWORD, ACCOUNT_PASSWORD))
                .andExpect(model().attributeHasFieldErrorCode(ViewConstants.ATTR_FORM, "newPassword",
                        "error.password.same.as.current"));
    }

    @Test
    void changePassword_replacesPassword() throws Exception {
        UserAccount account = createAccount(Role.USER);

        mockMvc.perform(passwordChange(account, ACCOUNT_PASSWORD, NEW_PASSWORD, NEW_PASSWORD))
                .andExpect(redirectedUrl("/me/profile"));

        mockMvc.perform(formLogin("/login").user(account.getUsername()).password(NEW_PASSWORD))
                .andExpect(authenticated());
        mockMvc.perform(formLogin("/login").user(account.getUsername()).password(ACCOUNT_PASSWORD))
                .andExpect(unauthenticated());
    }

    private MockHttpServletRequestBuilder profileUpdate(UserAccount account, String displayName, String bio,
                                                        MockMultipartFile avatar) {
        return multipart("/me/profile").file(avatar)
                .param("displayName", displayName)
                .param("bio", bio)
                .with(user(principal(account))).with(csrf());
    }

    private MockHttpServletRequestBuilder passwordChange(UserAccount account, String currentPassword,
                                                         String newPassword, String confirmPassword) {
        return post("/me/password")
                .param("currentPassword", currentPassword)
                .param("newPassword", newPassword)
                .param("confirmPassword", confirmPassword)
                .with(user(principal(account))).with(csrf());
    }

    private static AppUserPrincipal principal(UserAccount account) {
        return AppUserPrincipal.from(account, Instant.now());
    }

    private Path storedFile(String storageKey) {
        return Path.of(storageProperties.root()).resolve(storageKey);
    }

    private static MockMultipartFile pngAvatar() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB), "png", output);
        return new MockMultipartFile("avatar", "anh-dai-dien.png", "image/png", output.toByteArray());
    }
}
