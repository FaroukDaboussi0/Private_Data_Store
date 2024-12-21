package com.Farouk.security.storge.Service;

import com.Farouk.security.storge.Data.FileType;
import com.Farouk.security.storge.Data.KeywordRequest;
import com.Farouk.security.storge.Data.QuizRequest;
import com.Farouk.security.storge.Data.UploadedFile;
import com.Farouk.security.storge.Repository.UploadedFileRepository;
import com.Farouk.security.user.Data.User;
import org.hibernate.sql.Update;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.Farouk.security.storge.Data.FileType.DOCUMENT;

@Service
public class FilesStorageService {

    private final String uploadDir = "C:/uploads";
    private final UploadedFileRepository uploadedFileRepository;

    public FilesStorageService(UploadedFileRepository uploadedFileRepository) {
        this.uploadedFileRepository = uploadedFileRepository;
    }


    public void storeFiles(List<MultipartFile> files, User user, String directoryPath) throws IOException {
        Path userDir = Paths.get(uploadDir, user.getId().toString());
        Path targetDirectoryPath = userDir.resolve(directoryPath);

        if (!Files.exists(targetDirectoryPath)) {
            Files.createDirectories(targetDirectoryPath);
        }

        for (MultipartFile file : files) {
            String fileName = StringUtils.cleanPath(file.getOriginalFilename());
            Path filePath = targetDirectoryPath.resolve(fileName);

            if (Files.exists(filePath)) {
                // File already exists, handle accordingly
                System.out.println("File exists: " + fileName);
                // You can throw an exception, return a specific message, or take any other action
                // For now, let's just print a message
            } else {
                // File does not exist, proceed with uploading
                Files.copy(file.getInputStream(), filePath);
                System.out.println("File uploaded successfully: " + fileName);

                // Save file details to the database
                UploadedFile uploadedFile = new UploadedFile();
                uploadedFile.setLink(filePath.toString());
                uploadedFile.setFileName(fileName);
                uploadedFile.setDate(new Date());
                uploadedFile.setSize((float) file.getSize());

                // Extracting file extension to determine FileType
                String extension = fileName.substring(Math.max(0, fileName.length() - 3)).toLowerCase();

                FileType fileType;
                switch (extension.toLowerCase()) {
                    case "pdf":
                    case "doc":
                    case "docx":
                        fileType = DOCUMENT;
                        break;
                    case "jpg":
                    case "jpeg":
                    case "png":
                    case "gif":
                        fileType = FileType.IMAGE;
                        break;
                    case "mp4":
                    case "avi":
                    case "mkv":
                        fileType = FileType.VIDEO;
                        break;
                    case "mp3":
                    case "wav":
                    case "flac":
                        fileType = FileType.AUDIO;
                        break;
                    default:
                        fileType = FileType.OTHER;
                        break;
                }
                uploadedFile.setFileType(fileType);
                //api call
                // Set other file details
                uploadedFile.setKeywords(getKeywords(filePath.toString(),fileType.toString())); // You can set keywords if needed
                uploadedFile.setUser(user);
                // Save the UploadedFile entity to the database
                uploadedFileRepository.save(uploadedFile);
            }
        }
    }
    public Map<String, Object> listFilesAndDirectoriesInUserFolder(User user) throws IOException {
        Path userDir = Paths.get(uploadDir, user.getId().toString());
        System.out.println("User Directory Path: " + userDir);

        if (!Files.exists(userDir) || !Files.isDirectory(userDir)) {
            // User's directory does not exist or is not a directory
            throw new IllegalArgumentException("Invalid user directory");
        }

        Map<String, Object> result = new HashMap<>();

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(userDir)) {
            for (Path path : directoryStream) {
                if (Files.isDirectory(path)) {
                    // Recursively process subdirectories
                    String subdirectoryName = path.getFileName().toString();
                    Map<String, Object> subdirectoryContents = listFilesAndDirectoriesInSubdirectory(path);
                    result.put(subdirectoryName, subdirectoryContents);
                } else {
                    // Add file to the result
                    result.put(path.getFileName().toString(), null);
                }
            }
        }

