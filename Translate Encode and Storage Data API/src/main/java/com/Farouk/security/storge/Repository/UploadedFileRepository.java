package com.Farouk.security.storge.Repository;

import com.Farouk.security.storge.Data.FileType;
import com.Farouk.security.storge.Data.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.util.List;
import java.util.Optional;
@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFile,Long> {
    // Use a native query with projection to fetch only the file links
    @Query(value = "SELECT link FROM uploaded_file WHERE LOWER(file_name) LIKE %:fileName% AND user_id = :userId", nativeQuery = true)
    List<String> findLinkByFileNameContainingIgnoreCaseAndUser_Id(@Param("fileName") String fileName, @Param("userId") Integer userId);
    UploadedFile findByFileName(String fileName);
    List<UploadedFile> findByFileNameContainingOrKeywordsContaining(String fileName, String keywords);
    List<UploadedFile> findByFileType(FileType fileType);
}