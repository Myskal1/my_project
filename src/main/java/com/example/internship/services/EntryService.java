package com.example.internship.services;

import com.example.internship.entities.Entry;
import com.example.internship.entities.User;
import com.example.internship.repositories.EntryRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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

    @Autowired
    public EntryService(EntryRepo entryRepo, UserService userService) {
        this.entryRepo = entryRepo;
        this.userService = userService;
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
                String fileName = saveImage(image);
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

        // Убедимся, что сравниваем только имя файла
        String fileName = imageUrl.replace("images/", "");
        if (!entry.getImages().contains(fileName)) {
            LOGGER.warning("Image file name not found in entry images: " + fileName);
            return false;
        }

        entry.getImages().remove(fileName);
        entryRepo.save(entry);

        Path filePath = Paths.get(UPLOAD_DIR, fileName);
        LOGGER.info("Attempting to delete file: " + filePath.toAbsolutePath());

        try {
            boolean fileDeleted = Files.deleteIfExists(filePath);
            LOGGER.info("File deletion status: " + fileDeleted);
        } catch (IOException e) {
            LOGGER.severe("Error deleting file: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    private String saveImage(MultipartFile image) throws IOException {
        String originalFilename = image.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("Invalid file name: " + originalFilename);
        }

        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = UUID.randomUUID().toString() + fileExtension;

        Path path = Paths.get(UPLOAD_DIR, fileName);
        LOGGER.info("Saving file: " + fileName + " at path: " + path.toAbsolutePath());
        Files.createDirectories(path.getParent());
        Files.write(path, image.getBytes());

        return fileName;
    }

}
