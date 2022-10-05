package com.example.ej13uploaddownloadfile.application;

import com.example.ej13uploaddownloadfile.dtos.output.FileOutputDto;
import com.example.ej13uploaddownloadfile.entity.File;
import com.example.ej13uploaddownloadfile.exceptions.FileNotFound;
import com.example.ej13uploaddownloadfile.repository.FileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class FileServiceImpl implements FileService {
    @Autowired
    FileRepository fileRepository;

    Path root;


    /**
     * Función que recibe el directorio dónde se van a ubicar los archivos que se suben en el servidor en tiempo de ejecución,
     * si no existe se creará un directorio con ese nombre
     * @param path = Directorio dónde se van a subir los archivos
     * @exception RuntimeException Se devolverá un mensaje en tiempo de ejecución en la consola
     */
    @Override
    public void init(String path) {
        try {
            this.root = Paths.get(Objects.isNull(path) ? "src/" : path);
            Files.createDirectory(root);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize folder for upload!");
        }
    }


    /**
     * Función que elimina el directorio pasado por parametro,
     * si path es nulo cógera como path predeterminado src
     * @param path = Directorio que se desea eliminar
     */
    @Override
    public void deleteAll(String path) {
        this.root = Paths.get(Objects.isNull(path) ? "src/" : path);
        FileSystemUtils.deleteRecursively(root.toFile());
        fileRepository.deleteAll();
    }


    /**
     * Función para guardar el archivo pasado por parametro, tanto en la base de datos como en el equipo local
     * @param file = archivo que se desea guardar
     * @return FileOutputDto del fichero guardado
     * @exception RuntimeException
     */
    @Override
    public FileOutputDto save(MultipartFile file) {
        try {
            System.out.println(root);
            Files.copy(file.getInputStream(), this.root.resolve(file.getOriginalFilename()));
            System.out.println(root);

            File newFile = new File(file.getContentType().split("/")[1], file.getOriginalFilename(), LocalDateTime.now());

            fileRepository.save(newFile);

            return new FileOutputDto(newFile);
        } catch (Exception e) {
            throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
        }
    }


    /**
     * Función que buscará el fichero que tenga el mismo nombre que el pasado por parámetro, primero se buscará en la base de datos,
     * para obtener toda su información y a partir de su propiedad name poder buscarlo en local formando una Uri
     * @param filename = nombre del archivo
     * @return Resource = archivo que tiene el mismo nombre que le hemos pasado, previamente comprobado si existe o no
     * @exception FileNotFound en caso de que no se encuentre el archivo
     */
    @Override
    public Resource getFileByName(String filename) {
        try {
            File fileObjeto = fileRepository.findByName(filename).orElseThrow(() -> new FileNotFound("File does not exist"));

            Path file = root.resolve(fileObjeto.getName());
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new FileNotFound("The file does not exist");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }


    /**
     * Función qeu busca el archivo que tenga la id pasada por parámetro, primero se buscara en la base de datos, para obtener
     * la información del archivo y poder establecer su Uri a partir de su nombre
     * @param id = id del archivo que deseamos buscar
     * @return Resource = archivo que hemos buscado
     * @exception FileNotFound en caseo de que no encontremos el archivo en la base de datos o no es encuentre en local
     */
    @Override
    public Resource getFileById(Long id) {
        try {
            File fileObj = fileRepository.findById(id).orElseThrow(() -> new FileNotFound("File does not exist"));

            Path file = root.resolve(fileObj.getName());
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new FileNotFound("The file does not exist");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }
}
