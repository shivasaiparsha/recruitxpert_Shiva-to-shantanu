package com.tool.RecruitXpert.Service;

import com.tool.RecruitXpert.Entities.ResumeEntity;
import com.tool.RecruitXpert.Entities.User;
import com.tool.RecruitXpert.Exceptions.ResumeNotFoundException;
import com.tool.RecruitXpert.Exceptions.UserNotFoundException;
import com.tool.RecruitXpert.Repository.ResumeRepository;
import com.tool.RecruitXpert.Repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.tool.RecruitXpert.ResumeUtility.ResumeUtilities.getMediaType;
import static com.tool.RecruitXpert.Transformer.ResumeBuilderTransformer.buildResumeObject;

@Service
@Slf4j
public class ResumeService {
    @Autowired
    ResumeRepository resumeRepository;
    @Autowired
    UserRepository userRepository;


    // get all list of resumes

    // save resume in db
    public String  saveResumeToDb(MultipartFile file, int userId) throws IOException {

        long sizeInBytes = file.getSize(); // Getting size of the uploaded file in bytes
        long sizeInKb=sizeInBytes/1024;
        long sizeInMb = sizeInBytes /2048; // Converting bytes to megabyte
        long lowBound=50;
        long upperBound=1; // 1mb size
        if(sizeInKb<50) {
            log.error("resume size is less than  50kb log");
            throw new IOException("uploaded Resume doc size"+sizeInKb+"KB is lessthan required size"+lowBound);
        }

        if(sizeInMb>upperBound) {
            log.error("resume size is greater than  1mb log");
            throw new IOException("uploaded image size"+sizeInMb+"MB is greaterthan required size"+upperBound);
        }

        User user = userRepository.findById(userId).get();
        ResumeEntity resume= buildResumeObject(file, user);
        resumeRepository.save(resume);
        user.getResumeList().add(resume);
        userRepository.save(user);

//        get current versioning  == 0
//        set resumeId


        return "resume uploaded successfully";
    }

    // download  Resume service
    public ResponseEntity<byte[]> downloadResume(Integer id) {
        Optional<ResumeEntity> dbImageData = resumeRepository.findById(id);
        byte[] images = dbImageData.get().getResume();

        String typearray[] = dbImageData.get().getDocType().split("/");
        String type = typearray[1];

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(getMediaType(type))
                .body(images);
    }


    // find all resumes of particular User with user id
    public List<InputStream> findAllResumes(int userid) throws Exception {
        User user = userRepository.findById(userid).get();
        if (user == null) throw new UserNotFoundException("user not found with userId " + userid);
        List<ResumeEntity> resumeEntitiesList = resumeRepository.findByUser(user);

        if (resumeEntitiesList.size() == 0) {
            throw new ResumeNotFoundException("Resume  Not Found Exception");
        }
        List<ResumeEntity> resumeList = new ArrayList<>();
        for (ResumeEntity resumeEntity : resumeEntitiesList) {
            resumeList.add(resumeEntity);
        }

        List<InputStream> pdfStreams = new ArrayList<>();

        for (ResumeEntity pdfEntity : resumeEntitiesList) {
            // Create an InputStream from the byte array of the PDF content
            InputStream pdfStream = new ByteArrayInputStream(pdfEntity.getResume());
            pdfStreams.add(pdfStream);
        }

        return pdfStreams;
    }

    // Delete resume by resumeId service
    public String deleteResumeByResumeId(int resumeId) throws Exception {

        if (!resumeRepository.existsById(resumeId))
            throw new Exception("resume not found exception"); //if resume id not found throw exception
        resumeRepository.deleteById(resumeId);
        return resumeId + " resume : " + resumeId + " deleted successfully";
    }

    //delete resumes of user by user id services
    public String deleteUserByUserId(int userId) throws Exception {
        if (!userRepository.existsById(userId))
            throw new UserNotFoundException("user not found with userId " + userId);

        User user = userRepository.findById(userId).get();
        resumeRepository.deleteByUserId(userId);
        return "successfully deleted resumes of userId :" + userId;
    }

    public List<ResumeEntity> getResumeList() {
        return resumeRepository.findAll();
    }

    public String versionControlMethod(int resumeId) {

        // set resume id in user entity
        Optional<ResumeEntity> optional = resumeRepository.findById(resumeId);
        ResumeEntity resume = optional.get();
        int userId = resume.getUser().getUserId();

        User user = userRepository.findById(userId).get();
        user.setCurrentResumeVersion(resumeId);

        userRepository.save(user);

        return "resume updated successfully";
    }

    public ResponseEntity<byte[]>  showResume(int userId) {
        Optional<User> op = userRepository.findById(userId);
        User user = op.get();
        int resumeId = user.getCurrentResumeVersion();
        ResponseEntity<byte[]> resume =  downloadResume(resumeId);
        return resume;
    }

}