        return result;
    }

    private Map<String, Object> listFilesAndDirectoriesInSubdirectory(Path subdirectory) throws IOException {
        Map<String, Object> subdirectoryContents = new HashMap<>();

        try (DirectoryStream<Path> subdirectoryStream = Files.newDirectoryStream(subdirectory)) {
            for (Path subpath : subdirectoryStream) {
                if (Files.isDirectory(subpath)) {
                    // Recursively process subdirectories
                    String subdirectoryName = subpath.getFileName().toString();
                    Map<String, Object> subdirectoryContentsRecursive = listFilesAndDirectoriesInSubdirectory(subpath);
                    subdirectoryContents.put(subdirectoryName, subdirectoryContentsRecursive);
                } else {
                    // Add file to the result
                    subdirectoryContents.put(subpath.getFileName().toString(), null);
                }
            }
        }

        return subdirectoryContents;
    }

    public Path getFilePath(User user, String fileName, String subdirectory) {
        Path userDir = Paths.get(uploadDir, user.getId().toString(), subdirectory);
        return userDir.resolve(fileName);
    }


    public Resource loadFileAsResource(User user, String fileName, String subdirectory) {
        Path filePath = getFilePath(user, fileName, subdirectory);

        try {
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return resource;
            } else {
                return null; // Or throw an exception if you want to handle this case differently
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error occurred while loading the file: " + fileName, e);
        }
    }
    public String getKeywords(String link, String type) {
        // Define the URL of the FastAPI endpoint
        String url = "http://localhost:8000/keyword";

        // Create a RestTemplate instance
        RestTemplate restTemplate = new RestTemplate();

        // Create a KeywordRequest instance with the provided link and type
        KeywordRequest request = new KeywordRequest(link, type);

        // Make an HTTP POST request to the FastAPI endpoint with the KeywordRequest object as the request body
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, request, String.class);



        // Process the response
        return responseEntity.getBody();
    }
    public  List<String>  searchFileLinksByNameAndUserId(String fileName, Integer userId) {
        return uploadedFileRepository.findLinkByFileNameContainingIgnoreCaseAndUser_Id(fileName, userId);
    }
    public UploadedFile getfiledetails(String filename) {
        return uploadedFileRepository.findByFileName(filename);
    }


    public List<UploadedFile> searchFiles(String searchTerm) {
        FileType fileType = null;
        if (searchTerm.equals("DOCUMENT") || searchTerm.equals("IMAGE") || searchTerm.equals("VIDEO") || searchTerm.equals("AUDIO") || searchTerm.equals("OTHER")) {
            fileType = FileType.valueOf(searchTerm);
        }

        if (fileType != null) {
            return uploadedFileRepository.findByFileType(fileType);
        } else {
            return uploadedFileRepository.findByFileNameContainingOrKeywordsContaining(searchTerm, searchTerm);
        }
    }
    public String generateQuiz(QuizRequest quizRequest) {
        // Define the URL of the FastAPI endpoint
        String url = "http://localhost:8000/quiz";

        // Create a RestTemplate instance
        RestTemplate restTemplate = new RestTemplate();

        try {
            // Make an HTTP POST request to the FastAPI endpoint with the link as the request body
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, quizRequest, String.class);

            // Check if the request was successful (status code 200)
            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                // Process the response
                return "Quiz generation successful!";
            } else {
                // Handle other status codes
                return "Error: Unexpected status code - " + responseEntity.getStatusCode();
            }
        } catch (HttpClientErrorException e) {
            // Check if the exception is due to a 404 error (path not found)
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return "Error: The specified path does not exist.";
            } else {
                // Handle other client errors
                return "Error: Client error occurred - " + e.getMessage();
            }
        } catch (Exception e) {
            // Handle other exceptions
            return "Error: An unexpected error occurred - " + e.getMessage();
        }
    }
    }

