package com.tool.RecruitXpert.Service;

import com.tool.RecruitXpert.DTO.UserDTO.SignUserDto;
import com.tool.RecruitXpert.DTO.UserDTO.UpdateUserStatus;
import com.tool.RecruitXpert.DTO.UserDTO.UserRequest;
import com.tool.RecruitXpert.DTO.UserDTO.UserResponse;
import com.tool.RecruitXpert.Entities.User;
import com.tool.RecruitXpert.Exceptions.UserNotFoundException;
import com.tool.RecruitXpert.Repository.UserRepository;
import com.tool.RecruitXpert.Transformer.UserTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JavaMailSender mailSender;

    public String signUp(SignUserDto signUp) throws Exception{

//        validation : check unique email
        boolean check = userRepository.existsByEmail(signUp.getEmail());
        if(check) throw new RuntimeException("Email already present");

        User user = new User(signUp.getEmail(), signUp.getPassword());
        userRepository.save(user);

        // email integration
        SimpleMailMessage mailMessage=new SimpleMailMessage();

        String body="Hi Welcome to RecruitXpert \n"+" Let's start your job search";
        mailMessage.setSubject("RecruitXpert");
        mailMessage.setFrom("shivasaiparsha@gmail.com");
        mailMessage.setTo(user.getEmail());
        mailMessage.setText(body);
        mailSender.send(mailMessage);
        return "signup successfully";
    }

    public String logIn(SignUserDto login){
        User user = userRepository.findByEmail(login.getEmail()).get();

        if(user.getEmail().equals(login.getEmail()) &&
                user.getPassword().equals(login.getPassword())){
            user.setPasswordCount(0); // for each successful login setting count 0;
            return "successful login";
        }

        if (user.isAccountBlock())
            throw new RuntimeException("Oops! you're account is blocked! reach-out to Admin");

        if(user.getPasswordCount() > 3){
            user.setAccountBlock(true);
            throw new RuntimeException("You've already done 3 wrong attempts, now " +
                    "kindly reach-out to admin for further actions.");
        }

        else {  // if wrong then each time we're increasing count
            int count = user.getPasswordCount();
            user.setPasswordCount(count + 1);
            return "Incorrect organisation, email or password";
        }
    }


    public UserResponse addUser(UserRequest userRequest) {
        User user = UserTransformer.UserRequestToUser(userRequest);
        User savedUser = userRepository.save(user);
        return UserTransformer.UserToUserResponse(savedUser);
    }

    public String deleteUser(int id) {
            Optional<User> optionalUser = userRepository.findById(id);
            if(!optionalUser.isPresent()){
                throw new UserNotFoundException("User not Found");
            }
            userRepository.deleteById(id);
            return "User Successfully Deleted";
    }

    public String updateUserStatus(UpdateUserStatus updateUserStatus) {
        Optional<User> optionalUser = userRepository.findById(updateUserStatus.getId());
        if(!optionalUser.isPresent()){
            throw new UserNotFoundException("User not Found");
        }
           User user = new User();
           user.setStatus(updateUserStatus.getStatus());
           userRepository.save(user);
           return "Status Updated Succesfully";
    }

    public String updateUser(UserRequest userRequest) {
        User user = UserTransformer.UserRequestToUser(userRequest);
        userRepository.save(user);
        return "Updated Successfully !!";

    }
}
