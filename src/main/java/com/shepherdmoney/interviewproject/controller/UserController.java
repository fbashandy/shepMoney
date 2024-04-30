package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.model.User;
import com.shepherdmoney.interviewproject.repository.UserRepository;

import com.shepherdmoney.interviewproject.vo.request.CreateUserPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {

    // TODO: wire in the user repository (~ 1 line)
    @Autowired
    private UserRepository userRepository;
    @PutMapping("/user")
    public ResponseEntity<Integer> createUser(@RequestBody CreateUserPayload payload) {
        // TODO: Create an user entity with information given in the payload, store it in the database
        User newUser = new User(payload.getName(), payload.getEmail());
        // save
        newUser = userRepository.save(newUser);
        //
        return ResponseEntity.ok(newUser.getId());
        //       and return the id of the user in 200 OK response

    }

    @DeleteMapping("/user")
    public ResponseEntity<String> deleteUser(@RequestParam int userId) {
        // TODO: Return 200 OK if a user with the given ID exists, and the deletion is successful
        //       Return 400 Bad Request if a user with the ID does not exist
        //       The response body could be anything you consider appropriate
        if (userRepository.existsById(userId)) {
            // exist then delete
            userRepository.deleteById(userId);
            // success
            return ResponseEntity.ok("User deleted successfully.");
        } else {
            // doesn't exist 400 bad request
            return ResponseEntity.badRequest().body("User not found with ID: " + userId);
        }
    }
}
