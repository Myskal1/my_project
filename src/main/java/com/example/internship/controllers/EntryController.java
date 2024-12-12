package com.example.internship.controllers;

import com.example.internship.entities.Entry;
import com.example.internship.exceptions.ErrorResponse;
import com.example.internship.services.EntryService;
import com.example.internship.services.UserDetailsImpl;
import com.example.internship.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/entries")
public class EntryController {

    private final EntryService entryService;
    private final UserService userService;

    public EntryController(EntryService entryService, UserService userService) {
        this.entryService = entryService;
        this.userService = userService;
    }

    @GetMapping
    public List<Entry> getAllEntries(Authentication authentication) {
        String email = ((UserDetailsImpl) authentication.getPrincipal()).getEmail();
        return entryService.getAllEntries(email);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Entry> getEntryById(@PathVariable Long id, Authentication authentication) {
        String email = ((UserDetailsImpl) authentication.getPrincipal()).getEmail();
        Entry entry = entryService.getEntryById(id, email);
        if (entry == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        return ResponseEntity.ok(entry);
    }

    @PostMapping
    public ResponseEntity<Entry> createEntry(@RequestBody Entry entry, Authentication authentication) {
        String email = ((UserDetailsImpl) authentication.getPrincipal()).getEmail();
        Entry createdEntry = entryService.createEntry(entry, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdEntry);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Entry> updateEntry(@PathVariable Long id, @RequestBody Entry updatedEntry, Authentication authentication) {
        String email = ((UserDetailsImpl) authentication.getPrincipal()).getEmail();
        Entry entry = entryService.updateEntry(id, updatedEntry, email);
        if (entry == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        return ResponseEntity.ok(entry);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEntry(@PathVariable Long id, @RequestParam(required = false) Boolean confirm, Authentication authentication) {
        if (confirm == null || !confirm) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Confirm deletion with parameter 'confirm=true'."));
        }

        String email = ((UserDetailsImpl) authentication.getPrincipal()).getEmail();
        boolean deleted = entryService.deleteEntry(id, email);

        if (deleted) {
            return ResponseEntity.ok("Successfully deleted");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Entry not found or you don't have permission to delete this entry."));
        }
    }

    @PostMapping("/{id}/images")
    public ResponseEntity<?> uploadImages(@PathVariable Long id,
                                          @RequestParam("images") List<MultipartFile> images,
                                          Authentication authentication) {
        String email = ((UserDetailsImpl) authentication.getPrincipal()).getEmail();
        List<String> uploadedImages = entryService.addImagesToEntry(id, images, email);

        if (uploadedImages == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found.");
        }

        return ResponseEntity.ok(uploadedImages);
    }

    @DeleteMapping(value = "/{id}/images/{fileName}")
    public ResponseEntity<?> deleteImage(@PathVariable Long id,
                                         @PathVariable String fileName,
                                         Authentication authentication) {
        String email = ((UserDetailsImpl) authentication.getPrincipal()).getEmail();
        String imageUrl = "images/" + fileName;

        boolean success = entryService.deleteImageFromEntry(id, imageUrl, email);

        if (!success) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Image or entry not found.");
        }

        return ResponseEntity.ok("Image successfully deleted.");
    }


    @GetMapping("/{id}/images/{fileName}")
    public ResponseEntity<?> downloadImage(@PathVariable Long id, @PathVariable String fileName) {
        File file = new File("images/" + fileName);

        if (!file.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Image not found");
        }

        try {
            byte[] imageData = Files.readAllBytes(file.toPath());
            String contentType = "image/jpeg";
            if (fileName.endsWith(".png")) {
                contentType = "image/png";
            } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                contentType = "image/jpeg";
            }

            return ResponseEntity.status(HttpStatus.OK)
                    .contentType(MediaType.valueOf(contentType))
                    .body(imageData);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error reading the image file");
        }
    }
}
