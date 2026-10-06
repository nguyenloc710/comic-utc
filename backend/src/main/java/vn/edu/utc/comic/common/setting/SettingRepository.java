package vn.edu.utc.comic.common.setting;

import org.springframework.data.jpa.repository.JpaRepository;

/** Truy vấn tham số vận hành. */
public interface SettingRepository extends JpaRepository<Setting, String> {
}
