package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.Attachment;
import eu.bbmri.eric.csit.service.negotiator.database.repository.AttachmentRepository;
import eu.bbmri.eric.csit.service.negotiator.dto.attachments.AttachmentDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.attachments.AttachmentMetadataDTO;
import eu.bbmri.eric.csit.service.negotiator.exceptions.StorageException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.StorageFileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

// @Service(value = "DefaultAttachmentService")
public class FileSystemAttachmentService implements AttachmentService {

  private final Path rootLocation;
  @Autowired private final AttachmentRepository attachmentRepository;
  private final ModelMapper modelMapper;

  @Autowired
  public FileSystemAttachmentService(
      AttachmentRepository attachmentRepository, ModelMapper modelMapper) {
    this.rootLocation = Paths.get("/tmp");
    this.attachmentRepository = attachmentRepository;
    this.modelMapper = modelMapper;
  }

  @Override
  public AttachmentMetadataDTO create(MultipartFile file) {
    Attachment attachment = Attachment.builder().name(file.getOriginalFilename()).build();
    attachment = attachmentRepository.save(attachment);
    try {
      if (file.isEmpty()) {
        throw new StorageException("Failed to store empty file.");
      }
      Path destinationFile =
          this.rootLocation
              .resolve(Paths.get(Objects.requireNonNull(file.getOriginalFilename())))
              .normalize()
              .toAbsolutePath();
      if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
        // This is a security check
        throw new StorageException("Cannot store file outside current directory.");
      }
      try (InputStream inputStream = file.getInputStream()) {
        Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      throw new StorageException("Failed to store file.", e);
    }
    return modelMapper.map(attachment, AttachmentMetadataDTO.class);
    //    return AttachmentDTO.builder().id(attachment.getId()).name(attachment.getName()).build();
  }

  @Override
  public AttachmentDTO findById(String id) {
    return null;
  }

  @Override
  public AttachmentMetadataDTO findMetadataById(String id) {
    return null;
  }

  @Override
  public List<AttachmentMetadataDTO> getAllFiles() {
    return null;
  }

  public Stream<Path> loadAll() {
    try {
      return Files.walk(this.rootLocation, 1)
          .filter(path -> !path.equals(this.rootLocation))
          .map(this.rootLocation::relativize);
    } catch (IOException e) {
      throw new StorageException("Failed to read stored files", e);
    }
  }

  public Path load(String filename) {
    return rootLocation.resolve(filename);
  }

  public Resource loadAsResource(String filename) {
    try {
      Path file = load(filename);
      Resource resource = new UrlResource(file.toUri());
      if (resource.exists() || resource.isReadable()) {
        return resource;
      } else {
        throw new StorageFileNotFoundException("Could not read file: " + filename);
      }
    } catch (MalformedURLException e) {
      throw new StorageFileNotFoundException("Could not read file: " + filename, e);
    }
  }

  public void deleteAll() {
    FileSystemUtils.deleteRecursively(rootLocation.toFile());
  }

  public void init() {
    try {
      Files.createDirectories(rootLocation);
    } catch (IOException e) {
      throw new StorageException("Could not initialize storage", e);
    }
  }
}