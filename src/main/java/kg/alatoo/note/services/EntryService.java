package kg.alatoo.note.services;

import kg.alatoo.note.entities.Entry;
import kg.alatoo.note.entities.User;
import kg.alatoo.note.repositories.EntryRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class EntryService {

    private static final Logger LOGGER = Logger.getLogger(EntryService.class.getName());
    private static final String UPLOAD_DIR = "images/";

    private final EntryRepo entryRepo;
    private final UserService userService;
    private final S3Client s3Client;

    @Autowired
    public EntryService(EntryRepo entryRepo, UserService userService) {
        this.entryRepo = entryRepo;
        this.userService = userService;
        // Initialize the S3Client
        this.s3Client = S3Client.builder()
                .region(software.amazon.awssdk.regions.Region.of("eu-north-1")) // Choose the correct region
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("YOUR_ACCESS_KEY", "YOUR_SECRET_KEY"))) // Use the appropriate keys
                .build();
    }

    public List<Entry> getAllEntries(String email) {
        User user = userService.findByEmail(email);
        if (user == null) {
            LOGGER.warning("User not found with email: " + email);
            return new ArrayList<>();
        }
        return entryRepo.findByUserId(user.getId());
    }

    public Entry getEntryById(Long id, String email) {
        User user = userService.findByEmail(email);
        if (user == null) {
            LOGGER.warning("User not found with email: " + email);
            return null;
        }
        return entryRepo.findByIdAndUserId(id, user.getId()).orElse(null);
    }

    public Entry createEntry(Entry entry, String email) {
        User user = userService.findByEmail(email);
        if (user == null) {
            LOGGER.warning("User not found with email: " + email);
            return null;
        }
        entry.setUser(user);
        return entryRepo.save(entry);
    }

    public Entry updateEntry(Long id, Entry updatedEntry, String email) {
        Entry existingEntry = getEntryById(id, email);
        if (existingEntry == null) {
            LOGGER.warning("Entry not found or access denied for entry ID: " + id);
            return null;
        }

        existingEntry.setTitle(updatedEntry.getTitle());
        existingEntry.setContent(updatedEntry.getContent());
        existingEntry.setStatus(updatedEntry.getStatus());

        return entryRepo.save(existingEntry);
    }

    public boolean deleteEntry(Long id, String email) {
        Entry existingEntry = getEntryById(id, email);
        if (existingEntry == null) {
            LOGGER.warning("Entry not found or access denied for entry ID: " + id);
            return false;
        }

        entryRepo.delete(existingEntry);
        return true;
    }

    public List<String> addImagesToEntry(Long id, List<MultipartFile> images, String email) {
        Entry entry = getEntryById(id, email);
        if (entry == null) {
            LOGGER.warning("Entry not found or access denied for entry ID: " + id);
            return null;
        }

        List<String> uploadedImages = new ArrayList<>();

        for (MultipartFile image : images) {
            try {
                String fileName = saveImageToS3(image);
                if (fileName != null) {
                    uploadedImages.add(fileName);
                    entry.getImages().add(fileName);
                }
            } catch (IllegalArgumentException | IOException e) {
                LOGGER.log(Level.SEVERE, "Error while processing image upload: " + e.getMessage(), e);
            }
        }

        entryRepo.save(entry);
        LOGGER.info("Images successfully added to entry. Current images: " + entry.getImages());
        return uploadedImages;
    }

    public boolean deleteImageFromEntry(Long id, String imageUrl, String email) {
        User user = userService.findByEmail(email);
        Entry entry = entryRepo.findByIdAndUserId(id, user.getId()).orElse(null);
        if (entry == null) {
            LOGGER.warning("Entry not found for user: " + email + ", id: " + id);
            return false;
        }

        String fileName = imageUrl.replace("images/", "");
        if (!entry.getImages().contains(fileName)) {
            LOGGER.warning("Image file name not found in entry images: " + fileName);
            return false;
        }

        entry.getImages().remove(fileName);
        entryRepo.save(entry);

        // Delete image from S3
        deleteImageFromS3(fileName);

        return true;
    }

    private String saveImageToS3(MultipartFile image) throws IOException {
        String originalFilename = image.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("Invalid file name: " + originalFilename);
        }

        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = UUID.randomUUID().toString() + fileExtension;

        // Logic to upload file to S3
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket("your-bucket-name")
                .key("images/" + fileName)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(image.getBytes()));
        return "images/" + fileName; // Return the path to store in the database
    }

    private void deleteImageFromS3(String fileName) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket("your-bucket-name")
                .key("images/" + fileName)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
    }

}