package com.Farouk.security.storge.Controller;
import com.Farouk.security.storge.Data.QuizRequest;
import com.Farouk.security.storge.Data.UploadedFile;
import com.Farouk.security.storge.Service.FilesStorageService;
import com.Farouk.security.user.Data.User;
import com.Farouk.security.user.Data.UserRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/files")
public class StorgeController {
    @Autowired
    private FilesStorageService fileStorageService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/uploadFiles")
    public ResponseEntity<String> uploadFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("directoryPath") String directoryPath
    ) {
        System.out.println("Debug message: salem");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User userDetails = (User) authentication.getPrincipal();
        System.out.println("Debug message: 2");
        try {
            fileStorageService.storeFiles(files, userDetails, directoryPath);
            System.out.println("Debug message: 3");
            return ResponseEntity.ok("Files uploaded successfully!");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload files.");
        }
    }

    @GetMapping("/listFilesAndDirectories")
    public ResponseEntity<Map<String, Object>> listFilesAndDirectories() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            // User not authenticated
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        User userDetails = (User) authentication.getPrincipal();

        try {
            Map<String, Object> directoryStructure = fileStorageService.listFilesAndDirectoriesInUserFolder(userDetails);
            return ResponseEntity.ok(directoryStructure);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    @GetMapping("/getfile")
    public ResponseEntity<Resource> downloadFile(@RequestParam  String subdirectory,
                                                 @RequestParam  String fileName) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User userDetails = (User) authentication.getPrincipal();
        Resource resource = (Resource) fileStorageService.loadFileAsResource(userDetails, fileName, subdirectory);



        if (resource == null) {
            return ResponseEntity.notFound().build();
        }



        String mimeType = Files.probeContentType(Paths.get(resource.getFile().getAbsolutePath()));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .header(HttpHeaders.CONTENT_TYPE,  mimeType)
                .body(resource);
    }
    @GetMapping("/searchbyname")
    public  List<String>  searchFileLinksByNameAndUserId(@RequestParam String fileName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User userDetails = (User) authentication.getPrincipal();
        return fileStorageService.searchFileLinksByNameAndUserId(fileName, userDetails.getId());
    }
    @GetMapping("/getfiledetails")
    public UploadedFile getfiledetails(@RequestParam String filename){
        return fileStorageService.getfiledetails(filename);
    }
    @GetMapping("/search")
    public List<UploadedFile> searchFiles(@RequestParam String searchTerm) {
        // Call the method in your service class to search for files
        return fileStorageService.searchFiles(searchTerm);
    }
    @GetMapping("/generate-quiz")
    public ResponseEntity<String> generateQuiz(@RequestBody QuizRequest quizRequest) {
        String path = quizRequest.path;
        if (path == null || path.isEmpty()) {
            return ResponseEntity.badRequest().body("Link parameter is missing or empty");
        }

        String result = fileStorageService.generateQuiz(quizRequest);
        return ResponseEntity.ok(result);
    }

}

