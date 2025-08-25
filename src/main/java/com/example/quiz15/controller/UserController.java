package com.example.quiz15.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.quiz15.service.ifs.UserService;
import com.example.quiz15.vo.AddInfoReq;
import com.example.quiz15.vo.BasicRes;
import com.example.quiz15.vo.LoginReq;

import jakarta.validation.Valid;
/**
 * @CrossOrigin
 * 可跨域資源共享的請求</br>
 *  (雖然前端後端系統都在自己的同一台電腦 但前端呼叫後端的 api 時 也會被認為是跨域請求)
 */
@CrossOrigin 
@RestController
public class UserController {
	@Autowired
	private UserService userService;
	
	@PostMapping(value="user/addInfo")
	public BasicRes addInfo(@Valid @RequestBody AddInfoReq req) {
		return userService.addInfo(req);
	}
	
	@PostMapping(value="user/login")
	public BasicRes login(@Valid @RequestBody LoginReq req) {
		return userService.login(req);
	}
}
